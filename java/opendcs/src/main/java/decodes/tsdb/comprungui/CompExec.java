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

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import opendcs.dai.CompDependsDAI;
import opendcs.dai.ComputationDAI;
import opendcs.dai.TimeSeriesDAI;
import opendcs.dai.TsGroupDAI;
import lrgs.gui.DecodesInterface;
import ilex.cmdline.BooleanToken;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import ilex.util.EnvExpander;
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
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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

	private HashSet<TimeSeriesIdentifier> tsids = new HashSet<>();
	private HashSet<DbKey> specifiedCompIDs = new HashSet<>();
	private Date since = null, until = null;
	private SimpleDateFormat sdf = new SimpleDateFormat(dateSpec);
	private TimeSeriesDAI timeSeriesDAO = null;
	private ComputationDAI computationDAO = null;
	private TsGroupDAI tsGroupDAO = null;
	private PresentationGroup presGrp = null;
	private OutputFormatter outputFormatter = null;
	private CompDependsDAI compDependsDAO = null;

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
				log.atError()
				   .setCause(ex)
				   .log("Invalid Since Argument '{}' -- format must be '{}' UTC", sinceToken.getValue(), dateSpec);
				System.exit(1);
			}
		}
		if (untilToken.getValue() != null)
		{
			try { until = sdf.parse(untilToken.getValue()); }
			catch(ParseException ex)
			{
				log.atError()
				   .setCause(ex)
				   .log("Invalid Until Argument '{}' -- format must be '{}' UTC", untilToken.getValue(), dateSpec);
				System.exit(1);
			}
		}

		// if outputFmt and presGrp are provided, initialize them.
		String pgName = presGrpToken.getValue();
		if (pgName != null && !pgName.isEmpty())
		{
			presGrp = Database.getDb().presentationGroupList.find(pgName);
			try
			{
				if (presGrp != null)
					presGrp.prepareForExec();
			}
			catch (InvalidDatabaseException ex)
			{
				log.atError()
				   .setCause(ex)
				   .log("Cannot initialize presentation group '{}'' -- will not use.", pgName);
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
				log.atError().setCause(ex).log("Cannot make output formatter.");
				System.exit(1);
			}
		}

		log.info("{} comp IDs and {} TSIDs supplied -- evaluating.", specifiedCompIDs.size(), tsids.size());

		// Now all arguments are read either from cmd line or control file.
		if (tsids.isEmpty() && specifiedCompIDs.isEmpty())
		{
			log.info("Nothing to do -- No time series or computations specified.");
			System.exit(1);
		}
		else if (specifiedCompIDs.isEmpty())
		{
			log.info("Evaluating comps that need to run for specified TSIDs.");
			// Determine the possible enabled computations for the specified TSIDs
			// use CP_COMP_DEPENDS.
			for(DbKey compId : compDependsDAO.getCompIdsFor(tsids, DbKey.NullKey))
				specifiedCompIDs.add(compId);
		}
		else if (tsids.isEmpty())
		{
			// Comps but no TSIDs are specified. Find all possible inputs for the named comps.
			log.info("Comps but no TSIDs supplied, will use all possible triggers.");
			for(DbKey compID : specifiedCompIDs)
				tsids.addAll(compDependsDAO.getTriggersFor(compID));
			log.info("After evaluating triggers, we have {} TSIDs.", tsids.size());
		}
		else
		{
			// both comps and TSIDs are specified. Nothing to do.
		}

		tsGroupDAO.close();

		log.info("After eval, there are {} TSIDs and {} comps.", tsids.size(), specifiedCompIDs.size());

		// This will hold list of fully expanded comps to run
		ArrayList<DbComputation> toRun = new ArrayList<>();

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
				log.info("Read tsid '{}' since={}, until={}, result={} values.",
						 tsid.getUniqueString(), since, until, n);
				// Set the flag so that every value read is treated as a trigger.
				for(int idx = 0; idx < n; idx++)
					VarFlags.setWasAdded(cts.sampleAt(idx));
				theData.addTimeSeries(cts);
			}
			catch (Exception ex)
			{
				log.atWarn().setCause(ex).log("Error fetching input data.");
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

		ComputationExecution execution = new ComputationExecution(theDb);

		theData = execution.execute(toRun, theData);

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
				log.atWarn().setCause(ex).log("Cannot output results.");
			}
		}
		else // Else write directly to database.
		{
			log.info("Saving data: {}", theData.size());
			for(CTimeSeries cts : theData.getAllTimeSeries())
			{
				log.info("Saving: {}", cts.getDisplayName());
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
					log.info(s);
					if (!quietToken.getValue())
						System.out.println(s);
					try { timeSeriesDAO.saveTimeSeries(cts); }
					catch(BadTimeSeriesException ex)
					{
						log.atWarn().setCause(ex).log("Cannot save time series '{}'", cts.getTimeSeriesIdentifier());
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
	@Override
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
		try
		{
			app.execute(args);
		}
		catch(Exception ex)
		{
			log.atError().setCause(ex).log("Unable to run application.");
		}
	}

	private void readControlFile(String filename)
		throws DbIoException
	{
		String exp = EnvExpander.expand(filename);
		File ctrlfile = new File(exp);
		if (!ctrlfile.canRead())
		{
			log.error("Cannot read control file '{}' -- check name and permissions.", exp);
			System.exit(1);
		}
		try (LineNumberReader lnr = new LineNumberReader(new FileReader(ctrlfile)))
		{
			String line;
			while ((line = lnr.readLine()) != null)
			{
				line = line.trim();
				if (line.isEmpty() || line.charAt(0) == '#')
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
						log.atError()
						   .setCause(ex)
						   .log("Invalid Since on line {} '{}' -- format must be '{}'' UTC",
						   		lnr.getLineNumber(), sinceToken.getValue(), dateSpec);
						System.exit(1);
					}
				}
				else if (TextUtil.startsWithIgnoreCase(line, "until "))
				{
					try { until = sdf.parse(line.substring(6)); }
					catch(ParseException ex)
					{
						log.atError()
						   .setCause(ex)
						   .log("Invalid Until on line {} '{}' -- format must be '{}'' UTC",
						   		lnr.getLineNumber(), untilToken.getValue(), dateSpec);
						System.exit(1);
					}
				}


			}
		}
		catch (IOException ex)
		{
			log.atError().setCause(ex).log("Error reading control file '{}'", exp);
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
			log.atError().setCause(ex).log("Invalid TSID '{}'", tsidStr);
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
			log.error("No matching group ID or name for '{}'", nameOrId);
			System.exit(1);
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
		}
		catch(Exception ex)
		{
			try
			{
				specifiedCompIDs.add(computationDAO.getComputationId(nameOrId.trim()));
			}
			catch (NoSuchObjectException ex2)
			{
				ex2.addSuppressed(ex);
				log.atError().setCause(ex).log("No matching computation ID or name for '{}'", nameOrId);
				System.exit(1);
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
						log.trace("Omitting sensor '{}' as per Presentation Group.", sensor.getName());
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