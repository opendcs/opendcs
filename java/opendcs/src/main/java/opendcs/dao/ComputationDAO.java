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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

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
import decodes.tsdb.DbComputation;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TsdbDatabaseVersion;
import ilex.util.TextUtil;
import opendcs.dai.AlgorithmDAI;
import opendcs.dai.CompDependsDAI;
import opendcs.dai.CompDependsNotifyDAI;
import opendcs.dai.ComputationDAI;
import opendcs.dai.DataTypeDAI;
import opendcs.dai.LoadingAppDAI;
import opendcs.dai.PropertiesDAI;
import opendcs.dai.TsGroupDAI;
import opendcs.dao.DbObjectCache.CacheIterator;
import opendcs.util.functional.ThrowingSupplier;
import opendcs.util.sql.WrappedConnection;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

/**
Data Access Object for reading/writing computations.
*/
public class ComputationDAO extends DaoBase implements ComputationDAI
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();

	protected static DbObjectCache<DbComputation> compCache =
		new DbObjectCache<>(60 * 60 * 1000L, false);
	private static long lastCacheFill = 0L;
	public static final long CACHE_TIME_LIMIT = 20 * 60 * 1000L;

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
			log.debug("Setting manual connection for algorithmDAO");
			algorithmDAO.setManualConnection(myCon);
			dataTypeDAO.setManualConnection(myCon);
			tsGroupDAO.setManualConnection(myCon);
			loadingAppDAO.setManualConnection(myCon);
		}
		return new WrappedConnection(myCon, c -> {});
	}

	private void fillCache()
	{
		if (System.currentTimeMillis() - lastCacheFill < CACHE_TIME_LIMIT)
		{
			return;
		}
		log.debug("ComputationDAO.fillCache()");

		String q = "select " + compTableColumns + " from CP_COMPUTATION ";

		try
		{
			// clear the cache as we're rebuilding it. Though analyzing usage, this may never
			// be required.
			compCache.clear();
			final int[] n = new int[1];
			n[0] = 0;
			doQuery(q, rs ->
			{
				compCache.put(rs2comp(rs));
				n[0]++;
			});
			log.debug("{} cp_computation recs read.", n[0]);

			n[0] = propsDao.readPropertiesIntoCache("CP_COMP_PROPERTY", compCache);
			log.debug("{} cp_comp_property recs read.", n[0]);

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
			n[0] = 0;
			doQuery(q, rs ->
			{
				DbKey compId = DbKey.createDbKey(rs, 1);
				DbComputation comp = compCache.getByKey(compId);
				if (comp == null)
				{
					log.warn("CP_COMP_TS_PARM with comp id={} with no matching computation.", compId);
				}
				else
				{
					rs2compParm(comp, rs);
					n[0]++;
				}
			});
			log.debug("{} cp_comp_ts_parm recs read.", n[0]);

			for(CacheIterator it = compCache.iterator(); it.hasNext(); )
			{
				DbComputation comp = (DbComputation)it.next();

				// Make sure site IDs and datatype IDs are set in the parms
				for(DbCompParm parm : comp.getParmList())
					if (!parm.getSiteDataTypeId().isNull())
						try { db.expandSDI(parm); }
						catch (NoSuchObjectException e) {}
			}
			log.debug("fillCache finished, {} computations cached.", compCache.size());
		}
		catch(Exception ex)
		{
			log.atWarn()
			   .setCause(ex)
			   .log("Exception filling computation hash.");
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
							log.atWarn()
							   .setCause(ex)
							   .log("Computation ID={} with algo ID={} -- cannot find matching algorithm.",
							        compId, comp.getAlgorithmId());
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
			String msg = "Error reading computation id=" + compId;
			log.atWarn()
			   .setCause(ex)
			   .log(msg);
			throw new DbIoException(msg, ex);
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
		try
		{
			inTransaction(dao ->
			{
				try(TsGroupDAI tsGroupDao = db.makeTsGroupDAO())
				{
					tsGroupDao.inTransactionOf(dao);
					doQuery("select * from CP_COMP_TS_PARM where computation_id = ?", rs -> rs2compParm(comp,rs), comp.getId());
					doQuery("select * from CP_COMP_PROPERTY where computation_id= ?", rs ->
					{
						String name = rs.getString(2);
						String value = rs.getString(3);
						if (value == null)
						{
							value = "";
						}

						comp.setProperty(name, value);
					},
					comp.getId());

					// Retrieve groups used by computations (group DAO will cache)
					if (!comp.getGroupId().isNull())
					{
						comp.setGroup(tsGroupDAO.getTsGroupById(comp.getGroupId()));
					}

					// Make sure site IDs and datatype IDs are set in the parms
					for(DbCompParm parm : comp.getParmList())
					{
						if (!parm.getSiteDataTypeId().isNull())
						{
							try
							{
								db.expandSDI(parm);
							}
							catch (NoSuchObjectException e) {}
						}
					}
				}
			});
		}
		catch (Exception ex)
		{
			throw new DbIoException("Unable to fill computation data.", ex);
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
					log.debug("No match finding DATE_TIME_LOADED for computation id={}, name={}",
							  comp.getKey(), comp.getUniqueName());
					return false;
				}
				Date lmt = db.getFullDate(rs, 1);
				return lmt.getTime() <= comp.getLastModified().getTime();
			}
		}
		catch(Exception ex)
		{
			log.atWarn()
			   .setCause(ex)
			   .log("Error checking  computation id={}", comp.getKey());
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
							log.atWarn()
							   .setCause(ex)
							   .log("Computation '{}' with algo ID={} -- cannot find matching algorithm.",
							   		name, comp.getAlgorithmName());
						}
					}
					fillCompSubordinates(comp);

					DbKey appId = comp.getAppId();
					if (!appId.isNull())
					{
						getAppId.setLong(1,appId.getValue());
						try(ResultSet rs2 = getAppId.executeQuery() )
						{
							if (rs.next())
							{
								comp.setApplicationName(rs.getString(1));
							}
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
			String msg = "Error reading computation '" + name + "'";
			throw new DbIoException(msg, ex);
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
		log.debug("listCompsForGUI {}", filter);

		if (compCache.size() == 0)
			fillCache();

		ArrayList<DbComputation> ret = new ArrayList<>();
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
	 * Apply the filter to the cached computations.
	 * @param filter the computation filter containing site, algorithm, datatype, interval, process, and group names
	 * @return List of computations
	 */
	public List<DbComputation> listComps(Predicate<DbComputation> filter) throws DbIoException
	{
		log.debug("listComps {}", filter);

		fillCache();

		List<DbComputation> ret = new ArrayList<>();
		for (DbObjectCache<DbComputation>.CacheIterator it = compCache.iterator(); it.hasNext(); )
		{
			DbComputation comp = it.next();
			if (filter.test(comp))
			{
				ret.add(comp);
			}
		}
		return ret;
	}

	@Override
	public void writeComputation( DbComputation comp )
		throws DbIoException
	{
		log.trace("writeComputation name={}", comp.getName());
		final boolean isNew = ((ThrowingSupplier<Boolean,DbIoException>) (() -> {
			boolean tmpIsNew = comp.getId().isNull();
			if (tmpIsNew)
			{
				// Could be an XML import of an existing comp.
				// Try to determine id from name.
				try
				{
					DbKey id = getComputationId(comp.getName());
					tmpIsNew = id.isNull();
					if (!tmpIsNew)
					{
						log.info("Determined comp id={} from comp name '{}'", id, comp.getName());
					}
					comp.setId(id);
				}
				catch(NoSuchObjectException ex) { /* ignore */ }
			}
			return tmpIsNew;
		})).get();

		try
		{
			final DbKey appId = ((ThrowingSupplier<DbKey,Exception>)(() -> {
				DbKey tmpAppId = comp.getAppId();
				if (tmpAppId.isNull())
				{
					String appName = comp.getApplicationName();
					if (appName != null && !appName.isEmpty())
					{
						String q = "select LOADING_APPLICATION_ID from "
						+ "hdb_loading_application "
						+ "where LOADING_APPLICATION_NAME = ?";
						try
						{
							tmpAppId = getSingleResult(q, rs-> DbKey.createDbKey(rs, 1), appName);
						}
						catch(SQLException ex)
						{
							log.atWarn().setCause(ex).log("Query '{}'", q);
							tmpAppId = Constants.undefinedId;
						}
					}
				}
				return tmpAppId;
			})).get();

			DbKey algoId =  comp.getAlgorithmId();
			if (algoId.isNull())
			{
				String algoName = comp.getAlgorithmName();
				log.trace("Computation has undefined algo ID, will lookup name '{}'", algoName);
				if (algoName != null && !algoName.trim().isEmpty())
				{
					DbCompAlgorithm algo = null;
					try
					{
						algo = algorithmDAO.getAlgorithm(algoName.trim());
					}
					catch(NoSuchObjectException ex)
					{
						algo = null;
					}
					if (algo != null)
					{
						comp.setAlgorithm(algo);
					}
					else
					{
						log.trace("Algorithm still null!");
					}
				}
			}

			comp.setLastModified(new Date());
			inTransaction(dao ->
			{
				try (CompDependsDAI compDependsDAO = db.makeCompDependsDAO();
					 DataTypeDAI dataTypeDao = db.makeDataTypeDAO();)
				{
					dataTypeDao.inTransactionOf(dao);
					compDependsDAO.inTransactionOf(dao);

					ArrayList<Object> parameters = new ArrayList<>();
					final StringBuffer query = new StringBuffer(512); // arbitrary starting size

					if (isNew)
					{
						DbKey id = getKey("CP_COMPUTATION");
						comp.setId(id);
						query.append("INSERT INTO CP_COMPUTATION(" + compTableColumnsNoTabName
							+ ") VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?");
						parameters.add(id);
						parameters.add(comp.getName());
						parameters.add(comp.getAlgorithmId());
						parameters.add(comp.getComment());
						parameters.add(appId);
						parameters.add(comp.getLastModified());
						parameters.add(comp.isEnabled());
						parameters.add(comp.getValidStart());
						parameters.add(comp.getValidEnd());
						if (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_6)
						{
							query.append(", ?");
							parameters.add(comp.getGroupId());
						}
						query.append(")");
					}
					else // update
					{
						query.append("UPDATE CP_COMPUTATION"
							+  " SET COMPUTATION_NAME = ?"
							+ ", ALGORITHM_ID = ?"
							+ ", CMMNT = ?"
							+ ", LOADING_APPLICATION_ID = ?"
							+ ", DATE_TIME_LOADED = ?"
							+ ", ENABLED = ?"
							+ ", EFFECTIVE_START_DATE_TIME = ?"
							+ ", EFFECTIVE_END_DATE_TIME = ?");
						parameters.add(comp.getName());
						parameters.add(comp.getAlgorithmId());
						parameters.add(comp.getComment());
						parameters.add(appId);
						parameters.add(comp.getLastModified());
						parameters.add(comp.isEnabled());
						parameters.add(comp.getValidStart());
						parameters.add(comp.getValidEnd());
						if (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_6)
						{
							query.append(", GROUP_ID = ?");
							parameters.add(comp.getGroupId());
						}
						query.append(" WHERE COMPUTATION_ID = ?");
						parameters.add(comp.getId());
					}

					doModify(query.toString(), parameters.toArray(new Object[0]));
					DbKey id = comp.getId();
					// Delete parameters. Will do nothing if new.
					dao.doModify("DELETE FROM CP_COMP_TS_PARM WHERE COMPUTATION_ID = ?", id);
					dao.doModify("DELETE FROM CP_COMP_PROPERTY WHERE COMPUTATION_ID = ?", id);

					// For V5 databases, we must store the group ID in the first parm
					// See the loop below.
					DbKey groupId = db.getTsdbVersion() >= 6 ? Constants.undefinedId : comp.getGroupId();

					for(Iterator<DbCompParm> it = comp.getParms(); it.hasNext(); )
					{
						DbCompParm dcp = it.next();

						// NOTE The HDB CP_COMP_TS_PARM_ARCHIVE table does not allow null in SDI, use -1.
						DbKey sdiKey = dcp.getSiteDataTypeId();
						Long sdiValue = null;
						if (db.isHdb() && sdiKey.isNull())
						{
							sdiValue = -1L;
						}
						else if (!sdiKey.isNull())
						{
							sdiValue = sdiKey.getValue();
						}
						parameters.clear();
						query.setLength(0);
						query.append("INSERT INTO CP_COMP_TS_PARM VALUES (?, ?, ?, ?, ?, ?, ?");
						parameters.add(id);
						parameters.add(dcp.getRoleName());
						parameters.add(sdiValue);
						parameters.add(dcp.getInterval());
						parameters.add(dcp.getTableSelector());
						parameters.add(dcp.getDeltaT());
						parameters.add(dcp.getModelId());

						if (db.getTsdbVersion() >= 5)
						{
							DataType dt = dcp.getDataType();

							// parm uses previously unknown ID? Must write it too.
							if (dt != null && dt.getId() == Constants.undefinedId)
							{
								dataTypeDao.writeDataType(dt);
								dcp.setDataTypeId(dt.getId());
							}
							query.append(", ?");
							parameters.add(dcp.getDataTypeId());

							groupId = Constants.undefinedId;
							if (db.getTsdbVersion() < TsdbDatabaseVersion.VERSION_8)
							{
								query.append(",?");
								parameters.add(groupId); // old slot for group id
							}

							if (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_6)
							{
								String dtu = dcp.getDeltaTUnits();
								query.append(", ?");
								parameters.add(dtu);
							}

							if (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_9)
							{
								query.append(", ?");
								parameters.add(dcp.getSiteId());
							}
						}

						query.append(")");
						doModify(query.toString(), parameters.toArray(new Object[0]));
					}

					// (re)add properties
					for(Enumeration<?> e = comp.getPropertyNames();
						e.hasMoreElements(); )
					{
						String nm = (String)e.nextElement();
						doModify("INSERT INTO CP_COMP_PROPERTY VALUES (?,?,?)", id, nm, comp.getProperty(nm));
					}

					if (db.isCwms())
					{
						// CWMS DB 14 uses the Comp Depends Updater Daemon. So send a NOTIFY.
						if (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_14)
						{
							try (CompDependsNotifyDAI dai = db.makeCompDependsNotifyDAO())
							{
								log.trace("Saving CompDepends record.");
								dai.inTransactionOf(dao);
								CpDependsNotify cdn = new CpDependsNotify();
								cdn.setEventType(CpDependsNotify.CMP_MODIFIED);
								cdn.setKey(comp.getKey());
								dai.saveRecord(cdn);
							}
						}
						// Older versions the GUI must update dependencies directly.
						else if (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_5)
						{
							compDependsDAO.writeCompDepends(comp);
						}
					}
					else if (db.isOpenTSDB() && db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_67)
					{
						try (CompDependsNotifyDAI dai = db.makeCompDependsNotifyDAO())
						{
							dai.inTransactionOf(dao);
							CpDependsNotify cdn = new CpDependsNotify();
							cdn.setEventType(CpDependsNotify.CMP_MODIFIED);
							cdn.setKey(comp.getKey());
							dai.saveRecord(cdn);
						}
					}
				}
			});
			// Note HDB does the notifications via Trigger, so no need to do anything.
		}
		catch(Exception ex)
		{
			throw new DbIoException("Error writing computation", ex);
		}
	}

	@Override
	public DbKey getComputationId(String name)
		throws DbIoException, NoSuchObjectException
	{
		try
		{
			DbKey ret = getSingleResultOr("select COMPUTATION_ID from CP_COMPUTATION where COMPUTATION_NAME = ?",
								   rs -> DbKey.createDbKey(rs,1), null, name);
			if (ret == null)
			{
				throw new NoSuchObjectException("No computation for name '" + name + "'");
			}
			return ret;
		}
		catch(SQLException ex)
		{
			String msg = "Error getting computation ID for name '" + name + "'";
			throw new DbIoException(msg, ex);
		}

	}

	@Override
	public void deleteComputation(DbKey id)
		throws DbIoException, ConstraintException
	{
		/*
			I'm pretty sure this could just be a delete ... cascade on cp_computation
			but that can go overboard so it'll require a little investigation first
			so as not to break anything.
		*/
		try
		{
			inTransaction(dao ->
			{
				try (CompDependsDAI compDependsDAO = db.makeCompDependsDAO())
				{
					compDependsDAO.inTransactionOf(dao);
					compDependsDAO.deleteCompDependsForCompId(id);
					dao.doModify("delete from CP_COMP_TS_PARM where COMPUTATION_ID = ?", id);
					dao.doModify("delete from CP_COMP_PROPERTY where COMPUTATION_ID = ?", id);
					dao.doModify("delete from CP_COMPUTATION where COMPUTATION_ID = ?", id);
				}
			});
		}
		catch (Exception ex)
		{
			throw new DbIoException("Unable to delete computation with id="+id.toString(), ex);
		}
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