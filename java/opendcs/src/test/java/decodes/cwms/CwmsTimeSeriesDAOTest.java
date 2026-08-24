package decodes.cwms;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import decodes.sql.DbKey;
import decodes.tsdb.DataCollection;
import decodes.tsdb.DbIoException;
import decodes.tsdb.TasklistRec;
import decodes.util.DecodesSettings;
import fixtures.NonPoolingConnectionOwner;
import opendcs.dai.DataTypeDAI;
import opendcs.dai.SiteDAI;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

final class CwmsTimeSeriesDAOTest
{
	@Test
	public void testExceedsMaxTimeGap()
	{
		try(CwmsTimeSeriesDAO cwmsTimeSeriesDAO = new CwmsTimeSeriesDAO(new NonPoolingConnectionOwner(), "SWT"))
		{
			Calendar first = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			first.set(2015, Calendar.JANUARY, 1, 0, 0, 0);
			Calendar second = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			second.set(2015, Calendar.JANUARY, 9, 0, 0, 0);
			assertTrue(cwmsTimeSeriesDAO.exceedsMaxTimeGap(first.getTime(), second.getTime()));
			second.set(2015, Calendar.JANUARY, 8, 0, 0, 0);
			assertTrue(cwmsTimeSeriesDAO.exceedsMaxTimeGap(first.getTime(), second.getTime()));
		}
	}

	@Test
	public void testWithinMaxTimeGap()
	{
		try(CwmsTimeSeriesDAO cwmsTimeSeriesDAO = new CwmsTimeSeriesDAO(new NonPoolingConnectionOwner(), "SWT"))
		{
			Calendar first = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			first.set(2015, Calendar.JANUARY, 1, 0, 0, 0);
			Calendar second = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			second.set(2015, Calendar.JANUARY, 7, 23, 59, 59);
			assertFalse(cwmsTimeSeriesDAO.exceedsMaxTimeGap(first.getTime(), second.getTime()));
		}
	}

	/**
	 * {@link DecodesSettings} is a process wide singleton, so anything a test flips has to be put
	 * back or it leaks into whatever runs next in the same JVM.
	 */
	@AfterEach
	void restoreRetryFailedComputations()
	{
		DecodesSettings.instance().retryFailedComputations = false;
	}

	@Test
	void testBuildTaskListQueryDefaults()
	{
		try(CwmsTimeSeriesDAO dao = new CwmsTimeSeriesDAO(new NonPoolingConnectionOwner(), "SWT"))
		{
			String sql = dao.buildTaskListQuery(100, 0);

			assertTrue(sql.contains("from CP_COMP_TASKLIST a"));
			// The loading application id must stay bind parameter 1.
			assertTrue(sql.contains("where a.LOADING_APPLICATION_ID = ?"));
			assertTrue(sql.contains("ROWNUM < 100"));
			assertEquals(1, countOccurrences(sql, '?'), "expected exactly one bind parameter");
			assertFalse(sql.contains("FAIL_TIME"));
			assertFalse(sql.contains("DATE_TIME_LOADED"));
			assertTrue(sql.endsWith(" ORDER BY a.site_datatype_id, a.start_date_time"));
		}
	}

	@Test
	void testBuildTaskListQueryIncludesDebounceClause()
	{
		try(CwmsTimeSeriesDAO dao = new CwmsTimeSeriesDAO(new NonPoolingConnectionOwner(), "SWT"))
		{
			String sql = dao.buildTaskListQuery(50, 30);

			assertTrue(sql.contains("a.DATE_TIME_LOADED <= SYSDATE - ?/86400"));
			// Debounce seconds are bound second, after the loading application id.
			assertEquals(2, countOccurrences(sql, '?'), "expected two bind parameters");
			assertTrue(sql.indexOf("LOADING_APPLICATION_ID = ?") < sql.indexOf("DATE_TIME_LOADED"));
		}
	}

	@Test
	void testBuildTaskListQueryIncludesFailTimeClauseWhenRetryEnabled()
	{
		DecodesSettings.instance().retryFailedComputations = true;
		try(CwmsTimeSeriesDAO dao = new CwmsTimeSeriesDAO(new NonPoolingConnectionOwner(), "SWT"))
		{
			String sql = dao.buildTaskListQuery(100, 0);

			assertTrue(sql.contains("a.FAIL_TIME is null OR SYSDATE - a.FAIL_TIME >= 1/24"));
			// The retry clause carries no bind parameter of its own.
			assertEquals(1, countOccurrences(sql, '?'));
		}
	}

	@Test
	void testBuildTaskListQueryCombinesRetryAndDebounceClauses()
	{
		DecodesSettings.instance().retryFailedComputations = true;
		try(CwmsTimeSeriesDAO dao = new CwmsTimeSeriesDAO(new NonPoolingConnectionOwner(), "SWT"))
		{
			String sql = dao.buildTaskListQuery(25, 15);

			assertTrue(sql.contains("FAIL_TIME"));
			assertTrue(sql.contains("DATE_TIME_LOADED"));
			assertEquals(2, countOccurrences(sql, '?'), "expected two bind parameters");
			assertTrue(sql.endsWith(" ORDER BY a.site_datatype_id, a.start_date_time"));
		}
	}

	/**
	 * The tsid cache is static, so a test that seeds it has to leave it empty for the next one.
	 */
	@AfterEach
	void clearTsidCache()
	{
		CwmsTimeSeriesDAO.cache.clear();
	}

	@Test
	void testReadTasklistRecsMapsColumnsToRecords() throws SQLException
	{
		ResultSet rs = mock(ResultSet.class);
		when(rs.next()).thenReturn(true, true, false);
		when(rs.getDate(4)).thenReturn(sqlDate(2015, Calendar.JANUARY, 1), sqlDate(2015, Calendar.JANUARY, 2));
		when(rs.getInt(1)).thenReturn(11, 12);
		when(rs.getLong(2)).thenReturn(101L, 102L);
		when(rs.getDouble(3)).thenReturn(1.5, 2.5);
		when(rs.wasNull()).thenReturn(false);
		when(rs.getString(5)).thenReturn("N", "Y");
		when(rs.getString(6)).thenReturn("ft", "m");
		when(rs.getBigDecimal(8)).thenReturn(BigDecimal.valueOf(3), (BigDecimal) null);

		try(CwmsTimeSeriesDAO dao = new CwmsTimeSeriesDAO(new NonPoolingConnectionOwner(), "SWT"))
		{
			List<TasklistRec> recs = dao.readTasklistRecs(rs);

			assertEquals(2, recs.size());
			TasklistRec first = recs.get(0);
			assertEquals(11, first.getRecordNum());
			assertEquals(101L, first.getSdi().getValue());
			assertEquals(1.5, first.getValue());
			assertFalse(first.isValueWasNull());
			assertFalse(first.isDeleted(), "'N' delete flag is not a deletion");
			assertEquals("ft", first.getUnitsAbbr());
			assertEquals(3L, first.getQualityCode());

			TasklistRec second = recs.get(1);
			assertEquals(12, second.getRecordNum());
			assertTrue(second.isDeleted(), "'Y' delete flag marks the row deleted");
			assertEquals("m", second.getUnitsAbbr());
			assertEquals(0L, second.getQualityCode(), "a null quality code reads as 0");
		}
	}

	@Test
	void testReadTasklistRecsReportsNullValues() throws SQLException
	{
		ResultSet rs = mock(ResultSet.class);
		when(rs.next()).thenReturn(true, false);
		when(rs.getDate(4)).thenReturn(sqlDate(2015, Calendar.JANUARY, 1));
		when(rs.getInt(1)).thenReturn(11);
		when(rs.getLong(2)).thenReturn(101L);
		when(rs.getDouble(3)).thenReturn(0.0);
		// First wasNull() answers the site datatype id, the second answers the value.
		when(rs.wasNull()).thenReturn(false, true);
		when(rs.getString(5)).thenReturn("N");
		when(rs.getString(6)).thenReturn("ft");
		when(rs.getBigDecimal(8)).thenReturn(null);

		try(CwmsTimeSeriesDAO dao = new CwmsTimeSeriesDAO(new NonPoolingConnectionOwner(), "SWT"))
		{
			List<TasklistRec> recs = dao.readTasklistRecs(rs);

			assertEquals(1, recs.size());
			assertTrue(recs.get(0).isValueWasNull());
		}
	}

	@Test
	void testReadTasklistRecsStopsAtMaxTimeGap() throws SQLException
	{
		ResultSet rs = mock(ResultSet.class);
		// The second row is 8 days past the first, beyond the 7 day default gap.
		when(rs.next()).thenReturn(true, true, false);
		when(rs.getDate(4)).thenReturn(sqlDate(2015, Calendar.JANUARY, 1), sqlDate(2015, Calendar.JANUARY, 9));
		when(rs.getInt(1)).thenReturn(11, 12);
		when(rs.getLong(2)).thenReturn(101L, 102L);
		when(rs.getDouble(3)).thenReturn(1.5, 2.5);
		when(rs.wasNull()).thenReturn(false);
		when(rs.getString(5)).thenReturn("N", "N");
		when(rs.getString(6)).thenReturn("ft", "ft");
		when(rs.getBigDecimal(8)).thenReturn(null, (BigDecimal) null);

		try(CwmsTimeSeriesDAO dao = new CwmsTimeSeriesDAO(new NonPoolingConnectionOwner(), "SWT"))
		{
			List<TasklistRec> recs = dao.readTasklistRecs(rs);

			assertEquals(1, recs.size(), "reading stops at the first row beyond the max time gap");
			assertEquals(11, recs.get(0).getRecordNum());
		}
	}

	@Test
	void testReadTasklistRecsConsumesTsCodeChangeForCachedTsid() throws SQLException
	{
		CwmsTsId tsid = new CwmsTsId();
		tsid.setUniqueString("TESTSITE.Stage.Inst.15Minutes.0.raw");
		tsid.setKey(DbKey.createDbKey(101L));
		CwmsTimeSeriesDAO.cache.put(tsid);

		ResultSet rs = mock(ResultSet.class);
		when(rs.next()).thenReturn(true, false);
		when(rs.getDate(4)).thenReturn(sqlDate(2015, Calendar.JANUARY, 1));
		when(rs.getInt(1)).thenReturn(11);
		when(rs.getLong(2)).thenReturn(101L);
		when(rs.getDouble(3)).thenReturn(0.0);
		when(rs.wasNull()).thenReturn(false);
		when(rs.getString(5)).thenReturn("U");
		// Column 9 carries the new ts code for a TsCodeChanged notification.
		when(rs.getLong(9)).thenReturn(999L);

		try(CwmsTimeSeriesDAO dao = new CwmsTimeSeriesDAO(new NonPoolingConnectionOwner(), "SWT"))
		{
			List<TasklistRec> recs = dao.readTasklistRecs(rs);

			assertTrue(recs.isEmpty(), "a consumed TsCodeChanged row is not returned as data");
			assertNull(CwmsTimeSeriesDAO.cache.getByKey(DbKey.createDbKey(101L)), "old key is removed");
			assertSame(tsid, CwmsTimeSeriesDAO.cache.getByKey(DbKey.createDbKey(999L)), "tsid is re-keyed");
		}
	}

	@Test
	void testReadTasklistRecsKeepsTsCodeChangeRowWhenTsidNotCached() throws SQLException
	{
		ResultSet rs = mock(ResultSet.class);
		when(rs.next()).thenReturn(true, false);
		when(rs.getDate(4)).thenReturn(sqlDate(2015, Calendar.JANUARY, 1));
		when(rs.getInt(1)).thenReturn(11);
		when(rs.getLong(2)).thenReturn(101L);
		when(rs.getDouble(3)).thenReturn(1.5);
		when(rs.wasNull()).thenReturn(false);
		when(rs.getString(5)).thenReturn("U");
		when(rs.getString(6)).thenReturn("ft");
		when(rs.getBigDecimal(8)).thenReturn(null);

		try(CwmsTimeSeriesDAO dao = new CwmsTimeSeriesDAO(new NonPoolingConnectionOwner(), "SWT"))
		{
			List<TasklistRec> recs = dao.readTasklistRecs(rs);

			assertEquals(1, recs.size(), "an uncached tsid leaves the row to be handled as data");
			assertFalse(recs.get(0).isDeleted());
		}
	}

	@Test
	void testApplyTsCodeChangeReturnsFalseWhenTsidNotCached() throws SQLException
	{
		ResultSet rs = mock(ResultSet.class);

		try(CwmsTimeSeriesDAO dao = new CwmsTimeSeriesDAO(new NonPoolingConnectionOwner(), "SWT"))
		{
			assertFalse(dao.applyTsCodeChange(rs, DbKey.createDbKey(4242L)));
		}
	}

	@Test
	void testCollectTasklistRecsAttachesTasklistHandle() throws Exception
	{
		DataCollection dataCollection = new DataCollection();

		try(CwmsTimeSeriesDAO dao = new CwmsTimeSeriesDAO(new NonPoolingConnectionOwner(), "SWT"))
		{
			dao.collectTasklistRecs(Collections.emptyList(), dataCollection, DbKey.createDbKey(7L));
		}

		assertNotNull(dataCollection.getTasklistHandle(), "callers need the handle to release records");
	}

	@Test
	void testLogReturnedTimeSeriesHandlesEmptyCollection()
	{
		try(CwmsTimeSeriesDAO dao = new CwmsTimeSeriesDAO(new NonPoolingConnectionOwner(), "SWT"))
		{
			assertDoesNotThrow(() -> dao.logReturnedTimeSeries(new DataCollection()));
		}
	}

	@Test
	void testGetNewDataBindsApplicationIdAndReturnsCollection() throws Exception
	{
		ResultSet rs = mock(ResultSet.class);
		when(rs.next()).thenReturn(false);
		PreparedStatement stmt = mock(PreparedStatement.class);
		when(stmt.executeQuery()).thenReturn(rs);
		Connection conn = mock(Connection.class);
		when(conn.prepareStatement(anyString())).thenReturn(stmt);

		try(CwmsTimeSeriesDAO dao = new CwmsTimeSeriesDAO(ownerFor(conn), "SWT"))
		{
			DataCollection collection = dao.getNewData(DbKey.createDbKey(7L), 100, 0);

			assertNotNull(collection.getTasklistHandle());
			verify(stmt).setLong(1, 7L);
			verify(stmt, never()).setInt(eq(2), anyInt());
		}
	}

	@Test
	void testGetNewDataBindsDebounceSeconds() throws Exception
	{
		ResultSet rs = mock(ResultSet.class);
		when(rs.next()).thenReturn(false);
		PreparedStatement stmt = mock(PreparedStatement.class);
		when(stmt.executeQuery()).thenReturn(rs);
		Connection conn = mock(Connection.class);
		when(conn.prepareStatement(anyString())).thenReturn(stmt);

		try(CwmsTimeSeriesDAO dao = new CwmsTimeSeriesDAO(ownerFor(conn), "SWT"))
		{
			dao.getNewData(DbKey.createDbKey(7L), 100, 30);

			verify(stmt).setLong(1, 7L);
			verify(stmt).setInt(2, 30);
		}
	}

	@Test
	void testGetNewDataWrapsSqlException() throws Exception
	{
		PreparedStatement stmt = mock(PreparedStatement.class);
		when(stmt.executeQuery()).thenThrow(new SQLException("tasklist unavailable"));
		Connection conn = mock(Connection.class);
		when(conn.prepareStatement(anyString())).thenReturn(stmt);

		try(CwmsTimeSeriesDAO dao = new CwmsTimeSeriesDAO(ownerFor(conn), "SWT"))
		{
			DbIoException ex = assertThrows(DbIoException.class,
					() -> dao.getNewData(DbKey.createDbKey(7L), 100, 0));

			assertTrue(ex.getMessage().contains("tasklist unavailable"));
		}
	}

	/**
	 * {@link CwmsTimeSeriesDAO#getConnection()} hands the connection to the site and datatype DAOs,
	 * which the bare fixture returns as null.
	 */
	private static NonPoolingConnectionOwner ownerFor(Connection conn)
	{
		NonPoolingConnectionOwner owner = new NonPoolingConnectionOwner()
		{
			@Override
			public SiteDAI makeSiteDAO()
			{
				return mock(SiteDAI.class);
			}

			@Override
			public DataTypeDAI makeDataTypeDAO()
			{
				return mock(DataTypeDAI.class);
			}
		};
		owner.setConnection(conn);
		return owner;
	}

	private static java.sql.Date sqlDate(int year, int month, int day)
	{
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		cal.clear();
		cal.set(year, month, day);
		return new java.sql.Date(cal.getTimeInMillis());
	}

	private static int countOccurrences(String s, char c)
	{
		int count = 0;
		for(int i = 0; i < s.length(); i++)
		{
			if(s.charAt(i) == c)
			{
				count++;
			}
		}
		return count;
	}
}
