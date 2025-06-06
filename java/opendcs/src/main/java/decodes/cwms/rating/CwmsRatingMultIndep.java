/**
 * $Id$
 * 
 * $Log$
 * Revision 1.12  2017/10/23 13:36:01  mmaloney
 * Log stack trace on rating exceptions.
 *
 * Revision 1.11  2017/08/22 19:32:16  mmaloney
 * Improve comments
 *
 * Revision 1.10  2017/02/16 14:41:26  mmaloney
 * Close CwmsRatingDao in final block.
 *
 * Revision 1.9  2017/02/09 17:23:42  mmaloney
 * Allow locationOverride to contain wildcards.
 *
 * Revision 1.8  2016/12/16 14:22:01  mmaloney
 * Added locationOverride property.
 *
 * Revision 1.7  2016/09/29 18:54:36  mmaloney
 * CWMS-8979 Allow Database Process Record to override decodes.properties and
 * user.properties setting. Command line arg -Dsettings=appName, where appName is the
 * name of a process record. Properties assigned to the app will override the file(s).
 *
 * Revision 1.6  2016/01/13 15:15:04  mmaloney
 * rating retrieval
 *
 * Revision 1.5  2015/07/14 17:53:54  mmaloney
 * Added 'useDepLocation' property with default=false.
 * Set to true to use the dep param location for building rating spec.
 *
 * Revision 1.4  2015/01/06 02:09:11  mmaloney
 * dev
 *
 * Revision 1.3  2015/01/05 21:04:39  mmaloney
 * Automatically convert units to the dataUnits reported by the RatingSet.
 *
 * Revision 1.2  2014/12/18 21:52:21  mmaloney
 * In error messages, print the specId.
 *
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 * Revision 1.8  2012/11/20 21:17:18  mmaloney
 * Implemented cache for ratings.
 *
 * Revision 1.7  2012/11/20 19:50:00  mmaloney
 * dev
 *
 * Revision 1.6  2012/11/20 16:29:52  mmaloney
 * fixed typos in variable names.
 *
 * Revision 1.5  2012/11/12 20:13:52  mmaloney
 * Do the rating in the time slice method. Not after.
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
 * This software was written by Cove Software, LLC ("COVE") under contract 
 * to the United States Government. 
 * 
 * No warranty is provided or implied other than specific contractual terms
 * between COVE and the U.S. Government
 * 
 * Copyright 2016 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * All rights reserved.
 */
package decodes.cwms.rating;

import java.io.PrintStream;
import java.sql.Connection;
import java.util.Date;

import ilex.util.Logger;
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
import hec.lang.Const;

import java.util.ArrayList;

import decodes.tsdb.TimeSeriesIdentifier;
import decodes.util.TSUtil;

@Algorithm(description = "Implements CWMS rating computations.\n" +
"Uses the CWMS API provided by HEC to do the rating.")
public class CwmsRatingMultIndep
	extends decodes.tsdb.algo.AW_AlgorithmBase
{
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
//		indep2Values = new ArrayList<Double>();
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
//		indep3Values = new ArrayList<Double>();
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
//		indep4Values = new ArrayList<Double>();
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
//		indep5Values = new ArrayList<Double>();
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
//		indep6Values = new ArrayList<Double>();
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
//		indep7Values = new ArrayList<Double>();
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
//		indep8Values = new ArrayList<Double>();
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
//		indep9Values = new ArrayList<Double>();
		numIndeps = 9;

		return indepSpecId;
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
		specId = buildIndepSpec();
		
		ParmRef depParmRef = getParmRef("dep");
		String specLocation = indep1SiteName;
		if (useDepLocation)
		{
			SiteName depSiteName = depParmRef.compParm.getSiteName(Constants.snt_CWMS);
			if (depSiteName == null)
				debug1("No dependent site name available, using site from indep1");
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
					debug1(module + " Converting " + pname + " units for time series " 
						+ indepParmRef.timeSeries.getTimeSeriesIdentifier().getUniqueString() + " from "
						+ indepParmRef.timeSeries.getUnitsAbbr() + " to " + punits[pidx]);
					TSUtil.convertUnits(indepParmRef.timeSeries, punits[pidx]);
				}
			}
			
			// Likewise for the dependent param:
			if (punits.length > 1 && punits[punits.length-1] != null
			 && depParmRef.timeSeries.getUnitsAbbr() != null
			 && !depParmRef.timeSeries.getUnitsAbbr().equalsIgnoreCase(punits[punits.length-1]))
			{
				debug1(module + " Converting dep units from "
					+ depParmRef.timeSeries.getUnitsAbbr() + " to " + punits[punits.length-1]);
				TSUtil.convertUnits(depParmRef.timeSeries, punits[punits.length-1]);
			}

		}
		catch (RatingException ex)
		{
			String m = "Cannot read rating for '" + specId + "': " + ex;
			warning(m);
			ex.printStackTrace(Logger.instance().getLogOutput() != null 
				? Logger.instance().getLogOutput() : System.err);
			throw new DbCompException(m);
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
		
		
		
//		double valueSet[] = new double[numIndeps];
//		valueSet[0] = indep1;
//		if (numIndeps >= 2) valueSet[1] = indep2;
//		if (numIndeps >= 3) valueSet[2] = indep3;
//		if (numIndeps >= 4) valueSet[3] = indep4;
//		if (numIndeps >= 5) valueSet[4] = indep5;
//		if (numIndeps >= 6) valueSet[5] = indep6;
//		if (numIndeps >= 7) valueSet[6] = indep7;
//		if (numIndeps >= 8) valueSet[7] = indep8;
//		if (numIndeps >= 9) valueSet[8] = indep9;
		
		
		
		
//		try
//		{
//			double output = ratingSet.rateOne(valueSet, _timeSliceBaseTime.getTime());
//			setOutput(dep, output);
//			if (Logger.instance().getMinLogPriority() == Logger.E_DEBUG3)
//			{
//				StringBuilder sb = new StringBuilder();
//				for(int i=0; i<numIndeps; i++)
//					sb.append((i>0?", ":"") + "i" + (i+1) + "=" + valueSet[i]);
//				sb.append(" -- output=" + output);
//				debug3(sb.toString());
//			}
//		}
//		catch (RatingException ex)
//		{
//			warning("Rating failure specId='" + specId + "': " + ex);
//			if (Logger.instance().getLogOutput() != null)
//				ex.printStackTrace(Logger.instance().getLogOutput());
//		}
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
	{
		
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
			debug1("Calling rate with " + valueSets.length + " inputs and " 
				+ valueTimes.length + " values each.");
			
			double depVals[] = ratingSet.rate(conn, valueTimes, valueSets);
			
			for(int i=0; i<depVals.length; i++)
			{
				if (depVals[i] != Const.UNDEFINED_DOUBLE)
					setOutput(dep, depVals[i], new Date(valueTimes[i]));
				else
					warning("ValueSet at time " + debugSdf.format(new Date(valueTimes[i]))
						+ " could not be rated (most likely reason is that it is outside table bounds.)");
			}
		}
		catch(Exception ex)
		{
			String msg = "Rating failure: " + ex;
			warning(msg);
			PrintStream out = Logger.instance().getLogOutput();
			if (out == null)
				out = System.err;
			ex.printStackTrace(out);
			Throwable cause = ex.getCause();
			if (cause != null)
			{
				warning("...cause: " + cause);
				cause.printStackTrace(out);
			}
		}
	}
}
