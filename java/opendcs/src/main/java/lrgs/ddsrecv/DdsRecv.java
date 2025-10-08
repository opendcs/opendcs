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
package lrgs.ddsrecv;

import ilex.util.EnvExpander;
import ilex.util.IDateFormat;

import java.io.File;
import java.util.Date;
import java.util.Iterator;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.slf4j.MDC.MDCCloseable;

import lrgs.archive.MsgArchive;
import lrgs.common.BadConfigException;
import lrgs.common.DcpMsg;
import lrgs.common.DcpMsgFlag;
import lrgs.common.SearchCriteria;
import lrgs.lrgsmain.LrgsConfig;
import lrgs.lrgsmain.LrgsInputException;
import lrgs.lrgsmain.LrgsInputInterface;
import lrgs.lrgsmain.LrgsMain;

/**
 * This is the main class for the DDS Receive Module. The module uses two
 * threads:
 * <ol>
 * <li>The DdsRecvConList thread manages a list of connections. It tries to keep
 * a connection open to all specified DDS servers, periodically sending a ping
 * request to exercise the connection. It also periodically checks the DDS
 * Receiver configuration file and executes any changes it finds.</li>
 * <li>The DdsRecv (this class) thread keeps a real-time stream of incoming DCP
 * message going, using the highest priority active connection. When one
 * connection fails, it re-evaluates.</li>
 * </ol>
 */
public class DdsRecv extends Thread implements LrgsInputInterface
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
    /** Module name */
    public static String module = "DdsRecv";

    /** Event num meaning that no connections are configured. */
    public static final int EVT_NO_CONNECTIONS = 1;

    /** Event num meaning that connection failed. */
    public static final int EVT_CONNECTION_FAILED = 2;

    /** Event num meaning that connection failed. */
    public static final int EVT_BAD_CONFIG = 3;

    /** Manages the primary group connections */
    protected DdsRecvConList recvConList;

    /** Shutdown flag */
    protected boolean isShutdown;

    /** We check for config changes this often. */
    protected static final long cfgCheckTime = 30000L;

    /** The configuration is stored in this 'settings' object. */
    protected DdsRecvSettings ddsRecvSettings;

    /** Last time that settings were read, used to detect changes. */
    protected long lastConfigRead;

    /** LrgsMain provides links to other modules. */
    protected LrgsMain lrgsMain;

    /** Last time that a message was retrieved. */
    protected long lastMsgRecvTime;

    /** Messages are archived here. */
    protected MsgArchive msgArchive;

    /** Slot number for the parent DDS receiver. */
    protected int slot;

    /** current status code (see LrgsInputInterface) */
    protected int statusCode;

    /** Explanatory status string */
    protected String status;

    /** secondary flag */
    private boolean isSecondary = false;

    /** Manages the secondary group connections */
    protected DdsRecvConList recvSeconConList;

    private long enableTime = 0L;
    private long lastStatusTime = 0L;
    private int numLastHour = 0, numThisHour = 0;

    /**
     * Constructor
     *
     * @param lrgsMain
     *            the program main object.
     * @param msgArchive
     *            used to store incoming messages.
     */
    public DdsRecv(LrgsMain lrgsMain, MsgArchive msgArchive)
    {
        this.lrgsMain = lrgsMain;
        this.msgArchive = msgArchive;
        recvConList = new DdsRecvConList(lrgsMain);
        isShutdown = false;
        ddsRecvSettings = DdsRecvSettings.instance();
        lastConfigRead = 0L;
        slot = -1;
        statusCode = DL_INIT;
        status = "Initializing";
        lastMsgRecvTime = System.currentTimeMillis() - 3600000L;
    }

    /** Causes the entire DdsRecv module to shut down. */
    public void shutdown()
    {
        isShutdown = true;
        recvConList.shutdown();
        statusCode = DL_DISABLED;
        status = "Shutdown";
    }

    /**
     * Sets the last receive time. Called once prior to starting the thread.
     *
     * @param rt
     *            the last receive time, usually retrieved from the quality log.
     */
    public void setLastMsgRecvTime(long rt)
    {
        if (rt <= 0)
        {
            rt = System.currentTimeMillis() - 3600000L;
        }
        lastMsgRecvTime = rt;
        log.debug("{}: LastMsgRecvTime set to {}", module, lastMsgRecvTime);
    }

    /**
     * Thread run method
     */
    public void run()
    {
        try (MDCCloseable mdc = MDC.putCloseable("LrgsInput", module))
        {
            log.trace("starting.");

            checkConfig();

            recvConList.start();
            long lastCfgCheck = 0L;
            statusCode = DL_STRSTAT;
            status = "Active";
            while (!isShutdown)
            {
                if (System.currentTimeMillis() - lastCfgCheck > cfgCheckTime)
                {
                    checkConfig();

                    lastCfgCheck = System.currentTimeMillis();
                }
                if (LrgsConfig.instance().enableDdsRecv)
                {
                    status = "Active";
                    statusCode = DL_STRSTAT;
                    getSomeData();
                }
                else
                {
                    status = "Disabled";
                    statusCode = DL_DISABLED;
                    // Logger.instance().debug3(module + " not enabled.");
                    try { sleep(1000L); } catch (InterruptedException ex) {}
                }
            }
        }
    }

    /**
     * Internal method to get the next message and archive it.
     */
    protected void getSomeData()
    {
        // If we don't currently have a connection, get the highest priority
        // one from the list that's ready-to-go.
        DdsRecvConnection con = recvConList.currentConnection;
        if (con == null)
        {
            con = getConnection();
            if (con == null)
            {
                if (isSecondary)
                    log.trace("{}:{} No 'Secondary Group' DDS Connections available to receive data.",
                              module, EVT_NO_CONNECTIONS);
                else
                    log.trace(" No 'Primary Group' DDS Connections available to receive data.",
                              module, EVT_NO_CONNECTIONS);

                try { sleep(1000L); } catch (InterruptedException ex) {}
                return;
            }
            log.trace("{}:{} DDS Connections ARE available to receive data.", module, -EVT_NO_CONNECTIONS);
            // We now have a new connection. Send it the search criteria.
            SearchCriteria searchCrit = buildSearchCrit();
            log.debug("{} Sending searchcrit to connection '{}': {}",
                      module, con.getName(), searchCrit.toString());
            if (!con.sendSearchCriteria(searchCrit))
            {
                recvConList.currentConnection = null;
                return;
            }
        }
        try
        {
            DcpMsg dcpMsg = con.getDcpMsg();
            if (dcpMsg != null)
            {
                lastMsgRecvTime = System.currentTimeMillis();
                archiveMsg(dcpMsg, con);
                numThisHour++;
            }
            else
            // all caught up, pause for 1 sec.
            {
                allCaughtUp();
            }
        }
        catch (LrgsInputException ex)
        {
            if (!isShutdown)
            {
                log.atWarn()
                   .setCause(ex)
                   .log("{}- Connection to {} failed -- will switch to different connection.",
                       EVT_CONNECTION_FAILED, con.getName());
            }
            recvConList.currentConnection = null;
        }
    }

    /**
     * Template method to get a connection.
     *
     * @return a connection from the pool.
     */
    protected DdsRecvConnection getConnection()
    {
        return recvConList.getCurrentConnection();
    }

    /**
     * Template method to archive a message.
     *
     * @param dcpMsg the message
     * @param con receive connections
     */
    protected void archiveMsg(DcpMsg dcpMsg, DdsRecvConnection con)
    {
        msgArchive.archiveMsg(dcpMsg, con);
    }

    /**
     * Template method to take action when server reports that we are all caught
     * up. This base-class implementation just pauses 1 second.
     */
    protected void allCaughtUp()
    {
        try { sleep(1000L); } catch (InterruptedException ex) {}
    }

    /**
     * Builds a new search criteria to initialize a new connection.
     */
    protected SearchCriteria buildSearchCrit()
    {
        // We now have a new connection. Send it the search criteria.
        SearchCriteria searchCrit = new SearchCriteria();
        try
        {
            searchCrit.setLrgsSince(IDateFormat.time_t2string((int) (lastMsgRecvTime / 1000L) - 60));
            for (NetlistGroupAssoc nga : ddsRecvSettings.getNetlistGroupAssociations())
            {
                String netlistGroup = nga.getGroupName();

                if (netlistGroup.equalsIgnoreCase("both")
                 || (isSecondary && netlistGroup.equalsIgnoreCase("secondary"))
                 || (!isSecondary && netlistGroup.equalsIgnoreCase("primary")))
                {
                    if (nga.getNetlistName().toLowerCase().startsWith("source=")
                     && nga.getNetlistName().length() > 7)
                    {
                        String t = nga.getNetlistName().substring(7);
                        int tn = DcpMsgFlag.sourceName2Value(t);
                        if (tn != -1)
                            searchCrit.addSource(tn);
                        else
                            log.warn("{} invalid source specified '{}'", module, nga.getNetlistName());
                    }
                    else if (nga.getNetworkList() != null)
                        searchCrit.addNetworkList(nga.getNetworkList().makeFileName());
                }
            }
            if (ddsRecvSettings.decodesAll)
                searchCrit.addNetworkList("<all>");
            if (ddsRecvSettings.decodesProduction)
                searchCrit.addNetworkList("<production>");
        }

        catch (Exception ex)
        {
            log.atError().setCause(ex).log("Unable to build search criteria.");
        }
        return searchCrit;
    }

    /**
     * Check the configuration file to see if it has changed. If so, reload it
     * and put the changes into effect.
     */
    protected void checkConfig()
    {
        log.trace("{} checkConfig", module);
        String fn = EnvExpander.expand(LrgsConfig.instance().ddsRecvConfig);
        File cf = new File(fn);

        if (cf.lastModified() > lastConfigRead || ddsRecvSettings.networkListsHaveChanged())
        {

            lastConfigRead = System.currentTimeMillis();
            if (!isSecondary)
            { // reloads configuration from file if primary
                // group thread.
                try
                {
                    synchronized (ddsRecvSettings)
                    {
                        ddsRecvSettings.setFromFile(fn);
                        ddsRecvSettings.setReloaded(true);
                        log.info("{}:{} Loaded Config File '{}'", module, (-EVT_BAD_CONFIG), cf);

                    }

                }
                catch (BadConfigException ex)
                {
                    log.atError()
                       .setCause(ex)
                       .log("{}:{} Cannot read DDS Recv Config File '{}'", module, EVT_BAD_CONFIG, cf);
                }
            }

            else
            // secondary group thread waits for the configuration to be reloaded
            {
                while (!ddsRecvSettings.isReloaded())
                {
                    try
                    {
                        sleep(5000L);
                    }
                    catch (Exception ex)
                    {

                        log.atError()
                           .setCause(ex)
                           .log("{}:{} Cannot read DDS Recv Config File '{}'", module, EVT_BAD_CONFIG, cf);
                    }
                    continue;
                }
                synchronized (ddsRecvSettings)
                {
                    ddsRecvSettings.setReloaded(false);
                }

            }

            synchronized (recvConList)
            {
                recvConList.removeAll();
                for (Iterator it = ddsRecvSettings.getConnectConfigs(); it.hasNext();)
                {
                    DdsRecvConnectCfg ddsCfg = (DdsRecvConnectCfg) it.next();

                    if (isSecondary)
                    { // adds to secondary group list
                        if (ddsCfg.group != null && ddsCfg.group.equalsIgnoreCase("secondary"))
                            recvConList.addConnection(ddsCfg);
                    }
                    else
                    { // adds to primary group list
                        if (ddsCfg.group == null || ddsCfg.group.equalsIgnoreCase("primary"))
                            recvConList.addConnection(ddsCfg);

                    }
                }
            }
        }
    }

    // =====================================================================
    // Methods from LrgsInputInterface
    // =====================================================================

    /**
     * @return the type of this input interface.
     */
    public int getType()
    {
        if (isSecondary)
            return DL_DDS_SECONDRAY;
        else
            return DL_DDS;
    }

    /**
     * All inputs must keep track of their 'slot', which is a unique index into
     * the LrgsMain's vector of all input interfaces.
     *
     * @param slot
     *            the slot number.
     */
    public void setSlot(int slot)
    {
        this.slot = slot;
    }

    /** @return the slot numbery that this interface was given at startup */
    public int getSlot()
    {
        return this.slot;
    }

    /**
     * @return the name of this interface.
     */
    public String getInputName()
    {
        if (isSecondary)
        {
            return "DDS-Recv:Main(Secondary)";
        }
        else
        {
            return "DDS-Recv:Main";
        }
    }

    /**
     * Initializes the interface. May throw LrgsInputException when an
     * unrecoverable error occurs.
     */
    public void initLrgsInput() throws LrgsInputException
    {
    }

    /**
     * Shuts down the interface. Any errors encountered should be handled within
     * this method.
     */
    public void shutdownLrgsInput()
    {
    }

    /**
     * Enable or Disable the interface. The interface should only attempt to
     * archive messages when enabled.
     *
     * @param enabled
     *            true if the interface is to be enabled, false if disabled.
     */
    public void enableLrgsInput(boolean enabled)
    {
        if (enabled)
        {
            enableTime = System.currentTimeMillis();
        }
        else
        {
            enableTime = 0L;
        }
    }

    /**
     * @return true if this downlink can report a Bit Error Rate.
     */
    public boolean hasBER()
    {
        return false;
    }

    /**
     * @return the Bit Error Rate as a string.
     */
    public String getBER()
    {
        return "";
    }

    /**
     * @return true if this downlink assigns a sequence number to each msg.
     */
    public boolean hasSequenceNums()
    {
        return false;
    }

    /**
     * @return the numeric code representing the current status.
     */
    public int getStatusCode()
    {
        return statusCode;
    }

    private static final long MS_PER_HR = 3600*1000L;

    /**
     * @return a short string description of the current status.
     */
    public String getStatus()
    {
        long now = System.currentTimeMillis();
        if (now/MS_PER_HR > lastStatusTime/MS_PER_HR)  // Hour just changed
        {
            String s = "ddsMinHourly";
            log.trace("Looking for property '{}'", s);
            int minHourly = LrgsConfig.instance().ddsMinHourly;
            if (minHourly > 0                          // Feature Enabled
             && enableTime != 0L                       // Currently Enabled
             && (now - enableTime > 3*MS_PER_HR))      // Have been up for at least 3 hours
            {
                if (numThisHour < minHourly)
                {
                    log.warn("{} {} for hour ending {} number of messages received={} " +
                             "which is under minimum threshold of {}",
                             module, getInputName(), new Date((now / MS_PER_HR) * MS_PER_HR), numThisHour, minHourly);
                }
                if (numThisHour < (numLastHour/2))
                {
                    log.warn("{} {} for hour ending {} number of messages received={} " +
                             "which is under half previous hour's total of {}",
                             module, getInputName(), new Date((now / MS_PER_HR) * MS_PER_HR), numThisHour, numLastHour);
                }
            }

            // Rollover the counts.
            numLastHour = numThisHour;
            numThisHour = 0;
        }

        lastStatusTime = now;
        return status;
    }

    public int getDataSourceId()
    {
        return -1;
    }

    /** @return true if this interface receives APR messages */
    public boolean getsAPRMessages()
    {
        return true;
    }

    /**
     * @return the isSecondary
     */
    public boolean isSecondary()
    {
        return isSecondary;
    }

    /**
     * @param isSecondary
     *            the isSecondary to set
     */
    public void setSecondary(boolean isSecondary)
    {
        this.isSecondary = isSecondary;
    }

    @Override
    public String getGroup()
    {
        return null;
    }

}
