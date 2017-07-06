/**
 * $Id$
 * 
 * $Log$
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import opendcs.dai.AlgorithmDAI;
import opendcs.dai.CompDependsDAI;
import opendcs.dai.TimeSeriesDAI;
import opendcs.dai.TsGroupDAI;
import decodes.sql.DbKey;
import decodes.tsdb.BadTimeSeriesException;
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
	public void removeTsDependencies(TimeSeriesIdentifier tsid)
		throws DbIoException
	{
		DbKey key = tsid.getKey();
		// Remove any computation dependencies to this time-series.
		doModify("DELETE FROM CP_COMP_DEPENDS WHERE " + cpCompDepends_col1 + " = " 
			+ key);
		
		// Disable any computations that use this time-series as input or
		// output.
		String q = "select distinct a.computation_id from "
			+ "cp_computation a, cp_comp_ts_parm b "
			+ "where a.computation_id = b.computation_id "
			+ "and a.enabled = 'Y' "
			+ "and b.site_datatype_id = " + key;
		String mq = "update cp_computation set enabled = 'N' "
			+ "where computation_id in (" + q + ")";
		doModify(mq);
		
		// If this ts is explicitly included in a group, remove it.
		q = "delete from tsdb_group_member_ts where "
			+ (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_9 ? "ts_id" : "data_id")
			+ " = "+key;
		doModify(q);
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
			for(DbKey dataId : dataIds)
			{
				String q = 
				  "INSERT INTO CP_COMP_DEPENDS(" + cpCompDepends_col1 + ",COMPUTATION_ID) "
				  + "VALUES(" + dataId + ", " + compId + ")";
				doModify(q);
			}
		}
		catch(Exception ex)
		{
			String msg = "Exception populating CP_COMP_DEPENDS table: " + ex;
			warning(msg);
			ex.printStackTrace(Logger.instance().getLogOutput());
			throw new DbIoException(msg);
		}
	}

	@Override
	public void deleteCompDependsForTsKey(DbKey timeSeriesKey)
		throws DbIoException
	{
		if (db.isHdb())
			return;
		
		// Remove the CP_COMP_DEPENDS records & re-add.
		String q = "DELETE FROM CP_COMP_DEPENDS WHERE " + cpCompDepends_col1 + " = "
			+ timeSeriesKey;
		doModify(q);
	}
	
	@Override
	public void deleteCompDependsForCompId(DbKey compId)
		throws DbIoException
	{
		if (db.isHdb())
			return;
		// Remove the CP_COMP_DEPENDS records & re-add.
		String q = "DELETE FROM CP_COMP_DEPENDS WHERE COMPUTATION_ID = "
			+ compId;
		doModify(q);
	}
	
	public void close()
	{
		super.close();
		algorithmDAO.close();
		tsGroupDAO.close();
	}

	@Override
	public ArrayList<TimeSeriesIdentifier> getTriggersFor(DbComputation comp)
		throws DbIoException
	{
		String q = "SELECT " + cpCompDepends_col1 + " FROM CP_COMP_DEPENDS "
			+ "WHERE COMPUTATION_ID = " + comp.getId();
		ResultSet rs = doQuery(q);
		
		TimeSeriesDAI timeSeriesDAO = db.makeTimeSeriesDAO();
		ArrayList<TimeSeriesIdentifier> ret = new ArrayList<TimeSeriesIdentifier>();
		try
		{
			while(rs.next())
			{
				DbKey tsKey = DbKey.createDbKey(rs, 1);
				try
				{
					ret.add(timeSeriesDAO.getTimeSeriesIdentifier(tsKey));
				}
				catch (NoSuchObjectException e)
				{
					warning("Bogus Time Series Key " + tsKey + " in CP_COMP_DEPENDS "
						+ "for computation " + comp.getKey() + ":" + comp.getName());
				}
			}
		}
		catch (SQLException ex)
		{
			throw new DbIoException("Error executing '" + q + "': " + ex);
		}
		finally
		{
			timeSeriesDAO.close();
		}
		return ret;
	}

	@Override
	public ArrayList<DbKey> getCompIdsFor(ArrayList<TimeSeriesIdentifier> tsids, DbKey appId)
		throws DbIoException
	{
		ArrayList<DbKey> ret = new ArrayList<DbKey>();
		
		StringBuilder inClause = new StringBuilder(" in (");
		int n = 0;
		for (TimeSeriesIdentifier tsid : tsids)
		{
			if (n++ > 0)
				inClause.append(", ");
			inClause.append(tsid.getKey().toString());
		}
		inClause.append(")");
		if (n == 0)
			return ret;
		
		String q = "select a.computation_id from cp_comp_depends a, cp_computation b"
			+ " where a.computation_id = b.computation_id"
			+ " and a." + cpCompDepends_col1 + inClause.toString();
		if (!DbKey.isNull(appId))
			q = q + " and b.loading_application_id = " + appId;
			
		try
		{
			ResultSet rs = doQuery(q);
			while (rs.next())
				ret.add(DbKey.createDbKey(rs, 1));
		}
		catch(Exception ex)
		{
			warning("getCompIdsFor() error in query '" + q + "': " + ex);
		}
		return ret;
	}
}
