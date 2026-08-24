package decodes.cwms;

import java.util.Calendar;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import decodes.util.DecodesSettings;
import fixtures.NonPoolingConnectionOwner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
