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
package decodes.tsdb;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;

import opendcs.dai.CompDependsDAI;
import opendcs.dai.ComputationDAI;
import opendcs.dai.LoadingAppDAI;
import opendcs.dai.TimeSeriesDAI;
import opendcs.dai.TsGroupDAI;
import opendcs.dao.DaoBase;
import lrgs.gui.DecodesInterface;
import ilex.cmdline.BooleanToken;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import ilex.util.EnvExpander;
import ilex.util.TextUtil;
import decodes.sql.DbKey;
import decodes.util.CmdLineArgs;
import decodes.util.DecodesException;
import decodes.cwms.CwmsTimeSeriesDb;
import decodes.db.Constants;
import decodes.db.DataType;
import decodes.db.Database;
import decodes.hdb.HdbTsId;


/**
This is the main class for the daemon that updates CP_COMP_DEPENDS.
*/
public class CpCompDependsUpdater extends TsdbAppTemplate
{
    private static Logger log = OpenDcsLoggerFactory.getLogger();
    /** Holds app name, id, & description. */
    private CompAppInfo appInfo;

    /** My lock in the time-series database */
    private TsdbCompLock myLock;

    private boolean shutdownFlag;
    private String hostname;
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

    private CompEventSvr compEventSvr = null;

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

    /** Sets default app name (and log file) to compdepends */
    protected void addCustomArgs(CmdLineArgs cmdLineArgs)
    {
        appNameArg.setDefaultValue("compdepends");
        cmdLineArgs.addToken(fullEvalOnStartup);
        cmdLineArgs.addToken(groupCacheDump);
        cmdLineArgs.addToken(fullEvalOnly);
    }

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
            +", appId=" + getAppId();
        if (theDb.isCwms())
            msg = msg + ", officeID=" + ((CwmsTimeSeriesDb)theDb).getDbOfficeId();
        log.info(msg + " Starting ==============");


        groupHelper = theDb.makeGroupHelper();

        String dir = groupCacheDump.getValue();
        if (dir != null && dir.length() > 0)
        {
            String expDir = EnvExpander.expand(dir);
            File dumpDir = new File(expDir);
            if (!dumpDir.isDirectory()
             && !dumpDir.mkdirs())
            {
                log.warn("Cannot create group cache dump dir '{}'. -- Will not dump group cache.", expDir);
                dumpDir = null;
            }
            groupHelper.setGroupCacheDumpDir(dumpDir);
        }
        CpDependsNotify prevNotify = null;
        String action="";
        while(!shutdownFlag)
        {
            try(LoadingAppDAI loadingAppDAO = theDb.makeLoadingAppDAO();
                CompDependsDAI compDependsDAO = theDb.makeCompDependsDAO();)
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
                    log.info("Doing one-time full evaluation on startup");
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
                CpDependsNotify ccdn = compDependsDAO.getCpCompDependsNotify();
                if (ccdn != null)
                {
                    if (prevNotify != null && ccdn.equals(prevNotify))
                    {
                        log.info("Ignoring duplicate notify '{}'", ccdn);
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
                log.atError().setCause(ex).log("No Lock - Application exiting.");
                shutdownFlag = true;
            }
            catch(DbIoException ex)
            {
                log.atError().setCause(ex).log("Database Error while {}", action);
                shutdownFlag = true;
                databaseFailed = true;
            }
            catch(Exception ex)
            {
                log.atError().setCause(ex).log("Unexpected exception while {}", action);
                shutdownFlag = true;
                databaseFailed = true;
            }
        }
        closeDb();
    }

    /** Initialization phase -- any error is fatal. */
    private void initialize()
        throws LockBusyException, DbIoException, NoSuchObjectException
    {
        try (LoadingAppDAI loadingAppDao = theDb.makeLoadingAppDAO();)
        {
            appInfo = loadingAppDao.getComputationApp(getAppId());

            try { hostname = InetAddress.getLocalHost().getHostName(); }
            catch(Exception e) { hostname = "unknown"; }

            // If this process can be monitored, start an Event Server.
            if (TextUtil.str2boolean(appInfo.getProperty("monitor")) && compEventSvr == null)
            {
                try
                {
                    compEventSvr = new CompEventSvr(determineEventPort(appInfo));
                    compEventSvr.startup();
                }
                catch (IOException ex)
                {
                    log.atError()
                       .setCause(ex)
                       .log("Cannot create Event server -- no events available to external clients.");
                }
            }
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
        try    (
            TimeSeriesDAI timeSeriesDAO = theDb.makeTimeSeriesDAO();
            LoadingAppDAI loadingAppDao = theDb.makeLoadingAppDAO();
            ComputationDAI computationDAO = theDb.makeComputationDAO();
            TsGroupDAI tsGroupDAO = theDb.makeTsGroupDAO();
            )
        {
            dumpTsidCache();

            log.info("Refreshing Computation Cache...");
            enabledCompCache.clear();

            CompFilter enabledOnly = new CompFilter();
            enabledOnly.setEnabledOnly(true);
            ArrayList<DbComputation> comps = computationDAO.listCompsForGUI(enabledOnly);
            for(DbComputation comp : comps)
            {
                // Skip timed computations.
                if (comp.getProperty("timedCompInterval") != null)
                    continue;
                expandComputationInputs(comp);
                enabledCompCache.add(comp);
            }

            log.info("After loading, {} computation in cache.", enabledCompCache.size());

            log.info("Refreshing Group Cache...");
            tsGroupDAO.fillCache();

            log.info("Expanding Groups in Cache...");
            groupHelper.evalAll();

            log.info("Reloading CP_COMP_DEPENDS Cache...");
            reloadCpCompDependsCache();
        }
        catch (Exception ex)
        {
            log.atError().setCause(ex).log("Error refreshing caches");
        }
        lastCacheRefresh = System.currentTimeMillis();
    }


    /**
     * Processes a CP_DEPENDS_NOTIFY record.
     * @param ccdn
     */
    private void processNotify(CpDependsNotify ccdn)
    {
        log.info("Processing: {}", ccdn);
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
        case CpDependsNotify.TS_CODE_CHANGED:
            log.warn("Received TS_CODE_CHANGE notification for (new) code={} -- Not supported.", ccdn.getKey());
        }
        if (success)
            done++;
        else
            errs++;

        log.debug("End of notify processing, success={}", success);
    }

    private boolean tsCreated(DbKey tsKey)
    {
        log.info("Received TS_CREATED message for TS Key=" + tsKey);

        try(TimeSeriesDAI timeSeriesDAO = theDb.makeTimeSeriesDAO();
            TsGroupDAI tsGroupDAO = theDb.makeTsGroupDAO();)
        {
            TimeSeriesIdentifier tsid = null;
            try
            {
                tsid = timeSeriesDAO.getTimeSeriesIdentifier(tsKey);
            }
            catch (NoSuchObjectException ex)
            {
                log.atWarn()
                   .setCause(ex)
                   .log("Received TS_CREATED message for TS Key={} which does not exist in the DB -- assuming deleted.", tsKey);
                return tsDeleted(tsKey);
            }
            // Note: the get method above will automatically add it to the cache.
            dumpTsidCache();

            // Adjust the groups in my cache which may include this new time series.
            log.trace("tsCreated - checking group membership for {}", tsid.getUniqueString());
            groupHelper.checkGroupMembership(tsid);

            // Determine computations that will use this new TS as input
            toAdd.clear();
            for(DbComputation comp : enabledCompCache)
            {
                // Either not enabled or a timed computation
                if (!comp.isEnabled() || comp.getProperty("timedCompInterval") != null)
                    continue;

                if (comp.getGroupId() == Constants.undefinedId)
                {
                    log.trace("Considering non-group comp {}: {}", comp.getId(), comp.getName());
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
                        log.warn("Computation ID={} '{}' has an invalid group ID. Skipping.",
                                 comp.getId(), comp.getName());
                        continue;
                    }
                    log.atTrace().log(() -> "Considering group comp " + comp.getId() + ": " + comp.getName()
                                          + " which uses group " + grp.getGroupId() + " " + grp.getGroupName()
                                          + " with " + grp.getExpandedList().size() + " tsids in the expanded list.");

                nextTsid:
                    for(TimeSeriesIdentifier grpTsid : grp.getExpandedList())
                    {
                        log.trace(" ... Trying group tsid {} {}", grpTsid.getKey(), grpTsid.getUniqueString());
                        for(Iterator<DbCompParm> parmit = comp.getParms(); parmit.hasNext(); )
                        {
                            DbCompParm parm = parmit.next();
                            if (!parm.isInput())
                                continue;
                            TimeSeriesIdentifier grpTsidCopy = grpTsid.copyNoKey();
                            theDb.transformUniqueString(grpTsidCopy, parm);
                            if (tsid.getUniqueString().equalsIgnoreCase(grpTsidCopy.getUniqueString()))
                            {
                                log.trace(" ... MATCH: After morphing tsid={}", tsid.getUniqueString());
                                addCompDepends(tsid.getKey(), comp.getId());
                                break nextTsid;
                            }
                        }
                    }
                }
            }

            int n = toAdd.size();
            writeToAdd2Db(Constants.undefinedId);
            log.debug("{} computations will be triggered by this new time series.", n);

            // If the new TS triggers one or more computations, there may be new values
            // written before the dependency was created above. Re-write the data
            // for these values because the trigger (or queue handler for CWMS) would have
            // missed them.
            if (n > 0)
            {
                try
                {
                    CTimeSeries cts = theDb.makeTimeSeries(tsid);
                    timeSeriesDAO.fillTimeSeries(cts, null, null);
                    timeSeriesDAO.saveTimeSeries(cts);
                }
                catch (NoSuchObjectException ex)
                {
                    log.atWarn()
                       .setCause(ex)
                       .log("Bad TSID received in create notification: {}", tsid.getUniqueString());
                }
                catch (BadTimeSeriesException ex)
                {
                    log.atWarn()
                       .setCause(ex)
                       .log("Error reading data for new time series '{}'", tsid.getUniqueString());
                }
                catch(DbIoException ex)
                {
                    log.atWarn()
                       .setCause(ex)
                       .log("Error rewriting time series data for '{}'", tsid.getUniqueString());
                }
            }

            return true;
        }
        catch (DbIoException ex)
        {
            log.atWarn().setCause(ex).log("Error processing TS_CREATED for key={}", tsKey);
            return false;
        }
    }


    /**
     * The time series with the passed key has been removed from the database.
     * All we do is remove it from our cache and dump it to a file if opted.
     * @param tsKey the time series key.
     */
    private boolean tsDeleted(DbKey tsKey)
    {
        String q = "";
        try    (Connection conn = theDb.getConnection();
             TimeSeriesDAI timeSeriesDAO = theDb.makeTimeSeriesDAO();
             TsGroupDAI tsGroupDAO = theDb.makeTsGroupDAO();
             CompDependsDAI compDependsDao = theDb.makeCompDependsDAO();
             DaoBase dao = new DaoBase(theDb, "tsDeleted", conn);
            )
        {
            timeSeriesDAO.setManualConnection(conn);
            tsGroupDAO.setManualConnection(conn);

            TimeSeriesIdentifier tsidRemoved = timeSeriesDAO.getCache().getByKey(tsKey);
            if (tsidRemoved != null)
            {
                timeSeriesDAO.getCache()
                             .remove(tsKey);
            }
            dumpTsidCache();

            // Remove this key from all groups
            for(TsGroup grp : tsGroupDAO.getTsGroupList(null))
            {
                if (!grp.getIsExpanded()) // may have timed out in the cache and reread from db.
                {
                    groupHelper.expandTsGroup(grp);
                }

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
                {
                    for(TimeSeriesIdentifier tsid : grp.getTsMemberList())
                    {
                        if (tsid.getKey() == tsKey)
                        {
                            grp.getTsMemberList().remove(tsid);
                            q = "delete from TSDB_GROUP_MEMBER_TS where "
                                + "GROUP_ID = ?"
                                + " and "
                                + (theDb.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_9 ? "ts_id" : "data_id")
                                + " = ?";
                            try
                            {
                                dao.doModify(q,grp.getGroupId(), tsKey);
                            }
                            catch (SQLException ex)
                            {
                                log.atWarn()
                                   .setCause(ex)
                                   .log("tsDeleted Error in query '{}', for id '{}' because: {}",
                                                q,tsKey.getValue(), ex.getLocalizedMessage());
                            }
                            break;
                        }
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

            compDependsDao.deleteCompDependsForTsKey(tsKey);
            return true;
        }
        catch (SQLException | DbIoException ex)
        {
            log.atWarn()
               .setCause(ex)
               .log("tsDeleted Error in query '{}', for id '{}' because: {}",
                    q,tsKey.getValue(), ex.getLocalizedMessage());
            return false;
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
        try (ComputationDAI computationDAO = theDb.makeComputationDAO();)
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
                                log.atWarn().setCause(ex).log("tsDeleted() Error in writeComputation.");
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Called when a message received saying a comp was modified.
     * @param compId
     */
    private boolean compModified(DbKey compId)
    {
        DbComputation comp = null;
        log.info("Received COMP_MODIFIED for compId={}", compId);

        try (ComputationDAI computationDAO = theDb.makeComputationDAO();)
        {
            comp = computationDAO.getComputationById(compId);
            if (comp != null)
            {
                // MJM 20181029 if a Timed computation, then don't compute any inputs
                if (comp.getProperty("timedCompInterval") != null)
                {
                    // remove any dependencies for this comp ID
                    // setting comp to null will cause this to happen below
                    comp = null;
                    log.info("This is a timed computation. No dependencies will be created.");
                }
                else // interval==null means normal triggered comp.
                    expandComputationInputs(comp);
            }
        }
        catch (DbIoException ex)
        {
            log.atWarn()
               .setCause(ex)
               .log("Received COMP_MODIFIED for compId={} but cannot read computation from DB.", compId);
            return false;
        }
        catch (NoSuchObjectException ex)
        {
            comp = null;
            log.atInfo()
               .setCause(ex)
               .log("Received COMP_MODIFIED for compId={} but it no longer " +
                    "exists in the DB -- assuming comp deleted.", compId);
            // fall through
        }

        // Remove old copy of this computation from cached set of computations
        for(Iterator<DbComputation> compit = enabledCompCache.iterator(); compit.hasNext(); )
        {
            DbComputation rcomp = compit.next();
            if (rcomp.getId().equals(compId))
            {
                log.info("Removed old copy of computation {} from the cache.", compId);
                compit.remove();
                break; // can only be one
            }
        }

        // Remove all old dependencies for this computation.
        for(Iterator<CpCompDependsRecord> ccdit = cpCompDependsCache.iterator(); ccdit.hasNext(); )
        {
            CpCompDependsRecord ccd = ccdit.next();
            if (ccd.getCompId().equals(compId))
            {
                ccdit.remove();
            }
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
            try (CompDependsDAI compDependsDAO = theDb.makeCompDependsDAO();)
            {
                compDependsDAO.deleteCompDependsForCompId(compId);
            }
            catch (DbIoException ex)
            {
                log.atWarn().setCause(ex).log("Error removing dependency entries for disabled computation.");
            }
        }

        return true;
    }

    /** Called with an enabled computation */
    public void evalComp(DbComputation comp)
    {
        log.info("Evaluating dependencies for comp {}: {}", comp.getId(), comp.getName());
        if (!doingFullEval)
        {
            toAdd.clear();
        }
        try (TimeSeriesDAI timeSeriesDAO = theDb.makeTimeSeriesDAO();
             TsGroupDAI tsGroupDAO = theDb.makeTsGroupDAO();)
        {
            if (comp.isEnabled() && comp.getProperty("timedCompInterval") == null)
            {
                log.info("comp is enabled for appID={}", comp.getAppId());
                // If not a group comp just add the completely-specified parms.
                TsGroup grp = null;
                if (!DbKey.isNull(comp.getGroupId()))
                {
                    try
                    {
                        grp = tsGroupDAO.getTsGroupById(comp.getGroupId());
                    }
                    catch (DbIoException ex)
                    {
                        log.atWarn()
                           .setCause(ex)
                           .log("Computation ID={} '{}' uses invalid groupID={}. Ignoring.",
                                comp.getId(), comp.getName(), comp.getGroupId());
                        return;
                    }
                }
                if (grp == null)
                {
                    log.info("NOT a group comp");
                    for(Iterator<DbCompParm> parmit = comp.getParms();
                        parmit.hasNext(); )
                    {
                        DbCompParm parm = parmit.next();
                        log.info("Checking {}", parm.getRoleName());
                        if (!parm.isInput())
                            continue;
                        // short-cut: for CWMS, the SDI in the parm _is_
                        // the time-series ID. so we don't have to look it up.
                        DbKey tsKey = Constants.undefinedId;
                        DataType dt = parm.getDataType();
                        log.atInfo()
                           .log(() -> "Checking input parm " + parm.getRoleName()
                                    + " sdi=" + parm.getSiteDataTypeId() + " intv=" + parm.getInterval()
                                    + " tabsel=" + parm.getTableSelector() + " modelId=" + parm.getModelId()
                                    + " dt=" + (dt==null?"null":dt.getCode()) + " siteId=" + parm.getSiteId()
                                    + " siteName=" + parm.getSiteName());
                        if (theDb.isHdb())
                        {
                            // For HDB, the SDI is not the same as time series key.
                            TimeSeriesIdentifier tmpTsid = new HdbTsId();
                            theDb.transformUniqueString(tmpTsid, parm);
                            log.info("After transform, param ID='{}'", tmpTsid.getUniqueString());
                            TimeSeriesIdentifier tsid = timeSeriesDAO.getCache().getByUniqueName(
                                tmpTsid.getUniqueString());
                            if (tsid != null)
                            {
                                tsKey = tsid.getKey();
                                log.info("From cache, this is TS_IS={}", tsKey);
                            }
                            else
                                log.info("No such time-series in the cache.");
                        }
                        else
                            tsKey = parm.getSiteDataTypeId();
                        if (!tsKey.isNull())
                            addCompDepends(tsKey, comp.getId());
                    }
                }
                else // it is a group computation
                {
                    // The cached version may have been too old and a fresh copy read from the DB.
                    // If so, it needs to be expanded before use.
                    if (!grp.getIsExpanded())
                    {
                        try { groupHelper.expandTsGroup(grp); }
                        catch(DbIoException ex)
                        {
                            log.atError()
                               .setCause(ex)
                               .log("Cannot evaluate group ID={} '{}'... Therefore " +
                                    "cannot evaluation computation '{}'",
                                    grp.getKey(), grp.getGroupName(), comp.getName());
                            return;
                        }
                    }
                    log.info("IS a group comp with group {} {}" +
                             "numExpandedMembers: {}",
                             grp.getGroupId(), grp.getGroupName(), grp.getExpandedList().size());

                    // For each time series in the expanded list
                    for(TimeSeriesIdentifier tsid : grp.getExpandedList())
                    {
                        log.trace("Checking group tsid={}", tsid.getUniqueString());
                        // for each input parm
                        for(Iterator<DbCompParm> parmit = comp.getParms();
                                parmit.hasNext(); )
                        {
                            DbCompParm parm = parmit.next();
                            log.trace("parm '{}'", parm.getRoleName());
                            if (!parm.isInput())
                            {
                                log.trace("Not an input. Skipping.");
                                continue;
                            }
                            // Transform the group TSID by the parm
                            log.atTrace()
                               .log(() -> "Checking input parm " + parm.getRoleName()
                                        + " sdi=" + parm.getSiteDataTypeId() + " intv=" + parm.getInterval()
                                        + " tabsel=" + parm.getTableSelector() + " modelId=" + parm.getModelId()
                                        + " dt=" + parm.getDataType() + " siteId=" + parm.getSiteId()
                                        + " siteName=" + parm.getSiteName());
                            TimeSeriesIdentifier tmpTsid = tsid.copyNoKey();
                            log.trace("Triggering ts={}", tmpTsid.getUniqueString());
                            theDb.transformUniqueString(tmpTsid, parm);
                            log.trace("After transform, param ID='{}'", tmpTsid.getUniqueString());
                            TimeSeriesIdentifier parmTsid =
                                timeSeriesDAO.getCache().getByUniqueName(tmpTsid.getUniqueString());
                            // If the transformed TSID exists, it is a dependency.
                            if (parmTsid != null)
                                addCompDepends(parmTsid.getKey(), comp.getId());
                            else
                                log.trace("TS {} not in cache.", tmpTsid.getUniqueString());
                        }
                    }
                }
            }
            if (!doingFullEval)
            {
                try
                {
                    writeToAdd2Db(comp.getId());
                }
                catch(DbIoException ex)
                {
                    log.atWarn().setCause(ex).log("Error adding computation to staging area.");
                }
            }
        }
    }

    private boolean groupModified(DbKey groupId)
    {
        log.info("groupModified({})", groupId);

        try (ComputationDAI computationDAO = theDb.makeComputationDAO();
             TsGroupDAI tsGroupDAO = theDb.makeTsGroupDAO();
             CompDependsDAI compDependsDAO = theDb.makeCompDependsDAO();)
        {
            TsGroup newGrp = null;
            try
            {
                newGrp = tsGroupDAO.getTsGroupById(groupId, true);
            }
            catch (DbIoException ex)
            {
                log.atWarn().setCause(ex).log("groupModified({}) cannot get group: ", groupId);
                newGrp = null;
                return false;
            }

            if (newGrp != null)
            {
                log.info("groupModified {}:{} numSites={}",
                         newGrp.getGroupId(), newGrp.getGroupName(), newGrp.getSiteIdList().size());
                log.info("Group {}:{} Added/Replaced in cache, numSites={}",
                         newGrp.getGroupId(), newGrp.getGroupName(), newGrp.getSiteIdList().size());

                groupHelper.expandTsGroup(newGrp);
            }
            else // Group was deleted.
            {
                log.info("groupModified({}) -- not in DB. Assuming group was deleted.", groupId);
                // Note: call to getTsGroupById above will have removed it from the cache.
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
                if (!comp.isEnabled() || comp.getProperty("timedCompInterval") != null)
                {
                    continue;
                }

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
                            log.atWarn().setCause(ex).log("Unable to write computation.");
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
                    log.info(q.toString());
                    tsGroupDAO.doModify(q.toString());
                }
                catch (DbIoException ex)
                {
                    log.atWarn().setCause(ex).log("Error in query '{}'", q.toString());
                }
            }
        }
        catch(DbIoException ex)
        {
            log.atWarn().setCause(ex).log("groupModified groupID={}", groupId);
        }
        return true;
    }

    private boolean fullEval()
    {
        log.info("fullEval()");
        refreshCaches();

        // Set the doingFullEval flag which tells evalComp to simply
        // accumulate results in the scratchpad. Don't merge to CP_COMP_DEPENDS.
        try (CompDependsDAI compDepends = theDb.makeCompDependsDAO();)
        {
            toAdd.clear();
            doingFullEval = true;
            for(DbComputation comp : enabledCompCache)
            {
                evalComp(comp);
            }
            doingFullEval = false;

            // Insert all the toAdd records into the scratchpad
            compDepends.transaction(dao ->
            {
                log.info("Clearing scratch pad.");
                compDepends.clearScratchpad();
                log.info("Adding records to scratch pad.");
                compDepends.addRecordsToScratchPad(toAdd);
                log.info("Merging scratch pad to active.");
                compDepends.mergeScratchPadToActive();
            });
            return true;
        }
        catch(DbIoException ex)
        {
            log.atWarn()
               .setCause(ex)
               .log("Error during full eval.");
            return false;
        }
    }

    private DbComputation getCompFromCache(DbKey compId)
    {
        for(DbComputation comp : enabledCompCache)
        {
            if (compId.equals(comp.getId()))
            {
                return comp;
            }
        }
        return null;
    }

    protected void addCompDepends(DbKey tsKey, DbKey compId)
    {
        CpCompDependsRecord rec = new CpCompDependsRecord(tsKey, compId);
        log.debug("addCompDepends({}, {}) before, toAdd.size={}", tsKey, compId, toAdd.size());
        toAdd.add(rec);
    }

    protected void writeToAdd2Db(DbKey compId2Delete) throws DbIoException
    {
        if (toAdd.size() == 0)
        {
            return;
        }
        try(CompDependsDAI compDependsDAO = theDb.makeCompDependsDAO();)
        {
            compDependsDAO.transaction(dao ->
            {
                // Clear the scratchpad
                dao.clearScratchpad();
                if (compId2Delete != Constants.undefinedId)
                {
                    dao.deleteCompDependsForCompId(compId2Delete);
                }
                // We do this after as deleteCompDependsForCompId
                // removes from both cp_comp_depends and cp_comp_depends_scratchpad
                dao.addRecordsToScratchPad(toAdd);
                // Just in case, delete any records from the scratchpad that are already
                // in compdepends:
                dao.removeExistingFromScratch();
                dao.fillActiveFromScratch();
                // Finally, clear the scratchpad, otherwise this can leave a foreign key to TS_ID
                // that may prevent time series from being deleted.
                dao.clearScratchpad();

                // Now, since we deleted the deps at the start of the operation,
                // even if the dependency existed before treat it as a new dependency.
                // Enqueue all data for the time-series back to the notify time
                // as tasklist records.
                //            createTaskListRecordsFor(toAdd);
                // MJM: We discovered that creating tasklist records takes a very long
                // time since we have to query r_instant (and other tables) by date_time_loaded
                // and there is no index on date_time_loaded. Each time series was talking
                // well over a minute to do the query. So punt for now.
            });
        }
        catch (DbIoException ex)
        {
            throw new DbIoException("Error in adjusting compdepends tables.", ex);
        }
    }


    /**
     * Flush the cache and then load all the CP_COMP_DEPENDS records
     * for my appId.
     */
    private void reloadCpCompDependsCache()
    {
        cpCompDependsCache.clear();
        try (CompDependsDAI compDepends = theDb.makeCompDependsDAO();)
        {
            cpCompDependsCache.addAll(compDepends.getAllCompDependsEntries());
        }
        catch (Exception ex)
        {
            log.atWarn().setCause(ex).log("Unable to reload CompDepends Cache.");
            return;
        }
    }

    private void expandComputationInputs(DbComputation comp)
        throws DbIoException
    {
        // Input parameters must have the SDI's expanded
        for(Iterator<DbCompParm> parmit = comp.getParms(); parmit.hasNext(); )
        {
            DbCompParm parm = parmit.next();
            if (parm.isInput() && parm.getSiteId() == Constants.undefinedId)
            {
                try
                {
                    theDb.expandSDI(parm);
                }
                catch(NoSuchObjectException ex)
                {
                    log.atWarn()
                       .setCause(ex)
                       .log("Error Expanding Parameter information. (NOTE: ignore if group computation.)");
                }
            }
        }
    }


    private void dumpTsidCache()
    {
        try (TimeSeriesDAI timeSeriesDAO = theDb.makeTimeSeriesDAO() )
        {
            File dir = groupHelper.getGroupCacheDumpDir();
            if (dir != null)
            {
                File f = new File(dir, "tsids");

                try (PrintWriter pw = new PrintWriter(f))
                {
                    for(Iterator<TimeSeriesIdentifier> tsidit = timeSeriesDAO.getCache().iterator(); tsidit.hasNext();)
                    {
                        pw.println(tsidit.next());
                    }
                }
                catch (IOException ex)
                {
                    log.atWarn().setCause(ex).log("Cannot save tsid dump to '{}'", f.getPath());
                }
            }
        }
    }

    public HashSet<CpCompDependsRecord> getToAdd()
    {
        return toAdd;
    }

}