/*
 * $Id$
 * 
 * $Log$
 * Revision 1.5  2017/07/06 20:32:59  mmaloney
 * dev
 *
 * Revision 1.4  2017/07/06 20:31:45  mmaloney
 * dev
 *
 * Revision 1.3  2017/07/06 20:28:42  mmaloney
 * dev
 *
 * Revision 1.2  2017/07/06 20:23:59  mmaloney
 * Changed -c to -C for computation IDs.
 *
 * Revision 1.1  2017/07/06 19:06:22  mmaloney
 * Created.
 *
 * 
 * This software was written by Cove Software, LLC ("COVE") under contract 
 * to the United States Government. 
 * 
 * No warranty is provided or implied other than specific contractual terms
 * between COVE and the U.S. Government
 * 
 * Copyright 2017 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * All rights reserved.
 */
package decodes.tsdb.comprungui;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Properties;
import java.util.TimeZone;

import opendcs.dai.CompDependsDAI;
import opendcs.dai.ComputationDAI;
import opendcs.dai.TimeSeriesDAI;
import opendcs.dai.TsGroupDAI;
import lrgs.gui.DecodesInterface;
import ilex.cmdline.BooleanToken;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.util.TextUtil;
import ilex.var.TimedVariable;
import ilex.var.Variable;
import decodes.consumer.DataConsumerException;
import decodes.consumer.OutputFormatter;
import decodes.consumer.OutputFormatterException;
import decodes.consumer.PipeConsumer;
import decodes.datasource.GoesPMParser;
import decodes.datasource.RawMessage;
import decodes.datasource.UnknownPlatformException;
import decodes.db.DataPresentation;
import decodes.db.Database;
import decodes.db.InvalidDatabaseException;
import decodes.db.PresentationGroup;
import decodes.db.TransportMedium;
import decodes.decoder.DecodedMessage;
import decodes.decoder.Sensor;
import decodes.decoder.TimeSeries;
import decodes.sql.DbKey;
import decodes.tsdb.*;
import decodes.util.CmdLineArgs;
import decodes.util.TSUtil;

/**
 * This class is the "compexec" command line utility for running computations
 * without the need for triggers.
 */
public class CompExec extends TsdbAppTemplate
{
	private String dateSpec = "yyyy/MM/dd-HH:mm:ss";
	private StringToken tsidToken = new StringToken("T", "TSID(s)", "", 
		TokenOptions.optSwitch|TokenOptions.optMultiple, null);
	private StringToken groupIdToken = new StringToken("G", "Group ID", "", 
		TokenOptions.optSwitch|TokenOptions.optMultiple, null);
	private StringToken compIdToken = new StringToken("C", "Computation ID(s)", "",
		TokenOptions.optSwitch|TokenOptions.optMultiple, null);
	private StringToken ctrlFileToken = new StringToken("f", "Control File", "", 
		TokenOptions.optSwitch, null);
	private StringToken sinceToken = new StringToken("S", "Since Time in " + dateSpec, "", 
		TokenOptions.optSwitch, null);
	private StringToken untilToken = new StringToken("U", "Until Time in " + dateSpec, "", 
		TokenOptions.optSwitch, null);
	private StringToken outputFmtToken = new StringToken("o", "Output Format", "",
		TokenOptions.optSwitch, null);
	private StringToken presGrpToken = new StringToken("R", "Presentation Group", "",
		TokenOptions.optSwitch, null);
	private BooleanToken quietToken = new BooleanToken("q", "Quiet - no prompts or stats.", "",
		TokenOptions.optSwitch, false);
	private StringToken tzArg = new StringToken("Z", "Time Zone", "", 
		TokenOptions.optSwitch, "UTC");

	private HashSet<TimeSeriesIdentifier> tsids = new HashSet<TimeSeriesIdentifier>();
//	private ArrayList<DbComputation> specifiedComps = new ArrayList<DbComputation>();
	private HashSet<DbKey> specifiedCompIDs = new HashSet<DbKey>();
	private Date since = null, until = null;
	private SimpleDateFormat sdf = new SimpleDateFormat(dateSpec);
	private TimeSeriesDAI timeSeriesDAO = null;
	private ComputationDAI computationDAO = null;
	private TsGroupDAI tsGroupDAO = null;
	private PresentationGroup presGrp = null;
	private OutputFormatter outputFormatter = null;
	private CompDependsDAI compDependsDAO = null;
//	private CpCompDependsUpdater compDependsUpdater = null;

	/** Constructor */
	public CompExec()
	{
		super(null);
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	
	/** Runs the GUI */
	public void runApp()
		throws Exception
	{
		timeSeriesDAO = theDb.makeTimeSeriesDAO();
		computationDAO = theDb.makeComputationDAO();
		tsGroupDAO = theDb.makeTsGroupDAO();
		compDependsDAO = theDb.makeCompDependsDAO();
		
		// If ctrl file provided it has everything.
		if (ctrlFileToken.getValue() != null)
			readControlFile(ctrlFileToken.getValue());
		for(int idx = 0; idx < tsidToken.NumberOfValues(); idx++)
			if (tsidToken.getValue(idx) != null)
				loadTsid(tsidToken.getValue(idx));

		for(int idx = 0; idx < groupIdToken.NumberOfValues(); idx++)
			if (groupIdToken.getValue(idx) != null)
				loadGroup(groupIdToken.getValue(idx));

		for(int idx = 0; idx < compIdToken.NumberOfValues(); idx++)
			if (compIdToken.getValue(idx) != null)
				loadComp(compIdToken.getValue(idx));
		
		if (sinceToken.getValue() != null)
		{
			try { since = sdf.parse(sinceToken.getValue()); }
			catch(ParseException ex)
			{
				System.err.println("Invalid Since Argument '" + sinceToken.getValue()
					+ "' -- format must be '" + dateSpec + " UTC");
				System.exit(1);
			}
		}
		if (untilToken.getValue() != null)
		{
			try { until = sdf.parse(untilToken.getValue()); }
			catch(ParseException ex)
			{
				System.err.println("Invalid Since Argument '" + untilToken.getValue()
					+ "' -- format must be '" + dateSpec + " UTC");
				System.exit(1);
			}
		}
		
		// if outputFmt and presGrp are provided, initialize them.
		String pgName = presGrpToken.getValue();
		if (pgName != null && pgName.length() > 0)
		{
			presGrp = Database.getDb().presentationGroupList.find(pgName);
			try
			{
				if (presGrp != null)
					presGrp.prepareForExec();
			}
			catch (InvalidDatabaseException ex)
			{
				System.err.println("Cannot initialize presentation group '" 
					+ pgName + ": " + ex + " -- will not use.");
				presGrp = null;
			}
		}

		if (outputFmtToken.getValue() != null)
		{
			try
			{
				outputFormatter = OutputFormatter.makeOutputFormatter(
					outputFmtToken.getValue(), TimeZone.getTimeZone(tzArg.getValue()),
					presGrp, cmdLineArgs.getCmdLineProps(), null);
			}
			catch (OutputFormatterException ex)
			{
				System.err.println("Cannot make output formatter: " + ex);
				System.exit(1);
			}
		}
		
		info("" + specifiedCompIDs.size() + " comp IDs and "+ tsids.size()
			+ " TSIDs supplied -- evaluating.");
		
		// Now all arguments are read either from cmd line or control file.
		if (tsids.isEmpty() && specifiedCompIDs.isEmpty())
		{
			System.out.println("Nothing to do -- No time series or computations specified.");
			System.exit(1);
		}
		else if (specifiedCompIDs.isEmpty())
		{
			info("Evaluating comps that need to run for specified TSIDs.");
			// Determine the possible enabled computations for the specified TSIDs
			// use CP_COMP_DEPENDS.
			for(DbKey compId : compDependsDAO.getCompIdsFor(tsids, DbKey.NullKey))
				specifiedCompIDs.add(compId);
		}
		else if (tsids.isEmpty())
		{
			// Comps but no TSIDs are specified. Find all possible inputs for the named comps.
			info("Comps but no TSIDs supplied, will use all possible triggers.");
			for(DbKey compID : specifiedCompIDs)
				tsids.addAll(compDependsDAO.getTriggersFor(compID));
			info("After evaluating triggers, we have " + tsids.size() + " TSIDs.");
		}
		else
		{
			// both comps and TSIDs are specified. Nothing to do.
		}
		
		tsGroupDAO.close();
		
		info("After eval, there are " + tsids.size() + " TSIDs and " + specifiedCompIDs.size() + " comps.");
		
		// This will hold list of fully expanded comps to run
		ArrayList<DbComputation> toRun = new ArrayList<DbComputation>();

		// The resolver will make concrete clones and maintain toRun list of comps.
		DbCompResolver resolver = new DbCompResolver(theDb);
		
		for(DbKey compId : specifiedCompIDs)
		{
			DbComputation comp = computationDAO.getComputationById(compId);

			if (comp.hasGroupInput())
			{
				for (TimeSeriesIdentifier tsid : compDependsDAO.getTriggersFor(compId))
				{
					if (!tsids.contains(tsid))
						continue;

					DbComputation concrete = DbCompResolver.makeConcrete(theDb, timeSeriesDAO, tsid, comp, false);
					if (concrete != null)
						// The resolver's addToResults method gets rid of duplicates.
						resolver.addToResults(toRun, concrete, null);
				}
			}
			else // non-group comp, no need to make concrete.
				toRun.add(comp);
		}
		
		// Fetch the data for the specified time range for the specified TSIDs.
		DataCollection theData = new DataCollection();
		
		for(TimeSeriesIdentifier tsid : tsids)
		{
			try
			{
				CTimeSeries	cts = timeSeriesDAO.makeTimeSeries(tsid);
				int n = timeSeriesDAO.fillTimeSeries(cts, since, until);
				info("Read tsid '" + tsid.getUniqueString() + "' since="
					+ (since==null ? "null" : sdf.format(since)) + ", until="
					+ (until==null ? "null" : sdf.format(until))
					+ ", result=" + n + " values.");
				// Set the flag so that every value read is treated as a trigger.
				for(int idx = 0; idx < n; idx++)
					VarFlags.setWasAdded(cts.sampleAt(idx));
				theData.addTimeSeries(cts);
			}
			catch (Exception ex)
			{
				String msg = "Error fetching input data: " + ex;
				warning(msg);
				System.err.print(msg);
				ex.printStackTrace(System.err);
			}
		}
		
		if (!quietToken.getValue())
		{
			System.out.println("since="
				+ (since==null ? "anytime" : sdf.format(since))
				+ ", until="
				+ (until==null ? "anytime" : sdf.format(until)) + " UTC");
			System.out.println("# total comps=" + toRun.size() + ", # TSIDs=" + tsids.size());
			System.out.println("Okay to proceed (y/n)? ");
			String x = System.console().readLine();
			if (x == null || !(x.toLowerCase().startsWith("y")))
				System.exit(0);
		}
		
		// Execute the computations
		for(DbComputation comp2run : toRun)
		{
			try
			{
				info("Executing computation " + comp2run.getName());
				comp2run.prepareForExec(theDb);
				comp2run.apply(theData, theDb);
			}
			catch (Exception ex)
			{
				String msg = "Error executing comp '" + comp2run.getName() + "': " + ex;
				warning(msg);
			}
		}

		// if an output format is specified, format the data and send to stdout
		if (outputFormatter != null)
		{
			byte[] dummyData = new byte[0];
			RawMessage rawMsg = new RawMessage(dummyData);
			rawMsg.setPlatform(null);
			rawMsg.setTransportMedium(null);
			String ttype="site";
			String tidArg="unknown";
			rawMsg.setTransportMedium(new TransportMedium(null, ttype, tidArg));
			rawMsg.setTimeStamp(new Date());
			rawMsg.setHeaderLength(0);
			rawMsg.setPM(GoesPMParser.MESSAGE_TIME, new Variable(new Date()));
			rawMsg.setPM(GoesPMParser.MESSAGE_LENGTH, new Variable(0L));
			
			try
			{
				outputTimeSeries(rawMsg, theData.getAllTimeSeries());
			}
			catch (Exception ex)
			{
				warning("Cannot output results: " + ex);
				System.err.println("Cannot output results: " + ex);
				ex.printStackTrace(System.err);
			}
		}
		else // Else write directly to database.
		{
			for(CTimeSeries cts : theData.getAllTimeSeries())
			{
				int numChanges = 0;
				Date earliest=null, latest=null;
				for(int idx = 0; idx < cts.size(); idx++)
				{
					TimedVariable tv = cts.sampleAt(idx);
					if (VarFlags.mustWrite(tv) || VarFlags.mustDelete(tv))
					{
						numChanges++;
						if (earliest == null)
							earliest = tv.getTime();
						latest = tv.getTime();
					}
				}
				
				if (numChanges > 0)
				{
					String s = "Writing " + numChanges + " values for time series " 
						+ cts.getTimeSeriesIdentifier().getUniqueString()
						+ ", earliest=" + sdf.format(earliest) + ", latest=" + sdf.format(latest);
					info(s);
					if (!quietToken.getValue())
						System.out.println(s);
					try { timeSeriesDAO.saveTimeSeries(cts); }
					catch(BadTimeSeriesException ex)
					{
						warning("Cannot save time series '" + cts.getTimeSeriesIdentifier() + "': " + ex);
					}
				}
			}
		}
		timeSeriesDAO.close();
		compDependsDAO.close();
		computationDAO.close();

	}
	
	/**
	 * This method adds a command line argument to allow
	 * the user to turn off the Db Computations list filter.
	 * By default is on.
	 */
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		cmdLineArgs.addToken(ctrlFileToken);
		cmdLineArgs.addToken(compIdToken);
		cmdLineArgs.addToken(tsidToken);
		cmdLineArgs.addToken(groupIdToken);
		cmdLineArgs.addToken(sinceToken);
		cmdLineArgs.addToken(untilToken);
		cmdLineArgs.addToken(outputFmtToken);
		cmdLineArgs.addToken(presGrpToken);
		cmdLineArgs.addToken(tzArg);
		cmdLineArgs.addToken(quietToken);

		appNameArg.setDefaultValue("utility");
	}
	
	/** Main method. Used when running from the rumcomp script */
	public static void main(String[] args)
	{
		DecodesInterface.setGUI(false);
		DecodesInterface.silent = true;
		CompExec app = new CompExec();
		try{ app.execute(args); }
		catch(Exception ex)
		{
			System.err.println(ex.getMessage());
			ex.printStackTrace(System.err);
		}
	}
	
	private void readControlFile(String filename)
		throws DbIoException
	{
		String exp = EnvExpander.expand(filename);
		File ctrlfile = new File(exp);
		if (!ctrlfile.canRead())
		{
			System.err.println("Cannot read control file '" + exp 
				+ "' -- check name and permissions.");
			System.exit(1);
		}
		try
		{
			LineNumberReader lnr = new LineNumberReader(new FileReader(ctrlfile));
			String line;
			while ((line = lnr.readLine()) != null)
			{
				line = line.trim();
				if (line.length() == 0 || line.charAt(0) == '#')
					continue;
				
				if (TextUtil.startsWithIgnoreCase(line, "tsid "))
					loadTsid(line.substring(5).trim());
				else if (TextUtil.startsWithIgnoreCase(line, "group "))
					loadGroup(line.substring(6).trim());
				else if (TextUtil.startsWithIgnoreCase(line, "comp "))
					loadComp(line.substring(5).trim());
				else if (TextUtil.startsWithIgnoreCase(line, "since "))
				{
					try { since = sdf.parse(line.substring(6)); }
					catch(ParseException ex)
					{
						System.err.println("Invalid Since on line " + lnr.getLineNumber()
							+ " '" + sinceToken.getValue()
							+ "' -- format must be '" + dateSpec + " UTC");
						System.exit(1);
					}
				}
				else if (TextUtil.startsWithIgnoreCase(line, "until "))
				{
					try { until = sdf.parse(line.substring(6)); }
					catch(ParseException ex)
					{
						System.err.println("Invalid Until on line " + lnr.getLineNumber()
							+ " '" + sinceToken.getValue()
							+ "' -- format must be '" + dateSpec + " UTC");
						System.exit(1);
					}
				}

				
			}
			lnr.close();
		}
		catch (IOException ex)
		{
			System.err.println("Error reading control file '" + exp + "': " + ex);
			System.exit(1);
		}
	}
	
	/**
	 * Parse the TSID and add it to the list.
	 * @param tsidStr
	 * @throws DbIoException
	 */
	private void loadTsid(String tsidStr)
		throws DbIoException
	{
		try
		{
			tsids.add(timeSeriesDAO.getTimeSeriesIdentifier(tsidStr));
		}
		catch (NoSuchObjectException ex)
		{
			System.err.println("Invalid TSID '" + tsidStr + "': " + ex);
			System.exit(1);
		}
	}
	
	/**
	 * Load the specified group, expand it, and add the TSIDs to the list.
	 * @param nameOrId can be unique group name or ID
	 */
	private void loadGroup(String nameOrId)
		throws DbIoException
	{
		TsGroup tsGroup = null;
		try 
		{
			DbKey groupId = DbKey.createDbKey(Long.parseLong(nameOrId.trim()));
			tsGroup = tsGroupDAO.getTsGroupById(groupId);
		}
		catch(Exception ex)
		{
			tsGroup = tsGroupDAO.getTsGroupByName(nameOrId.trim());
		}
		if (tsGroup == null)
		{
			System.err.println("No matching group ID or name for '" + nameOrId + "'");
			System.exit(1);;
		}
		
		GroupHelper groupHelper = theDb.makeGroupHelper();
		groupHelper.expandTsGroup(tsGroup);
		tsids.addAll(tsGroup.getExpandedList());
	}
	
	private void loadComp(String nameOrId)
		throws DbIoException
	{
		try 
		{
			DbKey compId = DbKey.createDbKey(Long.parseLong(nameOrId.trim()));
			specifiedCompIDs.add(compId);
//			specifiedComps.add(computationDAO.getComputationById(compId));
		}
		catch(Exception ex)
		{
			try
			{
				specifiedCompIDs.add(computationDAO.getComputationId(nameOrId.trim()));
//				computationDAO.getComputationByName(nameOrId.trim());
			}
			catch (NoSuchObjectException ex2)
			{
				System.err.println("No matching computation ID or name for '" + nameOrId + "'");
				System.exit(1);;
			}
		}
	}
	
	public void outputTimeSeries(RawMessage rawMsg, Collection<CTimeSeries> ctss)
		throws OutputFormatterException, IOException,
		DataConsumerException, UnknownPlatformException
	{
		DecodedMessage decmsg = new DecodedMessage(rawMsg);
		for(CTimeSeries cts : ctss)
		{
			TimeSeries ts = TSUtil.convert2DecodesTimeSeries(cts);
			Sensor sensor = ts.getSensor();
			boolean toAdd = true;
			if (presGrp != null)
			{
				DataPresentation dp = presGrp.findDataPresentation(sensor);
				if (dp != null)
				{
					if (dp.getUnitsAbbr() != null
					 && dp.getUnitsAbbr().equalsIgnoreCase("omit"))
					{
						Logger.instance().log(Logger.E_DEBUG2,
							"Omitting sensor '" + sensor.getName() 
							+ "' as per Presentation Group.");
						toAdd = false;
					}
					else
						ts.formatSamples(dp);
				}
			}
			if (toAdd)
				decmsg.addTimeSeries(ts);
		}
		
		PipeConsumer pipeConsumer = new PipeConsumer();
		pipeConsumer.open("", new Properties());
		outputFormatter.formatMessage(decmsg, pipeConsumer);
	}

}
