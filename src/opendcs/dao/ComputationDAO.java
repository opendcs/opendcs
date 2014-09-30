/*
* $Id$
* 
* $Log$
* Revision 1.3  2014/07/03 12:53:42  mmaloney
* debug improvements.
*
* Revision 1.2  2014/05/20 14:41:06  mmaloney
* If comment read was null, change it to empty string. This makes it compatible
* with PG and it prevents compedit from falsely detecting changes.
*
* Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
* OPENDCS 6.0 Initial Checkin
* 
* This software was written by Cove Software, LLC ("COVE") under contract 
* to the United States Government. No warranty is provided or implied other 
* than specific contractual terms between COVE and the U.S. Government.
* 
* Copyright 2014 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
* All rights reserved.
*/
package opendcs.dao;

import ilex.util.Logger;
import ilex.util.TextUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import opendcs.dai.AlgorithmDAI;
import opendcs.dai.CompDependsDAI;
import opendcs.dai.ComputationDAI;
import opendcs.dai.DataTypeDAI;
import opendcs.dai.PropertiesDAI;
import opendcs.dai.TsGroupDAI;

import decodes.db.Constants;
import decodes.db.DataType;
import decodes.sql.DbKey;
import decodes.tsdb.CompFilter;
import decodes.tsdb.ConstraintException;
import decodes.tsdb.DbAlgoParm;
import decodes.tsdb.DbCompAlgorithm;
import decodes.tsdb.DbCompParm;
import decodes.tsdb.DbComputation;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TsdbDatabaseVersion;

/**
Data Access Object for reading/writing computations.
*/
public class ComputationDAO
	extends DaoBase 
	implements ComputationDAI
{
	protected static DbObjectCache<DbComputation> cache = 
		new DbObjectCache<DbComputation>(60 * 60 * 1000L, false);

	protected PropertiesDAI propsDao = null;
	protected AlgorithmDAI algorithmDAO = null;
	protected DataTypeDAI dataTypeDAO = null;
	protected TsGroupDAI tsGroupDAO = null;
	
	/** Defines columns to match the rs2comp method */
	public String compTableColumns = 
		"cp_computation.computation_id, computation_name, "
		+ "cp_computation.algorithm_id, "
		+ "cmmnt, loading_application_id, date_time_loaded, enabled, "
		+ "effective_start_date_time, effective_end_date_time";
	public String compTableColumnsNoTabName =
		"computation_id, computation_name, "
		+ "algorithm_id, "
		+ "cmmnt, loading_application_id, date_time_loaded, enabled, "
		+ "effective_start_date_time, effective_end_date_time";
	
	public ComputationDAO(DatabaseConnectionOwner tsdb)
	{
		super(tsdb, "ComputationDao");
		propsDao = db.makePropertiesDAO();
		algorithmDAO = db.makeAlgorithmDAO();
		dataTypeDAO = db.makeDataTypeDAO();
		tsGroupDAO = db.makeTsGroupDAO();
		if (tsdb.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_6)
		{
			compTableColumns = compTableColumns + ", group_id";
			compTableColumnsNoTabName = compTableColumnsNoTabName + ", group_id";
		}
	}

	@Override
	public DbComputation getComputationById(DbKey compId) throws DbIoException,
		NoSuchObjectException
	{
		DbComputation ret = cache.getByKey(compId, this);
		if (ret != null)
			return ret;
		
		String q = "select " + compTableColumns
			+ " from CP_COMPUTATION where COMPUTATION_ID = "
			+ compId;
	
		try
		{
			ResultSet rs = doQuery(q);
			if (rs.next())
			{
				DbComputation comp = rs2comp(rs);
Logger.instance().debug3("getComputationById: after rs2comp, groupId = " + comp.getGroupId());
				if (!comp.getAlgorithmId().isNull())
				{
					try
					{
						comp.setAlgorithm(algorithmDAO.getAlgorithmById(comp.getAlgorithmId()));
					}
					catch(NoSuchObjectException ex)
					{
						warning("Computation ID=" 
							+ compId + " with algo ID="
							+ comp.getAlgorithmId() + " -- cannot find matching "
							+ "algorithm.");
					}
				}
				ArrayList<DbComputation> ca = new ArrayList<DbComputation>();
				ca.add(comp);
				fillCompSubordinates(ca);

				DbKey appId = comp.getAppId();
				if (!appId.isNull())
				{
					q = "select LOADING_APPLICATION_NAME from "
					  + "HDB_LOADING_APPLICATION where LOADING_APPLICATION_ID = " 
					  + appId;
					rs = doQuery(q);
					if (rs.next())
						comp.setApplicationName(rs.getString(1));
				}
				cache.put(comp);
				return comp;
			}
			else
				throw new NoSuchObjectException("No computation with ID="
					+ compId);
		}
		catch(SQLException ex)
		{
			String msg = "Error reading computation id=" + compId + ": " + ex;
			warning(msg);
			throw new DbIoException(msg);
		}
	}

	/**
	 * Create computation object & fill with current row of result set.
	 */
	protected DbComputation rs2comp(ResultSet rs)
		throws SQLException
	{
		
		DbKey id = DbKey.createDbKey(rs, 1);
		
		String nm = rs.getString(2);
		DbComputation comp = new DbComputation(id, nm);

		comp.setAlgorithmId(DbKey.createDbKey(rs, 3));
		comp.setComment(rs.getString(4));
		if (comp.getComment() == null)
			comp.setComment("");
		comp.setAppId(DbKey.createDbKey(rs, 5));
		comp.setLastModified(db.getFullDate(rs, 6));
		comp.setEnabled(TextUtil.str2boolean(rs.getString(7)));
		comp.setValidStart(db.getFullDate(rs, 8));
		comp.setValidEnd(db.getFullDate(rs, 9));
		if (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_6)
			comp.setGroupId(DbKey.createDbKey(rs, 10));
		return comp;
	}

	/**
	 * Fills in the computation parameters, properties, and groups
	 * @param comps The list of computations to fill.
	 * @throws SQLException
	 * @throws DbIoException
	 */
	protected void fillCompSubordinates(List<DbComputation> comps)
		throws SQLException, DbIoException
	{
		// Strategy is to get 200 at a time with an in clause.
		StringBuilder inClause = new StringBuilder("WHERE COMPUTATION_ID IN (");
		int nInClause = 0;
		
		for(int idx = 0; idx < comps.size(); idx++)
		{
			inClause.append(comps.get(idx).getId().toString() + ",");
			if (++nInClause >= 200 || idx == comps.size() - 1)
			{
				inClause.setLength(inClause.length()-1);
				inClause.append(")");
			}
			else
				continue;
			
			// Get next batch of parm records.
			String q = "select * from CP_COMP_TS_PARM " + inClause.toString();
			ResultSet rs = doQuery(q);
			while(rs.next())
			{
				DbKey compId = DbKey.createDbKey(rs, 1);
				DbComputation comp = null;
				for(DbComputation tc : comps)
					if (compId.equals(tc.getId()))
					{
						comp = tc;
						break;
					}
				if (comp == null)
				{
					// shouldn't happen because of the IN clause above
					continue;
				}
				
				String role = rs.getString(2);
				DbKey sdi = DbKey.createDbKey(rs, 3);
				String intvl = rs.getString(4);
				String tabsel = rs.getString(5);
				int dt = rs.getInt(6);
				int modelId = rs.getInt(7);
				if (rs.wasNull())
					modelId = Constants.undefinedIntKey;
				DbCompParm dcp = new DbCompParm(role, sdi, intvl, tabsel, dt);
				dcp.setModelId(modelId);
				DbAlgoParm dap = null;
				DbCompAlgorithm algo = comp.getAlgorithm();
				if (algo != null)
					dap = algo.getParm(role);
				if (dap != null)
					dcp.setAlgoParmType(dap.getParmType());
				comp.addParm(dcp);
				if (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_5)
				{
					DbKey dtid = DbKey.createDbKey(rs, 8);
					dcp.setDataTypeId(dtid);
					dcp.setDataType(DataType.getDataType(dtid));
					
					/* Legacy: First Tempest implementation had groupd_id in the parm. */
					if (db.getTsdbVersion() < TsdbDatabaseVersion.VERSION_6)
						comp.setGroupId(DbKey.createDbKey(rs, 9));
				}
				if (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_6)
				{
					// V8 gets rid of the GROUP_ID column, so DELTA_T_UNITS is (9)
					dcp.setDeltaTUnits(rs.getString(
						db.getTsdbVersion() < TsdbDatabaseVersion.VERSION_8 ? 10 : 9));
				}
				if (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_9)
					dcp.setSiteId(DbKey.createDbKey(rs, 10));
			}

			propsDao.readPropertiesIntoList("CP_COMP_PROPERTY", comps, inClause.toString());
			
			// Reset in clause to empty.
			inClause.setLength(0);
			inClause.append("WHERE COMPUTATION_ID IN (");
			nInClause = 0;
		}

		// Retrieve groups used by computations (group DAO will cache)
		for(DbComputation comp : comps)
{Logger.instance().debug3("fillCompSubordinates comp '" + comp.getName() + "' id=" +
comp.getKey() + ", groupId=" + comp.getGroupId());
			if (!comp.getGroupId().isNull())
				comp.setGroup(tsGroupDAO.getTsGroupById(comp.getGroupId()));
}		
		// Make sure site IDs and datatype IDs are set in the parms
		for(DbComputation comp : comps)
			for(DbCompParm parm : comp.getParmList())
				if (!parm.getSiteDataTypeId().isNull())
					try { db.expandSDI(parm); }
					catch (NoSuchObjectException e) {}
	}

	@Override
	public boolean checkCachedObjectOK(CachableDbObject ob)
	{
		DbComputation comp = (DbComputation)ob;
		String q = "select DATE_TIME_LOADED from CP_COMPUTATION "
			+ " where COMPUTATION_ID = " + comp.getKey();
		try
		{
			ResultSet rs = doQuery2(q);
			if (rs == null || !rs.next())
			{
				String msg 
					= "No match finding DATE_TIME_LOADED for computation "
					+ "id=" + comp.getKey() + ", name=" + comp.getUniqueName();
				debug1(msg);
				return false;
			}
			Date lmt = db.getFullDate(rs, 1);
			return lmt.getTime() <= comp.getLastModified().getTime();
		}
		catch(Exception ex)
		{
			String msg = "Error checking  computation id=" + comp.getKey() + ": " + ex;
			warning(msg);
			return false;
		}
	}
	
	@Override
	public DbComputation getComputationByName(String name)
		throws DbIoException, NoSuchObjectException
	{
		String q = "select " + compTableColumns 
			+ " from CP_COMPUTATION where COMPUTATION_NAME = '"
			+ name + "'";
		
		try
		{
			ResultSet rs = doQuery(q);
			if (rs.next())
			{
				DbComputation comp = rs2comp(rs);
				if (comp.getAlgorithmId() != Constants.undefinedId)
				{
					try
					{
						comp.setAlgorithm(algorithmDAO.getAlgorithmById(comp.getAlgorithmId()));
					}
					catch(NoSuchObjectException ex)
					{
						warning("Computation '" +name + "' with algo ID="
							+ comp.getAlgorithmId() + " -- cannot find matching "
							+ "algorithm.");
					}
				}
				ArrayList<DbComputation> ca = new ArrayList<DbComputation>();
				ca.add(comp);
				fillCompSubordinates(ca);

				DbKey appId = comp.getAppId();
				if (!appId.isNull())
				{
					q = "select LOADING_APPLICATION_NAME from "
					  + "HDB_LOADING_APPLICATION where LOADING_APPLICATION_ID = " 
					  + appId;
					rs = doQuery(q);
					if (rs.next())
						comp.setApplicationName(rs.getString(1));
				}
				return comp;
			}
			else
				throw new NoSuchObjectException("No computation named '"
					+ name + "'");
		}
		catch(SQLException ex)
		{
			String msg = "Error reading computation '" + name + "': " + ex;
			warning(msg);
			throw new DbIoException(msg);
		}
	}

	/**
	 * This queries computations and does the filtering by app ID and
	 * algorithm ID only. The application further filters by parameters.
	 * @param filter the computation filter containing app and algorithm IDs
	 * @return List of computations
	 */
	public ArrayList<DbComputation> listCompsForGUI(CompFilter filter)
		throws DbIoException
	{
		debug1("listCompsForGUI " + filter);
		
		ArrayList<DbComputation> ret = new ArrayList<DbComputation>();
		
		String q = "select " + compTableColumns + " from CP_COMPUTATION ";

		StringBuilder whereClause = new StringBuilder();
		DbKey procId = filter.getProcessId();
		if (!procId.isNull())
			whereClause.append(" where loading_application_id = " + procId + " ");

		DbKey algoId = filter.getAlgoId();
		if (!algoId.isNull())
		{
			if (whereClause.length() > 0)
				whereClause.append(" and ");
			else
				whereClause.append(" where ");
			whereClause.append(" algorithm_id = " + algoId + " ");
		}

		q = q + whereClause.toString();
		try
		{
			HashSet<DbKey> algoIds = new HashSet<DbKey>();
			HashSet<DbKey> appIds = new HashSet<DbKey>();
			ResultSet rs = doQuery(q);
			while(rs != null && rs.next())
			{
				DbComputation comp = rs2comp(rs);
				ret.add(comp);
				appIds.add(comp.getAppId());
				if (!comp.getAlgorithmId().isNull())
					algoIds.add(comp.getAlgorithmId());
			}
			
			// Get only apps that have at least one comp.
			q = "select distinct a.LOADING_APPLICATION_ID, a.LOADING_APPLICATION_NAME "
				+ " from HDB_LOADING_APPLICATION a "
				+ " where exists (select b.LOADING_APPLICATION_ID from CP_COMPUTATION b "
				+ " where a.LOADING_APPLICATION_ID = b.LOADING_APPLICATION_ID)";
			rs = doQuery(q);
			while(rs != null && rs.next())
			{
				DbKey key = DbKey.createDbKey(rs, 1);
				String name = rs.getString(2);
				for(DbComputation comp : ret)
					if (comp.getAppId().equals(key))
						comp.setApplicationName(name);
			}
			
			// Read each algorithm only once and assign to using computation.
			for(DbKey algorithmId : algoIds)
			{
				DbCompAlgorithm algo = algorithmDAO.getAlgorithmById(algorithmId);
				
				for(DbComputation comp : ret)
				{
					if (algorithmId.equals(comp.getAlgorithmId()))
					{
						comp.setAlgorithm(algo);
					}
				}
			}
			
			fillCompSubordinates(ret);
		}
		catch(Exception ex)
		{
			String msg = "Exception reading computation: " + ex;
			warning(msg);
			System.err.println(msg);
			ex.printStackTrace(System.err);
		}
		
		return ret;
	}

	@Override
	public void writeComputation( DbComputation comp )
		throws DbIoException
	{		
		Logger.instance().debug2("writeComputation name=" + comp.getName());
		DbKey id = comp.getId();
		boolean isNew = id.isNull();
		String q;

		if (isNew)
		{
			// Could be an XML import of an existing comp.
			// Try to determine id from name.
			try
			{
				id = getComputationId(comp.getName());			
				isNew = id.isNull();
				if (!isNew)
					info("Determined comp id=" + id
						+ " from comp name '" + comp.getName() + "'");
				comp.setId(id);
			}
			catch(NoSuchObjectException ex) { /* ignore */ }
		}

		try 
		{
			DbKey appId = comp.getAppId();
			
			if (appId.isNull())
			{
				String appName = comp.getApplicationName();
				if (appName != null && appName.length() > 0)
				{
					q = "select LOADING_APPLICATION_ID from "
					  + "hdb_loading_application "
					  + "where LOADING_APPLICATION_NAME = '" + appName + "'";
					try
					{
						ResultSet rs = doQuery(q);
						if (rs.next())
							appId = DbKey.createDbKey(rs, 1);
					}
					catch(SQLException ex)
					{
						warning("Query '" + q + "': " + ex);
						appId = Constants.undefinedId;
					}
				}
			}
			DbKey algoId = comp.getAlgorithmId();
			
			if (algoId.isNull())
			{
				String algoName = comp.getAlgorithmName();
				Logger.instance().debug2(
					"Computation has undefined algo ID, will lookup name '"
					+ algoName + "'");
				if (algoName != null && algoName.trim().length() > 0)
				{
					DbCompAlgorithm algo = null;
					try { algo = algorithmDAO.getAlgorithm(algoName.trim()); }
					catch(NoSuchObjectException ex) { algo = null; }
					if (algo != null)
						comp.setAlgorithm(algo);
					else Logger.instance().debug2("Algorithm still null!");
				}
			}


			String appIdStr = (appId.isNull() ? "null" : ("" + appId));
			comp.setLastModified(new Date());
			if (isNew)
			{
				id = getKey("CP_COMPUTATION");
				comp.setId(id);
				q = "INSERT INTO CP_COMPUTATION(" + compTableColumnsNoTabName
					+ ") VALUES("
					+ id
					+ ", " + sqlString(comp.getName())
					+ ", " + comp.getAlgorithmId()
					+ ", " + sqlString(comp.getComment())
					+ ", " + appIdStr
					+ ", " + db.sqlDate(comp.getLastModified())
					+ ", " + db.sqlBoolean(comp.isEnabled())
					+ ", " + db.sqlDate(comp.getValidStart())
					+ ", " + db.sqlDate(comp.getValidEnd());
				if (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_6)
					q = q + ", " + comp.getGroupId();
				q = q + ")";
			}
			else // update
			{
				q = "UPDATE CP_COMPUTATION"
			  		+  " SET COMPUTATION_NAME = " + sqlString(comp.getName()) 
					+ ", ALGORITHM_ID = " + comp.getAlgorithmId()
					+ ", CMMNT = " + sqlString(comp.getComment())
					+ ", LOADING_APPLICATION_ID = " + appIdStr
					+ ", DATE_TIME_LOADED = " + db.sqlDate(comp.getLastModified())
					+ ", ENABLED = " + db.sqlBoolean(comp.isEnabled())
					+ ", EFFECTIVE_START_DATE_TIME = " 
						+ db.sqlDate(comp.getValidStart())
					+ ", EFFECTIVE_END_DATE_TIME = " 
						+ db.sqlDate(comp.getValidEnd());
				if (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_6)
					q = q + ", GROUP_ID = " + comp.getGroupId();
				q = q + " WHERE COMPUTATION_ID = " + id;
			}

			doModify(q);

			// Delete parameters. Will do nothing if new.
			q = "DELETE FROM CP_COMP_TS_PARM WHERE COMPUTATION_ID = " + id;
			doModify(q);
			q = "DELETE FROM CP_COMP_PROPERTY WHERE COMPUTATION_ID = " + id;
			doModify(q);
			
			// For V5 databases, we must store the group ID in the first parm
			// See the loop below.
			DbKey groupId = db.getTsdbVersion() >= 6 ? Constants.undefinedId : comp.getGroupId();

			for(Iterator<DbCompParm> it = comp.getParms(); it.hasNext(); )
			{
				DbCompParm dcp = it.next();
				q = "INSERT INTO CP_COMP_TS_PARM VALUES ("
					+ id + ", "
					+ sqlString(dcp.getRoleName()) + ", " 
					+ (dcp.getSiteDataTypeId().isNull() ? "-1" : dcp.getSiteDataTypeId())
						+ ", "
					+ sqlString(dcp.getInterval()) + ", " 
					+ sqlString(dcp.getTableSelector()) + ", " 
					+ dcp.getDeltaT() + ", "
					+ dcp.getModelId();
				
				if (db.getTsdbVersion() >= 5)
				{
					DataType dt = dcp.getDataType();

					// parm uses previously unknown ID? Must write it too.
					if (dt != null && dt.getId() == Constants.undefinedId)
					{
						DataTypeDAI dataTypeDao = db.makeDataTypeDAO();
						try { dataTypeDao.writeDataType(dt); }
						finally { dataTypeDao.close(); }
						dcp.setDataTypeId(dt.getId());
					}
					q = q + ", " + dcp.getDataTypeId();
					
					groupId = Constants.undefinedId;
					if (db.getTsdbVersion() < 8)
						q = q + ", " + groupId;  // old slot for group id

					if (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_6)
					{
						String dtu = dcp.getDeltaTUnits();
						q = q + ", " +
							(dtu == null ? "null" : ("'" + dtu + "'"));
					}
					
					if (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_9)
					{
						q = q + ", " + dcp.getSiteId();
					}
				}

				q += ")";
				doModify(q);
			}

			// (re)add properties
			for(Enumeration<?> e = comp.getPropertyNames();
				e.hasMoreElements(); )
			{
				String nm = (String)e.nextElement();
				q = "INSERT INTO CP_COMP_PROPERTY VALUES ("
					+ id + ", "
					+ sqlString(nm) + ", "
					+ sqlString(comp.getProperty(nm)) + ")";
				
				doModify(q);
			}

			if (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_5 && db.isCwms())
			{
				CompDependsDAI compDependsDAO = db.makeCompDependsDAO();
				try { compDependsDAO.writeCompDepends(comp); }
				finally { compDependsDAO.close(); }
			}
		}
		catch(DbIoException ex)
		{
			warning(ex.getMessage());
			throw ex;
		}
	}

	@Override
	public DbKey getComputationId(String name)
		throws DbIoException, NoSuchObjectException
	{
		String q = "select COMPUTATION_ID from CP_COMPUTATION "
			+ "where COMPUTATION_NAME = '" + name + "'";
		try
		{
			ResultSet rs = doQuery(q);
			if (rs.next())
				return DbKey.createDbKey(rs, 1);
			throw new NoSuchObjectException("No computation for name '" + name 
				+ "'");
		}
		catch(SQLException ex)
		{
			String msg = "Error getting computation ID for name '" 
				+ name + "': " + ex;
			failure(msg);
			throw new DbIoException(msg);
		}
	}

	@Override
	public void deleteComputation(DbKey id)
		throws DbIoException, ConstraintException
	{
		String q = "delete from CP_COMP_TS_PARM where COMPUTATION_ID = " + id;
		doModify(q);
		q = "delete from CP_COMP_PROPERTY where COMPUTATION_ID = " + id;
		doModify(q);
		q = "delete from CP_COMPUTATION where COMPUTATION_ID = "+id;
		doModify(q);
		
		// Remove the CP_COMP_DEPENDS records & re-add.
		if (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_5 && db.isCwms())
		{
			CompDependsDAI compDependsDAO = db.makeCompDependsDAO();
			try { compDependsDAO.deleteCompDependsForCompId(id); }
			finally { compDependsDAO.close(); }
		}
	}

	@Override
	public void close()
	{
		super.close();
		tsGroupDAO.close();
		dataTypeDAO.close();
		algorithmDAO.close();
		propsDao.close();
	}

}

