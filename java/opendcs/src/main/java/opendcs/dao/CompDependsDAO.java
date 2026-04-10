/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
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
package opendcs.dao;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import opendcs.dai.AlgorithmDAI;
import opendcs.dai.CompDependsDAI;
import opendcs.dai.CompDependsNotifyDAI;
import opendcs.dai.TimeSeriesDAI;
import opendcs.dai.TsGroupDAI;
import opendcs.util.functional.ThrowingConsumer;
import opendcs.util.sql.WrappedConnection;
import decodes.sql.DbKey;
import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.CpCompDependsRecord;
import decodes.tsdb.CpDependsNotify;
import decodes.tsdb.DbAlgoParm;
import decodes.tsdb.DbCompAlgorithm;
import decodes.tsdb.DbCompParm;
import decodes.tsdb.DbComputation;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsGroup;
import decodes.tsdb.TsdbDatabaseVersion;

public class CompDependsDAO extends DaoBase implements CompDependsDAI
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();

	private String cpCompDepends_col1 = null;
	protected AlgorithmDAI algorithmDAO = null;
	protected TsGroupDAI tsGroupDAO = null;


	public CompDependsDAO(DatabaseConnectionOwner tsdb)
	{
		super(tsdb, "CompDependsDAO");
		cpCompDepends_col1 = tsdb.isHdb()
			|| tsdb.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_9 ?
			"TS_ID" : "SITE_DATATYPE_ID";
		algorithmDAO = tsdb.makeAlgorithmDAO();
		tsGroupDAO = db.makeTsGroupDAO();
	}

	@Override
	public Connection getConnection()
	{
		// Overriding getConnection allows lazy connection setting and
		// ensures subordinate DAOs will use same conn as this object.
		if (myCon == null)
		{
			super.getConnection();
			algorithmDAO.setManualConnection(myCon);
			tsGroupDAO.setManualConnection(myCon);
		}
		return new WrappedConnection(myCon, c -> {});
	}

	@Override
	public void removeTsDependencies(TimeSeriesIdentifier tsid)
		throws DbIoException
	{
		final DbKey key = tsid.getKey();
		try
		{
			doModify("delete from cp_comp_depends where " + cpCompDepends_col1 +" = ?", key);
			doModify(
				"update cp_computation set enabled = 'N' "
				+ "where computation_id in ("
				+ 	"select distinct a.computation_id from "
				+	 "cp_computation a, cp_comp_ts_parm b "
				+ 	"where a.computation_id = b.computation_id "
				+ 	"and a.enabled = 'Y' "
				+ 	"and b.site_datatype_id = ?"
				+ ")",
				key
			);
			doModify(
				"delete from tsdb_group_member_ts where "
				+ (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_9 ? "ts_id" : "data_id")
				+ " = ?",
				key
			);
		}
		catch (Exception ex)
		{
			throw new DbIoException("failed to remove computations dependencies", ex);
		}
	}

	@Override
	public void writeCompDepends( DbComputation comp )
		throws DbIoException
	{
		try
		{
			DbKey compId = comp.getId();
			log.trace("writeCompDepends({}), id={}", comp.getName(), compId);

			deleteCompDependsForCompId(compId);
			if (!comp.isEnabled())
				return;

			DbCompAlgorithm algo = comp.getAlgorithm();
			if (algo == null)
			{
				String algoName = comp.getAlgorithmName();
				if (algoName != null)
					algo = algorithmDAO.getAlgorithm(algoName);
			}

			// If the computation has a group input, get the group and
			// expand it.
			TsGroup tsGroup = null;
			if (!comp.getGroupId().isNull())
			{
				tsGroup = tsGroupDAO.getTsGroupById(comp.getGroupId());
				if (tsGroup != null && !tsGroup.getIsExpanded())
					db.expandTsGroup(tsGroup);
			}

			// Make a HashSet of all possible ts_id's that can be input to this computation.
			HashSet<DbKey> dataIds = new HashSet<DbKey>();
			for(Iterator<DbCompParm> dcpit = comp.getParms(); dcpit.hasNext(); )
			{
				DbCompParm dcp = dcpit.next();
				String parmType = dcp.getAlgoParmType();
				if (parmType == null && algo != null)
				{
					DbAlgoParm algoParm = algo.getParm(dcp.getRoleName());
					if (algoParm != null)
						parmType = algoParm.getParmType();
				}
				if (parmType == null || !parmType.startsWith("i"))
					continue;

				DbKey sdi = dcp.getSiteDataTypeId();
				// If it has an SDI, it means it is fully defined.
				if (sdi != null && !sdi.isNull())
				{
					log.trace("writeCompDepends: explicit sdi={} for role {}", sdi, dcp.getRoleName());
					dataIds.add(sdi);
				}
				// Otherwise it is a partial definition, expand by group
				else if (tsGroup != null)
				{
					for(TimeSeriesIdentifier grpTsid : tsGroup.getExpandedList())
					{
						try
						{
							TimeSeriesIdentifier tsid =
								db.transformTsidByCompParm(grpTsid, dcp, false, false, null);
							if (tsid != null)
								dataIds.add(tsid.getKey());
						}
						catch(NoSuchObjectException ex)
						{
							// No problem -- this potential input doesn't exist. Skip it.
						}
						catch(BadTimeSeriesException ex)
						{
							log.atTrace().setCause(ex).log("bad time series");
							// Invalid tsid. Skip it.
						}
					}
				}
				// Otherwise incomplete definition and no group - skip it.
			}

			log.trace("Total dds for dependencies={}", dataIds.size());

			try (Connection conn = getConnection();
				PreparedStatement insertDepends = conn.prepareStatement(
				"INSERT INTO CP_COMP_DEPENDS(" + cpCompDepends_col1 + ",COMPUTATION_ID) "
					+ "VALUES(?,?)"
				);)
			{ //NOTE: this may need some tuning for batch size,
				// fairly simple to do with an on modulus size = 0 executeBatch statement
				for(DbKey dataId : dataIds)
				{
					insertDepends.setLong(1, dataId.getValue() );
					insertDepends.setLong(2, compId.getValue() );
					insertDepends.addBatch();
				}
				insertDepends.executeBatch();
			}
		}
		catch(Exception ex)
		{
			String msg = "Exception populating CP_COMP_DEPENDS table.";
			throw new DbIoException(msg, ex);
		}
	}

	@Override
	public void deleteCompDependsForTsKey(DbKey timeSeriesKey)
		throws DbIoException
	{
		if (db.isHdb())
		{
			return;
		}
		try
		{
			doModify("delete from cp_comp_depends where " + cpCompDepends_col1 + " = ?", timeSeriesKey);
			doModify("delete from cp_comp_depends_scratchpad where " + cpCompDepends_col1 + " = ?", timeSeriesKey);
		}
		catch (Exception ex)
		{
			throw new DbIoException("Unable to remove entries for time series.", ex);
		}
	}

	@Override
	public void deleteCompDependsForCompId(DbKey compId)
		throws DbIoException
	{
		if (db.isHdb())
		{
			return;
		}
		// Remove the CP_COMP_DEPENDS records & re-add.
		try
		{
			doModify("delete from cp_comp_depends where computation_id = ?", compId);
			doModify("delete from cp_comp_depends_scratchpad where COMPUTATION_ID = ?", compId);
		}
		catch (Exception ex)
		{
			throw new DbIoException("Unable to remove entries for computation.", ex);
		}
	}

	public void close()
	{
		super.close();
		algorithmDAO.close();
		tsGroupDAO.close();
	}

	@Override
	public ArrayList<TimeSeriesIdentifier> getTriggersFor(DbKey compID)
		throws DbIoException
	{
		try(Connection conn = getConnection();
			TimeSeriesDAI timeSeriesDAO = db.makeTimeSeriesDAO();
			DaoBase dao = new DaoBase(this.db,"triggersFor",conn);)
		{
			timeSeriesDAO.setManualConnection(conn);
			return (ArrayList<TimeSeriesIdentifier>)dao.getResults(
						"SELECT " + cpCompDepends_col1 + " FROM CP_COMP_DEPENDS "
					  + "WHERE COMPUTATION_ID = ?", rs ->
					{
						DbKey tsKey = DbKey.createDbKey(rs, 1);
						try
						{
							return timeSeriesDAO.getTimeSeriesIdentifier(tsKey);
						}
						catch (NoSuchObjectException ex)
						{
							log.atWarn()
							   .setCause(ex)
							   .log("Bogus Time Series Key {} in CP_COMP_DEPENDS for computation {}",
							   		tsKey, compID);
						}
						catch (DbIoException ex)
						{
							log.atError()
							   .setCause(ex)
							   .log("Database error retrieving identifier.");
						}
						return null;
					}, compID);
		}
		catch (SQLException ex)
		{
			throw new DbIoException("Error executing trigger comp retrieval.", ex);
		}
	}

	// NOTE: This is only used by a rarely used command line application to run computation. Thus is on the low
	// priority list of things to apply bind vars to; however, if you think you can do so by all means do.
	@Override
	public ArrayList<DbKey> getCompIdsFor(Collection<TimeSeriesIdentifier> tsids, DbKey appId)
		throws DbIoException
	{
		ArrayList<DbKey> ret = new ArrayList<DbKey>();

		StringBuilder inClause = new StringBuilder(" in (");
		int n = 0;
		for (TimeSeriesIdentifier tsid : tsids)
		{
			if (n++ > 0)
			{
				inClause.append(", ");
			}
			inClause.append(tsid.getKey().toString());
		}
		inClause.append(")");
		if (n == 0)
		{
			return ret;
		}
		ArrayList<Object> parameters = new ArrayList<>();
		String q = "select a.computation_id from cp_comp_depends a, cp_computation b"
			     + " where a.computation_id = b.computation_id"
				 + " and a." + cpCompDepends_col1 + inClause.toString();

		if (!DbKey.isNull(appId))
		{
			q = q + " and b.loading_application_id = ?";
			parameters.add(appId);
		}

		try
		{
			// While using bind for the in-list is what's really required, we
			// at least have a slight improvement no leaving a ResultSet open.
			doQuery(q, rs ->
			{
				ret.add(DbKey.createDbKey(rs, 1));
			}, parameters.toArray(new Object[0]));
		}
		catch(Exception ex)
		{
			log.atWarn()
			   .setCause(ex)
			   .log("getCompIdsFor() error in query '{}'", q );
		}
		return ret;
	}

	@Override
	public CpDependsNotify getCpCompDependsNotify()
	{
		try(CompDependsNotifyDAI dai = db.makeCompDependsNotifyDAO())
		{
			return dai.getNextRecord();
		}
		catch(DbIoException ex)
		{
			log.atWarn().setCause(ex).log("Unable to get next record.");
		}
		return null;
	}

	@Override
	public void clearScratchpad() throws DbIoException
	{
		String q = "DELETE FROM CP_COMP_DEPENDS_SCRATCHPAD";
		try
		{
			doModify(q,new Object[0]);
		}
		catch (SQLException ex)
		{
			throw new DbIoException("Unable to clear scratch pad.", ex);
		}
	}

	@Override
	public List<CpCompDependsRecord> getAllCompDependsEntries() throws DbIoException
	{
		String q = "SELECT TS_ID, COMPUTATION_ID FROM CP_COMP_DEPENDS";
		try
		{
			return getResults(q, rs ->
			{
				return new CpCompDependsRecord(DbKey.createDbKey(rs, 1), DbKey.createDbKey(rs, 2));
			});
		}
		catch (SQLException ex)
		{
			throw new DbIoException("Unable to retrieve comp depends records.", ex);
		}
	}

	@Override
	public void addRecords(Collection<CpCompDependsRecord> records) throws DbIoException
	{
		String q = "insert into cp_comp_depends(ts_id, computation_id) values(?,?)"; //+ ccd.getTsKey() + ", " + ccd.getCompId() + ")";
		try
		{
			withStatement(q, stmt ->
			{
				for(CpCompDependsRecord ccd : records)
				{
					stmt.setLong(1, ccd.getTsKey().getValue());
					stmt.setLong(2,ccd.getCompId().getValue());
					stmt.addBatch();
				}
				stmt.executeBatch();
			});
		}
		catch (SQLException ex)
		{
			throw new DbIoException("Unable to save comp depends records.", ex);
		}
	}

	@Override
	public void addRecordsToScratchPad(Collection<CpCompDependsRecord> records) throws DbIoException
	{
		String q = "insert into cp_comp_depends_scratchpad(ts_id, computation_id) values(?,?)";
		try
		{
			withStatement(q, stmt ->
			{
				for(CpCompDependsRecord ccd : records)
				{
					stmt.setLong(1, ccd.getTsKey().getValue());
					stmt.setLong(2, ccd.getCompId().getValue());
					stmt.addBatch();
				}
				stmt.executeBatch();
			});
		}
		catch (SQLException ex)
		{
			throw new DbIoException("Unable to save comp depends records.", ex);
		}
	}

	@Override
	public void mergeScratchPadToActive() throws DbIoException
	{
		try
		{
			// The scratchpad is now what we want CP_COMP_DEPENDS to look like.
			// Mark's 2-line SQL to move the scratchpad to CP_COMP_DEPENDS.
			String q = "delete from cp_comp_depends "
				+ "where(computation_id, ts_id) in "
				+ "(select computation_id, ts_id from cp_comp_depends " +
					(db.isOracle() ? "minus" : "except")  + " select computation_id, ts_id from cp_comp_depends_scratchpad)";
			doModify(q, new Object[0]);

			q = "insert into cp_comp_depends( computation_id, ts_id) "
				+ "(select computation_id, ts_id from cp_comp_depends_scratchpad "
				+ (db.isOracle() ? "minus" : "except") + " select computation_id, ts_id from cp_comp_depends)";
			doModify(q, new Object[0]);
		}
		catch (Exception ex)
		{
			throw new DbIoException("Unable to transfer dependency information from scratch pad to active table.", ex);
		}
	}

	@Override
	public void transaction(ThrowingConsumer<CompDependsDAI,DbIoException> consumer) throws DbIoException
	{
		try
		{
			inTransaction(dao ->
			{
				try (CompDependsDAI dai = db.makeCompDependsDAO();)
				{
					dai.inTransactionOf(dao);
					consumer.accept(dai);
				}
			});
		}
		catch (Exception ex)
		{
			throw new DbIoException("Unable to finish transaction operations.", ex);
		}
	}

	@Override
	public void removeExistingFromScratch() throws DbIoException
	{
		try
		{
			doModify("delete from cp_comp_depends_scratchpad sp "
				+ "where exists(select * from cp_comp_depends cd where cd.computation_id = sp.computation_id "
				+ "and cd.ts_id = sp.ts_id)", new Object[0]);
		}
		catch (SQLException ex)
		{
			throw new DbIoException("Unable to remove existing records from scratch pad.", ex);
		}
	}

	@Override
	public void fillActiveFromScratch() throws DbIoException
	{
		try
		{
			doModify("INSERT INTO CP_COMP_DEPENDS SELECT * FROM CP_COMP_DEPENDS_SCRATCHPAD", new Object[0]);
		}
		catch (SQLException ex)
		{
			throw new DbIoException("Unable to remove existing records from scratch pad.", ex);
		}
	}

}