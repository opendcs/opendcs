package decodes.snotel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TimeZone;

import decodes.db.DataSource;
import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.RoutingSpec;
import decodes.db.ScheduleEntry;
import decodes.tsdb.CompAppInfo;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TsdbAppTemplate;
import ilex.util.DirectoryMonitorThread;
import ilex.util.EnvExpander;
import ilex.util.FileUtil;
import ilex.util.IDateFormat;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;
import lrgs.common.DcpAddress;
import opendcs.dai.LoadingAppDAI;
import opendcs.dai.ScheduleEntryDAI;

public class ConfigMonitor
	extends DirectoryMonitorThread
	implements FilenameFilter
{
	public static String module = "SnotelConfigMonitor";
	private String configDir = "$DCSTOOL_USERDIR/snotelConfig";
	private String realtimeDir = "$DCSTOOL_USERDIR/snotelRealtime";
	private String historyDir = "$DCSTOOL_USERDIR/historyRealtime";
	private File cfgD = null, rtD = null, hstD = null;
	private long configLMT = 0L, realtimeLMT = 0L, historyLMT = 0L;
	private ArrayList<File> cfgFiles = new ArrayList<File>();
	private ArrayList<File> rtFiles = new ArrayList<File>();
	private ArrayList<File> hstFiles = new ArrayList<File>();
	private SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
	private LinkedList<HistoryRetrieval> histQueue = new LinkedList<HistoryRetrieval>();

	public ConfigMonitor()
	{
		// Dates in files are always interpreted as PST.
		sdf.setTimeZone(TimeZone.getTimeZone("GMT-08:00"));
	}

	@Override
	protected void processFile(File file)
	{
		// does nothing. The accept method adds file to list if it's acceptable.
	}

	@Override
	protected void finishedScan()
	{
	Logger.instance().debug1(module + ".finishedScan #cfgs=" + cfgFiles.size()
			+ ", #rts=" + rtFiles.size() + ", #hst=" + hstFiles.size());
		
Logger.instance().debug1(module + " # sched entries=" + Database.getDb().schedEntryList.size());		
		// For config dir, only take the file with the latest LMT.
		File maxLMT = null;
		for(File f : cfgFiles)
			if (maxLMT == null || f.lastModified() > maxLMT.lastModified())
				maxLMT = f;
		if (maxLMT != null)
		{
			loadConfig(maxLMT);
			configLMT = maxLMT.lastModified();
		}
		cfgFiles.clear();
		
		// For realtime dir, only take the file with the latest LMT.
		maxLMT = null;
		for(File f : rtFiles)
			if (maxLMT == null || f.lastModified() > maxLMT.lastModified())
				maxLMT = f;
		if (maxLMT != null)
		{
			processRealtime(maxLMT);
			realtimeLMT = maxLMT.lastModified();
		}
		rtFiles.clear();
		
		// For history dir, process each file in order
		for(File f : hstFiles)
		{
			if (f.lastModified() > historyLMT)
				historyLMT = f.lastModified();
			
			// read each line of each file into a history retrieval request and queue it.
			readHstFile(f);
		}
		hstFiles.clear();
		
		manageHistoryRetrievals();
	}

	private void processRealtime(File rtList)
	{

		RoutingSpec rtrs = Database.getDb().routingSpecList.find("snotel-realtime");
		if (rtrs == null)
		{
			Logger.instance().failure(module + " cannot process realtime list file because "
				+ "the 'snotel-realtime' routing spec does not exist!");
			return;
		}
		String specFile = rtrs.getProperty("snotelSpecFile");
		if (specFile == null)
		{
			specFile = "$DCSTOOL_USERDIR/snotel-platforms.csv";
			Logger.instance().warning(module + " The 'snotel-realtime' routing spec does not "
				+ "have a specFile property. Using default of " + specFile);
		}

		File sf = new File(EnvExpander.expand(specFile));
		try
		{
			FileUtil.copyFile(rtList, sf);
			Logger.instance().info(module + " received new realtime list file '" + rtList.getPath()
				+ "' copied to specFile location '" + sf.getPath() + "'");
		}
		catch (IOException e)
		{
			Logger.instance().failure(module + " Error copying new realtime list file '"
				+ rtList.getPath() + "' to specFile location '" + sf.getPath() + "'");
		}
	}

	private void loadConfig(File cfgFile)
	{
		Logger.instance().info(module + " received new config file '" + cfgFile.getPath());
		Properties props = new Properties();
		try
		{
			props.load(new FileInputStream(cfgFile));
		}
		catch (IOException ex)
		{
			Logger.instance().failure(module + " Cannot load new config file '" 
				+ cfgFile.getPath() + "' " + ex
				+ " -- Check that file is readable and that it is a valid Java properties file.");
			return;
		}
		
		RoutingSpec rtrs = Database.getDb().routingSpecList.find("snotel-realtime");
		if (rtrs == null)
		{
			Logger.instance().failure(module + " cannot process config file because "
				+ "the 'snotel-realtime' routing spec does not exist!");
			return;
		}
		RoutingSpec hstrs = Database.getDb().routingSpecList.find("snotel-history");
		if (hstrs == null)
		{
			Logger.instance().failure(module + " cannot process config file because "
				+ "the 'snotel-history' routing spec does not exist!");
			return;
		}
		
		ScheduleEntry rtse = getSE("snotel-realtime");
		
		String s = PropertiesUtil.getIgnoreCase(props, "retrievalFrequency");
		if (s != null)
		{
			if (rtse == null)
			{
				Logger.instance().failure(module 
					+ " the 'snotel-realtime' schedule entry does not exist! "
					+ "Cannot change retrievalFrequency");
			}
			else
			{
				rtse.setRunInterval(s);
				try
				{
					rtse.write();
				}
				catch (DatabaseException ex)
				{
					Logger.instance().failure(module 
						+ " Error writing snotel-realtime schedule entry: " + ex);
				}
			}
		}
		
		boolean writeRS = false;
		
		s = PropertiesUtil.getIgnoreCase(props, "fileBufferTime");
		if (s != null)
		{
			try
			{
				Integer.parseInt(s);
				rtrs.setProperty("bufferTimeSec", s);
				hstrs.setProperty("bufferTimeSec", s);
				writeRS = true;
			}
			catch(NumberFormatException ex)
			{
				Logger.instance().failure(module 
					+ " bad 'fileBufferTime' setting '" + s + "' -- should be integer -- ignored.");
			}
		}

		boolean writeDS = false;
		s = PropertiesUtil.getIgnoreCase(props, "lrgs1");
		if (s != null)
		{
			int colon = s.indexOf(':');
			int port = 16003;
			String host = s;
			if (colon > 0)
			{
				host = s.substring(0,colon);
				try { port = Integer.parseInt(s.substring(colon+1)); }
				catch(NumberFormatException ex)
				{
					Logger.instance().failure(module 
						+ " lrgs1 bad port setting in '" + s + "' -- port set to 16003");
					port = 16003;
				}
			}
			DataSource ds = Database.getDb().dataSourceList.get("lrgs1");
			if (ds == null)
			{
				Logger.instance().failure(module 
					+ " cannot process lrgs1 because there is no lrgs1 data source!");
			}
			else
			{
				ds.setDataSourceArg("hostname=" + host + ", port=" + port);
				try
				{
					ds.write();
					writeDS = writeRS = true;
				}
				catch (DatabaseException ex)
				{
					Logger.instance().failure(module + " error writing lrgs1 data source: " + ex);
				}
			}
		}
		s = PropertiesUtil.getIgnoreCase(props, "lrgs2");
		if (s != null)
		{
			int colon = s.indexOf(':');
			int port = 16003;
			String host = s;
			if (colon > 0)
			{
				host = s.substring(0,colon);
				try { port = Integer.parseInt(s.substring(colon+1)); }
				catch(NumberFormatException ex)
				{
					Logger.instance().failure(module 
						+ " lrgs2 bad port setting in '" + s + "' -- port set to 16003");
					port = 16003;
				}
			}
			DataSource ds = Database.getDb().dataSourceList.get("lrgs2");
			if (ds == null)
			{
				Logger.instance().failure(module 
					+ " cannot process lrgs2 because there is no lrgs2 data source!");
			}
			else
			{
				ds.setDataSourceArg("hostname=" + host + ", port=" + port);
				try
				{
					ds.write();
					writeDS = writeRS = true;
				}
				catch (DatabaseException ex)
				{
					Logger.instance().failure(module + " error writing lrgs2 data source: " + ex);
				}
			}
		}
		s = PropertiesUtil.getIgnoreCase(props, "lrgs3");
		if (s != null)
		{
			int colon = s.indexOf(':');
			int port = 16003;
			String host = s;
			if (colon > 0)
			{
				host = s.substring(0,colon);
				try { port = Integer.parseInt(s.substring(colon+1)); }
				catch(NumberFormatException ex)
				{
					Logger.instance().failure(module 
						+ " lrgs3 bad port setting in '" + s + "' -- port set to 16003");
					port = 16003;
				}
			}
			DataSource ds = Database.getDb().dataSourceList.get("lrgs3");
			if (ds == null)
			{
				Logger.instance().failure(module 
					+ " cannot process lrgs3 because there is no lrgs3 data source!");
			}
			else
			{
				ds.setDataSourceArg("hostname=" + host + ", port=" + port);
				try
				{
					ds.write();
					writeDS = writeRS = true;
				}
				catch (DatabaseException ex)
				{
					Logger.instance().failure(module + " error writing lrgs3 data source: " + ex);
				}
			}
		}
		s = PropertiesUtil.getIgnoreCase(props, "lrgs4");
		if (s != null)
		{
			int colon = s.indexOf(':');
			int port = 16003;
			String host = s;
			if (colon > 0)
			{
				host = s.substring(0,colon);
				try { port = Integer.parseInt(s.substring(colon+1)); }
				catch(NumberFormatException ex)
				{
					Logger.instance().failure(module 
						+ " lrgs4 bad port setting in '" + s + "' -- port set to 16003");
					port = 16003;
				}
			}
			DataSource ds = Database.getDb().dataSourceList.get("lrgs4");
			if (ds == null)
			{
				Logger.instance().failure(module 
					+ " cannot process lrgs4 because there is no lrgs4 data source!");
			}
			else
			{
				ds.setDataSourceArg("hostname=" + host + ", port=" + port);
				try
				{
					ds.write();
					writeDS = writeRS = true;
				}
				catch (DatabaseException ex)
				{
					Logger.instance().failure(module + " error writing lrgs4 data source: " + ex);
				}
			}
		}
		
		s = PropertiesUtil.getIgnoreCase(props, "lrgsUser");
		if (s != null)
		{
			rtrs.setProperty("username", s);
			hstrs.setProperty("username", s);
			writeRS = true;
		}
		
		s = PropertiesUtil.getIgnoreCase(props, "lrgsPassword");
		if (s != null)
		{
			rtrs.setProperty("password", s);
			hstrs.setProperty("password", s);
			writeRS = true;
		}
		
		DataSource grp = Database.getDb().dataSourceList.get("lrgsgroup");
		if (writeDS)
		{
			if (grp == null)
				Logger.instance().failure(module + " missing required 'lrgsgroup' data source!");
			else
			{
				try { grp.write(); }
				catch(DatabaseException ex)
				{
					Logger.instance().failure(module + " Error writing group data source: " + ex);
				}
			}
		}

		if (writeRS)
		{
			try
			{
				// the output formatter adds a virtual netlist. Remove it before writing.
				rtrs.dataSource = grp;
				hstrs.dataSource = grp;
				RoutingSpec rtc = rtrs.copy();
				rtc.networkListNames.clear();
				rtc.networkLists.clear();
				rtc.write();
				
				RoutingSpec hstc = hstrs.copy();
				hstc.networkListNames.clear();
				hstc.networkLists.clear();
				hstc.write();
			}
			catch(DatabaseException ex)
			{
				Logger.instance().failure(module + " Error writing routing spec: " + ex);
			}
		}
	}

	/**
	 * Called at the end of each scan.
	 */
	private void manageHistoryRetrievals()
	{
		if (histQueue.isEmpty())
		{
Logger.instance().debug1(module + " histQueue empty");;
			return;
		}
		HistoryRetrieval hr = histQueue.pop();
		Logger.instance().info(module + ".manageHistRet: " + hr.getSpec() + " " 
			+ hr.getStart() + " " + hr.getEnd());
		
		RoutingSpec hstrs = Database.getDb().routingSpecList.find("snotel-history");
		if (hstrs == null)
		{
			Logger.instance().failure(module + " Missing required 'snotel-history' routing spec!");
			return;
		}
		String s = hstrs.getProperty("snotelSpecFile");
		if (s == null)
		{
			Logger.instance().failure(module 
				+ " 'snotel-history' routing spec missing required 'snotelSpecFile' property!");
			return;
		}
			
		File specFile = new File(EnvExpander.expand(s));
		if (specFile.exists())
		{	
			Logger.instance().debug1(module + " history spec file '" + specFile.getPath()
				+ "' with LMT " + new Date(specFile.lastModified()));
			if (System.currentTimeMillis() - specFile.lastModified() < 300000L) // 5 min
				return;
			else specFile.delete();
		}

		PrintWriter pw = null;
		try
		{
			Logger.instance().info(module + " writing history spec file '" + specFile.getPath() + "'");
			pw = new PrintWriter(specFile);
			pw.println(hr.getSpec());
		}
		catch (IOException ex)
		{
			Logger.instance().failure(module + " Cannot write to '" + specFile.getPath() + "': " + ex);
			pw = null;
			return;
		}
		finally
		{
			if (pw != null)
				try { pw.close(); } catch(Exception ex) {}
		}
		
		hstrs.sinceTime = IDateFormat.toString(hr.getStart(), false);
		hstrs.untilTime = IDateFormat.toString(hr.getEnd(), false);
		
		ScheduleEntry hstse = getSE("snotel-history");
		if (hstse == null)
		{
			Logger.instance().failure(module + " Missing required 'snotel-history' schedule entry!");
			return;
		}
		try
		{
			hstrs.write();
			hstse.setEnabled(true);
			hstse.write();
		}
		catch (DatabaseException e)
		{
			Logger.instance().failure(module + " Cannot modify 'snotel-history' schedule entry: " + e);
		}
	}

	/**
	 * Read the passed history file into one or more history requests and queue them.
	 * @param f the file.
	 */
	private void readHstFile(File f)
	{
Logger.instance().debug1(module + " reading history file '" + f.getPath() + "'");
		LineNumberReader lnr = null;
		try
		{
			lnr = new LineNumberReader(new FileReader(f));
			String line;
			while((line = lnr.readLine()) != null)
			{
				line = line.trim();
				if (line.length() == 0 || line.charAt(0) == '#')
					continue;
			
				String fields[] = new String[6];
				StringTokenizer st = new StringTokenizer(line, ",");
				int nt = 0;
				while(nt < 6 && st.hasMoreTokens())
					fields[nt++] = st.nextToken();
				
				if (nt < 6)
				{
					Logger.instance().warning(module + " " 
						+ f.getName() + ":" + lnr.getLineNumber() + " '" + line
						+ "' incorrect number of pipe-separated fields. 6 required. -- Skipped.");
					continue;
				}
				int stationId = 0;
				try { stationId = Integer.parseInt(fields[0]); }
				catch(Exception ex)
				{
					Logger.instance().warning(module + " "
						+ f.getName() + ":" + lnr.getLineNumber() + " '" + line
						+ "' bad site number in first field. Must be integer. -- Skipped.");
					continue;
				}
				String stationName = fields[1];
				
				DcpAddress dcpAddress = new DcpAddress(fields[2].toUpperCase());
				
				if (fields[3].length() == 0)
				{
					Logger.instance().warning(module + " "
						+ f.getName() + ":" + lnr.getLineNumber() + " '" + line	
						+ "' missing data format in last field, should be A or B. -- Skipped.");
					continue;
				}
				char formatFlag = fields[3].charAt(0);
				
				SnotelPlatformSpec spec = new SnotelPlatformSpec(stationId, stationName, 
					dcpAddress, formatFlag);

				Date start = null;
				try { start = sdf.parse(fields[4]); }
				catch(ParseException ex)
				{
					Logger.instance().warning(module + " "
						+ f.getName() + ":" + lnr.getLineNumber() + " '" + line	
						+ "' Second field must have start date as MM/DD/YYYY -- Skipped.");
					continue;
				}
				Date end = null;
				try { end = sdf.parse(fields[5]); }
				catch(ParseException ex)
				{
					Logger.instance().warning(module + " "
						+ f.getName() + ":" + lnr.getLineNumber() + " '" + line	
						+ "' Third field must have end date as MM/DD/YYYY -- Skipped.");
					continue;
				}
				HistoryRetrieval hr = new HistoryRetrieval(spec, start, end);
				histQueue.add(hr);
			}
		}
		catch (IOException ex)
		{
			lnr = null;
			Logger.instance().warning(module + " History File " + f.getName()
				+ " cannot be read: " + ex);
		}
		finally
		{
			if (lnr != null)
				try { lnr.close(); } catch(Exception ex) {}
		}
	}

	@Override
	protected void cleanup()
	{
	}

	public void setConfigDir(String configDir)
	{
		this.configDir = configDir;
Logger.instance().debug1(module + "setConfigDir " + configDir);
	}

	public void setRealtimeDir(String realtimeDir)
	{
Logger.instance().debug1(module + "setRealtimegDir " + realtimeDir);
		this.realtimeDir = realtimeDir;
	}

	public void setHistoryDir(String historyDir)
	{
Logger.instance().debug1(module + "setHistoryDir " + historyDir);
		this.historyDir = historyDir;
	}

	@Override
	public void run()
	{
		Logger.instance().info(module + ".run()");
		
		// The purpose of overloading is to sett a delay before the monitor
		// thread starts so that the directories can be set from reading the
		// properties file.
		try { sleep(10000L); } catch(InterruptedException ex) {}
		super.addDirectory(cfgD = new File(EnvExpander.expand(configDir)));
		super.addDirectory(rtD = new File(EnvExpander.expand(realtimeDir)));
		super.addDirectory(hstD = new File(EnvExpander.expand(historyDir)));
		super.setFilenameFilter(this);
		Logger.instance().info(module + " starting cfgDir=" + cfgD.getPath() + ", rtDir="
			+ rtD.getPath() + ", hstDir=" + hstD.getPath());
		super.run();
	}

	@Override
	public boolean accept(File dir, String name)
	{
		// This is from filename filter. Only accept files whose LMT is after
		// the last one we processed.
		File f = new File(dir, name);
		long lmt = f.lastModified();
		if (cfgD.getPath().equals(dir.getPath()))
		{
			if (lmt > configLMT)
			{
				cfgFiles.add(f);
				return true;
			}
		}
		else if (rtD.getPath().equals(dir.getPath()))
		{
			if (lmt > realtimeLMT)
			{
				rtFiles.add(f);
				return true;
			}
		}
		else if (hstD.getPath().equals(dir.getPath()))
		{
			if (lmt > historyLMT)
			{
				hstFiles.add(f);
				return true;
			}
		}
		return false;
	}
	
	private ScheduleEntry getSE(String name)
	{
		LoadingAppDAI loadingAppDAO = Database.getDb().getDbIo().makeLoadingAppDAO();
		ScheduleEntryDAI scheduleEntryDAO = Database.getDb().getDbIo().makeScheduleEntryDAO();
		try
		{
			CompAppInfo cai = loadingAppDAO.getComputationApp("RoutingScheduler");
			ArrayList<ScheduleEntry> dbEntries = scheduleEntryDAO.listScheduleEntries(cai);
			for(ScheduleEntry se : dbEntries)
				if (se.getName().equalsIgnoreCase(name))
					return se;
			Logger.instance().warning(module + " getSE(" + name + ") - se not found.");
		}
		catch (DbIoException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (NoSuchObjectException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally
		{
			scheduleEntryDAO.close();
			loadingAppDAO.close();
		}
		return null;
	}

	public static void main(String args[])
		throws Exception
	{
		ConfigMonitor cfgMon = new ConfigMonitor();
		cfgMon.readHstFile(new File(args[0]));
		System.out.println("Read " + cfgMon.histQueue.size() + " history requests.");
		for(HistoryRetrieval hr : cfgMon.histQueue)
			System.out.println(hr.toString());
		
	}
}
