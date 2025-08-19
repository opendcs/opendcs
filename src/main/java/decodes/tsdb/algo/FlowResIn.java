package decodes.tsdb.algo;

import decodes.tsdb.DbCompException;
import ilex.var.NamedVariable;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;
@Algorithm(description = "Calculates reservoir input with the equation\n" +
		"  inflow = Storage + Outflow\n" +
		"\n" +
		"Outflow should be provided in cfs\n" +
		"Storage  should be provided in ac-ft and will be converted to cfs based on the\n" +
		"interval of data.\n" +
		"( this comp will need to be created 3 times for each project, 15minutes, 1hour, and 1day )\n" +
		"there may be a way to group things\n" +
		"\n" +
		"Storage should be defined in the algorithm record as a delta type with one o the following\n" +
		"intervals: 15minutes, 1hour, 1day.\n" +
		" NOTE: there are ac-ft to cfs conversions build into this comp, do NOT use metric input, the comp\n" +
		"       will provide bogus results.\n")
public class FlowResIn
	extends decodes.tsdb.algo.AW_AlgorithmBase
{
	@Input
	public double ResOut;
	@Input
	public double Dstor;

	// Enter any local class variables needed by the algorithm.
    int inCount;
    double inTotal;

	@Output
	public NamedVariable ResIn = new NamedVariable("ResIn", 0);

	@org.opendcs.annotations.PropertySpec(value="1")
	public long averageSamples = 1;

 
	// Allow javac to generate a no-args constructor.
 
	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	protected void initAWAlgorithm( )
		throws DbCompException
	{
		_awAlgoType = AWAlgoType.TIME_SLICE;
		// Code here will be run once, after the algorithm object is created.
		inCount=0;
        inTotal=0;
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
		inCount=0;
		inTotal=0;
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
	protected void doAWTimeSlice()
		throws DbCompException
	{
		double in = 0;
		double conversion = 1.0;
		String interval = this.getInterval("ResOut").toLowerCase();
		debug3("Finding conversion for internval: " + interval);
		if (interval.equals("15minutes"))
		{
			// y ac-ft = x cfs * 900 sec * 2.29568411*10^-5 ac-ft/ft^3
			// 900 seconds = 15 minutes
			// we are converting to cfs from ac-ft so divided by this factor
			conversion = 1 / .020618557;
		}
		else if (interval.equals("1hour"))
		{
			conversion = 12.1;
		}
		else if (interval.equals("1day") || interval.equals("~1day"))
		{
			conversion = 0.50417;
		}
		debug3("Conversion factor = " + conversion);
		debug3("Dstor = " + Dstor + ", Outflow = " + ResOut);
		debug3(" Dstor  in cfs = " + (Dstor * conversion));
		// The 15 minute and hourly calculation do not use evap
		in = Dstor * conversion + ResOut;
		if (in < 0)
		{
			warning("Inflow set to 0 because the calculation came out to less than 0");
			in = 0;
		}
		else
		{
			inCount++;
			inTotal += in;
		}
		debug3("In = " + in + "Total = " + inTotal + "Count = " + inCount + "ave " + averageSamples);
		if (inCount > 0)
			in = inTotal / inCount;
		setOutput(ResIn, in);
		if (inCount >= averageSamples)
		{
			inCount = 0;
			inTotal = 0;
		}
	}
 
	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
		throws DbCompException
	{
		// This code will be executed once after each group of time slices.
		// For TimeSlice algorithms this is done once after all slices.
		// For Aggregating algorithms, this is done after each aggregate
		// period.
	}
}
