/*
*
*/
package lrgs.lrgsmon;

import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.util.UserAuthFile;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import ilex.util.EnvExpander;
import lrgs.db.DdsConnectionStats;
import lrgs.db.DdsPeriodStats;
import lrgs.db.LrgsDatabase;
import lrgs.db.LrgsDatabaseException;
import lrgs.lrgsmain.LrgsConfig;

/**
 * Main class for the Lrgs User Utilization Reports.
 * 
 */
public class DdsStatReport
{
	private DdsStatReportGenerator ddsStatRptGen;
	/** The logger */
	private Logger logger;
	private String module = "DdsStatReport";
	private Date startTime;
	private Date endTime;
	private String hourlyFileName;
	private String lrgsName = "";
	private String timeZone = "UTC";
	// Directory in which to place output HTML files
	private File outputDir;
	private final String START_TIME = "startTime";
	private final String END_TIME = "endTime";
	private List<DdsConnectionStats> ddsConnectionList;
	private List<DdsPeriodStats> ddsPeriodStatsList;
	
	/**
	 * Construct main Lrgs Dds Stat Report (User Utilization Report).
	 */
	public DdsStatReport(String inStartTime, String inEndTime, 
						String inHourlyFileName, String inHtmlFilesDirectory,
						String inLrgsConfigFilePath, String inLrgsName)
	{
		logger = Logger.instance();
		//logger.info(module + " startTime arg= " + inStartTime);
		//logger.info(module + " endTime arg = " + inEndTime);
		//logger.info(module + " hourlyFileName arg = " + inHourlyFileName);
		//logger.info(module + " htmlFilesDirectory arg = " + 
		//												inHtmlFilesDirectory);
		//logger.info(module + " lrgs Config file path arg = " + 
		//												inLrgsConfigFilePath);
		//logger.info(module + " lrgs name arg = " + inLrgsName);

		// Load the LRGS DB Configuration file.
		loadLrgsDBConfiguration(inLrgsConfigFilePath);
		
		// Set start to be used when searching DB
		startTime = getInputDate(inStartTime, START_TIME);
		// Set end time to be used when searching DB
		endTime = getInputDate(inEndTime, END_TIME);		
		hourlyFileName = inHourlyFileName;
		lrgsName = inLrgsName;
		setOutputDir(inHtmlFilesDirectory);
	}

	/**
	 * Sets the output directory used to store the 
	 * usage report html files.
	 * 
	 * @param dir directory where the files are stored
	 */
	public void setOutputDir(String dir)
	{
		outputDir = new File(dir);
	}

	/**
	 * Main method for the DdsStatReport Program.
	 * 
	 * @param args arguments given by user
	 */
	public static void main(String[] args)
	{
		// This parses all args & sets up the logger & debug level.
		DdsStatReportCmdLineArgs cmdLineArgs = new DdsStatReportCmdLineArgs();
		cmdLineArgs.parseArgs(args);

		// Instantiate & run the DdsStatReport Program.
		DdsStatReport ddsStatReport=new DdsStatReport(
			cmdLineArgs.getStartTime(),
			cmdLineArgs.getEndTime(),
			cmdLineArgs.getHourlyFileName(),
			EnvExpander.expand(cmdLineArgs.getHtmlFilesDirectory()),
			cmdLineArgs.getLrgsConfigFilePath(),
			cmdLineArgs.getLrgsName());

		ddsStatReport.run();
	}
	
	/**
	 * Called from static main. This method will call the ddsStatRptGen
	 * Object to generate the DDS connection usage report.
	 */
	public void run()
	{
		// Make sure that we have a valid output directory 
		if (!outputDir.isDirectory())
		{
			outputDir.mkdirs();
			if (!outputDir.isDirectory())
			{
				logger.fatal(module + 
						" Cannot access or create output directory '"
						+ outputDir.getPath() + "' -- aborting.");
				System.exit(1);
			}
		}
		
		// Get the dds_connection data from LRGS Database 
		// Get the dds_period_stats data from LRGS Database
		readDdsConnectionInfo();

		// Generate the Usage Report
		ddsStatRptGen = new DdsStatReportGenerator(timeZone);
		ddsStatRptGen.generateDdsStatReport(startTime, endTime, 
											hourlyFileName,outputDir, 
											ddsConnectionList, 
											ddsPeriodStatsList,
											lrgsName);
	}
	
	/**
	 * This method parse the user input start time and end time.
	 * If the given time does not contain the hour it will be set
	 * accorinf to the following rule: if it is start time: hour
	 * will be set to 00:00, if it is end time: hour will be set 
	 * to 23:59.
	 * If the user did not enter any time at all: this method will
	 * return todays date with the hour set as said above.
	 *  
	 * @param givenDate the user start time or end time input
	 * @return Date the formatted input date 
	 */
	private Date getInputDate(String givenDate, String startEndFlag)
	{
		Date retDate = null;
		// Set timezone to timeZone from Lrgs Config or UTC
		TimeZone tz = TimeZone.getTimeZone(timeZone);
 		
 		if (givenDate != null && !(givenDate.startsWith("Today")))
 		{	// Parse assuming user enter HH (hour)
 			DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHH");
 			dateFormat.setTimeZone(tz);
 			try
 			{
 				retDate = dateFormat.parse(givenDate);
 				if (startEndFlag.equals(END_TIME))
 				{	// Add 59 minutes plus 59 seconds plus 999 milliseconds
 					retDate = 
						new Date(retDate.getTime() + 3540000L + 59000L + 999L);
 				}
 			} catch (ParseException e)
 			{	// if we get here, try parsing without the HH
 				DateFormat dateFormatNoHH =	new SimpleDateFormat("yyyyMMdd");
 				dateFormatNoHH.setTimeZone(tz);
 				try
				{
					retDate = dateFormatNoHH.parse(givenDate);
					if (startEndFlag.equals(END_TIME))
					{	// Set hour to 23:59:59:999
						retDate = 
						new Date(retDate.getTime() + 82800000L + 3540000L 
													+ 59000L + 999L);
					}
				} catch (ParseException ex)
				{
					logger.fatal("Cannot parse the following input time '"
							+ givenDate + "' -- aborting. Use Format for"
							+ " start/end time yyyyMMdd -or- yyyyMMddHH");
					System.exit(1);
				}
 			}
 		}
 		else
 		{	// User did not enter time
 			retDate = new Date(); //Default to today's Date
 			long MS_PER_DAY = 86400000; //1 day = 86400000 milliseconds
 			long timeInMs = retDate.getTime();
 			timeInMs = (timeInMs/MS_PER_DAY) * MS_PER_DAY;
 			if (startEndFlag.equals(START_TIME))
			{	// Set hour to 00:00:00 				
 				retDate.setTime(timeInMs);
			}
			else // This is End Time ,Set hour to 23:59:59:999 -> HH:mm:ss:mmm
			{
				timeInMs = timeInMs + MS_PER_DAY -1;
				retDate.setTime(timeInMs);
			}
 		}
 		// This code is for debugging purpose only
 		//DateFormat dateFmt = new SimpleDateFormat("yyyyMMddHHmmssSSS");
 		//dateFmt.setTimeZone(tz);
 		//logger.info("Date Time after formatted= " + dateFmt.format(retDate));
 		
 		return retDate;
	}
	
	/**
	 * This method will connect to the LRGS Database and
	 * read data from the dds_connection table and from the 
	 * dds_period table.
	 *
	 */
	private void readDdsConnectionInfo()
	{		
		// Get username & password
		String username = "lrgs_adm";
		String password = "";
		String authFileName = EnvExpander.expand("$LRGSHOME/.lrgsdb.auth");
		UserAuthFile authFile = new UserAuthFile(authFileName);
		try 
		{
			authFile.read();
			username = authFile.getUsername();
			password = authFile.getPassword();
		}
		catch(Exception ex)
		{
			String msg = module + " Cannot read DB auth from file '" 
				+ authFileName+ "': " + ex;
			Logger.instance().warning(msg);
		}
		Properties credentials = new Properties();
		credentials.setProperty("username", username);
	    credentials.setProperty("password", password);
	
	    LrgsDatabase lrgsDb = null;
	    lrgsDb = new LrgsDatabase();
		//logger.info(module + " Attempting connection to LRGS db '"
		//			+ "' as user '" + username + "'");
		// Connect to LRGS Database and fill out dds arrays.
		try
		{
			lrgsDb.connect(credentials);
			ddsConnectionList = lrgsDb.getConnectionStats(startTime, endTime);
			ddsPeriodStatsList = lrgsDb.getPeriodStats(startTime, endTime);
			lrgsDb.closeConnection();
		}
		catch(LrgsDatabaseException ex)
		{
			logger.fatal(module + ":" +
					" Cannot interface with LRGS database: " + ex);
			System.exit(1);
		}
	}

	/**
	 * This method loads the LRGS DB configuration file.
	 * 
	 * @param lrgsConfigFilePath the file path to the LRGS DB config file
	 */
	private void loadLrgsDBConfiguration(String lrgsConfigFilePath)
	{
		LrgsConfig cfg = LrgsConfig.instance();
		String filePath = EnvExpander.expand(lrgsConfigFilePath);
		cfg.setConfigFileName(filePath);
		try
		{
			cfg.loadConfig();
			String tz = cfg.sqlTimeZone; // Get timezone from config
			if (tz != null)
				timeZone = tz;
		} catch (IOException ex)
		{
			logger.fatal(module + ":" +
					" Cannot load the LRGS database config file: " + ex);
			System.exit(1);
		}
	}
}
