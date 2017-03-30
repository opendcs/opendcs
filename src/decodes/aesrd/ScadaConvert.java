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
import ilex.util.TextUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

import lrgs.gui.DecodesInterface;
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

public class ScadaConvert 
	extends TsdbAppTemplate
	implements PropertiesOwner, ProcWaiterCallback
{
	private static final String module = "ScadaConvert";
	private CompAppInfo appInfo = null;
	private TsdbCompLock myLock = null;
	private boolean shutdown = false;
	private File inputDir = null;
	private File outputDir = null;
	private File specFile = null;
	private String fileExt = ".dat";
	private File dailyArchiveDir = null;
	private long lastSpecLoad = 0L;
	private ArrayList<ScadaDecodeSpec> decodeSpecs = new ArrayList<ScadaDecodeSpec>();
	private SimpleDateFormat arcNameSdf = new SimpleDateFormat("yyMMdd");
	private SimpleDateFormat scadaFmt1 = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
	private SimpleDateFormat scadaFmt2 = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
	private SimpleDateFormat outputFileFmt = new SimpleDateFormat("yyyyMMddHHmmss");
	private SimpleDateFormat albertaLdrFmt = new SimpleDateFormat("yyyyMMdd HHmm");
	private PrintWriter outputFile = null;
	private String outputFilePath = null;
	private int numFilesProcessed = 0;
	private int numFileErrors = 0;
	private int maxAgeHours = 24;
	private NumberFormat numberFormat = NumberFormat.getNumberInstance();
	private String cmdAfterFile = null;
	private boolean cmdFinished = false;
	private String cmdInProgress = null;
	private int cmdExitStatus = -1;
	private CompEventSvr compEventSvr = null;

	private PropertySpec propSpecs[] = 
	{
		new PropertySpec("inputDir", PropertySpec.DIRECTORY, 
			"Name of directory to monitor for incoming raw SCADA files"),
		new PropertySpec("fileExt", PropertySpec.STRING, 
			"Only process input files with this extension (default = .dat)"),
		new PropertySpec("outputDir", PropertySpec.DIRECTORY, 
			"Directory in which to store output Alberta Loader files"),
		new PropertySpec("specFile", PropertySpec.FILENAME, 
			"Pathname of 'scdalst.in' file"),
		new PropertySpec("dailyArchiveDir", PropertySpec.DIRECTORY, 
			"Directory for daily archive files (may be null)"),
		new PropertySpec("MaxAgeHours", PropertySpec.INT, 
			"(default=24) discard data older than this"),
		new PropertySpec("DoneDir", PropertySpec.DIRECTORY, 
			"Put raw files here after processing."),
		new PropertySpec("CmdAfterFile", PropertySpec.STRING,
			"Optional command to execute after finishing file. The command will be passed the "
			+ "filename as an argument.")
	};

	public ScadaConvert()
	{
		super("scadaconv.log");
		arcNameSdf.setTimeZone(TimeZone.getTimeZone("MST"));
		scadaFmt1.setTimeZone(TimeZone.getTimeZone("MST"));
		scadaFmt2.setTimeZone(TimeZone.getTimeZone("MST"));
		outputFileFmt.setTimeZone(TimeZone.getTimeZone("MST"));
		albertaLdrFmt.setTimeZone(TimeZone.getTimeZone("MST"));
		numberFormat.setGroupingUsed(false);
		numberFormat.setMaximumFractionDigits(3);
		numberFormat.setMinimumFractionDigits(3);
	}

	@Override
	protected void runApp() throws Exception
	{
		info("runApp() Starting");
		init();
		long lastLockCheck = System.currentTimeMillis();
		// Set lastCheck to cause first check 5 seconds after startup.
		setAppStatus("running");
		while(!shutdown)
		{
			File inputFiles[] = inputDir.listFiles();
			if (inputFiles != null && inputFiles.length > 0)
			{
				checkSpecs();
				for(File f : inputFiles)
					if (f.getName().endsWith(fileExt))
						processFile(f);
				if (outputFile != null)
				{
					outputFile.close();
					if (cmdAfterFile != null)
					{
						StringBuilder sb = new StringBuilder(EnvExpander.expand(cmdAfterFile));
						sb.append(" ");
						sb.append(outputFilePath);
						exec(sb.toString());
					}
					outputFile = null;
				}
			}
			if (System.currentTimeMillis() - lastLockCheck > 10000L)
			{
				LoadingAppDAI loadingAppDao = theDb.makeLoadingAppDAO();

				try
				{
					loadingAppDao.checkCompProcLock(myLock);
					appInfo = loadingAppDao.getComputationApp(appId);
				}
				catch (LockBusyException ex)
				{
					warning("Shutting down: lock removed: " + ex);
					shutdown = true;
				}
				finally
				{
					loadingAppDao.close();
				}
			}
			
			try { Thread.sleep(1000L); }
			catch(InterruptedException ex) {}
		}
		info("shutting down.");
		cleanup();
		System.exit(0);
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

	private void processFile(File f)
	{
		if (dailyArchiveDir != null)
			archive(f);
		
		try
		{
			LineNumberReader lnr = new LineNumberReader(new FileReader(f));
			String line;
			while((line = lnr.readLine()) != null)
			{
				String sline = stripQuotes(line).trim();
				String column[] = sline.split(",");
				if (column == null || column.length < 3)
					continue;
				Date timeStamp = null;
				try { timeStamp = scadaFmt1.parse(column[0]); }
				catch (ParseException ex)
				{
					try { timeStamp = scadaFmt2.parse(column[0]); }
					catch (ParseException ex2)
					{
						warning(f.getName() + "(" + lnr.getLineNumber() 
							+ ") Unparsable date/time '" + column[0] + "' line skipped.");
						continue;
					}
				}
				if (System.currentTimeMillis() - timeStamp.getTime() > maxAgeHours*3600000L)
				{
					debug("Discarding too-old sample at time " + column[0]);
					continue;
				}
				// Truncate ID to max of 21 chars.
				if (column[1].length() > 21)
					column[1] = column[1].substring(0, 21);
				ScadaDecodeSpec spec = null;
				for(ScadaDecodeSpec tspec : decodeSpecs)
					if (tspec.label.equalsIgnoreCase(column[1]))
					{
						spec = tspec;
						break;
					}
				if (spec == null)
				{
					warning(f.getName() + "(" + lnr.getLineNumber() 
						+ ") No spec matches label '" + column[1] + "' line skipped.");
					continue;
				}
				output(column, spec, timeStamp);
			}
			lnr.close();
			String doneDir = appInfo.getProperty("DoneDir");
			if (doneDir != null)
				FileUtil.copyFile(f, 
					new File(EnvExpander.expand(doneDir), f.getName()));
			f.delete();
		}
		catch (IOException ex)
		{
			warning("Error processing file '" + f.getPath() + "': " + ex);
			numFileErrors++;
		}
		setAppStatus("files=" + (++numFilesProcessed) + ", errors=" + numFileErrors);
	}
	
	private String stripQuotes(String line)
	{
		StringBuilder sb = new StringBuilder();
		for(int idx = 0; idx < line.length(); idx++)
		{
			char c = line.charAt(idx);
			if (c != '"')
				sb.append(c);
		}
		return sb.toString();
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

	private void checkSpecs()
	{
		if (specFile.lastModified() > lastSpecLoad)
		{
			lastSpecLoad = System.currentTimeMillis();
			decodeSpecs.clear();
			try
			{
				LineNumberReader lnr = new LineNumberReader(
					new FileReader(specFile));
				String line;
				while((line = lnr.readLine()) != null)
				{
					if (line.startsWith("#") || line.startsWith("END "))
						continue;
					try
					{
						decodeSpecs.add(new ScadaDecodeSpec(line));
					}
					catch (NoSuchObjectException e)
					{
						warning("Invalid line " + lnr.getLineNumber() + " '" + line + "' -- ignored.");
					}
				}
				lnr.close();
				info("Parsed " + decodeSpecs.size() + " specifications from file '" 
					+ specFile.getPath() + "'");
			}
			catch (IOException ex)
			{
				warning("Cannot read spec file '" + specFile.getPath() + "': " + ex);
			}
		}
	}
	
	private void output(String column[], ScadaDecodeSpec spec, Date timestamp)
	{
		try
		{
			if (outputFile == null)
			{
				File of = new File(outputDir, 
					"scda-" + outputFileFmt.format(new Date()));
				outputFile = new PrintWriter(of);
				outputFilePath = of.getPath();
			}
			for(int idx=0; idx < spec.sensorCodes.length; idx++)
			{
				if (spec.sensorCodes[idx] == null)
					continue;
				if (column.length < idx+2)
					break;
				
				String vs = column[idx+2];
				if (vs == null || vs.trim().length() == 0)
					continue;
				try
				{
					double d = Double.parseDouble(vs.trim());
					vs = numberFormat.format(d);
				}
				catch(Exception ex)
				{
					warning("Skipping bad value '" + vs + "'");
					continue;
				}
				
				// NNNNNNNN YYYYMMDD HHMMVVVVVVVVSSSSR
				outputFile.println(
					TextUtil.setLengthLeftJustify(spec.newleafSite, 8)
					+ " " + albertaLdrFmt.format(timestamp)
					+ TextUtil.setLengthLeftJustify(vs, 8)
					+ TextUtil.setLengthLeftJustify(spec.sensorCodes[idx], 4)
					+ "R");
			}
		}
		catch(IOException ex)
		{
			warning("Error writing output line: " + ex);
		}
	}

	private void init()
	{
		LoadingAppDAI loadingAppDao = theDb.makeLoadingAppDAO();
		try
		{
			appInfo = loadingAppDao.getComputationApp(appId);
			
			
			// If this process can be monitored, start an Event Server.
			if (TextUtil.str2boolean(appInfo.getProperty("monitor")) && compEventSvr == null)
			{
				try 
				{
					compEventSvr = new CompEventSvr(determineEventPort(appInfo));
					compEventSvr.startup();
				}
				catch(IOException ex)
				{
					failure("Cannot create Event server: " + ex
						+ " -- no events available to external clients.");
				}
			}

			String hostname = "unknown";
			try { hostname = InetAddress.getLocalHost().getHostName(); }
			catch(Exception e) { hostname = "unknown"; }

			myLock = loadingAppDao.obtainCompProcLock(appInfo, getPID(), hostname); 
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
		
		String s = PropertiesUtil.getIgnoreCase(appInfo.getProperties(), "inputDir");
		if (s == null)
		{
			warning("Missing required 'inputDir' application property.");
			shutdown = true;
			return;
		}
		s = EnvExpander.expand(s);
		inputDir = new File(s);
		if (!inputDir.isDirectory())
		{
			warning("Specified input directory '" + s + "' is not a directory.");
			shutdown = true;
			return;
		}

		s = PropertiesUtil.getIgnoreCase(appInfo.getProperties(), "outputDir");
		if (s == null)
		{
			warning("Missing required 'outputDir' application property.");
			shutdown = true;
			return;
		}
		s = EnvExpander.expand(s);
		outputDir = new File(s);
		if (!outputDir.isDirectory())
		{
			warning("Specified output directory '" + s + "' is not a directory.");
			shutdown = true;
			return;
		}
		
		s = PropertiesUtil.getIgnoreCase(appInfo.getProperties(), "specFile");
		if (s == null)
		{
			warning("Missing required 'specFile' application property.");
			shutdown = true;
			return;
		}
		s = EnvExpander.expand(s);
		specFile = new File(s);
		if (!specFile.canRead())
		{
			warning("Specified spec file '" + s + "' is not readable.");
			shutdown = true;
			return;
		}

		s = PropertiesUtil.getIgnoreCase(appInfo.getProperties(), "fileExt");
		if (s != null)
			fileExt = s;
		
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
		
		s = PropertiesUtil.getIgnoreCase(appInfo.getProperties(), "maxAgeHours");
		if (s != null)
		{
			try { maxAgeHours = Integer.parseInt(s); }
			catch(NumberFormatException ex)
			{
				warning("Bad 'maxAgeHours' property '" + s + "' -- will use default of 24.");
				maxAgeHours = 24;
			}
		}
		
		s = PropertiesUtil.getIgnoreCase(appInfo.getProperties(), "cmdAfterFile");
		if (s != null && s.trim().length() > 0)
			cmdAfterFile = s;

	}

	private void cleanup()
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

	@Override
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		setSilent(true);
		appNameArg.setDefaultValue("ScadaConvert");
	}
	
	/**
	 * The main method.
	 * @param args command line arguments.
	 */
	public static void main( String[] args )
		throws Exception
	{
		TsdbAppTemplate theApp = new ScadaConvert();
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
