/*
* $Id: ComputationDAO.java,v 1.15 2020/05/07 13:41:54 mmaloney Exp $
*
* $Log: ComputationDAO.java,v $
* Revision 1.15  2020/05/07 13:41:54  mmaloney
* When deleting a computation, first delete it's dependencies using CompDependsDAO.
*
* Revision 1.14  2020/02/14 15:18:25  mmaloney
* Updates for OpenTSDB
*
* Revision 1.13  2019/08/26 20:51:47  mmaloney
* Added checks for enabled and executable class.
*
* Revision 1.12  2019/02/26 17:16:44  mmaloney
* HDB 660
*
* Revision 1.11  2017/08/22 19:58:40  mmaloney
* Refactor
*
* Revision 1.10  2017/07/13 20:57:47  mmaloney
* HDB-397, when listing for GUI, only add a single copy of each computation.
*
* Revision 1.9  2017/03/03 19:21:33  mmaloney
* Bugfix - it was trying to delete CP_COMPUTATION record before CP_COMP_DEPENDS
* record, which in pre DB 14 version is defined as a foreign key.
*
* Revision 1.8  2016/11/29 01:19:07  mmaloney
* Refactoring.
*
* Revision 1.7  2016/11/19 15:55:51  mmaloney
* Join PARM records with parent records to bring in implicit VPD filter in CWMS.
*
* Revision 1.6  2016/07/20 15:47:30  mmaloney
* Optimizations.
*
* Revision 1.5  2016/01/27 22:11:22  mmaloney
* Added compEditList method.
*
* Revision 1.4  2014/09/30 13:32:15  mmaloney
* removed season_id
*
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;

import opendcs.dai.AlgorithmDAI;
import opendcs.dai.CompDependsDAI;
import opendcs.dai.CompDependsNotifyDAI;
import opendcs.dai.ComputationDAI;
import opendcs.dai.DataTypeDAI;
import opendcs.dai.LoadingAppDAI;
import opendcs.dai.PropertiesDAI;
import opendcs.dai.TsGroupDAI;
import opendcs.dao.DbObjectCache.CacheIterator;
import opendcs.util.sql.WrappedConnection;
import decodes.db.Constants;
import decodes.db.DataType;
import decodes.sql.DbKey;
import decodes.tsdb.CompAppInfo;
import decodes.tsdb.CompFilter;
import decodes.tsdb.ConstraintException;
import decodes.tsdb.CpDependsNotify;
import decodes.tsdb.DbAlgoParm;
import decodes.tsdb.DbCompAlgorithm;
import decodes.tsdb.DbCompParm;
import decodes.tsdb.DbCompResolver;
import decodes.tsdb.DbComputation;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsGroup;
import decodes.tsdb.TsdbDatabaseVersion;
import decodes.tsdb.compedit.ComputationInList;

/**
Data Access Object for reading/writing computations.
*/
public class ComputationDAO
	extends DaoBase
	implements ComputationDAI
{
	protected static DbObjectCache<DbComputation> compCache =
		new DbObjectCache<DbComputation>(60 * 60 * 1000L, false);

	protected PropertiesDAI propsDao = null;
	protected AlgorithmDAI algorithmDAO = null;
	protected DataTypeDAI dataTypeDAO = null;
	protected TsGroupDAI tsGroupDAO = null;
	protected LoadingAppDAI loadingAppDAO = null;

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
		loadingAppDAO = db.makeLoadingAppDAO();
		if (tsdb.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_6)
		{
			compTableColumns = compTableColumns + ", group_id";
			compTableColumnsNoTabName = compTableColumnsNoTabName + ", group_id";
		}
	}
	
	@Override
	public Connection getConnection()
	{
		// Overriding getConnection allows lazy connection setting and
		// ensures subordinate DAOs will use same conn as this object.
		if (myCon == null)
		{
			super.getConnection();
			propsDao.setManualConnection(myCon);
debug1("Setting manual connection for algorithmDAO");
			algorithmDAO.setManualConnection(myCon);
			dataTypeDAO.setManualConnection(myCon);
			tsGroupDAO.setManualConnection(myCon);
			loadingAppDAO.setManualConnection(myCon);
		}
		return new WrappedConnection(myCon, c -> {});
	}

	private void fillCache()
	{
		debug1("ComputationDAO.fillCache()");

		String q = "select " + compTableColumns + " from CP_COMPUTATION ";

		try
		{
			ResultSet rs = doQuery(q);
			int n = 0;
			while(rs != null && rs.next())
			{
				compCache.put(rs2comp(rs));
				n++;
			}
			Logger.instance().debug1("" + n + " cp_computation recs read.");

			n = propsDao.readPropertiesIntoCache("CP_COMP_PROPERTY", compCache);
			Logger.instance().debug1("" + n + " cp_comp_property recs read.");

			ArrayList<CompAppInfo> apps = loadingAppDAO.listComputationApps(true);
			ArrayList<DbCompAlgorithm> algos = algorithmDAO.listAlgorithms();

			// Associate comps with groups, apps & algorithms.
			for(CacheIterator it = compCache.iterator(); it.hasNext(); )
			{
				DbComputation comp = (DbComputation)it.next();
				if (!DbKey.isNull(comp.getGroupId()))
					comp.setGroup(tsGroupDAO.getTsGroupById(comp.getGroupId()));

				if (!DbKey.isNull(comp.getAppId()))
					for(CompAppInfo cai : apps)
						if (comp.getAppId().equals(cai.getAppId()))
							comp.setApplicationName(cai.getAppName());

				if (!DbKey.isNull(comp.getAlgorithmId()))
					for(DbCompAlgorithm algo : algos)
						if (comp.getAlgorithmId().equals(algo.getId()))
						{
							comp.setAlgorithm(algo);
							comp.setAlgorithmName(algo.getName());
						}
			}

			// Note the parms rely on the algorithms being in place. So get them now.
			q = "select a.* from CP_COMP_TS_PARM a, CP_COMPUTATION b "
				+ "where a.COMPUTATION_ID = b.COMPUTATION_ID";
			rs = doQuery(q);
			n = 0;
			while(rs.next())
			{
				DbKey compId = DbKey.createDbKey(rs, 1);
				DbComputation comp = compCache.getByKey(compId);
				if (comp == null)
				{
					warning("CP_COMP_TS_PARM with comp id=" + compId + " with no matching computation.");
					continue;
				}

				rs2compParm(comp, rs);
				n++;
			}
			Logger.instance().debug1("" + n + " cp_comp_ts_parm recs read.");

			for(CacheIterator it = compCache.iterator(); it.hasNext(); )
			{
				DbComputation comp = (DbComputation)it.next();

				// Make sure site IDs and datatype IDs are set in the parms
				for(DbCompParm parm : comp.getParmList())
					if (!parm.getSiteDataTypeId().isNull())
						try { db.expandSDI(parm); }
						catch (NoSuchObjectException e) {}
			}
			debug1("fillCache finished, " + compCache.size() + " computations cached.");
		}
		catch(Exception ex)
		{
			String msg = "Exception filling computation hash: " + ex;
			warning(msg);
			System.err.println(msg);
			ex.printStackTrace(System.err);
		}
	}

	@Override
	public DbComputation getComputationById(DbKey compId) throws DbIoException,
		NoSuchObjectException
	{
		DbComputation ret = compCache.getByKey(compId, this);
		if (ret != null)
			return ret;

		try(Connection c = getConnection();
			PreparedStatement getComp = c.prepareStatement(
				"select " + compTableColumns + " from CP_COMPUTATION where COMPUTATION_ID = ?"
			);
			PreparedStatement getAppId = c.prepareStatement(
				"select LOADING_APPLICATION_NAME from HDB_LOADING_APPLICATION where LOADING_APPLICATION_ID = ?"
			);
		)
		{
			getComp.setLong(1,compId.getValue());
			try( ResultSet rs = getComp.executeQuery(); ) {
				if (rs.next())
				{
					DbComputation comp = rs2comp(rs);
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
					fillCompSubordinates(comp);

					DbKey appId = comp.getAppId();
					if (!appId.isNull())
					{
						getAppId.setLong(1,appId.getValue());
						try( ResultSet rs2 = getAppId.executeQuery(); ){
							if (rs.next())
							comp.setApplicationName(rs.getString(1));
						}


					}
					compCache.put(comp);
					return comp;
				}
				else
					throw new NoSuchObjectException("No computation with ID="
						+ compId);
			}


		}
		catch(SQLException ex)
		{
			String msg = "Error reading computation id=" + compId + ": " + ex;
			warning(msg);
			throw new DbIoException(msg);
		}
	}

	/**
	 * Create computation object &amp; fill with current row of result set.
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
	 * @param comp The computation to fill.
	 * @throws SQLException
	 * @throws DbIoException
	 */
	protected void fillCompSubordinates(DbComputation comp)
		throws SQLException, DbIoException
	{
		Connection conn = getConnection();
		try(
			PreparedStatement getParms = conn.prepareStatement(
				"select * from CP_COMP_TS_PARM where computation_id = ?"
			);
			PreparedStatement getProps = conn.prepareStatement(
				"select * from CP_COMP_PROPERTY where computation_id= ?"
			);
		){
			getParms.setLong(1,comp.getId().getValue());

			String q = "select * from CP_COMP_TS_PARM where computation_id = " + comp.getId();
			try( ResultSet rs = getParms.executeQuery() ){
				while(rs.next())
				rs2compParm(comp, rs);
			}

			getProps.setLong(1,comp.getId().getValue());
			try( ResultSet rs = getProps.executeQuery()){
				while(rs.next())
				{
					String name = rs.getString(2);
					String value = rs.getString(3);
					if (value == null)
						value = "";

					comp.setProperty(name, value);
				}
			}


			// Retrieve groups used by computations (group DAO will cache)
			if (!comp.getGroupId().isNull())
				comp.setGroup(tsGroupDAO.getTsGroupById(comp.getGroupId()));

			// Make sure site IDs and datatype IDs are set in the parms
			for(DbCompParm parm : comp.getParmList())
				if (!parm.getSiteDataTypeId().isNull())
					try { db.expandSDI(parm); }
					catch (NoSuchObjectException e) {}

		}
	}

	/**
	 * Result set contains cp_comp_ts_parm record for the passed computation.
	 * @param comp
	 * @param rs
	 */
	private void rs2compParm(DbComputation comp, ResultSet rs)
		throws SQLException
	{
		String role = rs.getString(2);
		DbKey sdi = DbKey.createDbKey(rs, 3);
		if (sdi.getValue() == -1)
			sdi = DbKey.NullKey;
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

			/* Legacy: First implementation had groupd_id in the parm. */
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

	@Override
	public boolean checkCachedObjectOK(CachableDbObject ob)
	{
		DbComputation comp = (DbComputation)ob;
		String q = "select DATE_TIME_LOADED from CP_COMPUTATION "
			+ " where COMPUTATION_ID = " + comp.getKey();
		Connection conn = getConnection();
		try(
			PreparedStatement getTimeLoaded = conn.prepareStatement(
				"select DATE_TIME_LOADED from CP_COMPUTATION where COMPUTATION_ID = ?"
			);
		) {
			getTimeLoaded.setLong(1,comp.getId().getValue());
			try(ResultSet rs = getTimeLoaded.executeQuery(); )
			{
				if (!rs.next())
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
		try(Connection conn = getConnection();
			PreparedStatement getComp = conn.prepareStatement(
				"select " + compTableColumns + " from CP_COMPUTATION where COMPUTATION_NAME = ?"
			);
			PreparedStatement getAppId = conn.prepareStatement(
				"select LOADING_APPLICATION_NAME from HDB_LOADING_APPLICATION where LOADING_APPLICATION_ID = ?"
			);
		){
			getComp.setString(1,name);

			try(ResultSet rs = getComp.executeQuery(); )
			{
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
					fillCompSubordinates(comp);

					DbKey appId = comp.getAppId();
					if (!appId.isNull())
					{
						getAppId.setLong(1,appId.getValue());
						try(ResultSet rs2 = getAppId.executeQuery() ) {
							if (rs.next())
							comp.setApplicationName(rs.getString(1));
						}
					}
					return comp;
				}
				else
					throw new NoSuchObjectException("No computation named '"
						+ name + "'");
			}
		}
		catch(SQLException ex)
		{
			String msg = "Error reading computation '" + name + "': " + ex;
			warning(msg);
			throw new DbIoException(msg);
		}
	}


	/**
	 * Apply the application and algorithm parts of the filter to the cached computations.
	 * @param filter the computation filter containing app and algorithm IDs
	 * @return List of computations
	 */
	public ArrayList<DbComputation> listCompsForGUI(CompFilter filter)
		throws DbIoException
	{
		debug1("listCompsForGUI " + filter);

		if (compCache.size() == 0)
			fillCache();

		ArrayList<DbComputation> ret = new ArrayList<DbComputation>();
		for(CacheIterator it = compCache.iterator(); it.hasNext(); )
		{
			DbComputation comp = (DbComputation)it.next();
			DbKey procId = filter.getProcessId();
			if (!procId.isNull() && !procId.equals(comp.getAppId()))
				continue;

			DbKey algoId = filter.getAlgoId();
			if (!algoId.isNull() && !algoId.equals(comp.getAlgorithmId()))
				continue;

			if (filter.isEnabledOnly() && !comp.isEnabled())
				continue;

			if (filter.getExecClassName() != null
			 && comp.getAlgorithm() != null
			 && !TextUtil.strEqual(filter.getExecClassName(), comp.getAlgorithm().getExecClass()))
				continue;

			ret.add(comp);
		}
		return ret;
	}

	/**
	 * New 6.2 method for listing computations for the CompEdit GUI.
	 * @param filter
	 * @return
	 */
	@Override
	public ArrayList<ComputationInList> compEditList(CompFilter filter)
			throws DbIoException
	{
		debug3("compEditList()");
		ArrayList<ComputationInList> ret = new ArrayList<ComputationInList>();

		String columns = "cmp.computation_id, cmp.computation_name, "
			+ "cmp.algorithm_id, cmp.cmmnt, cmp.loading_application_id, cmp.enabled";
		String tables = "cp_computation cmp";
		StringBuilder where = new StringBuilder();

		// Build the where clause.
		if (!DbKey.isNull(filter.getAlgoId()))
		{
			if (where.length() > 0)
				where.append(" and ");
			where.append("algorithm_id = " + filter.getAlgoId());
		}
		if (!DbKey.isNull(filter.getProcessId()))
		{
			if (where.length() > 0)
				where.append(" and ");
			where.append("loading_application_id = " + filter.getProcessId());
		}
		if (filter.isEnabledOnly())
		{
			if (where.length() > 0)
				where.append(" and ");
			where.append("ENABLED = " + sqlBoolean(true));
		}
		// Group comp query does not include param fields.
		String groupWhere = where.toString();

		// Build the query for NON group comps.
		if (where.length() > 0)
			where.append(" and ");

		where.append("(cmp.group_id is null or cmp.group_id = -1)");
		if (!DbKey.isNull(filter.getSiteId())
		 || !DbKey.isNull(filter.getDataTypeId())
		 || filter.getIntervalCode() != null)
		{
			tables = tables + ", cp_comp_ts_parm prm";
			where.append(" and cmp.computation_id = prm.computation_id");

			if (db.isCwms() || db.isOpenTSDB())
			{
				// CWMS already has site_id and datatype_id in the fully-defined parms.
				if (!DbKey.isNull(filter.getSiteId()))
					where.append(" and prm.site_id = " + filter.getSiteId());
				if (!DbKey.isNull(filter.getDataTypeId()))
					where.append(" and prm.datatype_id = " + filter.getDataTypeId());
			}
			else if (db.isHdb())
			{
				tables = tables + ", HDB_SITE_DATATYPE sdt";
				where.append(" and prm.site_datatype_id = sdt.site_datatype_id");
				if (!DbKey.isNull(filter.getSiteId()))
					where.append(" and sdt.site_id = " + filter.getSiteId());
				if (!DbKey.isNull(filter.getDataTypeId()))
					where.append(" and sdt.datatype_id = " + filter.getDataTypeId());
			}

			if (filter.getIntervalCode() != null)
				where.append(" and lower(prm.INTERVAL"
					+ (db.isHdb()?"":"_ABBR") +") = '" + filter.getIntervalCode().toLowerCase() + "'");
		}

		// Get all non-group computations via where clause using all filter
		if (DbKey.isNull(filter.getGroupId()))
		{
			String q = "select " + columns + " from " + tables;
			if (where.length() > 0)
				q = q + " where " + where.toString();
			q = q + " order by cmp.computation_id";
			try
			{
				debug3("Getting NON-group comps with query '" + q + "'");
				ResultSet rs = doQuery(q);
				int n = 0;
				DbKey lastCompId = DbKey.NullKey;
				while(rs.next())
				{
					// A computation may have multiple params that pass the filter.
					// Only add a given computation to the list once.
					DbKey compId = DbKey.createDbKey(rs, 1);
					if (compId.equals(lastCompId))
						continue;

					ret.add(
						new ComputationInList(compId, rs.getString(2),
							DbKey.createDbKey(rs, 3), DbKey.createDbKey(rs, 5),
							TextUtil.str2boolean(rs.getString(6)), rs.getString(4)));
					n++;
					lastCompId = compId;
				}
				debug3("" + n + " non-group computations retrieved.");
			}
			catch(Exception ex)
			{
				throw new DbIoException("CompuationDao.compEditList(): Error in query '" + q + "': " + ex);
			}
		}


		// Now get all group comps completely. The number should be small.
		String q = "select " + compTableColumns + " from CP_COMPUTATION "
			+ "where ";
		if (DbKey.isNull(filter.getGroupId())) // accept any comp with a group
			q = q + " (GROUP_ID is not null and GROUP_ID != -1) ";
		else // specific group is requested
			q = q + " GROUP_ID = " + filter.getGroupId() + " ";

		if (groupWhere.length() > 0)
			q = q + " and " + groupWhere.toString();

		ArrayList<CompAppInfo> apps = loadingAppDAO.listComputationApps(true);
		ArrayList<DbCompAlgorithm> algos = algorithmDAO.listAlgorithms();

		debug3("Expanding group comps and checking filter");
		try
		{
			ArrayList<DbKey> groupCompIds = new ArrayList<DbKey>();

			ResultSet rs = doQuery(q);
			int n = 0;
			while(rs != null && rs.next())
			{
				DbComputation comp = rs2comp(rs);
				compCache.put(comp);
				groupCompIds.add(comp.getKey());
				n++;
			}
			Logger.instance().debug1("" + n + " cp_computation group comp recs read.");

			q = "select prop.computation_id, prop.prop_name, prop.prop_value "
				+ "from cp_comp_property prop, cp_computation cmp "
				+ "where prop.computation_id = cmp.computation_id and "
				+ "(cmp.GROUP_ID is not null and cmp.GROUP_ID != -1)";
			if (groupWhere.length() > 0)
				q = q + " and " + groupWhere.toString();
			rs = doQuery(q);
			n = 0;
			while(rs != null && rs.next())
			{
				DbComputation comp = compCache.getByKey(DbKey.createDbKey(rs, 1));
				if (comp != null)
					comp.setProperty(rs.getString(2), rs.getString(3));
				n++;
			}
			Logger.instance().debug1("" + n + " cp_comp_property recs read.");

			// Associate comps with groups, apps & algorithms.
			for(CacheIterator it = compCache.iterator(); it.hasNext(); )
			{
				DbComputation comp = (DbComputation)it.next();
				if (!DbKey.isNull(comp.getGroupId()))
					comp.setGroup(tsGroupDAO.getTsGroupById(comp.getGroupId()));

				if (!DbKey.isNull(comp.getAppId()))
					for(CompAppInfo cai : apps)
						if (comp.getAppId().equals(cai.getAppId()))
						{
							comp.setApplicationName(cai.getAppName());
							break;
						}

				if (!DbKey.isNull(comp.getAlgorithmId()))
					for(DbCompAlgorithm algo : algos)
						if (comp.getAlgorithmId().equals(algo.getId()))
						{
							comp.setAlgorithm(algo);
							comp.setAlgorithmName(algo.getName());
							break;
						}
			}

			// Note the parms rely on the algorithms being in place. So get them now.
			q = "select prm.* "
				+ "from CP_COMP_TS_PARM prm, CP_COMPUTATION cmp "
				+ "where prm.COMPUTATION_ID = cmp.COMPUTATION_ID "
				+ "and (cmp.GROUP_ID is not null and cmp.GROUP_ID != -1)";
			if (groupWhere.length() > 0)
				q = q + " and " + groupWhere.toString();

			rs = doQuery(q);
			n = 0;
			while(rs.next())
			{
				DbKey compId = DbKey.createDbKey(rs, 1);
				DbComputation comp = compCache.getByKey(compId);
				if (comp != null)
					rs2compParm(comp, rs);
				n++;
			}
			Logger.instance().debug1("" + n + " cp_comp_ts_parm recs read.");

			for(CacheIterator it = compCache.iterator(); it.hasNext(); )
			{
				DbComputation comp = (DbComputation)it.next();

				// Make sure site IDs and datatype IDs are set in the parms
				for(DbCompParm parm : comp.getParmList())
					if (!parm.getSiteDataTypeId().isNull())
						try { db.expandSDI(parm); }
						catch (NoSuchObjectException e) {}
			}

			// Now the cache has all my group comps and groupCompIds is a list of the IDs
			// that are for this query.
			// Expand the groups, evaluate the comps, check the expanded params.
			for(DbKey compId : groupCompIds)
			{
				DbComputation groupComp = compCache.getByKey(compId);
				TsGroup group = groupComp.getGroup();

				if (group == null) // Means comp had an invalid group ID
				{
					Logger.instance().warning("Computation ID=" + compId + " has invalid group ID="
						+ groupComp.getGroupId() + " -- skipped.");
					continue;
				}

				// If no TS-specific filtering is done, there's no need to expand.
				if (DbKey.isNull(filter.getSiteId()) && DbKey.isNull(filter.getDataTypeId())
				 && filter.getIntervalCode() == null)
				{
					if (filter.passes(groupComp))
						ret.add(
							new ComputationInList(groupComp.getKey(), groupComp.getName(),
								groupComp.getAlgorithmId(), groupComp.getAppId(),
								groupComp.isEnabled(), groupComp.getComment()));
					continue;
				}

				// Group object may be shared by multiple comps. Only expand it once.
				if (!group.getIsExpanded())
					db.expandTsGroup(group);

				for(TimeSeriesIdentifier tsid : group.getExpandedList())
					try
					{
						if (filter.passes(DbCompResolver.makeConcrete((TimeSeriesDb)db, tsid, groupComp, false)))
						{
							ret.add(
								new ComputationInList(groupComp.getKey(), groupComp.getName(),
									groupComp.getAlgorithmId(), groupComp.getAppId(),
									groupComp.isEnabled(), groupComp.getComment()));
							break;
						}
					}
					catch(NoSuchObjectException ex)
					{
						Logger.instance().debug1("Cannot expand comp(" + groupComp.getId()
							+ ") " + groupComp.getName() + ": " + ex);
					}
			}

		}
		catch(Exception ex)
		{
			String msg = "Exception listing computations for GUI: " + ex;
			warning(msg);
			System.err.println(msg);
			ex.printStackTrace(System.err);
		}

		// Fill in algo name & process name, or leave that for app?
		for(ComputationInList cil : ret)
		{
			DbKey appId = cil.getProcessId();
			if (!DbKey.isNull(appId))
				for(CompAppInfo cai : apps)
					if (appId.equals(cai.getKey()))
					{
						cil.setProcessName(cai.getAppName());
						break;
					}
			DbKey algoId = cil.getAlgorithmId();
			if (!DbKey.isNull(algoId))
				for(DbCompAlgorithm algo : algos)
					if (algoId.equals(algo.getKey()))
					{
						cil.setAlgorithmName(algo.getName());
						break;
					}
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

				// NOTE The HDB CP_COMP_TS_PARM_ARCHIVE table does not allow null in SDI, use -1.
				String sdiValue = "" +
					(db.isHdb() && DbKey.isNull(dcp.getSiteDataTypeId()) ? -1 : dcp.getSiteDataTypeId());

				q = "INSERT INTO CP_COMP_TS_PARM VALUES ("
					+ id + ", "
					+ sqlString(dcp.getRoleName()) + ", "
					+ sdiValue + ", "
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
						dataTypeDao.setManualConnection(getConnection());
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

			if (db.isCwms())
			{
				// CWMS DB 14 uses the Comp Depends Updater Daemon. So send a NOTIFY.
				if (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_14)
				{
					try (CompDependsNotifyDAI dai = db.makeCompDependsNotifyDAO())
					{
						CpDependsNotify cdn = new CpDependsNotify();
						cdn.setEventType(CpDependsNotify.CMP_MODIFIED);
						cdn.setKey(comp.getKey());
						dai.saveRecord(cdn);
					}
				}
				// Older versions the GUI must update dependencies directly.
				else if (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_5)
				{
					CompDependsDAI compDependsDAO = db.makeCompDependsDAO();
					compDependsDAO.setManualConnection(getConnection());
					try { compDependsDAO.writeCompDepends(comp); }
					finally { compDependsDAO.close(); }
				}
			}
			else if (db.isOpenTSDB() && db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_67)
			{
				try (CompDependsNotifyDAI dai = db.makeCompDependsNotifyDAO())
				{
					CpDependsNotify cdn = new CpDependsNotify();
					cdn.setEventType(CpDependsNotify.CMP_MODIFIED);
					cdn.setKey(comp.getKey());
					dai.saveRecord(cdn);
				}
			}
			// Note HDB does the notifications via Trigger, so no need to do anything.
		}
		catch(DbIoException ex)
		{
			warning(ex.getMessage());
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			ex.printStackTrace(pw);
			warning(sw.toString());
			throw ex;
		}
	}

	@Override
	public DbKey getComputationId(String name)
		throws DbIoException, NoSuchObjectException
	{
		String q = "select COMPUTATION_ID from CP_COMPUTATION "
			+ "where COMPUTATION_NAME = '" + name + "'";
		Connection conn = getConnection();
		try(
			PreparedStatement getCompId = conn.prepareStatement(
				"select COMPUTATION_ID from CP_COMPUTATION where COMPUTATION_NAME = ?"
			);
		) {
			getCompId.setString(1,name);
			try(ResultSet rs = getCompId.executeQuery();)
			{

				if (rs.next())
					return DbKey.createDbKey(rs.getLong("COMPUTATION_ID"));
				throw new NoSuchObjectException("No computation for name '" + name
					+ "'");
			}
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

		Connection conn = getConnection();
		boolean defaultAutoCommit = false;



		/*
			I'm pretty sure this could just be a delete ... cascade on cp_computation
			but that can go overboard so it'll require a little investigation first
			so as not to break anything.
		*/
		try(
			PreparedStatement deleteParms = conn.prepareStatement(
				"delete from CP_COMP_TS_PARM where COMPUTATION_ID = ?"
			);
			PreparedStatement deleteProps = conn.prepareStatement(
				"delete from CP_COMP_PROPERTY where COMPUTATION_ID = ?"
			);
			PreparedStatement deleteComp = conn.prepareStatement(
				"delete from CP_COMPUTATION where COMPUTATION_ID = ?"
			);
		 ){
			defaultAutoCommit = conn.getAutoCommit();
			conn.setAutoCommit(false);
				// Have to delete the dependencies, otherwise foreign key will prevent comp delete.
			CompDependsDAI compDependsDAO = db.makeCompDependsDAO();
			compDependsDAO.setManualConnection(getConnection());
			try
			{
				compDependsDAO.deleteCompDependsForCompId(id);
			}
			finally { compDependsDAO.close(); }


			deleteParms.setLong(1,id.getValue());
			deleteParms.execute();

			deleteProps.setLong(1,id.getValue());
			deleteProps.execute();

			deleteComp.setLong(1,id.getValue());
			deleteProps.execute();

			conn.commit(); // now we commit knowing we deleted everything

		} catch( SQLException err ){

		} finally {
			try{ conn.setAutoCommit(defaultAutoCommit); } catch(Exception err){ warning("could not reset autocommit on connection"); }
		}

		String q = "delete from CP_COMP_TS_PARM where COMPUTATION_ID = " + id;
		doModify(q);
		q = "delete from CP_COMP_PROPERTY where COMPUTATION_ID = " + id;
		doModify(q);

		q = "delete from CP_COMPUTATION where COMPUTATION_ID = "+id;
		doModify(q);
	}

	@Override
	public void close()
	{
		super.close();
		loadingAppDAO.close();
		tsGroupDAO.close();
		dataTypeDAO.close();
		algorithmDAO.close();
		propsDao.close();
	}

}
