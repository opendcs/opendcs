package decodes.tsdb.algo;

import java.util.Date;

import ilex.util.TextUtil;
import ilex.var.NamedVariableList;
import ilex.var.NamedVariable;

import decodes.tsdb.DbAlgorithmExecutive;
import decodes.tsdb.DbCompException;
import decodes.tsdb.DbIoException;
import decodes.tsdb.IntervalIncrement;
import decodes.tsdb.VarFlags;

//AW:IMPORTS
import java.util.Calendar;
import java.util.GregorianCalendar;
import decodes.tsdb.IntervalCodes;
import decodes.tsdb.ParmRef;
import ilex.var.TimedVariable;
import ilex.var.NoConversionException;
//AW:IMPORTS_END

//AW:JAVADOC
/**
Dis-aggregates by spreading out the input values to the outputs in various
ways (fill, interpolate, split).
The interval of the input should always be equal to, or longer than, the output.
Example: Input is daily, output is hour. 24 output values are written covering
the period of each input.
The 'method' property determines how each output period is determined:
<ul>
  <li>fill (default) - Each output is the same as the input covering the period.
      </li>
  <li>interp - Determine the output by interpolating between input values</li>
  <li>split - Divide the input equally between the outputs for the period.</li>
</ul>
 */
//AW:JAVADOC_END
public class DisAggregate
	extends AW_AlgorithmBase
{
//AW:INPUTS
	public double input;	//AW:TYPECODE=i
	String _inputNames[] = { "input" };
//AW:INPUTS_END

//AW:LOCALVARS
	ParmRef iref = null;
	String iintv = null;
	IntervalIncrement iintvii = null;
	ParmRef oref = null;
	String ointv = null;
	IntervalIncrement ointvii = null;
	double prevInputV = 0.0;
	long prevInputT = 0;
//AW:LOCALVARS_END

//AW:OUTPUTS
	public NamedVariable output = new NamedVariable("output", 0);
	String _outputNames[] = { "output" };
//AW:OUTPUTS_END

//AW:PROPERTIES
	public String method = "fill";
	String _propertyNames[] = { "method" };
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
//AW:USERINIT_END
	}
	
	/**
	 * This method is called once before iterating all time slices.
	 */
	protected void beforeTimeSlices()
		throws DbCompException
	{
//AW:BEFORE_TIMESLICES

		// Get interval of input
		iref = getParmRef("input");
		if (iref == null)
			throw new DbCompException("Cannot determine period of 'input'");

		iintv = iref.compParm.getInterval();
		iintvii = IntervalCodes.getIntervalCalIncr(iintv);
		if (iintvii == null || iintvii.getCount() == 0)
			throw new DbCompException(
			   "Cannot DisAggregate from 'input' with instantaneous interval.");

		// Get interval of output
		oref = getParmRef("output");
		if (oref == null)
			throw new DbCompException("Cannot determine period of 'output'");
		ointv = oref.compParm.getInterval();
		ointvii = IntervalCodes.getIntervalCalIncr(ointv);
		if (ointvii == null || ointvii.getCount() == 0)
			throw new DbCompException(
			  "Cannot DisAggregate from 'output' with instantaneous interval.");

		if (!method.equalsIgnoreCase("fill") && !method.equalsIgnoreCase("split"))
			throw new DbCompException("Illegal method '" + method + "' -- allowed values are "
				+ "'split' and 'fill'.");
		
		// Normally for disagg, output units will be the same as input.
		String inUnits = getInputUnitsAbbr("input");
		if (inUnits != null && inUnits.length() > 0)
			setOutputUnitsAbbr("output", inUnits);

info("input intv=" + iintvii.toString()
	+ ", output intv=" + ointvii.toString());

		
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

		// if interval of output >= interval of input then output a single 
		// value at the input time.

		// MJM 2011 03/20 this code takes advantage of the fact that the
		// calendar constants are numbered such that the smaller the constant,
		// the larger the increment
		if (ointvii.getCalConstant() < iintvii.getCalConstant()
		 || (ointvii.getCalConstant() == iintvii.getCalConstant()
			&& ointvii.getCount() >= iintvii.getCount()))
		{
			// Just output one value at input time.
			setOutput(output, input);
			return;
		}
		
		// Iteration goes from time of THIS input up to (but not including)
		// time of NEXT input.
		Date startT = new Date(_timeSliceBaseTime.getTime());
		aggCal.setTime(startT);
		aggCal.add(iintvii.getCalConstant(), iintvii.getCount());
		Date endT = aggCal.getTime();
		aggCal.setTime(startT);

info("baseTime=" + debugSdf.format(_timeSliceBaseTime) + ", startT=" + debugSdf.format(startT)
+ ", endT=" + debugSdf.format(endT)
+ ", method=" + method + ", v=" + input);
		if (method.equalsIgnoreCase("fill"))
		{
			for(Date t = startT; t.before(endT);
				aggCal.add(ointvii.getCalConstant(), ointvii.getCount()),
				t = aggCal.getTime())
			{
				setOutput(output, input, t);
			}
		}
		else if (method.equalsIgnoreCase("split"))
		{
			int n = 0;
			Date start = aggCal.getTime(); // remember start time
			// first count # of values
			for(Date t = startT; t.before(endT);
				aggCal.add(ointvii.getCalConstant(), ointvii.getCount()),
				t = aggCal.getTime())
			{
				n++;
			}
			// Reset and divide input by the number.
			aggCal.setTime(start);
			double v = n == 0 ? input : input/(double)n;
			for(Date t = startT; t.before(endT);
				aggCal.add(ointvii.getCalConstant(), ointvii.getCount()),
				t = aggCal.getTime())
			{
				setOutput(output, v, t);
			}
		}

//AW:TIMESLICE_END
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
	{
//AW:AFTER_TIMESLICES
		setOutputUnitsAbbr("output", getInputUnitsAbbr("input"));
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
