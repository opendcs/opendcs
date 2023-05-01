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

//AW:IMPORTS
//AW:IMPORTS_END

//AW:JAVADOC
/**
Blue Mesa Unregulated Inflow Computation
Takes Taylor Park Delta Storage from t-1 and
adds it to Blue Mesa Inflow to get Unregulated Inflow
 */
//AW:JAVADOC_END
public class BMDCUnreg extends decodes.tsdb.algo.AW_AlgorithmBase
{
//AW:INPUTS
	public double TPRCDeltaStorage;	//AW:TYPECODE=i
	public double BMDCInflow;			//AW:TYPECODE=i
	
	String _inputNames[] = { "TPRCDeltaStorage", "BMDCInflow" };
//AW:INPUTS_END

//AW:LOCALVARS

//AW:LOCALVARS_END

//AW:OUTPUTS
	public NamedVariable unreg = new NamedVariable("unreg", 0);
	String _outputNames[] = { "unreg" };
//AW:OUTPUTS_END

//AW:PROPERTIES
	public String TPRCDeltaStorage_missing = "fail";
	public String BMDCInflow_missing		= "fail";

	String _propertyNames[] = { "TPRCDeltaStorage_missing", "BMDCInflow_missing"};
//AW:PROPERTIES_END

	// Allow javac to generate a no-args constructor.

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	protected void initAWAlgorithm( )
	{
//AW:INIT
		_awAlgoType = AWAlgoType.TIME_SLICE;
//AW:INIT_END

//AW:USERINIT
//AW:USERINIT_END
	}
	
	/**
	 * This method is called once before iterating all time slices.
	 */
	protected void beforeTimeSlices()
	{
//AW:BEFORE_TIMESLICES
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
		double sum = 0.0;
		sum = TPRCDeltaStorage;
		
debug3("doAWTimeSlice, TPRCDeltaStorage="+TPRCDeltaStorage+
		" BMDCInflow="+BMDCInflow+" sum="+sum);

		setOutput(unreg, BMDCInflow + sum);
//AW:TIMESLICE_END
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
	{
//AW:AFTER_TIMESLICES
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
