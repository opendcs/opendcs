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
package decodes.hdb.algo;

import java.util.Date;

import ilex.var.NamedVariable;
import decodes.tsdb.DbCompException;
//AW:IMPORTS
// Place an import statements you need here.
import java.util.TimeZone;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.util.Calendar;
import java.util.GregorianCalendar;

import java.sql.Connection;
import java.text.SimpleDateFormat;

// this new import was added by M. Bogner Aug 2012 for the 3.0 CP upgrade project
import decodes.tsdb.algo.AWAlgoType;
import decodes.tsdb.ParmRef;
import decodes.hdb.dbutils.DBAccess;
import decodes.hdb.dbutils.DataObject;
import decodes.util.DecodesSettings;
import decodes.util.PropertySpec;
//import decodes.tsdb.DbCompConfig;
import decodes.hdb.dbutils.RBASEUtils;
//AW:IMPORTS_END

//AW:JAVADOC
/**
This algorithm is for aggregations across HDB expected intervals.
Will do any ORACLE based aggregation that takes only the one value
as the function  ie MAX(value)  so this algorithm can presently do:

min, max, avg, count, sum, median, stddev, and variance

specify the aggregate they desire by stating the oracle
function in the aggregate_name property  ie aggregate_name=sum

MIN_VALUES_REQUIRED: setting to zero means use max number observations for interval
NO_ROUNDING: determines if rounding to the 5th decimal point is desired, default FALSE
 */
//AW:JAVADOC_END
public class DynamicAggregatesAlg extends decodes.tsdb.algo.AW_AlgorithmBase
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
//AW:INPUTS
	public double input;	//AW:TYPECODE=i
	String _inputNames[] = { "input" };
//AW:INPUTS_END

//AW:LOCALVARS
	// Enter any local class variables needed by the algorithm.
        // version 12 change by M. Bogner 2/2013 to add NO_ROUNDING property
        // version 1.0.13 moded by M. Bogner March 2013 for CP 5.3 project
        // since surrogate keys (like SDI's) where changed to a DbKey class insetad of a long
	String alg_ver = "1.0.13";
	String query;
	int count = 0;
	boolean do_setoutput = true;
	boolean is_current_period;
	String flags;
	Connection conn = null;
	Date date_out;
	int total_count;
	long mvr_count;
	long mvd_count;

	PropertySpec specs[] =
	{
		new PropertySpec("aggregate_name", PropertySpec.STRING,
			"(required) The name of the Oracle aggregate function."),
		new PropertySpec("min_values_required", PropertySpec.INT,
			"(default=1) No output produced if fewer than this many inputs in the aggregate period."),
		new PropertySpec("min_values_desired", PropertySpec.INT,
			"(default=0) If fewer than this many inputs in the period, output flagged as a partial calculation."),
		new PropertySpec("partial_calculations", PropertySpec.BOOLEAN,
			"(default=false) If true, then partial calcs are accepted but flagged as 'T' (temporary)."),
		new PropertySpec("no_rounding", PropertySpec.BOOLEAN,
			"(default=false) If true, then no rounding is done on the output value."),
		new PropertySpec("validation_flag", PropertySpec.STRING,
			"(empty) Always set this validation flag in the output."),
		new PropertySpec("negativeReplacement", PropertySpec.NUMBER,
			"(no default) If set, and output would be negative, then replace with the number supplied.")
	};


//AW:LOCALVARS_END

//AW:OUTPUTS
	public NamedVariable output = new NamedVariable("output", 0);
	String _outputNames[] = { "output" };
//AW:OUTPUTS_END

//AW:PROPERTIES
	public boolean no_rounding = false;
	public boolean partial_calculations = false;
	public long min_values_required = 1;
	public long min_values_desired = 0;
	public String aggregate_name = "NONE";
    public String validation_flag = "";
	public double negativeReplacement = Double.NEGATIVE_INFINITY;

	String _propertyNames[] = { "partial_calculations", "min_values_required", "min_values_desired", "aggregate_name",
	"validation_flag","no_rounding", "negativeReplacement" };
//AW:PROPERTIES_END

	// Allow javac to generate a no-args constructor.

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	protected void initAWAlgorithm( )
		throws DbCompException
	{
//AW:INIT
		_awAlgoType = AWAlgoType.AGGREGATING;
		_aggPeriodVarRoleName = "output";
//AW:INIT_END

//AW:USERINIT
		// Code here will be run once, after the algorithm object is created.
		noAggregateFill = true;
//AW:USERINIT_END
	}

	/**
	 * This method is called once before iterating all time slices.
	 */
	protected void beforeTimeSlices()
		throws DbCompException
	{
//AW:BEFORE_TIMESLICES
		// This code will be executed once before each group of time slices.
		// For TimeSlice algorithms this is done once before all slices.
		// For Aggregating algorithms, this is done before each aggregate
		// period.
		query = null;
		count = 0;
		do_setoutput = true;
		flags = "";
		conn = null;
		date_out = null;
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
		// Enter code to be executed at each time-slice.
//AW:TIMESLICE_END
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
	{
//AW:AFTER_TIMESLICES
		// This code will be executed once after each group of time slices.
		// For TimeSlice algorithms this is done once after all slices.
		// For Aggregating algorithms, this is done after each aggregate
		// period.
		// calculate number of days in the month in case the numbers are for month derivations
		log.debug("DynamicAggregates-{} BEGINNING OF AFTER TIMESLICES: for period: {} SDI: {}  MVR: {}",
				  alg_ver, _aggregatePeriodBegin, getSDI("input"), min_values_required );
		do_setoutput = true;
//
		// get the input and output parameters and see if its model data
		ParmRef parmRef = getParmRef("input");
		if (parmRef == null) warning("Unknown aggregate control output variable 'INPUT'");
		String input_interval = parmRef.compParm.getInterval();
		String table_selector = parmRef.compParm.getTableSelector();
                int model_run_id = parmRef.timeSeries.getModelRunId();
		parmRef = getParmRef("output");
		if (parmRef == null) warning("Unknown aggregate control output variable 'OUTPUT'");
		String output_interval = parmRef.compParm.getInterval();
//

                TimeZone tz = TimeZone.getTimeZone("GMT");
                GregorianCalendar cal = new GregorianCalendar(tz);
                GregorianCalendar cal1 = new GregorianCalendar();
                cal1.setTime(_aggregatePeriodBegin);
                cal.set(cal1.get(Calendar.YEAR),cal1.get(Calendar.MONTH),cal1.get(Calendar.DAY_OF_MONTH),0,0);
		mvr_count = min_values_required;
		mvd_count = min_values_desired;


//		first see if there are bad negative min settings for other than a monthly aggregate...
		if ( !output_interval.equalsIgnoreCase("month"))
		{
		   if (mvr_count < 0 || mvd_count < 0)
		   {
		     log.warn("DynamicAggregatesAlg-{} Warning: Illegal negative setting of minimum values criteria for non-Month aggregates", alg_ver);
		     log.warn("DynamicAggregatesAlg-{} Warning: Minimum values criteria for non-Month aggregates set to 1", alg_ver);
		     if (mvd_count < 0) mvd_count = 1;
		     if (mvr_count < 0) mvr_count = 1;
		   }
		   if ((input_interval.equalsIgnoreCase("instant") || output_interval.equalsIgnoreCase("hour")) && mvr_count == 0)
		   {

		     log.warn("DynamicAggregatesAlg-{} Warning: Illegal zero setting of minimum values criteria for instant/hour aggregates", alg_ver);
		     log.warn("DynamicAggregatesAlg-{} Warning: Minimum values criteria for instant/hour aggregates set to 1", alg_ver);
		     mvr_count = 1;
		   }
		}

//		check and set minimums for yearly aggregates
		if ( output_interval.equalsIgnoreCase("year") || output_interval.equalsIgnoreCase("wy") )
		{
		   if (mvr_count == 0)
		   {
		      // mod Dec 2010 by M. Bogner to account for leap year bug for wy interval
		      int day_count = cal.getActualMaximum(Calendar.DAY_OF_YEAR);
                      // if wy just set to 365; to hard to figure if it is affected by a leap year
		      if (output_interval.equalsIgnoreCase("wy")) day_count = 365;
		      if (input_interval.equalsIgnoreCase("month")) mvr_count = 12;
		      if (input_interval.equalsIgnoreCase("day")) mvr_count = day_count;
		      if (input_interval.equalsIgnoreCase("hour")) mvr_count = day_count*24;
	  	   }
		}

//		check and set minimums for monthly aggregates
		if ( output_interval.equalsIgnoreCase("month"))
		{
		   int days = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
		   log.trace("-{} We have calculated that there are {} days in this month", alg_ver, days);
		   //  now if the required numbers are negative then calculate based on total days in month
		   if (mvr_count <= 0 && input_interval.equalsIgnoreCase("day")) mvr_count = days + mvr_count;
		   if (mvr_count <= 0 && input_interval.equalsIgnoreCase("hr")) mvr_count = days*24 + mvr_count;
		   if (mvd_count <= 0 && input_interval.equalsIgnoreCase("day")) mvd_count = days + mvd_count;
		   if (mvd_count <= 0 && input_interval.equalsIgnoreCase("hr")) mvd_count = days*24 + mvd_count;
		}
//
//		check and set minimums for daily aggregates
		if ( output_interval.equalsIgnoreCase("day"))
		{
		   if (mvr_count == 0 && input_interval.equalsIgnoreCase("hour"))
		   {
		     mvr_count = 24;
		   }
		   else if (mvr_count == 0)
		   {
		     log.warn("DynamicAggregatesAlg-{} Warning: Illegal zero setting of minimum values criteria " +
			 		  "for {} to daily aggregates",
					  alg_ver, input_interval);
		     log.warn("DynamicAggregatesAlg-{} Warning: Minimum values criteria for daily aggregates set to 1",
			 		  alg_ver);
		     if (mvd_count == 0) mvd_count = 1;
		     if (mvr_count == 0) mvr_count = 1;
		   }

		}
//
		// get the connection  and a few other classes so we can do some sql
		// TODO: wrap in try-with-resources, also suggest NOT getting another connection (see line 306)
		conn = tsdb.getConnection();
		SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");
                sdf.setTimeZone(
                        TimeZone.getTimeZone(
                        	DecodesSettings.instance().aggregateTimeZone));
//                        DbCompConfig.instance().getAggregateTimeZone()));
		String status = null;
		DataObject dbobj = new DataObject();
		dbobj.put("ALG_VERSION",alg_ver);
                RBASEUtils rbu = new RBASEUtils(dbobj,conn);
//
		// now construct the additional model_run_id clause if model data
		String if_model_data = "";
		if (table_selector.equalsIgnoreCase("M_"))
		{
		  if_model_data = " model_run_id = " + model_run_id + " and ";
		}
		//  see if we are in a current window
		String query = "select hdb_utilities.date_in_window('" + output_interval.toLowerCase() +
		               "',to_date('" +  sdf.format(_aggregatePeriodBegin) +
		               "','dd-MM-yyyy HH24:MI')) is_current_period from dual";
		conn = tsdb.getConnection();
		DBAccess db = new DBAccess(conn);
		status = db.performQuery(query,dbobj);
		if (status.startsWith("ERROR"))
		{
		  log.warn("DynamicAggregatesAlg-{} Aborted: see following error message", alg_ver);
		  log.warn(status);
		  return;
		}
		log.trace(query);
		// do the aggregate query to get the aggregate value and the total_count of the records
		String lower_limit = " >= ";
		if(!aggLowerBoundClosed)
		lower_limit = " > ";
		String upper_limit = " < ";
		if(aggUpperBoundClosed)
		upper_limit = " <= ";
		// TODO: use bind varss. Will it suck, yes, but it avoids sql injection attacks and will likely boost performance
		// as the Oracle query planner will be able to save the plan
		query = "select round(" + aggregate_name + "(value),7) result, count(*) total_count  from " +
			 table_selector + input_interval.toLowerCase() +
		         " where " + if_model_data + "  site_datatype_id = " + getSDI("input") +
		         " and start_date_time " + lower_limit +  "to_date('" +  sdf.format(_aggregatePeriodBegin) + "','dd-MM-yyyy HH24:MI')" +
		         " and start_date_time " + upper_limit +  "to_date('" +  sdf.format(_aggregatePeriodEnd) + "','dd-MM-yyyy HH24:MI')";

                // new option to not request any rounding added 2/2013 as requested by LC
                if (no_rounding)
                {
		   query =
                         "select " + aggregate_name + "(value) result, count(*) total_count  from " +
			 table_selector + input_interval.toLowerCase() +
		         " where " + if_model_data + "  site_datatype_id = " + getSDI("input") +
		         " and start_date_time " + lower_limit +  "to_date('" +  sdf.format(_aggregatePeriodBegin) + "','dd-MM-yyyy HH24:MI')" +
		         " and start_date_time " + upper_limit +  "to_date('" +  sdf.format(_aggregatePeriodEnd) + "','dd-MM-yyyy HH24:MI')";
                }

		status = db.performQuery(query,dbobj);
		log.trace(" SQL STRING:{}   DBOBJ: {} STATUS: {}", query, dbobj.toString(), status);
		// now see if the aggregate query worked if not the abort!!!
		if (status.startsWith("ERROR"))
		{
		  log.warn("DynamicAggregatesAlg-{} Aborted: see following error message", alg_ver);
		  log.warn(status);
		  return;
		}
		// now see how many records were found for this aggregate
		//  and see if this calc is in current period and if partial calc is set
		total_count = Integer.parseInt(dbobj.get("total_count").toString());
//
		//  delete any existing resultant value if this no records exist
		if (total_count == 0)
		{
			log.trace("DynamicAggregates-{} Aborted: No records: {} SDI: {}",
					  alg_ver, _aggregatePeriodBegin, getSDI("input"));
			deleteOutput(output);
 			return;
		}

//              otherwise we have some records so continue...

		is_current_period = ((String)dbobj.get("is_current_period")).equalsIgnoreCase("Y");
		if (!is_current_period && total_count < mvr_count)
		{
		  	log.debug("DynamicAggregates-{} Aborted: Minimum required records not met for historic period: {} SDI: " +
		  	 		  "{}  MVR: {} RecordCount: {}",
		  		      alg_ver, _aggregatePeriodBegin, getSDI("input"), mvr_count, total_count);
		  	do_setoutput = false;
		}
		if (is_current_period && !partial_calculations && total_count < mvr_count)
		{
			log.debug("DynamicAggregates-{} Aborted: Minimum required records not met for current period: {} " +
					  "SDI: {}  MVR: {} RecordCount: {}",
			 		  alg_ver, _aggregatePeriodBegin, getSDI("input"), mvr_count, total_count);
			do_setoutput = false;
		}
//
		log.trace("MVRI: {}  MVD: {} do_setoutout: {}", mvr_count, mvd_count, do_setoutput);
		log.trace("ICP: {} TotalCount: {}", is_current_period, total_count);

		// set the output if all is successful and set the flags appropriately
		if (do_setoutput)
		{
			if (total_count < mvd_count)
				flags = flags + "n";
			if (is_current_period && total_count < mvr_count)
			// now we have a partial calculation, so do what needs to be done
			// for partials
			{
				setHdbValidationFlag(output, 'T');
				// call the RBASEUtils merge method to add a "seed record" to
				// cp_historic_computations table
				// The code modified the SDI from the getSDI method due to a
				// change in int to long primitive type
				// This code modified by M. Bogner August 2012 for the 3.0 CP
				// upgrade project
				// This code modified by M. Bogner March 2013 for the 5.2 CP
				// upgrade project where the surrogate keys modified to an
				// object
				int tempSDI = (int) getSDI("input").getValue();

				rbu.merge_cp_hist_calc((int) comp.getAppId().getValue(), tempSDI, input_interval,
					_aggregatePeriodBegin, _aggregatePeriodEnd, "dd-MM-yyyy HH:mm",
					tsdb.getWriteModelRunId(), table_selector);
			}
			Double value_out = Double.valueOf(dbobj.get("result").toString());
			log.trace("FLAGS: {}", flags);
			if (flags != null)
				setHdbDerivationFlag(output, flags);
			//
			/* added to allow users to automatically set the Validation column */
			if (validation_flag.length() > 0)
				setHdbValidationFlag(output, validation_flag.charAt(1));

			// Added for HDB issue 386
			if (value_out < 0.0 && negativeReplacement != Double.NEGATIVE_INFINITY)
			{
				log.debug("Computed aggregate={}, will use negativeReplacement={}",
						  value_out, negativeReplacement);
				value_out = negativeReplacement;
			}
			log.info("Setting output for agg period starting {}", this._aggregatePeriodBegin);
			setOutput(output, value_out);
		}
		// delete any existing value if this calculation failed
		if (!do_setoutput)
		{
			log.info("Deleting output for agg period starting {}", this._aggregatePeriodBegin);
			deleteOutput(output);
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

	@Override
	protected PropertySpec[] getAlgoPropertySpecs()
	{
		return specs;
	}

}
