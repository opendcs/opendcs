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
// the surrogate keys where changed from a long to a DbKey object
import decodes.sql.DbKey;


//AW:IMPORTS
// Place an import statements you need here.
import java.util.TimeZone;
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
import decodes.util.DecodesSettings;
//import decodes.tsdb.DbCompConfig;
import decodes.hdb.dbutils.RBASEUtils;
//AW:IMPORTS_END

//AW:JAVADOC
/**
PowerToEnergyALg - calculates Energy base on power readings converted
to hourly Megawatt rates

Parameters:

partial_calculations: boolean: default false: if current period partial calculations will be performed
min_values_required: number: default 1: the minimum number of observations required to perform computation
min_values_desired: number: default 0: the minimum number of observations desired to perform computation
validation_flag: string: default empty: the validation flag value to be sent to the database 

 */

//AW:JAVADOC_END
public class PowerToEnergyAlg
	extends decodes.tsdb.algo.AW_AlgorithmBase
{
//AW:INPUTS
	public double input;	//AW:TYPECODE=i
	String _inputNames[] = { "input" };
//AW:INPUTS_END

//AW:LOCALVARS
	// Enter any local class variables needed by the algorithm.
// Version 1.0.04 resulted from mod for CP 3.0 Upgrade project by M. Bogner Aug 2012
// Version 1.0.05 resulted from mod for CP 5.3 Upgrade project by M. Bogner March 2013
        String alg_ver = "1.0.05";
	String query;
	boolean do_setoutput = true;
	boolean is_current_period;
	String flags;
	Connection conn = null;
	Date date_out;
	Double tally;
	int total_count;
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
		conn = null;
		date_out = null;
		tally = 0.0;
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
			tally += input;
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
		// first calculate number of days in the month in case the numbers are for month derivations
//
		//  delete any existing value if this period has no records 
		// and do nothibg else but return
		if (total_count == 0)
		{
		   debug3("PowerToEnergyAlg: No records for period: " + _aggregatePeriodEnd + " SDI: " + getSDI("input"));
		   deleteOutput(output);
                   return;
		}

		do_setoutput = true;
		ParmRef parmRef = getParmRef("input");
		if (parmRef == null) 
                {
		   warning("PowerToEnergyAlg: Unknown aggregate control output variable 'INPUT'");
                   return;
		}
		String input_interval = parmRef.compParm.getInterval();
		String table_selector = parmRef.compParm.getTableSelector();
		parmRef = getParmRef("output");
		if (parmRef == null) 
                {
		   warning("PowerToEnergyAlg: Unknown aggregate control output variable 'OUTPUT'");
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

		     warning("PowerToEnergyAlg-"+alg_ver+": Warning: Illegal negative setting of minimum values criteria for non-Month aggregates");
		     warning("PowerToEnergyAlg-"+alg_ver+": Warning: Minimum values criteria for non-Month aggregates set to 1");
		     if (mvd_count < 0) mvd_count = 1;
		     if (mvr_count < 0) mvr_count = 1;
		   }
		   if ((input_interval.equalsIgnoreCase("instant") || output_interval.equalsIgnoreCase("hour")) && mvr_count == 0) 
		   {

		     warning("PowerToEnergyAlg-"+alg_ver+": Warning: Illegal zero setting of minimum values criteria for instant/hour aggregates");
		     warning("PowerToEnergyAlg-"+alg_ver+": Warning: Minimum values criteria for instant/hour aggregates set to 1");
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
		   else if (mvr_count == 0 && !input_interval.equalsIgnoreCase("day") )
		   {
		     warning("PowerToEnergyAlg-"+alg_ver+": Warning: Illegal zero setting of minimum values criteria for " 
		     + input_interval + " to daily aggregates");
		     warning("PowerToEnergyAlg-"+alg_ver+": Warning: Minimum values criteria for daily aggregates set to 1");
		     if (mvd_count == 0) mvd_count = 1;
		     if (mvr_count == 0) mvr_count = 1;
		   }

		}
//
		// get the connection and a few other classes so we can do some sql
		conn = tsdb.getConnection();
		DBAccess db = new DBAccess(conn);
		DataObject dbobj = new DataObject();
                dbobj.put("ALG_VERSION",alg_ver);
// Cast to in  resulted from mod for CP 3.0 Upgrade project by M. Bogner Aug 2012
// call to getValue method resulted from mod for CP 5.3 Upgrade project by M. Bogner March 2013
                Integer sdi = new Integer((int) getSDI("input").getValue());
                String dt_fmt = "dd-MMM-yyyy HH:mm";
 
		RBASEUtils rbu = new RBASEUtils(dbobj,conn);
		SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");
                sdf.setTimeZone(
                        TimeZone.getTimeZone(DecodesSettings.instance().aggregateTimeZone));
 
		String status = null;
//              important that you standardize the dates based on the input interval
                debug3(" Input Interval:" + input_interval + "   PeriodBegin: " + _aggregatePeriodBegin + " PeriodEnd:  " 
		+ _aggregatePeriodEnd);

                
                rbu.getStandardDates(sdi,input_interval,_aggregatePeriodBegin,_aggregatePeriodEnd,dt_fmt);

                double average_power = tally / (double) total_count;
		//  see if we are in a current window and do the query to do the volume calculation
		String query = "select round( hdb_utilities.get_sdi_unit_factor( " + getSDI("input") + 
                        ") * hdb_utilities.get_sdi_unit_factor( " + getSDI("output") +
                        ") * 24 * " +
                   	"( to_date('" +   (String) dbobj.get("SD_EDT")  + "','dd-MM-yyyy HH24:MI') - " +
		   	"to_date('" +  (String) dbobj.get("SD_SDT") + "','dd-MM-yyyy HH24:MI')" +
		   	" ) * " + tally + ",5) energy , " + 
			"hdb_utilities.date_in_window('" + output_interval.toLowerCase() +
		        "',to_date('" +  sdf.format(_aggregatePeriodBegin) +
		        "','dd-MM-yyyy HH24:MI')) is_current_period from dual";
                // now do the query for all the needed data
		status = db.performQuery(query,dbobj);
                debug3(" SQL STRING:" + query + "   DBOBJ: " + dbobj.toString() + "STATUS:  " + status);

		// see if there was an error
		if (status.startsWith("ERROR"))
                {

		   warning(" PowerToEnergyAlg-"+alg_ver+":  Failed due to following oracle error");
		   warning(" PowerToEnergyAlg-"+alg_ver+": " +  status);
		   return;
		}
//
		debug3("PowerToEnergyAlg-"+alg_ver+ "  " + _aggregatePeriodEnd + " SDI: " + getSDI("input") + "  MVR: " + mvr_count + " RecordCount: " + total_count);
		// now see how many records were found for this aggregate
		//  and see if this calc is in current period and if partial calc is set
		is_current_period = ((String)dbobj.get("is_current_period")).equalsIgnoreCase("Y");
		if (!is_current_period && total_count < mvr_count)
                {
 		   do_setoutput = false;
		   debug1("PowerToEnergyAlg-"+alg_ver+": Minimum required records not met for historic period: " + _aggregatePeriodEnd + " SDI: " + getSDI("input") + "  MVR: " + mvr_count + " RecordCount: " + total_count);
		}
		if (is_current_period && !partial_calculations && total_count < mvr_count)
                {
 		   do_setoutput = false;
		   debug1("PowerToEnergyAlg-"+alg_ver+": Minimum required records not met for current period: " + _aggregatePeriodEnd + " SDI: " + getSDI("input") + "  MVR: " + mvr_count + " RecordCount: " + total_count);
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
                      // the following method signature was changed by M. Bogner March 2013 for the CP 5.3 project
                      // where the surrogate keys (like SDI) where changed from a long to a DbKey class
                      //rbu.merge_cp_hist_calc(comp.getAppId(),(int) getSDI("input"),input_interval,_aggregatePeriodBegin,

                      rbu.merge_cp_hist_calc( (int) comp.getAppId().getValue(),(int) getSDI("input").getValue(),input_interval,_aggregatePeriodBegin,
                      _aggregatePeriodEnd,"dd-MM-yyyy HH:mm",tsdb.getWriteModelRunId(),table_selector);

                   }

		   debug3("PowerToEnergyAlg-"+alg_ver+": Derivation FLAGS: " + flags);
		   if (flags != null) setHdbDerivationFlag(output,flags);
                   Double energy = new Double(dbobj.get("energy").toString());
		   //
                   /* added to allow users to automatically set the Validation column  */
                   if (validation_flag.length() > 0) setHdbValidationFlag(output,validation_flag.charAt(1));
		   setOutput(output,energy);
		}
//
		//  delete any existing value if this calculation failed
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
