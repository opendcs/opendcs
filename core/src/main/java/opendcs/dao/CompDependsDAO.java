/**
 * $Id: CompDependsDAO.java,v 1.7 2020/05/07 13:50:16 mmaloney Exp $
 *
 * $Log: CompDependsDAO.java,v $
 * Revision 1.7  2020/05/07 13:50:16  mmaloney
 * Also delete from scratchpad when deleting dependencies.
 *
 * Revision 1.6  2017/08/22 19:58:40  mmaloney
 * Refactor
 *
 * Revision 1.5  2017/07/06 19:06:54  mmaloney
 * New method to support comp exec command.
 *
 * Revision 1.4  2016/12/16 14:31:30  mmaloney
 * Added getTriggersFor method.
 *
 * Revision 1.3  2014/12/19 19:26:57  mmaloney
 * Handle version change for column name tsdb_group_member_ts data_id vs. ts_id.
 *
 * Revision 1.2  2014/07/03 12:53:41  mmaloney
 * debug improvements.
 *
 *
 * This software was written by Cove Software, LLC ("COVE") under contract
 * to the United States Government.
 *
 * No warranty is provided or implied other than specific contractual terms
 * between COVE and the U.S. Government
 *
 * Copyright 2014 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * All rights reserved.
 */
package opendcs.dao;

import ilex.util.Logger;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

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
			debug3("writeCompDepends(" + comp.getName() + ", id=" + compId);

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
					debug3("writeCompDepends: explicit sdi=" + sdi
						+ " for role " + dcp.getRoleName());
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
							// Invalid tsid. Skip it.
						}
					}
				}
				// Otherwise incomplete definition and no group - skip it.
			}

			debug3("Total dds for dependencies=" + dataIds.size());
			Connection conn = getConnection();
			try(
				PreparedStatement insertDepends = conn.prepareStatement(
				"INSERT INTO CP_COMP_DEPENDS(" + cpCompDepends_col1 + ",COMPUTATION_ID) "
					+ "VALUES(?,?)"
				);
			){ //NOTE: this may need some tuning for batch size,
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
			String msg = "Exception populating CP_COMP_DEPENDS table: " + ex;
			warning(msg);
			ex.printStackTrace(Logger.instance().getLogOutput());
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
			throw new DbIoException("Unable to remove entries for time series.",ex);
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
							warning("Bogus Time Series Key " + tsKey + " in CP_COMP_DEPENDS "
								+ "for computation " + compID);
						}
						catch (DbIoException ex)
						{
							failure("Database error retrieving identifier: " + ex.getLocalizedMessage());
						}
						return null;
					}, compID);
		}
		catch (SQLException ex)
		{
			throw new DbIoException("Error executing trigger comp retrieval: " + ex);
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
			warning("getCompIdsFor() error in query '" + q + "': " + ex);
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
			warning("Unable to get next record. " + ex.getLocalizedMessage());
			PrintStream ps = Logger.instance().getLogOutput();
			if (ps != null)
			{
				ex.printStackTrace(ps);
			}
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
			throw new DbIoException("Unable to clear scratch pad.");
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
		String q = "insert into cp_comp_depends_scratchpad(ts_id, computation_id) values(?,?)"; //+ ccd.getTsKey() + ", " + ccd.getCompId() + ")";
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
					// NOTE: we can call getConnection plainly here as
					// we know we are in a transaction and only have the one connection.
					dai.setManualConnection(dao.getConnection());
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
