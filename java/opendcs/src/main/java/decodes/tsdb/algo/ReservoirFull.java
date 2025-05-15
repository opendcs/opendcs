package decodes.tsdb.algo;

import java.util.Date;

import ilex.var.NamedVariableList;
import ilex.var.NamedVariable;
import decodes.tsdb.DbAlgorithmExecutive;
import decodes.tsdb.DbCompException;
import decodes.tsdb.DbIoException;
import decodes.tsdb.ParmRef;
import decodes.tsdb.VarFlags;
import org.opendcs.annotations.PropertySpec;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;

@Algorithm(description ="Given reservoir storage (output of rating computation), and a property 'capacity', output the percent\n" + 
"full and storage remaining." )
public class ReservoirFull
	extends decodes.tsdb.algo.AW_AlgorithmBase
{
	@Input 
	public double storage;

	@Output(type = Double.class)
	public NamedVariable percentFull = new NamedVariable("percentFull", 0);
	@Output(type = Double.class)
	public NamedVariable storageRemaining = new NamedVariable("storageRemaining", 0);

	@PropertySpec
	public double capacity = 1;

	// Allow javac to generate a no-args constructor.

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	@Override
	protected void initAWAlgorithm( )
		throws DbCompException
	{
		_awAlgoType = AWAlgoType.TIME_SLICE;
	}
	
	/**
	 * This method is called once before iterating all time slices.
	 */
	@Override
	protected void beforeTimeSlices()
		throws DbCompException
	{
		String inUnits = getInputUnitsAbbr("storage");
		if (inUnits != null && inUnits.length() > 0)
			setOutputUnitsAbbr("storageRemaining", inUnits);
		setOutputUnitsAbbr("percentFull", "%");
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
		setOutput(storageRemaining, capacity - storage);
		if (capacity > 0.0)
			setOutput(percentFull, (storage / capacity) * 100.);
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	@Override
	protected void afterTimeSlices()
		throws DbCompException
	{
		// This code will be executed once after each group of time slices.
		// For TimeSlice algorithms this is done once after all slices.
		// For Aggregating algorithms, this is done after each aggregate
		// period.
	}
}
