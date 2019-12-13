/*
 * This software was written by Cove Software, LLC. under contract to the 
 * U.S. Government. This software is property of the U.S. Government and 
 * may be used by permission only.
 * 
 * No warranty is provided or implied other than specific contractual terms.
 * 
 * Copyright 2014 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * All rights reserved.
 */
package decodes.tsdb.test;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import decodes.tsdb.ComputationApp;
import decodes.tsdb.DataCollection;
import decodes.tsdb.DbComputation;
import decodes.tsdb.DbIoException;
import decodes.tsdb.DeleteTs;
import decodes.tsdb.DisableComps;
import decodes.tsdb.ImportComp;
import decodes.tsdb.TsImport;
import decodes.tsdb.TsdbAppTemplate;
import decodes.tsdb.alarm.AlarmImport;
import decodes.util.CmdLineArgs;
import decodes.util.ExportTimeSeries;
import ilex.cmdline.BooleanToken;
import ilex.cmdline.TokenOptions;
import ilex.util.CmdLine;
import ilex.util.CmdLineProcessor;
import ilex.util.EnvExpander;
import ilex.util.FileUtil;
import ilex.util.Logger;
import lrgs.gui.DecodesInterface;
import opendcs.dai.ComputationDAI;
import opendcs.dai.LoadingAppDAI;

/**
 * General purpose database command-line utility
 * @author mmaloney
 *
 */
public class TestRunner extends TsdbAppTemplate
{
	private CmdLineProcessor cmdLineProc = new CmdLineProcessor();
	private Date since = null, until = null;
	private TimeZone tz = TimeZone.getTimeZone("UTC");
	private ArrayList<String> tsids = new ArrayList<String>();
	private SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy/HH:mm");
	private String appName = "compproc_regtest";
	private String outputName = "output";
	private PrintStream outs = System.out;
	private String presGrp = null;
	private BooleanToken interactiveMode = new BooleanToken("i", "Interactive Mode",
		"", TokenOptions.optSwitch, false);
	private int debugLevel = 3;
	private String testname = "test";
		
	private CmdLine tsidsCmd = 
		new CmdLine("tsids", "[list-of-tsids]")
		{
			public void execute(String[] tokens)
			{
				if (tokens.length < 2)
				{
					usage();
					return;
				}
				for(int idx=1; idx < tokens.length; idx++)
				{
					String s = expand(tokens[idx]);
					for(String x : s.split(" "))
						tsids.add(x);
				}
				Logger.instance().info("TSIDS command, tsid list is now: " + expand("$TSIDS"));
			}
		};
	
	private CmdLine clearTsidsCmd =
		new CmdLine("cleartsids", "(clears internal list of tsids)")
		{
			public void execute(String[] tokens)
			{
				tsids.clear();
				Logger.instance().info("CLEARTSIDS command");
			}
		
		};
		
	private CmdLine tzCmd =
		new CmdLine("tz", 
			"[tzid] - set timezone for since & until")
		{
			public void execute(String[] tokens)
			{
				tz = TimeZone.getTimeZone(tokens[1]);
				sdf.setTimeZone(tz);
				Logger.instance().info("TZ Timezone set to " + tz.getID());
			}
		};
		
	private CmdLine sinceCmd =
		new CmdLine("since", "[dd-MMM-yyy/HH:mm] - set since time for test")
		{
			public void execute(String[] tokens)
			{
				if (tokens.length < 2)
				{
					since = null;
					return;
				}
				try
				{
					since = sdf.parse(tokens[1]);
					Logger.instance().info("SINCE " + tokens[1] + " -- now set to " + sdf.format(since));
				}
				catch (ParseException e)
				{
					e.printStackTrace();
				}
			}
		};
	private CmdLine untilCmd =
		new CmdLine("until", 
			"[dd-MMM-yyy/HH:mm] - set until time for test")
		{
			public void execute(String[] tokens)
			{
				if (tokens.length < 2)
				{
					until = null;
					return;
				}

				try
				{
					until = sdf.parse(tokens[1]);
					Logger.instance().info("UNTIL " + tokens[1] + " -- now set to " + sdf.format(until));
				}
				catch (ParseException e)
				{
					e.printStackTrace();
				}
			}
		};

	private CmdLine procCmd =
		new CmdLine("proc", "[compproc-app-name]")
		{
			public void execute(String[] tokens)
			{
				setAppName(tokens[1]);
			}
		};
		

	private CmdLine sleepCmd =
		new CmdLine("sleep", "[number-of-seconds]")
		{
			public void execute(String[] tokens)
			{
				try
				{
					int n = Integer.parseInt(tokens[1]);
					Logger.instance().info("SLEEP " + n);
					Thread.sleep(n * 1000L);
					Logger.instance().info("          (sleep complete)");
				}
				catch(NumberFormatException ex)
				{
					ex.printStackTrace();
					return;
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		};
		
		
	private CmdLine logCmd =	
		new CmdLine("log", "message to be logged.")
		{
			public void execute(String[] tokens)
			{
				StringBuilder sb = new StringBuilder();
				for(int idx = 1; idx < tokens.length; idx++)
					sb.append(tokens[idx] + " ");
				Logger.instance().info(expand(sb.toString()));
			}
		};
		
		
	private CmdLine outputCmd =	
		new CmdLine("output", "[name-of-output-file]")
		{
			public void execute(String[] tokens)
			{
				try
				{
					outs = new PrintStream(new File(outputName = tokens[1]));
					Logger.instance().info("OUTPUT " + tokens[1]);
				}
				catch (FileNotFoundException e)
				{
					e.printStackTrace();
				}
			}
		};
		
	private CmdLine presgroupCmd =	
		new CmdLine("presgroup", "[name of presentation group]")
		{
			public void execute(String[] tokens)
			{
				presGrp = tokens[1];
			}
		};
		
	private CmdLine disablecompsCmd =	
		new CmdLine("disablecomps", " -- Disable computations for the named comp proc App")
		{
			public void execute(String[] tokens)
			{
				disableComps(tokens);
			}
		};
		
	private CmdLine deletetsCmd =	
		new CmdLine("deletets", "[list blank-separated of TSIDs] - Delete TS data between since & until")
		{
			public void execute(String[] tokens)
			{
				deleteTs(tokens);
			}
		};
		
	private CmdLine flushtriggersCmd =	
		new CmdLine("flushtriggers", "Delete any tasklist entries for this app ID.")
		{
			public void execute(String[] tokens)
			{
				flushtriggers(tokens);
			}
		};
	
	private CmdLine compImportCmd =
		new CmdLine("compimport", "Import computation records from XML.")
		{
			@Override
			public void execute(String[] tokens)
				throws IOException, EOFException
			{
				compImport(tokens);
			}
		
		};
		
	private CmdLine alarmImportCmd =
		new CmdLine("alarmimport", "Import computation records from XML.")
		{
			@Override
			public void execute(String[] tokens)
				throws IOException, EOFException
			{
				alarmImport(tokens);
			}
		};
	
		
	private CmdLine importTsCmd =
		new CmdLine("importts", "Import time series data.")
		{
			@Override
			public void execute(String[] tokens)
				throws IOException, EOFException
			{
				importTs(tokens);
			}		
		};
		
	private CmdLine outputTsCmd =
		new CmdLine("outputts", "Output time series data.")
		{
			@Override
			public void execute(String[] tokens)
				throws IOException, EOFException
			{
				outputTs(tokens);
			}		
		};

	private CmdLine compprocCmd =
		new CmdLine("compproc", "Run computation processor")
		{
			@Override
			public void execute(String[] tokens)
				throws IOException, EOFException
			{
				compproc(tokens);
			}		
		};
		
	private CmdLine echoCmd =
		new CmdLine("echo", "String containing $var names")
		{
			@Override
			public void execute(String[] tokens)
				throws IOException, EOFException
			{
				int idx = cmdLineProc.inputLine.toLowerCase().indexOf("echo");
				String line = 
					cmdLineProc.inputLine.length() < idx + 6 ? "" : cmdLineProc.inputLine.substring(idx+5);
				Logger.instance().info("ECHO " + line);
				outs.println(expand(line));
			}		
		};
		
	private CmdLine exitCmd =
		new CmdLine("exit", "(Terminates the test run)")
		{
			@Override
			public void execute(String[] tokens)
				throws IOException, EOFException
			{
				Logger.instance().info("EXIT");
				cmdLineProc.shutdown();
			}
		};

	private CmdLine debugLevelCmd =
		new CmdLine("debugLevel", "(0=no debug, 3=most verbose)")
		{
			@Override
			public void execute(String[] tokens)
				throws IOException, EOFException
			{
				if (tokens.length != 2)
				{
					error("Invalid DEBUGLEVEL command -- requires a single integer art.", null);
					return;
				}
				try
				{
					int lev = Integer.parseInt(tokens[1]);
					if (lev < 0 || lev > 3)
						lev = 3;
					setDebugLevel(lev);
				}
				catch(NumberFormatException ex)
				{
					error("Invalid debug level '" + tokens[1] + "' -- must be 0, 1, 2, or 3", null);
					return;
				}
			}
		};
		
	private void setDebugLevel(int lev)
	{
		this.debugLevel = lev;
		Logger.instance().setMinLogPriority(
			lev == 0 ? Logger.E_INFORMATION :
			lev == 1 ? Logger.E_DEBUG1 :
			lev == 2 ? Logger.E_DEBUG2 : Logger.E_DEBUG3);
		Logger.instance().info("DEBUGLEVEL set to " + lev);
	}
			
	private String expand(String ins)
	{
		Properties props = new Properties(cmdLineProc.getAssignments());
		if (tsids.size() > 0)
		{
			StringBuilder sb = new StringBuilder();
			for(String tsid : tsids)
				sb.append(tsid + " ");
			String s = sb.toString().trim();
			props.put("TSIDS", s);
			props.put("tsids", s);
		}
		if (since != null)
			props.put("SINCE", sdf.format(since));
		if (until != null)
			props.put("UNTIL", sdf.format(until));
		props.put("TZ", tz.getID());
		props.put("PROC", appName);
		props.put("testname", testname);
		return EnvExpander.expand(ins, props);
	}
	
	protected void setAppName(String appName)
	{
		this.appName = appName;
		
		LoadingAppDAI appDao = theDb.makeLoadingAppDAO();
		try
		{
			setAppId(appDao.lookupAppId(appName));
			Logger.instance().info("PROC set appName to '" + appName + "' appId=" + getAppId());
		}
		catch (Exception ex)
		{
			Logger.instance().failure("Error looking up PROC name '" + appName + "': " + ex);
		}
	}

	public TestRunner()
	{
		super("util.log");
		DecodesInterface.silent = true;
	}

	@Override
	protected void runApp() throws Exception
	{
		setDebugLevel(3);
		
		if (interactiveMode.getValue())
			cmdLineProc.prompt = "cmd: ";
		else // reading from a file -- no prompt
		{
			cmdLineProc.prompt = null;
			File title = new File("title");
			if (title.canRead())
				testname = FileUtil.getFileContents(title).trim();
		}
		cmdLineProc.addCmd(tsidsCmd);
		cmdLineProc.addCmd(tzCmd);
		cmdLineProc.addCmd(sinceCmd);
		cmdLineProc.addCmd(untilCmd);
		cmdLineProc.addCmd(procCmd);
		cmdLineProc.addCmd(sleepCmd);
		cmdLineProc.addCmd(logCmd);
		cmdLineProc.addCmd(outputCmd);
		cmdLineProc.addCmd(presgroupCmd);
		cmdLineProc.addCmd(disablecompsCmd);
		cmdLineProc.addCmd(deletetsCmd);
		cmdLineProc.addCmd(flushtriggersCmd);
		cmdLineProc.addCmd(compImportCmd);
		cmdLineProc.addCmd(importTsCmd);
		cmdLineProc.addCmd(outputTsCmd);
		cmdLineProc.addCmd(echoCmd);
		cmdLineProc.addCmd(exitCmd);
		cmdLineProc.addCmd(debugLevelCmd);
		cmdLineProc.addCmd(compprocCmd);
		cmdLineProc.addCmd(clearTsidsCmd);
		cmdLineProc.addCmd(alarmImportCmd);
				
		cmdLineProc.addHelpAndQuitCommands();
		
		cmdLineProc.processInput();
	}

	
	/**
	 * @param args
	 */
	public static void main(String[] args)
		throws Exception
	{
		TestRunner tr = new TestRunner();
		tr.execute(args);
	}
	
	@Override
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		appNameArg.setDefaultValue("compproc_regtest");
		cmdLineArgs.addToken(interactiveMode);
	}

	protected void disableComps(String[] tokens)
	{
		Logger.instance().info("DISABLECOMPS for app ID = " + getAppId());
		
		try
		{
			String q = "update cp_computation set enabled = 'N' "
				+ "where loading_application_id = " + getAppId();
			theDb.doModify(q);
	
			// And just to be thorough ...
			q = "delete from cp_comp_depends where computation_id in ("
				+ "select computation_id from cp_computation where loading_application_id = "
				+ getAppId() + ")";
			theDb.doModify(q);
	
			// Now delete any stray tasklist entries.
			q = "delete from cp_comp_tasklist where loading_application_id = " + getAppId();
			theDb.doModify(q);
		}
		catch(Exception ex)
		{
			error("Error in DISABLECOMPS: " + ex, ex);
		}
	}
	
	protected void deleteTs(String[] tokens)
	{
		DeleteTs subApp = 
			new DeleteTs()
			{
				@Override
				public void createDatabase() {}
				@Override
				public void tryConnect() {}
			};
			
		subApp.getCmdLineArgs().setNoInit(true);
		subApp.setAppId(getAppId());
		subApp.setNoExitAfterRunApp(true);
		try
		{
			tokens = tokens2args(tokens, false, false, false, true);
			String[] newtoks = new String[tokens.length + tsids.size()];
			for(int idx = 0; idx < tokens.length; idx++)
				newtoks[idx] = tokens[idx];
			for(int idx = 0; idx < tsids.size(); idx++)
				newtoks[idx + tokens.length] = tsids.get(idx);
			tokens = newtoks;
			Logger.instance().info("DELETETS executing with args " + toks2str(tokens));
			subApp.execute(tokens);
		}
		catch (Exception e)
		{
			System.err.println("Error executing cmd '" + tokens[0] + "': " + e);
			e.printStackTrace(System.err);
		}
	}
	
	protected void flushtriggers(String[] tokens)
	{
		try
		{
			Logger.instance().info("FLUSHTRIGGERS");
			DataCollection dc = null;
			while(!(dc = theDb.getNewData(getAppId())).isEmpty())
				theDb.releaseNewData(dc);
		}
		catch(DbIoException ex)
		{
			error("Error in flushtriggers: " + ex, ex);
		}
	}

	protected void compImport(String[] tokens) 
	{
		ImportComp subApp = 
			new ImportComp()
			{
				@Override
				public void createDatabase() {}
				@Override
				public void tryConnect() {}
			};
					
		subApp.getCmdLineArgs().setNoInit(true);
		subApp.setAppId(getAppId());
		subApp.setNoExitAfterRunApp(true);
		try
		{
			tokens = tokens2args(tokens, false, false, false, false);
			Logger.instance().info("COMPIMPORT");
			subApp.execute(tokens);
		}
		catch (Exception e)
		{
			System.err.println("Error executing cmd '" + tokens[0] + "': " + e);
			e.printStackTrace(System.err);
		}
	}
	
	protected void alarmImport(String[] tokens)
	{
		AlarmImport subApp = 
			new AlarmImport()
			{
				@Override
				public void createDatabase() {}
				@Override
				public void tryConnect() {}
			};
						
		subApp.getCmdLineArgs().setNoInit(true);
		subApp.setAppId(getAppId());
		subApp.setNoExitAfterRunApp(true);
		try
		{
			tokens = tokens2args(tokens, false, false, false, false);
			Logger.instance().info("ALARMIMPORT");
			subApp.execute(tokens);
		}
		catch (Exception e)
		{
			System.err.println("Error executing cmd '" + tokens[0] + "': " + e);
			e.printStackTrace(System.err);
		}
	}

	protected void importTs(String[] tokens)
	{
		TsImport subApp = 
			new TsImport()
			{
				@Override
				public void createDatabase() {}		
				@Override
				public void tryConnect() {}
			};
						
		subApp.getCmdLineArgs().setNoInit(true);
		subApp.setNoExitAfterRunApp(true);
		String args[] = tokens2args(tokens, false, false, false, false);
		try
		{
			subApp.execute(args);
		}
		catch (Exception e)
		{
			System.err.println("Error executing cmd '" + tokens[0] + "': " + e);
			e.printStackTrace(System.err);
		}
	}

	protected void outputTs(String[] tokens)
	{
		ExportTimeSeries subApp = new ExportTimeSeries()
		{
			@Override
			public void createDatabase()
			{
			}
			@Override
			public void tryConnect() {}
		};

		subApp.getCmdLineArgs().setNoInit(true);
		subApp.setNoExitAfterRunApp(true);
		String args[] = tokens2args(tokens, false, false, true, true);
		ArrayList<String> sa = new ArrayList<String>();
		for(String a : args) sa.add(a);
		for(String tsid : tsids)
			sa.add(tsid);
		args = new String[sa.size()];
		for(int idx = 0; idx < args.length; idx++)
			args[idx] = sa.get(idx);
		
		try
		{
			subApp.setOutputStream(outs);
			subApp.execute(args);
		}
		catch (Exception e)
		{
			error("Error executing cmd '" + tokens[0] + "': " + e, e);
		}
	}

	protected void compproc(String[] tokens)
	{
		ComputationApp subApp = new ComputationApp()
		{
			@Override
			public void createDatabase() {}
			@Override
			public void tryConnect() {}

		};

		subApp.setAppId(this.getAppId());
		subApp.getCmdLineArgs().setNoInit(true);
		subApp.setNoExitAfterRunApp(true);
		String args[] = tokens2args(tokens, true, true, false, false);
		
		try
		{
			subApp.execute(args);
		}
		catch (Exception e)
		{
			error("Error executing cmd '" + tokens[0] + "': " + e, e);
		}
	}
	
	private String[] tokens2args(String[] tokens, boolean addAppName, boolean addTestMode,
		boolean addPresGrp, boolean addTimes)
	{
		boolean testModePresent = false;
		boolean appNamePresent = false;
		boolean debugLevPresent = false;
		boolean logFilePresent = false;
		boolean presGrpPresent = false;
		boolean sincePresent = false;
		boolean untilPresent = false;
		boolean tzPresent = false;
		
		ArrayList<String> args = new ArrayList<String>();
		
		for(String tok : tokens)
		{
			args.add(tok);
			if (tok.startsWith("-l"))
				logFilePresent = true;
			if (tok.startsWith("-a"))
				appNamePresent = true;
			if (tok.startsWith("-d"))
				debugLevPresent = true;
			if (addTestMode && tok.startsWith("-T"))
				testModePresent = true;
			if (addPresGrp && tok.startsWith("-G"))
				presGrpPresent = true;
			if (addTimes && tok.startsWith("-S"))
				sincePresent = true;
			if (addTimes && tok.startsWith("-U"))
				untilPresent = true;
			if (addTimes && tok.startsWith("-Z"))
				tzPresent = true;
		}
		
		// 1st token is program name. Remove it.
		args.remove(0);

		if (!logFilePresent)
		{
			args.add(0, "-l");
			args.add(1, cmdLineArgs.getLogFile());
		}
		if (!debugLevPresent)
		{
			args.add(0, "-d");
			args.add(1, "" + debugLevel);

		}
		if (addAppName && !appNamePresent)
		{
			args.add(0, "-a");
			args.add(1, "" + appName);
		}
		if (addTestMode && !testModePresent)
			args.add(0, "-T");
		if (addPresGrp && !presGrpPresent && presGrp != null && presGrp.length() > 0)
		{
			args.add(0, "-G");
			args.add(1, presGrp);
		}
		if (addTimes && !tzPresent)
		{
			args.add(0, "-Z");
			args.add(1, tz.getID());
		}
		if (addTimes && !untilPresent && until != null)
		{
			args.add(0, "-U");
			args.add(1, sdf.format(until));
		}
		if (addTimes && !sincePresent && since != null)
		{
			args.add(0, "-S");
			args.add(1, sdf.format(since));
		}

		String ret[] = new String[args.size()];
		for(int idx = 0; idx < ret.length; idx++)
			ret[idx] = args.get(idx);
		
		return ret;
	}
	
	private String toks2str(String[] tokens)
	{
		StringBuilder sb = new StringBuilder();
		for(String tok : tokens)
			sb.append(tok + " ");
		
		return sb.toString().trim();
	}

	private void error(String msg, Throwable ex)
	{
		Logger.instance().warning(msg);
		System.err.println(msg);
		if (ex != null)
		{
			ex.printStackTrace(System.err);
			if (Logger.instance().getLogOutput() != null)
				ex.printStackTrace(Logger.instance().getLogOutput());
		}
	}

}
