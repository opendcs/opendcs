/*
*  $Id$
*/
package lrgs.ldds;

import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.LinkedList;
import java.text.SimpleDateFormat;
import java.io.IOException;

import ilex.util.SequenceFileLogger;
import ilex.util.Logger;
import ilex.util.EnvExpander;
import ilex.net.BasicServer;

import lrgs.db.LrgsDatabaseThread;
import lrgs.db.DdsPeriodStats;

/**
This class implements a background thread that periodically collects
status from each running client thread. The stats are written to a
special log file.
*/
public class LddsLoggerThread extends Thread
    implements StatLogger
{
    BasicServer theServer;
    SequenceFileLogger statLogger;
    SimpleDateFormat dateFormat;
    private boolean shutdownFlag;
    private LrgsDatabaseThread ldt;
    DdsPeriodStats currentHour;
    private long lastLogStatMsec = 0L;
    private long connectMsecs = 0L;   /* connections * msec */
    private long hourStartMsec = 0L;
    private int prevNumClients = 0;

    /**
      Constructor.
      @param svr the server object
      @param logname the file in which to periodically write client stats.
    */
    public LddsLoggerThread(BasicServer svr, String logname)
    {
        theServer = svr;
        shutdownFlag = false;
        dateFormat = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String logFileName = EnvExpander.expand(logname);
        try
        {
            statLogger = new SequenceFileLogger("DDS", logFileName);
            statLogger.setMaxLength(5000000);
            statLogger.setUsePriority(false);
        }
        catch(IOException ex)
        {
            Logger.instance().log(Logger.E_FAILURE,
                "Cannot open '" + logFileName
                + "' DDS stats will not be logged! : " + ex);
            statLogger = null;
        }
        ldt = LrgsDatabaseThread.instance();
        currentHour = null;
    }

    /** Called when server is exiting. */
    public void shutdown()
    {
        shutdownFlag = true;
    }

    /**
      Thread run method: sleep for a minute, then poll each client thread
      and write its status to the file.
    */
    public void run()
    {
        initCurrentHour();
        long lastMin = System.currentTimeMillis() / 60000L;
        long lastHour = lastMin / 60;
        Logger.instance().info("LddsLoggerThread starting. h="
            + lastHour+", m=" + lastMin);
        while(!shutdownFlag && statLogger != null)
        {
            long thisMin = System.currentTimeMillis() / 60000L;
            long thisHour = thisMin / 60;
            if (thisMin > lastMin)
            {
                lastMin = thisMin;
                logStats();
                if (thisHour != lastHour)
                {
                    startNewHour();
                    lastHour = thisHour;
                }
            }
            try { sleep(1000L); }
            catch(InterruptedException ex) {}
        }
        if (currentHour != null)
        {
            ldt.enqueue(currentHour);
        }

        try { sleep(1000L); }
        catch(InterruptedException ex) {}
        if (statLogger != null)
        {
            statLogger.close();
        }
        statLogger = null;
        Logger.instance().info("LddsLoggerThread exiting.");
    }

    private void logStats()
    {
        if (statLogger == null)
        {
            return;
        }
        // Collect & log info from each thread.

        LinkedList svrThreads = theServer.getAllSvrThreads();
        for(Iterator it = svrThreads.iterator(); it.hasNext(); )
        {
            Object ob = it.next();
            if (ob instanceof LddsThread)
            {
                LddsThread lt = (LddsThread)ob;
                logStat(lt);
            }
        }
        ldt.enqueue(currentHour);
        logStat(null);
    }

    /**
     * Log a change in status. Called when any client connects
     * or disconnects, or once per minute from the thread above.
     */
    public synchronized void logStat(LddsThread lt)
    {
        if (statLogger == null)
        {
            return;
        }
        if (lt != null)
        {
            lt.myStats.setLastActivity(lt.getLastActivity());
            statLogger.log(Logger.E_INFORMATION,
                lt.myStats.getConnectionId()
                + " " + lt.getClientName()
                + " " + lt.retrieveNumServed()
                + " " + dateFormat.format(lt.getLastActivity())
                + " " + (lt.user != null ? lt.user.getClientDdsVersion() : ""));
            ldt.enqueue(lt.myStats);
        }

        long now = System.currentTimeMillis();
        LinkedList llst = theServer.getAllSvrThreads();
        for(Iterator it = llst.iterator(); it.hasNext(); )
        {
            LddsThread lddsThread = (LddsThread)it.next();
            currentHour.addMsgsDelivered(lddsThread.myStats.getMsgTally());
        }
        int numClients = theServer.getNumSvrThreads();

        if (currentHour != null)
        {
            doInitCurrentHour();
        }
        int min = currentHour.getMinClients();
        int max = currentHour.getMaxClients();

        if (numClients < min)
        {
            min = numClients;
            currentHour.setMinClients(min);
        }
        if (numClients > max)
        {
            max = numClients;
            currentHour.setMaxClients(max);
        }

        long elapsedMsec = now - lastLogStatMsec;
        long added = elapsedMsec * prevNumClients;
        connectMsecs += added;
        long msecThisHour = now - hourStartMsec;
        if (msecThisHour > 0)
        {
            double ave = (double)connectMsecs / (double)msecThisHour;
            currentHour.setAveClients(ave);
        }
        ldt.enqueue(currentHour);
        prevNumClients = numClients;
        lastLogStatMsec = now;
    }

    public synchronized void rotateLogs()
    {
        statLogger.rotateLogs();
    }

    /**
     * Called once only on startup.
     * Retrieve current hour from DB if it exists.
     * Initialize averaging variables.
     */
    private synchronized void initCurrentHour()
    {
        doInitCurrentHour();
    }

    private void doInitCurrentHour()
    {
        long now = System.currentTimeMillis();
        hourStartMsec = (now / 3600000L) * 3600000L;
        Date perStart = new Date(hourStartMsec);
        currentHour = ldt.getPeriodStats(perStart);

        double ave = currentHour.getAveClients();

        // Initialize tallies for computing the average.
        prevNumClients = 0;
        long elapsedMsec = now - hourStartMsec;
        connectMsecs = (long)(elapsedMsec * ave);
        lastLogStatMsec = now;
    }




    private synchronized void startNewHour()
    {
        if (currentHour != null)
        {
            ldt.enqueue(currentHour);
        }

        currentHour = new DdsPeriodStats();
        long now = System.currentTimeMillis();
        hourStartMsec = (now / 3600000L) * 3600000L;
        currentHour.setStartTime(new Date(hourStartMsec));

        int numClients = theServer.getNumSvrThreads();
        currentHour.setMinClients( numClients );
        currentHour.setMaxClients( numClients );
        currentHour.setAveClients( (double)numClients );

        // Initialize tallies for computing the average.
        prevNumClients = theServer.getNumSvrThreads();
        long elapsedMsec = now - hourStartMsec;
        connectMsecs = (elapsedMsec * numClients);

        lastLogStatMsec = now;

        // Purge dds stats records more than 60 days old.
        Date cutoff = new Date(hourStartMsec - 60 * 24 * 3600000L);
        ldt.deleteDdsStatsBefore(cutoff);
    }



    public synchronized void incrNumAuth()
    {
        currentHour.incrNumAuth();
    }

    public synchronized void incrNumUnAuth()
    {
        currentHour.incrNumUnAuth();
    }

    public synchronized void incrBadPasswords()
    {
        currentHour.incrBadPasswords();
    }

    public synchronized void incrBadUsernames()
    {
        currentHour.incrBadUsernames();
    }
}
