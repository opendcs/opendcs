/*
 * This software was written by Cove Software, LLC ("COVE") under contract
 * to Alberta Environment and Sustainable Resource Development (Alberta ESRD).
 * No warranty is provided or implied other than specific contractual terms 
 * between COVE and Alberta ESRD.
 *
 * Copyright 2014 Alberta Environment and Sustainable Resource Development.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package decodes.aesrd;

import ilex.util.EnvExpander;
import ilex.util.FileUtil;
import ilex.util.Logger;
import ilex.util.ProcWaiterCallback;
import ilex.util.ProcWaiterThread;
import ilex.util.PropertiesUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

import opendcs.dai.LoadingAppDAI;
import decodes.tsdb.CompAppInfo;
import decodes.tsdb.CompEventSvr;
import decodes.tsdb.DbIoException;
import decodes.tsdb.LockBusyException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TsdbAppTemplate;
import decodes.tsdb.TsdbCompLock;
import decodes.util.CmdLineArgs;
import decodes.util.PropertiesOwner;
import decodes.util.PropertySpec;

/**
 * Main class for Alberta ESRD SCADA Retrieval.
 * @author mmaloney
 */
public class ScadaRetrieve 
	extends TsdbAppTemplate
	implements PropertiesOwner, ProcWaiterCallback
{
	private static final String module = "ScadaRetrieve";
	private CompAppInfo appInfo = null;
	private TsdbCompLock myLock = null;
	private boolean shutdown = false;
	
	private String dbUrl = "jdbc:jtds://[servername[\\instanceName][:port]]";
	private int numDays = 2;
	private TimeZone dbTZ = TimeZone.getTimeZone("MST");
	private SimpleDateFormat dbSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	private SimpleDateFormat arcNameSdf = new SimpleDateFormat("yyMMdd");
	private File tagFile = null;
	private Date nextQuery = null;
	private int intervalHours = 4;
	private File outputDir = null;
	private File tmpDir = null;
	private String filenameTemplate="scada-${DATE(yyyyMMdd-HHmm)}.zrxp";
	private File dailyArchiveDir = null;
	private int goodQueries = 0;
	private int numErrors = 0;
	private int firstHour = 0, firstMinute = 0;
	private TimeZone zrxpTZ = TimeZone.getTimeZone("MST");
	private SimpleDateFormat zrxpSdf = new SimpleDateFormat("yyyyMMddHHmmss");

	private String cmdAfterFile = null;
	private boolean cmdFinished = false;
	private String cmdInProgress = null;
	private int cmdExitStatus = -1;
	
	private long configIntervalMsec = 600000L; // config every 10 min.
	private long nextConfig = 0L;
	private String tagList = "";
	private static final String queryTemplateDefault = 
		"SELECT v_AnalogHistory.DateTime, lower(v_AnalogHistory.TagName), "
		+ "Value AS data, v_AnalogHistory.Quality "
//		+ "Right(Str([Value]),25) AS data, v_AnalogHistory.Quality "
		+ "FROM v_AnalogHistory "
		+ "WHERE v_AnalogHistory.DateTime>GETDATE()-$numdays AND "
		+ "v_AnalogHistory.TagName IN ($TAGLIST)";
	private String queryTemplate = queryTemplateDefault;
	private String username = "", password = "";
	private String jdbcDriverClass = "net.sourceforge.jtds.jdbc.Driver";
	private File lastFile = null;
	private long DISPLAY_INTERVAL_MS = 1800 * 1000L; // half hour
	private NumberFormat numberFormat = NumberFormat.getNumberInstance();
	private boolean haveLock = false;

	private PropertySpec propSpecs[] = 
	{
		new PropertySpec("dbUrl", PropertySpec.STRING, 
			"URL for connecting to database (jdbc:jtds://host[\\instance][:port])"),
		new PropertySpec("username", PropertySpec.STRING, 
			"Username in the SQL Database"),
		new PropertySpec("password", PropertySpec.STRING, 
			"Password in the SQL Database"),
		new PropertySpec("jdbcDriverClass", PropertySpec.STRING, 
			"Template for output file name. Default=scada-$DATE(yyyyMMdd-HHmm).dat"),
		new PropertySpec("tagfile", PropertySpec.FILENAME, 
			"Pathname of file containing list of SCADA tags"),
		new PropertySpec("numdays", PropertySpec.INT, 
			"Number of days for which to query data (default = 2)"),
		new PropertySpec("dbTZ", PropertySpec.TIMEZONE, 
			"Used to interpret date/time from database and in scheduling queries"),
		new PropertySpec("timeformat", PropertySpec.STRING, 
			"Java SimpleDateFormat spec for date/times from db. Default=MM/dd/yyyy HH:mm:ss"),
		new PropertySpec("firstQuery", PropertySpec.STRING, 
			"hour:minute of first query of day, e.g. 00:37"),
		new PropertySpec("interval", PropertySpec.INT,
			"Interval in hours between queries."),
		new PropertySpec("outputDir", PropertySpec.DIRECTORY,
			"Directory name for output ZRXP files"),
		new PropertySpec("tmpDir", PropertySpec.DIRECTORY,
			"Temporary directory where the files are initially built"),
		new PropertySpec("filenameTemplate", PropertySpec.STRING, 
			"Template for output file name. Default=scada-$DATE(yyyyMMdd-HHmm).dat"),
		new PropertySpec("dailyArchiveDir", PropertySpec.DIRECTORY, 
			"Directory for daily archive files (may be null)"),
		new PropertySpec("zrxpTZ", PropertySpec.TIMEZONE, 
			"Used to format the output ZRXP, default=MST"),
		new PropertySpec("queryFile", PropertySpec.FILENAME,
			"Optional text file containing the SQL query to execute (Overrides default)"),
		new PropertySpec("CmdAfterFile", PropertySpec.STRING,
			"Optional command to execute after finishing file. The command will be passed the "
			+ "filename as an argument.")
	};

	public ScadaRetrieve()
	{
		super("scadaconv.log");
		setSilent(true);
		arcNameSdf.setTimeZone(TimeZone.getTimeZone("MST"));
		numberFormat.setGroupingUsed(false);
		numberFormat.setMaximumFractionDigits(3);
	}

	@Override
	protected void runApp() throws Exception
	{
		setAppStatus("Initializing");
		surviveDatabaseBounce = true;
		init();
		configure();
		
		long lastLockCheck = System.currentTimeMillis();
		// Set lastCheck to cause first check 5 seconds after startup.
		setAppStatus("running");

		// Force next query to happen immediately.
		nextQuery = new Date(0L);
		while(!shutdown)
		{
			Date now = new Date();
			if (now.getTime() >= nextConfig)
			{
				configure();
				if (nextQuery.getTime() != 0L)
					determineNextQuery();
			}
			if (!nextQuery.after(now))
			{
				query();
				if (lastFile != null) // query was successful
				{
					if (dailyArchiveDir != null)
						archive(lastFile);
					if (cmdAfterFile != null)
					{
						StringBuilder sb = new StringBuilder(EnvExpander.expand(cmdAfterFile));
						sb.append(" ");
						sb.append(lastFile.getPath());
						exec(sb.toString());
					}
				}
				determineNextQuery();
			}
			else
				debug("Awaiting next query at " + nextQuery);
			
			if (System.currentTimeMillis() - lastLockCheck > 10000L)
			{
				LoadingAppDAI loadingAppDao = theDb.makeLoadingAppDAO();

				try
				{
					myLock.setStatus("goodQueries=" + goodQueries + ", numErrors=" + numErrors);
					loadingAppDao.checkCompProcLock(myLock);
					appInfo = loadingAppDao.getComputationApp(appId);
				}
				catch (LockBusyException ex)
				{
					warning("Shutting down: lock removed: " + ex);
					shutdown = true;
					haveLock = false;
				}
				finally
				{
					loadingAppDao.close();
				}
			}
			
			try { Thread.sleep(5000L); }
			catch(InterruptedException ex) {}
		}
		info("shutting down.");
		cleanup();
		System.exit(0);
	}
	
	private void query()
	{
		info("Starting query");
		lastFile = null;
		loadTags();
		if (tagList.length() == 0)
		{
			warning("Tag list is empty. Aborting query.");
			numErrors++;
			return;
		}
		
		// Construct JDBC Driver. Then login to database.
		Connection con = connect();
		if (con == null)
		{
			numErrors++;
			return;
		}

		Properties props = new Properties(System.getProperties());
		props.setProperty("numdays", "" + numDays);
		props.setProperty("TAGLIST", tagList);
		String q = EnvExpander.expand(queryTemplate, props);
		
		// Execute the query, saving results in an array.
		ArrayList<ScadaQueryRow> rows = new ArrayList<ScadaQueryRow>();
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			debug("Creating statement");
			stmt = con.createStatement();
			debug("Executing query '" + q + "'");
			rs = stmt.executeQuery(q);
			while(rs != null && rs.next())
			{
				String ds = rs.getString(1);
				String tag = rs.getString(2);
				String data = rs.getString(3);
				String quality = rs.getString(4);
				
debug("row: ds='" + ds + "', tag='" + tag + "', data='" + data + "', qual='" + quality + "'");
				
				if (ds == null || tag == null)
				{
					debug("Discarding null result row: " + ds + ", " + tag + ", "
						+ data + ", " + quality);
					continue;
				}
				ds = ds.trim();
				tag = tag.trim();
				if (data == null || data.equalsIgnoreCase("null") || data.startsWith("-777"))
					continue;
				data = data.trim();
				Date d = null;
				try
				{
					int dot = ds.indexOf('.');
					if (dot > 0 && ds.length() > dot+3)
						ds = ds.substring(0, dot+4);
					d = dbSdf.parse(ds);
if (tag.equalsIgnoreCase("DDDTHY_Pwr_Plant_Flo_cms"))
	debug("\tds='" + ds + "' parsed to " + d + " with tz=" + dbSdf.getTimeZone().getID());
				}
				catch(ParseException ex)
				{
					warning("Cannot parse date for result set: " + ds + ", " + tag + ", "
						+ data + ", " + quality);
					continue;
				}
				rows.add(new ScadaQueryRow(d, tag, data, quality));
			}
		}
		catch (SQLException ex)
		{
			String msg = "Error executing query '" + q + ": " + ex;
			warning(msg);
			System.err.println(msg);
			ex.printStackTrace();
			numErrors++;
			return;
		}
		finally
		{
			if (rs != null) try { rs.close(); } catch(Exception e) {}
			if (stmt != null) try { stmt.close(); } catch(Exception e) {}
			if (con != null) try { con.close(); } catch(Exception e) {}
		}

		// Sort by tag and then date/time.
		Collections.sort(rows);
		
		// Now normalize the times. They only want even half-hour values.
		// Construct a new set with half-hour times by interpolating.
		ArrayList<ScadaQueryRow> displayRows = new ArrayList<ScadaQueryRow>();
		ScadaQueryRow lastRow = null;
		for(ScadaQueryRow sqr : rows)
		{
			long thisMs = sqr.getDatetime().getTime();
			if ((thisMs / DISPLAY_INTERVAL_MS) * DISPLAY_INTERVAL_MS == thisMs)
			{
				displayRows.add(sqr); // this is already an even half hour value.
			}
			else if (lastRow != null && lastRow.getTag().equalsIgnoreCase(sqr.getTag()))
			{
				long t1 = lastRow.getDatetime().getTime();
				long t2 = thisMs;
				double y1, y2;
				try
				{
					y1 = Double.parseDouble(lastRow.getData().trim());
					y2 = Double.parseDouble(sqr.getData().trim());
					long dt = t2 - t1;
					if (dt > 0 && dt < DISPLAY_INTERVAL_MS*4) // can only interpolate 4 intervals.
					{
						double dy = y2 - y1;
						double slope = dy / (double)dt;
						// start at 1st half hour after last sample
						for(long t = ((t1 + (DISPLAY_INTERVAL_MS-1)) / DISPLAY_INTERVAL_MS) * DISPLAY_INTERVAL_MS;
							t < t2; t += DISPLAY_INTERVAL_MS)
						{
							double v = y1 + ((t-t1) / (double)dt) * slope;
							ScadaQueryRow nr = new ScadaQueryRow(new Date(t), sqr.getTag(), 
								numberFormat.format(v), sqr.getQuality());
							displayRows.add(nr);
debug("Added interpolated" + nr);
if (sqr.getTag().equalsIgnoreCase("DDDTHY_Pwr_Plant_Flo_cms"))
	debug("\t1=" + new Date(t1) + ", t2=" + new Date(t2) + ", t=" + new Date(t));

						}
					}
				}
				catch(Exception ex)
				{
					warning("Bad number, cannot interpolate: '" + lastRow.getData() + "', '" + sqr.getData() + "'");
				}
			}
			lastRow = sqr;
		}
		info("After normalization, " + rows.size() + " query rows reduced to " + displayRows.size() 
			+ " display rows.");
		
		String filename = EnvExpander.expand(filenameTemplate);
		File tmpOut = new File(tmpDir, filename);
		PrintWriter pw = null;
		try
		{
			pw = new PrintWriter(new FileWriter(tmpOut));
			String curtag = "";
			for(ScadaQueryRow sqr : displayRows)
			{
				if (!curtag.equals(sqr.getTag()))
				{
					// Start a new ZRXP header
					pw.println("#REXCHANGE" + sqr.getTag() + "|*|RINVAL-777|*|TZ" 
						+ zrxpTZ.getID() + "|*|");
					// they don't want this: pw.println("#LAYOUT(timestamp,value)|*|");

					curtag = sqr.getTag();
				}
				// Print the ZRXP data line
				pw.println(zrxpSdf.format(sqr.getDatetime()) + " " + sqr.getData());
			}
			pw.close();
		}
		catch(IOException ex)
		{
			warning("Error writing to '" + tmpOut.getPath() + "': " + ex);
			numErrors++;
			return;
		}
		finally
		{
			if (pw != null) try {pw.close();} catch(Exception ex) {}
		}

		File outfile = new File(outputDir, filename);
		try { FileUtil.moveFile(tmpOut, outfile); }
		catch(IOException ex)
		{
			warning("Cannot move '" + tmpOut.getPath() + "' to '" 
				+ outfile.getPath() + "': " + ex);
			numErrors++;
			return;
		}
		lastFile = outfile;
		goodQueries++;
	}
	
	private Connection connect()
	{
		Logger.instance().info("Connecting to '" + dbUrl + "' as user '" + username 
			+ "' with driver '" + jdbcDriverClass + "'");
		try
		{
			Class.forName(jdbcDriverClass);
			return DriverManager.getConnection(dbUrl, username, password);
		}
		catch (ClassNotFoundException ex)
		{
			String msg = "Bad jdbcDriverClass '" + jdbcDriverClass + "': " + ex;
			failure(msg);
			System.err.println(msg);
			ex.printStackTrace(System.err);
			return null;
		}
		catch (SQLException ex)
		{
			String msg = "Error connecting to database at  '" + dbUrl 
				+ "' as user " + username + ": " + ex;
			failure(msg);
			System.err.println(msg);
			ex.printStackTrace(System.err);
			return null;
		}
	}
	
	private void loadTags()
	{
		StringBuilder sb = new StringBuilder();
		try
		{
			LineNumberReader lnr = new LineNumberReader(new FileReader(tagFile));
			String line;
			while((line = lnr.readLine()) != null)
			{
				line = line.trim();
				if (line.length() == 0 || line.startsWith("#"))
					continue;
				int space;
				for(space = 0; space < line.length(); space++)
					if (Character.isWhitespace(line.charAt(space)))
						break;
				if (space < line.length())
					line = line.substring(0,space);
				if (sb.length() > 0)
					sb.append(',');
				sb.append("'" + line.toLowerCase() + "'");
			}
			lnr.close();
			tagList = sb.toString();
			debug("taglist=" + tagList);
		}
		catch (FileNotFoundException e)
		{
			warning("Tag file '" + tagFile.getPath() + "' does not exist.");
			tagList = "";
		}
		catch (IOException ex)
		{
			warning("Error reading tag file '" + tagFile.getPath() + "': " + ex);
			tagList = "";
		}
	}

	private void exec(String cmd)
	{
		this.cmdInProgress = cmd;
		int cmdTimeout = 20;
		debug("Executing '" + cmdInProgress 
			+ "' and waiting up to " + cmdTimeout 
			+ " seconds for completion.");
		cmdFinished = false;
		try 
		{
			cmdExitStatus = -1;
			ProcWaiterThread.runBackground(cmdInProgress, 
				"cmdAfterFile", this, cmdInProgress);
		}
		catch(IOException ex)
		{
			warning("Cannot execute '" 
				+ cmdInProgress + "': " + ex);
			cmdInProgress = null;
			cmdFinished = true;
			return;
		}
		long startMsec = System.currentTimeMillis();
		while(!cmdFinished
		 && (System.currentTimeMillis()-startMsec) / 1000L < cmdTimeout)
		{
			try { Thread.sleep(1000L); }
			catch(InterruptedException ex) {}
		}
		if (cmdFinished)
			debug("Command '" + cmdInProgress 
				+ "' completed with exit status " + cmdExitStatus);
		else
			warning("Command '" + cmdInProgress + "' Did not complete!");
	}


	private void archive(File f)
	{
		File archFile = new File(dailyArchiveDir, arcNameSdf.format(new Date()) + ".scda");
		try
		{
			FileUtil.copyStream(new FileInputStream(f), 
				new FileOutputStream(archFile, true));
		}
		catch (IOException ex)
		{
			warning("Error writing to daily archive '" + archFile.getPath() + "': " + ex);
		}
	}


	private void init()
	{
		LoadingAppDAI loadingAppDao = theDb.makeLoadingAppDAO();
		try
		{
			appInfo = loadingAppDao.getComputationApp(appId);
			
			// Look for EventPort and EventPriority properties. If found,
			String evtPorts = appInfo.getProperty("EventPort");
			if (evtPorts != null)
			{
				try 
				{
					int evtPort = Integer.parseInt(evtPorts.trim());
					CompEventSvr compEventSvr = new CompEventSvr(evtPort);
					compEventSvr.startup();
				}
				catch(NumberFormatException ex)
				{
					Logger.instance().warning("App Name " + appInfo.getAppName()
						+ ": Bad EventPort property '" + evtPorts
						+ "' must be integer. -- ignored.");
				}
				catch(IOException ex)
				{
					Logger.instance().failure(
						"Cannot create Event server: " + ex
						+ " -- no events available to external clients.");
				}
			}

			String hostname = "unknown";
			try { hostname = InetAddress.getLocalHost().getHostName(); }
			catch(Exception e) { hostname = "unknown"; }

			myLock = loadingAppDao.obtainCompProcLock(appInfo, getPID(), hostname);
			haveLock = true;
		}
		catch (LockBusyException ex)
		{
			warning("Cannot run: lock busy: " + ex);
			shutdown = true;
			return;
		}
		catch (DbIoException ex)
		{
			warning("Database I/O Error: " + ex);
			shutdown = true;
			return;
		}
		catch (NoSuchObjectException ex)
		{
			warning("Cannot run: No such app name '" + appNameArg.getValue() + "': " + ex);
			shutdown = true;
			return;
		}
		finally
		{
			loadingAppDao.close();
		}
	}
	
	private void configure()
	{
		info("Loading configuration");
		
		LoadingAppDAI loadingAppDao = theDb.makeLoadingAppDAO();
		try { appInfo = loadingAppDao.getComputationApp(appId); }
		catch(Exception ex)
		{
			warning("Cannot read application info: " + ex);
			shutdown = true;
			return;
		}
		finally { loadingAppDao.close(); }
		
		dbUrl = PropertiesUtil.getIgnoreCase(appInfo.getProperties(), "dbUrl");
		if (dbUrl == null)
		{
			warning("Missing required 'dbUrl' application property.");
			shutdown = true;
			return;
		}
		
		String s = PropertiesUtil.getIgnoreCase(appInfo.getProperties(), "numDays");
		if (s != null)
		{
			try { numDays = Integer.parseInt(s); }
			catch(NumberFormatException ex)
			{
				warning("Bad 'numDays' property '" + s + "' -- will use default of 2.");
				numDays = 2;
			}
		}
		
		s = PropertiesUtil.getIgnoreCase(appInfo.getProperties(), "dbTZ");
		if (s != null)
			dbTZ = TimeZone.getTimeZone(s);
		
		s = PropertiesUtil.getIgnoreCase(appInfo.getProperties(), "timeformat");
		if (s != null)
			dbSdf = new SimpleDateFormat(s);
		dbSdf.setTimeZone(dbTZ);
		
		s = PropertiesUtil.getIgnoreCase(appInfo.getProperties(), "zrxpTZ");
		if (s != null)
			zrxpTZ = TimeZone.getTimeZone(s);
		zrxpSdf.setTimeZone(zrxpTZ);
		
		s = PropertiesUtil.getIgnoreCase(appInfo.getProperties(), "tagFile");
		tagFile = new File(EnvExpander.expand(s));
		if (!tagFile.canRead())
		{
			warning("Cannot read tag file '" + s + "'");
			shutdown = true;
			return;
		}
		
		s = PropertiesUtil.getIgnoreCase(appInfo.getProperties(), "outputDir");
		outputDir = new File(EnvExpander.expand(s));
		if (!outputDir.isDirectory() && !outputDir.mkdirs())
		{
			warning("Output Directory '" + s + "' does not exist and cannot be created.");
			shutdown = true;
			return;
		}
		
		s = PropertiesUtil.getIgnoreCase(appInfo.getProperties(), "tmpDir");
		tmpDir = new File(EnvExpander.expand(s));
		if (!tmpDir.isDirectory() && !tmpDir.mkdirs())
		{
			warning("Temporary Directory '" + s + "' does not exist and cannot be created.");
			shutdown = true;
			return;
		}
		
		s = PropertiesUtil.getIgnoreCase(appInfo.getProperties(), "filenameTemplate");
		if (s != null)
			filenameTemplate = s;
		
		s = PropertiesUtil.getIgnoreCase(appInfo.getProperties(), "dailyArchiveDir");
		if (s != null)
		{
			s = EnvExpander.expand(s);
			dailyArchiveDir = new File(s);
			if (!dailyArchiveDir.isDirectory())
			{
				warning("Specified daily archive directory '" + s + "' is not a directory.");
				shutdown = true;
				return;
			}
		}
		
		s = PropertiesUtil.getIgnoreCase(appInfo.getProperties(), "cmdAfterFile");
		if (s != null && s.trim().length() > 0)
			cmdAfterFile = s;

		s = PropertiesUtil.getIgnoreCase(appInfo.getProperties(), "interval");
		if (s != null && s.trim().length() > 0)
		{
			try { intervalHours = Integer.parseInt(s); }
			catch(NumberFormatException ex)
			{
				warning("Bad 'interval' property '" + s + "' -- will use default of 4.");
				intervalHours = 4;
			}
			if (intervalHours <= 0)
			{
				warning("Bad 'interval' property '" + s + "' -- will use default of 4.");
				intervalHours = 4;
			}
		}
		
		s = PropertiesUtil.getIgnoreCase(appInfo.getProperties(), "firstQuery");
		if (s == null)
			firstHour = firstMinute = 0;
		else
		{
			int colon = s.indexOf(':');
			try
			{
				firstHour = Integer.parseInt(s.substring(0,colon));
				firstMinute = Integer.parseInt(s.substring(colon+1));
			}
			catch(Exception ex)
			{
				warning("Bad firstQuery property '" + s + "' -- will use 00:00");
				firstHour = firstMinute = 0;
			}
		}
		
		s = PropertiesUtil.getIgnoreCase(appInfo.getProperties(), "jdbcDriverClass");
		if (s != null && s.trim().length() > 0)
			jdbcDriverClass = s.trim();
		
		username = PropertiesUtil.getIgnoreCase(appInfo.getProperties(), "username");
		password = PropertiesUtil.getIgnoreCase(appInfo.getProperties(), "password");
		
		s = PropertiesUtil.getIgnoreCase(appInfo.getProperties(), "queryFile");
		if (s != null && s.trim().length() > 0)
		{
			String fn = EnvExpander.expand(s);
			try
			{
				queryTemplate = FileUtil.getFileContents(new File(fn)).trim();
			}
			catch(IOException ex)
			{
				warning("Cannot load query from '" + fn + "':" + ex + " -- will use default query.");
				queryTemplate = queryTemplateDefault;
			}
		}
		else
			queryTemplate = queryTemplateDefault;
		
		nextConfig = System.currentTimeMillis() + configIntervalMsec;
	}
	
	private void determineNextQuery()
	{
		Calendar cal = Calendar.getInstance(dbTZ);
		Date now = new Date();
		cal.setTime(now);
		int startDay = cal.get(Calendar.DAY_OF_YEAR);
		cal.set(Calendar.HOUR_OF_DAY, firstHour);
		cal.set(Calendar.MINUTE, firstMinute);
		cal.set(Calendar.SECOND, 0);
		while(cal.getTime().before(now) && cal.get(Calendar.DAY_OF_YEAR) == startDay)
			cal.add(Calendar.HOUR_OF_DAY, intervalHours);
		if (cal.get(Calendar.DAY_OF_YEAR) != startDay)
		{
			// Day advanced to the next day. Next query is first of day, tomorrow.
			cal.set(Calendar.HOUR_OF_DAY, firstHour);
			cal.set(Calendar.MINUTE, firstMinute);
		}
		nextQuery = cal.getTime();
		info("Next query will be at " + dbSdf.format(nextQuery));
	}

	private void cleanup()
	{
		if (haveLock)
		{
			LoadingAppDAI loadingAppDao = theDb.makeLoadingAppDAO();
			try
			{
				loadingAppDao.releaseCompProcLock(myLock);
			}
			catch (DbIoException ex)
			{
				warning("Error attempting to release lock: " + ex);
			}
			finally
			{
				loadingAppDao.close();
			}
		}
	}

	@Override
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		appNameArg.setDefaultValue("AlbertaScada");
	}

	
	
	/**
	 * The main method.
	 * @param args command line arguments.
	 */
	public static void main( String[] args )
		throws Exception
	{
		TsdbAppTemplate theApp = new ScadaRetrieve();
		theApp.execute(args);
	}

	public void info(String msg)
	{
		Logger.instance().info(module + " " + msg);
	}
	private void debug(String msg)
	{
		Logger.instance().debug1(module + " " + msg);
	}
	
	@Override
	public PropertySpec[] getSupportedProps()
	{
		return propSpecs;
	}

	@Override
	public boolean additionalPropsAllowed()
	{
		return true;
	}
	private void setAppStatus(String status)
	{
		if (myLock != null)
			myLock.setStatus(status);
	}

	@Override
	public void procFinished(String procName, Object obj, int exitStatus)
	{
		if (obj != cmdInProgress)
			return;
		cmdFinished = true;
		cmdExitStatus = exitStatus;
	}

}
