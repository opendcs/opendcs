package decodes.tsdb.algo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import org.opendcs.annotations.PropertySpec;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;

import ilex.var.NamedVariableList;
import ilex.var.NamedVariable;
import ilex.var.NoConversionException;
import decodes.tsdb.DbAlgorithmExecutive;
import decodes.tsdb.DbCompException;
import decodes.tsdb.DbIoException;
import decodes.tsdb.VarFlags;

@Algorithm(description = "Stat takes a single input and generate multiple statistical measurements for the given time window.\n" + //
		" Only the desired output parameter values need their timeseries set, if an output parameter is\n" + //
		" not set it will be ignored.\n"
		)
public class Stat extends decodes.tsdb.algo.AW_AlgorithmBase
{
		@Input
		double input;

		ArrayList<Double> inputData = new ArrayList<Double>();
		double tally;
		double _min;
		double _max;
		int count;

		@Output(type = Double.class)
		NamedVariable ave = new NamedVariable("ave", 0);
		@Output(type = Double.class)
		NamedVariable min = new NamedVariable("min", 0);
		@Output(type = Double.class)
		NamedVariable max = new NamedVariable("max", 0);
		@Output(type = Double.class)
		NamedVariable med = new NamedVariable("med", 0);
		@Output(type = Double.class)
		NamedVariable stddev = new NamedVariable("stddev", 0);

		@PropertySpec(value = "1")
		long minSamplesNeeded = 1;
		@PropertySpec(value = "true")
		boolean aveEnabled = true;
		@PropertySpec(value = "true")
		boolean minEnabled = true;
		@PropertySpec(value = "true")
		boolean maxEnabled = true;
		@PropertySpec(value = "true")
		boolean medEnabled = true;
		@PropertySpec(value = "true")
		boolean stddevEnabled = true;

		// Allow javac to generate a no-args constructor.

		/**
		 * Algorithm-specific initialization provided by the subclass.
		 */
		@Override
		protected void initAWAlgorithm( )
		{
			_awAlgoType = AWAlgoType.AGGREGATING;
			_aggPeriodVarRoleName = "ave";
		}
		
		/**
		 * This method is called once before iterating all time slices.
		 */
		@Override
		protected void beforeTimeSlices() throws DbCompException
		{
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
		protected void doAWTimeSlice() throws DbCompException
		{
			debug2("AverageAlgorithm:doAWTimeSlice, input=" + input + ", timeslice=" + debugSdf.format(_timeSliceBaseTime));
			if (!isMissing(input))
			{
				inputData.add(input);
				if(input<_min)
				{
					_min = input;
				}
				if(input>_max)
				{
					_max = input;
				}
				tally += input;
				count++;
			}
	//AW:TIMESLICE_END
		}

		/**
		 * This method is called once after iterating all time slices.
		 */
		@Override
		protected void afterTimeSlices() throws DbCompException
		{
			if (count < minSamplesNeeded)
			{
				warning("Do not have minimum # samples (" + minSamplesNeeded
					+ ") -- not producing an average.");
				if (_aggInputsDeleted)
				{
					deleteOutput(ave);
				}
			}
			debug3("After timeslice aggPeriodEnd=" + debugSdf.format(_aggregatePeriodEnd)
				+ " count=" + count + ", min=" + _min + ", max=" + _max + ", tally=" + tally);

			Collections.sort(inputData);

			if (aveEnabled)
			{
				setOutput(ave, tally / (double)count);
			}
			if (minEnabled)
			{
				setOutput(min, _min);
			}
			if (maxEnabled)
			{
				setOutput(max, _max);
			}
			if (medEnabled)
			{
				int medIdx = (count % 2 == 0) ? count/2-1 : count/2;
				setOutput(med, inputData.get(medIdx));
			}
			if (stddevEnabled)
			{
				setOutput(stddev, stdDeviation(inputData, tally/(double)count));
			}
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
}
