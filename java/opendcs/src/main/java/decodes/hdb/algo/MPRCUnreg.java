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

@Algorithm(description = "Morrow Point Unregulated Inflow Computation \n" +
"Sums Blue Mesa Delta Storage and Evaporation with Taylor Park Delta Storage from t-1, \n" +
"adds it to Morrow Point Inflow to get Unregulated Inflow")
public class MPRCUnreg extends decodes.tsdb.algo.AW_AlgorithmBase
{
	@Input
	public double TPRCDeltaStorage;
	@Input
	public double BMDCDeltaStorage;
	@Input
	public double BMDCEvap;
	@Input
	public double MPRCInflow;

	@Output(type = Double.class)
	public NamedVariable unreg = new NamedVariable("unreg", 0);
	
	@PropertySpec(value = "fail") 
	public String TPRCDeltaStorage_missing = "fail";
	@PropertySpec(value = "fail") 
	public String BMDCDeltaStorage_missing = "fail";
	@PropertySpec(value = "fail") 
	public String BMDCEvap_missing 		= "fail";
	@PropertySpec(value = "fail") 
	public String MPRCInflow_missing 		= "fail";

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
		sum = TPRCDeltaStorage;
		sum += BMDCDeltaStorage + BMDCEvap;
		
debug3("doAWTimeSlice, TPRCDeltaStorage="+TPRCDeltaStorage+
		" BMDCDeltaStorage="+BMDCDeltaStorage+" BMDCEvap="+BMDCEvap+" MPRCInflow="+MPRCInflow+" sum="+sum);

		setOutput(unreg, MPRCInflow + sum);
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	@Override
	protected void afterTimeSlices()
	{
	}
}
