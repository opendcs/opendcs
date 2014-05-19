package decodes.tsdb.algo;

import java.util.Date;

import ilex.var.NamedVariableList;
import ilex.var.NamedVariable;
import decodes.tsdb.DbAlgorithmExecutive;
import decodes.tsdb.DbCompException;
import decodes.tsdb.DbIoException;
import decodes.tsdb.VarFlags;

//AW:IMPORTS
// Place an import statements you need here.
//AW:IMPORTS_END

//AW:JAVADOC
/**
Estimate inflow over the interval from reservoir storage and outflow.
Estimated Inflow = delta(Storage) + AverageOutflow over period.
Inputs are: Storage (in cubic meters), and outflow (in cubic meters per second)
Output is estimated inflow (in cubic meters per second)

 */
//AW:JAVADOC_END
public class EstimatedInflow
	extends decodes.tsdb.algo.AW_AlgorithmBase
{
//AW:INPUTS
	public double storage;	//AW:TYPECODE=i
	public double outflow;	//AW:TYPECODE=i
	String _inputNames[] = { "storage", "outflow" };
//AW:INPUTS_END

//AW:LOCALVARS
	double store_t0 = 0.;
	double store_t1 = 0.;
	double out_t0 = 0.;
	double out_t1 = 0.;
	boolean firstInPeriod = true;
	int numSlices = 0;
	Date first = null;
	Date last = null;

//AW:LOCALVARS_END

//AW:OUTPUTS
	public NamedVariable inflow = new NamedVariable("inflow", 0);
	String _outputNames[] = { "inflow" };
//AW:OUTPUTS_END

//AW:PROPERTIES
	public boolean aggUpperBoundClosed = true;
	public boolean aggLowerBoundClosed = true;
	String _propertyNames[] = { "aggUpperBoundClosed", "aggLowerBoundClosed" };
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
		_aggPeriodVarRoleName = "inflow";
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
		firstInPeriod = true;
		numSlices = 0;
		first = null;
		last = null;
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
		numSlices++;
		if (firstInPeriod)
		{
			store_t0 = storage;
			out_t0 = outflow;
			first = _timeSliceBaseTime;
			firstInPeriod = false;
		}
		else
		{
			store_t1 = storage;
			out_t1 = outflow;
			last = _timeSliceBaseTime;
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
		if (numSlices >= 2)
		{
			// Determine # of seconds in the aggregate period
			long seconds = (last.getTime() - first.getTime())
				/ 1000L;
debug3("Period Start=" + first
+ ", Period End=" + last
+ ", elapsed seconds = " + seconds);
debug3("First Sample at " + first + ", storage=" + store_t0 + ", outflow=" + out_t0);
debug3(" Last Sample at " + last  + ", storage=" + store_t1 + ", outflow=" + out_t1);
			// Get delta-storage (cubic meters):
			double dStore = (store_t1 - store_t0);
			// Convert output discharge Q to volume over period
			// (cubic meters / sec ===> cubic meters)
			// Then take the average from the start to end of period.
			double ave_out = .5 * (out_t0*seconds + out_t1*seconds);
debug3("change in storage is " + dStore
+ ", average outflow over period is " + ave_out);
			// Add in the storage, and convert to meters per second.
			double volume = dStore + ave_out;
			double discharge = volume / seconds;
			
			// Output the estimated inflow in cubic meters.
			setOutput(inflow, discharge);
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
