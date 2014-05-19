package opendcs.dao;

import ilex.util.Logger;

import java.util.HashSet;
import java.util.Iterator;

import opendcs.dai.AlgorithmDAI;
import opendcs.dai.CompDependsDAI;
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
		q = "delete from tsdb_group_member_ts where data_id = "+key;
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
}
