package decodes.tsdb.algo;
 
import java.util.Date;
 
import ilex.var.NamedVariableList;
import ilex.var.NamedVariable;
import decodes.tsdb.DbAlgorithmExecutive;
import decodes.tsdb.DbCompException;
import decodes.tsdb.DbIoException;
import decodes.tsdb.VarFlags;
import decodes.tsdb.algo.AWAlgoType;
 
//AW:IMPORTS
// Place an import statements you need here.
import decodes.cwms.CwmsFlags;
//AW:IMPORTS_END
 
//AW:JAVADOC
/**
    Calculates reservoir input with the equation
 *  inflow = Storage + Outflow
 *  
 *  Outflow should be provided in cfs
 * Storage  should be provided in ac-ft and will be converted to cfs based on the
 * interval of data.
 * ( this comp will need to be created 3 times for each project, 15minutes, 1hour, and 1day )
 * there may be a way to group things
 * 
 * Storage should be defined in the algorithm record as a delta type with one o the following
 * intervals: 15minutes, 1hour, 1day.
 *
 * NOTE: there are ac-ft to cfs conversions build into this comp, do NOT use metric input, the comp
 *       will provide bogus results.
 */
//AW:JAVADOC_END
public class FlowResIn
	extends decodes.tsdb.algo.AW_AlgorithmBase
{
//AW:INPUTS
	public double ResOut;	//AW:TYPECODE=i
	public double Dstor;	//AW:TYPECODE=id
	String _inputNames[] = { "ResOut", "Dstor" };
//AW:INPUTS_END
 
//AW:LOCALVARS
	// Enter any local class variables needed by the algorithm.
    int inCount;
    double inTotal;
//AW:LOCALVARS_END
 
//AW:OUTPUTS
	public NamedVariable ResIn = new NamedVariable("ResIn", 0);
	String _outputNames[] = { "ResIn" };
//AW:OUTPUTS_END
 
//AW:PROPERTIES
	public long averageSamples = 1;
	String _propertyNames[] = { "averageSamples"};
//AW:PROPERTIES_END
        
 
	// Allow javac to generate a no-args constructor.
 
	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	protected void initAWAlgorithm( )
		throws DbCompException
	{
//AW:INIT
		_awAlgoType = AWAlgoType.TIME_SLICE;
//AW:INIT_END
 
//AW:USERINIT
		// Code here will be run once, after the algorithm object is created.
		inCount=0;
        inTotal=0;
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
		inCount=0;
	        inTotal=0;
//AW:BEFORE_TIMESLICES_END
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
//AW:TIMESLICE
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
//AW:TIMESLICE_END
	}
 
	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
		throws DbCompException
	{
//AW:AFTER_TIMESLICES
		// This code will be executed once after each group of time slices.
		// For TimeSlice algorithms this is done once after all slices.
		// For Aggregating algorithms, this is done after each aggregate
		// period.
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
