/*
*  $Id$
*/
package decodes.dcpmon;

import ilex.util.PropertiesUtil;

import java.io.File;
import java.util.Enumeration;
import java.util.Properties;



/**
This class holds the configuration information for the DCP Monitor application.
It also contains methods for reading the configuration from a Java properties
file.
*/
public class DcpMonitorConfig
{
	/** private singleton instance */
	private static DcpMonitorConfig _instance = null;

	/** Port to listen on for connections from the GUI. */
	public int serverPort;
//TODO Remove after testing done.
	
	/** DataSource name in the decodes database to use for input. */
	public String dataSourceName;

	/** Number of days to store data for. */
	public int numDaysStorage;

	/** URL pointing to the channel map file. */
	public String channelMapUrl;

	/** Local file name for ChannelMap */
	public String channelMapLocalFile;

	/** OMIT the following failure codes from reports: */
	public String omitFailureCodes;

	/** Optional user name to use in DDS connections. */
	public String ddsUserName;

	/** limit for red message time alarms */
	public int redMsgTime;

	/** limit for yellow message time alarms */
	public int yellowMsgTime;

	/** Any of these characters will show as red failure codes */
	public String redFailureCodes;

	/** Any of these characters will show as yellow failure codes */
	public String yellowFailureCodes;

	/** Values less than this will show as red signal strength. */
	public int redSignalStrength;

	/** Values less than this will show as yellow signal strength. */
	public int yellowSignalStrength;

	/** Offsets greater than this will show in red. */
	public int redFreqOffset;

	/** Offsets greater than this will show in yellow. */
	public int yellowFreqOffset;

	/** BV less than this will show in red. */
	public double redBattery;

	/** BV less than this will show in yellow. */
	public double yellowBattery;

	/** carrier more than this will result in 'C' code. */
	public long maxCarrierMS = 2500L;
	
	/** Last time configuration was loaded. */
	public long lastLoadTime;

	/** Use LRGS arguments here, rather than as defined in database. */
	public String lrgsDataSourceArg;

	/** URL from which to download the PDT, leave null to NOT download. */
	public String pdtUrl;

	/** Local file to load PDT from. */
	public String pdtLocalFile;

	/** Timeout value in seconds to use for LRGS interfaces. */
	public int lrgsTimeout;

	/** Raw properties read from file. */
	private Properties rawProps;

	/** Set to true to use the National Weather Service "HADS SITE" text file
	 * to get dcp name from there, in case DECODES DB does not has it. */
	public boolean hadsUse;

	/** URL from which to download the NWS Hads file, 
	 * leave null to NOT download. */
	public String hadsUrl;

	/** Local file to load the NWS file from. */
	public String hadsLocalFile;

	/** Name of merge directory. */
	public String mergeDir = "$DECODES_INSTALL_DIR/dcptoimport";
	
	/** File name of the control-district-list file within the merge dir */
	public String controlDistList = "controlling-districts.txt";
	
	/** This is used in particular for the RiverGages group names 
	 * This is used to convert from District to an actual group name
	 * in the dcpmon.conf */
	public String controlDistSuffix = "-RIVERGAGES-DAS";
	
	/** This is the dcpmon type */
	public String dcpMonType;
	
	/** This is used to identify the .nl (lrgs old style network list
	 * that are inserted into the SQL Database */
	public String nlNamePrefix;
	
	/**
	 * This is used to tell the Dcp Monitor that we want to monitor
	 * all available channels.
	 * Default is true.
	 */
	public boolean allChannels;

	/** URL to include for the agency home in the page footers */
	public String agencyHomeUrl = "http://mydomain.org/";
	public String agencyHomeDisplay = "My Agency Home";

	/** Set to true to have computations performed when viewing DCP Messages. */
	public boolean enableComputations = false;
	
	/** If computations enabled, you must provide a comp config file. */
	public String compConfig = "$DECODES_INSTALL_DIR/computations.conf";
	
	/** Singleton access method. */
	public static DcpMonitorConfig instance()
	{
		if (_instance == null)
			_instance = new DcpMonitorConfig();
		return _instance;
	}

	/** Private constructor. Sets default values for all parameters. */
	private DcpMonitorConfig()
	{
		serverPort = 17011;
		dataSourceName = "localhost";
		numDaysStorage = 10;
		redMsgTime = 0;
		yellowMsgTime = 2;
		redFailureCodes = "M";
		yellowFailureCodes = "?UT";
		redSignalStrength = 30;
		yellowSignalStrength = 32;
		redFreqOffset = 6;
		yellowFreqOffset = 5;
		redBattery = 9.0;
		yellowBattery = 11.0;
		omitFailureCodes = "";
		ddsUserName = null;
		lrgsDataSourceArg = null;
		pdtUrl = "https://dcs1.noaa.gov/pdts_compressed.txt";
		pdtLocalFile = "$DECODES_INSTALL_DIR/pdt";
		channelMapUrl = "https://dcs1.noaa.gov/chans_by_baud.txt";
		channelMapLocalFile = "$DECODES_INSTALL_DIR/cdt";;
		hadsUse = true;
		hadsUrl =
			"http://www.weather.gov/ohd/hads/compressed_defs/all_dcp_defs.txt";
		hadsLocalFile = "$DECODES_INSTALL_DIR/hads";
		lrgsTimeout = 0;

		dcpMonType = "dcpmon";
		nlNamePrefix = "DCPMonDONOTMODIFY-";
		allChannels = false;
	}

	/**
	 * Loads the configuration parameters from a properties object.
	 */
	public void loadFromProperties(Properties props)
	{
		String ignorePfx[] = { "group" };
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
		DcpGroupList dgl = DcpGroupList.instance();
		dgl.uncheckAll();
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

					DcpGroup dg = dgl.getGroup(gName);
					if (dg != null)
					{
						if (dg.checkForChange())
							anyChanges = true;
					}
					else
					{
						dgl.addLrgsNetworkList(gName, f, key);
						anyChanges = true;
					}
				}
				else
				{
					String gName = v;
					DcpGroup dg = dgl.getGroup(gName);
					if (dg != null)
					{
						if (dg.checkForChange())
							anyChanges = true;
					}
					else
					{
						dgl.addDecodesNetworkList(v, key);
						anyChanges = true;
					}
				}
			}
		}
		if (dgl.removeUnchecked())
			anyChanges = true;

		dgl.sort();

		return anyChanges;
	}
}
