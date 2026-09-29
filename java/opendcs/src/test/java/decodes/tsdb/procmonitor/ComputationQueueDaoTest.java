/*
* Where Applicable, Copyright 2026 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package decodes.tsdb.procmonitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import decodes.sql.DbKey;
import fixtures.NonPoolingConnectionOwner;
import fixtures.TestConnectionOwner;
import opendcs.dao.DaoBase;

class ComputationQueueDaoTest
{
	private TestConnectionOwner dbOwner = new NonPoolingConnectionOwner();

	@BeforeAll
	static void loadDriver() throws Exception
	{
		Class.forName("org.apache.derby.jdbc.EmbeddedDriver").newInstance();
	}

	@BeforeEach
	void createDatabase() throws Exception
	{
		Connection connection =
			DriverManager.getConnection("jdbc:derby:memory:queueDb;create=true");
		dbOwner.setConnection(connection);
		try (DaoBase dao = new DaoBase(dbOwner, "test"))
		{
			dao.doModify("create table test_session_office "
				+ "(db_office_code integer not null)");
			dao.doModify("insert into test_session_office values (?)", 1);
			dao.doModify("create table all_loading_applications "
				+ "(loading_application_id bigint primary key, "
				+ "loading_application_name varchar(24), db_office_code integer not null)");
			dao.doModify("create view hdb_loading_application as "
				+ "select la.loading_application_id "
				+ "from all_loading_applications la, test_session_office ctx "
				+ "where la.db_office_code = ctx.db_office_code");
			dao.doModify("create table cp_comp_tasklist "
				+ "(record_num integer primary key, loading_application_id bigint, "
				+ "site_datatype_id bigint)");
			dao.doModify("create table cwms_v_ts_id "
				+ "(ts_code bigint primary key, cwms_ts_id varchar(128))");

			dao.doModify("insert into all_loading_applications values (?, ?, ?)",
				100L, "SharedProcess", 1);
			dao.doModify("insert into all_loading_applications values (?, ?, ?)",
				200L, "OfficeOneProcess", 1);
			dao.doModify("insert into all_loading_applications values (?, ?, ?)",
				1100L, "SharedProcess", 2);
			dao.doModify("insert into cwms_v_ts_id values (?, ?)", 10L, "A.Stage.Inst.0.0.Raw");
			dao.doModify("insert into cwms_v_ts_id values (?, ?)", 20L, "B.Flow.Inst.0.0.Raw");

			dao.doModify("insert into cp_comp_tasklist values (?, ?, ?)", 1, 100L, 10L);
			dao.doModify("insert into cp_comp_tasklist values (?, ?, ?)", 2, 100L, 10L);
			dao.doModify("insert into cp_comp_tasklist values (?, ?, ?)", 3, 100L, 11L);
			dao.doModify("insert into cp_comp_tasklist values (?, ?, ?)", 4, 200L, 10L);
			dao.doModify("insert into cp_comp_tasklist values (?, ?, ?)", 5, 1100L, 20L);
			dao.doModify("insert into cp_comp_tasklist values (?, ?, ?)", 6, 1100L, 20L);
		}
	}

	@AfterEach
	void dropDatabase() throws Exception
	{
		try
		{
			DriverManager.getConnection("jdbc:derby:memory:queueDb;drop=true");
		}
		catch (SQLException ex)
		{
			if ("08006".equals(ex.getSQLState()))
				dbOwner.setConnection(null);
			else
				throw ex;
		}
	}

	@Test
	void readsDetailsAndClearsOnlySelectedApplication() throws Exception
	{
		DbKey app100 = DbKey.createDbKey(100L);
		DbKey app200 = DbKey.createDbKey(200L);
		try (ComputationQueueDao dao = new ComputationQueueDao(dbOwner))
		{
			Map<DbKey, Long> counts = dao.getQueueCounts();
			assertEquals(Long.valueOf(3L), counts.get(app100));
			assertEquals(Long.valueOf(1L), counts.get(app200));

			List<ComputationQueueDao.QueueDetail> details = dao.getQueueDetails(app100);
			assertEquals(2, details.size());
			assertEquals(2L, details.get(0).getQueueCount());
			assertEquals("A.Stage.Inst.0.0.Raw", details.get(0).getTimeSeriesId());
			assertEquals(1L, details.get(1).getQueueCount());
			assertNull(details.get(1).getTimeSeriesId());

			assertEquals(3, dao.clearQueue(app100));
			counts = dao.getQueueCounts();
			assertNull(counts.get(app100));
			assertEquals(Long.valueOf(1L), counts.get(app200));
		}
	}

	@Test
	void isolatesQueueOperationsBySessionOffice() throws Exception
	{
		DbKey officeOneApp = DbKey.createDbKey(100L);
		DbKey officeTwoApp = DbKey.createDbKey(1100L);
		try (ComputationQueueDao dao = new ComputationQueueDao(dbOwner))
		{
			Map<DbKey, Long> officeOneCounts = dao.getQueueCounts();
			assertEquals(2, officeOneCounts.size());
			assertEquals(Long.valueOf(3L), officeOneCounts.get(officeOneApp));
			assertNull(officeOneCounts.get(officeTwoApp));
			assertTrue(dao.getQueueDetails(officeTwoApp).isEmpty());
			assertEquals(0, dao.clearQueue(officeTwoApp));

			setSessionOffice(2);

			Map<DbKey, Long> officeTwoCounts = dao.getQueueCounts();
			assertEquals(1, officeTwoCounts.size());
			assertNull(officeTwoCounts.get(officeOneApp));
			assertEquals(Long.valueOf(2L), officeTwoCounts.get(officeTwoApp));

			List<ComputationQueueDao.QueueDetail> details =
				dao.getQueueDetails(officeTwoApp);
			assertEquals(1, details.size());
			assertEquals("B.Flow.Inst.0.0.Raw", details.get(0).getTimeSeriesId());
			assertEquals(2L, details.get(0).getQueueCount());
			assertEquals(0, dao.clearQueue(officeOneApp));
			assertEquals(2, dao.clearQueue(officeTwoApp));

			setSessionOffice(1);

			Map<DbKey, Long> preservedCounts = dao.getQueueCounts();
			assertEquals(Long.valueOf(3L), preservedCounts.get(officeOneApp));
			assertEquals(Long.valueOf(1L),
				preservedCounts.get(DbKey.createDbKey(200L)));
		}
	}

	private void setSessionOffice(int officeCode) throws Exception
	{
		try (DaoBase dao = new DaoBase(dbOwner, "test"))
		{
			dao.doModify("update test_session_office set db_office_code = ?",
				officeCode);
		}
	}

	@Test
	void queueColumnIsOnlyPresentForQueueMonitoring()
	{
		ProcStatTableModel standardModel = new ProcStatTableModel(null, false);
		assertEquals(8, standardModel.getColumnCount());
		assertEquals(Boolean.class, standardModel.getColumnClass(7));

		ProcStatTableModel queueModel = new ProcStatTableModel(null, true);
		assertEquals(9, queueModel.getColumnCount());
		assertEquals(Long.class, queueModel.getColumnClass(7));
		assertEquals(Boolean.class, queueModel.getColumnClass(8));
	}
}
