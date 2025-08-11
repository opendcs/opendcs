/*
* Copyright 2014 Cove Software, LLC
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
public class CwmsRatingMultIndep extends decodes.tsdb.algo.AW_AlgorithmBase
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
//AW:INPUTS
	public double indep1;	//AW:TYPECODE=i
	public double indep2;	//AW:TYPECODE=i
	public double indep3;	//AW:TYPECODE=i
	public double indep4;	//AW:TYPECODE=i
	public double indep5;	//AW:TYPECODE=i
	public double indep6;	//AW:TYPECODE=i
	public double indep7;	//AW:TYPECODE=i
	public double indep8;	//AW:TYPECODE=i
	public double indep9;	//AW:TYPECODE=i

	String _inputNames[] = { "indep1", "indep2", "indep3", "indep4", "indep5",
		"indep6", "indep7", "indep8", "indep9" };
//AW:INPUTS_END

//AW:LOCALVARS
	public static final String module = "CwmsRatingMultIndep";
	RatingSet ratingSet = null;
	Date beginTime = null;
	Date endTime = null;
	ArrayList<Long> indepTimes = new ArrayList<Long>();
//	ArrayList<Double> indep1Values = new ArrayList<Double>();
//	ArrayList<Double> indep2Values = null;
//	ArrayList<Double> indep3Values = null;
//	ArrayList<Double> indep4Values = null;
//	ArrayList<Double> indep5Values = null;
//	ArrayList<Double> indep6Values = null;
//	ArrayList<Double> indep7Values = null;
//	ArrayList<Double> indep8Values = null;
//	ArrayList<Double> indep9Values = null;
	int numIndeps = 1;
	String specId = "";
	String indep1SiteName = null;

	private ArrayList<ArrayList<Double>> valueSetsA = null;


	private String buildIndepSpec()
		throws DbCompException
	{
		ParmRef parmRef = getParmRef("indep1");
		if (parmRef == null)
			throw new DbCompException("No time series mapped to indep1");
		TimeSeriesIdentifier tsid = parmRef.timeSeries.getTimeSeriesIdentifier();
		if (tsid == null)
			throw new DbCompException("No time series identifier associated with indep1");

		indep1SiteName = tsid.getSiteName();
		String indepSpecId = tsid.getDataType().getCode();

		parmRef = getParmRef("indep2");
		if (parmRef == null || parmRef.timeSeries == null
		 || parmRef.timeSeries.getTimeSeriesIdentifier() == null)
		{
			if (!indep2_MISSING.equalsIgnoreCase("ignore"))
				throw new DbCompException("No time series mapped to indep2");

			return indepSpecId;
		}
		tsid = parmRef.timeSeries.getTimeSeriesIdentifier();
		indepSpecId = indepSpecId + "," + tsid.getDataType().getCode();
		numIndeps = 2;

		parmRef = getParmRef("indep3");
		if (parmRef == null || parmRef.timeSeries == null
		 || parmRef.timeSeries.getTimeSeriesIdentifier() == null)
		{
			if (!indep3_MISSING.equalsIgnoreCase("ignore"))
				throw new DbCompException("No time series mapped to indep3");
			return indepSpecId;
		}
		tsid = parmRef.timeSeries.getTimeSeriesIdentifier();
		indepSpecId = indepSpecId + "," + tsid.getDataType().getCode();
		numIndeps = 3;

		parmRef = getParmRef("indep4");
		if (parmRef == null || parmRef.timeSeries == null
		 || parmRef.timeSeries.getTimeSeriesIdentifier() == null)
		{
			if (!indep4_MISSING.equalsIgnoreCase("ignore"))
				throw new DbCompException("No time series mapped to indep4");

			return indepSpecId;
		}
		tsid = parmRef.timeSeries.getTimeSeriesIdentifier();
		indepSpecId = indepSpecId + "," + tsid.getDataType().getCode();
		numIndeps = 4;

		parmRef = getParmRef("indep5");
		if (parmRef == null || parmRef.timeSeries == null
		 || parmRef.timeSeries.getTimeSeriesIdentifier() == null)
		{
			if (!indep5_MISSING.equalsIgnoreCase("ignore"))
				throw new DbCompException("No time series mapped to indep5");

			return indepSpecId;
		}
		tsid = parmRef.timeSeries.getTimeSeriesIdentifier();
		indepSpecId = indepSpecId + "," + tsid.getDataType().getCode();
		numIndeps = 5;

		parmRef = getParmRef("indep6");
		if (parmRef == null || parmRef.timeSeries == null
		 || parmRef.timeSeries.getTimeSeriesIdentifier() == null)
		{
			if (!indep6_MISSING.equalsIgnoreCase("ignore"))
				throw new DbCompException("No time series mapped to indep6");

			return indepSpecId;
		}
		tsid = parmRef.timeSeries.getTimeSeriesIdentifier();
		indepSpecId = indepSpecId + "," + tsid.getDataType().getCode();
		numIndeps = 6;

		parmRef = getParmRef("indep7");
		if (parmRef == null || parmRef.timeSeries == null
		 || parmRef.timeSeries.getTimeSeriesIdentifier() == null)
		{
			if (!indep7_MISSING.equalsIgnoreCase("ignore"))
				throw new DbCompException("No time series mapped to indep7");

			return indepSpecId;
		}
		tsid = parmRef.timeSeries.getTimeSeriesIdentifier();
		indepSpecId = indepSpecId + "," + tsid.getDataType().getCode();
		numIndeps = 7;

		parmRef = getParmRef("indep8");
		if (parmRef == null || parmRef.timeSeries == null
		 || parmRef.timeSeries.getTimeSeriesIdentifier() == null)
		{
			if (!indep8_MISSING.equalsIgnoreCase("ignore"))
				throw new DbCompException("No time series mapped to indep8");

			return indepSpecId;
		}
		tsid = parmRef.timeSeries.getTimeSeriesIdentifier();
		indepSpecId = indepSpecId + "," + tsid.getDataType().getCode();
		numIndeps = 8;

		parmRef = getParmRef("indep9");
		if (parmRef == null || parmRef.timeSeries == null
		 || parmRef.timeSeries.getTimeSeriesIdentifier() == null)
		{
			if (!indep9_MISSING.equalsIgnoreCase("ignore"))
				throw new DbCompException("No time series mapped to indep9");

			return indepSpecId;
		}
		tsid = parmRef.timeSeries.getTimeSeriesIdentifier();
		indepSpecId = indepSpecId + "," + tsid.getDataType().getCode();
		numIndeps = 9;

		return indepSpecId;
	}
//AW:LOCALVARS_END

//AW:OUTPUTS
	public NamedVariable dep = new NamedVariable("dep", 0);
	String _outputNames[] = { "dep" };
//AW:OUTPUTS_END

//AW:PROPERTIES
	public String templateVersion = "USGS-EXSA";
	public String specVersion = "Production";
	public String indep1_MISSING = "fail";
	public String indep2_MISSING = "ignore";
	public String indep3_MISSING = "ignore";
	public String indep4_MISSING = "ignore";
	public String indep5_MISSING = "ignore";
	public String indep6_MISSING = "ignore";
	public String indep7_MISSING = "ignore";
	public String indep8_MISSING = "ignore";
	public String indep9_MISSING = "ignore";
	public boolean useDepLocation = false;
	public String locationOverride = "";

	public String _propertyNames[] = { "templateVersion", "specVersion",
		"indep1_MISSING", "indep2_MISSING", "indep3_MISSING", "indep4_MISSING", "indep5_MISSING",
		"indep6_MISSING", "indep7_MISSING", "indep8_MISSING", "indep9_MISSING",
		"useDepLocation", "locationOverride" };
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
		specId = buildIndepSpec();

		ParmRef depParmRef = getParmRef("dep");
		String specLocation = indep1SiteName;
		if (useDepLocation)
		{
			SiteName depSiteName = depParmRef.compParm.getSiteName(Constants.snt_CWMS);
			if (depSiteName == null)
				log.warn("No dependent site name available, using site from indep1: '{}'", specLocation);
			else
				specLocation = depSiteName.getNameValue();
		}

		if (locationOverride != null && locationOverride.length() > 0)
		{
			if (locationOverride.contains("*"))
				specLocation = CwmsTimeSeriesDb.morph(specLocation, locationOverride);
			else
				specLocation = locationOverride;
		}

		specId = specLocation
			+ "." + specId + ";" + depParmRef.compParm.getDataType().getCode() + "."
			+ templateVersion + "." + specVersion;

		// Retrieve the RatingSet object
		try (CwmsRatingDao crd = new CwmsRatingDao((CwmsTimeSeriesDb)tsdb))
		{
			Date earliestBaseTime = baseTimes.first();
			if (earliestBaseTime == null)
				earliestBaseTime = new Date();
			ratingSet = crd.getRatingSet(specId);

			// As per instructions from Mike Perryman: I must find out what the native units
			// for the rating are and convert before calling the Java rating. The Java rating
			// method assumes that data are already in correct units.
			String punits[] = ratingSet.getDataUnits();
			for(int pidx = 0; pidx < punits.length-1 && pidx < 9; pidx++)
			{
				if (punits[pidx] == null)
					continue;
				String pname = "indep" + (pidx+1);
				ParmRef indepParmRef = getParmRef(pname);
				if (indepParmRef == null || indepParmRef.timeSeries == null
				 || indepParmRef.timeSeries.getUnitsAbbr() == null)
					continue;

				if (!indepParmRef.timeSeries.getUnitsAbbr().equalsIgnoreCase(punits[pidx]))
				{
					log.debug("Converting {} units for time series {} from {} to {}",
							  pname, indepParmRef.timeSeries.getTimeSeriesIdentifier().getUniqueString(),
							  indepParmRef.timeSeries.getUnitsAbbr(), punits[pidx]);
					TSUtil.convertUnits(indepParmRef.timeSeries, punits[pidx]);
				}
			}

			// Likewise for the dependent param:
			if (punits.length > 1 && punits[punits.length-1] != null
			 && depParmRef.timeSeries.getUnitsAbbr() != null
			 && !depParmRef.timeSeries.getUnitsAbbr().equalsIgnoreCase(punits[punits.length-1]))
			{
				log.debug("Converting dep units from {} to {}.",
						  depParmRef.timeSeries.getUnitsAbbr(), punits[punits.length-1]);
				TSUtil.convertUnits(depParmRef.timeSeries, punits[punits.length-1]);
			}

		}
		catch (RatingException ex)
		{
			String m = "Cannot read rating for '" + specId + "'";
			throw new DbCompException(m, ex);
		}

		indepTimes.clear();

		// MJM 2017 10/31 Array list of indeps, for each indep and arraylist of values.
		valueSetsA = new ArrayList<ArrayList<Double>>();
		for(int idx = 0; idx < numIndeps; idx++)
			valueSetsA.add(new ArrayList<Double>());



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

		// If any non-ignored params are missing in this time-slice, skip it.
		if ((isMissing("indep1") && indep1_MISSING.equalsIgnoreCase("fail"))
		 || (numIndeps >= 2 && isMissing("indep2") && indep2_MISSING.equalsIgnoreCase("fail"))
		 || (numIndeps >= 3 && isMissing("indep3") && indep3_MISSING.equalsIgnoreCase("fail"))
		 || (numIndeps >= 4 && isMissing("indep4") && indep4_MISSING.equalsIgnoreCase("fail"))
		 || (numIndeps >= 5 && isMissing("indep5") && indep5_MISSING.equalsIgnoreCase("fail"))
		 || (numIndeps >= 6 && isMissing("indep6") && indep6_MISSING.equalsIgnoreCase("fail"))
		 || (numIndeps >= 7 && isMissing("indep7") && indep7_MISSING.equalsIgnoreCase("fail"))
		 || (numIndeps >= 8 && isMissing("indep8") && indep8_MISSING.equalsIgnoreCase("fail"))
		 || (numIndeps >= 9 && isMissing("indep9") && indep9_MISSING.equalsIgnoreCase("fail")))
			return;

		// MJM 10/31/2017 Modified to save values and do array rating in after method.
		indepTimes.add(_timeSliceBaseTime.getTime());
		valueSetsA.get(0).add(indep1);
		if (numIndeps >= 2) valueSetsA.get(1).add(indep2);
		if (numIndeps >= 3) valueSetsA.get(2).add(indep3);
		if (numIndeps >= 4) valueSetsA.get(3).add(indep4);
		if (numIndeps >= 5) valueSetsA.get(4).add(indep5);
		if (numIndeps >= 6) valueSetsA.get(5).add(indep6);
		if (numIndeps >= 7) valueSetsA.get(6).add(indep7);
		if (numIndeps >= 8) valueSetsA.get(7).add(indep8);
		if (numIndeps >= 9) valueSetsA.get(8).add(indep9);

////AW:TIMESLICE_END
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
	{
//AW:AFTER_TIMESLICES

		// Convert nested ArrayLists to double[TIMES][VALUES].
		// Note that the dimensions are reversed from the nested array lists.
		double[][] valueSets = new double[indepTimes.size()][numIndeps];
		for(int valIdx = 0; valIdx < indepTimes.size(); valIdx++)
			for(int indepIdx = 0; indepIdx < numIndeps; indepIdx++)
				valueSets[valIdx][indepIdx] = valueSetsA.get(indepIdx).get(valIdx);

		long valueTimes[] = new long[indepTimes.size()];
		for(int valIdx = 0; valIdx < indepTimes.size(); valIdx++)
			valueTimes[valIdx] = indepTimes.get(valIdx);


		try (Connection conn = tsdb.getConnection();)
		{
			log.debug("Calling rate with {} inputs and {} values each.", valueSets.length, valueTimes.length);

			double depVals[] = ratingSet.rate(conn, valueTimes, valueSets);

			for(int i=0; i<depVals.length; i++)
			{
				if (depVals[i] != Const.UNDEFINED_DOUBLE)
					setOutput(dep, depVals[i], new Date(valueTimes[i]));
				else
					log.warn("ValueSet at time {} could not be rated "
							+"(most likely reason is that it is outside table bounds.)",
							new Date(valueTimes[i]));
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
