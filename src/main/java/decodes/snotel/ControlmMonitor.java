package decodes.snotel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TimeZone;

import ilex.util.DirectoryMonitorThread;
import ilex.util.EnvExpander;
import ilex.util.FileUtil;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;
import lrgs.common.DcpAddress;

public class ControlmMonitor
	extends DirectoryMonitorThread
	implements FilenameFilter
{
	public static String module = "ControlmMonitor";
	
	private SnotelDaemon parent = null;
	private File cfgD = null, rtD = null, hstD = null;
	private SnotelStatus snotelStatus = null;
	private ArrayList<File> cfgFiles = new ArrayList<File>();
	private ArrayList<File> rtFiles = new ArrayList<File>();
	private ArrayList<File> hstFiles = new ArrayList<File>();
	private SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
	private LinkedList<HistoryRetrieval> histQueue = new LinkedList<HistoryRetrieval>();

	public ControlmMonitor(SnotelDaemon parent)
	{
		this.parent = parent;
		
		// Dates in files are always interpreted as PST.
		sdf.setTimeZone(TimeZone.getTimeZone("GMT-08:00"));
		SnotelConfig conf = parent.getConfig();
		super.addDirectory(cfgD = new File(EnvExpander.expand(conf.controlmConfigDir)));
		super.addDirectory(rtD = new File(EnvExpander.expand(conf.controlmRealtimeDir)));
		super.addDirectory(hstD = new File(EnvExpander.expand(conf.controlmHistoryDir)));
		super.setFilenameFilter(this);
		Logger.instance().info(module + " cfgDir=" + cfgD.getPath() + ", rtDir="
			+ rtD.getPath() + ", hstDir=" + hstD.getPath());
		
		snotelStatus = parent.getStatus();
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
		SnotelConfig conf = parent.getConfig();
		
		// Process config directive files in order
		Collections.sort(cfgFiles, 
			new Comparator<File>()
			{
				@Override
				public int compare(File o1, File o2)
				{
					long lr = o1.lastModified() - o2.lastModified();
					return lr < 0 ? -1 : lr > 0 ? 1 : 0;
				}
			});
		for(File f : cfgFiles)
		{
			loadConfig(f);
			snotelStatus.configLMT = f.lastModified();
			if (conf.moveToArchive)
				moveToArchive(f);
		}
		cfgFiles.clear();
		
		// For realtime dir, only take the file with the latest LMT.
		File maxLMT = null;
		for(File f : rtFiles)
			if (maxLMT == null || f.lastModified() > maxLMT.lastModified())
				maxLMT = f;
		if (maxLMT != null)
		{
			processRealtime(maxLMT);
			snotelStatus.realtimeLMT = maxLMT.lastModified();
		}
		if (conf.moveToArchive)
			for(File f : rtFiles)
				moveToArchive(f);

		rtFiles.clear();
		
		// For history dir, process each file in order
		Collections.sort(hstFiles, 
			new Comparator<File>()
			{
				@Override
				public int compare(File o1, File o2)
				{
					long lr = o1.lastModified() - o2.lastModified();
					return lr < 0 ? -1 : lr > 0 ? 1 : 0;
				}
			});
		for(File f : hstFiles)
		{
			readHstFile(f);
			snotelStatus.historyLMT = f.lastModified();
			if (conf.moveToArchive)
				moveToArchive(f);
		}
		hstFiles.clear();
		
		manageHistoryRetrievals();
		
		parent.scanFinished();
	}
	
	private void moveToArchive(File f)
	{
		File ad = new File(f.getParent() + "/archive");
		if (!ad.isDirectory())
			ad.mkdirs();
		try
		{
			FileUtil.moveFile(f, new File(ad, f.getName()));
			Logger.instance().info(module + " moved '" + f.getName() + "' to " + ad.getPath());
		}
		catch (IOException ex)
		{
			Logger.instance().warning(module + " Cannot move '" + f.getPath()
				+ "' to archive dir '" + ad.getPath() + "': " + ex);
		}

	}

	private void processRealtime(File rtList)
	{
		File stationList = new File(EnvExpander.expand(SnotelDaemon.snotelRtStationListFile));
		try
		{
			FileUtil.copyFile(rtList, stationList);
			Logger.instance().info(module + " received new realtime list file '" + rtList.getPath()
				+ "' copied to specFile location '" + stationList.getPath() + "'");
		}
		catch (IOException ex)
		{
			Logger.instance().warning(module + " Cannot copy " + rtList.getPath()
				+ " to " + stationList.getPath() + ": " + ex);
		}
	}

	private void loadConfig(File cfgFile)
	{
		Logger.instance().info(module + " received config file '" + cfgFile.getPath());
		
		Properties props = new Properties();
		try
		{
			props.load(new FileInputStream(cfgFile));
		}
		catch (IOException ex)
		{
			Logger.instance().failure(module + " Cannot load config file '" 
				+ cfgFile.getPath() + "' " + ex
				+ " -- Check that file is readable and that it is a valid Java properties file.");
			return;
		}

		SnotelConfig conf = parent.getConfig();
		PropertiesUtil.loadFromProps(conf, props);
		parent.saveConfig();
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
		if (parent.historyInProgress())
		{
			Logger.instance().debug1(module + " history retrieval in progress. Waiting.");;
			return;
		}
		HistoryRetrieval hr = histQueue.pop();
		Logger.instance().info(module + ".manageHistRet: " + hr.getSpec() + " " 
			+ hr.getStart() + " " + hr.getEnd());
		parent.runHistory(hr);
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
						+ "' incorrect number of comma-separated fields. 6 required. -- Skipped.");
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

	@Override
	public boolean accept(File dir, String name)
	{
		File f = new File(dir, name);
		
		// Ignore subdirectories.
		if (f.isDirectory())
			return false;
		
		// This is from filename filter. Only accept files whose LMT is after
		// the last one we processed.
		
		long lmt = f.lastModified();
		if (cfgD.getPath().equals(dir.getPath()))
		{
			if (lmt > snotelStatus.configLMT)
			{
				cfgFiles.add(f);
				return true;
			}
		}
		else if (rtD.getPath().equals(dir.getPath()))
		{
			if (lmt > snotelStatus.realtimeLMT)
			{
				rtFiles.add(f);
				return true;
			}
		}
		else if (hstD.getPath().equals(dir.getPath()))
		{
			if (lmt > snotelStatus.historyLMT)
			{
				hstFiles.add(f);
				return true;
			}
		}
		return false;
	}
	
}
