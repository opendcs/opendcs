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

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

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
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
		log.info("runApp() Starting");
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
					appInfo = loadingAppDao.getComputationApp(getAppId());
				}
				catch (LockBusyException ex)
				{
					log.atWarn().setCause(ex).log("Shutting down: lock removed");
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
		log.info("shutting down.");
		cleanup();
		System.exit(0);
	}

	private void exec(String cmd)
	{
		this.cmdInProgress = cmd;
		int cmdTimeout = 20;
		log.debug("Executing '{}' and waiting up to {} seconds for completion.", cmdInProgress, cmdTimeout);
		cmdFinished = false;
		try
		{
			cmdExitStatus = -1;
			ProcWaiterThread.runBackground(cmdInProgress,
				"cmdAfterFile", this, cmdInProgress);
		}
		catch(IOException ex)
		{
			log.atWarn().setCause(ex).log("Cannot execute '{}'", cmdInProgress);
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
			log.debug("Command '{}' completed with exit status {}", cmdInProgress, cmdExitStatus);
		else
			log.warn("Command '{}' Did not complete!", cmdInProgress);
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
						log.atWarn()
						   .setCause(ex)
						   .log("{}({}) Unparsable date/time '{}' line skipped.",
						 		f.getName(), lnr.getLineNumber(), column[0]);
						continue;
					}
				}
				if (System.currentTimeMillis() - timeStamp.getTime() > maxAgeHours*3600000L)
				{
					log.debug("Discarding too-old sample at time {}", column[0]);
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
					log.warn(f.getName() + "{}({}) No spec matches label '{}' line skipped.",
							 f.getName(), lnr.getLineNumber(), column[1]);
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
			log.atWarn()
			   .setCause(ex)
			   .log("Error processing file '{}'", f.getPath());
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
			log.atWarn()
			   .setCause(ex)
			   .log("Error writing to daily archive '{}'", archFile.getPath());
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
					catch (NoSuchObjectException ex)
					{
						log.atWarn()
						   .setCause(ex)
						   .log("Invalid line {} '{}' -- ignored.", lnr.getLineNumber(), line);
					}
				}
				lnr.close();
				log.info("Parsed {} specifications from file '{}'", decodeSpecs.size(), specFile.getPath());
			}
			catch (IOException ex)
			{
				log.atWarn()
				   .setCause(ex)
				   .log("Cannot read spec file '{}'", specFile.getPath());
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
					log.atWarn()
					   .setCause(ex)
					   .log("Skipping bad value '{}'", vs);
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
			log.atWarn()
			   .setCause(ex)
			   .log("Error writing output line");
		}
	}

	private void init()
	{
		try (LoadingAppDAI loadingAppDao = theDb.makeLoadingAppDAO())
		{
			appInfo = loadingAppDao.getComputationApp(getAppId());


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
					log.atError()
					   .setCause(ex)
					   .log("Cannot create Event server -- no events available to external clients.");
				}
			}

			String hostname = "unknown";
			try
			{
				hostname = InetAddress.getLocalHost().getHostName();
			}
			catch(Exception ex)
			{
				log.atTrace().setCause(ex).log("Unable to retrieve localhost name.");
				hostname = "unknown";
			}

			myLock = loadingAppDao.obtainCompProcLock(appInfo, getPID(), hostname);
		}
		catch (LockBusyException ex)
		{
			log.atWarn().setCause(ex).log("Cannot run: lock busy.");
			shutdown = true;
			return;
		}
		catch (DbIoException ex)
		{
			log.atError().setCause(ex).log("Database I/O Error.");
			shutdown = true;
			return;
		}
		catch (NoSuchObjectException ex)
		{
			log.atError().setCause(ex).log("Cannot run: No such app name '{}'", appNameArg.getValue());
			shutdown = true;
			return;
		}

		String s = PropertiesUtil.getIgnoreCase(appInfo.getProperties(), "inputDir");
		if (s == null)
		{
			log.warn("Missing required 'inputDir' application property.");
			shutdown = true;
			return;
		}
		s = EnvExpander.expand(s);
		inputDir = new File(s);
		if (!inputDir.isDirectory())
		{
			log.warn("Specified input directory '" + s + "' is not a directory.");
			shutdown = true;
			return;
		}

		s = PropertiesUtil.getIgnoreCase(appInfo.getProperties(), "outputDir");
		if (s == null)
		{
			log.warn("Missing required 'outputDir' application property.");
			shutdown = true;
			return;
		}
		s = EnvExpander.expand(s);
		outputDir = new File(s);
		if (!outputDir.isDirectory())
		{
			log.warn("Specified output directory '" + s + "' is not a directory.");
			shutdown = true;
			return;
		}

		s = PropertiesUtil.getIgnoreCase(appInfo.getProperties(), "specFile");
		if (s == null)
		{
			log.warn("Missing required 'specFile' application property.");
			shutdown = true;
			return;
		}
		s = EnvExpander.expand(s);
		specFile = new File(s);
		if (!specFile.canRead())
		{
			log.warn("Specified spec file '" + s + "' is not readable.");
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
				log.warn("Specified daily archive directory '{}' is not a directory.", s);
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
				log.atWarn().setCause(ex).log("Bad 'maxAgeHours' property '{}' -- will use default of 24.", s);
				maxAgeHours = 24;
			}
		}

		s = PropertiesUtil.getIgnoreCase(appInfo.getProperties(), "cmdAfterFile");
		if (s != null && s.trim().length() > 0)
			cmdAfterFile = s;

	}

	private void cleanup()
	{
		try (LoadingAppDAI loadingAppDao = theDb.makeLoadingAppDAO())
		{
			loadingAppDao.releaseCompProcLock(myLock);
		}
		catch (DbIoException ex)
		{
			log.atWarn().setCause(ex).log("Error attempting to release lock.");
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
