/*
*  $Id$
*
*  This is open-source software written by Cove Software LLC under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  This source code is provided completely without warranty.
*  
*  $Log$
*  Revision 1.5  2016/09/29 18:54:37  mmaloney
*  CWMS-8979 Allow Database Process Record to override decodes.properties and
*  user.properties setting. Command line arg -Dsettings=appName, where appName is the
*  name of a process record. Properties assigned to the app will override the file(s).
*
*  Revision 1.4  2016/06/27 15:27:05  mmaloney
*  Have to read data types as part of decodes init.
*
*  Revision 1.3  2014/12/19 19:24:58  mmaloney
*  Handle version change for column name tsdb_group_member_ts data_id vs. ts_id.
*
*  Revision 1.2  2014/08/22 17:23:04  mmaloney
*  6.1 Schema Mods and Initial DCP Monitor Implementation
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.46  2013/03/25 18:15:03  mmaloney
*  Refactor starting event server.
*
*  Revision 1.45  2013/03/21 18:27:39  mmaloney
*  DbKey Implementation
*
*/
package decodes.tsdb;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.net.InetAddress;

import opendcs.dai.ComputationDAI;
import opendcs.dai.LoadingAppDAI;
import opendcs.dai.TimeSeriesDAI;
import opendcs.dai.TsGroupDAI;
import lrgs.gui.DecodesInterface;
import ilex.cmdline.BooleanToken;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import ilex.util.EnvExpander;
import ilex.util.Logger;
import decodes.sql.DbKey;
import decodes.util.CmdLineArgs;
import decodes.util.DecodesException;
import decodes.cwms.CwmsTimeSeriesDb;
import decodes.db.Constants;
import decodes.db.DataType;
import decodes.db.Database;
import decodes.hdb.HdbTsId;
import decodes.hdb.HdbGroupHelper;


/**
This is the main class for the daemon that updates CP_COMP_DEPENDS.
*/
public class CpCompDependsUpdater
	extends TsdbAppTemplate
{
	/** Holds app name, id, & description. */
	private CompAppInfo appInfo;

	/** My lock in the time-series database */
	private TsdbCompLock myLock;
	
	private boolean shutdownFlag;
	private String hostname;
	private int evtPort = 0;
	private long lastCacheRefresh = 0L;

	/** Number of notifications successfully processed */
	private int done = 0;
	/** Number of notifications unsuccessfully processed */
	private int errs = 0;
	
	// Local caches for computations, groups, cp_comp_depends:
	private ArrayList<DbComputation> enabledCompCache = new ArrayList<DbComputation>();

	private GroupHelper groupHelper = null;
	
	private HashSet<CpCompDependsRecord> cpCompDependsCache = new HashSet<CpCompDependsRecord>();
	private HashSet<CpCompDependsRecord> toAdd = new HashSet<CpCompDependsRecord>();
	private BooleanToken fullEvalOnStartup = new BooleanToken("F", "Full Eval on Startup",
		"", TokenOptions.optSwitch, false);
	private StringToken groupCacheDump = new StringToken("G", "Dump group evaluations",
		"", TokenOptions.optSwitch, null);
	private BooleanToken fullEvalOnly = new BooleanToken("O", "Full Eval Only -- then quit.",
		"", TokenOptions.optSwitch, false);

	private boolean fullEvalDone = false;
	private Date notifyTime = new Date();
	private boolean doingFullEval = false;
	private TimeSeriesDAI timeSeriesDAO = null;

	/**
	 * Constructor called from main method after parsing arguments.
	 */
	public CpCompDependsUpdater()
	{
		super("compdepends.log");
		myLock = null;
		shutdownFlag = false;
		
		// Tell base class to reconnect gracefully if database goes down.
		surviveDatabaseBounce = true;
	}

	/** @return the application ID. */
	public DbKey getAppId() { return appId; }

	/** Sets default app name (and log file) to compdepends */
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		appNameArg.setDefaultValue("compdepends");
		cmdLineArgs.addToken(fullEvalOnStartup);
		cmdLineArgs.addToken(groupCacheDump);
		cmdLineArgs.addToken(fullEvalOnly);
	}

	/**
	 * Sets the application ID. 
	 * @param id the ID.
	 */
	public void setAppId(DbKey id) { this.appId = id; }

	/** @return the application name. */
	public String getAppName() 
	{
		return appInfo.getAppName(); 
	}

	/** @return the application comment. */
	public String getAppComment() 
	{
		return appInfo.getComment(); 
	}

	/**
	 * The application run method. Called after all initialization methods
	 * by the base class.
	 * @throws LockBusyException if another process has the lock
	 * @throws DbIoException on failure to access the database
	 * @throws NoSuchObjectException if the application is invalid.
	 */
	public void runApp( )
		throws LockBusyException, DbIoException, NoSuchObjectException
	{
		initialize();
		
		String msg = "============== CpCompDependsUpdater appName=" + getAppName() 
			+", appId=" + appId;
		if (theDb.isCwms())
			msg = msg + ", officeID=" + ((CwmsTimeSeriesDb)theDb).getDbOfficeId();
		Logger.instance().info(msg + " Starting ==============");
		
		timeSeriesDAO = theDb.makeTimeSeriesDAO();
		groupHelper = theDb.makeGroupHelper();

		String dir = groupCacheDump.getValue();
		if (dir != null && dir.length() > 0)
		{
			String expDir = EnvExpander.expand(dir);
			File dumpDir = new File(expDir);
			if (!dumpDir.isDirectory()
			 && !dumpDir.mkdirs())
			{
				warning("Cannot create group cache dump dir '" + expDir
					+ "'. -- Will not dump group cache.");
				dumpDir = null;
			}
			groupHelper.setGroupCacheDumpDir(dumpDir);
		}
		CpDependsNotify prevNotify = null;
		String action="";
		while(!shutdownFlag)
		{
			LoadingAppDAI loadingAppDAO = theDb.makeLoadingAppDAO();
			try
			{
				// Make sure this process's lock is still valid.
				action = "Checking lock";
				if (myLock == null)
					myLock = loadingAppDAO.obtainCompProcLock(appInfo, getPID(), hostname); 
				else
				{
					setAppStatus("Done=" + done + ", Errs=" + errs);
					loadingAppDAO.checkCompProcLock(myLock);
				}
				
				if ((fullEvalOnStartup.getValue() || fullEvalOnly.getValue()) && !fullEvalDone)
				{
					info("Doing one-time full evaluation on startup");
					CpDependsNotify ccdn = new CpDependsNotify();
					ccdn.setEventType(CpDependsNotify.FULL_EVAL);
					ccdn.setDateTimeLoaded(new Date());
					processNotify(ccdn);
					fullEvalDone = true;
					if (fullEvalOnly.getValue())
					{
						shutdownFlag = true;
						break;
					}
				}

				// Just to be safe, once per hour, reload all caches.
				long now = System.currentTimeMillis();
				if (now - lastCacheRefresh > 900000L) // every 15 minutes
				{
					action = "Refresh Caches";
					refreshCaches();
				}
				
				action = "Getting new data";
				CpDependsNotify ccdn = theDb.getCpCompDependsNotify();
				if (ccdn != null)
				{
					if (prevNotify != null && ccdn.equals(prevNotify))
					{
						info("Ignoring duplicate notify '" + ccdn + "'");
					}
					else
					{
						processNotify(ccdn);
						prevNotify = ccdn;
					}
				}
				else // Nothing to do now. Sleep a sec and try again.
				{
					try { Thread.sleep(1000L); }
					catch(InterruptedException ex) {}
				}
			}
			catch(LockBusyException ex)
			{
				Logger.instance().fatal("No Lock - Application exiting: " + ex);
				shutdownFlag = true;
			}
			catch(DbIoException ex)
			{
				warning("Database Error while " + action + ": " + ex);
				shutdownFlag = true;
				databaseFailed = true;
			}
			catch(Exception ex)
			{
				msg = "Unexpected exception while " + action + ": " + ex;
				warning(msg);
				System.err.println(msg);
				ex.printStackTrace(System.err);
				shutdownFlag = true;
				databaseFailed = true;
			}
			finally
			{
				loadingAppDAO.close();
			}
		}
		closeDb();
	}

	/** Initialization phase -- any error is fatal. */
	private void initialize()
		throws LockBusyException, DbIoException, NoSuchObjectException
	{
		LoadingAppDAI loadingAppDao = theDb.makeLoadingAppDAO();
		try
		{
			appInfo = loadingAppDao.getComputationApp(appId);

			try { hostname = InetAddress.getLocalHost().getHostName(); }
			catch(Exception e) { hostname = "unknown"; }

			// Look for EventPort and EventPriority properties. If found,
			String evtPorts = appInfo.getProperty("EventPort");
			if (evtPorts != null)
			{
				try 
				{
					evtPort = Integer.parseInt(evtPorts.trim());
					CompEventSvr compEventSvr = new CompEventSvr(evtPort);
					compEventSvr.startup();
				}
				catch(NumberFormatException ex)
				{
					Logger.instance().warning("App Name " + getAppName()
						+ ": Bad EventPort property '" + evtPorts
						+ "' must be integer. -- ignored.");
				}
				catch(IOException ex)
				{
					Logger.instance().failure(
						"Cannot create Event server: " + ex
						+ " -- no events available to external clients.");
				}
			}
		}
		catch(NoSuchObjectException ex)
		{
			Logger.instance().fatal("App Name " + getAppName() + ": " + ex);
			throw ex;
		}
		catch(DbIoException ex)
		{
			Logger.instance().fatal("App Name " + getAppName() + ": " + ex);
			throw ex;
		}
		finally
		{
			loadingAppDao.close();
		}
	}
	
	public void initDecodes()
		throws DecodesException
	{
		DecodesInterface.silent = true;
		if (DecodesInterface.isInitialized())
			return;
		DecodesInterface.initDecodesMinimal(cmdLineArgs.getPropertiesFile());
		Database.getDb().dataTypeSet.read();
	}

	/**
	 * The main method.
	 * @param args command line arguments.
	 */
	public static void main( String[] args )
		throws Exception
	{
		CpCompDependsUpdater app = new CpCompDependsUpdater();
		app.execute(args);
	}

	private void debug(String x)
	{
		Logger.instance().debug3("CompDependsUpdater(" + appId + "): " + x);
	}

	/**
	 * Sets the application's status string in its database lock.
	 */
	public void setAppStatus(String status)
	{
		if (myLock != null)
			myLock.setStatus(status);
	}
	
	private void refreshCaches()
	{
		LoadingAppDAI loadingAppDao = theDb.makeLoadingAppDAO();
		ComputationDAI computationDAO = theDb.makeComputationDAO();
		TsGroupDAI tsGroupDAO = theDb.makeTsGroupDAO();
		try
		{
			info("Refreshing TSID Cache...");
			timeSeriesDAO.reloadTsIdCache();
			dumpTsidCache();
			
			info("Refreshing Computation Cache...");
			enabledCompCache.clear();
			
			CompFilter enabledOnly = new CompFilter();
			enabledOnly.setEnabledOnly(true);
			ArrayList<DbComputation> comps = computationDAO.listCompsForGUI(enabledOnly);
			for(DbComputation comp : comps)
			{
				expandComputationInputs(comp);
				enabledCompCache.add(comp);
			}
			
			info("After loading, " + enabledCompCache.size()
				+ " computations in cache.");

			info("Refreshing Group Cache...");
			tsGroupDAO.fillCache();

			info("Expanding Groups in Cache...");
			groupHelper.evalAll();
			
			info("Reloading CP_COMP_DEPENDS Cache...");
			reloadCpCompDependsCache();
		}
		catch (Exception ex)
		{
			String msg = "Error refreshing caches: " + ex;
			Logger.instance().failure(msg);
			System.err.println(msg);
			ex.printStackTrace(System.err);
		}
		finally
		{
			tsGroupDAO.close();
			loadingAppDao.close();
			computationDAO.close();
		}
		lastCacheRefresh = System.currentTimeMillis();
	}
	

	/**
	 * Processes a CP_DEPENDS_NOTIFY record.
	 * @param ccdn
	 */
	private void processNotify(CpDependsNotify ccdn)
	{
		info("Processing: " + ccdn);
		boolean success = false;
		notifyTime = ccdn.getDateTimeLoaded();
		
		switch(ccdn.getEventType())
		{
		case CpDependsNotify.TS_CREATED:
			success = tsCreated(ccdn.getKey());
			break;
		case CpDependsNotify.TS_DELETED:
			success = tsDeleted(ccdn.getKey());
			break;
		case CpDependsNotify.TS_MODIFIED:
			tsDeleted(ccdn.getKey());
			success = tsCreated(ccdn.getKey());
			break;
		case CpDependsNotify.CMP_MODIFIED:
			success = compModified(ccdn.getKey());
			break;
		case CpDependsNotify.GRP_MODIFIED:
			success = groupModified(ccdn.getKey());
			break;
		case CpDependsNotify.FULL_EVAL:
			success = fullEval();
			break;
		}
		if (success)
			done++;
		else
			errs++;
		Logger.instance().debug1("End of nofity processing, success=" + success); 
	}
	
	private boolean tsCreated(DbKey tsKey)
	{
		TimeSeriesDAI timeSeriesDAO = theDb.makeTimeSeriesDAO();
		TsGroupDAI tsGroupDAO = theDb.makeTsGroupDAO();
		try
		{
			TimeSeriesIdentifier tsid = null;
			try { tsid = timeSeriesDAO.getTimeSeriesIdentifier(tsKey); }
			catch (NoSuchObjectException e)
			{
				warning("Received TS_CREATED message for TS Key="
					+ tsKey + " which does not exist in the DB -- assuming deleted.");
				return tsDeleted(tsKey);
			}
			// Note: the get method above will automaticall add it to the cache.
			dumpTsidCache();

			// Adjust the groups in my cache which may include this new time series.
			groupHelper.checkGroupMembership(tsid);

			// Determine computations that will use this new TS as input
			toAdd.clear();
			for(DbComputation comp : enabledCompCache)
			{
				if (comp.getGroupId() == Constants.undefinedId)
				{
					for(Iterator<DbCompParm> parmit = comp.getParms(); parmit.hasNext(); )
					{
						DbCompParm parm = parmit.next();
						if (!parm.isInput())
							continue;
						if (tsid.matchesParm(parm))
						{
							addCompDepends(tsid.getKey(), comp.getId());
							break;
						}
					}
				}
				else // This is a group computation
				{
					// Go through the expanded list of TSIDs in the group. Transform each
					// one by the input parms. If it then matches the passed tsid, then
					// this computation is a dependency.
					TsGroup grp = tsGroupDAO.getTsGroupById(comp.getGroupId());
					if (grp == null)
					{
						warning("Computation ID=" + comp.getId() + " '" + comp.getName() 
							+ "' has an invalid group ID. Skipping.");
						continue;
					}
				nextTsid:
					for(TimeSeriesIdentifier grpTsid : grp.getExpandedList())
					{
						for(Iterator<DbCompParm> parmit = comp.getParms(); parmit.hasNext(); )
						{
							DbCompParm parm = parmit.next();
							if (!parm.isInput())
								continue;
							TimeSeriesIdentifier grpTsidCopy = grpTsid.copyNoKey();
							theDb.transformUniqueString(grpTsidCopy, parm);
							if (tsid.getUniqueString().equalsIgnoreCase(grpTsidCopy.getUniqueString()))
							{
								addCompDepends(tsid.getKey(), comp.getId());
								break nextTsid;
							}
						}
					}
				}
			}

			writeToAdd2Db(Constants.undefinedId);
			return true;
		}
		catch (DbIoException ex)
		{
			String msg = "Error processing TS_CREATED for key=" + tsKey + ": " + ex;
			warning(msg);
			System.err.println(msg);
			ex.printStackTrace(System.err);
			return false;
		}
		finally
		{
			tsGroupDAO.close();
			timeSeriesDAO.close();
		}
	}

	
	/**
	 * The time series with the passed key has been removed from the database.
	 * All we do is remove it from our cache and dump it to a file if opted.
	 * @param tsKey the time series key.
	 */
	private boolean tsDeleted(DbKey tsKey)
	{
		TsGroupDAI tsGroupDAO = theDb.makeTsGroupDAO();
		String q = "";
		try
		{
			TimeSeriesIdentifier tsidRemoved = timeSeriesDAO.getCache().getByKey(tsKey);
			if (tsidRemoved != null)
				timeSeriesDAO.getCache().remove(tsKey);
			dumpTsidCache();
			
			// Remove this key from all groups
			for(TsGroup grp : tsGroupDAO.getTsGroupList(null))
			{
				if (!grp.getIsExpanded()) // may have timed out in the cache and reread from db.
					groupHelper.expandTsGroup(grp);
				
				boolean wasMember = false;
				for(Iterator<TimeSeriesIdentifier> tsidit = 
					grp.getExpandedList().iterator(); tsidit.hasNext(); )
				{
					TimeSeriesIdentifier tsid = tsidit.next();
					if (tsid.getKey() == tsKey)
					{
						tsidit.remove();
						wasMember = true;
						break;
					}
				}
				
				// If this was an explicit member of a group, remove the TSDB_GROUP_MEMBER_TS record
				if (wasMember)
					for(TimeSeriesIdentifier tsid : grp.getTsMemberList())
					{
						if (tsid.getKey() == tsKey)
						{
							grp.getTsMemberList().remove(tsid);
							q = "delete from TSDB_GROUP_MEMBER_TS where "
								+ "GROUP_ID = " + grp.getGroupId()
								+ " and "
								+ (theDb.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_9 ? "ts_id" : "data_id")
								+ " = " + tsKey;
							try
							{
								theDb.doModify(q);
							}
							catch (DbIoException ex)
							{
								String msg = "tsDeleted Error in query '" + q + "': " + ex;
								System.err.print(msg);
								ex.printStackTrace();
							}
							break;
						}
					}
			}
			
			// Remove this key from all comp-dependencies
			for(Iterator<CpCompDependsRecord> compDependsIt = cpCompDependsCache.iterator();
				compDependsIt.hasNext(); )
			{
				CpCompDependsRecord compDepends = compDependsIt.next();
				if (compDepends.getTsKey() == tsKey)
				{
					// Remove this record from the CpCompDepends cache.
					compDependsIt.remove();
					computationTsDeleted(compDepends, tsidRemoved);
				}
			}
	
			// Delete from cp_comp_depends table any tupple with this ts_id.
			q = "delete from CP_COMP_DEPENDS " + "where TS_ID = " + tsKey;

			theDb.doModify(q);
			return true;
		}
		catch (DbIoException ex)
		{
			String msg = "tsDeleted Error in query '" + q + "': " + ex;
			System.err.print(msg);
			ex.printStackTrace();
			return false;
		}
		finally
		{
			tsGroupDAO.close();
		}
	}

	/**
	 * The passed TSID has been removed. Update the computation cache
	 * @param compDepends
	 * @param tsidRemoved
	 */
	private void computationTsDeleted(CpCompDependsRecord compDepends, 
		TimeSeriesIdentifier tsidRemoved)
	{
		ComputationDAI computationDAO = theDb.makeComputationDAO();
		
		try
		{
			// Find the computation in the cache & update it.
			DbComputation comp = this.getCompFromCache(compDepends.getCompId());
			if (comp == null)
				return;
	
			// If this was an explicit non-group dependency, then modify the CP_COMP_TS_PARM
			// record (set it's sdi to undefinedId), disable the comp, and remove from resolver.
			TsGroup grp = comp.getGroup();
			if (grp == null)
			{
				for(Iterator<DbCompParm> parmit = comp.getParms();
					parmit.hasNext(); )
				{
					DbCompParm parm = parmit.next();
					if (parm.isInput()
					 && tsidRemoved.matchesParm(parm))
					{
						parm.setSiteDataTypeId(Constants.undefinedId);
						if (comp.getMissingAction(parm.getRoleName()) 
							!= MissingAction.IGNORE)
						{
							enabledCompCache.remove(comp);
							comp.setEnabled(false);
							try
							{
								computationDAO.writeComputation(comp);
							}
							catch (DbIoException ex)
							{
								String msg = "tsDeleted() Error in writeComputation: " + ex;
								System.out.println(msg);
								ex.printStackTrace();
							}
						}
					}
				}
			}
		}
		finally
		{
			computationDAO.close();
		}
	}

	/**
	 * Called when a message received saying a comp was modified.
	 * @param compId
	 */
	private boolean compModified(DbKey compId)
	{
		DbComputation comp = null;
		info("Received COMP_MODIFIED for compId=" + compId);
		ComputationDAI computationDAO = theDb.makeComputationDAO();
		try
		{
			comp = computationDAO.getComputationById(compId);
			if (comp != null)
				expandComputationInputs(comp);
		}
		catch (DbIoException ex)
		{
			String msg = "Received COMP_MODIFIED for compId=" + compId 
				+ " but cannot read computation from DB: " + ex;
			warning(msg);
			System.err.println(msg);
			ex.printStackTrace();
			return false;
		}
		catch (NoSuchObjectException ex)
		{
			comp = null;
			info("Received COMP_MODIFIED for compId=" + compId
				+ " but it no longer exists in the DB -- assuming comp deleted.");
			// fall through
		}
		finally
		{
			computationDAO.close();
		}
		
		// Remove old copy of this computation from cached set of computations
		for(Iterator<DbComputation> compit = enabledCompCache.iterator();
			compit.hasNext(); )
		{
			DbComputation rcomp = compit.next();
			if (rcomp.getId().equals(compId))
			{
				info("Removed old copy of computation " + compId + " from the cache.");
				compit.remove();
				break; // can only be one
			}
		}
		
		// Remove all old dependencies for this computation.
		for(Iterator<CpCompDependsRecord> ccdit = cpCompDependsCache.iterator();
			ccdit.hasNext(); )
		{
			CpCompDependsRecord ccd = ccdit.next();
			if (ccd.getCompId().equals(compId))
				ccdit.remove();
		}

		// Only save enabled comps in the cache.
		if (comp != null && comp.isEnabled())
		{
			enabledCompCache.add(comp);
			evalComp(comp);
		}
		else
		{
			// Have to remove comp-depends for the now-disabled or deleted comp.
			String q = "DELETE FROM CP_COMP_DEPENDS WHERE COMPUTATION_ID = " + compId;
			info(q);
			try
			{
				theDb.doModify(q);
			}
			catch (DbIoException ex)
			{
				warning("Error in '" + q + "': " + ex);
			}
		}

		return true;
	}

	/** Called with an enabled computation */
	private void evalComp(DbComputation comp)
	{
		info("Evaluating dependencies for comp " + comp.getId() + " " + comp.getName());
		if (!doingFullEval)
			toAdd.clear();
		if (comp.isEnabled())
		{
			info("comp is enabled for appID=" + comp.getAppId());
			// If not a group comp just add the completely-specified parms.
			TsGroup grp = null;
			if (!DbKey.isNull(comp.getGroupId()))
			{
				TsGroupDAI tsGroupDAO = theDb.makeTsGroupDAO();
				try
				{
					grp = tsGroupDAO.getTsGroupById(comp.getGroupId());
				}
				catch (DbIoException ex)
				{
					warning("Computation ID=" + comp.getId() + " '" + comp.getName()
						+ "' uses invalid groupID=" + comp.getGroupId() + ". Ignoring.");
					return;
				}
				finally
				{
					tsGroupDAO.close();
				}
			}
			if (grp == null)
			{
				info("NOT a group comp");
				for(Iterator<DbCompParm> parmit = comp.getParms();
					parmit.hasNext(); )
				{
					DbCompParm parm = parmit.next();
					if (!parm.isInput())
						continue;
					// short-cut: for CWMS and Tempest, the SDI in the parm _is_
					// the time-series ID. so we don't have to look it up.
					DbKey tsKey = Constants.undefinedId;
					DataType dt = parm.getDataType();
					info("Checking input parm " + parm.getRoleName()
						+ " sdi=" + parm.getSiteDataTypeId() + " intv=" + parm.getInterval()
						+ " tabsel=" + parm.getTableSelector() + " modelId=" + parm.getModelId()
						+ " dt=" + (dt==null?"null":dt.getCode()) + " siteId=" + parm.getSiteId()
						+ " siteName=" + parm.getSiteName());
					if (theDb.isHdb())
					{
						// For HDB, the SDI is not the same as time series key.
						TimeSeriesIdentifier tmpTsid = new HdbTsId();
						theDb.transformUniqueString(tmpTsid, parm);
						info("After transform, param ID='" + tmpTsid.getUniqueString() + "'");
						TimeSeriesIdentifier tsid = timeSeriesDAO.getCache().getByUniqueName(
							tmpTsid.getUniqueString());
						if (tsid != null)
						{
							tsKey = tsid.getKey();
							info("From cache, this is TS_IS=" + tsKey);
						}
						else
							info("No such time-series in the cache.");
					}
					else
						tsKey = parm.getSiteDataTypeId();
					if (!tsKey.isNull())
						addCompDepends(tsKey, comp.getId());
				}
			}
			else // it is a group computation
			{
				info("IS a group comp with group " + grp.getGroupId() + " " + grp.getGroupName()
					+ " numExpandedMembers: " + grp.getExpandedList().size());

				// For each time series in the expanded list
				for(TimeSeriesIdentifier tsid : grp.getExpandedList())
				{
					info("Checking group tsid=" + tsid.getUniqueString());
					// for each input parm
					for(Iterator<DbCompParm> parmit = comp.getParms();
							parmit.hasNext(); )
					{
						DbCompParm parm = parmit.next();
						info("  parm '" + parm.getRoleName() + "'");
						if (!parm.isInput())
						{
							info("     - Not an input. Skipping.");
							continue;
						}
						// Transform the group TSID by the parm
						info("Checking input parm " + parm.getRoleName()
							+ " sdi=" + parm.getSiteDataTypeId() + " intv=" + parm.getInterval()
							+ " tabsel=" + parm.getTableSelector() + " modelId=" + parm.getModelId()
							+ " dt=" + parm.getDataType() + " siteId=" + parm.getSiteId()
							+ " siteName=" + parm.getSiteName());
						TimeSeriesIdentifier tmpTsid = tsid.copyNoKey();
						info("Triggering ts=" + tmpTsid.getUniqueString());
						theDb.transformUniqueString(tmpTsid, parm);
						TimeSeriesIdentifier parmTsid = 
							timeSeriesDAO.getCache().getByUniqueName(tmpTsid.getUniqueString());
						info("After transform, param ID='" + tmpTsid.getUniqueString() + "'");
						// If the transformed TSID exists, it is a dependency.
						if (parmTsid != null)
							addCompDepends(parmTsid.getKey(), comp.getId());
						else
							info("TS " + tmpTsid.getUniqueString() + " not in cache.");
					}
				}
			}
		}
		if (!doingFullEval)
		{
			try { writeToAdd2Db(comp.getId()); }
			catch(DbIoException ex) { /* do nothing -- err msg already logged. */ }
		}
	}
	
	private boolean groupModified(DbKey groupId)
	{
		info("groupModified(" + groupId + ")");
		ComputationDAI computationDAO = theDb.makeComputationDAO();
		TsGroupDAI tsGroupDAO = theDb.makeTsGroupDAO();
		try
		{
			TsGroup newGrp = null;
			try
			{
				newGrp = tsGroupDAO.getTsGroupById(groupId, true);
			}
			catch (DbIoException ex)
			{
				String msg = "groupModified(" + groupId + ") cannot get group: "
						+ ex;
				warning(msg);
				System.err.println(msg);
				ex.printStackTrace();
				newGrp = null;
				return false;
			}
	
			if (newGrp != null)
			{
	info("groupModified " + newGrp.getGroupId() + ":" + newGrp.getGroupName()
	+ " numSites=" + newGrp.getSiteIdList().size());
				info("Group " + newGrp.getGroupId() + ":" + newGrp.getGroupName()
					+ " Added/Replaced in cache, numSites=" + newGrp.getSiteIdList().size());

				groupHelper.expandTsGroup(newGrp);
			}
			else // Group was deleted.
			{
				info("groupModified(" + groupId + ") -- not in DB. Assuming group was deleted.");
			}
	
			// Any group that includes/excludes/intersects THIS group needs to have
			// its expanded list re-evaluated. I.e. "parent" groups.
			ArrayList<DbKey> affectedGroupIds = new ArrayList<DbKey>();
			groupHelper.evaluateParents(groupId, affectedGroupIds);
	
			// affectedGroupIds is now a list of all groups that may have had their
			// expanded list modified by the current operation.
			// Now any computation that uses any of these affected groups must be re-evaluated.
			
			ArrayList<DbKey> disabledCompIds = new ArrayList<DbKey>();
			for(Iterator<DbComputation> compit = enabledCompCache.iterator(); compit.hasNext();)
			{
				DbComputation comp = compit.next();
				if (comp.getGroupId() != Constants.undefinedId
				 && affectedGroupIds.contains(comp.getGroupId()))
				{
					// This computation is affected!
					TsGroup grp = tsGroupDAO.getTsGroupById(comp.getGroupId());
					if (grp == null) // means group was deleted
					{
						comp.setEnabled(false);
						comp.setGroupId(Constants.undefinedId);
						comp.setGroup(null);
						try
						{
							computationDAO.writeComputation(comp);
							disabledCompIds.add(comp.getId());
							compit.remove();
						}
						catch (DbIoException ex)
						{
							ex.printStackTrace();
						}
					}
					else // Re-evaluate comp depends because the underlying group is changed.
					{
						evalComp(comp);
					}
				}
			}
				
			// If any comps were disabled because the group was deleted, then
			// delete any comp-depends records.
			if (disabledCompIds.size() > 0)
			{
				StringBuilder q = new StringBuilder(
					"delete from cp_comp_depends where computation_id in(");
				for(int idx = 0; idx < disabledCompIds.size(); idx++)
				{
					if (idx > 0)
						q.append(", ");
					q.append(disabledCompIds.get(idx));
				}
				q.append(")");
				try
				{
					info(q.toString());
					theDb.doModify(q.toString(), true);
				}
				catch (DbIoException ex)
				{
					warning("Error in query '" + q.toString() + "': " + ex);
				}
			}
		}
		catch(DbIoException ex)
		{
			warning("groupModified groupID=" + groupId + ": " + ex);
		}
		finally
		{
			tsGroupDAO.close();
			computationDAO.close();
		}
		return true;
	}

	private boolean fullEval()
	{
		refreshCaches();
		
		// Set the doingFullEval flag which tells evalComp to simply
		// accumulate results in the scratchpad. Don't merge to CP_COMP_DEPENDS.
		String q = "clearing scratchpad.";
		try
		{
			toAdd.clear();
			doingFullEval = true;
			for(DbComputation comp : enabledCompCache)
				evalComp(comp);
			doingFullEval = false;

			// Insert all the toAdd records into the scratchpad
			clearScratchpad();
			for(CpCompDependsRecord ccd : toAdd)
			{
				q = "INSERT INTO CP_COMP_DEPENDS_SCRATCHPAD VALUES("
				  + ccd.getTsKey() + ", " + ccd.getCompId() + ")";
info(q);
				theDb.doModify(q);
			}
		
			// The scratchpad is now what we want CP_COMP_DEPENDS to look like.
			// Mark's 2-line SQL to move the scratchpad to CP_COMP_DEPENDS.
			q = "delete from cp_comp_depends "
			+ "where(computation_id, ts_id) in "
			+ "(select computation_id, ts_id from cp_comp_depends " +
				"minus select computation_id, ts_id from cp_comp_depends_scratchpad)";
			theDb.doModify(q, true);
		
			q = "insert into cp_comp_depends( computation_id, ts_id) "
				+ "(select computation_id, ts_id from cp_comp_depends_scratchpad "
				+ "minus select computation_id, ts_id from cp_comp_depends)";
			theDb.doModify(q, true);
			return true;
		}		
		catch(DbIoException ex)
		{
			String msg = "fullEval Error in '" + q + "': " + ex;
			warning(msg);
			System.err.println(msg);
			ex.printStackTrace();
			return false;
		}
	}
	
	private DbComputation getCompFromCache(DbKey compId)
	{
		for(DbComputation comp : enabledCompCache)
			if (compId.equals(comp.getId()))
				return comp;
		return null;
	}
	
	private void addCompDepends(DbKey tsKey, DbKey compId)
	{
		CpCompDependsRecord rec = new CpCompDependsRecord(tsKey, compId);
debug("addCompDepends(" + tsKey + ", " + compId + ") before, toAdd.size=" + toAdd.size());
		toAdd.add(rec);
debug("   after, toAdd.size=" + toAdd.size());
	}
	
	private void clearScratchpad()
		throws DbIoException
	{
		// Clear the scratchpad
		String q = "DELETE FROM CP_COMP_DEPENDS_SCRATCHPAD";
		try
		{
			info(q);
			theDb.doModify(q);
		}
		catch (DbIoException ex)
		{
			warning("Error in query '" + q + "': " + ex);
			throw ex;
		}
		
	}
	private void writeToAdd2Db(DbKey compId2Delete)
		throws DbIoException
	{
		if (toAdd.size() == 0)
			return;

		// Clear the scratchpad
		String q = "DELETE FROM CP_COMP_DEPENDS_SCRATCHPAD";
		try
		{
info(q);
			theDb.doModify(q);
			
			// Insert all the toAdd records into the scratchpad
			for(CpCompDependsRecord ccd : toAdd)
			{
				q = "INSERT INTO CP_COMP_DEPENDS_SCRATCHPAD VALUES("
				  + ccd.getTsKey() + ", " + ccd.getCompId() + ")";
info(q);
				theDb.doModify(q);
			}
			
			//TODO - Ideally, the delete and insert should be done as a transaction.
			
			if (compId2Delete != Constants.undefinedId)
			{
				q = "DELETE FROM CP_COMP_DEPENDS WHERE COMPUTATION_ID = " + compId2Delete;
info(q);
				theDb.doModify(q);
			}
			// Copy the scratchpad to the cp_comp_depends table
			q = "INSERT INTO CP_COMP_DEPENDS SELECT * FROM CP_COMP_DEPENDS_SCRATCHPAD";
info(q);
			theDb.doModify(q);
			
//			if (compId2Delete != Constants.undefinedId)
//				theDb.commit(); // This should terminate the transaction.
			
			// Now, since we deleted the deps at the start of the operation,
			// even if the dependency existed before treat it as a new dependency.
			// Enqueue all data for the time-series back to the notify time
			// as tasklist records.
//			createTaskListRecordsFor(toAdd);
// MJM: We discovered that creating tasklist records takes a very long
// time since we have to query r_instant (and other tables) by date_time_loaded
// and there is no index on date_time_loaded. Each time series was talking
// well over a minute to do the query. So punt for now.
		}
		catch (DbIoException ex)
		{
			warning("Error in query '" + q + "': " + ex);
			throw ex;
		}
	}
	
//	/**
//	 * Enqueue data for the added dependencies back to the notification time.
//	 * @param added list of dependencies just added.
//	 */
//	private void createTaskListRecordsFor(HashSet<CpCompDependsRecord> added)
//		throws DbIoException
//	{
//		for(CpCompDependsRecord dep : added)
//		{
//			TimeSeriesIdentifier tsid = theDb.tsIdCache.get(dep.getTsKey());
//			if (tsid == null)
//			{
//				warning("createTaskListRecordsFor invalid tsKey=" + dep.getTsKey());
//				continue;
//			}
//			
//			try
//			{
//				theDb.writeTasklistRecords(tsid, notifyTime);
//			}
//			catch (NoSuchObjectException ex)
//			{
//				warning("createTaskListRecordsFor cannot makeTimeSeries for "
//					+ tsid + ": " + ex);
//			}
//			catch (BadTimeSeriesException ex)
//			{
//				warning("createTaskListRecordsFor cannot fillTimeSeries for "
//					+ tsid + ": " + ex);
//			}
//		}
//	}

	/**
	 * Flush the cache and then load all the CP_COMP_DEPENDS records
	 * for my appId.
	 */
	private void reloadCpCompDependsCache()
	{
		cpCompDependsCache.clear();
		String q = "SELECT TS_ID, COMPUTATION_ID FROM CP_COMP_DEPENDS";
		
		try
		{
			ResultSet rs = theDb.doQuery(q);
			while (rs != null && rs.next())
			{
				CpCompDependsRecord rec = new CpCompDependsRecord(
					DbKey.createDbKey(rs, 1), DbKey.createDbKey(rs, 2));
				cpCompDependsCache.add(rec);
			}
		}
		catch (Exception ex)
		{
			warning("Error in query '" + q + "': " + ex);
			return;
		}
	}
	
	
	private void expandComputationInputs(DbComputation comp)
		throws DbIoException
	{
		// Input parameters must have the SDI's expanded
		for(Iterator<DbCompParm> parmit = comp.getParms();
			parmit.hasNext(); )
		{
			DbCompParm parm = parmit.next();
			if (parm.isInput() && parm.getSiteId() == Constants.undefinedId)
			{
//				info("Expanding input parm '" + parm.getRoleName() + "' in comp '" + comp.getName() + "'");
				try { theDb.expandSDI(parm); }
				catch(NoSuchObjectException ex)
				{
					// Do nothing, it may be a group parm with no SDI specified.
				}
//				info("After expanding, siteId=" + parm.getSiteId() + ", sitename='" + parm.getSiteName() + "'");
			}
		}
	}

	
	private void dumpTsidCache()
	{
		File dir = groupHelper.getGroupCacheDumpDir();
		if (dir != null)
		{
			File f = new File(dir, "tsids");
			PrintWriter pw = null;
			try
			{
				pw = new PrintWriter(f);
				for(Iterator<TimeSeriesIdentifier> tsidit = timeSeriesDAO.getCache().iterator();
					tsidit.hasNext(); )
					pw.println(tsidit.next());
				pw.close();
			}
			catch (IOException ex)
			{
				warning("Cannot save tsid dump to '" + f.getPath() + "': " + ex);
			}
			finally
			{
				if (pw != null)
					try { pw.close(); } catch(Exception ex) {}
			}
		}
	}

}

