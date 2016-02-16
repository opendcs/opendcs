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
// this new import was added by M. Bogner March 2013 for the 5.3 CP upgrade project
// new class handles surrogate keys as an object
import decodes.sql.DbKey;


//AW:IMPORTS
// Place an import statements you need here.
import decodes.tsdb.ParmRef;
import java.util.Calendar;
import java.util.GregorianCalendar;
import decodes.hdb.HdbFlags;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import ilex.util.DatePair;
import decodes.tsdb.ParmRef;
import decodes.hdb.dbutils.DBAccess;
import decodes.hdb.dbutils.DataObject;
import decodes.tsdb.DbCompException;
import decodes.hdb.dbutils.RBASEUtils;
//AW:IMPORTS_END

//AW:JAVADOC
/**
Type a javadoc-style comment describing the algorithm class.
 */
//AW:JAVADOC_END
public class EOPInterpAlg
	extends decodes.tsdb.algo.AW_AlgorithmBase
{
//AW:INPUTS
	public double input;	//AW:TYPECODE=i
	String _inputNames[] = { "input" };
//AW:INPUTS_END

//AW:LOCALVARS
	// Enter any local class variables needed by the algorithm.
//  Alg version 1.02 mods for CP upgrade 3.0  By M. Bogner Aug 2012
//  Alg version 1.03 mods for CP upgrade 5.3  By M. Bogner March 2013 for Dbkey change
        String alg_ver = "1.0.03";
	double value_out ;
	boolean do_setoutput = true;
	Date date_out;
	Connection conn = null;
	int total_count = 0;

//AW:LOCALVARS_END

//AW:OUTPUTS
	public NamedVariable output = new NamedVariable("output", 0);
	String _outputNames[] = { "output" };
//AW:OUTPUTS_END

//AW:PROPERTIES
	public long desired_window_period = 0;
	public long req_window_period = 0;
        public String validation_flag = "";
	String _propertyNames[] = { "desired_window_period", "req_window_period", "validation_flag" };
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
		date_out = null;
		value_out = 0D;
		do_setoutput = true;
		total_count = 0;
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
		if (!isMissing(input))
		{
			value_out = input;
			date_out = _timeSliceBaseTime;
                        debug2("EOPINTERP- " + alg_ver + "  TimeSlice  VALUE: " + input);
                        total_count++;
		}
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
		//
		//
		//  now if there is no records	then delete output it if it exists
		if (total_count == 0)
		{
		    deleteOutput(output);
		    return;
		}
//
		// first see if the closest EOP record is within the windowing specs
//
		debug2 ("EOPINTERP- " + alg_ver + "  Period_BEG:  " + _aggregatePeriodBegin + "  Period_END: " + _aggregatePeriodEnd + "  Last Date:  " + date_out);
		long milly_diff = _aggregatePeriodEnd.getTime() - date_out.getTime();
		long milly_window = 0;
		Double new_window_value = 0D;
		Date   new_window_sdt = null;
		DataObject dbobj = new DataObject();
//



		ParmRef parmRef = getParmRef("output");
		if (parmRef == null) warning("Unknown aggregate control output variable 'OUTPUT'");
		String intstr_out = parmRef.compParm.getInterval();
		//  now check to see if before EOP record within required window
		if (intstr_out.equalsIgnoreCase("hour"))
		     milly_window = req_window_period * (MS_PER_HOUR / 60L);
		  else if (intstr_out.equalsIgnoreCase("day"))
		     milly_window = req_window_period * MS_PER_HOUR;
		  else if (intstr_out.equalsIgnoreCase("month"))
		     milly_window = req_window_period * MS_PER_DAY;
		  else if (intstr_out.equalsIgnoreCase("year"))
		     milly_window = req_window_period * MS_PER_DAY * 31;
		  else if (intstr_out.equalsIgnoreCase("wy"))
		     milly_window = req_window_period * MS_PER_DAY * 31;
		if ((milly_diff > milly_window) && (req_window_period != 0)) 
		{
		 do_setoutput = false;
		 debug2("EOPINTERP- " + alg_ver + " : SETTING OUTPUT FLAG TO FALSE");
		}
		//  now check to see if before EOP record within desired window
		if (intstr_out.equalsIgnoreCase("hour"))
		     milly_window = desired_window_period * (MS_PER_HOUR / 60L);
		  else if (intstr_out.equalsIgnoreCase("day"))
		     milly_window = desired_window_period * MS_PER_HOUR;
		  else if (intstr_out.equalsIgnoreCase("month"))
		     milly_window = desired_window_period * MS_PER_DAY;
		  else if (intstr_out.equalsIgnoreCase("year"))
		     milly_window = desired_window_period * MS_PER_DAY * 31;
		  else if (intstr_out.equalsIgnoreCase("wy"))
		     milly_window = desired_window_period * MS_PER_DAY * 31;
		if ((milly_diff > milly_window) && (desired_window_period != 0)) 
		{
			//  set the data flags to w
			setHdbDerivationFlag(output,"w");
		}
		//
		debug2("EOPINTERP- " + alg_ver + " WINDOW: " + milly_window + "  DIFF: " + milly_diff + "PERIOD: " + desired_window_period);
		//
		if (do_setoutput)  // value in the interval passed the test so continue with next step
		{ // block for getting BOP next period
//
		// now go get the first record of the next interval
                GregorianCalendar cal2 = new GregorianCalendar();
                int calIntervalRoll = 0;
// set the calendar interval field to use to roll the calendar back and forward
                if (intstr_out.equalsIgnoreCase("hour")) calIntervalRoll = Calendar.HOUR_OF_DAY;
                if (intstr_out.equalsIgnoreCase("day"))  calIntervalRoll =  Calendar.DAY_OF_MONTH;
                if (intstr_out.equalsIgnoreCase("year")) calIntervalRoll =  Calendar.YEAR;
                if (intstr_out.equalsIgnoreCase("month")) calIntervalRoll =  Calendar.MONTH;

		cal2.setTime(_aggregatePeriodBegin);
		//debug3("EOPINTERP- " + alg_ver + " window calendar before  forward roll: " + cal2.toString());
		cal2.add(calIntervalRoll,1);

		//debug3("EOPINTERP- " + alg_ver + " next  window calendar: " + cal2.toString());
		Date nextWindowSDT = cal2.getTime();
		// line removed to allow other roll forwards besides just an hour MAB 10/29/10
		//Date nextWindowSDT = new Date( _aggregatePeriodEnd.getTime() + MS_PER_HOUR);
		Date nextWindowEDT = nextWindowSDT; 
//   mod by M. Bogner for CP upgrade 3.0.  get SDI must have been changed to a long so
//   I cast it to an int
//   mod March 2013 by M. Bogner for CP upgrade 5.3.  get SDI must have been changed to an object so
//   I cast it to an int after I get a long with the getValue method
		Integer sdi = new Integer( (int) getSDI("input").getValue());
		conn = tsdb.getConnection();
		String dt_fmt = "dd-MMM-yyyy HH:mm";
		RBASEUtils rbu = new RBASEUtils(dbobj,conn);
		debug2("EOPINTERP- " + alg_ver + "  Interval: " + intstr_out + " NWSDT: " + nextWindowSDT);
		
		rbu.getStandardDates(sdi,intstr_out,nextWindowSDT,nextWindowEDT,dt_fmt);
		
		
		SimpleDateFormat sdf = new SimpleDateFormat(dt_fmt);
		do_setoutput = true;
		parmRef = getParmRef("input");
		if (parmRef == null) warning("Unknown aggregate control output variable 'INPUT'");
		String input_interval = parmRef.compParm.getInterval();
                String table_selector = parmRef.compParm.getTableSelector();

		String status = null;
		DBAccess db = new DBAccess(conn);
		// do the first record in next interval query to get the start_date_time and value 
		String query = "select to_char(start_date_time,'dd-mon-yyyy HH24:MI') nwsdt, value nwdv from " +
			" ( select start_date_time,value,rank() " +
			" over(order by start_date_time) rn from " + table_selector + input_interval.toLowerCase() +
			" where site_datatype_id = " + getSDI("input") +
		    " and start_date_time >= " +  "to_date('" +  (String) dbobj.get("SD_SDT") +
			"','dd-mon-yyyy HH24:MI')" + 
		    " and start_date_time < " +  "to_date('" + (String) dbobj.get("SD_EDT")  +
		 "','dd-mon-yyyy HH24:MI')) where rn = 1";
		status = db.performQuery(query,dbobj);
		debug2("EOPINTERP- " + alg_ver + " SQL STRING:" + query + "   DBOBJ: " + dbobj.toString() + "STATUS:  " + status);
		// now see if this next interval query worked if not then we can't continue!!!
		if (((String)dbobj.get("NWDV")).length() == 0)
		{
		    do_setoutput = false;
                    debug2("EOPINTERP- " + alg_ver + " : Cannot do Computation due to lack of EOP record: " + getSDI("input") + " " + _aggregatePeriodEnd );

		}
		if (status.startsWith("ERROR"))
                {
		   warning("EOPInterpAlg terminated due to following ORACLE ERROR");
                   warning(status);
		   return;
 		}
		} // end of block for BOP next period
		//
		//  now continue with calculation if do_setoutput is still true
		if (do_setoutput) // we have a good record and a next record so do the calculation 
		{  
			debug2 ("EOPINTERP- " + alg_ver + "  " + dbobj.toString());
			//
			// now get the date, value of first record in next interval to see if it passes muster
			new_window_value = new Double(dbobj.get("nwdv").toString());
			new_window_sdt   = new Date(dbobj.get("nwsdt").toString());
			milly_diff = new_window_sdt.getTime() - _aggregatePeriodEnd.getTime(); 
			milly_window = 0;
			//  now check to see if next window  BOP record within required window
			if (intstr_out.equalsIgnoreCase("hour"))
		     		milly_window = req_window_period * (MS_PER_HOUR / 60L);
		  	else if (intstr_out.equalsIgnoreCase("day"))
		     		milly_window = req_window_period * MS_PER_HOUR;
		  	else if (intstr_out.equalsIgnoreCase("month"))
		     		milly_window = req_window_period * MS_PER_DAY;
		  	else if (intstr_out.equalsIgnoreCase("year"))
		     		milly_window = req_window_period * MS_PER_DAY * 31;
		  	else if (intstr_out.equalsIgnoreCase("wy"))
		     		milly_window = req_window_period * MS_PER_DAY * 31;
			if ((milly_diff > milly_window) && (req_window_period != 0)) 
			{
		 	   do_setoutput = false;
			   debug1("EOPINTERP- " + alg_ver + " : OUTPUT FALSE DUE TO WINDOW EXCEEDED:  " + _aggregatePeriodBegin + "  SDI: " + getSDI("input"));
			}
			//  now check to see if the after EOP record within desired window
			if (intstr_out.equalsIgnoreCase("hour"))
		     		milly_window = desired_window_period * (MS_PER_HOUR / 60L);
		  	else if (intstr_out.equalsIgnoreCase("day"))
		     		milly_window = desired_window_period * MS_PER_HOUR;
		  	else if (intstr_out.equalsIgnoreCase("month"))
		     		milly_window = desired_window_period * MS_PER_DAY;
		  	else if (intstr_out.equalsIgnoreCase("year"))
		     		milly_window = desired_window_period * MS_PER_DAY * 31;
		  	else if (intstr_out.equalsIgnoreCase("wy"))
		     		milly_window = desired_window_period * MS_PER_DAY * 31;
			if ((milly_diff > milly_window) && (desired_window_period != 0)) 
			{
				//  set the data flags to w
				setHdbDerivationFlag(output,"w");
			}
		}   // end of the block that we have a good record to compute from
		 
		//
		if (do_setoutput)
		{
		        debug2("EOPINTERP- " + alg_ver + ": SETTING OUTPUT: DOING A SETOutput");
			// now do the interpolation of the eop of this period to the BOP for next period
			long milly_diff_total = new_window_sdt.getTime() - date_out.getTime();
			long milly_diff_end = _aggregatePeriodEnd.getTime() - date_out.getTime();
			double val_diff = new_window_value - value_out;
			float	percent_diff =  (float) milly_diff_end / (float) milly_diff_total;
			value_out = value_out + (val_diff * percent_diff);
			debug2("EOPINTERP- " + alg_ver + " NWSDT: " + new_window_sdt + " NWVal: " + new_window_value  + " Val_diff: " + val_diff);
			//
                        /* added to allow users to automatically set the Validation column  */
                        if (validation_flag.length() > 0) setHdbValidationFlag(output,validation_flag.charAt(1));
			//
		    	// now the new value has been calculated so set it and we are done
			setOutput(output,value_out);
		}
		//
		//  now if there is no record to output then delete it if it exists
		if (!do_setoutput)
		{
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
}
