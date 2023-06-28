/*
*  $Id$
*/
package lrgs.lrgsmain;

import java.net.InetAddress;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;

import lrgs.apistatus.AttachedProcess;
import lrgs.apistatus.DownLink;
import lrgs.apistatus.ArchiveStatistics;
import lrgs.apistatus.QualityMeasurement;
import lrgs.archive.QualLogFile;
import lrgs.archive.QualLogEntry;
import lrgs.archive.MergeFilter;
import lrgs.common.LrgsStatusProvider;
import lrgs.ddsserver.JLddsThread;
import lrgs.gui.LrgsApp;
import lrgs.ldds.LddsParams;
import lrgs.statusxml.LrgsStatusSnapshotExt;
import lrgs.dqm.DqmInterface;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.slf4j.helpers.Util.getCallingClass;;

/**
  Class JavaLrgsStatusProvider implements the LrgsStatusProvide interface
  by accessing java-only methods in the archive and downlinks.
*/
public class JavaLrgsStatusProvider
    implements LrgsStatusProvider
{
    private static final Logger logger = LoggerFactory.getLogger(getCallingClass());
    /** Reference back to parent. */
    private LrgsMain lrgsMain;

    /** The status snapshot that everyone will work from. */
    private LrgsStatusSnapshotExt lsse;

    /** The last time a message was received. */
    private int lastMsgRcv;

    /** Used to maintain the quality log. */
    QualLogFile qualLogFile;

    /** Current minute's quality log entry. */
    QualLogEntry minuteQuality;

    /** Slot number for the DdsRecv main interface (set from LrgsMain) */
    public int ddsRecvMainSlotNum;

    /** Slot number for the DdsRecv main interface (set from LrgsMain) */
    public int ddsRecvSecMainSlotNum;

    /** Slot number for the DrgsRecv main interface (set from LrgsMain) */
    public int drgsRecvMainSlotNum;

    public int networkDcpMainSlotNum;

    /** Slot number for the NoaaportRecv main interface (set from LrgsMain) */
    public int noaaportRecvMainSlotNum;

    public Date lrgsStartupTime;

    /** Last hour we put dd info into. */
    private int lastDomsatDroppedHour = -1;

    /** The DQM Interface if one is active. */
    private DqmInterface dqmInterface;

    /** Last Domsat Sequence Number Seen */
    public int lastDomsatSequenceNum;

    public long qualLogLastModified = 0L;

    /**
     * Construct the provider and an internal status snapshot structure,
     * along with all the subordinate structures.
     * @param lrgsMain the parent.
     */
    public JavaLrgsStatusProvider(LrgsMain lrgsMain)
    {
        this.lrgsMain = lrgsMain;
        ddsRecvMainSlotNum = -1;
        drgsRecvMainSlotNum = -1;
        noaaportRecvMainSlotNum = -1;
        networkDcpMainSlotNum = -1;
        ddsRecvSecMainSlotNum=-1; // added for secondary group

        lsse = new LrgsStatusSnapshotExt();

        lsse.fullVersion = LrgsApp.AppVersion + " (" + LrgsApp.releasedOn + ")";

        LrgsConfig cfg = LrgsConfig.instance();
        lsse.setMaxDownlinks(cfg.maxDownlinks);
        lsse.setMaxClients(cfg.ddsMaxClients);
        lsse.isUsable = false;
        lsse.currentNumClients = 0;

        lsse.lss.lrgsTime = (int)(System.currentTimeMillis() / 1000L);
        lsse.lss.currentHour = (short)((lsse.lss.lrgsTime / 3600) % 24);
        lsse.lss.primaryMissingCount = 0;
        lsse.lss.totalRecoveredCount = 0;
        lsse.lss.totalGoodCount = 0;

        lsse.lss.downLinks = new DownLink[lsse.maxDownlinks];
        for(int i=0; i<lsse.maxDownlinks; i++)
        {
            lsse.addDownLink(new DownLink(), i);
        }

        lsse.lss.qualMeas = new QualityMeasurement[24];
        for(int i=0; i<24; i++)
        {
            lsse.lss.qualMeas[i] = new QualityMeasurement(false, 0, 0, 0);
        }

        int maxcli = lsse.maxClients == 0 ? 250 : lsse.maxClients;
        lsse.lss.attProcs = new AttachedProcess[maxcli];
        for(int i=0; i<maxcli; i++)
        {
            AttachedProcess ap = new AttachedProcess();
            ap.pid = -1;
            lsse.addProcess(ap, i);
        }

        lsse.lss.arcStats = new ArchiveStatistics();
        lsse.lss.arcStats.dirOldest = 0;
        lsse.lss.arcStats.dirNext = 0;
        lsse.lss.arcStats.dirWrap = (short)0;
        lsse.lss.arcStats.dirSize = 0;
        lsse.lss.arcStats.oldestOffset = 0;
        lsse.lss.arcStats.nextOffset = 0;
        lsse.lss.arcStats.oldestMsgTime = 0;
        lsse.lss.arcStats.lastSeqNum = 0;
        lsse.lss.arcStats.maxMessages = 0;
        lsse.lss.arcStats.maxBytes = 0;
        lastMsgRcv = 0;
        qualLogFile = new QualLogFile("$LRGSHOME/quality.log");
        qualLogLastModified = qualLogFile.lastModified();
        minuteQuality = new QualLogEntry();
        dqmInterface = null;
        lastDomsatSequenceNum = 0;
        lrgsStartupTime = new Date();

        try
        {
            lsse.hostname = InetAddress.getLocalHost().getHostName();
        }
        catch(Exception e)
        {
            lsse.hostname = "Unknown";
        }

        if (cfg.getNoTimeout())
        {
            lsse.systemStatus = "Active";
            lsse.isUsable = true;
        }
    }

    /** Attach to the status provider. */
    public void attach()
    {
    }

    /** Detach from the status provider. */
    public void detach()
    {
        qualLogFile.append(minuteQuality);
        qualLogFile.close();
    }

    /**
     * Sets the DAPS DQM Interface Object.
     */
    public void setDqmInterface(DqmInterface ddi)
    {
        dqmInterface = ddi;
    }

    /**
     * Called periodically (once per second) to gather status.
     */
    public synchronized void updateStatusSnapshot()
    {
        LrgsConfig cfg = LrgsConfig.instance();
        int now = (int)(System.currentTimeMillis() / 1000L);
        if (now/60 != lsse.lss.lrgsTime/60)
        {
            qualLogFile.append(minuteQuality);
            minuteQuality = new QualLogEntry();
        }
        lsse.lss.lrgsTime = now;
        if (lsse.lss.lrgsTime - lastMsgRcv > cfg.timeoutSeconds
         && !cfg.getNoTimeout())
        {
            if (lsse.isUsable)
            {
                lsse.isUsable = false;
                logger.error("{} LRGS Timeout: No data in {} seconds", LrgsMain.EVT_TIMEOUT, cfg.timeoutSeconds);
            }
            lsse.systemStatus = "Timeout";
        }
        else if (!lsse.isUsable)
        {
            logger.info("{} LRGS Recovery: Receiving Data Again!", -LrgsMain.EVT_TIMEOUT);
            lsse.isUsable = true;
            lsse.systemStatus = "Running";
        }

        syncDownlinks();

        lsse.currentNumClients = getAllClients(lsse.lss.attProcs);

        int hour = (lsse.lss.lrgsTime / 3600) % 24;
        if (hour != lsse.lss.currentHour)
        {
            // Starting a new hour, zero out the status structs.
            lsse.lss.currentHour = (short)hour;
            lsse.domsatDropped[hour] = 0;
            QualityMeasurement qm = lsse.lss.qualMeas[hour];
            qm.containsData = true;
            qm.numGood = 0;
            qm.numDropped = 0;
            qm.numRecovered = 0;

            for(int i=0; i<lsse.downlinkQMs.length; i++)
            {
                qm = lsse.downlinkQMs[i].dl_qual[hour];
                qm.containsData = true;
                qm.numGood = 0;
                qm.numDropped = 0;
                qm.numRecovered = 0;
            }
        }

        lsse.lss.arcStats.dirSize = lrgsMain.msgArchive.getTotalMessageCount();
        lsse.lss.arcStats.oldestMsgTime =
            lrgsMain.msgArchive.getOldestDapsTime();
    }

    /**
     * Reads the quality log to initialize the most recent 24 hours of
     * quality info. Thus it is available for displays.
     */
    public synchronized void initQualityLog()
    {
        syncDownlinks();
        qualLogFile.initQualityStatus(lsse);
    }

    private void syncDownlinks()
    {
        for(int i=0; i<lsse.maxDownlinks; i++)
        {
            DownLink dl = lsse.lss.downLinks[i];
            LrgsInputInterface lii = lrgsMain.getLrgsInput(i);
            if (lii != null)
            {
                dl.name = lii.getInputName();
                dl.type = (short)lii.getType();
                dl.hasBER = lii.hasBER();
                dl.hasSeqNum = lii.hasSequenceNums();
                dl.statusCode = (short)lii.getStatusCode();
                if (dl.hasBER)
                {
                    dl.BER = lii.getBER();
                }
                dl.statusString = lii.getStatus();
                if (dl.type == LrgsInputInterface.DL_DDS)
                {
                    ddsRecvMainSlotNum = i;
                }
                else if (dl.type == LrgsInputInterface.DL_DDS_SECONDRAY)
                {
                    ddsRecvSecMainSlotNum = i;
                }
                else if (dl.type == LrgsInputInterface.DL_DRGS)
                {
                    drgsRecvMainSlotNum = i;
                }
                else if (dl.type == LrgsInputInterface.DL_NETWORKDCP)
                {
                    networkDcpMainSlotNum = i;
                }
                dl.group = lii.getGroup();
            }
            else
            {
                dl.type = LrgsInputInterface.DL_UNUSED;
            }
        }
    }

    /**
     * @return downlink of the specified type or null if not found.
     */
    public DownLink findDownLink(int type)
    {
        for(DownLink dl : lsse.lss.downLinks)
        {
            if (type == dl.type)
            {
                return dl;
            }
        }
        return null;
    }

    /**
     * @return a structure with a snapshot of current status.
     */
    public synchronized LrgsStatusSnapshotExt getStatusSnapshot()
    {
        updateStatusSnapshot();
        return lsse;
    }

    /**
      Fills in the passed list of AttachedProcess structures. Fills in the
      entire list. Unused slots will have a blank name.
      @param allClients array of AttachedProcess objects to be populated.
      @return the number of non-blank slots.
    */
    public int getAllClients(AttachedProcess allClients[])
    {
        int i=0;
        try
        {
            LinkedList svrThreads = lrgsMain.getDdsServer().getAllSvrThreads();
            for(Iterator it = svrThreads.iterator();
                i < allClients.length && it.hasNext(); i++)
            {
                JLddsThread jlt = (JLddsThread)it.next();
                AttachedProcess ap = allClients[i];
                ap.lastPollTime = (int)(jlt.getLastActivity().getTime()/1000L);
                int idleSeconds = (int)(System.currentTimeMillis() / 1000L) - ap.lastPollTime;
                ap.stale_count = (short)(idleSeconds / (LddsParams.ServerHangupSeconds/4));
                ap.status = ap.stale_count == 0 ? "running" : ("stale-"+ap.stale_count);
            }
        }
        catch(java.util.NoSuchElementException ex)
        {
            // Ignored
        }
        return i;
    }

    /**
     * Called from msg archiving code when a new message is received.
     * @param dl_slot the unique slot for this downlink interface.
     * @param arcTime unix time_t that this message was archived.
     * @param failcode failure code of message received.
     * @param seqNum sequence number if this message has one, -1 if not.
     * @param idxNum Index number in current day file.
     * @param mergeResult the merge filter result code.
     */
    public synchronized void receivedMsg(int dl_slot, int arcTime,
        char failcode, int seqNum, int idxNum, int mergeResult)
    {
        lastMsgRcv = arcTime;

        if (!lsse.isUsable)
        {
            logger.info("{} LRGS Recovery: Receiving Data Again!",-LrgsMain.EVT_TIMEOUT);
            lsse.isUsable = true;
            lsse.systemStatus = "Running";
        }

        if (idxNum >= 0)
        {
            lsse.lss.arcStats.dirNext = idxNum+1;
        }
        DownLink dl = lsse.lss.downLinks[dl_slot];
        dl.lastMsgRecvTime = arcTime;

        int goodInc = (failcode == 'G') ? 1 : 0;
        int badInc  = (failcode == '?') ? 1 : 0;

        if (seqNum != -1)
        {
            lsse.lss.arcStats.lastSeqNum = seqNum;
            dl.lastSeqNum = seqNum;
        }

        // Update this slot's quality measurement for this hour.
        int hour = (arcTime / 3600) % 24;
        QualityMeasurement qm = lsse.downlinkQMs[dl_slot].dl_qual[hour];
        qm.containsData = true;
        qm.numGood += goodInc;
        qm.numDropped += badInc;

        // Update the 'archived' quality measurements for this hour.
        QualityMeasurement arcQM = lsse.lss.qualMeas[hour];
        arcQM.containsData = true;

        if (mergeResult == MergeFilter.SAVE_DCPMSG)
        {
            arcQM.numGood += goodInc;
            arcQM.numDropped += badInc;
            minuteQuality.archivedGood += goodInc;
            minuteQuality.archivedErr += badInc;
        }
        else if (mergeResult == MergeFilter.OVERWRITE_PREV_BAD)
        {
            // It could be a bad overwriting a bad, so use the incs:
            arcQM.numGood += goodInc;
            arcQM.numDropped += badInc;
            arcQM.numDropped--;
            minuteQuality.archivedGood++;
            minuteQuality.archivedErr--;
        }
        // Else no changes to archived stats: Either discarded or overwrote-good

        /*
          DDS and DRGS are composit downlinks that may have several connections.
          After tracking the individual slot quality, we also aggregate
          statistics in the parent slot.
        */
        if (dl.type == LrgsInputInterface.DL_DDSCON && dl.group.equalsIgnoreCase(LrgsInputInterface.PRIMARY))
        {
            dl = lsse.lss.downLinks[ddsRecvMainSlotNum];
            dl.lastMsgRecvTime = arcTime;
            QualityMeasurement mainQM =
                lsse.downlinkQMs[ddsRecvMainSlotNum].dl_qual[hour];
            mainQM.containsData = true;
            mainQM.numGood += goodInc;
            mainQM.numDropped += badInc;
            minuteQuality.ddsGood += goodInc;
            minuteQuality.ddsErr += badInc;
        }
        else if (dl.type == LrgsInputInterface.DL_DDSCON && dl.group.equalsIgnoreCase(LrgsInputInterface.SECONDARY))
        {
            dl = lsse.lss.downLinks[ddsRecvSecMainSlotNum];
            dl.lastMsgRecvTime = arcTime;
            QualityMeasurement mainQM =
                lsse.downlinkQMs[ddsRecvSecMainSlotNum].dl_qual[hour];
            mainQM.containsData = true;
            mainQM.numGood += goodInc;
            mainQM.numDropped += badInc;
            minuteQuality.ddsGood += goodInc;
            minuteQuality.ddsErr += badInc;
        }
        else if (dl.type == LrgsInputInterface.DL_DRGSCON)
        {
            dl = lsse.lss.downLinks[drgsRecvMainSlotNum];
            dl.lastMsgRecvTime = arcTime;
            QualityMeasurement mainQM =
                lsse.downlinkQMs[drgsRecvMainSlotNum].dl_qual[hour];
            mainQM.containsData = true;
            mainQM.numGood += goodInc;
            mainQM.numDropped += badInc;
            minuteQuality.drgsGood += goodInc;
            minuteQuality.drgsErr += badInc;
        }
        else if (dl.type == LrgsInputInterface.DL_NETDCPCONT
              || dl.type == LrgsInputInterface.DL_NETDCPPOLL)
        {
            dl = lsse.lss.downLinks[networkDcpMainSlotNum];
            dl.lastMsgRecvTime = arcTime;
            QualityMeasurement mainQM =
                lsse.downlinkQMs[networkDcpMainSlotNum].dl_qual[hour];
            mainQM.containsData = true;
            mainQM.numGood += goodInc;
            mainQM.numDropped += badInc;
            minuteQuality.drgsGood += goodInc;
            minuteQuality.drgsErr += badInc;
        }
        else if (dl.type == LrgsInputInterface.DL_DOMSAT)
        {
            minuteQuality.domsatGood += goodInc;
            minuteQuality.domsatErr += badInc;
            lastDomsatSequenceNum = seqNum;
        }
        else if (dl.type == LrgsInputInterface.DL_NOAAPORTCON)
        {
            dl = lsse.lss.downLinks[noaaportRecvMainSlotNum];
            dl.lastMsgRecvTime = arcTime;
            QualityMeasurement npQM =
                lsse.downlinkQMs[noaaportRecvMainSlotNum].dl_qual[hour];
            npQM.containsData = true;
            npQM.numGood += goodInc;
            npQM.numDropped += badInc;
            minuteQuality.noaaportGood += goodInc;
            minuteQuality.noaaportErr += badInc;
        }
        else if (dl.type == LrgsInputInterface.DL_LRIT)
        {
            minuteQuality.lritGood += goodInc;
            minuteQuality.lritErr += badInc;
        }
        else if (dl.type == LrgsInputInterface.DL_GR3110)
        {
            minuteQuality.gr3110Count += goodInc;
        }
        else if (dl.type == LrgsInputInterface.DL_IRIDIUM)
            minuteQuality.iridiumCount += goodInc;
        else if (dl.type == LrgsInputInterface.DL_EDL)
            minuteQuality.edlCount += goodInc;
    }

    /**
     * Called when a domsat dropout is detected.
     * @param numDropped the number of messages dropped.
     * @param arcTime the current time_t
     * @param gapStart the sequence number of the start of the gap.
     * @param elapsedSec the elapsed number of seconds in the gap.
     */
    public synchronized void domsatDropped(int numDropped, int arcTime,
        int gapStart, int elapsedSec)
    {
        int hour = (arcTime / 3600) % 24;
        if (hour != lastDomsatDroppedHour)
        {
            // Starting new hour? Zero it out first.
            lsse.domsatDropped[hour] = 0;
            lastDomsatDroppedHour = hour;
        }
        lsse.domsatDropped[hour] += numDropped;

        minuteQuality.domsatDropped += numDropped;

        /*
         * This is a hook for DAPS, which monitors quality on the DOMSAT
         * uplink. Most LRGS systems will not have a dqmInterface.
         */
        if (dqmInterface != null)
            dqmInterface.domsatDropped(gapStart, numDropped, elapsedSec);
    }


    public void setSystemStatus(String status)
    {
        lsse.systemStatus = status;
    }

    /**
     * Called when we get a new client connection.
     * @return AttachedProcess with which to track the client's status,
     *         or null if all client slots are used.
     */
    public AttachedProcess getFreeClientSlot()
    {
        for(int i=0; i< lsse.lss.attProcs.length; i++)
        {
            AttachedProcess ap = lsse.lss.attProcs[i];
            if (ap.pid == -1)
            {
                return ap;
            }
        }
        return null;
    }

    /**
     * @return ms time for the last time message was received over the
     * DDS link.
     */
    public long getLastDdsReceiveTime()
    {
        if (ddsRecvMainSlotNum == -1)
        {
            logger.debug("StatusProvider.getLastDdsReceiveTime no " +
                "ddsRecvMainSlotNum, returning now - 1 hour");
            return System.currentTimeMillis() - 3600000L;
        }
        else
        {
            DownLink dl = lsse.lss.downLinks[ddsRecvMainSlotNum];
            logger.debug("StatusProvider.getLastDdsReceiveTime returning {}.",
                         new Date(dl.lastMsgRecvTime * 1000L));
            return dl.lastMsgRecvTime * 1000L;
        }
    }

    /**
     * @return ms time for the last time message was received over the
     * DDS Secondary(Backup) link.
     */
    public long getLastSecDdsReceiveTime()
    {
        if (ddsRecvSecMainSlotNum == -1)
        {
            return System.currentTimeMillis() - 3600000L;
        }
        else
        {
            DownLink dl = lsse.lss.downLinks[ddsRecvSecMainSlotNum];
            return dl.lastMsgRecvTime * 1000L;
        }
    }


    /**
     * @return ms time for the last time message was received over any link.
     */
    public long getLastReceiveTime()
    {
        long latest = 0L;
        String latestName = "(unknown)";
        for(int slot = 0; slot<lsse.lss.downLinks.length; slot++)
        {
            if (lsse.lss.downLinks[slot].type != LrgsInputInterface.DL_UNUSED)
            {
                long t = lsse.lss.downLinks[slot].lastMsgRecvTime * 1000L;
                if (t > latest)
                {
                    latest = t;
                    latestName = lsse.lss.downLinks[slot].name;
                }
            }
        }
        logger.info("At LRGS Startup, last msg was received at {} on downlink {}.",
                    new Date(latest),
                    latestName);
        return latest;
    }

    public boolean isUsable()
    {
        return lsse.isUsable;
    }

    public void setIsUsable(boolean tf) { lsse.isUsable = tf; }
}
