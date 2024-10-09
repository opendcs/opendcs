package decodes.tsdb.algo;

import decodes.tsdb.DbCompException;
import decodes.tsdb.IntervalCodes;
import ilex.var.NamedVariable;
import opendcs.opentsdb.Interval;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;

@Algorithm(description ="Takes two inputs, Air temperature and dew point, to calculate relative humidity" )
public class RelativeHumidity extends AW_AlgorithmBase
{

	@Input
	public double temperature;
	@Input
	public double dewPoint;

	@Output
	public NamedVariable output = new NamedVariable("output", 0);


	// Allow javac to generate a no-args constructor.

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	protected void initAWAlgorithm() throws DbCompException
	{
//AW:INIT
		_awAlgoType = AWAlgoType.TIME_SLICE;
//AW:INIT_END

//AW:USERINIT
		// Code here will be run once, after the algorithm object is created.
//AW:USERINIT_END
	}
	
	/**
	 * This method is called once before iterating all time slices.
	 */
	protected void beforeTimeSlices() throws DbCompException
	{
		
		// Validation
		String outputIntvs = getParmRef("output").compParm.getInterval();
		Interval outputIntv = IntervalCodes.getInterval(outputIntvs);
		String temperatureIntvs = getParmRef("temperature").compParm.getInterval();
		Interval temperatureIntv = IntervalCodes.getInterval(temperatureIntvs);

		if(!outputIntv.equals(temperatureIntv))
		{
			throw new DbCompException("Output interval does not match temperature interval");
		}

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
	protected void doAWTimeSlice() throws DbCompException
	{
		if (isMissing(temperature) || isMissing(dewPoint))
		{
			return;
		}
		if (dewPoint > temperature)
		{
			throw new DbCompException("dew point can not be greater than temperature");
		}

    	// Calculate the numerator
		double numerator = Math.exp((17.625 * dewPoint) / (243.04 + dewPoint));

    	// Calculate the denominator
		double denominator = Math.exp((17.625 * temperature) / (243.04 + temperature));

    	// Calculate the relative humidity
		double RH = 100 * (numerator / denominator);

		double RRH = Math.round( RH * 1000.0) / 1000.0;

		setOutput(output, RRH);

	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices() throws DbCompException
	{

	}

}
