package decodes.hdb.algo;

import java.util.Date;

import ilex.var.NamedVariableList;
import ilex.var.NamedVariable;
import decodes.tsdb.DbAlgorithmExecutive;
import decodes.tsdb.DbCompException;
import decodes.tsdb.DbIoException;
import decodes.tsdb.VarFlags;
// this new import was added by M. Bogner Aug 2012 for the 3.0 CP upgrade project
import decodes.tsdb.algo.AWAlgoType;
// this new import was added by M. Bogner March 2013 for the 5.3 CP upgrade project
// where surrogate keys in the CP were changed to a DbKey object
import decodes.sql.DbKey;

//AW:IMPORTS
import decodes.tsdb.RatingStatus;
import decodes.hdb.HDBRatingTable;
//AW:IMPORTS_END

//AW:JAVADOC
/**
Implements the Shift and Rating Table Lookups from the database.
Independent value is called "indep".
Dependent values are "shift" and "dep".
Shift may or may not be stored in the database

By default, uses a shift input in the "singleShift" property.
If variableShift is defined as true, then uses a rating algorithm
from the database to find the shift.

Default value for shiftTableType is "Stage Shift" which
will linearly interpolate shifts from a single shift table.
Other options are:
Time Interpolated Shift: lookup shifts separated in time, do
   linear interpolations in time.
Time Interpolated Variable Shift: linear interpolate shifts from stage,
   do linear interpolations in time from the resulting shifts
   
The shift, if any, is added to the indep value, and then the dep value is found via 
a "Stage Flow" rating table type.
 */
//AW:JAVADOC_END
public class HdbShiftRating
	extends decodes.tsdb.algo.AW_AlgorithmBase
{
//AW:INPUTS
	public double indep;	//AW:TYPECODE=i
	String _inputNames[] = { "indep" };
//AW:INPUTS_END

//AW:LOCALVARS
	HDBRatingTable ShiftTable = null;
	HDBRatingTable RatingTable = null;

//AW:LOCALVARS_END

//AW:OUTPUTS
	public NamedVariable dep = new NamedVariable("dep", 0);
	public NamedVariable shift = new NamedVariable("shift", 0);
	String _outputNames[] = { "dep", "shift" };
//AW:OUTPUTS_END

//AW:PROPERTIES
	public double singleShift = 0;
	public boolean variableShift = false;
	public String shiftTableType = "Stage Shift";
	String _propertyNames[] = { "variableShift", "singleShift", "shiftTableType" };
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
		// Find the name for the input parameter.
// this int cast was added by M. Bogner Aug 2012 for the 3.0 CP upgrade project
//		int indep_sdi = (int) getSDI("indep");
// this line was moded by M. Bogner March 2013 for the 5.3 CP upgrade project
// because the surrogate keys in the CP were changed to an object
		DbKey indep_sdi = getSDI("indep");
		debug3("Constructing Shift and Rating tables for sdi " +
				indep_sdi
				);
		//default non extrapolation of lookups is fine here.
		ShiftTable = new HDBRatingTable(tsdb,shiftTableType,indep_sdi);
		RatingTable = new HDBRatingTable(tsdb,"Stage Flow",indep_sdi);
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
		//TODO: fix updates of rating table detection,
		//for now, just force tables to recheck the database for a new rating id
		// at every group of time slices
		ShiftTable.resetRatingId();
		RatingTable.resetRatingId();
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
		if (isMissing(indep)) {
			deleteAllOutputs();
		}
		else {
			if (variableShift) {
				// user wants a shift from the database
				RatingStatus shiftRS = ShiftTable.doRating(indep,_timeSliceBaseTime);
				indep = indep + shiftRS.dep;
				
				debug3("Lookup Shift result: " +
						shiftRS.dep + " resulting indep: "+ indep);
				setOutput(shift, shiftRS.dep);
			} else
			{
				//user input shift, default 0
				indep = indep + singleShift;
				debug3("Constant shift result:" +
						singleShift + " resulting indep: "+ indep);
				setOutput(shift, singleShift);
			}
			RatingStatus rs = RatingTable.doRating(indep,_timeSliceBaseTime);

			debug3("Flow result:" +	rs.dep);
			setOutput(dep, rs.dep);
		}
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
