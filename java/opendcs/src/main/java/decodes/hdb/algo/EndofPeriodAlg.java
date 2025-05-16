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
import org.opendcs.annotations.PropertySpec;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;
import decodes.tsdb.ParmRef;


@Algorithm(description = "Copies input to output at EndofPeriod based on interval window")
public class EndofPeriodAlg extends decodes.tsdb.algo.AW_AlgorithmBase
{
	@Input
	public double input;

	// Enter any local class variables needed by the algorithm.
	double value_out;
	boolean do_setoutput = true;
	Date date_out;
	int total_count;

	@Output(type = Double.class)
	public NamedVariable output = new NamedVariable("output", 0);
	
	@PropertySpec(value = "0")
	public long desired_window_period = 0;
	@PropertySpec(value = "0")
	public long req_window_period = 0;
	@PropertySpec(value = "")
	public String validation_flag = "";

	// Allow javac to generate a no-args constructor.

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	protected void initAWAlgorithm() throws DbCompException
	{
		_awAlgoType = AWAlgoType.AGGREGATING;
		_aggPeriodVarRoleName = "output";
	}

	/**
	 * This method is called once before iterating all time slices.
	 */
	protected void beforeTimeSlices() throws DbCompException
	{
		// This code will be executed once before each group of time slices.
		// For TimeSlice algorithms this is done once before all slices.
		// For Aggregating algorithms, this is done before each aggregate
		// period.
		date_out = null;
		value_out = 0D;
		do_setoutput = true;
		total_count = 0;
	}

	/**
	 * Do the algorithm for a single time slice. AW will fill in user-supplied
	 * code here. Base class will set inputs prior to calling this method. User
	 * code should call one of the setOutput methods for a time-slice output
	 * variable.
	 *
	 * @throw DbCompException (or subclass thereof) if execution of this
	 *        algorithm is to be aborted.
	 */
	protected void doAWTimeSlice() throws DbCompException
	{
		// Enter code to be executed at each time-slice.
		if (!isMissing(input))
		{
			value_out = input;
			date_out = _timeSliceBaseTime;
			total_count++;
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
		//
		// delete any existing value if this period has no records
		// and do nothibg else but return
		if (total_count == 0)
		{
			debug3(" EndofPeriodALg: No records found for this period: " + _aggregatePeriodEnd + "  SDI: "
				+ getSDI("input"));
			deleteOutput(output);
			return;
		}

		debug3("EndofPeriodALg:" + "  aggLowerBoundClosed: " + aggLowerBoundClosed
			+ "  aggUpperBoundClosed: " + aggUpperBoundClosed);
		debug3("EndofPeriodALg:" + "  aggregatePeriodBegin: " + debugSdf.format(_aggregatePeriodBegin)
			+ "  aggregatePeriodEnd: " + debugSdf.format(_aggregatePeriodEnd));
		debug3(" EndofPeriodALg: Last found for this period: " + debugSdf.format(date_out) + "  SDI: " + getSDI("input"));

		long milly_diff = _aggregatePeriodEnd.getTime() - date_out.getTime();
		long milly_window = 0;
		ParmRef parmRef = getParmRef("output");
		if (parmRef == null)
			warning("Unknown aggregate control output variable 'OUTPUT'");
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
			debug1(" EndofPeriodALg: OUTPUT FALSE DUE to Window exceeded: " + _aggregatePeriodEnd + "  SDI: "
				+ getSDI("input"));
		}
		// now check to see if record within desired window
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
			// set the data flags to w
			setHdbDerivationFlag(output, "w");
		}
		debug3("WINDOW: " + milly_window + "  DIFF: " + milly_diff + "PERIOD: " + desired_window_period);
		if (do_setoutput)
		{
			debug3(" EndofPeriod : SETTING OUTPUT: DOING A SETOutput");
			/* added to allow users to automatically set the Validation column */
			if (validation_flag.length() > 0)
				setHdbValidationFlag(output, validation_flag.charAt(1));
			setOutput(output, value_out);
		}
		// now if there is no record to output then delete if it exists
		if (!do_setoutput)
		{
			deleteOutput(output);
		}
	}
}
