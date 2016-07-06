package decodes.tsdb.algo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import ilex.var.NamedVariableList;
import ilex.var.NamedVariable;
import ilex.var.NoConversionException;
import decodes.tsdb.DbAlgorithmExecutive;
import decodes.tsdb.DbCompException;
import decodes.tsdb.DbIoException;
import decodes.tsdb.VarFlags;

//AW:JAVADOC
/**
AverageAlgorithm averages single 'input' parameter to a single 'average' 
parameter. The averaging period is determined by the interval of the output
parameter.

 */
//AW:JAVADOC_END
public class Stat  extends decodes.tsdb.algo.AW_AlgorithmBase
{
	//AW:INPUTS
		double input;	//AW:TYPECODE=i
		String _inputNames[] = { "input" };
	//AW:INPUTS_END

	//AW:LOCALVARS
		ArrayList<Double> inputData = new ArrayList<Double>();
		double tally;
		double _min;
		double _max;
		int count;

	//AW:LOCALVARS_END

	//AW:OUTPUTS
		NamedVariable ave = new NamedVariable("ave", 0);
		NamedVariable min = new NamedVariable("min", 0);
		NamedVariable max = new NamedVariable("max", 0);
		NamedVariable med = new NamedVariable("med", 0);
		NamedVariable stddev = new NamedVariable("stddev", 0);
		
		String _outputNames[] = { "ave", "min", "max", "med", "stddev" };
	//AW:OUTPUTS_END

	//AW:PROPERTIES
		long minSamplesNeeded = 1;
		boolean aveEnabled = true;
		boolean minEnabled = true;
		boolean maxEnabled = true;
		boolean medEnabled = true;
		boolean stddevEnabled = true;
		String _propertyNames[] = { "minSamplesNeeded", "aveEnabled", "minEnabled", "maxEnabled", "medEnabled", 
			"stddevEnabled" };
	//AW:PROPERTIES_END

		// Allow javac to generate a no-args constructor.

		/**
		 * Algorithm-specific initialization provided by the subclass.
		 */
		protected void initAWAlgorithm( )
		{
	//AW:INIT
			_awAlgoType = AWAlgoType.AGGREGATING;
			_aggPeriodVarRoleName = "ave";
	//AW:INIT_END

	//AW:USERINIT
			// No one-time init required.
	//AW:USERINIT_END
		}
		
		/**
		 * This method is called once before iterating all time slices.
		 */
		protected void beforeTimeSlices()
		{
	//AW:BEFORE_TIMESLICES
			// Zero out the tally & count for this agg period.
			tally = 0.0;
			count = 0;
			_min = Double.POSITIVE_INFINITY;
			_max = Double.NEGATIVE_INFINITY;
			
			// Normally for average, output units will be the same as input.
			String inUnits = getInputUnitsAbbr("input");
			if (inUnits != null && inUnits.length() > 0)
			{
				setOutputUnitsAbbr("ave", inUnits);
				setOutputUnitsAbbr("min", inUnits);
				setOutputUnitsAbbr("max", inUnits);
				setOutputUnitsAbbr("med", inUnits);
				setOutputUnitsAbbr("stddev", inUnits);
			}
			this.debug3("Starting aggregate period at " + debugSdf.format(_aggregatePeriodBegin));
//			debug3("present: ave=" + isAssigned("ave")
//				+ ", min=" + isAssigned("min")
//				+ ", max=" + isAssigned("max")
//				+ ", med=" + isAssigned("med")
//				+ ", stddev=" + isAssigned("stddev"));

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
			debug2("AverageAlgorithm:doAWTimeSlice, input=" + input + ", timeslice=" + debugSdf.format(_timeSliceBaseTime));
			if (!isMissing(input))
			{
				inputData.add(input);
				if(input<_min)
					_min = input;
				if(input>_max)
					_max = input;
				tally += input;
				count++;
			}
	//AW:TIMESLICE_END
		}

		/**
		 * This method is called once after iterating all time slices.
		 */
		protected void afterTimeSlices()
		{
//AW:AFTER_TIMESLICES
			if (count < minSamplesNeeded)
			{
				warning("Do not have minimum # samples (" + minSamplesNeeded
					+ ") -- not producing an average.");
				if (_aggInputsDeleted)
					deleteOutput(ave);
			}
			debug3("After timeslice aggPeriodEnd=" + debugSdf.format(_aggregatePeriodEnd)
				+ " count=" + count + ", min=" + _min + ", max=" + _max + ", tally=" + tally);

			Collections.sort(inputData);

			if (aveEnabled)
				setOutput(ave, tally / (double)count);
			if (minEnabled)
				setOutput(min, _min);
			if (maxEnabled)
				setOutput(max, _max);
			if (medEnabled)
			{
				int medIdx = (count % 2 == 0) ? count/2-1 : count/2;
				setOutput(med, inputData.get(medIdx));
			}
			if (stddevEnabled)
				setOutput(stddev, stdDeviation(inputData, tally/(double)count));
//AW:AFTER_TIMESLICES_END
		}
		
		/**Standard Deviation based on entire population
		 * 
		 * @param data - population
		 * @param average - average of data
		 * @return result - the standard deviation for the given data
		 */
		private double stdDeviation(ArrayList<Double> data, double average)
		{
			double result = 0.0;
			for(Double input : data)
			{
				double v = input - average;
				result += v*v;
			}
			result = Math.sqrt((result/inputData.size()));		
			return result;
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
