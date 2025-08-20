/*
*  $Id$
*/
package lrgs.statusxml;

import java.net.InetAddress;
import ilex.util.Logger;

import lrgs.apistatus.*;
import lrgs.gui.LrgsApp;
import lrgs.lrgsmain.LrgsInputInterface;
import lrgs.networkdcp.NetworkDcpStatusList;

/**
This class extends the CORBA IDL-Generated LrgsStatusSnapshot
by adding in additional fields not present there.
Since the IDL generates Java code that declares the data structure
as 'final' we have to extend it via a wrapper, rather than inheritance.
*/
public class LrgsStatusSnapshotExt
{
	/** The wrapped IDL-defined data structure */
	public LrgsStatusSnapshot lss;

	/** This host's name. */
	public String hostname;

	/** Overall description of system status, e.g. "running" */
	public String systemStatus;

	/** Global indicator as to whether this LRGS is usable. */
	public boolean isUsable;

	/** Current number of clients this LRGS is supporting */
	public int currentNumClients;

	/** Maximum number of clients that this LRGS can support. */
	public int maxClients;

	/** Maximum number of downlinks that this LRGS can support. */
	public int maxDownlinks;

	/** Major LRGS Version Number */
	public int majorVersion;

	/** Minor LRGS Version Number */
	public int minorVersion;

	/** Full version string with date for display */
	public String fullVersion = null;
	
	/** Network DCP Status List, or null if no net dcps are used. */
	public NetworkDcpStatusList networkDcpStatusList = null;

	public class DownlinkQMs
	{
		/** Hourly Quality measurements for this downlink. */
		public QualityMeasurement dl_qual[];

		public DownlinkQMs()
		{
			dl_qual = new QualityMeasurement[24];
			for(int i=0; i<24; i++)
				dl_qual[i] = new QualityMeasurement(false, 0, 0, 0);
		}
	};

	/** Since the 'dropped' member in QualityMeasurement now means parity
	 * errors, we need a place to explicitly store the number of dropped
	 * messages from DOMSAT. This array contains 24 hours of dropped counts.
	 */
	public int domsatDropped[];
	

	public DownlinkQMs downlinkQMs[];

	/** default constructor */
	public LrgsStatusSnapshotExt()
	{
		clear();
	}

	/** Set all status to its default state. */
	public void clear()
	{
		systemStatus = "unknown";
		isUsable = false;
		currentNumClients = 0;
		lss = new LrgsStatusSnapshot();
		lss.lrgsTime = 0;
		lss.currentHour = 0;
		lss.primaryMissingCount = 0;
		lss.totalRecoveredCount = 0;
		lss.totalGoodCount = 0;
		lss.downLinks = null;
		lss.qualMeas = new QualityMeasurement[24];
		for(int i=0; i<24; i++)
			lss.qualMeas[i] = new QualityMeasurement(false, 0, 0, 0);
		lss.arcStats = new ArchiveStatistics();
		lss.attProcs = null;
		downlinkQMs = null;
		try { hostname = InetAddress.getLocalHost().getHostName(); }
		catch(Exception e) { hostname = "Unknown"; }
		maxClients = 0;
		maxDownlinks = 0;
		majorVersion = LrgsApp.majorVersion;
		minorVersion = LrgsApp.minorVersion;
		domsatDropped = new int[24];
		for(int i=0; i<24; i++)
			domsatDropped[i] = 0;
	}

	/** 
	  This must be called prior to adding any processes. 
	  @param max maximum number of attached processes
	*/
	public void setMaxClients(int max)
	{
		maxClients = max;
		lss.attProcs = new AttachedProcess[max];
		for(int i=0; i<max; i++)
			lss.attProcs[i] = null;
	}
		
	/** 
	  Adds a process to the set at a particular slot. 
	  @param ap The IDL-define AttachedProcess structure
	  @param slot the slot number
	*/
	public void addProcess(AttachedProcess ap, int slot)
	{
		if (slot >= 0 && lss.attProcs != null && slot < lss.attProcs.length)
			lss.attProcs[slot] = ap;
		else
			Logger.instance().log(Logger.E_WARNING, "Invalid process slot "
				+ slot + " -- process status will be discarded.");
	}

	/** 
	  Must be called prior to adding any downlinks. 
	  @param max maximum number of downlinks
	*/
	public void setMaxDownlinks(int max)
	{
		maxDownlinks = max;
		lss.downLinks = new DownLink[max];
		downlinkQMs = new DownlinkQMs[max];
		for(int i=0; i<max; i++)
		{
			lss.downLinks[i] = null;
			downlinkQMs[i] = null;
		}
	}

	/**
	  Places the passed DownLink structure into this collection.
	  @param dl the DownLink structure
	  @param slot the slot number
	*/
	public void addDownLink(DownLink dl, int slot)
	{
		if (slot >= 0 && lss.downLinks != null && slot < lss.downLinks.length)
		{
			lss.downLinks[slot] = dl;
			downlinkQMs[slot] = new DownlinkQMs();
		}
		else
			Logger.instance().log(Logger.E_WARNING, "Invalid downlink slot "
				+ slot + " -- downlink status will be discarded.");
	}

	/**
	  Places the passed QualityMeasurement structure into this collection.
	  @param qm the QualityMeasurement structure
	  @param hour the hour (0...23)
	*/
	public void addQualityMeasurement(QualityMeasurement qm, int hour)
	{
		lss.qualMeas[hour] = qm;
	}

	/**
	  Sets Quality Measurements for a DownLink.
	  @param slot the slot
	  @param qm the QualityMeasurement structure
	  @param hour the hour (0...23)
	*/
	public void addDownlinkQualityMeasurement(int slot, 
		QualityMeasurement qm, int hour)
	{
		downlinkQMs[slot].dl_qual[hour] = qm;
	}

	/**
	 * Returns the array of quality measurements for a given downlink type,
	 * or null if not found.
	 * @param dltype the downlink type
	 * @return the array of quality measurements for a given downlink type,
	 * or null if not found.
	 */
	public QualityMeasurement[] getDownlinkQualityHistory(int dltype)
	{
		for(int slot = 0; slot < maxDownlinks; slot++)
			if (lss.downLinks[slot] != null
			 && lss.downLinks[slot].type == dltype)
				return downlinkQMs[slot].dl_qual;
		return null;
	}

	/**
	 * @return last DOMSAT Sequence number received, or -1 if this
	 * status doesn't contain a DOMSAT downlink.
	 */
	public long getDomsatSeqNum()
	{
		for(int slot=0; slot < maxDownlinks; slot++)
		{
			DownLink dnl = lss.downLinks[slot];
			if (dnl != null
			 && dnl.type == LrgsInputInterface.DL_DOMSAT)
				return dnl.lastSeqNum;
		}
		return -1;
	}
				
}
