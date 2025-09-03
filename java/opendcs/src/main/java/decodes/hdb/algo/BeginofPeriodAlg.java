package decodes.hdb.algo;

import java.util.Date;

import ilex.var.NamedVariable;
import decodes.tsdb.DbCompException;
// this new import was added by M. Bogner Aug 2012 for the 3.0 CP upgrade project
import decodes.tsdb.algo.AWAlgoType;
// this new import was added by M. Bogner March 2013 for the 5.3 CP upgrade project
// new class handles surrogate keys as an object
import org.opendcs.annotations.PropertySpec;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.tsdb.ParmRef;

@Algorithm(description = "Type a javadoc-style comment describing the algorithm class.")
public class BeginofPeriodAlg extends decodes.tsdb.algo.AW_AlgorithmBase
{	
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	@Input
	public double input;

	double value_out;
	int count = 0;
	boolean do_setoutput = true;
	Date date_out;

	@Output(type = Double.class)
	public NamedVariable output = new NamedVariable("output", 0);

	@PropertySpec(value = "0") 
	public long req_window_period = 0;
	@PropertySpec(value = "0") 
	public long desired_window_period = 0;
	@PropertySpec(value = "")
	public String validation_flag = "";

	// Allow javac to generate a no-args constructor.

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	protected void initAWAlgorithm( )
		throws DbCompException
	{
		_awAlgoType = AWAlgoType.AGGREGATING;
		_aggPeriodVarRoleName = "output";
	}
	
	/**
	 * This method is called once before iterating all time slices.
	 */
	protected void beforeTimeSlices()
		throws DbCompException
	{
		// This code will be executed once before each group of time slices.
		// For TimeSlice algorithms this is done once before all slices.
		// For Aggregating algorithms, this is done before each aggregate
		// period.
		count = 0;
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
		// Enter code to be executed at each time-slice.
		if ((count == 0) && (!isMissing(input)))
		{
		log.trace("BeginofPeriodAlg: FOUND first record");
			value_out = input;
			date_out = _timeSliceBaseTime;
			count++;
		}
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
	{
		// This code will be executed once after each group of time slices.
		// For TimeSlice algorithms this is done once after all slices.
		// For Aggregating algorithms, this is done after each aggregate
		// period.

		//  now if there is no record s to evaluate then delete if it exists and return
		if (count == 0)
		{
		    deleteOutput(output);
		    return;
		}

		long milly_diff = date_out.getTime() - _aggregatePeriodBegin.getTime();
		long milly_window = 0;
		ParmRef parmRef = getParmRef("output");
		if (parmRef == null)
		{ 
			log.warn("Unknown aggregate control output variable 'OUTPUT'");
		}
		String intstr = parmRef.compParm.getInterval();
		if (intstr.equalsIgnoreCase("hour"))
		     milly_window = req_window_period * (MS_PER_HOUR / 60L);
		  else if (intstr.equalsIgnoreCase("day"))
		     milly_window = req_window_period * MS_PER_HOUR;
		  else if (intstr.equalsIgnoreCase("month"))
		     milly_window = req_window_period * MS_PER_DAY;
		  else if (intstr.equalsIgnoreCase("year"))
		     milly_window = req_window_period * MS_PER_DAY * 31;
		  else if (intstr.equalsIgnoreCase("wy"))
		     milly_window = req_window_period * MS_PER_DAY * 31;
		if ((milly_diff > milly_window) && (req_window_period != 0)) 
		{
		 do_setoutput = false;
		 log.debug("BeginofPeriodAlg: NO OUTPUT: {} SDI: {}", _aggregatePeriodBegin, getSDI("input"));
		}
		//  now check to see if record within desired window
		if (intstr.equalsIgnoreCase("hour"))
		     milly_window = desired_window_period * (MS_PER_HOUR / 60L);
		  else if (intstr.equalsIgnoreCase("day"))
		     milly_window = desired_window_period * MS_PER_HOUR;
		  else if (intstr.equalsIgnoreCase("month"))
		     milly_window = desired_window_period * MS_PER_DAY;
		  else if (intstr.equalsIgnoreCase("year"))
		     milly_window = desired_window_period * MS_PER_DAY * 31;
		  else if (intstr.equalsIgnoreCase("wy"))
		     milly_window = desired_window_period * MS_PER_DAY * 31;
		if ((milly_diff > milly_window) && (desired_window_period != 0)) 
		{
		//  set the data flags to w
		setHdbDerivationFlag(output,"w");
		}
		log.trace("BeginofPeriodAlg:  WINDOW: {}  DIFF: {} PERIOD: {}", milly_window, milly_diff, req_window_period);
		if (do_setoutput)
		{
		    /* added to allow users to automatically set the Validation column  */
		    if (validation_flag.length() > 0) setHdbValidationFlag(output,validation_flag.charAt(1));
		    log.trace("BeginofPeriodAlg: SETTING OUTPUT: DOING A SETOutput");
		    setOutput(output,value_out);
		}
		//  now if there is no record to output then delete if it exists
		if (!do_setoutput)
		{
		    deleteOutput(output);
		}
	}
}
