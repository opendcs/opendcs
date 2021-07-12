package decodes.snotel;

/**
 * This bean class holds the config variable for the Snotel Daemon.
 * @author mmaloney
 *
 */
public class SnotelConfig
{
	// The 3 CONTROL-M directories to be monitored:
	public String controlmConfigDir = "$DCSTOOL_USERDIR/controlmConfig";
	public String controlmRealtimeDir = "$DCSTOOL_USERDIR/controlmRealtime";
	public String controlmHistoryDir = "$DCSTOOL_USERDIR/controlmHistory";
	
	/** # seconds to buffer multiple messages into same output file */
	public int fileBufferTime = 5;
	
	/** primary LRGS host:port */
	public String lrgs1 = "nlrgs1.noaa.gov:16003";
	public String lrgs2 = "cdabackup.wcda.noaa.gov:16003";
	public String lrgs3 = "lrgseddn1.cr.usgs.gov:16003";
	public String lrgs4 = "nlrgs2.noaa.gov:16003";
	
	// user and password are required
	public String lrgsUser = null;
	public String lrgsPassword = null;
	
	public String outputDir = "$DCSTOOL_USERDIR/snotel-out";
	
	public String realtimeSince = "now - 1 hour 10 minutes";
	
	public String outputTZ = "GMT-08:00";
	
	public boolean moveToArchive = false;
	
	public String outputTmp = "$DCSTOOL_USERDIR/tmp";
	
	public int retrievalFreq = 60;

	public SnotelConfig()
	{
	}

}
