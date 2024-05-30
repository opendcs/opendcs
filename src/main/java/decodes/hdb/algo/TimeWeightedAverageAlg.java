package decodes.hdb.algo;

import java.util.Date;

import ilex.var.NamedVariableList;
import ilex.var.NamedVariable;
import decodes.tsdb.DbAlgorithmExecutive;
import decodes.tsdb.DbCompException;
import decodes.tsdb.DbIoException;
import decodes.tsdb.VarFlags;
// this new import was added by M. Bogner Aug 2012 for the 3.0 CP upgrade project
import decodes.tsdb.algo.AWAlgoType;
// this new import was added by M. Bogner Martch 2013 for the 5.3 CP upgrade project
// where the surrogate keys (like SDI) were changed form a long to a DbKey class
import decodes.sql.DbKey;



//AW:IMPORTS
// Place an import statements you need here.
import java.util.TimeZone;
import java.util.Calendar;
import java.util.GregorianCalendar;

import decodes.hdb.HdbFlags;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.text.DecimalFormat;

import ilex.util.DatePair;
import decodes.tsdb.ParmRef;
import decodes.hdb.dbutils.DBAccess;
import decodes.hdb.dbutils.DataObject;
import decodes.hdb.dbutils.RBASEUtils;
import decodes.tsdb.DbCompException;
import decodes.util.DecodesSettings;

//AW:IMPORTS_END

//AW:JAVADOC
/**
This algorithm does a time weighted average over the interval period

Parameters:

partial_calculations: boolean: default false: if current period partial calculations will be performed
min_values_required: number: default 1: the minimum number of observations required to perform computation
min_values_desired: number: default 0: the minimum number of observations desired to perform computation
validation_flag: string: default empty: the validation flag value to be sent to the database

 */
//AW:JAVADOC_END
public class TimeWeightedAverageAlg
	extends decodes.tsdb.algo.AW_AlgorithmBase
{
//AW:INPUTS
	public double input;	//AW:TYPECODE=i
	String _inputNames[] = { "input" };
//AW:INPUTS_END

//AW:LOCALVARS
	// Enter any local class variables needed by the algorithm.
// version 1.0.05 modification to fix date math for previous and next data window
// version 1.0.06 modification to fix CP 3.0 Upgrade issues, by M. Bogner Aug 2012
// previous version worked on Solaris but not on Linux 
// version 1.0.07 modification for CP 5.3 Upgrade project, by M. Bogner March 2013
// where the surrogate keys (like SDI) were changed from a long to a DbKey class
        String alg_ver = "1.0.07";
	String query;
	boolean do_setoutput = true;
	boolean is_current_period;
	boolean have_beginning_record;
	String flags;
	Date[] date_out = new Date[60];
	Double[] value_out = new Double[60];
	int total_count;
	int index;
	double tally;
        long mvr_count;
        long mvd_count;


//AW:LOCALVARS_END

//AW:OUTPUTS
	public NamedVariable output = new NamedVariable("output", 0);
	String _outputNames[] = { "output" };
//AW:OUTPUTS_END

//AW:PROPERTIES
	public boolean partial_calculations = false;
	public long min_values_required = 1;
	public long min_values_desired = 0;
        public String validation_flag = "";
	String _propertyNames[] = { "partial_calculations", "min_values_required", "min_values_desired",
	"validation_flag" };
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
		total_count = 0;
		do_setoutput = true;
		flags = "";
		tally = 0.0;
		have_beginning_record = false;
		index = 0;
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
                if ((total_count == 0) && (!isMissing(input)))
                {
		  if (_timeSliceBaseTime.equals(_aggregatePeriodBegin))
 		  {
                    debug3("FOUND First begin date"+ _timeSliceBaseTime);
		    have_beginning_record = true;
		    index = -1;
                  }
 		}
                if (!isMissing(input))
		{
                  index++;
                  total_count++;
                  value_out[index] = input;
                  date_out [index] = _timeSliceBaseTime;
		  debug3( "Index:  " + index + "  Value:  " + input + "  DATE: " + _timeSliceBaseTime);
		}
 

//AW:TIMESLICE_END
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	@Override
	protected void afterTimeSlices() throws DbCompException
	{
//AW:AFTER_TIMESLICES
		// This code will be executed once after each group of time slices.
		// For TimeSlice algorithms this is done once after all slices.
		// For Aggregating algorithms, this is done after each aggregate
		// period.
                //  now if there is no records  then delete output it if it exists then return
                if (total_count == 0)
                {
                    deleteOutput(output);
                    return;
                }
//
		do_setoutput = true;
		ParmRef parmRef = getParmRef("input");
		if (parmRef == null) 
		{
		   warning("Unknown aggregate control output variable 'INPUT'");
		   return;
		}
		String input_interval = parmRef.compParm.getInterval();
                String table_selector = parmRef.compParm.getTableSelector();

		parmRef = getParmRef("output");
		if (parmRef == null) 
		{
		   warning("Unknown aggregate control output variable 'OUTPUT'");
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

		     warning("TWAINTERPALG-"+alg_ver+" Warning: Illegal negative setting of minimum values criteria for non-Month aggregates");
		     warning("TWAINTERPALG-"+alg_ver+" Warning: Minimum values criteria for non-Month aggregates set to 1");
		     if (mvd_count < 0) mvd_count = 1;
		     if (mvr_count < 0) mvr_count = 1;
		   }
		   if ((input_interval.equalsIgnoreCase("instant") || output_interval.equalsIgnoreCase("hour")) && mvr_count == 0) 
		   {

		     warning("TWAINTERPALG-"+alg_ver+" Warning: Illegal zero setting of minimum values criteria for instant/hour aggregates");
		     warning("TWAINTERPALG-"+alg_ver+" Warning: Minimum values criteria for instant/hour aggregates set to 1");
		     mvr_count = 1;
		   }
		}

//		check and set minimums for yearly aggregates
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
		   else if (mvr_count == 0)
		   {
		     warning("TWAINTERPALG-"+alg_ver+" Warning: Illegal zero setting of minimum values criteria for " 
		     + input_interval + " to daily aggregates");
		     warning("TWAINTERPALG-"+alg_ver+" Warning: Minimum values criteria for daily aggregates set to 1");
		     if (mvd_count == 0) mvd_count = 1;
		     if (mvr_count == 0) mvr_count = 1;
		   }

		}
//
		//
		// get the connection so we can do some sql
		// also set up a few necessary classes to continue
		try (Connection conn = tsdb.getConnection())
		{
			DBAccess db = new DBAccess(conn);
			DataObject dbobj = new DataObject();
					dbobj.put("ALG_VERSION",alg_ver);
					String dt_fmt = "dd-MMM-yyyy HH:mm";
			SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");
					sdf.setTimeZone(
							TimeZone.getTimeZone(DecodesSettings.instance().aggregateTimeZone));

			String status = null;
	//   getSDI method casted to int since it was changed sometime to a long, M. Bogner Aug 2012
	//   getSDI getValue method since it was changed to a Dbkey object for CP 5.3 project, M. Bogner March 2013
					Integer sdi = (int) getSDI("input").getValue();
			RBASEUtils rbu = new RBASEUtils(dbobj,conn);
			GregorianCalendar cal2 = new GregorianCalendar();
			int calIntervalRoll = 0;

	// set the calendar interval field to use to roll the calendar back and forward
			if (output_interval.equalsIgnoreCase("hour")) calIntervalRoll = Calendar.HOUR_OF_DAY;
			if (output_interval.equalsIgnoreCase("day"))  calIntervalRoll =  Calendar.DAY_OF_MONTH;
			if (output_interval.equalsIgnoreCase("year")) calIntervalRoll =  Calendar.YEAR;
			if (output_interval.equalsIgnoreCase("month")) calIntervalRoll =  Calendar.MONTH;

			// query to see if we are in current period
					String query = "select hdb_utilities.date_in_window('" + output_interval.toLowerCase() +
								"',to_date('" +  sdf.format(_aggregatePeriodBegin) +
								"','dd-MM-yyyy HH24:MI')) is_current_period from dual";
					status = db.performQuery(query,dbobj);
			// see if there was an error
			if (status.startsWith("ERROR"))
			{
			warning(status);
			return;
			}
					debug3(" ICPQ SQL STRING:" + query + "   DBOBJ: " + dbobj.toString() + "STATUS:  " + status);
			//
			debug3("TWAINTERP- " + alg_ver + " : " +  getSDI("input") + " " + _aggregatePeriodBegin  + "  MVR: " + mvr_count + " RecordCount: " + total_count);
			//
			// now see how many records were found for this aggregate
			//  and see if this calc is in current period and if partial calc is set
			if (total_count == 0) 
			{
			debug2("TWAINTERP- " + alg_ver + " : Cannot do Computation due to 0 records: " + getSDI("input") + " " + _aggregatePeriodBegin );
			do_setoutput = false;
			}
			is_current_period = ((String)dbobj.get("is_current_period")).equalsIgnoreCase("Y");
			if (!is_current_period && total_count < mvr_count)
			{
			debug2("TWAINTERP- " + alg_ver + " : Minimum required records not met for historic period: " + getSDI("input") + " " + _aggregatePeriodBegin  + "  MVR: " + mvr_count + " RecordCount: " + total_count);
			do_setoutput = false;
			}
			if (is_current_period && !partial_calculations && total_count < mvr_count)
			{
			debug2("TWAINTERP- " + alg_ver + " : Minimum required records not met for current period: " + getSDI("input") + " " + _aggregatePeriodBegin  + "  MVR: " + mvr_count + " RecordCount: " + total_count);
			do_setoutput = false;
			}
			//
	//

			if (do_setoutput)  // then we are still ok so continue with the calculation
					{
					// declare some common variables to do the interpolation
					Double new_window_value; 
					Date new_window_sdt;
					long milly_diff_total;
					long milly_diff_end;
					double val_diff;
					float   percent_diff;
					double interpolated_value;
	//
			//  if we don't have a begininning period record go get the record before and interpoplate
					if (!have_beginning_record)
			{
					// now go get the last record of the previous interval
	//                   Date previousWindowSDT = new Date( _aggregatePeriodBegin.getTime() - MS_PER_HOUR);
			cal2.setTime(_aggregatePeriodBegin);
					//debug3("previous window calendar before rollback: " + cal2.toString());
			cal2.add(calIntervalRoll,-1);

					//debug3("previous window calendar: " + cal2.toString());
					Date previousWindowSDT = cal2.getTime();
					Date previousWindowEDT = previousWindowSDT;
					debug3("Interval: " + output_interval + " PWSDT: " + previousWindowSDT);
					rbu.getStandardDates(sdi,output_interval,previousWindowSDT,previousWindowEDT,dt_fmt);
					// do the first record in next interval query to get the start_date_time and value
					query = "select to_char(start_date_time,'dd-mon-yyyy HH24:MI') pwsdt, value pwdv from " +
							" ( select start_date_time,value,rank() " +
							" over(order by start_date_time desc ) rn from " + table_selector + input_interval.toLowerCase() +
							" where site_datatype_id = " + getSDI("input") +
							" and start_date_time >= " +  "to_date('" +  (String) dbobj.get("SD_SDT") +
							"','dd-mon-yyyy HH24:MI')" +
							" and start_date_time < " +  "to_date('" + (String) dbobj.get("SD_EDT")  +
							"','dd-mon-yyyy HH24:MI')) where rn = 1";
					status = db.performQuery(query,dbobj);
					debug3(" BBOP SQL STRING:" + query + "   DBOBJ: " + dbobj.toString() + "STATUS:  " + status);
					//
					if (status.startsWith("ERROR"))
					{
						warning(status);
						return;
					}

					// now see if this next interval query worked if not then we can't continue!!!
					if (((String)dbobj.get("PWDV")).length() != 0)
					{
					// now get the date, value of first record in previous interval to see if it passes muster
					new_window_value = Double.valueOf(dbobj.get("pwdv").toString());
					new_window_sdt   = new Date(dbobj.get("pwsdt").toString());
					// now do the interpolation of the eop of previous period to the BOP for this period
					milly_diff_total = date_out[1].getTime() - new_window_sdt.getTime();
					milly_diff_end = date_out[1].getTime() - _aggregatePeriodBegin.getTime();
					val_diff = value_out[1] - new_window_value;
					percent_diff =  (float) milly_diff_end / (float) milly_diff_total;
					interpolated_value = value_out[1] + (val_diff * percent_diff);
					debug3(" NWSDT: " + new_window_sdt + " NWVal: " + new_window_value  + " Val_diff: " + val_diff);
					//

	
					// now set the first value in the array to the interpolated value as well as BOP date
					value_out[0] = interpolated_value;
					date_out[0] = _aggregatePeriodBegin;
			}

			else 
			{
				debug2("TWAINTERP- " + alg_ver + " : Cannot do Computation due to lack of EOP record: " + getSDI("input") + " " + _aggregatePeriodEnd );
				do_setoutput = false;
			}


			}
	//
			//  now go get the eop record and interpolate the end value


					if (do_setoutput)  // value in the interval passed the test so continue with next step
					{ // block for getting BOP next period
					// now go get the first record of the next interval
	//                   Date nextWindowSDT = new Date( _aggregatePeriodEnd.getTime() + MS_PER_HOUR);
			cal2.setTime(_aggregatePeriodBegin);
			cal2.add(calIntervalRoll,1);

					Date nextWindowSDT = cal2.getTime();
					Date nextWindowEDT = nextWindowSDT;
					debug3("Interval: " + output_interval + " NWSDT: " + nextWindowSDT);
					rbu.getStandardDates(sdi,output_interval,nextWindowSDT,nextWindowEDT,dt_fmt);
					// do the first record in next interval query to get the start_date_time and value
					query = "select to_char(start_date_time,'dd-mon-yyyy HH24:MI') nwsdt, value nwdv from " +
							" ( select start_date_time,value,rank() " +
							" over(order by start_date_time) rn from " + table_selector + input_interval.toLowerCase() +
							" where site_datatype_id = " + getSDI("input") +
							" and start_date_time >= " +  "to_date('" +  (String) dbobj.get("SD_SDT") +
							"','dd-mon-yyyy HH24:MI')" +
							" and start_date_time < " +  "to_date('" + (String) dbobj.get("SD_EDT")  +
							"','dd-mon-yyyy HH24:MI')) where rn = 1";
					status = db.performQuery(query,dbobj);
					debug3(" NBOP SQL STRING:" + query + "   DBOBJ: " + dbobj.toString() + "STATUS:  " + status);
			//
					// now see if this next interval query worked if not then we can't continue!!!
					if (status.startsWith("ERROR")) 
			{
				warning(status);
				return;
			}
					// now see if this next interval query returned a record if not then we can't continue!!!
					if (((String)dbobj.get("NWDV")).length() != 0)
					{
					// now get the date, value of first record in next interval to see if it passes muster
					new_window_value = Double.valueOf(dbobj.get("nwdv").toString());
					new_window_sdt   = new Date(dbobj.get("nwsdt").toString());
					// now do the interpolation of the eop of this period to the BOP for next period
				debug3(new_window_sdt + "  " + index);
					milly_diff_total = new_window_sdt.getTime() - date_out[index].getTime();
					milly_diff_end = _aggregatePeriodEnd.getTime() - date_out[index].getTime();
					val_diff = new_window_value - value_out[index];
					percent_diff =  (float) milly_diff_end / (float) milly_diff_total;
					interpolated_value = value_out[index] + (val_diff * percent_diff);
					debug3(" Millydiff_T:  " + milly_diff_total  + " Millydiff_End: " + milly_diff_end  + " PERCENT_diff: " + percent_diff);
					debug3(" NWSDT: " + new_window_sdt + " NWVal: " + new_window_value  + " Val_diff: " + val_diff);
					debug3(" Interpolated Value: " + interpolated_value );
					//
	
			// now set the last value in the array to the interpolated value as well as eop date
			index++;
					value_out[index] = interpolated_value;
					date_out[index] = _aggregatePeriodEnd;
			}

			else 
			{
				debug2("TWAINTERP-"+alg_ver+": Cannot do Computation due to lack of EOP record: " + getSDI("input") + " " + _aggregatePeriodEnd );
				do_setoutput = false;
			}


					} // end of block for BOP next period
					//

			}  // end of big block to continue the calculation	

			//
			// calculate and set the weighted average if all is successful and set the flags appropriately
			if (do_setoutput) 
			{
			double weighted_average = 0.0D;
			double total_area = 0.0D;
			double time_diff  = 0.0D;
			double midpoint = 0.0D;

			// now do the area calculation:  which is the sum of the area of the midpoints of the collected
					// data points (mipoint * time) and all divided by the total time 
					for (int i =0; i < index ; i++)
			{
				debug3("Index: " + i + value_out[i] + "  " + value_out[i+1] + "  " + date_out[i+1].getTime() + "  " + date_out[i].getTime());
				midpoint =   (value_out[i+1] + value_out[i]) / 2 ;
				time_diff =  date_out[i+1].getTime() - date_out[i].getTime(); 
				total_area = total_area + (midpoint * time_diff)/MS_PER_DAY;
				debug3("Index: " + i + "  MidPt: " + midpoint + "  timediff: " + time_diff + "  T.A.: " + total_area);

			}
			time_diff = ((double) _aggregatePeriodEnd.getTime() - (double) _aggregatePeriodBegin.getTime())/(double)MS_PER_DAY;
			weighted_average = total_area / time_diff;
			debug3("TWAINTERP-"+alg_ver+": ENDTIME : " +  _aggregatePeriodEnd + "  BEGIN_TIME: " + _aggregatePeriodBegin);
			debug3("TWAINTERP-"+alg_ver+": TOTAL TIME: " + time_diff);
			debug2("TWAINTERP-"+alg_ver+": WEIGHTED_AVERAGE: " + weighted_average + " TOTALAREA: " + total_area  );
			//  set the dataflags appropriately	
			if (total_count < mvd_count) flags = flags + "n";
					if (is_current_period && total_count < mvr_count)
					//  now we have a partial calculation, so do what needs to be done for partials
					{
						setHdbValidationFlag(output,'T');
						// call the RBASEUtils merge method to add a "seed record" to cp_historic_computations table
	//   getSDI method casted to int since it was changed sometime to a long, M. Bogner Aug 2012
	// getValue method add to getSDI method because surrogate keys (like SDI) were changed to DbKey object for 5.3 CP project
						//rbu.merge_cp_hist_calc(comp.getAppId(),(int) getSDI("input"),input_interval,_aggregatePeriodBegin,
						rbu.merge_cp_hist_calc( (int) comp.getAppId().getValue(),(int) getSDI("input").getValue(),input_interval,_aggregatePeriodBegin,
						_aggregatePeriodEnd,"dd-MM-yyyy HH:mm",tsdb.getWriteModelRunId(),table_selector);

					}

			debug3("FLAGS: " + flags);
			if (flags != null) setHdbDerivationFlag(output,flags);
			//
					/* added to allow users to automatically set the Validation column  */
					if (validation_flag.length() > 0) setHdbValidationFlag(output,validation_flag.charAt(1));
					// this calculation usually gives a really big decimal portion so cut it down to 5 decimals
					DecimalFormat fiveDForm = new DecimalFormat("#.#####");
					weighted_average = Double.valueOf(fiveDForm.format(weighted_average));
			setOutput(output,weighted_average);
			}
			//
			//  delete any existing value if this calculation failed
			if (!do_setoutput)
			{
			deleteOutput(output);
			}
		}
		catch (SQLException ex)
		{
			throw new DbCompException("Unable to get sql connection.", ex);
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
