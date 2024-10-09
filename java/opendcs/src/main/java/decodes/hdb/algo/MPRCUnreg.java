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
Morrow Point Unregulated Inflow Computation
Sums Blue Mesa Delta Storage and Evaporation with Taylor Park Delta Storage from t-1,
adds it to Morrow Point Inflow to get Unregulated Inflow
 */
//AW:JAVADOC_END
public class MPRCUnreg extends decodes.tsdb.algo.AW_AlgorithmBase
{
//AW:INPUTS
	public double TPRCDeltaStorage;	//AW:TYPECODE=i
	public double BMDCDeltaStorage;	//AW:TYPECODE=i
	public double BMDCEvap;			//AW:TYPECODE=i
	public double MPRCInflow;			//AW:TYPECODE=i
	
	String _inputNames[] = { "TPRCDeltaStorage", "BMDCDeltaStorage", "BMDCEvap", "MPRCInflow" };
//AW:INPUTS_END

//AW:LOCALVARS

//AW:LOCALVARS_END

//AW:OUTPUTS
	public NamedVariable unreg = new NamedVariable("unreg", 0);
	String _outputNames[] = { "unreg" };
//AW:OUTPUTS_END

//AW:PROPERTIES
	public String TPRCDeltaStorage_missing = "fail";
	public String BMDCDeltaStorage_missing = "fail";
	public String BMDCEvap_missing 		= "fail";
	public String MPRCInflow_missing 		= "fail";
	String _propertyNames[] = { "TPRCDeltaStorage_missing", "BMDCDeltaStorage_missing", "BMDCEvap_missing", "MPRCInflow_missing"};
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
		sum += BMDCDeltaStorage + BMDCEvap;
		
debug3("doAWTimeSlice, TPRCDeltaStorage="+TPRCDeltaStorage+
		" BMDCDeltaStorage="+BMDCDeltaStorage+" BMDCEvap="+BMDCEvap+" MPRCInflow="+MPRCInflow+" sum="+sum);

		setOutput(unreg, MPRCInflow + sum);
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
