package decodes.tsdb.algo;

import java.util.Date;

import ilex.var.NamedVariable;

import decodes.tsdb.DbCompException;
import decodes.tsdb.IntervalIncrement;

import decodes.tsdb.IntervalCodes;
import decodes.tsdb.ParmRef;
import org.opendcs.annotations.PropertySpec;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;

@Algorithm(description = "Dis-aggregates by spreading out the input values to the outputs in various\n" +
		"ways (fill, split).\n" +
		"The interval of the input should always be equal to, or longer than, the output.\n" +
		"Example: Input is daily, output is hour. 24 output values are written covering\n" +
		"the period of each input.\n" +
		"The 'method' property determines how each output period is determined:\n" +
		"<ul>\n" +
		"  <li>fill (default) - Each output is the same as the input covering the period.\n" +
		"      </li>\n" +
		"  <li>split - Divide the input equally between the outputs for the period.</li>\n" +
		"</ul>")

public class DisAggregate extends AW_AlgorithmBase
{
	private static final String OUTPUTSTRING = "output";
	@Input
	public double input;	//AW:TYPECODE=i

	ParmRef iref = null;
	String iintv = null;
	IntervalIncrement iintvii = null;
	ParmRef oref = null;
	String ointv = null;
	IntervalIncrement ointvii = null;

	@Output
	public NamedVariable output = new NamedVariable(OUTPUTSTRING, 0);

	@PropertySpec(value = "fill")
	public String method = "fill";

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
		oref = getParmRef(OUTPUTSTRING);
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
			setOutputUnitsAbbr(OUTPUTSTRING, inUnits);

		info("input intv=" + iintvii.toString()
			+ ", output intv=" + ointvii.toString());
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
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	@Override
	protected void afterTimeSlices()
	{
		setOutputUnitsAbbr(OUTPUTSTRING, getInputUnitsAbbr("input"));
	}
}
