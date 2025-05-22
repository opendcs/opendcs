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
import org.opendcs.annotations.PropertySpec;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;


@Algorithm(description = "Flaming Gorge Unregulated Inflow Computation\n" +
"Takes Fontenelle Delta Storage and Evap from t-1 and\n" +
"adds them to Flaming Gorge Inflow to get Unregulated Inflow"
public class FLGUUnreg extends decodes.tsdb.algo.AW_AlgorithmBase
{
	@Input
	public double FTRWDeltaStorage;
	@Input
	public double FTRWEvap;
	@Input
	public double FLGUInflow;
	
	@Output(type = Double.class)
	public NamedVariable unreg = new NamedVariable("unreg", 0);

	@PropertySpec(value = "fail")
	public String FTRWDeltaStorage_missing = "fail";
	@PropertySpec(value = "fail")
	public String FTRWEvap_missing 		= "fail";
	@PropertySpec(value = "fail")
	public String FLGUInflow_missing		= "fail";

	// Allow javac to generate a no-args constructor.

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	@Override
	protected void initAWAlgorithm( )
	{
		_awAlgoType = AWAlgoType.TIME_SLICE;
	}
	
	/**
	 * This method is called once before iterating all time slices.
	 */
	@Override
	protected void beforeTimeSlices()
	{
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
	@Override
	protected void doAWTimeSlice()
		throws DbCompException
	{
		double sum = 0.0;
		sum = FTRWDeltaStorage + FTRWEvap;
		
debug3("doAWTimeSlice, sum=" + sum + " unreg =" + (FLGUInflow + sum));

		setOutput(unreg, FLGUInflow + sum);
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	@Override
	protected void afterTimeSlices()
	{
	}
}
