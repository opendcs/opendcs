/**
 * $Id$
 * 
 * $Log$
 * Revision 1.4  2015/01/05 21:04:39  mmaloney
 * Automatically convert units to the dataUnits reported by the RatingSet.
 *
 * Revision 1.3  2014/12/23 14:15:57  mmaloney
 * Debug to print input and output to/from HEC Rating method.
 *
 * Revision 1.2  2014/12/18 21:52:21  mmaloney
 * In error messages, print the specId.
 *
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 * Revision 1.7  2013/04/25 20:46:07  mmaloney
 * Fixed error message.
 *
 * Revision 1.6  2012/11/20 21:17:18  mmaloney
 * Implemented cache for ratings.
 *
 * Revision 1.5  2012/11/12 19:53:03  mmaloney
 * dev
 *
 * Revision 1.4  2012/11/12 19:36:04  mmaloney
 * Use version of method that passes officeID.
 * The one without office ID always returns a RatingSpec with no Ratings in it.
 *
 * Revision 1.3  2012/11/09 21:50:24  mmaloney
 * fixed init
 *
 * Revision 1.2  2012/11/09 21:10:42  mmaloney
 * Fixed imports.
 *
 * Revision 1.1  2012/11/09 21:06:20  mmaloney
 * Checked in Rating Algorithms.
 *
 *
 */
package decodes.cwms.rating;

import java.util.Date;

import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.var.NamedVariableList;
import ilex.var.NamedVariable;
import decodes.cwms.CwmsTimeSeriesDb;
import decodes.tsdb.DbAlgorithmExecutive;
import decodes.tsdb.DbCompException;
import decodes.tsdb.DbIoException;
import decodes.tsdb.TimeSeriesHelper;
import decodes.tsdb.VarFlags;
import decodes.tsdb.algo.AWAlgoType;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.ParmRef;
import ilex.var.TimedVariable;


//AW:IMPORTS
import hec.data.RatingException;
import hec.data.cwmsRating.RatingSet;

import java.util.ArrayList;

import decodes.tsdb.TimeSeriesIdentifier;
//AW:IMPORTS_END

//AW:JAVADOC
/**
Implements CWMS rating computations.
Uses the CWMS API provided by HEC to do the rating.
*/
//AW:JAVADOC_END
public class CwmsRatingSingleIndep
	extends decodes.tsdb.algo.AW_AlgorithmBase
{
//AW:INPUTS
	public double indep;	//AW:TYPECODE=i
	String _inputNames[] = { "indep" };
//AW:INPUTS_END

//AW:LOCALVARS
	RatingSet ratingSet = null;
	Date beginTime = null;
	Date endTime = null;
	ArrayList<Long> indepTimes = new ArrayList<Long>();
	ArrayList<Double> indepValues = new ArrayList<Double>();
	String specId = "";
	public static final String module = "CwmsRatingSingleIndep";
//AW:LOCALVARS_END

//AW:OUTPUTS
	public NamedVariable dep = new NamedVariable("dep", 0);
	String _outputNames[] = { "dep" };
//AW:OUTPUTS_END

//AW:PROPERTIES
	public String templateVersion = "USGS-EXSA";
	public String specVersion = "Production";
	public String _propertyNames[] = { "templateVersion", "specVersion" };
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
		// Get parm refs for indep and dep
		ParmRef indepParmRef = getParmRef("indep");
		ParmRef depParmRef = getParmRef("dep");
		
		// Build rating spec ID as follows:
		// {indep:CwmsLocID}.{indep:cwmsDataType};{dep:cwmsDataType}.{templateVersion}.{specVersion}
		TimeSeriesIdentifier indepTsid = indepParmRef.timeSeries.getTimeSeriesIdentifier();
		
		specId = indepTsid.getSiteName() + "." 
			+ indepTsid.getDataType().getCode() + ";"
			+ depParmRef.compParm.getDataType().getCode() + "."
			+ templateVersion + "." + specVersion;
			
		// Retrieve the RatingSet object
		try
		{
			CwmsRatingDao crd = new CwmsRatingDao((CwmsTimeSeriesDb)tsdb);
			ratingSet = crd.getRatingSet(specId);
			
			// As per instructions from Mike Perryman: I must find out what the native units
			// for the rating are and convert before calling the Java rating. The Java rating
			// method assumes that data are already in correct units.
			String punits[] = ratingSet.getDataUnits();
			if (punits.length > 0 && punits[0] != null
			 && indepParmRef.timeSeries.getUnitsAbbr() != null
			 && !indepParmRef.timeSeries.getUnitsAbbr().equalsIgnoreCase(punits[0]))
			{
				debug1(module + " Converting indep units for time series " 
					+ indepParmRef.timeSeries.getTimeSeriesIdentifier().getUniqueString() + " from "
					+ indepParmRef.timeSeries.getUnitsAbbr() + " to " + punits[0]);
				TimeSeriesHelper.convertUnits(indepParmRef.timeSeries, punits[0]);
			}
			// Likewise for the dependent param:
			if (punits.length > 1 && punits[1] != null
			 && depParmRef.timeSeries.getUnitsAbbr() != null
			 && !depParmRef.timeSeries.getUnitsAbbr().equalsIgnoreCase(punits[1]))
			{
debug1(module + " depTSID=" + depParmRef.timeSeries.getTimeSeriesIdentifier());
				debug1(module + " Converting dep units for time series " 
					+ depParmRef.timeSeries.getTimeSeriesIdentifier().getUniqueString() + " from "
					+ depParmRef.timeSeries.getUnitsAbbr() + " to " + punits[1]);
				TimeSeriesHelper.convertUnits(depParmRef.timeSeries, punits[1]);
			}
		}
		catch (RatingException ex)
		{
			throw new DbCompException("Cannot read rating for '" + specId + "': " + ex);
		}

		indepTimes.clear();
		indepValues.clear();
//AW:BEFORE_TIMESLICES_END
	}

	/**
	 * Do the algorithm for a single time slice.
	 * AW will fill in user-supplied code here.
	 * Base class will set inputs prior to calling this method.
	 * User code should call one of the setOutput methods for a time-slice
	 * output variable.
	 *
	 * @throw DbCompException (or subclass thereof) if execution of this
	 *        algorithm is to be aborted.
	 */
	protected void doAWTimeSlice()
		throws DbCompException
	{
//AW:TIMESLICE
		if (ratingSet == null)
			throw new DbCompException("No rating set!");
		
		// Just collect the times & values. We do the rating after Time Slices.
		indepTimes.add(_timeSliceBaseTime.getTime());
		indepValues.add(indep);
//AW:TIMESLICE_END
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
	{
//AW:AFTER_TIMESLICES
		long []times = new long[indepTimes.size()];
		double []vals = new double[indepTimes.size()];
		for(int i=0; i<times.length; i++)
		{
			times[i] = indepTimes.get(i);
			vals[i] = indepValues.get(i);
		}

//		try
//		{
//			debug1("Calling rate with " + times.length + " times/values");
//			double depVals[] = ratingSet.rate(times, vals);
//			
//			for(int i=0; i<times.length; i++)
//				setOutput(dep, depVals[i], new Date(times[i]));
//		}
//		catch(Exception ex)
//		{
//			String msg = "Rating failure: " + ex;
//			warning(msg);
//		}
		
		// The rate method will arrays will throw RatingException if ANY
		// values are out of range and produce no result. Therefore I must
		// rate each value individually.
		for(int i=0; i<times.length; i++)
		{
			Date d = new Date(times[i]);
			try
			{
				double ratedValue = ratingSet.rate(vals[i], times[i]);
debug3("RatingSet.rate: input=" + vals[i] + ", output=" + ratedValue + ", at time " + debugSdf.format(d));
				setOutput(dep, ratedValue, d);
			}
			catch(RatingException ex)
			{
				String msg = "RatingException for spec '" + specId + "' value " + vals[i]
					+ " at time " + debugSdf.format(d) + ": " + ex;
				warning(msg);
			}
		}
		
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
