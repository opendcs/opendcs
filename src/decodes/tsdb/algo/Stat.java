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
		double min;
		double max;
		int count;

	//AW:LOCALVARS_END

	//AW:OUTPUTS
		NamedVariable average = new NamedVariable("average", 0);
		NamedVariable mn = new NamedVariable("minimum", 0);
		NamedVariable mx = new NamedVariable("maximum", 0);
		NamedVariable median = new NamedVariable("median", 0);
		NamedVariable stdDev = new NamedVariable("standard_deviation", 0);
		
		String _outputNames[] = { "average", "minimum", "maximum", "median", "standard_deviation" };
	//AW:OUTPUTS_END

	//AW:PROPERTIES
		long minSamplesNeeded = 1;
		long aveEnabled = 1;
		long minEnabled = 1;
		long maxEnabled = 1;
		long medEnabled = 1;
		long deviationEnabled = 1;
		String _propertyNames[] = { "minSamplesNeeded", "aveEnabled", "minEnabled", "maxEnabled", "medEnabled", "deviationEnabled" };
	//AW:PROPERTIES_END

		// Allow javac to generate a no-args constructor.

		/**
		 * Algorithm-specific initialization provided by the subclass.
		 */
		protected void initAWAlgorithm( )
		{
	//AW:INIT
			_awAlgoType = AWAlgoType.AGGREGATING;
			_aggPeriodVarRoleName = "average";
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
			min = 123456790.0;
			max = -123456790.0;
			
			// Normally for average, output units will be the same as input.
			String inUnits = getInputUnitsAbbr("input");
			if (inUnits != null && inUnits.length() > 0)
			{
				setOutputUnitsAbbr("average", inUnits);
				setOutputUnitsAbbr("minimum", inUnits);
				setOutputUnitsAbbr("maximum", inUnits);
				setOutputUnitsAbbr("median", inUnits);
				setOutputUnitsAbbr("standard_deviation", inUnits);
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
			//	debug2("AverageAlgorithm:doAWTimeSlice, input=" + input);
			if (!isMissing(input))
			{
				inputData.add(input);
				if(input<min)
					min = input;
				if(input>max)
					max = input;
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
//			debug2("AverageAlgorithm:afterTimeSlices, count=" + count);
	//debug1("AverageAlgorithm:afterTimeSlices, per begin="
	//+ debugSdf.format(_aggregatePeriodBegin) + ", end=" + debugSdf.format(_aggregatePeriodEnd));
			
			
			Collections.sort(inputData);
			if (count >= minSamplesNeeded)
			{
				if(count==minSamplesNeeded)
					setOutput(median, input);
				else
					if(medEnabled!=0)
					{
						if(count%2==0)
							setOutput(median, (inputData.get((count/2)-1)+inputData.get(count/2))/2);
						else
							setOutput(median, inputData.get(((int)count/2)));
					}
				
				if(aveEnabled!=0)
				{
					setOutput(average, tally / (double)count);
				}
				if(minEnabled!=0)
				{
					setOutput(mn, min);
				}
				if(maxEnabled!=0)
				{
					setOutput(mx, max);
				}
				if(deviationEnabled!=0)
				{
					setOutput(stdDev, stdDeviation(inputData, tally/(double)count));	
				}
				inputData.clear();
			}
			else 
			{
				warning("Do not have minimum # samples (" + minSamplesNeeded
					+ ") -- not producing an average.");
				if (_aggInputsDeleted)
					deleteOutput(average);
			}
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
