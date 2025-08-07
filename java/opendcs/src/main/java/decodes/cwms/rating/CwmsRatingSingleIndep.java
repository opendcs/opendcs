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
package decodes.cwms.rating;

import java.io.PrintStream;
import java.sql.Connection;
import java.util.Date;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.var.NamedVariable;
import decodes.cwms.CwmsTimeSeriesDb;
import decodes.db.Constants;
import decodes.db.SiteName;
import decodes.tsdb.DbCompException;
import decodes.tsdb.algo.AWAlgoType;
import decodes.tsdb.ParmRef;
//AW:IMPORTS
import hec.data.RatingException;
import hec.data.cwmsRating.RatingSet;
import hec.lang.Const;

import java.util.ArrayList;

import decodes.tsdb.TimeSeriesIdentifier;
//AW:IMPORTS_END
import decodes.util.TSUtil;

//AW:JAVADOC
/**
Implements CWMS rating computations.
Uses the CWMS API provided by HEC to do the rating.
*/
//AW:JAVADOC_END
public class CwmsRatingSingleIndep extends decodes.tsdb.algo.AW_AlgorithmBase
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
	public boolean useDepLocation = false;
	public String _propertyNames[] = { "templateVersion", "specVersion", "useDepLocation" };
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
		// {CwmsLocID}.{indep:cwmsDataType};{dep:cwmsDataType}.{templateVersion}.{specVersion}
		TimeSeriesIdentifier indepTsid = indepParmRef.timeSeries.getTimeSeriesIdentifier();

		String specLocation = indepTsid.getSiteName();
		// OpenDCS 6.1 RC10 feature allows location to be taken from dep variable:
		if (useDepLocation)
		{
			SiteName depSiteName = depParmRef.compParm.getSiteName(Constants.snt_CWMS);
			if (depSiteName == null)
				log.warn("No dependent site name available, using site from indep1: '{}'", specLocation);
			else
				specLocation = depSiteName.getNameValue();
		}

		specId = specLocation + "."
			+ indepTsid.getDataType().getCode() + ";"
			+ depParmRef.compParm.getDataType().getCode() + "."
			+ templateVersion + "." + specVersion;

		// Retrieve the RatingSet object
		CwmsRatingDao crd = new CwmsRatingDao((CwmsTimeSeriesDb)tsdb);
		try
		{
			Date earliestBaseTime = baseTimes.first();
//debug3("Will do rating for the following " + baseTimes.size() + " base times:");
//for(Date baseTime : baseTimes)
//{
//	debug3("    " + baseTime.toString());
//}
			if (earliestBaseTime == null)
				earliestBaseTime = new Date();
			ratingSet = crd.getRatingSet(specId);

			// As per instructions from Mike Perryman: I must find out what the native units
			// for the rating are and convert before calling the Java rating. The Java rating
			// method assumes that data are already in correct units.
			String punits[] = ratingSet.getDataUnits();
			if (punits.length > 0 && punits[0] != null
			 && indepParmRef.timeSeries.getUnitsAbbr() != null
			 && !indepParmRef.timeSeries.getUnitsAbbr().equalsIgnoreCase(punits[0]))
			{
				log.debug("Converting indep units for time series {} from {} to {}.",
				          indepParmRef.timeSeries.getTimeSeriesIdentifier().getUniqueString(), indepParmRef.timeSeries.getUnitsAbbr(), punits[0]);
				TSUtil.convertUnits(indepParmRef.timeSeries, punits[0]);
			}
			// Likewise for the dependent param:
			if (punits.length > 1 && punits[1] != null
			 && depParmRef.timeSeries.getUnitsAbbr() != null
			 && !depParmRef.timeSeries.getUnitsAbbr().equalsIgnoreCase(punits[1]))
			{
				log.debug("depTSID={}", depParmRef.timeSeries.getTimeSeriesIdentifier());
				log.debug(" Converting dep units from {} to {}.",depParmRef.timeSeries.getUnitsAbbr(), punits[1]);
				TSUtil.convertUnits(depParmRef.timeSeries, punits[1]);
			}
		}
		catch (RatingException ex)
		{
			throw new DbCompException("Cannot read rating for '" + specId + "'", ex);
		}
		finally
		{
			crd.close();
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

		// Note:
		// There is a 'method' variable in each RatingSpec in the database that controls
		// the rating behavior independently for above range, below range, and within range
		// but no exact matching value. E.g.:
		//	<in-range-method>LINEAR</in-range-method>
		//	<out-range-low-method>ERROR</out-range-low-method>
		//	<out-range-high-method>ERROR</out-range-high-method>
		// Consequently, always use the array type rating method and let the database
		// determine what happens.
		// If NULL is the method then the dep output wil be set to Const.UNDEFINED_DOUBLE


		try (Connection conn = tsdb.getConnection();)
		{
			log.debug("Calling rate with {} times/values", times.length);
			double depVals[] = ratingSet.rate(conn, times, vals);
					//ratingSet.rate(times, vals);

			for(int i=0; i<times.length; i++)
			{
				if (depVals[i] != Const.UNDEFINED_DOUBLE)
					setOutput(dep, depVals[i], new Date(times[i]));
				else
					log.warn("Value {} at time {} could not be rated"
					        +"(most likely reason is that it is outside table bounds.)",
							vals[i], new Date(times[i]));
			}
		}
		catch(Exception ex)
		{
			log.atWarn().setCause(ex).log("Rating failure.");
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
