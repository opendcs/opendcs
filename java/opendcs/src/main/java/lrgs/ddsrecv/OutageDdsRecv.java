/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*/
package lrgs.ddsrecv;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Date;

import ilex.util.EnvExpander;
import ilex.util.IDateFormat;
import ilex.util.Logger;
import lrgs.common.BadConfigException;
import lrgs.common.DcpAddress;
import lrgs.common.DcpMsg;
import lrgs.common.NetworkList;
import lrgs.common.SearchCriteria;
import lrgs.lrgsmain.LrgsConfig;
import lrgs.lrgsmain.LrgsMain;
import lrgs.lrgsmain.LrgsInputException;
import lrgs.lrgsmain.LrgsInputInterface;
import lrgs.archive.MsgArchive;
import lrgs.db.Outage;
import lrgs.db.LrgsDatabaseThread;
import lrgs.db.LrgsConstants;
import lrgs.drgsrecv.DrgsRecvMsgThread;

/**
This is the main class for the enhanced DDS Receive Module that works
from a prioritized queue of outages. It is new for LRGS version 6.
<p>
The main loop is different in that rather than receiving a constant
stream of data from DDS connections, it processes outages and only
receives data that has been dropped.
<p>
This object is used when the configuration "enableNetbackRecv" variable
is set to true. If false, the base class 'DdsRecv' module is used.
*/
public class OutageDdsRecv
	extends DdsRecv
{
	private Outage currentOutage;
	private LrgsDatabaseThread dbThread;
	private SearchCriteria outageCrit;
	private int numRealMsgsRecovered;
	private int totalNumRecovered;
	private ArrayList<DdsRecvConnection> domsatOutageTried;
	private boolean domsatSeqRecvd[] = null;

	/**
	 * Constructor 
	 * @param lrgsMain the program main object.
	 * @param msgArchive used to store incoming messages.
	 */
	public OutageDdsRecv(LrgsMain lrgsMain, MsgArchive msgArchive)
	{
		super(lrgsMain, msgArchive);
		this.module = "OutageDdsRecv";
		currentOutage = null;
		dbThread = LrgsDatabaseThread.instance();
		outageCrit = null;
		domsatOutageTried = null;
		domsatSeqRecvd = null;
	}

	/**
	 * Sets the last receive time. Called once prior to starting the thread.
	 * @param rt the last receive time, usually retrieved from the quality log.
	 */
	public void setLastMsgRecvTime(long rt)
	{
		if (rt == 0L)
		{
			Logger.instance().info(module + " Initializing for first time. Will"
				+ " start retrieval 1 hour ago.");
			rt = System.currentTimeMillis() - 3600000L;
		}
		else
			Logger.instance().info(module + " Initializing at time "
				+ (new Date(rt)));

		super.setLastMsgRecvTime(rt);
	
		// Assert a new System outage from where we left off.
		Outage sysOutage = new Outage();
		sysOutage.setOutageType(LrgsConstants.systemOutageType);
		sysOutage.setBeginTime(new Date(rt-30000L));
		sysOutage.setEndTime(new Date(System.currentTimeMillis() + 10000L));
		dbThread.assertOutage(sysOutage);
	}

	/** 
 	* Thread run method 
	*/
	public void run()
	{
		Logger.instance().debug1(module + " starting.");
		checkConfig();

		recvConList.start();
		long lastCfgCheck = 0L;

		LrgsConfig cfg = LrgsConfig.instance();
		if (!cfg.enableLritRecv
		 && !cfg.enableDrgsRecv
		 && !cfg.enableDomsatRecv)
		{
			Logger.instance().warning(module +
				" No satellite links enabled. Asserting continuous real-time "
				+ "outage.");
			Outage rtOut = new Outage();
			rtOut.setOutageType(LrgsConstants.realTimeOutageType);
			rtOut.setBeginTime(new Date());
			dbThread.assertOutage(rtOut);
		}
		
		statusCode = DL_STRSTAT;
		status = "Waiting";
		while(!isShutdown)
		{
			if (System.currentTimeMillis() - lastCfgCheck > cfgCheckTime)
			{
				checkConfig();
				lastCfgCheck = System.currentTimeMillis();
			}
			if (cfg.enableDdsRecv)
			{
				status = "Waiting";
				statusCode = DL_STRSTAT;
				processOutage();
			}
			else
			{
				status = "Disabled";
				statusCode = DL_DISABLED;
				currentOutage = null;
				try { sleep(1000L); }
				catch(InterruptedException ex) {}
			}
		}
	}

	/**
	 * Process the highest priority outage in the outage queue.
	 */
	private void processOutage()
	{
		Outage otg = dbThread.highestPriorityOutage();
		if (otg != currentOutage)
		{
			if (currentOutage != null)
				abortCurrentOutage(currentOutage.getStatusCode());
			if (otg != null
			 && otg.getStatusCode() == LrgsConstants.outageStatusActive)
			{
				initOutage(otg);
			}
			else // No outage to process, pause for half a sec.
			{
				try { sleep(500L); }
				catch(InterruptedException ex) {}
			}
		}

		if (currentOutage != null)
			getSomeData();
	}

	/**
	 * Called when the current outage is about to change.
	 */
	private void abortCurrentOutage(char newstat)
	{
		if (currentOutage != null)
		{
			Logger.instance().info(module + " Aborting outage: " + currentOutage
				+ ", newstat=" + newstat);
			status = "Waiting";
			char stat = currentOutage.getStatusCode();
			if (stat != newstat)
			{
				// If status changed, either re-assert or delete current outage.
				if (newstat == LrgsConstants.outageStatusRecovered)
					dbThread.deleteOutage(currentOutage);
				else
				{
					// For DOMSAT outages partially recovered, adjust start
					// & end sequence nums if possible.
					if (currentOutage.getOutageType() 
						== LrgsConstants.domsatGapOutageType
					 && newstat == LrgsConstants.outageStatusPartial)
					{
						int first = -1;
						int last = -1;
						for(int i=0; i < domsatSeqRecvd.length; i++)
							if (!domsatSeqRecvd[i])
							{
								if (first == -1)
									first = i;
								last = i;
							}
						int beg = currentOutage.getBeginSeq();
						currentOutage.setBeginSeq(beg + first);
						currentOutage.setEndSeq(beg + last);
						Logger.instance().info(
		"DOMSAT outage range adjusted to " + currentOutage.getBeginSeq()
							+ " ... " + currentOutage.getEndSeq());
					}
					dbThread.changeOutageStatus(currentOutage, newstat);
				}
			}
		}
		currentOutage = null;
		recvConList.clearCurrentConnection();
	}

	/**
	 * Initializes a new Outage.
	 */
	private void initOutage(Outage otg)
	{
		// Set the 'currentConnection' to null. This will force the first
		// call to getSomeData() to select and initialize a new connection.
		recvConList.currentConnection = null;

		currentOutage = otg;
		if (currentOutage == null)
			return;

		numRealMsgsRecovered = 0;
		totalNumRecovered = 0;
		outageCrit = new SearchCriteria();
		Date d = otg.getBeginTime();
		if (d != null)
			outageCrit.setLrgsSince(IDateFormat.toString(d, false));
		d = otg.getEndTime();
		if (d != null)
			outageCrit.setLrgsUntil(IDateFormat.toString(d, false));

		domsatOutageTried = null;

		switch(currentOutage.getOutageType())
		{
		case LrgsConstants.systemOutageType:
			for(NetlistGroupAssoc nga : ddsRecvSettings.getNetlistGroupAssociations())
				outageCrit.addNetworkList(nga.getNetworkList().makeFileName());
			status = "R:SystemOtg";
			break;
		case LrgsConstants.domsatGapOutageType:
			// Domsat module guarantees that seqEnd > seqStart, so don't
			// worry about wrap-around.
			outageCrit.seqStart = currentOutage.getBeginSeq();
			outageCrit.seqEnd = currentOutage.getEndSeq();
			domsatOutageTried = new ArrayList<DdsRecvConnection>();
			domsatSeqRecvd = 
				new boolean[(outageCrit.seqEnd - outageCrit.seqStart) + 1];
			for(int i=0; i<domsatSeqRecvd.length; i++)
				domsatSeqRecvd[i] = false;
			status = "R:DomsatOtg";
			break;
		case LrgsConstants.damsntOutageType:
			{
				LrgsInputInterface lii = 
					lrgsMain.getLrgsInputById(currentOutage.getSourceId());
				if (lii instanceof DrgsRecvMsgThread)
				{
					DrgsRecvMsgThread drmt = (DrgsRecvMsgThread)lii;
					int chanArray[] = drmt.getChanArray();
					for(int i=0; i< chanArray.length; i++)
						outageCrit.addChannelToken("&" + chanArray[i]);
				}
			}
			status = "R:DamsNtOtg";
			break;
		case LrgsConstants.realTimeOutageType:
		  {
			for(NetlistGroupAssoc nga : ddsRecvSettings.getNetlistGroupAssociations())
				outageCrit.addNetworkList(nga.getNetworkList().makeFileName());
			d = new Date(System.currentTimeMillis() + 2 * 365 * 24 * 3600000L);
			String ds = IDateFormat.toString(d, false);
			outageCrit.setLrgsUntil(ds);
			outageCrit.setDapsUntil(ds);
			status = "R:RealTime";
			break;
		  }
		case LrgsConstants.missingDCPMsgOutageType:
			outageCrit.addDcpAddress(
				new DcpAddress(currentOutage.getDcpAddress()));
			status = "R:DcpOtg";
			break;
		}
		Logger.instance().info("OutageDdsRecv.initOutage: " + currentOutage);
	}

	// Called from getSomeData() when we have selected a new connection.
	protected SearchCriteria buildSearchCrit()
	{
		// Just return the search-crit prepared when we started the outage.
		return outageCrit;
	}

	/**
	 * Archive the message, as does the base class, but also update the
	 * search criteria.
	 */
	protected void archiveMsg(DcpMsg dcpMsg, DdsRecvConnection con)
	{
		msgArchive.archiveMsg(dcpMsg, con);
		char fc = dcpMsg.getFailureCode();
		if (fc == 'G' || fc == '?')
			numRealMsgsRecovered++;
		totalNumRecovered++;
		Date dapsTime = dcpMsg.getDapsTime();

		switch(currentOutage.getOutageType())
		{
		// For these 3 types, update the outage begin time as data comes in.
		// We will drag the outage begin time one minute behind msg received.
		case LrgsConstants.systemOutageType:
		case LrgsConstants.realTimeOutageType:
		case LrgsConstants.damsntOutageType:
			{
				Date outageBegin = currentOutage.getBeginTime();
				long obl = outageBegin.getTime();
				long dtl = dapsTime.getTime();
				if (dtl - 60000L > obl)
					outageBegin.setTime(dtl - 60000L);
				outageCrit.setLrgsSince(
					IDateFormat.toString(outageBegin, false));
			}
			break;
		case LrgsConstants.domsatGapOutageType:
			{
				int seqNum = dcpMsg.getSequenceNum();
				if (seqNum >= 0)
				{
					int outageBeginSeq = currentOutage.getBeginSeq();
					int i = seqNum - outageBeginSeq;
					if (i >= 0 && i < domsatSeqRecvd.length)
						domsatSeqRecvd[i] = true;
				}
			}
			break;
		case LrgsConstants.missingDCPMsgOutageType:
			// No need to do anything special.
			break;
		}
	}

	/**
	 * Called when connection indicates either 'UNTIL' or timeout.
	 * Treat either case the same. Update the current outage.
	 */
	protected void allCaughtUp()
	{
		switch(currentOutage.getOutageType())
		{
		case LrgsConstants.systemOutageType:
			abortCurrentOutage(LrgsConstants.outageStatusRecovered);
			break;
		case LrgsConstants.domsatGapOutageType:
		{
			String conName = recvConList.currentConnection != null ? 
				recvConList.currentConnection.getName() : "(?)";
			if (!checkDomsatResults(conName))
				// Force it to try same outage with different connection.
				recvConList.currentConnection = null;
			else // All received!
				abortCurrentOutage(LrgsConstants.outageStatusRecovered);
			break;
		}
		case LrgsConstants.damsntOutageType:
			// If this was a real-time channel outage, do nothing.
			// ELSE if an end time was specified, set status code.
			if (currentOutage.getEndTime() != null)
			{
				abortCurrentOutage(LrgsConstants.outageStatusRecovered);
			}
			else // On real-time catchup, just sleep and we'll try again.
			{
				try { Thread.sleep(1000L); }
				catch(InterruptedException ex) {}
			}
			break;
		case LrgsConstants.realTimeOutageType:
			// On real-time catchup, just sleep and we'll try again.
			try { Thread.sleep(1000L); }
			catch(InterruptedException ex) {}
			break;
		case LrgsConstants.missingDCPMsgOutageType:
			abortCurrentOutage(
				numRealMsgsRecovered > 0 ? 
					LrgsConstants.outageStatusRecovered :
					LrgsConstants.outageStatusFailed);
			break;
		}
	}

	/**
	 * Overrides base class implementation to handle DOMSAT outages.
	 * For a DOMSAT outage, we must track which connections have already
	 * been tried.
	 */
	protected DdsRecvConnection getConnection()
	{
		// Special behavior is only for DOMSAT outages.
		if (currentOutage.getOutageType() != LrgsConstants.domsatGapOutageType)
			return super.getConnection();

		DdsRecvConnection con = 
			recvConList.getUntriedConnection(domsatOutageTried);
		if (con != null)
			domsatOutageTried.add(con);
		else // No untried connections available.
		{
			checkDomsatResults("final");
	
			char code = (totalNumRecovered > 0)
				? LrgsConstants.outageStatusPartial
				: LrgsConstants.outageStatusFailed;

			// We have now tried all available connections. Set outage failed.
			abortCurrentOutage(code);
		}
		return con;
	}

	/**
	 * Log the results of a domsat recovery attempt.
	 * @return true if all messages were recovered, false if not.
	 */
	private boolean checkDomsatResults(String conName)
	{
		int numMissed = 0;
		int numRecvd = 0;
		for(int i=0; i < domsatSeqRecvd.length; i++)
		{
			if (domsatSeqRecvd[i])
				numRecvd++;
			else
				numMissed++;
		}
		Logger.instance().info("DOMSAT Outage response (" 
			+ conName + ") "
			+ " dropped=" + domsatSeqRecvd.length
			+ ", Seq#Recvd=" + numRecvd 
			+ ", Seq#Missed=" + numMissed
			+ ", totalRecovered=" + totalNumRecovered);

		return totalNumRecovered >= domsatSeqRecvd.length;
	}


	//=====================================================================
	// Methods from LrgsInputInterface
	//=====================================================================

	/**
	 * @return the type of this input interface.
	 */
	public int getType() { return DL_DDS; }

	/**
	 * @return the name of this interface.
	 */
	public String getInputName() { return "Netback:Main"; }

	/**
	 * @return true if this downlink assigns a sequence number to each msg.
	 */
	public boolean hasSequenceNums() { return true; }
}
