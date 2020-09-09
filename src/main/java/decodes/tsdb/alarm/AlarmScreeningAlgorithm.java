/**
 * $Id$
 * 
 * $Log$
 * Revision 1.3  2019/08/19 14:55:33  mmaloney
 * Permit undefined output param. Move mail server props to ComputationApp
 *
 * Revision 1.2  2019/08/07 14:18:58  mmaloney
 * 6.6 RC04
 *
 * Revision 1.1  2019/07/02 13:48:03  mmaloney
 * 6.6RC04 First working Alarm Implementation
 *
 */
package decodes.tsdb.alarm;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ilex.var.NamedVariableList;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;
import ilex.var.NamedVariable;
import decodes.tsdb.DbAlgorithmExecutive;
import decodes.tsdb.DbCompException;
import decodes.tsdb.DbIoException;
import decodes.tsdb.IntervalIncrement;
import decodes.tsdb.VarFlags;
import decodes.tsdb.alarm.mail.MailerException;
import decodes.tsdb.algo.AWAlgoType;
import decodes.util.PropertySpec;
import decodes.hdb.HdbFlags;
import decodes.sql.DbKey;
import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.ParmRef;
import ilex.var.TimedVariable;
import opendcs.dai.AlarmDAI;
import opendcs.dai.TimeSeriesDAI;
import decodes.tsdb.TimeSeriesIdentifier;

//AW:IMPORTS
// Place an import statements you need here.
//AW:IMPORTS_END

//AW:JAVADOC
/**
Look for alarm screening records in the database and apply to input parameter.
 */
//AW:JAVADOC_END
public class AlarmScreeningAlgorithm
	extends decodes.tsdb.algo.AW_AlgorithmBase
{
//AW:INPUTS
	public double input;	//AW:TYPECODE=i
	String _inputNames[] = { "input" };
//AW:INPUTS_END

//AW:LOCALVARS
	// Will be set to true if input and output refer to the same time series.
	boolean _inputIsOutput = false;
	boolean _noOutput = false;
	private Date earliestTrigger = null, latestTrigger = null;
	private ArrayList<AlarmScreening> screenings = new ArrayList<AlarmScreening>();
	ParmRef inputParm = null;
	private AlarmScreening tScreening = null;
	private AlarmLimitSet tLimitSet = null;
	
	// Enter any local class variables needed by the algorithm.
	PropertySpec algoPropSpecs[] =
	{
		new PropertySpec("noOverwrite", PropertySpec.BOOLEAN, "(default=false) "
			+ "Set to true to disable overwriting of output parameter."),
		new PropertySpec("setInputFlags", PropertySpec.BOOLEAN, "(default=false) "
			+ "Set to true to set quality flags on the input parameter."),
		new PropertySpec("noOutputOnReject", PropertySpec.BOOLEAN, "(default=false) "
			+ "If true and the value is REJECTED, then do not write output param at all. "),
		new PropertySpec("setDataFlags", PropertySpec.BOOLEAN, "(default=true) "
			+ "Set to false to prevent this algorithm from saving screening flag results "
			+ "in each time series value.")
	};
	
	@Override
	protected PropertySpec[] getAlgoPropertySpecs()
	{
		return algoPropSpecs;
	}
	
	public void getAlarmScreenings(TimeSeriesIdentifier inputTsid)
		throws DbCompException
	{
		AlarmDAI alarmDAO = tsdb.makeAlarmDAO();
		
		// May be called from ComputationApp.doCMC to check Missing Computations. In this case
		// there will be no earliest Trigger. Since missing checks are always done based on 'now',
		// set earliestTrigger to now
		if (earliestTrigger == null)
			earliestTrigger = new Date();

		debug2("getAlarmScreenings inputTsid=" + inputTsid.getUniqueString() 
			+ " earliestTrigger=" + debugSdf.format(earliestTrigger));
		try
		{
			DbKey siteId = inputTsid.getSite().getId();
			DbKey dtId = inputTsid.getDataTypeId();
			screenings = alarmDAO.getScreenings(siteId, dtId, comp.getAppId());
			if (screenings == null)
				throw new DbCompException("Invalid TSID for screening '" + inputTsid.getUniqueString() + "'");
debug3("\tAlarmDAO returned " + screenings.size() + 
	" matches for siteID=" +  siteId + ", dtId=" + dtId + ", appId=" + comp.getAppId());

for(int idx=0; idx<screenings.size(); idx++)
{
	AlarmScreening als = screenings.get(idx);
	debug3("\t\t" + idx + ": " + als.getScreeningName() + " siteid=" + als.getSiteId() + ", dtid=" + als.getDatatypeId() 
	+ ", start=" + (als.getStartDateTime() == null ? "null" : debugSdf.format(als.getStartDateTime())) );
}
			
			// If there are no site-specific screenings, OR if the earliest input is before the 
			// site specific screenings, look for a generic one with siteId==nullkey.
			if (screenings.size() == 0 
			 || (screenings.get(0).getStartDateTime() != null 
			       && earliestTrigger.before(screenings.get(0).getStartDateTime())))
			{
debug3("Looking for generic screenings");
				boolean noSiteScreenings = screenings.size() == 0;
				
				// Need to look for generic screening
				ArrayList<AlarmScreening> genScr = 
					alarmDAO.getScreenings(DbKey.NullKey, inputTsid.getDataTypeId(), comp.getAppId());
debug3("DAO returned " + genScr.size() + " generic screenings");
				if (genScr != null && genScr.size() > 0)
				{
					for(int idx = 0; 
						idx < genScr.size() 
						&& (noSiteScreenings
						    || (genScr.get(idx).getStartDateTime() == null
						       || genScr.get(idx).getStartDateTime().before(earliestTrigger)))
							; idx++)
					{

						screenings.add(idx, genScr.get(idx));
					}
				}
			}
debug1("There are " + screenings.size() + " screenings: ");
for(AlarmScreening as : screenings) debug1("   start = " + as.getStartDateTime());
		}
		catch (Exception ex)
		{
			String msg = "Error reading alarm screenings: " + ex;
			warning(msg);
			PrintStream logout = Logger.instance().getLogOutput();
			if (logout != null)
				ex.printStackTrace(logout);
			throw new DbCompException(msg);
		}
		finally
		{
			alarmDAO.close();
		}
		if (screenings.size() == 0)
			throw new DbCompException("No applicable screenings for TSID '" 
				+ inputTsid.getUniqueString() + "'");
	}

	/**
	 * Find the appropriate screening (by start date) and limit set (by season) for
	 * time t. Set the instance variables tScreening and tLimitSet;
	 * @param t the time
	 * @return true if a screening and limit set was found. False if not.
	 */
	public boolean initScreeningAndLimitSet(Date t)
	{
		tScreening = null;
		tLimitSet = null;
		
		// Find the latest screening with start <= t.
		for(AlarmScreening as : screenings)
		{
			if (as.getStartDateTime() != null && as.getStartDateTime().after(t))
				break;
			tScreening = as;
		}
		
		if (tScreening == null)
		{
			info("No applicable screening for '" + inputParm.tsid.getUniqueString() 
				+ "' at time " + debugSdf.format(t));
			return false;
		}
		
		// Now find the limit set within the screening.
		for(AlarmLimitSet als : tScreening.getLimitSets())
			if (!als.isPrepared())
				als.prepareForExec();
		
		for(AlarmLimitSet als : tScreening.getLimitSets())
		{
			if (als.getSeason() == null) // This is the default (non-seasonal) limit set?
			{	
				tLimitSet = als;
			}
			else if (als.getSeason().isInSeason(t))
			{
				tLimitSet = als;
				break;
			}
		}
		if (tLimitSet == null)
		{
			info("Screening '" + tScreening.getScreeningName() + "' with id=" + tScreening.getScreeningId()
				+ " does not have a limit set for date/time=" + debugSdf.format(t));
			return false;
		}
		
		return true;
	}



//AW:LOCALVARS_END

//AW:OUTPUTS
	public NamedVariable output = new NamedVariable("output", 0);
	String _outputNames[] = { "output" };
//AW:OUTPUTS_END

//AW:PROPERTIES
	public boolean noOutputOnReject = false;
	public boolean noOverwrite = false;
	public boolean setInputFlags = false;
	public boolean setDataFlags = true;
	String _propertyNames[] = { "noOutputOnReject", "noOverwrite", "setInputFlags", "setDataFlags" };
//AW:PROPERTIES_END

	// Allow javac to generate a no-args constructor.
	
	

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	protected void initAWAlgorithm( )
		throws DbCompException
	{
//AW:INIT
		_awAlgoType = AWAlgoType.TIME_SLICE;
//AW:INIT_END

//AW:USERINIT
		// Code here will be run once, after the algorithm object is created.
//AW:USERINIT_END
	}
	
	/**
	 * This method is called once before iterating all time slices.
	 */
	protected void beforeTimeSlices()
		throws DbCompException
	{
//AW:BEFORE_TIMESLICES
		inputParm = getParmRef("input");
		TimeSeriesIdentifier inputTsid = getParmTsId("input");
		if (inputTsid == null)
			throw new DbCompException("'input' param has no TSID.");
		
		// Determine if input and output refer to the same time series.
		ParmRef outputParm = getParmRef("output");
		TimeSeriesIdentifier outputTsid = null;
		if (outputParm != null)
		{
			outputTsid = outputParm.timeSeries.getTimeSeriesIdentifier();
			if (outputTsid == null)
			{
				TimeSeriesDAI timeSeriesDAO = tsdb.makeTimeSeriesDAO();
				try
				{
					timeSeriesDAO.fillTimeSeriesMetadata(outputParm.timeSeries);
				}
				catch (Exception ex)
				{
					throw new DbCompException("No output tsid and can't retrieve: " + ex);
				}
				finally
				{
					timeSeriesDAO.close();
				}
				outputTsid = outputParm.timeSeries.getTimeSeriesIdentifier();
				if (outputTsid == null)
				{
					// Allow no output
					_noOutput = true;
					info("No output time-series -- will generate alarms "
						+ (setInputFlags ? "and set input flags." : "only."));
				}
			}
		}
		else
			_noOutput = true;

		_inputIsOutput = _noOutput || inputTsid.getKey() == outputTsid.getKey();
		info("_inputIsOutput=" + _inputIsOutput);

		// Find the first and last values in the time series that are triggers.
		earliestTrigger = null;
		for(int idx = 0; idx < inputParm.timeSeries.size(); idx++)
		{
			TimedVariable tv = inputParm.timeSeries.sampleAt(idx);
			if (VarFlags.wasAdded(tv))
			{
				if (earliestTrigger == null)
					earliestTrigger = tv.getTime();
				latestTrigger = tv.getTime();
			}
			else
				continue;
		}
		if (earliestTrigger == null)
			throw new DbCompException("triggered for input tsid '" + inputTsid.getUniqueString()
			+ "' but no input trigger values.");
		
		getAlarmScreenings(inputTsid); // will throw DbCompException if it fails.
		if (screenings.size() == 0)
			throw new DbCompException("no applicable screenings for tsid '" + inputTsid.getUniqueString()
				+ " between triggers " + debugSdf.format(earliestTrigger) + " and " + debugSdf.format(latestTrigger));
		
		debug1("Triggers: earliest=" + debugSdf.format(earliestTrigger) + ", latest=" + debugSdf.format(latestTrigger)
			+ ", retrieved " + screenings.size() + " screenings.");
		
		// Prefetch data needed for ROC and stuck sensor alarms.
debug3("beforeTimeSlices, expanding for ROC & Stuck Pre-Fetch...");
		Date fetchFrom = null;
		for(int idx = 0; idx < inputParm.timeSeries.size(); idx++)
		{
			TimedVariable tv = inputParm.timeSeries.sampleAt(idx);
			if (!VarFlags.wasAdded(tv))
				continue;
			
			if (!initScreeningAndLimitSet(tv.getTime()))
				continue;
debug3("\tFor t=" + debugSdf.format(tv.getTime()) + " getStuckDuration='" + tLimitSet.getStuckDuration()
+ "' hasRocLimits=" + tLimitSet.hasRocLimits());			
			if (tLimitSet.getStuckDuration() != null)
			{
				IntervalIncrement stuckDurII = IntervalIncrement.parse(tLimitSet.getStuckDuration());
				if (stuckDurII != null)
				{
					aggCal.setTime(tv.getTime());
					int count = stuckDurII.getCount();
					if (count > 0)
						count = -count;
					aggCal.add(stuckDurII.getCalConstant(), count);
					Date t = aggCal.getTime();
					if (fetchFrom == null || t.before(fetchFrom))
						fetchFrom = t;
				}
			}
			
			if (tLimitSet.hasRocLimits())
			{
				IntervalIncrement rocII = IntervalIncrement.parse(tLimitSet.getRocInterval());
				if (rocII != null)
				{
					aggCal.setTime(tv.getTime());
					int count = rocII.getCount();
					if (count > 0)
						count = -count;
					aggCal.add(rocII.getCalConstant(), count);
					Date t = aggCal.getTime();
					if (fetchFrom == null || t.before(fetchFrom))
						fetchFrom = t;
				}
				else
					warning("Unparsable ROC Interval '" + tLimitSet.getRocInterval() 
						+ "' -- no ROC limits can be checked.");
			}
		}
		
		// If we need historical data, fetch it, but don't overwrite existing data in the TS object.
		if (fetchFrom != null)
		{
			try
			{
				int n = tsdb.fillTimeSeries(inputParm.timeSeries, fetchFrom, latestTrigger, true, false, false);
				debug3("beforeTimeSlices: " + n + " values prefetched for ROC and stuck sensor checks.");
			}
			catch (Exception ex)
			{
				warning("Error filling time series '" + inputParm.tsid.getUniqueString()
					+ "' for time range " + debugSdf.format(fetchFrom) + " ... " 
					+ debugSdf.format(latestTrigger) + ": " + ex);
			}
		}
		
		// Note: It is up to the user to make sure input and output are in the correct units.
		
//AW:BEFORE_TIMESLICES_END
	}

	
	/**
	 * Do the algorithm for a single time slice.
	 * AW will fill in user-supplied code here.
	 * Base class will set inputs prior to calling this method.
	 * User code should call one of the setOutput methods for a time-slice
	 * output variable.
	 *
	 * @throws DbCompException (or subclass thereof) if execution of this
	 *        algorithm is to be aborted.
	 */
	protected void doAWTimeSlice()
		throws DbCompException
	{
//AW:TIMESLICE
		Date t = this._timeSliceBaseTime;
		
		if (!initScreeningAndLimitSet(t))
			return;
		
		debug1("Executing screening ' " + tScreening.getScreeningName() + "' with id=" 
			+ tScreening.getScreeningId() + " with limit set season="
			+ (tLimitSet.getSeason() == null ? "(default)" : tLimitSet.getSeason().getAbbr())
			+ " at time " + debugSdf.format(t) + " with value " + input);
		
		// NOTE: Alarm flag definitions are the same for HDB and OpenTSDB, so we use the HdbFlags
		// definitions here. After accumulating flag values, convert them to CWMS if necessary.
		int flags = HdbFlags.SCREENED;
		
		double UL = AlarmLimitSet.UNASSIGNED_LIMIT;
		
		// Check the absolute value limits
		if (tLimitSet.getRejectHigh() != UL && input >= tLimitSet.getRejectHigh())
			flags |= HdbFlags.SCR_VALUE_REJECT_HIGH;
		else if (tLimitSet.getCriticalHigh() != UL && input >= tLimitSet.getCriticalHigh())
			flags |= HdbFlags.SCR_VALUE_CRITICAL_HIGH;
		else if (tLimitSet.getWarningHigh() != UL && input >= tLimitSet.getWarningHigh())
			flags |= HdbFlags.SCR_VALUE_WARNING_HIGH;
		else if (tLimitSet.getRejectLow() != UL && input <= tLimitSet.getRejectLow())
			flags |= HdbFlags.SCR_VALUE_REJECT_LOW;
		else if (tLimitSet.getCriticalLow() != UL && input <= tLimitSet.getCriticalLow())
			flags |= HdbFlags.SCR_VALUE_CRITICAL_LOW;
		else if (tLimitSet.getWarningLow() != UL && input <= tLimitSet.getWarningLow())
			flags |= HdbFlags.SCR_VALUE_WARNING_LOW;
		
		double delta = 0.0;
		if (tLimitSet.getRocInterval() != null)
		{
			// Use the interval to determine an actual time period, then fetch time series
			// data if necessary and compute a delta.
			IntervalIncrement rocII = IntervalIncrement.parse(tLimitSet.getRocInterval());
			TimedVariable startOfPeriod = null;
debug1("ROC interval=" + tLimitSet.getRocInterval() + " parsed to "
+ (rocII==null ? "ERROR" : (""+rocII.getCount() + " const=" + rocII.getCalConstant())));

			if (rocII != null)
			{
				aggCal.setTime(t);
				int count = rocII.getCount();
				if (count > 0)
					count = -count;
				aggCal.add(rocII.getCalConstant(), count);
				Date from = aggCal.getTime();
				startOfPeriod = inputParm.timeSeries.findInterp(from.getTime()/1000L);
				double prev = 0.0;
				if (startOfPeriod != null)
				{
					try { delta = input - (prev=startOfPeriod.getDoubleValue()); }
					catch(Exception ex)
					{
						warning("Cannot do ROC check because startOfPeriod had non-numeric value: " 
							+ startOfPeriod + " - " + ex);
						startOfPeriod = null;
					}
				}
else debug1("\tvalue at start of period not found.");
debug1("ROC check prev=" + debugSdf.format(from) + " " + prev + ", delta=" + delta + ", high limits r/c/w="
+ tLimitSet.getRejectRocHigh() + "/" + tLimitSet.getCriticalRocHigh() + "/" + tLimitSet.getWarningRocHigh());
				if (startOfPeriod != null)
				{
					// Check the ROC limits
					if (tLimitSet.getRejectRocHigh() != UL && delta >= tLimitSet.getRejectRocHigh())
						flags |= HdbFlags.SCR_ROC_REJECT_HIGH;
					else if (tLimitSet.getCriticalRocHigh() != UL && delta >= tLimitSet.getCriticalRocHigh())
						flags |= HdbFlags.SCR_ROC_CRITICAL_HIGH;
					else if (tLimitSet.getWarningRocHigh() != UL && delta >= tLimitSet.getWarningRocHigh())
						flags |= HdbFlags.SCR_ROC_WARNING_HIGH;
					else if (tLimitSet.getRejectRocLow() != UL && delta <= tLimitSet.getRejectRocLow())
						flags |= HdbFlags.SCR_ROC_REJECT_LOW;
					else if (tLimitSet.getCriticalRocLow() != UL && delta <= tLimitSet.getCriticalRocLow())
						flags |= HdbFlags.SCR_ROC_CRITICAL_LOW;
					else if (tLimitSet.getWarningRocLow() != UL && delta <= tLimitSet.getWarningRocLow())
						flags |= HdbFlags.SCR_ROC_WARNING_LOW;
				}
			}
		}

		double variance = 0.0;
		if (tLimitSet.getStuckDuration() != null)
		{
			IntervalIncrement stuckDurII = IntervalIncrement.parse(tLimitSet.getStuckDuration());
			if (stuckDurII != null)
			{
				aggCal.setTime(t);
				int count = stuckDurII.getCount();
				if (count > 0)
					count = -count;
				aggCal.add(stuckDurII.getCalConstant(), count);
				Date from = aggCal.getTime();
				int n = 0;
				double lowv = 0.0, highv = 0.0;
				for(int idx = inputParm.timeSeries.findNextIdx(from); 
					idx != -1 && idx < inputParm.timeSeries.size(); idx++)
				{
					TimedVariable tv = inputParm.timeSeries.sampleAt(idx);
					if (tv.getTime().after(t))
						break;
					try
					{
						double v = tv.getDoubleValue();
						if (n++ == 0)
							lowv = highv = v;
						else
						{
							if (v < lowv)
								lowv = v;
							if (v > highv)
								highv = v;
						}
					}
					catch(Exception ex) {}
				}
				if (n > 1 && (variance = (highv - lowv)) <= tLimitSet.getStuckTolerance())
				{
					flags |= HdbFlags.SCR_STUCK_SENSOR_DETECTED;
				}
			}
		}
		
		checkAlarms(t, input, delta, variance, flags);
		
		
		// If one of:
		//   - property saying to set the input flags
		//   - The input and output are the same time series
		if ((setInputFlags || _inputIsOutput) && setDataFlags)
		{
			setInputFlagBits("input", flags, HdbFlags.SCREENING_MASK);
		}
		// Note: if (_noOutput && ! either of the above) then the comp doesn't
		// save the flags anywhere. The only purpose would be to generate alarms.
		
		// If there is an output that is different from the input
		if (!_noOutput && !_inputIsOutput)
		{
Logger.instance().info("output different from input, noOutputOnReject=" + noOutputOnReject 
+ ", flags=0x" + Integer.toHexString(flags) + ", isRejected=" + HdbFlags.isRejected(flags)
+ ", noOverwrite=" + noOverwrite);
			if (!(noOutputOnReject && HdbFlags.isRejected(flags)))
			{
				if (noOverwrite)
					flags |= VarFlags.NO_OVERWRITE; // 0x0c
				if (setDataFlags)
					output.setFlags(flags);
				setOutput(output, input);
			}
		}
		
//AW:TIMESLICE_END
	}

	private void checkAlarms(Date t, double value, double delta, double variance, int flags)
	{
		//  Hand the alarm assertion (or de-assertion) off to the singleton AlarmManager (TBD)
		//  AlarmManager maintains a queue of assertions.
		AlarmManager.instance(tsdb, comp.getAppId()).checkAlarms(
			inputParm.tsid, tLimitSet, tScreening, t, value, delta, variance, flags);
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
		throws DbCompException
	{
//AW:AFTER_TIMESLICES
		// This code will be executed once after each group of time slices.
		// For TimeSlice algorithms this is done once after all slices.
		// For Aggregating algorithms, this is done after each aggregate
		// period.
//AW:AFTER_TIMESLICES_END
	}

	/**
	 * Required method returns a list of all input time series names.
	 */
	public String[] getInputNames()
	{
		return _inputNames;
	}

	/**
	 * Required method returns a list of all output time series names.
	 */
	public String[] getOutputNames()
	{
		return _outputNames;
	}

	/**
	 * Required method returns a list of properties that have meaning to
	 * this algorithm.
	 */
	public String[] getPropertyNames()
	{
		return _propertyNames;
	}

	public AlarmLimitSet gettLimitSet()
	{
		return tLimitSet;
	}

	public AlarmScreening gettScreening()
	{
		return tScreening;
	}
	
}
