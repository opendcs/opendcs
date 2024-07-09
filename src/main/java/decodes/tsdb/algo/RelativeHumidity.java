package decodes.tsdb.algo;

import decodes.cwms.CwmsConstants;
import decodes.cwms.CwmsTsId;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DbCompException;
import decodes.tsdb.IntervalCodes;
import ilex.var.NamedVariable;
import ilex.var.NoConversionException;
import ilex.var.TimedVariable;
import opendcs.opentsdb.Interval;

import java.util.Date;

//AW:IMPORTS
// Place an import statements you need here.
//AW:IMPORTS_END

//AW:JAVADOC

/**
Interpolate input data to fit Irregular time series pattern.
 */
//AW:JAVADOC_END
public class RelativeHumidity
	extends AW_AlgorithmBase
{
//AW:INPUTS
	public double temperature;	//AW:TYPECODE=i
	public double dewPoint;	//AW:TYPECODE=i
	String _inputNames[] = { "temperature", "dewPoint" };
//AW:INPUTS_END

//AW:LOCALVARS
	// Enter any local class variables needed by the algorithm.
//AW:LOCALVARS_END

//AW:OUTPUTS
	public NamedVariable output = new NamedVariable("output", 0);
	String _outputNames[] = { "output" };
//AW:OUTPUTS_END

//AW:PROPERTIES
	String _propertyNames[] = {};
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
//AW:USERINIT_END
	}
	
	/**
	 * This method is called once before iterating all time slices.
	 */
	protected void beforeTimeSlices()
		throws DbCompException
	{
//AW:BEFORE_TIMESLICES
		
		// Validation
		String outputIntvs = getParmRef("output").compParm.getInterval();
		Interval outputIntv = IntervalCodes.getInterval(outputIntvs);
		String temperatureIntvs = getParmRef("temperature").compParm.getInterval();
		Interval temperatureIntv = IntervalCodes.getInterval(temperatureIntvs);

		if(!outputIntv.equals(temperatureIntv)){
			throw new DbCompException("Output interval does not match temperature interval");
		}
		
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
		if(isMissing(temperature) || isMissing(dewPoint)){
			return;
		}
		if (dewPoint > temperature){
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
		
//AW:TIMESLICE_END
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
		throws DbCompException
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
