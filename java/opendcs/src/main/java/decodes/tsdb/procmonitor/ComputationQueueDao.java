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

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;
import opendcs.dao.DaoBase;
import opendcs.dao.DatabaseConnectionOwner;

/**
 * Reads and clears the CWMS computation task-list queue.
 */
public class ComputationQueueDao extends DaoBase
{
	private static final String APP_COUNTS_QUERY =
		"select tl.loading_application_id, count(*) "
		+ "from cp_comp_tasklist tl "
		+ "join hdb_loading_application la "
		+ "on la.loading_application_id = tl.loading_application_id "
		+ "group by tl.loading_application_id";

	private static final String TS_COUNTS_QUERY =
		"select tl.site_datatype_id, tsi.cwms_ts_id, count(*) "
		+ "from cp_comp_tasklist tl "
		+ "join hdb_loading_application la "
		+ "on la.loading_application_id = tl.loading_application_id "
		+ "left join cwms_v_ts_id tsi on tsi.ts_code = tl.site_datatype_id "
		+ "where tl.loading_application_id = ? "
		+ "group by tl.site_datatype_id, tsi.cwms_ts_id "
		+ "order by count(*) desc";

	public ComputationQueueDao(DatabaseConnectionOwner db)
	{
		super(db, "ComputationQueueDao");
	}

	public Map<DbKey, Long> getQueueCounts() throws DbIoException
	{
		Map<DbKey, Long> counts = new LinkedHashMap<DbKey, Long>();
		try
		{
			doQuery(APP_COUNTS_QUERY, rs ->
				counts.put(DbKey.createDbKey(rs, 1), rs.getLong(2)));
			return counts;
		}
		catch (SQLException ex)
		{
			throw new DbIoException("Unable to read computation queue counts.", ex);
		}
	}

	public List<QueueDetail> getQueueDetails(DbKey applicationId) throws DbIoException
	{
		try
		{
			return getResults(TS_COUNTS_QUERY, rs -> new QueueDetail(
				DbKey.createDbKey(rs, 1), rs.getString(2), rs.getLong(3)),
				applicationId);
		}
		catch (SQLException ex)
		{
			throw new DbIoException("Unable to read computation queue details.", ex);
		}
	}

	public int clearQueue(DbKey applicationId) throws DbIoException
	{
		try
		{
			return doModify(
				"delete from cp_comp_tasklist "
				+ "where loading_application_id = ? "
				+ "and exists (select 1 from hdb_loading_application la "
				+ "where la.loading_application_id = ?)",
				applicationId, applicationId);
		}
		catch (SQLException ex)
		{
			throw new DbIoException("Unable to clear computation queue.", ex);
		}
	}

	public static class QueueDetail
	{
		private final DbKey timeSeriesCode;
		private final String timeSeriesId;
		private final long queueCount;

		QueueDetail(DbKey timeSeriesCode, String timeSeriesId, long queueCount)
		{
			this.timeSeriesCode = timeSeriesCode;
			this.timeSeriesId = timeSeriesId;
			this.queueCount = queueCount;
		}

		public DbKey getTimeSeriesCode()
		{
			return timeSeriesCode;
		}

		public String getTimeSeriesId()
		{
			return timeSeriesId;
		}

		public long getQueueCount()
		{
			return queueCount;
		}
	}
}
