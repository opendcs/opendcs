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
package lrgs.ddsserver;

import java.net.*;
import java.io.*;
import java.util.Iterator;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.net.*;
import ilex.util.Counter;
import ilex.util.EnvExpander;
import ilex.util.FileCounter;
import ilex.util.Pair;
import ilex.util.QueueLogger;
import ilex.util.SimpleCounter;
import lrgs.apistatus.AttachedProcess;
import lrgs.common.ArchiveUnavailableException;
import lrgs.archive.MsgArchive;
import lrgs.common.NetlistDcpNameMapper;
import lrgs.ldds.GetHostnameThread;
import lrgs.ldds.LddsLoggerThread;
import lrgs.ldds.LddsParams;
import lrgs.ldds.LddsThread;
import lrgs.lrgsmain.LrgsConfig;
import lrgs.lrgsmain.JavaLrgsStatusProvider;


/**
Main class for the LRGS DDS server that uses the Java-Only-Archive.
*/
public class DdsServer extends BasicServer implements Runnable
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
    public static final String module = "DdsSvr";

    /** Event number meaning server disabled */
    public static final int EVT_SVR_DISABLED = 1;

    /** Event number meaning can't make client */
    public static final int EVT_INTERNAL_CLIENT = 2;

    /** Event number meaning we already have max clients. */
    public static final int EVT_MAX_CLIENTS = 3;

    /** Event number meaning error on listening socket. */
    public static final int EVT_LISTEN = 4;

    /** Control switch used by main to enable/disable the server. */
    private boolean enabled;

    /** Logs statistics for each client-thread each minute: */
    public static LddsLoggerThread statLoggerThread;

    /** Internal shutdown flag */
    boolean shutdownFlag;

    private String status;

    /** Passed from parent, allows clients to retrieve event messages from Q */
    private QueueLogger qlog;

    /** The archive object from which to serve dcp messages. */
    public MsgArchive msgArchive;

    /** Global mapper for DCP names. */
    NetlistDcpNameMapper globalMapper;

    /** Provides status to clients. */
    public JavaLrgsStatusProvider statusProvider;

    /** Server uses a FileCounter to assign unique ID to each connection. */
    public static final String counterName = "$LRGSHOME/dds-counter";

    private Counter conIdCounter;

    /**
      Constructor.
      @param port the listening port.
      @param bindaddr indicates the NIC to listen on if there are more than one.
      @param msgArchive the MsgArchive object to serve data from.
      @param qlog the QueueLogger to serve event messages from.
      @param statusProvider
      @throws IOException on invalid port number or socket already bound.
    */
    public DdsServer(int port, InetAddress bindaddr, MsgArchive msgArchive,
        QueueLogger qlog, JavaLrgsStatusProvider statusProvider, Pair<ServerSocketFactory,SSLSocketFactory> socketFactories)
        throws IOException
    {
        super(port, bindaddr,socketFactories);
        enabled = false;
        shutdownFlag = false;
        this.msgArchive = msgArchive;
        this.qlog = qlog;
        globalMapper = new NetlistDcpNameMapper(
            new File(EnvExpander.expand(LrgsConfig.instance().ddsNetlistDir)),
            null);
        this.statusProvider = statusProvider;
        conIdCounter = null;
        setModuleName(module);
    }

    /**
      Used by main as a switch to coordinate archive and DDS.
      When disabled, hangup on all clients and don't accept new ones.
      @param tf true if enabling the DDS server.
    */
    public void setEnabled(boolean tf)
    {
        if (tf != enabled)
        {
            if (enabled)
            {
                killAllSvrThreads();
            }
            enabled = tf;
        }
    }

    /** Initializes the application. */
    public void init()
        throws ArchiveUnavailableException
    {
        status = "R";
        BackgroundStuff bs = new BackgroundStuff(this);
        bs.start();
        statLoggerThread = new LddsLoggerThread(this,
            LrgsConfig.instance().ddsUsageLog);
        statLoggerThread.start();
        log.debug("Starting DDS Listening thread.");
        GetHostnameThread ght = GetHostnameThread.instance();
        if (!ght.isAlive())
        {
            ght.start();
        }
        Thread listenThread = new Thread(this);
        listenThread.start();
        try
        {
            conIdCounter = new FileCounter(EnvExpander.expand(counterName));
        }
        catch(IOException ex)
        {
            log.atWarn().setCause(ex).log("Cannot create File-conIdCounter.");
            conIdCounter = new SimpleCounter(1);
        }
    }

    /** Updates the status in the LRGS client slot. */
    public void updateStatus()
    {
        switch(status.length())
        {
            case 0:
            case 10:
                    status = "R"; break;
            case 1: status = "Ru"; break;
            case 2: status = "Run"; break;
            case 3: status = "Runn"; break;
            case 4: status = "Runni"; break;
            case 5: status = "Runnin"; break;
            case 6: status = "Running"; break;
            case 7: status = "Running-"; break;
            case 8: status = "Running--"; break;
            case 9: status = "Running---"; break;
        }
    }

    /** Shuts the application down. */
    public void shutdown()
    {
        log.info("shutting down.");
        shutdownFlag = true;
        statLoggerThread.shutdown();
        setEnabled(false);
        super.shutdown();
    }

    /**
      Overloaded from BasicServer, this constructs a new LddsThread
      to handle this client connection.
      @param sock the client connection socket
    */
    protected BasicSvrThread newSvrThread(Socket sock)
        throws IOException
    {
        try
        {
            log.trace("New DDS client. KeepAlive={} SoLinger={} SoTimeout={} TcpNoDelay={} ReuseAddress={}",
                      sock.getKeepAlive(), sock.getSoLinger(), sock.getSoTimeout(),
                      sock.getTcpNoDelay(), sock.getReuseAddress());
        }
        catch(Exception ex)
        {
            log.atWarn().setCause(ex).log("Exception setting or printing socket options.");
        }

        if (!enabled)
        {
            log.warn("{}:{}- Cannot accept new client, Server disabled.", module, EVT_SVR_DISABLED);
            sock.close();
            return null;
        }

        int numcli = getNumSvrThreads();
        int maxcli = LrgsConfig.instance().ddsMaxClients;
        if (maxcli > 0 && numcli >= maxcli)
        {
            log.warn("{}:{} Cannot accept new client, already have max of {} connected.",
                     module, EVT_MAX_CLIENTS, maxcli);
            sock.close();
            return null;
        }

        AttachedProcess ap = statusProvider.getFreeClientSlot();
        if (ap == null)
        {
            log.warn("{}:{} Cannot get free client data structure, already have max of {} connected.",
                     module, EVT_MAX_CLIENTS, maxcli);
            sock.close();
        }
        else
        {
            log.debug("{}:{} New client accepted", module, -EVT_MAX_CLIENTS);
        }

        try
        {
            int id = conIdCounter.getNextValue();
            //Work around for when file is bad
            if (id == -1)
            {
                conIdCounter.setNextValue(1);
                log.warn("{}:Re-setting {} to 1", module, EnvExpander.expand(counterName));
            }
            id = conIdCounter.getNextValue();
            //End work around
            LddsThread ret = new JLddsThread(this, sock, id, this.socketFactory, msgArchive,
                globalMapper, ap);
            ret.statLogger = statLoggerThread;
            ret.setQueueLogger(qlog);
            ret.setStatusProvider(statusProvider);
            return ret;
        }
        catch(IOException ex)
        {
            throw ex;
        }
        catch(Exception ex)
        {
            String msg = "Unexpected exception creating new client thread";
            log.atError().setCause(ex).log("{}:{} - {}", module, EVT_INTERNAL_CLIENT, msg);
            return null;
        }
    }

    /** From java.lang.Runnable interface. */
    public void run()
    {
        try
        {
            log.trace("{} ServerSocket.getSoTimeout={}", module, listeningSocket.getSoTimeout());
            log.trace("{} ServerSocket.getReceiveBufferSize={}", module, listeningSocket.getReceiveBufferSize());
            log.trace("{} ServerSocket.getReuseAddress={}", module, listeningSocket.getReuseAddress());
        }
        catch(Exception ex)
        {
            log.atWarn().setCause(ex).log("{} Error getting or setting server socket params.", module);
        }
        try { listen(); }
        catch(IOException ex)
        {
            log.atWarn().setCause(ex).log("{}:{}- Error on listening socket: ", module, EVT_LISTEN);
        }
    }

    protected void listenTimeout()
    {
        log.info("{} listen timeout", module);
    }
}


/**
  Check configuration to see if server has been disabled.
*/
class BackgroundStuff extends Thread
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
    DdsServer svr;

    BackgroundStuff(DdsServer svr)
    {
        this.svr = svr;
    }

    public void run()
    {
        long lastNetlistCheck = System.currentTimeMillis();

        try { sleep((long)2000); }
        catch (InterruptedException ie) {}
        while(svr.shutdownFlag == false)
        {
            svr.updateStatus();

            // Hangup on clients who have gone catatonic.
            long now = System.currentTimeMillis();
            LddsThread badClient = null;

            synchronized(svr)
            {
                for(@SuppressWarnings("rawtypes")
                Iterator it = svr.getSvrThreads(); it.hasNext(); )
                {
                    LddsThread lt = (LddsThread)it.next();
                    if ((now - lt.getLastActivity().getTime())
                        > (LddsParams.ServerHangupSeconds*1000L))
                    {
                        badClient = lt;
                        break;
                        // Can't disconnect inside this loop because it will
                        // cause a modification to the vector we're iterating!
                    }
                }
            }
            if (badClient != null) // Found one to hang up on?
            {
                log.debug("{} Hanging up on client '{}' due to inactivity for more than {} seconds.",
                          DdsServer.module, badClient.getClientName(), LddsParams.ServerHangupSeconds);
                badClient.disconnect();
                badClient = null;
            }

            // Check global network lists for change once per minute.
            if (now - lastNetlistCheck > 60000L)
            {
                lastNetlistCheck = now;
                svr.globalMapper.check();
            }

            try { sleep((long)2000); }
            catch (InterruptedException ie) {}
        }
    }
}