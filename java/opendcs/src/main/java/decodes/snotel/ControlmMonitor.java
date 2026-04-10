/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
* 
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software 
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations 
* under the License.
*/
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
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TimeZone;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.util.DirectoryMonitorThread;
import ilex.util.EnvExpander;
import ilex.util.FileUtil;
import ilex.util.PropertiesUtil;
import lrgs.common.DcpAddress;

public class ControlmMonitor extends DirectoryMonitorThread	implements FilenameFilter
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
		sdf.setTimeZone(TimeZone.getTimeZone(parent.getConfig().outputTZ));
		SnotelConfig conf = parent.getConfig();
		super.addDirectory(cfgD = new File(EnvExpander.expand(conf.controlmConfigDir)));
		super.addDirectory(rtD = new File(EnvExpander.expand(conf.controlmRealtimeDir)));
		super.addDirectory(hstD = new File(EnvExpander.expand(conf.controlmHistoryDir)));
		super.setFilenameFilter(this);
		log.info("cfgDir={}, rtDir={}, hsttDir={}", cfgD.getPath(), rtD.getPath(), hstD.getPath());
		
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
		log.trace("finishedScan #cfgs={}, #rts={}, #hst={}", cfgFiles.size(), rtFiles.size(), hstFiles.size());
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
			log.info("moved '{}' to '{}'", f.getName(), ad.getPath());
		}
		catch (IOException ex)
		{
			log.atWarn().setCause(ex).log("Cannot move '{}' to archive dir '{}'", f.getPath(), ad.getPath());
		}

	}

	private void processRealtime(File rtList)
	{
		File stationList = new File(EnvExpander.expand(SnotelDaemon.snotelRtStationListFile));
		try
		{
			FileUtil.copyFile(rtList, stationList);
			log.info(" received new realtime list file '{}' copied to specFile location '{}'",
					 rtList.getPath(), stationList.getPath());
		}
		catch (IOException ex)
		{
			log.atWarn().setCause(ex).log("Cannot copy '{}' to '{}'", rtList.getPath(), stationList.getPath());
		}
	}

	private void loadConfig(File cfgFile)
	{
		log.info("received config file '{}'", cfgFile.getPath());
		
		Properties props = new Properties();
		
		try(FileInputStream fis = new FileInputStream(cfgFile);)
		{
			props.load(fis);
		}
		catch (IOException ex)
		{
			log.atError()
			   .setCause(ex)
			   .log("Cannot load config file '{}'  -- Check that file is readable " +
			   		"and that it is a valid Java properties file.",
					cfgFile.getPath());
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
			log.trace("histQueue empty");
			return;
		}
		if (parent.historyInProgress())
		{
			log.debug("history retrieval in progress. Waiting.");
			return;
		}
		HistoryRetrieval hr = histQueue.pop();
		log.info("manageHistRet: {} {} {} ", hr.getSpec(), hr.getStart(), hr.getEnd());
		parent.runHistory(hr);
	}

	/**
	 * Read the passed history file into one or more history requests and queue them.
	 * @param f the file.
	 */
	private void readHstFile(File f)
	{
		log.debug("reading history file '{}'", f.getPath());
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
					log.warn("{}:{} '{}'' incorrect number of comma-separated fields. 6 required. -- Skipped.",
							 f.getName(), lnr.getLineNumber(), line);
					continue;
				}
				int stationId = 0;
				try { stationId = Integer.parseInt(fields[0]); }
				catch(Exception ex)
				{
					log.atWarn()
					   .setCause(ex)
					   .log("{}:{} '{}'' bad site number in first field. Must be integer. -- Skipped.",
							f.getName(), lnr.getLineNumber(), line);
					continue;
				}
				String stationName = fields[1];
				
				DcpAddress dcpAddress = new DcpAddress(fields[2].toUpperCase());
				
				if (fields[3].length() == 0)
				{
					log.warn("{}:{} '{}' missing data format in last field, should be A or B. -- Skipped.",
							 f.getName(), lnr.getLineNumber(), line);
					continue;
				}
				char formatFlag = fields[3].charAt(0);
				
				SnotelPlatformSpec spec = new SnotelPlatformSpec(stationId, stationName, 
					dcpAddress, formatFlag);

				Date start = null;
				try { start = sdf.parse(fields[4]); }
				catch(ParseException ex)
				{
					log.atWarn()
					   .setCause(ex)
					   .log("{}:{} '{}' Second field must have start date as MM/DD/YYYY -- Skipped.",
							f.getName(), lnr.getLineNumber(), line);
					continue;
				}
				Date end = null;
				try 
				{
					end = sdf.parse(fields[5]);
					// 6/2/21 meeting, the end time should include the entire day specified,
					// so add 23:59:59 to it.
					Calendar cal = Calendar.getInstance();
					cal.setTimeZone(TimeZone.getTimeZone(parent.getConfig().outputTZ));
					cal.setTime(end);
					cal.set(Calendar.HOUR_OF_DAY, 23);
					cal.set(Calendar.MINUTE, 59);
					cal.set(Calendar.SECOND, 59);
					end = cal.getTime();
				}
				catch(ParseException ex)
				{
					log.atWarn()
					   .setCause(ex)
					   .log("{}:{} '{}' Third field must have end date as MM/DD/YYYY -- Skipped.",
							f.getName(), lnr.getLineNumber(), line);
					continue;
				}
				HistoryRetrieval hr = new HistoryRetrieval(spec, start, end);
				histQueue.add(hr);
				log.debug("New history retrieval: {}", hr);
			}
		}
		catch (IOException ex)
		{
			lnr = null;
			log.atWarn().setCause(ex).log("History File '{}' cannot be read.", f.getName());
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
			if (lmt > snotelStatus.configLMT || parent.getConfig().moveToArchive)
			{
				cfgFiles.add(f);
				return true;
			}
		}
		else if (rtD.getPath().equals(dir.getPath()))
		{
			if (lmt > snotelStatus.realtimeLMT || parent.getConfig().moveToArchive)
			{
				rtFiles.add(f);
				return true;
			}
		}
		else if (hstD.getPath().equals(dir.getPath()))
		{
			if (lmt > snotelStatus.historyLMT || parent.getConfig().moveToArchive)
			{
				hstFiles.add(f);
				return true;
			}
		}
		return false;
	}
	
}
