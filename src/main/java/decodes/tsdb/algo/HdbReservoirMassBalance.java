package decodes.tsdb.algo;

import ilex.var.NamedVariable;
import decodes.tsdb.DbCompException;

import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;


@Algorithm(description = "Implements a reservoir mass balance\n" +
		"Output is inflow.\n" +
		"<p>Inputs are:\n" +
		"<ul>\n" +
		"<li>dStor (delta Storage)</li>\n" +
		"<li>rel (total release from reservoir)</li>\n" +
		"<li>bStor (delta Bank Storage)</li>\n" +
		"<li>evap (net reservoir evaporation)</li>\n" +
		"<li>div (diversion)</li>\n" +
		"</ul>\n" +
		"Any of these can be set to be optional by setting a property.\n" +
		"Storage, evaporation, and bank storage should be in units of acre-ft.\n" +
		"Rest in units of cfs.\n" +
		"\n" +
		"<p>Properties include: \n" +
		"<ul> \n" +
		"<li>dBStor_MISSING - whether delta bank storage is required (set to fail)\n" +
		"</li>\n" +
		"<li>evap_MISSING - whether evaporation is required (set to fail)\n" +
		"</li>\n" +
		"<li>div_MISSING - whether diversion is required (set to fail)\n" +
		"</li>\n" +
		"</ul>")
public class HdbReservoirMassBalance
	extends decodes.tsdb.algo.AW_AlgorithmBase
{
	@Input
	double dStor;
	@Input
	double rel;
	@Input
	double dBStor;
	@Input
	double evap;
	@Input
	double div;

	@Output
	NamedVariable inflow = new NamedVariable("inflow", 0);

	@org.opendcs.annotations.PropertySpec(value="ignore")
	String dBStor_MISSING = "ignore";
	@org.opendcs.annotations.PropertySpec(value="ignore")
	String evap_MISSING = "ignore";
	@org.opendcs.annotations.PropertySpec(value="ignore")
	String div_MISSING = "ignore";

	// Allow javac to generate a no-args constructor.

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	protected void initAWAlgorithm( )
		throws DbCompException
	{
		_awAlgoType = AWAlgoType.TIME_SLICE;
	}
	
	/**
	 * This method is called once before iterating all time slices.
	 */
	protected void beforeTimeSlices()
		throws DbCompException
	{
		// This code will be executed once before each group of time slices.
		// For TimeSlice algorithms this is done once before all slices.
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
		// inflow = S(t) - S(t-1) + O + E + (BS(t) - BS(t-1)) + D
		// or change in storage, release, evap, bank storage and diversion

		
		double in = dStor/1.98347;
		in += rel;

		// only reason for these to not be missing is if they are required
		if(!isMissing(evap))	in += evap/1.98347;
		if(!isMissing(dBStor))	in += dBStor/1.98347;
		if(!isMissing(div))		in += div;
		
		setOutput(inflow, in);
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
	{
		// This code will be executed once after each group of time slices.
		// For TimeSlice algorithms this is done once after all slices.
	}

}
