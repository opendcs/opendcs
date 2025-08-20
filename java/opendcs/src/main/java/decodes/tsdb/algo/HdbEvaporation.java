package decodes.tsdb.algo;

import ilex.var.NamedVariable;
import decodes.tsdb.DbCompException;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;

@Algorithm(description = "Implements an evaporation computation\n" +
		"Primary value is \"area\".\n" +
		"Really need average area, so two choices for second input:\n" +
		"prevArea = previous EOP area or diffArea = deltaArea\n" +
		"For now, chose prevArea.\n" +
		"Third input is evapCoeff. This is a SDI pointer to either\n" +
		"a coefficient timeseries, or to lookup a coefficient from the stat tables\n" +
		"Output is evaporation as a volume.\n" +
		"<p>Properties include: \n" +
		"\n" +
		"<ul><li>ignoreTimeSeries - completely ignore changes to any timeseries value from evapCoeff, and always lookup from database.\n" +
		"</li></ul></p>\n")
public class HdbEvaporation
	extends decodes.tsdb.algo.AW_AlgorithmBase
{
	@Input
	public double area;
	@Input
	public double evapCoeff;

	@Output
	public NamedVariable evap = new NamedVariable("evap", 0);

	@org.opendcs.annotations.PropertySpec(value="true")
	public boolean ignoreTimeSeries = true;
	@org.opendcs.annotations.PropertySpec(value="ignore")
	public String evapCoeff_MISSING = "ignore";

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
		if (ignoreTimeSeries || isMissing(evapCoeff)) {
			evapCoeff = getCoeff("evapCoeff");
		}
		setOutput(evap, area*evapCoeff);			
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
