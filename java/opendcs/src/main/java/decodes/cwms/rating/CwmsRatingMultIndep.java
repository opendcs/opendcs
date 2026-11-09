/*
 * This software was written by Cove Software, LLC ("COVE") under contract 
 * to the United States Government. 
 * 
 * No warranty is provided or implied other than specific contractual terms
 * between COVE and the U.S. Government
 * 
 * U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 */
package decodes.cwms.rating;

import java.io.PrintStream;
import java.sql.Connection;
import java.util.Date;

import ilex.var.NamedVariable;
import decodes.cwms.CwmsTimeSeriesDb;
import decodes.db.Constants;
import decodes.db.SiteName;
import decodes.tsdb.DbCompException;
import decodes.tsdb.algo.AWAlgoType;
import decodes.tsdb.ParmRef;
import org.opendcs.annotations.PropertySpec;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;

import hec.data.RatingException;
import hec.data.cwmsRating.RatingSet;
import decodes.cwms.HecConstants;

import java.util.ArrayList;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.tsdb.TimeSeriesIdentifier;
import decodes.util.TSUtil;

@Algorithm(description = "Implements CWMS rating computations.\n" +
"Uses the CWMS API provided by HEC to do the rating.")
public class CwmsRatingMultIndep extends decodes.tsdb.algo.AW_AlgorithmBase
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	@Input
	public double indep1;
	@Input
	public double indep2;
	@Input
	public double indep3;
	@Input
	public double indep4;
	@Input
	public double indep5;
	@Input
	public double indep6;
	@Input
	public double indep7;
	@Input
	public double indep8;
	@Input
	public double indep9;

	public static final String module = "CwmsRatingMultIndep";
	RatingSet ratingSet = null;
	Date beginTime = null;
	Date endTime = null;
	ArrayList<Long> indepTimes = new ArrayList<Long>();
	int numIndeps = 1;
	String specId = "";
	String indep1SiteName = null;

	private ArrayList<ArrayList<Double>> valueSetsA = null;

	
	/**
	 * Builds the comma-separated list of independent parameter codes from the time series
	 * mapped to the indep roles, and sets numIndeps to the number found. Parameters are used
	 * up to the first role that isn't mapped to a time series, unless its own indepN_MISSING
	 * property demands one.
	 */
	String buildIndepSpec()
		throws DbCompException
	{
		String missingAction[] =
		{
			indep1_MISSING, indep2_MISSING, indep3_MISSING, indep4_MISSING, indep5_MISSING,
			indep6_MISSING, indep7_MISSING, indep8_MISSING, indep9_MISSING
		};

		StringBuilder indepSpecId = new StringBuilder();
		numIndeps = 0;
		for (int idx = 0; idx < missingAction.length; idx++)
		{
			String role = "indep" + (idx + 1);
			TimeSeriesIdentifier tsid = getParmTsId(role);
			if (tsid == null)
			{
				if (!missingAction[idx].equalsIgnoreCase("ignore"))
					throw new DbCompException("No time series mapped to " + role);
				break;
			}
			if (numIndeps == 0)
				indep1SiteName = tsid.getSiteName();
			else
				indepSpecId.append(',');
			indepSpecId.append(tsid.getDataType().getCode());
			numIndeps++;
		}

		if (numIndeps == 0)
			throw new DbCompException("No time series mapped to indep1");
		return indepSpecId.toString();
	}

	/**
	 * @return the number of independent parameters the loaded rating requires, or -1 if the
	 * rating doesn't expose a count.
	 */
	private int templateIndepCount()
	{
		if (ratingSet.getRatingSpec() == null && ratingSet.getRatingCount() == 0)
			return -1;
		try
		{
			return ratingSet.getIndParamCount();
		}
		catch (RatingException ex)
		{
			log.atWarn().setCause(ex).log("Cannot determine the independent parameter count for rating '{}'", specId);
			return -1;
		}
	}

	/**
	 * Compares the parameters mapped in the computation against the ones the rating requires.
	 * @return the number of independent parameters to use for this run.
	 * @throws DbCompException if the computation maps a different number of parameters
	 * than the rating takes.
	 */
	static int validateIndepCount(String specId, String compName, int mappedIndeps, int ratingIndeps)
		throws DbCompException
	{
		if (ratingIndeps < 0)
		{
			log.warn("Rating '{}' doesn't declare how many independent parameters it takes -- "
					+ "using the {} inputs mapped to '{}'.", specId, mappedIndeps, compName);
			return mappedIndeps;
		}
		if (ratingIndeps != mappedIndeps)
			throw new DbCompException("Rating '" + specId + "' takes " + ratingIndeps
				+ " independent parameter(s) but computation '" + compName + "' maps " + mappedIndeps
				+ ". Remove the unused 'indep' input parameters or use a rating that takes "
				+ mappedIndeps + ".");
		return ratingIndeps;
	}

	@Output(type = Double.class)
	public NamedVariable dep = new NamedVariable("dep", 0);

	@PropertySpec(value = "USGS-EXSA") 
	public String templateVersion = "USGS-EXSA";
	@PropertySpec(value = "Production") 
	public String specVersion = "Production";
	@PropertySpec(value = "fail") 
	public String indep1_MISSING = "fail";
	@PropertySpec(value = "ignore") 
	public String indep2_MISSING = "ignore";
	@PropertySpec(value = "ignore") 
	public String indep3_MISSING = "ignore";
	@PropertySpec(value = "ignore") 
	public String indep4_MISSING = "ignore";
	@PropertySpec(value = "ignore") 
	public String indep5_MISSING = "ignore";
	@PropertySpec(value = "ignore") 
	public String indep6_MISSING = "ignore";
	@PropertySpec(value = "ignore") 
	public String indep7_MISSING = "ignore";
	@PropertySpec(value = "ignore") 
	public String indep8_MISSING = "ignore";
	@PropertySpec(value = "ignore") 
	public String indep9_MISSING = "ignore";
	@PropertySpec(value = "false") 
	public boolean useDepLocation = false;
	@PropertySpec(value = "") 
	public String locationOverride = "";

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
		String indepSpecId = buildIndepSpec();
		
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
			+ "." + indepSpecId + ";" + depParmRef.compParm.getDataType().getCode() + "."
			+ templateVersion + "." + specVersion;

		// Retrieve the RatingSet object
		try (CwmsRatingDao crd = new CwmsRatingDao((CwmsTimeSeriesDb)tsdb))
		{
			Date earliestBaseTime = baseTimes.first();
			if (earliestBaseTime == null)
				earliestBaseTime = new Date();
			ratingSet = crd.getRatingSet(specId);

			// The rating is authoritative for how many independent parameters it takes.
			// Mapped inputs beyond that number would silently add columns to the value
			// sets passed to the rating.
			numIndeps = validateIndepCount(specId, comp.getName(), numIndeps, templateIndepCount());
			
			// As per instructions from Mike Perryman: I must find out what the native units
			// for the rating are and convert before calling the Java rating. The Java rating
			// method assumes that data are already in correct units.
			String punits[] = ratingSet.getDataUnits();
			for(int pidx = 0; punits != null && pidx < numIndeps && pidx < punits.length-1; pidx++)
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
			if (punits != null && punits.length > 1 && punits[punits.length-1] != null
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
		if (ratingSet == null)
		{
			throw new DbCompException("No rating set!");
		}
		
		double indepVals[] = { indep1, indep2, indep3, indep4, indep5,
							   indep6, indep7, indep8, indep9 };
		String indepMissingActions[] = { indep1_MISSING, indep2_MISSING, indep3_MISSING,
										 indep4_MISSING, indep5_MISSING, indep6_MISSING,
										 indep7_MISSING, indep8_MISSING, indep9_MISSING };

		// If any non-ignored params are missing in this time-slice, skip it.
		// Inputs beyond numIndeps are not part of the rating spec, so they're not considered.
		for (int idx = 0; idx < numIndeps; idx++)
		{
			if (!isAssigned("indep" + (idx + 1)))
			{
				continue;
			}

			if (isMissing(indepVals[idx]) && "fail".equalsIgnoreCase(indepMissingActions[idx]))
			{
				return;
			}
		}

		// MJM 10/31/2017 Modified to save values and do array rating in after method.
		indepTimes.add(_timeSliceBaseTime.getTime());
		for (int idx = 0; idx < numIndeps; idx++)
		{
			valueSetsA.get(idx).add(indepVals[idx]);
		}
		
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
	{
		if (ratingSet == null || indepTimes.isEmpty())
			return;
		
		// Convert nested ArrayLists to double[TIMES][VALUES].
		// Note that the dimensions are reversed from the nested array lists.
		double[][] valueSets = new double[indepTimes.size()][numIndeps];
		for(int valIdx = 0; valIdx < indepTimes.size(); valIdx++)
			for(int indepIdx = 0; indepIdx < numIndeps; indepIdx++)
				valueSets[valIdx][indepIdx] = valueSetsA.get(indepIdx).get(valIdx);
		
		long valueTimes[] = new long[indepTimes.size()];
		for(int valIdx = 0; valIdx < indepTimes.size(); valIdx++)
			valueTimes[valIdx] = indepTimes.get(valIdx);
		
		
		try (Connection conn = tsdb.getConnection())
		{
			log.debug("Calling rate with {} values and {} inputs each.", valueTimes.length, numIndeps);
			
			double depVals[] = ratingSet.rate(conn, valueTimes, valueSets);
			
			for(int i=0; i<depVals.length; i++)
			{
				if (depVals[i] != HecConstants.UNDEFINED_DOUBLE)
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
	}
}
