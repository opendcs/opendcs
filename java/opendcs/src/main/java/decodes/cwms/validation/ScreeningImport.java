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
package decodes.cwms.validation;

import ilex.cmdline.BooleanToken;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import ilex.util.TextUtil;

import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import lrgs.gui.DecodesInterface;
import decodes.cwms.CwmsTimeSeriesDb;
import decodes.cwms.validation.dao.ScreeningDAI;
import decodes.tsdb.DbIoException;
import decodes.tsdb.IntervalCodes;
import decodes.tsdb.IntervalIncrement;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsdbAppTemplate;
import decodes.util.CmdLineArgs;
import opendcs.dai.TimeSeriesDAI;

public class ScreeningImport extends TsdbAppTemplate
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	public String module = "ScreeningImport";

	private BooleanToken noTsidsToken = new BooleanToken("T", "(Do NOT import TSIDs and their screening assignments.)",
		"", TokenOptions.optSwitch, false);
	private StringToken fileNameToken = new StringToken("", "Screening file(s) to import",
		"", TokenOptions.optArgument | TokenOptions.optRequired | TokenOptions.optMultiple, "");
	private BooleanToken yesToken = new BooleanToken("y", "(Yes do the import. Don't ask for confirmation.)",
		"", TokenOptions.optSwitch, false);

	private Screening currentScreening = null;
	private ScreeningCriteria critBuffer = null;
	private ArrayList<Screening> screenings = new ArrayList<Screening>();

	// For each screening name, a list of time series assigned to it.
	private HashMap<String, ArrayList<String>> screeningName2TsidMap = new HashMap<String, ArrayList<String>>();

	private TimeSeriesDAI timeSeriesDAO = null;
	private LineNumberReader lnr = null;
	private String curFile = null;

	enum ParseState { Top, InScreening, InCriteria };
	private ParseState parseState = ParseState.Top;
	private int numWarnings = 0;

	private ScreeningImport()
	{
		super("screening.log");
	}

	@Override
	public void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		cmdLineArgs.addToken(noTsidsToken);
		cmdLineArgs.addToken(yesToken);
		cmdLineArgs.addToken(fileNameToken);
		DecodesInterface.silent = true;
	}

	@Override
	protected void runApp() throws Exception
	{
		timeSeriesDAO = theDb.makeTimeSeriesDAO();

		int totalWarnings = 0;
		for(int idx=0; idx < fileNameToken.NumberOfValues(); idx++)
		{
			numWarnings = 0;
			curFile = fileNameToken.getValue(idx);
			if (!yesToken.getValue())
				System.out.println("Reading file " + curFile);
			else
				log.info("Reading file {}", curFile);
			readFile(curFile);
			if (numWarnings > 0)
			{
				System.out.println("There were " + numWarnings + " errors in reading this file.");
				totalWarnings += numWarnings;
			}
		}
		curFile = null;

		// Printout list of what will be imported
		if (!yesToken.getValue())
		{
			System.out.println("The following screenings will be imported:");
			for(Screening s : screenings)
			{
				System.out.println("\t" + s.getScreeningName());
				if (!noTsidsToken.getValue())
				{
					System.out.println("\t...For time series:");
					ArrayList<String> tsidList = screeningName2TsidMap.get(s.getScreeningName());
					if (tsidList == null)
						System.out.println("\t\t(none)");
					else for(String tsid : tsidList)
						System.out.println("\t\t" + tsid);
				}
			}
			if (totalWarnings > 0)
				System.out.println("NOTE: There were " + totalWarnings + " in reading the screening files.");

			System.out.print("OK to continue? (y/n)");
			System.out.flush();
			String line = System.console().readLine();
			if (line == null || !line.trim().toLowerCase().startsWith("y"))
				System.exit(1);
		}

		ScreeningDAI screeningDAO = ((CwmsTimeSeriesDb)theDb).makeScreeningDAO();
		// Write the screenings
		for(Screening screening : screenings)
		{
			try
			{
				screeningDAO.writeScreening(screening);
			}
			catch(DbIoException ex)
			{
				log.atWarn().setCause(ex).log("Cannot write screening '{}'", screening.getScreeningName());
				continue;
			}

			if (!noTsidsToken.getValue())
			{
				// Resolve the time series and create if necessary
				ArrayList<String> tsidList = screeningName2TsidMap.get(screening.getScreeningName());
				if (tsidList != null)
				{
					for(String tsidStr : tsidList)
					{
						TimeSeriesIdentifier tsid = getTsid(tsidStr);
						if (tsid != null)
							screeningDAO.assignScreening(screening, tsid, true);
					}
				}
			}
		}


		timeSeriesDAO.close();
	}

	/**
	 * Read the passed file name and load into screening buffers.
	 * @param value
	 */
	private void readFile(String filename)
	{
		File file = new File(filename);
		parseState = ParseState.Top;
		currentScreening = null;
		critBuffer = null;

		log.debug("Loading Screening file '{}'", filename);

		try
		{
			lnr = new LineNumberReader(new FileReader(file));
			String line;
			while((line = lnr.readLine()) != null)
			{
				line = line.trim();
				if (line.length() == 0 || line.charAt(0) == '*')
					continue;

				StringTokenizer st = new StringTokenizer(line, " ");
				String kw = st.nextToken();

				if (kw.equalsIgnoreCase("SCREENING"))
					screening(st);
				else if (kw.equalsIgnoreCase("screening_end"))
					screening_end();
				else if (kw.equalsIgnoreCase("desc"))
					desc(line);
				else if (kw.equalsIgnoreCase("param"))
					param(st);
				else if (kw.equalsIgnoreCase("paramType"))
					paramType(st);
				else if (kw.equalsIgnoreCase("duration"))
					duration(st);
				else if (kw.equalsIgnoreCase("units"))
					units(st);
				else if (kw.equalsIgnoreCase("criteria_set"))
					criteria_set(st);
				else if (kw.equalsIgnoreCase("criteria"))
					criteria(st);
				else if (kw.equalsIgnoreCase("criteria_set_end"))
					criteria_set_end();
				else if (kw.equalsIgnoreCase("assign"))
					assign(line);
				else if (kw.equalsIgnoreCase("season"))
					season(st);
				else if (kw.equalsIgnoreCase("range_active"))
					range_active(st);
				else if (kw.equalsIgnoreCase("roc_active"))
					roc_active(st);
				else if (kw.equalsIgnoreCase("const_active"))
					const_active(st);
				else if (kw.equalsIgnoreCase("durmag_active"))
					durmag_active(st);
				else if (kw.equalsIgnoreCase("estimate"))
					estimate(line);
			}
		}
		catch (IOException ex)
		{
			log.atWarn().setCause(ex).log("Error reading screening file '{}'", filename);
		}
		finally
		{
			if (lnr != null)
			{
				try {lnr.close(); } catch(Exception ex) {}
				lnr = null;
			}
		}
	}

	private void estimate(String line)
	{
		if (parseState != ParseState.InCriteria)
		{
			log.warn("ESTIMATE while not inside a CRITERIA block. Ignored.");
			return;
		}

		critBuffer.setEstimateExpression(line.substring(8).trim());
	}

	private void durmag_active(StringTokenizer st)
	{
		if (currentScreening == null)
		{
			log.warn("DURMAG_ACTIVE while not in a SCREENING block. Ignored.");
			return;
		}
		if (!st.hasMoreTokens())
		{
			log.warn("DURMAG_ACTIVE with no boolean argument. Ignored.");
			return;
		}
		currentScreening.setDurMagActive(TextUtil.str2boolean(st.nextToken()));
	}

	private void const_active(StringTokenizer st)
	{
		if (currentScreening == null)
		{
			log.warn("CONST_ACTIVE while not in a SCREENING block. Ignored.");
			return;
		}
		if (!st.hasMoreTokens())
		{
			log.warn("CONST_ACTIVE with no boolean argument. Ignored.");
			return;
		}
		currentScreening.setConstActive(TextUtil.str2boolean(st.nextToken()));
	}

	private void roc_active(StringTokenizer st)
	{
		if (currentScreening == null)
		{
			log.warn("ROC_ACTIVE while not in a SCREENING block. Ignored.");
			return;
		}
		if (!st.hasMoreTokens())
		{
			log.warn("ROC_ACTIVE with no boolean argument. Ignored.");
			return;
		}
		currentScreening.setRocActive(TextUtil.str2boolean(st.nextToken()));
	}

	private void range_active(StringTokenizer st)
	{
		if (currentScreening == null)
		{
			log.warn("RANGE_ACTIVE while not in a SCREENING block. Ignored.");
			return;
		}
		if (!st.hasMoreTokens())
		{
			log.warn("RANGE_ACTIVE with no boolean argument. Ignored.");
			return;
		}
		currentScreening.setRangeActive(TextUtil.str2boolean(st.nextToken()));
	}

	private void season(StringTokenizer st)
	{
		if (parseState != ParseState.InCriteria)
		{
			log.warn("SEASON while not in a criteria block. Ignored.");
			return;
		}
		if (!st.hasMoreTokens())
		{
			log.warn("SEASON expected MM/DD. Ignored.");
			return;
		}
		String mmdd = st.nextToken();
		int slash = mmdd.indexOf('/');
		if (slash == -1)
		{
			log.warn("Cannot parse value after SEASON. Expected MM/DD. Got '{}'. Ignored.", mmdd);
			return;
		}
		try
		{
			int mm = Integer.parseInt(mmdd.substring(0, slash));
			int dd = Integer.parseInt(mmdd.substring(slash+1));
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.MONTH, mm - 1);
			cal.set(Calendar.DAY_OF_MONTH, dd);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			critBuffer.seasonStart = cal;
		}
		catch(Exception ex)
		{
			log.atWarn().setCause(ex).log("Cannot parse value after SEASON. Expected MM/DD. Got '{}'. Ignored.", mmdd);
			return;
		}
	}

	private void assign(String line)
	{
		line = line.trim();
		line = line.substring(6).trim();

		int colon = line.indexOf(':');
		String tsidStr = line.substring(0, colon).trim();
		String screeningName = line.substring(colon+1).trim();

		ArrayList<String> tsList = screeningName2TsidMap.get(screeningName);
		if (tsList == null)
		{
			tsList = new ArrayList<String>();
			screeningName2TsidMap.put(screeningName, tsList);
		}
		tsList.add(tsidStr);
	}

	private TimeSeriesIdentifier getTsid(String tsidStr)
	{
		TimeSeriesIdentifier tsid = null;
		try
		{
			try
			{
				tsid = timeSeriesDAO.getTimeSeriesIdentifier(tsidStr);
			}
			catch (NoSuchObjectException e)
			{
				tsid = theDb.makeEmptyTsId();
				tsid.setUniqueString(tsidStr);
				timeSeriesDAO.createTimeSeries(tsid);
			}
			return tsid;
		}
		catch (Exception ex)
		{
			log.atWarn().setCause(ex).log("Cannot retrieve or create time series '{}'", tsidStr);
			return null;
		}
	}

	private void criteria_set_end()
	{
		if (parseState != ParseState.InCriteria)
		{
			log.warn("CRITERIA_SET_END while not in a criteria block. Ignored.");
			return;
		}
		currentScreening.add(critBuffer);
		critBuffer = null;
		parseState = ParseState.InScreening;
	}

	private void criteria_set(StringTokenizer st)
	{
		if (parseState == ParseState.Top)
		{
			log.warn("CRITERIA_SET not inside a SCREENING block. Ignored.");
			return;
		}
		else if (parseState == ParseState.InCriteria)
		{
			log.warn("CRITERIA_SET while already in a criteria block. Starting new block.");
			criteria_set_end();
		}
		critBuffer = new ScreeningCriteria(null);
		parseState = ParseState.InCriteria;

	}

	private void units(StringTokenizer st)
	{
		if (currentScreening == null)
		{
			log.warn("UNITS not inside valid SCREENING block. Ignored.");
			return;
		}
		currentScreening.setCheckUnitsAbbr(st.nextToken());
	}

	private void duration(StringTokenizer st)
	{
		if (currentScreening == null)
		{
			log.warn("DURATION not inside valid SCREENING block. Ignored.");
			return;
		}
		currentScreening.setDurationId(st.nextToken());
	}

	private void paramType(StringTokenizer st)
	{
		if (currentScreening == null)
		{
			log.warn("PARAMTYPE not inside valid SCREENING block. Ignored.");
			return;
		}
		currentScreening.setParamTypeId(st.nextToken());
	}

	private void param(StringTokenizer st)
	{
		if (currentScreening == null)
		{
			log.warn("PARAM not inside valid SCREENING block. Ignored.");
			return;
		}
		currentScreening.setParamId(st.nextToken());
	}

	private void desc(String line)
	{
		if (currentScreening == null)
		{
			log.warn("DESC not inside valid SCREENING block. Ignored.");
			return;
		}
		String desc = currentScreening.getScreeningDesc();
		if (desc == null)
			desc = "";
		else
			desc = desc + "\n" + line.substring(4).trim();
	}

	private void screening_end()
	{
		if (currentScreening != null)
			screenings.add(currentScreening);
		currentScreening = null;
		critBuffer = null;
		parseState = ParseState.Top;
	}

	private void screening(StringTokenizer st)
	{
		if (parseState != ParseState.Top)
		{
			log.warn("SCREENING when already inside a screening. Assume missing SCREENING_END keyword.");
			screening_end();
		}

		if (!st.hasMoreTokens())
		{
			log.warn("SCREENING with no screening name. Ignored");
			return;
		}
		currentScreening = new Screening();
		currentScreening.setScreeningName(st.nextToken());
		parseState = ParseState.InScreening;
	}

	private void criteria(StringTokenizer st)
	{
		if (parseState != ParseState.InCriteria)
		{
			log.warn("CRITERIA while not in CRITERIA block. Ignored.");
			return;
		}
		if (!st.hasMoreTokens())
		{
			log.warn("CRITERIA with no type field -- ignored.");
			return;
		}
		String type = st.nextToken().toUpperCase();
		if (!st.hasMoreTokens())
		{
			log.warn("CRITERIA {} with no arguments.", type);
			return;
		}
		String tok = st.nextToken().toUpperCase();
		char qflag = tok.charAt(0);
		if (qflag != 'R' && qflag != 'Q' && qflag != 'M')
		{
			log.warn("CRITERIA {} with unrecognized qflag '{}' -- ignored.", type, qflag);
			return;
		}
		if (type.startsWith("ABS"))
		{
			// absolute magnitude test args: qflag min max
			if (st.countTokens() < 2)
			{
				log.warn("CRITERIA {} missing range variables -- ignored.", type);
				return;
			}
			double min = Double.NEGATIVE_INFINITY;
			double max = Double.POSITIVE_INFINITY;
			try
			{
				min = Double.parseDouble(st.nextToken());
				max = Double.parseDouble(st.nextToken());
			}
			catch(Exception ex)
			{
				log.atWarn().setCause(ex).log("CRITERIA {} non-numeric range variables -- ignored.", type);
				return;
			}
			critBuffer.addAbsCheck(new AbsCheck(qflag, min, max));
		}
		else if (type.startsWith("DUR"))
		{
			// duration magnitude test args: qflag min max duration
			if (st.countTokens() < 3)
			{
				log.warn("CRITERIA {} missing variables -- ignored.", type);
				return;
			}
			double min = Double.NEGATIVE_INFINITY;
			double max = Double.POSITIVE_INFINITY;
			try
			{
				min = Double.parseDouble(st.nextToken());
				max = Double.parseDouble(st.nextToken());
			}
			catch(Exception ex)
			{
				log.atWarn().setCause(ex).log("CRITERIA {} non-numeric range variables -- ignored.", type);
				return;
			}
			tok = st.nextToken();
			String duration = dur2IntervalCode(tok);
			critBuffer.addDurCheckPeriod(
				new DurCheckPeriod(qflag, duration, min, max));
		}
		else if (type.startsWith("CONST"))
		{
			// constant value test args: qflag duration [min [tolerance [maxmissing]]]
			if (!st.hasMoreTokens())
			{
				log.warn("CRITERIA {} missing duration -- ignored.", type);
				return;
			}
			tok = st.nextToken();
			String duration = dur2IntervalCode(tok);
			double min = 0.0;
			double tolerance = 0.0;
			int nmissing = 0;
			IntervalIncrement maxGap = null;
			if (st.hasMoreTokens())
			{
				try
				{
					min = Double.parseDouble(st.nextToken());
					if (st.hasMoreTokens())
					{
						tolerance = Double.parseDouble(st.nextToken());
						if (st.hasMoreTokens())
						{
							String missingArg = st.nextToken();
							try { nmissing = Integer.parseInt(missingArg); }
							catch(NumberFormatException ex)
							{
								maxGap = IntervalCodes.getIntervalCalIncr(missingArg);
								if (maxGap == null)
									log.warn("Invalid nmissing argument '{}' -- ignored.", missingArg);
							}
						}
					}
				}
				catch(Exception ex)
				{
					log.atWarn().setCause(ex).log("CRITERIA {} non-numeric min and/or tolerance.", type);
					return;
				}
			}
			ConstCheck cc = new ConstCheck(qflag, duration, min, tolerance, nmissing);
			cc.setMaxGap(maxGap);
			critBuffer.addConstCheck(cc);

		}
		else if (type.startsWith("RATE"))
		{
			// rate of change test args: qflag neg-rate positive-rate
			if (st.countTokens() < 2)
			{
				log.warn("CRITERIA {} missing variables -- ignored.", type);
				return;
			}
			double neg = 0.0;
			double pos = 0.0;
			try
			{
				neg = Double.parseDouble(st.nextToken());
				pos = Double.parseDouble(st.nextToken());
			}
			catch(Exception ex)
			{
				log.atWarn().setCause(ex).log("CRITERIA {} non-numeric range variables -- ignored.", type);
				return;
			}
			critBuffer.addRocPerHourCheck(
				new RocPerHourCheck(qflag, neg, pos));
		}
		else
		{
			log.warn("CRITERIA {} not implemented.", type);
		}
	}

	// Convert a datchk duration into a CWMS interval/duration code
	private String dur2IntervalCode(String tok)
	{
		int idx = 0;
		while(idx < tok.length() && Character.isDigit(tok.charAt(idx)))
			idx++;
		if (idx == 0)
			return IntervalCodes.int_cwms_zero;
		int n = Integer.parseInt(tok.substring(0, idx));
		char u = idx == tok.length() ? 'H' :
			Character.toUpperCase(tok.charAt(idx));
		if (u == 'M')
			switch(n)
			{
			case 1: return IntervalCodes.int_one_minute;
			case 2: return IntervalCodes.int_two_minutes;
			case 3: return IntervalCodes.int_three_minutes;
			case 4: return IntervalCodes.int_four_minutes;
			case 5: return IntervalCodes.int_five_minutes;
			case 6: return IntervalCodes.int_six_minutes;
			case 10: return IntervalCodes.int_ten_minutes;
			case 12: return IntervalCodes.int_twelve_minutes;
			case 15: return IntervalCodes.int_fifteen_minutes;
			case 20: return IntervalCodes.int_twenty_minutes;
			case 30: return IntervalCodes.int_thirty_minutes;
			}
		else if (u == 'H')
			switch(n)
			{
			case 1: return IntervalCodes.int_one_hour;
			case 2: return IntervalCodes.int_two_hours;
			case 3: return IntervalCodes.int_three_hours;
			case 4: return IntervalCodes.int_four_hours;
			case 6: return IntervalCodes.int_six_hours;
			case 8: return IntervalCodes.int_eight_hours;
			case 12: return IntervalCodes.int_twelve_hours;
			}
		else if (u == 'D')
			switch(n)
			{
			case 1: return IntervalCodes.int_one_day;
			case 2: return IntervalCodes.int_two_days;
			case 3: return IntervalCodes.int_three_days;
			case 4: return IntervalCodes.int_four_days;
			case 5: return IntervalCodes.int_five_days;
			case 6: return IntervalCodes.int_six_days;
			}
		return IntervalCodes.int_one_hour;
	}

	/**
	 * Reads all the input files specified in the config and prints a
	 * report containing all screenings.
	 * @param args <configfile> [<seasonTimeZone>]
	 */
	public static void main(String args[])
		throws Exception
	{
		ScreeningImport me = new ScreeningImport();
		me.execute(args);
	}


}
