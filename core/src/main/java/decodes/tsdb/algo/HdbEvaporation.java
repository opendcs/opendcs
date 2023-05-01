package decodes.tsdb.algo;

import java.util.Date;

import ilex.var.NamedVariableList;
import ilex.var.NamedVariable;
import decodes.tsdb.DbAlgorithmExecutive;
import decodes.tsdb.DbCompException;
import decodes.tsdb.DbIoException;
import decodes.tsdb.VarFlags;

//AW:IMPORTS
import decodes.tsdb.RatingStatus;
import decodes.hdb.HDBRatingTable;
//AW:IMPORTS_END

//AW:JAVADOC
/**
Implements an evaporation computation
Primary value is "area".
Really need average area, so two choices for second input:
prevArea = previous EOP area or diffArea = deltaArea
For now, chose prevArea.
Third input is evapCoeff. This is a SDI pointer to either
a coefficient timeseries, or to lookup a coefficient from the stat tables
Output is evaporation as a volume.
<p>Properties include: 
<ul> 
<li>ignoreTimeSeries - completely ignore changes to any timeseries value from evapCoeff, and always lookup from database.
</li>
</ul>


 */
//AW:JAVADOC_END
public class HdbEvaporation
	extends decodes.tsdb.algo.AW_AlgorithmBase
{
//AW:INPUTS
	public double area;	//AW:TYPECODE=i
	public double evapCoeff;	//AW:TYPECODE=i
	String _inputNames[] = { "area", "evapCoeff" };
//AW:INPUTS_END

//AW:LOCALVARS

//AW:LOCALVARS_END

//AW:OUTPUTS
	public NamedVariable evap = new NamedVariable("evap", 0);
	String _outputNames[] = { "evap" };
//AW:OUTPUTS_END

//AW:PROPERTIES
	public boolean ignoreTimeSeries = true;
	public String evapCoeff_MISSING = "ignore";
	String _propertyNames[] = { "ignoreTimeSeries", "evapCoeff_MISSING" };
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
		// This code will be executed once before each group of time slices.
		// For TimeSlice algorithms this is done once before all slices.
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
		if (ignoreTimeSeries || isMissing(evapCoeff)) {
			evapCoeff = getCoeff("evapCoeff");
		}
		setOutput(evap, area*evapCoeff);			
//AW:TIMESLICE_END
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
	{
//AW:AFTER_TIMESLICES
		// This code will be executed once after each group of time slices.
		// For TimeSlice algorithms this is done once after all slices.
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
