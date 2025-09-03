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
import decodes.tsdb.algo.AWAlgoType;

import java.util.TimeZone;
import java.util.Calendar;
import java.util.GregorianCalendar;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;

import decodes.tsdb.ParmRef;
import decodes.hdb.dbutils.DBAccess;
import decodes.hdb.dbutils.DataObject;
import decodes.util.DecodesSettings;
import decodes.hdb.dbutils.RBASEUtils;
import org.opendcs.annotations.PropertySpec;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

@Algorithm(description = "VolumeToFlowAlg calculates average flows based on the sum of the inputs volumes for the given period\n" +		
"Flow (cfs) = sum of volumes * 43560 sq ft per acre / # of days / 86400 seconds\n" +	
"This algorithm assumes units are in cfs and acre-feet; if not a conversion is done to output the correct units\n" +
"Parameters:\n\n" +	
"partial_calculations: boolean: default false: if current period partial calculations will be performed\n" +
"min_values_required: number: default 1: the minimum number of observations required to perform computation\n" +
"min_values_desired: number: default 0: the minimum number of observations desired to perform computation\n" +	
"validation_flag: string: default empty: the validation flag value to be sent to the database\n" +
"flow_factor: Number: use as multiplier for volume to flow factor (.5,.5041) : Default: 43560/86400") 	
public class VolumeToFlowAlg extends decodes.tsdb.algo.AW_AlgorithmBase
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	@Input
	public double input;

	// Enter any local class variables needed by the algorithm.
// mod 1.0.06 added by M. Bogner Aug 2012 for the 3.0 CP upgrade project
// mod 1.0.07 added by M. Bogner March 2013 for the 5.3 CP upgrade project
// since surrogate keys (like SDI's) where changed to a DbKey class insetad of a long
	String alg_ver = "1.0.07";
	String query;
	boolean do_setoutput = true;
	boolean is_current_period;
	String flags;
	Date date_out;
	Double tally;
	int total_count;
	long mvr_count;
	long mvd_count;


	@Output(type = Double.class)
	public NamedVariable output = new NamedVariable("output", 0);

	@PropertySpec(value = "false") 
	public boolean partial_calculations = false;
	@PropertySpec(value = "1") 
	public long min_values_required = 1;
	@PropertySpec(value = "0") 
	public long min_values_desired = 0;
	@PropertySpec(value = " 43560/86400 ") 
	public String flow_factor = " 43560/86400 ";
	@PropertySpec(value = "")
	public String validation_flag = "";

	// Allow javac to generate a no-args constructor.

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	@Override
	protected void initAWAlgorithm( )
		throws DbCompException
	{
		_awAlgoType = AWAlgoType.AGGREGATING;
		_aggPeriodVarRoleName = "output";
	}
	
	/**
	 * This method is called once before iterating all time slices.
	 */
	@Override
	protected void beforeTimeSlices()
		throws DbCompException
	{
		// This code will be executed once before each group of time slices.
		// For TimeSlice algorithms this is done once before all slices.
		// For Aggregating algorithms, this is done before each aggregate
		// period.
		query = null;
		total_count = 0;
		do_setoutput = true;
		flags = "";
		date_out = null;
		tally = 0.0;
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
	@Override
	protected void doAWTimeSlice()
		throws DbCompException
	{
		// Enter code to be executed at each time-slice.
		if (!isMissing(input))
		{
			tally += input;
			total_count++;
		}
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	@Override
	protected void afterTimeSlices() throws DbCompException
	{
		// This code will be executed once after each group of time slices.
		// For TimeSlice algorithms this is done once after all slices.
		// For Aggregating algorithms, this is done after each aggregate
		// period.
		// first calculate number of days in the month in case the numbers are for month derivations
		//
		//  delete any existing value if this period has no records 
		// and do nothing else but return
		if (total_count == 0)
		{
		   deleteOutput(output);
		   return;
		}
		do_setoutput = true;
		ParmRef parmRef = getParmRef("input");
		if (parmRef == null) 
		{
		   log.warn("VolumeToFlowAlg: Unknown aggregate control output variable 'INPUT'");
		                   return;
		}
		String input_interval = parmRef.compParm.getInterval();
		String table_selector = parmRef.compParm.getTableSelector();
		parmRef = getParmRef("output");
		if (parmRef == null) 
		                {
		   log.warn("VolumeToFlowAlg: Unknown aggregate control output variable 'OUTPUT'");
		                   return;
		}
		String output_interval = parmRef.compParm.getInterval();
		//
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
		      log.warn("VolumeToFlowAlg-{} Warning: Illegal negative setting of minimum values criteria " +
			 		  "for non-Month aggregates",
					  alg_ver);
		     log.warn("VolumeToFlowAlg-{} Warning: Minimum values criteria for non-Month aggregates set to 1",
			 		  alg_ver);
		     if (mvd_count < 0) mvd_count = 1;
		     if (mvr_count < 0) mvr_count = 1;
		   }
		   if ((input_interval.equalsIgnoreCase("instant") || output_interval.equalsIgnoreCase("hour")) && mvr_count == 0) 
		   {
		     log.warn("VolumeToFlowAlg-{} Warning: Illegal zero setting of minimum values criteria " +
			 		  "for instant/hour aggregates",
					  alg_ver);
		     log.warn("VolumeToFlowAlg-{} Warning: Minimum values criteria for instant/hour aggregates set to 1",
			 		  alg_ver);
		     mvr_count = 1;
		   }
		}
		//	check and set minimums for yearly aggregates
		if ( output_interval.equalsIgnoreCase("year") || output_interval.equalsIgnoreCase("wy") )
		{
		   if (mvr_count == 0)
		   {
		      if (input_interval.equalsIgnoreCase("month")) mvr_count = 12;
		      if (input_interval.equalsIgnoreCase("day")) mvr_count = cal.getActualMaximum(Calendar.DAY_OF_YEAR);
		      if (input_interval.equalsIgnoreCase("hour")) mvr_count = cal.getActualMaximum(Calendar.DAY_OF_YEAR)*24;
			  	   }
		}
		//		check and set minimums for monthly aggregates
		if ( output_interval.equalsIgnoreCase("month")) 
		{
		   int days = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
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
		   else if (mvr_count == 0 && !input_interval.equalsIgnoreCase("day") ) 
		   {
		     log.warn("VolumeToFlowAlg-{} Warning: Illegal zero setting of minimum values criteria for " +
		     		  "{} to daily aggregates",
					  alg_ver, input_interval);
		     log.warn("VolumeToFlowAlg-{} Warning: Minimum values criteria for daily aggregates set to 1",
			 		  alg_ver);
		     if (mvd_count == 0) mvd_count = 1;
		     if (mvr_count == 0) mvr_count = 1;
		   }
		}
		//
		// get the connection and a few other classes so we can do some sql
		try (Connection conn = tsdb.getConnection())
		{
			DBAccess db = new DBAccess(conn);
			DataObject dbobj = new DataObject();
							dbobj.put("ALG_VERSION",alg_ver);
	// mod 1.0.06 added int cast by M. Bogner Aug 2012 for the 3.0 CP upgrade project
	// mod 1.0.07 added new getValue method call by M. Bogner March 2013 for the 5.3 CP upgrade project
	// since surrogate keys (like SDI's) where changed to a DbKey class instead of a long
							Integer sdi = (int) getSDI("input").getValue();
							String dt_fmt = "dd-MMM-yyyy HH:mm";
			
			RBASEUtils rbu = new RBASEUtils(dbobj,conn);
			SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");
			sdf.setTimeZone(
				TimeZone.getTimeZone(DecodesSettings.instance().aggregateTimeZone));

			String status = null;
							rbu.getStandardDates(sdi,output_interval,_aggregatePeriodBegin,_aggregatePeriodEnd,dt_fmt);
			//  see if we are in a current window and do the query to do the volume calculation
			String query = "select round( hdb_utilities.get_sdi_unit_factor( " + getSDI("input") + 
									") * hdb_utilities.get_sdi_unit_factor( " + getSDI("output") +
									") * " + flow_factor + " / " +
								"( to_date('" +   (String) dbobj.get("SD_EDT")  + "','dd-MM-yyyy HH24:MI') - " +
				"to_date('" +  (String) dbobj.get("SD_SDT") + "','dd-MM-yyyy HH24:MI')" +
				" ) * " + tally + ",5) flow , " + 
				"hdb_utilities.date_in_window('" + output_interval.toLowerCase() +
					"',to_date('" +  sdf.format(_aggregatePeriodBegin) +
					"','dd-MM-yyyy HH24:MI')) is_current_period from dual";
							// now do the query for all the needed data
			status = db.performQuery(query,dbobj);
							debug3(" SQL STRING:" + query + "   DBOBJ: " + dbobj.toString() + "STATUS:  " + status);
			// see if there was an error
			if (status.startsWith("ERROR"))
							{
			log.warn(" VolumeToFlowAlg-{}:  Failed due to following oracle error", alg_ver);
			log.warn(" VolumeToFlowAlg-{}: {}", alg_ver, status);
			return;
			}
			//
			log.trace("VolumeToFlowAlg-{} {} SDI: {}  MVR:{} RecordCount: {}",
				  alg_ver, _aggregatePeriodEnd, getSDI("input"),mvr_count, total_count);
			// now see how many records were found for this aggregate
			//  and see if this calc is in current period and if partial calc is set
			is_current_period = ((String)dbobj.get("is_current_period")).equalsIgnoreCase("Y");
			if (!is_current_period && total_count < mvr_count)
							{
				do_setoutput = false;
				log.debug("VolumeToFlowAlg-{} Minimum required records not met for historic period: {} SDI: {}  " +
					  "MVR: {} RecordCount: {}",
					  alg_ver, _aggregatePeriodEnd, getSDI("input"), mvr_count, total_count);
			}
			if (is_current_period && !partial_calculations && total_count < mvr_count)
							{
				do_setoutput = false;
				log.debug("VolumeToFlowAlg-{}: Minimum required records not met for current period: {} SDI: {} " +
					  "MVR: {} RecordCount: {}",
					  alg_ver, _aggregatePeriodEnd, getSDI("input"), mvr_count, total_count);
			}
			//
			//
			// do the volume calculation, set the output if all is successful and set the flags appropriately
			if (do_setoutput) 
			{
			//  set the dataflags appropriately	
			if (total_count < mvd_count) flags = flags + "n";
							if (is_current_period && total_count < mvr_count)
							//  now we have a partial calculation, so do what needs to be done for partials
							{
								setHdbValidationFlag(output,'T');
								// call the RBASEUtils merge method to add a "seed record" to cp_historic_computations table
										// this new method call was added by M. Bogner March 2013 for the 5.3 CP upgrade project
										// since surrogate keys (like SDI's) where changed to a DbKey class insetad of a long
								//rbu.merge_cp_hist_calc(comp.getAppId(), (int ) getSDI("input"),input_interval,_aggregatePeriodBegin,
								rbu.merge_cp_hist_calc( (int) comp.getAppId().getValue(), (int ) getSDI("input").getValue(),input_interval,_aggregatePeriodBegin,
								_aggregatePeriodEnd,"dd-MM-yyyy HH:mm",tsdb.getWriteModelRunId(),table_selector);
							}
			log.trace("VolumeToFlowAlg: Derivation FLAGS: {}", flags);
			if (flags != null) setHdbDerivationFlag(output,flags);
							Double flow = Double.valueOf(dbobj.get("flow").toString());
			//
							/* added to allow users to automatically set the Validation column  */
							if (validation_flag.length() > 0) setHdbValidationFlag(output,validation_flag.charAt(1));
			setOutput(output,flow);
			}
		}
		catch (SQLException ex)
		{
			throw new DbCompException("Unable to get SQL Connection.", ex);
		}
		//
		//  delete any existing value if this calculation failed
		if (!do_setoutput)
		{
		   deleteOutput(output);
		}
	}
}
