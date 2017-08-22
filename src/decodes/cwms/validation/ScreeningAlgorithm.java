/**
 * $Id$
 * 
 * Copyright 2015 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * 
 * $Log$
 * Revision 1.6  2016/02/29 22:14:07  mmaloney
 * Removed nuisance debugs.
 *
 * Revision 1.5  2015/11/12 15:17:12  mmaloney
 * Added HEC headers.
 *
 */
package decodes.cwms.validation;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;

import opendcs.dai.TimeSeriesDAI;
import ilex.util.Logger;
import ilex.util.TextUtil;
import ilex.var.NamedVariableList;
import ilex.var.NamedVariable;
import ilex.var.Variable;
import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DbAlgorithmExecutive;
import decodes.tsdb.DbCompException;
import decodes.tsdb.DbIoException;
import decodes.tsdb.IntervalCodes;
import decodes.tsdb.VarFlags;
import decodes.tsdb.algo.AWAlgoType;

//AW:IMPORTS
import ilex.var.TimedVariable;
import decodes.tsdb.ParmRef;
import decodes.tsdb.IntervalIncrement;
import decodes.cwms.CwmsFlags;
import decodes.cwms.validation.Screening;
import decodes.db.Site;
import decodes.tsdb.TimeSeriesIdentifier;
//AW:IMPORTS_END
import decodes.util.PropertySpec;
import decodes.util.TSUtil;

//AW:JAVADOC
/**
Base-class for screening algorithm.
Implemented by DatchkScreeningAlgorithm and CwmsScreeningAlgorithm.

 */
//AW:JAVADOC_END
public class ScreeningAlgorithm
	extends decodes.tsdb.algo.AW_AlgorithmBase
{
//AW:INPUTS
	public double input;	//AW:TYPECODE=i
	String _inputNames[] = { "input" };
//AW:INPUTS_END

//AW:LOCALVARS
	Screening screening = null;
	/** Must be overloaded by concrete class to find the screening. */
	protected Screening getScreening(TimeSeriesIdentifier tsid)
		throws DbCompException
	{
		return null;
	}
	boolean _inputIsOutput = false;
	public boolean inputIsOutput() { return _inputIsOutput; }
	
	PropertySpec algoPropSpecs[] =
	{
		new PropertySpec("noOverwrite", PropertySpec.BOOLEAN, "(default=false) "
			+ "Set to true to disable overwriting of output parameter."),
		new PropertySpec("setInputFlags", PropertySpec.BOOLEAN, "(default=false) "
			+ "Set to true to set quality flags on the input parameter."),
		new PropertySpec("setRejectMissing", PropertySpec.BOOLEAN, "(default=false) "
			+ "If true and the value is REJECTED, set the output flags to MISSING."),
		new PropertySpec("noOutputOnReject", PropertySpec.BOOLEAN, "(default=false) "
			+ "If true and the value is REJECTED, then do not write output param at all. "
			+ "Warning: This may leave a previous value for the output param at that time slice "
			+ "unchanged.")
	};
	
	@Override
	protected PropertySpec[] getAlgoPropertySpecs()
	{
		return algoPropSpecs;
	}


//AW:LOCALVARS_END

//AW:OUTPUTS
	public NamedVariable output = new NamedVariable("output", 0);
	String _outputNames[] = { "output" };
//AW:OUTPUTS_END

//AW:PROPERTIES
	public boolean noOverwrite = false;
	public boolean setInputFlags = false;
	public boolean setRejectMissing = false;
	public boolean noOutputOnReject = false;
	String _propertyNames[] = { "noOverwrite", "setInputFlags", "setRejectMissing",
		"noOutputOnReject" };
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
//AW:USERINIT_END
	}
	
	/**
	 * This method is called once before iterating all time slices.
	 */
	protected void beforeTimeSlices()
		throws DbCompException
	{
//AW:BEFORE_TIMESLICES
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
				ScreeningCriteria crit = screening.findForDate(tv.getTime());
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
		ScreeningCriteria crit = 
			screening != null ? screening.findForDate(_timeSliceBaseTime) : null;
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
		ParmRef inputParm = getParmRef("input");

		crit.executeChecks(dc, inputParm.timeSeries, _timeSliceBaseTime, output, this);
		
//AW:TIMESLICE_END
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
}
