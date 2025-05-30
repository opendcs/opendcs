/**
 * $Id$
 * 
 * Copyright 2015 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * 
 * $Log$
 * Revision 1.9  2019/05/13 15:06:39  mmaloney
 * Fixed time zones in screening season selection.
 *
 * Revision 1.8  2019/04/22 13:51:41  mmaloney
 * Added debug
 *
 * Revision 1.7  2017/08/22 19:33:02  mmaloney
 * Improve comments
 *
 * Revision 1.6  2016/02/29 22:14:07  mmaloney
 * Removed nuisance debugs.
 *
 * Revision 1.5  2015/11/12 15:17:12  mmaloney
 * Added HEC headers.
 *
 */
package decodes.cwms.validation;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.TreeSet;

import opendcs.dai.TimeSeriesDAI;
import ilex.util.TextUtil;
import ilex.var.NamedVariable;
import ilex.var.Variable;
import decodes.tsdb.DbCompException;
import decodes.tsdb.IntervalCodes;
import decodes.tsdb.VarFlags;
import decodes.tsdb.algo.AWAlgoType;

import ilex.var.TimedVariable;
import decodes.tsdb.ParmRef;
import decodes.tsdb.IntervalIncrement;
import decodes.cwms.CwmsFlags;
import decodes.db.Site;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.util.TSUtil;
import org.opendcs.annotations.PropertySpec;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;

@Algorithm(description = "Base-class for screening algorithm.\n" +
"Implemented by DatchkScreeningAlgorithm and CwmsScreeningAlgorithm.")
public class ScreeningAlgorithm
	extends decodes.tsdb.algo.AW_AlgorithmBase
{
	@Input
	public double input;

	Screening screening = null;
	/** Must be overloaded by concrete class to find the screening. */
	protected Screening getScreening(TimeSeriesIdentifier tsid)
		throws DbCompException
	{
		return null;
	}
	
	boolean _inputIsOutput = false;
	public boolean inputIsOutput() { return _inputIsOutput; }
	

	@Output(type = Double.class)
	public NamedVariable output = new NamedVariable("output", 0);
	
	@PropertySpec(value = "false", description="(default=false) Set to true to disable overwriting of output parameter.")
	public boolean noOverwrite = false;
	@PropertySpec(value = "false", description="(default=false) Set to true to set quality flags on the input parameter.")
	public boolean setInputFlags = false;
	@PropertySpec(value = "false", description="(default=false) If true and the value is REJECTED, set the output flags to MISSING.")
	public boolean setRejectMissing = false;
	@PropertySpec(value = "false", description="(default=false) If true and the value is REJECTED, then do not write output param at all.\n" +
	"Warning: This may leave a previous value for the output param at that time slice unchanged.")
	public boolean noOutputOnReject = false;

	// Allow javac to generate a no-args constructor.

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	protected void initAWAlgorithm( )
		throws DbCompException
	{
		_awAlgoType = AWAlgoType.TIME_SLICE;
	}
	
	/**
	 * This method is called once before iterating all time slices.
	 */
	protected void beforeTimeSlices()
		throws DbCompException
	{
		// This code will be executed once before iterating through all the time-slices
		
		// Find the Screening record
		ParmRef inputParm = getParmRef("input");
		TimeSeriesIdentifier inputTsid = inputParm.timeSeries.getTimeSeriesIdentifier();
		if (inputTsid == null)
			throw new DbCompException("No input time-series identifier!");
		
		// MJM 20121002 Enhancement: Allow input and output to refer to the same
		// time-series. This triggers special processing in the checks.
		ParmRef outputParm = getParmRef("output");
		TimeSeriesIdentifier outputTsid = outputParm.timeSeries.getTimeSeriesIdentifier();
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
				throw new DbCompException("No output time-series identifier!");
		}

		_inputIsOutput = inputTsid.getKey() == outputTsid.getKey();
		info("_inputIsOutput=" + _inputIsOutput);
		
		screening = getScreening(inputTsid);
		if (screening == null)
		{
			warning("No screening defined for " + inputTsid.getUniqueString());
			return;
		}
		
		// Using the tests, determine the amount of past-data needed at each time-slice.
		TreeSet<Date> needed = new TreeSet<Date>();
		IntervalIncrement tsinc = IntervalCodes.getIntervalCalIncr(inputTsid.getInterval());
		boolean inputIrregular = tsinc == null || tsinc.getCount() == 0;

		debug3("Retrieving additional data needed for checks.");
		ScreeningCriteria prevcrit = null;
		for(int idx = 0; idx<inputParm.timeSeries.size(); idx++)
		{
			TimedVariable tv = inputParm.timeSeries.sampleAt(idx);
			if (VarFlags.wasAdded(tv))
			{
				ScreeningCriteria crit = screening.findForDate(tv.getTime(), aggTZ);
				Site site = inputTsid.getSite();
				if (site != null && site.timeZoneAbbr != null && site.timeZoneAbbr.length() > 0)
				{
					TimeZone tz = TimeZone.getTimeZone(site.timeZoneAbbr);
					screening.setSeasonTimeZone(tz);
				}
				if (crit == null || crit == prevcrit)
					continue;
				crit.fillTimesNeeded(inputParm.timeSeries, needed, aggCal, this);
				prevcrit = crit;
			}
		}
		debug3("additional data done, #times needed=" + needed.size());
		
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
				throw new DbCompException(ex.toString());
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
			
			// Also, if there is an output, make sure its units match the input.
			setOutputUnitsAbbr("output", euAbbr);
		}
		
		if (screening.getCriteriaSeasons() != null)
		{
			debug1("There are " + screening.getCriteriaSeasons().size() + " seasons in the screening:");
			for(ScreeningCriteria sc : screening.getCriteriaSeasons())
			{
				debug1("    " + 
					(sc.getSeasonStart() == null ? "<all year>" : 
						(sc.getSeasonStart().get(Calendar.MONTH) + "/" 
						 + sc.getSeasonStart().get(Calendar.DAY_OF_MONTH) + " " 
						 + sc.getSeasonStart().getTimeZone().getID())));
			}
		}
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
		ScreeningCriteria crit = 
			screening != null ? screening.findForDate(_timeSliceBaseTime, aggTZ) : null;
		if (crit == null)
		{
			warning("No criteria for time=" + debugSdf.format(_timeSliceBaseTime));
			// Treat no criteria the same as a screening where everything passes.
			if (inputIsOutput())
			{
				Variable inputV = _timeSliceVars.findByNameIgnoreCase("input");
				int flags = inputV.getFlags();
				if ((flags & CwmsFlags.PROTECTED) != 0)
					return;
				flags &= (~(CwmsFlags.VALIDITY_MASK | CwmsFlags.TEST_MASK));
				flags |= (CwmsFlags.SCREENED | CwmsFlags.VALIDITY_OKAY);
				info("input=output, input flags=0x" 
					+ Integer.toHexString(inputV.getFlags()) + ", result flags=0x"
					+ Integer.toHexString(flags));
				if (flags == inputV.getFlags())
					return; // No changes to flags. Do not write output.
			}
			output.setValue(input);
			clearFlagBits(output, CwmsFlags.VALIDITY_MASK | CwmsFlags.TEST_MASK);
			setFlagBits(output, CwmsFlags.SCREENED | CwmsFlags.VALIDITY_OKAY);
			info("Writing output flags=0x" + Integer.toHexString(output.getFlags()));
			return;
		}
		
		debug1("Time Slice " + debugSdf.format(_timeSliceBaseTime) + " selected screening season start="
			+ (crit.getSeasonStart() == null ? "<all year>" : (crit.getSeasonStart().get(Calendar.MONTH) + "/" 
			 + crit.getSeasonStart().get(Calendar.DAY_OF_MONTH) + " " 
			 + crit.getSeasonStart().getTimeZone().getID())));
		
		ParmRef inputParm = getParmRef("input");

		crit.executeChecks(dc, inputParm.timeSeries, _timeSliceBaseTime, output, this);
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
		throws DbCompException
	{
		// This code will be executed once after each group of time slices.
		// For TimeSlice algorithms this is done once after all slices.
		// For Aggregating algorithms, this is done after each aggregate
		// period.
	}
}