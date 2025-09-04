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
package decodes.tsdb.algo.jep;

import ilex.util.TextUtil;
import ilex.var.TimedVariable;

import java.util.Date;
import java.util.HashSet;
import java.util.TimeZone;
import java.util.TreeSet;

import opendcs.dai.TimeSeriesDAI;

import org.nfunk.jep.JEP;
import org.nfunk.jep.ParseException;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.cwms.validation.Screening;
import decodes.cwms.validation.ScreeningCriteria;
import decodes.db.Site;
import decodes.tsdb.IntervalCodes;
import decodes.tsdb.IntervalIncrement;
import decodes.tsdb.ParmRef;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.VarFlags;
import decodes.tsdb.algo.AW_AlgorithmBase;
import decodes.util.TSUtil;

/**
 * JepContext provides a link to the database and the algorithm.
 * It also stores the current time slice date/time if this is being executed
 * within a time slice.
 * It also stores various context flags resulting from execution of cond, else, etc.
 */
public class JepContext
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private JEP parser = null;
	private boolean exitCalled = false;
	private boolean lastConditionFailed = false;
	private Date timeSliceBaseTime = null;
	private AW_AlgorithmBase algo = null;
	private TimeSeriesDb tsdb = null;
	private String gotoLabel = null;
	private String onErrorLabel = null;
	private boolean lastStatementWasCond = false;
	private HashSet<String> rolesInitializedForScreening = null;

	public JepContext(TimeSeriesDb tsdb, AW_AlgorithmBase algo)
	{
		this.tsdb = tsdb;
		this.algo = algo;
		parser = new JEP();
		parser.addStandardFunctions();
		parser.addStandardConstants();
		parser.setAllowAssignment(true);
		parser.setAllowUndeclared(true);
		parser.addFunction(LookupMetaFunction.funcName, new LookupMetaFunction());
		parser.addFunction(ConditionFunction.funcName, new ConditionFunction(this));
		parser.addFunction(ElseFunction.funcName, new ElseFunction(this));
		parser.addFunction(ExitFunction.funcName, new ExitFunction(this));
		// Old level name functions kept to not break existing code... yet.
		parser.addFunction("trace", new LogFunction(this, () -> log.atTrace()));
		parser.addFunction("debug3", new LogFunction(this, () -> log.atTrace()));
		parser.addFunction("debug2", new LogFunction(this, () -> log.atTrace()));
		parser.addFunction("debug1", new LogFunction(this, () -> log.atDebug()));
		parser.addFunction("debug", new LogFunction(this, () -> log.atDebug()));
		parser.addFunction("info", new LogFunction(this, () -> log.atInfo()));
		parser.addFunction("warning", new LogFunction(this, () -> log.atWarn()));
		parser.addFunction("warn", new LogFunction(this, () -> log.atWarn()));
		parser.addFunction(OnErrorFunction.funcName, new OnErrorFunction(this));
		parser.addFunction(DatchkFunction.funcName, new DatchkFunction(this));
		parser.addFunction(IsRejectedFunction.funcName, new IsRejectedFunction(this));
		parser.addFunction(IsQuestionableFunction.funcName, new IsQuestionableFunction(this));
		parser.addFunction(ScreeningFunction.funcName, new ScreeningFunction(this));

		if (tsdb == null || tsdb.isCwms())
			parser.addFunction(RatingFunction.funcName, new RatingFunction(this));
	}

	/**
	 * Call before executing a new expression to reset all flags.
	 */
	public void reset()
	{
		exitCalled = false;
		if (!lastStatementWasCond)
			lastConditionFailed = false;
		lastStatementWasCond = false;
		gotoLabel = null;
	}

	public boolean isExitCalled()
	{
		return exitCalled;
	}

	public void setExitCalled(boolean exitCalled)
	{
		this.exitCalled = exitCalled;
	}

	public boolean getLastConditionFailed()
	{
		return lastConditionFailed;
	}

	public void setLastConditionFailed(boolean lastConditionFailed)
	{
		this.lastConditionFailed = lastConditionFailed;
	}

	public Date getTimeSliceBaseTime()
	{
		return timeSliceBaseTime;
	}

	public void setTimeSliceBaseTime(Date timeSliceBaseTime)
	{
		this.timeSliceBaseTime = timeSliceBaseTime;
	}

	public AW_AlgorithmBase getAlgo()
	{
		return algo;
	}

	public TimeSeriesDb getTsdb()
	{
		return tsdb;
	}

	public String getGotoLabel()
	{
		return gotoLabel;
	}

	public void setGotoLabel(String gotoLabel)
	{
		this.gotoLabel = gotoLabel;
	}

	public String getOnErrorLabel()
	{
		return onErrorLabel;
	}

	public void setOnErrorLabel(String onErrorLabel)
	{
		this.onErrorLabel = onErrorLabel;
	}

	public JEP getParser()
	{
		return parser;
	}

	public void setLastStatementWasCond(boolean lastStatementWasCond)
	{
		this.lastStatementWasCond = lastStatementWasCond;
	}

	public void initForScreening(String inputRole, Screening screening)
		throws ParseException
	{
		if (rolesInitializedForScreening == null)
			rolesInitializedForScreening = new HashSet<String>();
		else if (rolesInitializedForScreening.contains(inputRole))
			return; // already done.

		log.trace("Retrieving additional data needed for checks for '{}'.", inputRole);
		rolesInitializedForScreening.add(inputRole);

		ParmRef inputParm = algo.getParmRef(inputRole);
		TimeSeriesIdentifier inputTsid = inputParm.timeSeries.getTimeSeriesIdentifier();

		Site site = inputTsid.getSite();
		if (site != null && site.timeZoneAbbr != null && site.timeZoneAbbr.length() > 0)
		{
			TimeZone tz = TimeZone.getTimeZone(site.timeZoneAbbr);
			log.debug("Setting criteria season timezone to {}", tz.getID());
		}

		// Using the tests, determine the amount of past-data needed at each time-slice.
		TreeSet<Date> needed = new TreeSet<Date>();
		IntervalIncrement tsinc = IntervalCodes.getIntervalCalIncr(inputTsid.getInterval());
		boolean inputIrregular = tsinc == null || tsinc.getCount() == 0;

		ScreeningCriteria prevcrit = null;
		for(int idx = 0; idx<inputParm.timeSeries.size(); idx++)
		{
			TimedVariable tv = inputParm.timeSeries.sampleAt(idx);
			if (VarFlags.wasAdded(tv))
			{
				ScreeningCriteria crit = screening.findForDate(tv.getTime(), TimeZone.getTimeZone("UTC"));
				if (crit == null || crit == prevcrit)
					continue;
				crit.fillTimesNeeded(inputParm.timeSeries, needed, algo.aggCal, algo);
				prevcrit = crit;
			}
		}
		log.trace("additional data done for '{}', #times needed={}", inputRole, needed.size());

		if (needed.size() > 0)
		{
			TimeSeriesDAI timeSeriesDAO = tsdb.makeTimeSeriesDAO();
			try
			{
				// Optimization: if >= 50 values within 4 days,
				// use a range retrieval.
				Date start = needed.first();
				Date end = needed.last();
				if (inputIrregular
				 || (needed.size() >= 50 && end.getTime() - start.getTime() <= (4*24*3600*1000L)))
				{
					timeSeriesDAO.fillTimeSeries(inputParm.timeSeries, start, end, true, true, false);
				}
				else
					timeSeriesDAO.fillTimeSeries(inputParm.timeSeries, needed);
			}
			catch (Exception ex)
			{
				// log here because this ParseException doesn't allow us to
				ParseException toThrow = new ParseException("Unable to initialize contenxt, could not fill in time series.");
				toThrow.addSuppressed(ex);
				throw toThrow;
			}
			finally
			{
				timeSeriesDAO.close();
			}
		}

		String euAbbr = screening.getCheckUnitsAbbr();
		if (euAbbr != null
		 && TextUtil.strCompareIgnoreCase(euAbbr, inputParm.timeSeries.getUnitsAbbr()) != 0)
		{
			// Need to convert the input param into the proper units.
			TSUtil.convertUnits(inputParm.timeSeries, euAbbr);
		}
	}
}
