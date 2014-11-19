/*
*  $Id$
*/
package decodes.dcpmon;

import ilex.util.PropertiesUtil;

import java.io.File;
import java.util.Enumeration;
import java.util.Properties;

//import decodes.dcpmon_old.DcpGroup;
//import decodes.dcpmon_old.DcpGroupList;



/**
This class holds the configuration information for the DCP Monitor application.
It also contains methods for reading the configuration from a Java properties
file.
*/
public class DcpMonitorConfig
{
	/** Number of days to store data for. */
	public int numDaysStorage = 5;

	/** OMIT the following failure codes from reports: */
	public String omitFailureCodes = "";

	/** limit for red message time alarms */
	public int redMsgTime = 0;

	/** limit for yellow message time alarms */
	public int yellowMsgTime = 1;

	/** Any of these characters will show as red failure codes */
	public String redFailureCodes = "M";

	/** Any of these characters will show as yellow failure codes */
	public String yellowFailureCodes = "?UT";

	/** Values less than this will show as red signal strength. */
	public int redSignalStrength = 30;

	/** Values less than this will show as yellow signal strength. */
	public int yellowSignalStrength = 32;
	/** Offsets greater than this will show in red. */
	public int redFreqOffset = 6;

	/** Offsets greater than this will show in yellow. */
	public int yellowFreqOffset = 5;

	/** BV less than this will show in red. */
	public double redBattery = 9.0;

	/** BV less than this will show in yellow. */
	public double yellowBattery = 11.0;

	/** carrier more than this will result in 'C' code. */
	public long maxCarrierMS = 2500L;
	
	/** Last time configuration was loaded. */
	public long lastLoadTime;

	/** Raw properties read from file. */
	private Properties rawProps;

	/** @deprecated */
	public boolean hadsUse = true;

	public String pdtLocalFile = "/tmp/pdt";
	public String pdtUrl = "https://dcs1.noaa.gov/pdts_compressed.txt";
	public String cdtLocalFile = "/tmp/chans_by_baud.txt";
	public String cdtUrl = "https://dcs1.noaa.gov/chans_by_baud.txt";
	public String nwsXrefLocalFile = "/tmp/nwsxref.txt";
	public String nwsXrefUrl = "http://www.nws.noaa.gov/oh/hads/USGS/ALL_USGS-HADS_SITES.txt";

	/** Name of merge directory. */
	public String mergeDir = "$DECODES_INSTALL_DIR/dcptoimport";
	
	/** File name of the control-district-list file within the merge dir */
	public String controlDistList = "controlling-districts.txt";
	
	/** This is used in particular for the RiverGages group names 
	 * This is used to convert from District to an actual group name
	 * in the dcpmon.conf */
	public String controlDistSuffix = "-RIVERGAGES-DAS";
	
	/** This is the dcpmon type */
	public String dcpmonNameType;
	
	/**
	 * This is used to tell the Dcp Monitor that we want to monitor
	 * all available channels.
	 * Default is false.
	 */
	public boolean allChannels = false;

	/** Set to true to have computations performed when viewing DCP Messages. */
	public boolean enableComputations = false;
	
	/** If computations enabled, you must provide a comp config file. */
	public String compConfig = "$DECODES_INSTALL_DIR/computations.conf";
	
	public String rtstatUrl = "file:///tmp/lrgsstatus.html";
	
	public int statusErrorThreshold = 3600 * 4;
	
	/** @deprecated */
	public String nlNamePrefix = "";
	
	/** For web service components, we need a singleton */
	private static DcpMonitorConfig _instance = new DcpMonitorConfig();
	public static DcpMonitorConfig instance() { return _instance; }
	
	/** Private constructor. Sets default values for all parameters. */
	public DcpMonitorConfig()
	{
		dcpmonNameType = "dcpmon";
//		nlNamePrefix = "DCPMonDONOTMODIFY-";
	}

	/**
	 * Loads the configuration parameters from a properties object.
	 */
	public void loadFromProperties(Properties props)
	{
		String ignorePfx[] = { "grp" };
		PropertiesUtil.loadFromProps(this, props, ignorePfx);
		this.rawProps = props;
		lastLoadTime = System.currentTimeMillis();
	}

	/**
	  Cycles through properties, grabbing any group names. Loads network
	  lists if necessary.
	  @return true if any network lists were changed.
	*/
	public boolean checkAndLoadNetworkLists()
	{
		Enumeration<Object> kenum = rawProps.keys();
//		DcpGroupList dgl = DcpGroupList.instance();
//		dgl.uncheckAll();
		boolean anyChanges = false;
		while(kenum.hasMoreElements())
		{
			String key = (String)kenum.nextElement();
			key = key.toLowerCase();
			if (key.startsWith("group") && key.length() > 6)
			{
				String v = rawProps.getProperty(key);
				if (v.toLowerCase().startsWith("file:"))
				{
					// Group name is basic filename minus dir and ".nl" ext.
					String fname = v.substring(5);
					File f = new File(fname);

					String gName = fname;
					int idx = gName.lastIndexOf(File.separatorChar);
					if (idx != -1)
						gName = gName.substring(idx);
					if (gName.toLowerCase().endsWith(".nl"))
						gName = gName.substring(0,gName.length()-3);

//					DcpGroup dg = dgl.getGroup(gName);
//					if (dg != null)
//					{
//						if (dg.checkForChange())
//							anyChanges = true;
//					}
//					else
//					{
//						dgl.addLrgsNetworkList(gName, f, key);
//						anyChanges = true;
//					}
				}
				else
				{
//					String gName = v;
//					DcpGroup dg = dgl.getGroup(gName);
//					if (dg != null)
//					{
//						if (dg.checkForChange())
//							anyChanges = true;
//					}
//					else
//					{
//						dgl.addDecodesNetworkList(v, key);
//						anyChanges = true;
//					}
				}
			}
		}
//		if (dgl.removeUnchecked())
//			anyChanges = true;
//
//		dgl.sort();

		return anyChanges;
	}

	public String getRtstatUrl()
	{
		return rtstatUrl;
	}
}
