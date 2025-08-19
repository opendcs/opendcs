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
package decodes.datasource;

import java.util.Properties;
import java.util.Vector;

import javax.net.SocketFactory;

import org.opendcs.tls.TlsMode;
import org.opendcs.utils.WebUtility;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.util.Iterator;
import java.util.StringTokenizer;
import java.io.File;

import ilex.util.EnvExpander;
import ilex.util.TextUtil;
import ilex.var.NoConversionException;
import ilex.util.PropertiesUtil;
import ilex.var.Variable;

import decodes.db.Database;
import decodes.db.Platform;
import decodes.db.Constants;
import decodes.db.DataSource;
import decodes.db.TransportMedium;
import decodes.db.NetworkList;
import decodes.db.InvalidDatabaseException;
import decodes.db.DatabaseException;
import decodes.util.PropertySpec;

import lrgs.ldds.LddsClient;
import lrgs.ldds.ServerError;
import lrgs.lrgsmain.LrgsConfig;
import lrgs.common.DcpAddress;
import lrgs.common.LrgsErrorCode;
import lrgs.common.SearchCriteria;
import lrgs.common.DcpMsg;
import lrgs.common.DcpMsgFlag;


/**
  This is the implementation of the DataSourceInterface for LRGS
  network connections.
*/
public class LrgsDataSource extends DataSourceExec
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
    LddsClient lddsClient;
    String host;
    int port;
    String username;
    String password;
    int timeout;
    int consecutiveBadMessages;
    int MaxConsecutiveBadMessages = -1;
    boolean oldChannelRanges;
    boolean sendnl;
    private PMParser goesPMP;
    private PMParser iridiumPMP;
    private PMParser netdcpPMP;
    int retries;
    boolean singleModeOnly;    // Current connection forced to single mode.
    TlsMode tls;

    /** For use under hot-backup-group: It will set the following to a
     *  positive number (180). Thus once a connection fails, it stays
     *  timed-out for this many seconds. A timed-out connection will fail
     *  on its init() method right away.
     */
    private int timeoutSecOnError = 0;

    /** Msec value of the last time this connection failed. */
    private long lastError = 0L;

    /** Flag used to abort waiting client. */
    boolean abortFlag;

    private static int connum = 0;

    PropertySpec[] lrgsDsPropSpecs =
    {
        new PropertySpec("host", PropertySpec.HOSTNAME,
            "LRGS Data Source: Host name or IP Address of LRGS Server"),
        new PropertySpec("port", PropertySpec.INT,
            "LRGS Data Source: Listening port on LRGS Server (default = 16003)"),
        new PropertySpec("username", PropertySpec.STRING,
            "LRGS Data Source: DDS User name with which to connect to LRGS server"),
        new PropertySpec("password", PropertySpec.STRING,
            "LRGS Data Source: Password (if blank, use unauthenticated connection)"),
        new PropertySpec("lrgs.timeout", PropertySpec.INT,
            "LRGS Data Source: Number of idle seconds after which to assume server has failed (default=60)." +
            " For sparse data (i.e. small netlist) you should set this to a large value."),
        new PropertySpec("lrgs.maxConsecutiveBadmessages", PropertySpec.INT,
            "LRGS Data Source: How many bad messages in a row before it's decided this connection" +
            " is bad (default=-1 which means never assume bad messages should cause the connection to drop.)."),
        new PropertySpec("lrgs.tls", PropertySpec.JAVA_ENUM+TlsMode.class.getName(),
            "Use TLS connection for this LRGS.")
    };


    /**
	 * @see decodes.datasource.DataSourceExec#DataSourceExec(DataSource, Database) DataSourceExec Constructor
	 *
	 * @param ds data source
	 * @param db database
	 */
    public LrgsDataSource(DataSource ds, Database db)
    {
        super(ds,db);
        lddsClient = null;
        host = null;
        port = LrgsConfig.def_ddsListenPort;
        username = null;
        timeout = 60;
        consecutiveBadMessages = 0;
        oldChannelRanges = false;
        sendnl = true;
        retries = 0;
        singleModeOnly = false;
        abortFlag = false;
        tls = TlsMode.NONE;
        try { goesPMP = PMParser.getPMParser(Constants.medium_Goes); }
        catch(HeaderParseException e) {} // shouldn't happen.
        iridiumPMP = new IridiumPMParser();
        netdcpPMP = new EdlPMParser();
    }

    /**
      Extract host, port, and username.
      Do NOT make connection to LRGS here. Wait until first call to getMessage.
    */
    public void processDataSource()
        throws InvalidDatabaseException
    {
        log.debug("processDataSource for LrgsDataSource for '{}', args='{}'",
                  dbDataSource.getName(), dbDataSource.getDataSourceArg());

        // host defaults to ds name, will probably be overridden by property.
        host = dbDataSource.getName();
    }

    /**
      This data source is about to be used.
      Open the connection, construct search-criteria &amp; network lists.
      Send network list &amp; search criteria messages.
      <p>
      @param routingSpecProps the routing spec properties.
      @param since the since time from the routing spec.
      @param until the until time from the routing spec.
      @param networkLists contains NetworkList objects.
      @throws DataSourceException if the source could not be initialized.
    */
    public void init(Properties routingSpecProps, String since, String until,
        Vector<NetworkList> networkLists)
        throws DataSourceException
    {
        log.debug("LrgsDataSource.init() for '{}', since='{}', until='{}'",
                  dbDataSource.getName(), since, until);

        if (timeoutSecOnError > 0)
        {
            int elapsed = (int)((System.currentTimeMillis() - lastError)/1000L);
            if (elapsed < timeoutSecOnError)
            {
                throw new DataSourceException("Skipping LRGS at '"
                    + host + ':' + port + ", username='" + username
                    + "': timed-out.");
            }
        }

        abortFlag = false;

        // Build a complete property set. Routing Spec props override DS props.
        Properties allProps = new Properties(dbDataSource.arguments);
        PropertiesUtil.copyProps(allProps, routingSpecProps);

        String h = PropertiesUtil.getIgnoreCase(allProps, "host");
        if (h == null)
        {
            h =  PropertiesUtil.getIgnoreCase(allProps, "hostname");
        }
        if (h != null)
        {
            host = EnvExpander.expand(h);
        }
         log.trace("LrgsDataSource '{}' host=-{}, host property='{}'", dbDataSource.getName(), host, h);

        String ports = PropertiesUtil.getIgnoreCase(allProps, "port");
        if (ports != null)
        {
            try
            {
                port = Integer.parseInt(EnvExpander.expand(ports));
            }
            catch(NumberFormatException ex)
            {
                throw new DataSourceException("LRGS Data source '"
                    + host + "': invalid port '" + ports
                    + "' - must be a number", ex);
            }
        }
        else
        {
            // No port specified -- use default
            port = LrgsConfig.def_ddsListenPort;
        }
        // Username and password, if stored in properties
        // will be expanded at time of use to reduce the time they are 
        // in memory... and to avoid logging them.
        username = PropertiesUtil.getIgnoreCase(allProps, "username");
        if (username == null)
        {
            username = PropertiesUtil.getIgnoreCase(allProps, "user");
        }
        if (username == null)
        {
            throw new DataSourceException("LRGS Data source '"
                    + host + "': missing user name");
        }
        if (!(username.startsWith("${") && username.endsWith("}")))
        {
            log.warn(
                    "It appears that your username is saved directly in the property. This is not secure. " +
                    "It recommended that you migrate to one of the properties providers such as environment or secrets files."
                );
        }

        password = PropertiesUtil.getIgnoreCase(allProps, "password");
        if (password != null && password.trim().length() == 0)
        {
            password = null;
        }
        else if (password != null && !(password.startsWith("${") && password.endsWith("}")))
        {
            log.warn(
                    "It appears that your password is saved directly in the property. This is not secure. " +
                    "It recommended that you migrate to one of the properties providers such as environment or secrets files."
                );
        }

        String snl = PropertiesUtil.getIgnoreCase(allProps, "sendnl");
        if (snl != null && snl.equalsIgnoreCase("false"))
        {
            sendnl = false;
        }

        String s = PropertiesUtil.getIgnoreCase(allProps, "single");
        if (s != null)
        {
            singleModeOnly = !(s.equalsIgnoreCase("no")
                            || s.equalsIgnoreCase("false")
                            || s.equalsIgnoreCase("off"));
        }

        String scpath = PropertiesUtil.getIgnoreCase(allProps, "searchcrit");
        SearchCriteria searchCrit = null;
        boolean sendSc = false;
        if (scpath == null)
        {
            searchCrit = new SearchCriteria();
        }
        else
        {
            try
            {
                searchCrit = new SearchCriteria(new File(scpath));
                sendSc = true;
            }
            catch(Exception ex)
            {
                log.atWarn()
                   .setCause(ex)
                   .log("Data Source '{}' can't open searchcrit '{}' -- ignored.",
                        dbDataSource.getName(), scpath);
                searchCrit = new SearchCriteria();
            }
        }

        for(Object key : allProps.keySet())
        {
            String propName = (String)key;
            String value = allProps.getProperty(propName);
            if (value.length() == 0)
            {
                continue;
            }

            if (TextUtil.startsWithIgnoreCase(propName, "sc:CHANNEL"))
            {
                searchCrit.addChannelToken(value);
            }
            else if (propName.equalsIgnoreCase("sc:DAPS_STATUS"))
            {
                searchCrit.DapsStatus = value.charAt(0);
            }
            else if (TextUtil.startsWithIgnoreCase(propName, "sc:DCP_ADDRESS"))
            {
                searchCrit.addDcpAddress(new DcpAddress(value));
            }
            else if (TextUtil.startsWithIgnoreCase(propName, "sc:DCP_NAME"))
            {
                searchCrit.addDcpName(value);
            }
            else if (propName.equalsIgnoreCase("sc:SPACECRAFT"))
            {
                searchCrit.spacecraft = value.charAt(0);
            }
            else if (TextUtil.startsWithIgnoreCase(propName, "sc:SOURCE"))
            {
                searchCrit.addSource(DcpMsgFlag.sourceName2Value(value));
            }
            else if (propName.equalsIgnoreCase("sc:PARITY_ERROR"))
            {
                searchCrit.parityErrors = value.charAt(0);
            }
            else if (propName.equalsIgnoreCase("sc:ASCENDING_TIME"))
            {
                searchCrit.setAscendingTimeOnly(TextUtil.str2boolean(value));
            }
            else if (propName.equalsIgnoreCase("sc:RT_SETTLE_DELAY"))
            {
                searchCrit.setRealtimeSettlingDelay(TextUtil.str2boolean(value));
            }
        }

        final String tlsStr = PropertiesUtil.getIgnoreCase(allProps, "lrgs.tls");
        if (tlsStr != null)
        {
            tls = TlsMode.valueOf(tlsStr);
        }

        openConnection();

        try
        {
            if (since != null && since.trim().length() != 0)
            {
                searchCrit.setLrgsSince(since);
                sendSc = true;
            }
            if (until != null && until.trim().length() != 0)
            {
                searchCrit.setLrgsUntil(until);
                sendSc = true;
            }

            s = PropertiesUtil.getIgnoreCase(allProps, "dcpaddress");
            if (s != null)
            {
                try
                {
                    searchCrit.addDcpAddress( new lrgs.common.DcpAddress(s) );
                    sendSc = true;
                }
                catch(NumberFormatException ex)
                {
                    log.atWarn().setCause(ex).log("Invalid DCP address '{}' -- ignored.", s);
                }
            }

            s = PropertiesUtil.getIgnoreCase(allProps, "channel");
            if (s != null)
            {
                StringTokenizer st = new StringTokenizer(s, ":");
                while(st.hasMoreTokens())
                    searchCrit.addChannelToken(st.nextToken());
                sendSc = true;
            }


            // Process Network List files named in searchcrit.
            Vector<String> namesToAdd = new Vector<String>();
            for(Iterator<String> it = searchCrit.NetlistFiles.iterator(); it.hasNext();)
            {
                String nm = it.next();

                // Allow implicit lists to pass. Will be handled by LddsClient.
                if (nm.equals("<all>") || nm.equalsIgnoreCase("<production>"))
                {
                    continue;
                }

                // Prefer lists from the DECODES DB.
                // If list is in the DECODES DB, send it.
                // Look for matching name with or without ".nl" extension.
                NetworkList decNL = db.networkListList.getNetworkList(nm);
                if (decNL == null)
                {
                    int idx = nm.lastIndexOf(".nl");
                    if (idx != -1)
                    {
                        String nnm = nm.substring(0, idx);
                        decNL = db.networkListList.getNetworkList(nnm);
                    }
                }
                if (decNL != null)
                {
                    try
                    {
                        // 'prepare' makes an LRGS network list with .nl ext.
                        if (!decNL.isPrepared())
                        {
                            decNL.prepareForExec();
                        }
                        lrgs.common.NetworkList lnl = decNL.legacyNetworkList;
                        if (lnl != null && sendnl)
                        {
                            lddsClient.sendNetList(lnl, null);
                            // Remove & re-add list name to ensure match on svr.
                            it.remove();
                            namesToAdd.add(lnl.makeFileName());
                            continue;
                        }
                    }
                    catch(InvalidDatabaseException ex)
                    {
                        log.atWarn()
                           .setCause(ex)
                           .log("Network list '{}' cannot be converted to LRGS netlist format:  -- skipped.",
                                nm);
                    }
                }

                // NO DECODES Netlist. Look for LRGS netlist file
                // Look in . or ./netlist.
                File f = new File(nm);
                if (!f.exists())
                {
                    f = new File("netlist" + File.separator + nm);
                }
                if (f.exists())
                {
                    if (sendnl)
                    {
                        lddsClient.sendNetList(f, f.getName());
                    }
                    continue;
                }

                // Else list doesn't exist here. Hope it exists on server.
                log.warn("Network list '{}' does not exist locally, will try server-resident.", nm);
            }
            for(Iterator<String> it = namesToAdd.iterator(); it.hasNext(); )
            {
                searchCrit.addNetworkList(it.next());
            }
            if (!allowDapsStatusMessages)
            {
                searchCrit.DapsStatus = SearchCriteria.NO;
            }

            // Now process network lists explicitely placed in the DECODES
            // routing spec.
            log.debug("LRGSDS: There are {} netlists explicitly in the RS", networkLists.size());
            for(Iterator<NetworkList> it = networkLists.iterator(); it.hasNext(); )
            {
                NetworkList nl = (NetworkList)it.next();
                if (nl == NetworkList.dummy_all
                 || nl == NetworkList.dummy_production)
                {
                    // These special list names <all> and <production>
                    // are expanded by LddsClient when the searchcrit is sent.
                    searchCrit.addNetworkList(nl.name);
                    sendSc = true;
                    continue;
                }

                lrgs.common.NetworkList lnl = null;
                try
                {
                    // 'prepare' makes an LRGS network list.
                    if (!nl.isPrepared())
                    {
                        nl.prepareForExec();
                    }
                    lnl = nl.legacyNetworkList;
                }
                catch(InvalidDatabaseException ex)
                {
                    log.atWarn()
                       .setCause(ex)
                       .log("Network list '{}' cannot be converted to LRGS netlist format: -- skipped.", nl.name);
                }

                if (lnl != null && sendnl)
                {
                    log.debug("Sending network list '{}'", nl.name);
                    lddsClient.sendNetList(lnl, null);
                }

                //Code added by Josue - Feb 20,2008 - check for null on lnl
                if (lnl != null)
                {
                    // Add reference to this netlist to my searchcrit.
                    searchCrit.addNetworkList(lnl.makeFileName());
                    sendSc = true;
                }
            }

            s = PropertiesUtil.getIgnoreCase(allProps, "ascendingTimeOnly");
            if (s != null)
            {
                searchCrit.setAscendingTimeOnly(TextUtil.str2boolean(s));
            }


            if (sendSc)
            {
                lddsClient.sendSearchCrit(searchCrit);
                if (routingSpecThread != null)
                {
                    routingSpecThread.implicitAllUsed =
                        lddsClient.implicitAllUsed;
                }
            }
        }
        catch(Exception ex)
        {
            close();
            lastError = System.currentTimeMillis();
            String msg =
                "Error initializing search criteria for LRGS connection at '"
                + host + '/' + port + ", username='" + username + "'";
            throw new DataSourceException(msg,ex);
        }

        String ts = PropertiesUtil.getIgnoreCase(allProps, "lrgs.timeout");
        if (ts == null)
        {
            ts = PropertiesUtil.getIgnoreCase(allProps, "timeout");
        }
        if (ts != null)
        {
            try { timeout = Integer.parseInt(ts); }
            catch(NumberFormatException ex)
            {
                log.atError()
                   .setCause(ex)
                   .log("Improper timeout value '{}' in LrgsDataSource '{}' -- ignored",
                        ts, dbDataSource.getName());
            }
        }
        ts = PropertiesUtil.getIgnoreCase(allProps, "lrgs.retries");
        if (ts != null)
        {
            try { retries = Integer.parseInt(ts); }
            catch(NumberFormatException ex)
            {
                log.atError()
                   .setCause(ex)
                   .log("Improper retries value '{}' in LrgsDataSource '{}' -- ignored",
                        ts, dbDataSource.getName());
            }
        }

        ts = PropertiesUtil.getIgnoreCase(allProps, "OldChannelRanges");
        if (ts != null)
        {
            oldChannelRanges = ts.equalsIgnoreCase("true");
            log.debug("Will use old channel ranges to determine Transport Medium");
        }

        ts = PropertiesUtil.getIgnoreCase(allProps,"lrgs.maxConsecutiveBadMessages","-1");
        try
        {
            MaxConsecutiveBadMessages = Integer.parseInt(ts);
        }
        catch (NumberFormatException ex)
        {
            log.atError()
               .setCause(ex)
               .log("MaxConsectiveBadMessage value ({}) is not a valid integer." +
                    " System will use default value of -1.",
                    ts);
        }
    }

    /**
      Closes the data source.
      This method is called by the routing specification when the data
      source is no longer needed.
    */
    public void close()
    {
        if (lddsClient != null)
        {
            try { lddsClient.sendGoodbye(); }
            catch(Exception e) {}
        }
        hangup();
    }

    /** Closes connection immediately without sending Goodbye message */
    private void hangup()
    {
        if (lddsClient != null)
        {
            lddsClient.disconnect();
        }
        lddsClient = null;
    }

    /**
      Reads the next raw message from the data source and returns it.
      This DataSource will fill in the message data and attempt to
      associate it with a TransportMedium object.

      @throws DataSourceTimeoutException if the specified number of seconds
      and retries has passed and no new message was received from the server.

      @throws DataSourceEndException if the server reports that we have
      reached the 'until' time specified in the search criteria.

      @throws DataSourceException if some other problem arises.
    */
    public RawMessage getRawMessage()
        throws DataSourceException
    {
        if (lddsClient == null || !lddsClient.isConnected())
        {
            throw new DataSourceException("LddsDataSource not connected to server");
        }

        // Absolute end time:
        long endTime = System.currentTimeMillis()
            + (timeout * (retries+1)) * 1000L;

        // Attempt to read a DCP Message from the connection.
        boolean gotOne = false;
        int numTries = 0;
        DcpMsg dcpMsg = null;
        char failureCode = 'x';
        while (!gotOne)
        {
            try
            {
                if (lddsClient == null)
                {
                    throw new DataSourceEndException("Aborted.");
                }
                dcpMsg = lddsClient.getDcpMsg(timeout);
            }
            catch(ServerError se)
            {
                // Server caught up and waiting for more data to arrive?
                if (se.Derrno == LrgsErrorCode.DMSGTIMEOUT)
                {
                    if (System.currentTimeMillis() >= endTime)
                    {
                        throw new DataSourceTimeoutException(dbDataSource.getName());
                    }
                    else
                    {
                        log.trace("Server returned DMSGTIMEOUT, ", (abortFlag ? "aborting." : "pausing."));
                        if (abortFlag)
                        {
                            throw new DataSourceEndException("Aborted.");
                        }
                        else if (routingSpecThread != null)
                        {
                            routingSpecThread.lrgsDataSourceCaughtUp();
                        }

                        try { Thread.sleep(1000L); }
                        catch (InterruptedException ie) {}
                        continue;
                    }
                }
                // Server says that specified UNTIL time reached?
                else if (se.Derrno == LrgsErrorCode.DUNTIL)
                {
                    close();
                    throw new DataSourceEndException("Until Time Reached.");
                }
                else // Some other error, propegate up.
                {
                    close();
                    lastError = System.currentTimeMillis();
                    throw new DataSourceException(
                        "Server Error on LRGS data source '"
                        + dbDataSource.getName() + "'", se);
                }
            }
            catch(DataSourceEndException ex)
            {
                throw ex;
            }
            catch(Exception ex) // ProtocolError or IOException
            {
                close();
                lastError = System.currentTimeMillis();
                String errmsg = "Error on LRGS data source '"+ dbDataSource.getName() + "'";
                throw new DataSourceException(errmsg,ex);
            }

            // Null means that no response was received from server.
            if (dcpMsg == null) // Indicates timeout occurred.
            {
                if (++numTries <= retries)
                {
                    continue;
                }

                //MJM Don't call close(), because this removes the data source from HotBackGroups
                //memory, which causes null ptr.

                lastError = System.currentTimeMillis();
                throw new DataSourceException("Timeout (" + timeout
                    + " seconds) on LRGS data source '"+dbDataSource.getName()+"'");
            }

            failureCode = dcpMsg.getFailureCode();
            if (failureCode == 'G' || failureCode == '?'
             || allowDapsStatusMessages)
            {
                gotOne = true;
            }
            else
            {
                log.debug("Skipping DAPS Status Message with type '{}' isGoesMsg={}, flags=0x{}",
                          failureCode, dcpMsg.isGoesMessage(), Integer.toHexString(dcpMsg.flagbits));
            }
        }

        // Parse the message & establish platform linkage.
        try
        {
            RawMessage ret = lrgsMsg2RawMessage(dcpMsg);
            consecutiveBadMessages = 0;
            ret.dataSourceName = getName();
            return ret;
        }
        catch(HeaderParseException ex)
        {
            consecutiveBadMessages++;
            if (MaxConsecutiveBadMessages < 0)
            {
                log.atDebug().setCause(ex).log("Unable to parse header for station.");
                if( dcpMsg != null)
                {
                    RawMessage ret = new RawMessage(dcpMsg.getData());
                    ret.setOrigDcpMsg(dcpMsg);
                    ret.dataSourceName = getName();
                    return ret;
                }
                else
                {
                    return null;
                }
            }
            else if (consecutiveBadMessages >= MaxConsecutiveBadMessages)
            {
                close();
                lastError = System.currentTimeMillis();
                throw new DataSourceException(
                    "Too many consecutive bad messages from '"
                    + dbDataSource.getName() + "': closing.",ex);
            }
            else
            {
                return getRawMessage();
            }
        }
        // Allow UnknownPlatformException to be propagated.
    }

    /**
      Converts an LRGS 'DcpMsg' object into a DECODES RawMessage object.
      @param dcpMsg the LRGS data structure containing a DCP message.
      @return RawMessage containing the passed data.
    */
    public RawMessage lrgsMsg2RawMessage(DcpMsg dcpMsg)
        throws UnknownPlatformException, HeaderParseException
    {
        RawMessage ret = new RawMessage(dcpMsg.getData());
        ret.setOrigDcpMsg(dcpMsg);
        String addrField;
        int chan;
        PMParser pmp = null;
        if (DcpMsgFlag.isIridium(dcpMsg.flagbits))
        {
            pmp =  iridiumPMP;
        }
        else if (DcpMsgFlag.isDamsNtDcp(dcpMsg.flagbits))
        {
            pmp = goesPMP;
        }
        else if (DcpMsgFlag.isNetDcp(dcpMsg.flagbits))
        {
            pmp = netdcpPMP;
        }
        else
        {
            pmp = goesPMP;
        }

        try
        {
            pmp.parsePerformanceMeasurements(ret);
        }
        catch(HeaderParseException ex)
        {
            // Kludge for Sutron NetDCPs that are being used by USACE NAE.
            // The DCPs are retrieved via DAMS-NT over the network. They may also
            // arrive via DDS from another LRGS.
            // Thus if we tried netDCP header and it failed, try GOES.
            String em = ex.toString();
            if (pmp == netdcpPMP)
            {
                try
                {
                    log.info(" Failed to parse NetDCP message with EDL Header parser. Will attempt with GOES.");
                    (pmp = goesPMP).parsePerformanceMeasurements(ret);
                    em = null; // Success
                }
                catch(HeaderParseException e2)
                {
                    em = "Attempted EDL message with both netdcp and goes header parsers.";
                }
            }
            if (em != null)
            {
                log.atError()
                   .setCause(ex)
                   .log("Could not parse message header for '{}' flags=0x{}: {}",
                          new String(dcpMsg.getData(), 0, 20),
                          Integer.toHexString(dcpMsg.getFlagbits()),
                          em);
                throw ex;
            }
        }

        try
        {
            addrField =
                ret.getPM(GoesPMParser.DCP_ADDRESS).getStringValue().toUpperCase();

            Variable v;
            if (pmp == netdcpPMP
             && (v = ret.getPM(EdlPMParser.STATION)) != null)
                addrField = v.getStringValue();

            v = ret.getPM(GoesPMParser.CHANNEL);
            chan = v == null ? Constants.undefinedIntKey : v.getIntValue();
        }
        catch(NoConversionException ex)
        {
            String s = "Non-numeric channel in DCP message header";
            throw new HeaderParseException(s,ex);
        }

        // If msg was retrieved from new extended msg type, it will have
        // additional fields:
        if (dcpMsg.getBaud() != 0)
        {
            ret.setPM(GoesPMParser.BAUD, new Variable(dcpMsg.getBaud()));
        }
        if (dcpMsg.getCarrierStart() != null)
        {
            ret.setPM(GoesPMParser.CARRIER_START,
                new Variable(dcpMsg.getCarrierStart()));
        }
        if (dcpMsg.getCarrierStop() != null)
        {
            ret.setPM(GoesPMParser.CARRIER_STOP,
                new Variable(dcpMsg.getCarrierStop()));
        }
        if (dcpMsg.getDomsatTime() != null)
        {
            ret.setPM(GoesPMParser.DOMSAT_TIME,
                new Variable(dcpMsg.getDomsatTime()));
        }
        ret.setPM(GoesPMParser.DCP_MSG_FLAGS, new Variable(dcpMsg.flagbits));

        //
        // Establish platform and transport medium linkage, if possible.
        //

        // Attempt to get platform record using type Goes
        // Note: Platform list will find any matching GOES TM type (ST or RD).
        Platform p = null;
        try
        {
            if (db != null)
            {
                p = db.platformList.getPlatform(
                        pmp.getMediumType(), addrField, ret.getTimeStamp());
            }
        }
        catch(DatabaseException ex)
        {
            log.atWarn()
               .setCause(ex)
               .log("Cannot read platform record for message '{}'",
                    new String(ret.getData(), 0, (ret.getData().length > 19 ? 19 : ret.getData().length)));
            p = null;
        }

        if (p != null)
        {
            ret.setPlatform(p);  // Set platform reference in the raw message.

            // Use the base class resolver to find exact matching TM.
            TransportMedium tm = resolveTransportMedium(p, addrField, chan,
                oldChannelRanges);
            if (tm == null)
            {
                log.warn("Cannot resolve platform for addr='{}', chan={}", addrField, chan);
            }
            ret.setTransportMedium(tm);
        }
        else if (!getAllowNullPlatform()) // Couldn't find platform using TM
        {

            throw new UnknownPlatformException(
                "lrgsMsg2RawMessage: No platform matching '" + addrField
                + (dcpMsg.isGoesMessage() ? ("' and channel " + chan) : "'")
                + " and medium type " + pmp.getMediumType());
        }

        return ret;
    }

    //=================================================================
    // Internal methods
    //=================================================================

    private void openConnection()
        throws DataSourceException
    {
        log.debug("LrgsDataSource.openConnection to host '{}', port={}, tls={}", host, port, tls.name());
        hangup();
        try
        {
            final SocketFactory socketFactory = (tls != TlsMode.NONE) ?  WebUtility.socketFactory((cert) ->
                {
                    log.warn("Certificate for '{}' is not trusted.", cert.getHostname().orElse("<no hostname provided>"));
                    return false;
                })
                : null;
            final String realUserName = EnvExpander.expand(username);
            final String realPassword = EnvExpander.expand(password);
            lddsClient = new LddsClient(host, LrgsConfig.def_ddsListenPort, socketFactory, tls);

            lddsClient.setModule("lrgsds-" + (connum++));

            // Use Single-Message-Mode if user forces with "single" argument.
            // Otherwise, let client/server decide based on protoVersion.
            lddsClient.enableMultiMessageMode(!singleModeOnly);

            lddsClient.connect();
            if (password != null)
            {
                lddsClient.sendAuthHello(realUserName, realPassword);
            }
            else
            {
                lddsClient.sendHello(realUserName);
            }
            log.info("Connected to DDS server at {}:{}, username='{}'", host, port, username);

        }
        catch(Exception ex)
        {
            hangup();
            lastError = System.currentTimeMillis();
            throw new DataSourceException("Cannot connect to LRGS at '"
                + host + ':' + port + ", username='" + username + "'", ex);
        }
    }

    /** @return the currently connected host name. */
    public String getHostName() { return host; }


    /**
     * If we are in a loop waiting for a message from the server, this
     * method sets a flag, telling getRawMessage to abort.
     */
    public void abortGetRawMessage()
    {
        log.debug("LRGS Data Source aborting.");
        abortFlag = true;
    }

    /**
     * Set number of seconds after a failure that this connection stays
     * 'timed-out'.
     */
    public void setTimeoutSecOnError(int sec)
    {
        timeoutSecOnError = sec;
    }

    /**
     * Reset the counter for last error. This is done by hot-backup-group
     * if all of its constituents are in an error condition.
     */
    public void resetLastError()
    {
        lastError = 0L;
    }

    /**
     * Base class returns an empty array for backward compatibility.
     */
    @Override
    public PropertySpec[] getSupportedProps()
    {
        return lrgsDsPropSpecs;
    }

    /**
     * Base class return true for backward compatibility.
     */
    @Override
    public boolean additionalPropsAllowed()
    {
        return true;
    }

    @Override
    public boolean supportsTimeRanges()
    {
        return true;
    }

}
