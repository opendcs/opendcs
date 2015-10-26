/*
*  $Id$
*  
*  $Log$
*  Revision 1.11  2015/07/17 13:05:29  mmaloney
*  added 'showComputationEditor' boolean.
*
*  Revision 1.10  2015/02/16 16:18:04  mmaloney
*  Added pollMessageDir and pollRoutingTemplate
*
*  Revision 1.9  2015/02/06 18:47:20  mmaloney
*  Config option to not display certain buttons: Time Series, Groups, Run Comps, & Algorithms.
*
*  Revision 1.8  2015/01/31 15:44:31  mmaloney
*  Configurable Decoded Value Colors.
*
*  Revision 1.7  2015/01/06 16:09:33  mmaloney
*  First cut of Polling Modules
*
*  Revision 1.6  2014/12/11 20:30:54  mmaloney
*  Added eventPurgeDays with default = 5.
*
*  Revision 1.5  2014/10/07 12:34:54  mmaloney
*  DecodesSetting.showNetlistEditor
*
*  Revision 1.4  2014/10/02 14:16:32  mmaloney
*  Added platformListDesignatorCol
*
*  Revision 1.3  2014/05/27 12:54:38  mmaloney
*  dev
*
*  Revision 1.2  2014/05/22 14:13:44  mmaloney
*  Get rid of editPresentationGroup
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.32  2013/08/18 19:51:01  mmaloney
*  Added gui tz
*
*  Revision 1.31  2013/03/28 18:29:56  mmaloney
*  pdt cdt and nws localfiles should be customizable and should default to USERDIR.
*
*  Revision 1.30  2013/03/28 17:29:09  mmaloney
*  Refactoring for user-customizable decodes properties.
*
*  Revision 1.29  2013/03/21 18:27:40  mmaloney
*  DbKey Implementation
*
*  Revision 1.28  2013/02/28 16:43:52  mmaloney
*  Added PDT, CDT, and NWS XRef URL and File settings.
*
*  Revision 1.27  2013/02/20 15:07:24  gchen
*  Enhance a new feature to allow to use the maxComputationRetries property to limit the number of retries for those failed computations. There will be unlimited retries if maxComputationRetires=0.
*
*  This feature will apply to Tempest DB, CWMS, and HDB.
*
*  Revision 1.26  2012/12/12 16:04:42  mmaloney
*  default data source changed from drot to cdadata.
*  People should not be using drot except for troubleshooting domsat.
*
*  Revision 1.25  2012/05/15 15:15:05  mmaloney
*  Use DECODES_INSTALL_DIR, not DCSTOOL_HOME because this is in the legacy branch.
*
*  Revision 1.24  2011/03/18 14:17:40  mmaloney
*  Added param for validations
*
*  Revision 1.23  2011/02/16 15:31:39  mmaloney
*  Added schedulerUiPort, default = 16004
*
*  Revision 1.22  2011/01/06 16:52:52  mmaloney
*  Added 'showPlatformWizard' property.
*
*  Revision 1.21  2011/01/05 13:25:31  mmaloney
*  dev
*
*  Revision 1.20  2010/11/01 18:54:38  mmaloney
*  Added writeCwmsLocations
*
*  Revision 1.19  2010/10/22 18:01:41  mmaloney
*  CCP Refactoring
*
*/
package decodes.util;

import java.util.Properties;
import java.util.Enumeration;

import ilex.util.Logger;
import ilex.util.EnvExpander;
import ilex.util.PropertiesUtil;
import ilex.util.TextUtil;
import decodes.db.Constants;

/**
 * This class is a container for the various settings settable wherever
 * DECODES is installed.  It contains the settings, as well as methods for
 * loading the settings from a properties file.
 * <p>
 * Singleton access can be gained through the instance() method.
 */
public class DecodesSettings
	implements PropertiesOwner
{
	private static DecodesSettings _instance = null;
	
	public enum DbTypes { XML, DECODES_SQL, NWIS, CWMS, HDB, OPENTSDB };

	/** Code meaning NO database (for production only) */
	public static final int DB_NONE = -1;
	/** Code meaning XML database */
	public static final int DB_XML = 0;
	/** Code meaning URL database */
	public static final int DB_URL = 1;
	/** Code meaning SQL database */
	public static final int DB_SQL = 2;
	/** Code meaning NWIS Ingres SQL database */
	public static final int DB_NWIS = 3;
	/** Code meaning CWMS Oracle SQL database */
	public static final int DB_CWMS = 4;
	/** Code meaning OPEN TSDB Database */
	public static final int DB_OPENTSDB = 5;

	/** Editable database type (XML, URL, or SQL) */
 	public String editDatabaseType = "";

 	public int editDatabaseTypeCode = DB_XML;

	/** Editable database location. (directory for XML, URL for SQL) */
	public String editDatabaseLocation = "";

	/** Name of the JDBC Driver Class to use */
	public String jdbcDriverClass = "org.postgresql.Driver";

	/**
	  Name of class used to generate surrogate database keys (not used
	  for XML databases).
	  Default = "decodes.sql.SequenceKeyGenerator";
	*/
	public String sqlKeyGenerator = "decodes.sql.SequenceKeyGenerator";

	/** Format string for writing dates to the SQL database. */
	public String sqlDateFormat = "yyyy-MM-dd HH:mm:ss";

	/** Timezone for date/time stamps in the SQL database. */
	public String sqlTimeZone = "UTC";

	/** Name of file containing encrypted username & password */
	public String DbAuthFile = "$DCSTOOL_USERDIR/.decodes.auth";

	/** Time Series Database Class Name */
	public String dbClassName = "opendcs.opentsdb.OpenTsdb";

	/** Date format to use for parsing dates read from the database. */
	public String SqlReadDateFormat = "yyyy-MM-dd HH:mm:ss";

	/** Time zone used for determining aggregate periods in computations. */
	public String aggregateTimeZone = "UTC";

	/** 
	 * For SQL Database, this sets the Connection autoCommit option. 
	 * Set to true/false. The default is blank, which leaves the connection at
	 * whatever default is provided by the JDBC driver.
	 */
	public String autoCommit = "true";
	
	/** Default Agency for use in variable expansions */
	public String agency = "";
	
	/** Default Location for use in variable expansions */
	public String location = "";

	/** Site name type preference - must match an enumeration value. */
	public String siteNameTypePreference = Constants.snt_NWSHB5;

	/** Part of the config id that indicates who created it. */
	public String decodesConfigOwner = "";

	/** How to treat format labels.
	 * When set to "case-sensitive", it makes all format labels case-sensitive.
	 */
	public String decodesFormatLabelMode = "";

	/** Scan operations scans past EOL character */
	public boolean scanPastEOL = false;

	/** Transport ID type preference - for display only. */
	public String transportMediumTypePreference = Constants.medium_GoesST;

	/** Timezone to use in Script Edit Dialog in db editor. */
	public String editTimeZone;

	/** Name of default DataSource entry for retrieving sample messages */
	public String defaultDataSource = "cdadata.wcda.noaa.gov";

	/** Default dir to store routing spec status properties file: */
	public String routingStatusDir = "$DCSTOOL_USERDIR/routstat";

	/** Default data type standard, used in DB-editor & some formatters. */
	public String dataTypeStdPreference = Constants.datatype_SHEF;
	
	/** Timezone used in Decoding Wizard */
	public String decwizTimeZone = "UTC";

	/** Output format default for Decoding Wizard */
	public String decwizOutputFormat = "stdmsg";

	/** Default debug level for decoding wizard. */
	public int decwizDebugLevel = 0;

	/** Directory for moving raw data files. */
	public String decwizRawDataDir = "$HOME/raw-done";

	/** Directory  for archiving raw data and summary files . */
	public String archiveDataDir = null;

	/** File (template ) for archiving raw data files. */
	public String archiveDataFileName = null;

	/** File (template ) for archiving raw data files. */
	public String archiveSummaryFileName = null;

	/** File (template ) for archiving raw data files. */
	public String decodedDataFileName = null;

	/** Directory to save decoded data in. */
	public String decwizDecodedDataDir = "$HOME/decoded-done";

	/** Name of summary log file for decoding wizard. */
	public String decwizSummaryLog = "$HOME/summary.log";

	/** Provide default designator ( <device-id>-<seqno> for new platforms ) */
	public boolean setPlatformDesignatorName=false;

	/** Set to true if the 1st line of site description contains long name. */
	public boolean hdbSiteDescriptions = false;

	/** Language for internationalization */
	public String language = "en";

	/** Compiler options to use in algorithm editor. */
	public String algoEditCompileOptions = "";

	/** Period (# seconds) at which to check for computation changes. */
	public int compCheckPeriod = 120;

	/** Indicates the minimum algorithm id showed in the comp edit algo list */
	public int minAlgoId = 0;
	
	/** Indicates the minimum computation id showed in the comp edit 
	 * computation list */
	public int minCompId = 0;
	
	/** Indicates the minimum process id showed in the comp edit process list */
	public int minProcId = 0;

	/** 
	 * Max allowable missing values for auto interp/prev/next/closest fill. 
	 * This works for values where the interval is not 0 (i.e. not INSTANT).
	 * The setting here defines the default. It can be overridden by 
	 * computation/algorithm properties of the same name.
	 */
	public int maxMissingValuesForFill = 3;

	/** 
	 * Max allowable missing time for auto interp/prev/next/closest fill. 
	 * This works for any param, including INSTANT. The value is a number
	 * of seconds.
	 * The setting here defines the default. It can be overridden by 
	 * computation/algorithm properties of the same name.
	 */
	public int maxMissingTimeForFill = 3600*3;

	/**
	 * For CWMS Interface, this specifies an override to the office ID.
	 * Normally this is determined by the API based on your login.
	 */
	public String CwmsOfficeId = "";
	
	/** Set to true to allow DECODES to write CWMS Location records */
	public boolean writeCwmsLocations = true;
	
	/** Show the Platform Wizard button on the button panel */
	public boolean showPlatformWizard = false;
	
	/** Show the legacy network list button on the button panel */
	public boolean showNetlistEditor = false;
	
	/** Show the time series list/edit button */
	public boolean showTimeSeriesEditor = true;
	
	public boolean showComputationEditor = true;
	
	/** Show the time series group list/edit button */
	public boolean showGroupEditor = true;
	
	/** Show the 'Test Computations' button */
	public boolean showTestCmputations = true;
	
	/** Show the 'Algorithms' button on the launcher. */
	public boolean showAlgorithmEditor = true;
	
	/** For CWMS Datchk Validation configuration */
	public String datchkConfigFile = "$DCSTOOL_USERDIR/datchk.cfg";
	
	/** Routing Monitor URL */
	public String routingMonitorUrl = "file://$DECODES_INSTALL_DIR/routmon/routmon.html";
	
	/** Command to start browser */
	public String browserCmd = null;
	
	public String pdtLocalFile = "$DCSTOOL_USERDIR/pdt";
	public String pdtUrl = "https://dcs1.noaa.gov/pdts_compressed.txt";
	public String cdtLocalFile = "$DCSTOOL_USERDIR/chans_by_baud.txt";
	public String cdtUrl = "https://dcs1.noaa.gov/chans_by_baud.txt";
	public String nwsXrefLocalFile = "$DCSTOOL_USERDIR/nwsxref.txt";
	public String nwsXrefUrl = "http://www.nws.noaa.gov/oh/hads/USGS/ALL_USGS-HADS_SITES.txt";
	
	/** Set the maximum computation retries for failed records in Task List.
	 *  0: default, unlimited retries; 1: only retry once for failed comp records; etc */
	public int maxComputationRetries = 0 ;
	
	/** Time zone to use in GUI displays */
	public String guiTimeZone = "UTC"; 

	/** Default setting for computation EffectiveStart. Can be overridden by
	 * settings within each computation.
	 */
	public String CpEffectiveStart = "";
	
	/**
	 * If (false) then don't attempt to retry failed computations. 
	 * If (true) then do attempt to retry by using FAIL_TIME in the tasklist
	 * records to retry up to maxComputationRetries set above.
	 */
	public boolean retryFailedComputations = false;
	
	/**
	 * Process the minute offset fields when decoding ASCII self-describing messages.
	 */
	public boolean asciiSelfDescProcessMOFF = true;
	
	/** Default max decimals if no presentation element is found */
	public int defaultMaxDecimals = 4;
	
	public boolean platformListDesignatorCol = false;
	
	/** For OpenDCS 6.1, purge data acquisition events after this many days */
	public int eventPurgeDays = 5;
	
	public String pollScriptDir = "$HOME/SHARED/dacq/poll-scripts";
	public String pollMessageDir = "$HOME/SHARED/dacq/edl-done";
	public String pollRoutingTemplate = "PollGuiTemplate";
	
	public boolean rememberScreenPosition = true;
	
	public String decodeScriptColor1 = null;
	public String decodeScriptColor2 = null;
	public String decodeScriptColor3 = null;
	public String decodeScriptColor4 = null;
	public String decodeScriptColor5 = null;
	public String decodeScriptColor6 = null;
	public String decodeScriptColor7 = null;
	public String decodeScriptColor8 = null;
	
	// Python colors
	public String pyNormalColor      = "0x000000";
	public String pyKeywordColor     = "0x0000FF";
	public String pyBuiltinColor     = "0xD2691E";
	public String pyQuotedColor      = "0x00D000";
	public String pyTsRoleColor      = "0x8B0000";
	public String pyPropColor        = "0x4B0082";
	public String pyCommentColor     = "0x808000";
	public String pyCpFuncColor      = "0x8B4513";

	//===============================================================================
	
	private boolean _isLoaded = false;
	
	public boolean isLoaded() { return _isLoaded; }

	private static PropertySpec propSpecs[] =
	{
//		new PropertySpec("editDatabaseType", 
//			PropertySpec.JAVA_ENUM + "decodes.util.DecodesSettings.DbTypes",
//			"Database types supported by OPENDCS"),
//		new PropertySpec("editDatabaseLocation", PropertySpec.STRING,
//			"Editable database location. (directory for XML, URL for SQL)"),
		new PropertySpec("jdbcDriverClass", PropertySpec.STRING,
			"Name of the JDBC Driver Class to use"),
		new PropertySpec("sqlKeyGenerator", PropertySpec.STRING,
			"Name of class used to generate surrogate SQL database keys"),
		new PropertySpec("sqlDateFormat", PropertySpec.STRING,
			"Format string for writing dates to the SQL database"),
		new PropertySpec("sqlTimeZone", PropertySpec.TIMEZONE,
			"Timezone for date/time stamps in the SQL database"),
		new PropertySpec("DbAuthFile", PropertySpec.FILENAME,
			"Name of file containing encrypted username & password"),
		new PropertySpec("dbClassName", PropertySpec.STRING,
			"Time Series Database Class Name"),
		new PropertySpec("SqlReadDateFormat", PropertySpec.STRING,
			"Date format to use for parsing dates read from the database"),
		new PropertySpec("aggregateTimeZone", PropertySpec.TIMEZONE,
			"Time zone used for determining aggregate periods in computations"),
		new PropertySpec("autoCommit", PropertySpec.BOOLEAN,
			"For SQL Database, this sets the Connection autoCommit option"),
		new PropertySpec("agency", PropertySpec.STRING,
			"Default Agency for use in variable expansions"),
		new PropertySpec("location", PropertySpec.STRING,
			"Default Location for use in variable expansions"),
		new PropertySpec("siteNameTypePreference", PropertySpec.STRING,
			"Site name type preference - must match an value in the reference list"),
		new PropertySpec("decodesConfigOwner", PropertySpec.STRING,
			"Part of the config id that indicates who created it"),
		new PropertySpec("decodesFormatLabelMode", PropertySpec.STRING,
			"When set to 'case-sensitive', it makes all format labels case-sensitive"),
		new PropertySpec("scanPastEOL", PropertySpec.BOOLEAN,
			"DECODES Scan operations scans past EOL character"),
		new PropertySpec("transportMediumTypePreference", PropertySpec.STRING,
			"Transport ID type preference - for display only"),
		new PropertySpec("editOutputFormat", PropertySpec.STRING,
			"Output format to use in Script Edit Dialog in db editor"),
		new PropertySpec("editTimeZone", PropertySpec.TIMEZONE,
			"Timezone to use in Script Edit Dialog in db editor"),
		new PropertySpec("defaultDataSource", PropertySpec.STRING,
			"Name of default DataSource entry for retrieving sample messages"),
		new PropertySpec("routingStatusDir", PropertySpec.DIRECTORY,
			"Default dir to store routing spec status properties file"),
		new PropertySpec("dataTypeStdPreference", PropertySpec.STRING,
			"Default data type standard, used in DB-editor & some formatters"),
		new PropertySpec("decwizTimeZone", PropertySpec.TIMEZONE,
			"Timezone used in decoding wizard GUI"),
		new PropertySpec("decwizOutputFormat", PropertySpec.STRING,
			"Output format default for Decoding Wizard"),
		new PropertySpec("decwizDebugLevel", PropertySpec.INT,
			"Default debug level for decoding wizard (0=none, 3=most verbose)"),
		new PropertySpec("decwizRawDataDir", PropertySpec.DIRECTORY,
			"Directory for moving raw data files"),
		new PropertySpec("archiveDataDir", PropertySpec.DIRECTORY,
			"Directory  for archiving raw data and summary files"),
		new PropertySpec("archiveDataFileName", PropertySpec.STRING,
			"File template for archiving raw data files"),
		new PropertySpec("archiveSummaryFileName", PropertySpec.STRING,
			"File template for archiving raw data files"),
		new PropertySpec("decodedDataFileName", PropertySpec.STRING,
			"File template for archiving raw data files"),
		new PropertySpec("decwizDecodedDataDir", PropertySpec.DIRECTORY,
			"Directory to save decoded data in"),
		new PropertySpec("decwizSummaryLog", PropertySpec.FILENAME,
			"Name of summary log file for decoding wizard"),
		new PropertySpec("setPlatformDesignatorName", PropertySpec.BOOLEAN,
			"Provide default designator (deviceId-seqno) for new platforms"),
		new PropertySpec("hdbSiteDescriptions", PropertySpec.BOOLEAN,
			"Set to true if the 1st line of site description contains long name"),
		new PropertySpec("language", PropertySpec.STRING,
			"Language abbreviation for internationalization (en=English, es=Spanish)"),
		new PropertySpec("algoEditCompileOptions", PropertySpec.STRING,
			"Compiler options to use in algorithm editor"),
		new PropertySpec("compCheckPeriod", PropertySpec.INT,
			"Period (# seconds) at which to check for computation changes"),
		new PropertySpec("minAlgoId", PropertySpec.INT,
			"Indicates the minimum algorithm id showed in the comp edit algo list"),
		new PropertySpec("minCompId", PropertySpec.INT,
			"Indicates the minimum computation id showed in the comp edit computation list"),
		new PropertySpec("minProcId", PropertySpec.STRING,
			"Indicates the minimum process id showed in the comp edit process list"),
		new PropertySpec("maxMissingValuesForFill", PropertySpec.STRING,
			"Max allowable missing values for auto interp/prev/next/closest fill. "
			+ "This works for values where the interval is not 0 (i.e. not INSTANT). "
			+ "The setting here defines the default. It can be overridden by "
			+ "computation/algorithm properties of the same name"),
		new PropertySpec("maxMissingTimeForFill", PropertySpec.INT,
			"# seconds for max allowable missing time for auto interp/prev/next/closest " +
			"fill. The setting here can be overridden by computation/algorithm properties"),
		new PropertySpec("CwmsOfficeId", PropertySpec.STRING,
			"Deprecated for CWMS 2.2. For 2.1 this specifies the office ID to use" +
			" in querying the database."),
		new PropertySpec("writeCwmsLocations", PropertySpec.BOOLEAN,
			"Set to true to allow DECODES to write CWMS Location records"),
		new PropertySpec("showPlatformWizard", PropertySpec.BOOLEAN,
			"Show the Platform Wizard button on the button panel"),
		new PropertySpec("showNetlistEditor", PropertySpec.BOOLEAN,
			"Show the (legacy) network list editor button on the button panel"),
		new PropertySpec("showTimeSeriesEditor", PropertySpec.BOOLEAN,
			"Show the Time Series list/editor button on the button panel"),
		new PropertySpec("showComputationEditor", PropertySpec.BOOLEAN,
			"Show the Computation editor button on the button panel"),
		new PropertySpec("showGroupEditor", PropertySpec.BOOLEAN,
			"Show the Time Series Group list/editor button on the button panel"),
		new PropertySpec("showTestCmputations", PropertySpec.BOOLEAN,
			"Show the Test Computations button on the button panel"),
		new PropertySpec("showAlgorithmEditor", PropertySpec.BOOLEAN,
			"Show the Algorithms Editor button on the button panel"),
		new PropertySpec("datchkConfigFile", PropertySpec.FILENAME,
			"File containing CWMS Datchk Validation configuration"),
		new PropertySpec("routingMonitorUrl", PropertySpec.STRING,
			"Routing Monitor URL"),
		new PropertySpec("browserCmd", PropertySpec.STRING,
			"Command to start browser"),
		new PropertySpec("pdtLocalFile", PropertySpec.FILENAME,
			"GOES PDT is downloaded to this local file"),
		new PropertySpec("pdtUrl", PropertySpec.STRING,
			"GOES PDT is downloaded from this URL"),
		new PropertySpec("cdtLocalFile", PropertySpec.FILENAME,
			"GOES Channel Info stored in this local file"),
		new PropertySpec("cdtUrl", PropertySpec.STRING,
			"GOES Channel Info downloaded from this URL"),
		new PropertySpec("nwsXrefLocalFile", PropertySpec.FILENAME,
			"National Weather Service cross-reference stored in this local file"),
		new PropertySpec("nwsXrefUrl", PropertySpec.STRING,
			"National Weather Service cross-reference downloaded from this URL"),
		new PropertySpec("maxComputationRetries", PropertySpec.INT,
			"Maximum computation retries 0: default, unlimited retries; 1: only " +
			"retry once for failed comp records; etc"),
		new PropertySpec("guiTimeZone", PropertySpec.TIMEZONE,
			"Time zone to use in GUI displays"),
		new PropertySpec("CpEffectiveStart", PropertySpec.STRING,
			"This determines the oldest data that will be processed by the computation" +
			" processor. If defined, it should be a negative increment like '-3 days'"),
		new PropertySpec("retryFailedComputations", PropertySpec.BOOLEAN,
			"If (false) then don't attempt to retry failed computations. If (true)" +
			" then do attempt to retry by using FAIL_TIME in the tasklist records " +
			"to retry up to maxComputationRetries set above"),
		new PropertySpec("defaultMaxDecimals", PropertySpec.INT,
			"Default max decimals if no presentation element is found"),
		new PropertySpec("platformListDesignatorCol", PropertySpec.BOOLEAN,
			"if TRUE, then include a column for Designator in the Platform List"
			+ " of the DECODES Database Editor."),
		new PropertySpec("eventPurgeDays", PropertySpec.INT,
			"For OpenDCS 6.1, purge data acquisition events after this many days. Events "
			+ "are purged by each routing spec with a purgeOldEvents property that is true."),
		new PropertySpec("pollScriptDir", PropertySpec.DIRECTORY,
			"The directory where poll scripts are located."),
		new PropertySpec("pollMessageDir", PropertySpec.DIRECTORY,
			"The directory to open message files after a GUI poll."),
		new PropertySpec("pollRoutingTemplate", PropertySpec.STRING,
			"Name of the routing spec that PollGUI uses as a template."),
		new PropertySpec("rememberScreenPosition", PropertySpec.BOOLEAN,
			"Remember position and size of GUI screens when they are moved."),
		new PropertySpec("decodeScriptColor1", PropertySpec.STRING,
			"Hex color representation for decoded-data color 1. Should be 6 digits."
			+ " That is, 2 hex digits each for the RGB values. Default=0000FF "),
		new PropertySpec("decodeScriptColor2", PropertySpec.STRING,
			"Hex color representation for decoded-data color 2. Should be 6 digits."
			+ " That is, 2 hex digits each for the RGB values. Default=00FFFF "),
		new PropertySpec("decodeScriptColor3", PropertySpec.STRING,
			"Hex color representation for decoded-data color 3. Should be 6 digits."
			+ " That is, 2 hex digits each for the RGB values. Default=D2691E "),
		new PropertySpec("decodeScriptColor4", PropertySpec.STRING,
			"Hex color representation for decoded-data color 4. Should be 6 digits."
			+ " That is, 2 hex digits each for the RGB values. Default=00D000 "),
		new PropertySpec("decodeScriptColor5", PropertySpec.STRING,
			"Hex color representation for decoded-data color 5. Should be 6 digits."
			+ " That is, 2 hex digits each for the RGB values. Default=8B0000 "),
		new PropertySpec("decodeScriptColor6", PropertySpec.STRING,
			"Hex color representation for decoded-data color 6. Should be 6 digits."
			+ " That is, 2 hex digits each for the RGB values. Default=4B0082 "),
		new PropertySpec("decodeScriptColor7", PropertySpec.STRING,
			"Hex color representation for decoded-data color 7. Should be 6 digits."
			+ " That is, 2 hex digits each for the RGB values. Default=808000 "),
		new PropertySpec("decodeScriptColor8", PropertySpec.STRING,
			"Hex color representation for decoded-data color 8. Should be 6 digits."
			+ " That is, 2 hex digits each for the RGB values. Default=8B4513 "),
		new PropertySpec("pyNormalColor", PropertySpec.STRING,
			"Hex color representation for normal text in a python script. Should be 6 digits."
			+ " That is, 2 hex digits each for the RGB values."),
		new PropertySpec("pyKeywordColor", PropertySpec.STRING,
			"Hex color representation for keywords in a python script. Should be 6 digits."
			+ " That is, 2 hex digits each for the RGB values."),
		new PropertySpec("pyBuiltinColor", PropertySpec.STRING,
			"Hex color representation for built-in functions in a python script. Should be 6 digits."
			+ " That is, 2 hex digits each for the RGB values."),
		new PropertySpec("pyQuotedColor", PropertySpec.STRING,
			"Hex color representation for quoted strings in a python script. Should be 6 digits."
			+ " That is, 2 hex digits each for the RGB values."),
		new PropertySpec("pyTsRoleColor", PropertySpec.STRING,
			"Hex color representation for time series roles in a python script. Should be 6 digits."
			+ " That is, 2 hex digits each for the RGB values."),
		new PropertySpec("pyPropColor", PropertySpec.STRING,
			"Hex color representation for comp-property names in a python script. Should be 6 digits."
			+ " That is, 2 hex digits each for the RGB values."),
		new PropertySpec("pyCommentColor", PropertySpec.STRING,
			"Hex color representation for comments in a python script. Should be 6 digits."
			+ " That is, 2 hex digits each for the RGB values."),
		new PropertySpec("pyCpFuncColor", PropertySpec.STRING,
			"Hex color representation for CP Function Names in a python script. Should be 6 digits."
			+ " That is, 2 hex digits each for the RGB values.")
		
	};
	
	/**
	 * Default constructor.  This initializes all of the settings
	 * to their defaults.
	 */
	private DecodesSettings()
	{
	}

	/**
	 * @return the singleton object.
	 */
	public static DecodesSettings instance()
	{
		  if (_instance == null)
			  _instance = new DecodesSettings();
		  return _instance;
	}

	/**
	  Loads setting from the properties, which should have in turn been loaded
	  from the decodes properties file.
	  @param props the pre-loaded properties
	*/
	public void loadFromProperties(Properties props)
	{
		Logger.instance().debug1("Loading properties...");
		PropertiesUtil.loadFromProps(this, props);

		setDbTypeCode();

		editDatabaseLocation = EnvExpander.expand(editDatabaseLocation);

		String owner = System.getenv("ConfigCode");
		if ( owner != null && !owner.equals("") ) {
			decodesConfigOwner = owner;
		}
		_isLoaded = true;
	}
	
	private void setDbTypeCode()
	{
		if (editDatabaseType.equalsIgnoreCase("xml"))
			editDatabaseTypeCode = DB_XML;
		else if (editDatabaseType.equalsIgnoreCase("url"))
			editDatabaseTypeCode = DB_URL;
		else if (editDatabaseType.equalsIgnoreCase("sql"))
			editDatabaseTypeCode = DB_SQL;
		else if (editDatabaseType.equalsIgnoreCase("nwis"))
			editDatabaseTypeCode = DB_NWIS;
		else if (editDatabaseType.equalsIgnoreCase("cwms"))
			editDatabaseTypeCode = DB_CWMS;
		else if (editDatabaseType.equalsIgnoreCase("opentsdb"))
			editDatabaseTypeCode = DB_OPENTSDB;
	}

	/**
	  Loads setting from the properties, which should have in turn been loaded
	  from the decodes properties file.
	  @param props the pre-loaded properties
	*/
	public void loadFromUserProperties(Properties props)
	{
		Logger.instance().log(Logger.E_DEBUG1, "Loading user-custom properties...");
		Properties props2load = new Properties();
		for(Enumeration<?> nme = props.propertyNames(); nme.hasMoreElements(); )
		{
			String name = (String)nme.nextElement();
			props2load.setProperty(name, props.getProperty(name));
		}
		PropertiesUtil.loadFromProps(this, props2load);
		setDbTypeCode();
	}

	
	/**
	  Saves internal settings into properties.
	  @param props the Properties object
	*/
	public void saveToProps(Properties props)
	{
		if (editDatabaseTypeCode == DB_NONE)
			editDatabaseType = "NONE";
		else if (editDatabaseTypeCode == DB_XML)
			editDatabaseType = "XML";
		else if (editDatabaseTypeCode == DB_URL)
			editDatabaseType = "URL";
		else if (editDatabaseTypeCode == DB_SQL)
			editDatabaseType = "SQL";
		else if (editDatabaseTypeCode ==  DB_NWIS)
			editDatabaseType = "NWIS";
		else if (editDatabaseTypeCode ==  DB_CWMS)
			editDatabaseType = "CWMS";
		else if (editDatabaseTypeCode ==  DB_OPENTSDB)
			editDatabaseType = "OPENTSDB";

		PropertiesUtil.storeInProps(this, props, null);
		props.remove("databaseTypeCode");
		props.remove("editDatabaseTypeCode");
	}
	
	public boolean isToolkitOwner()
	{
		return TextUtil.strEqual(EnvExpander.expand("$DCSTOOL_HOME"),
			EnvExpander.expand("$DCSTOOL_USERDIR"));
	}

	@Override
	public PropertySpec[] getSupportedProps()
	{
		return propSpecs;
	}

	/**
	 * Only the props defined herein are allowed.
	 */
	@Override
	public boolean additionalPropsAllowed()
	{
		return false;
	}
}

