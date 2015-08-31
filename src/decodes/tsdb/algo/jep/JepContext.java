package decodes.tsdb.algo.jep;

import ilex.util.Logger;
import ilex.util.TextUtil;
import ilex.var.TimedVariable;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

import opendcs.dai.TimeSeriesDAI;

import org.nfunk.jep.JEP;
import org.nfunk.jep.ParseException;

import decodes.cwms.validation.Screening;
import decodes.cwms.validation.ScreeningCriteria;
import decodes.tsdb.IntervalCodes;
import decodes.tsdb.IntervalIncrement;
import decodes.tsdb.ParmRef;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesHelper;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.VarFlags;
import decodes.tsdb.algo.AW_AlgorithmBase;

/**
 * JepContext provides a link to the database and the algorithm.
 * It also stores the current time slice date/time if this is being executed
 * within a time slice.
 * It also stores various context flags resulting from execution of cond, else, etc.
 */
public class JepContext
{
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
		parser.addFunction("debug3", new LogFunction(this, Logger.E_DEBUG3));
		parser.addFunction("debug2", new LogFunction(this, Logger.E_DEBUG2));
		parser.addFunction("debug1", new LogFunction(this, Logger.E_DEBUG1));
		parser.addFunction("info", new LogFunction(this, Logger.E_INFORMATION));
		parser.addFunction("warning", new LogFunction(this, Logger.E_WARNING));
		parser.addFunction(OnErrorFunction.funcName, new OnErrorFunction(this));
		parser.addFunction(DatchkFunction.funcName, new DatchkFunction(this));
		parser.addFunction(IsRejectedFunction.funcName, new IsRejectedFunction(this));
		parser.addFunction(IsQuestionableFunction.funcName, new IsQuestionableFunction(this));

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
		
		algo.debug3("Retrieving additional data needed for checks for '" + inputRole + "'.");
		rolesInitializedForScreening.add(inputRole);
		
		ParmRef inputParm = algo.getParmRef(inputRole);
		TimeSeriesIdentifier inputTsid = inputParm.timeSeries.getTimeSeriesIdentifier();

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
				ScreeningCriteria crit = screening.findForDate(tv.getTime());
				if (crit == null || crit == prevcrit)
					continue;
				crit.fillTimesNeeded(inputParm.timeSeries, needed, algo.aggCal, algo);
				prevcrit = crit;
			}
		}
		algo.debug3("additional data done for '" + inputRole + "', #times needed=" + needed.size());
		
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
				throw new ParseException(ex.toString());
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
			TimeSeriesHelper.convertUnits(inputParm.timeSeries, euAbbr);
		}
	}
}
