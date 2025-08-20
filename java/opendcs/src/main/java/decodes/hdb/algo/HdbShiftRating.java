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
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;
import org.opendcs.annotations.PropertySpec;

import decodes.tsdb.RatingStatus;
import decodes.hdb.HDBRatingTable;

@Algorithm(description = "Implements the Shift and Rating Table Lookups from the database. \n" +
"Independent value is called \"indep\". \n" +
"Dependent values are \"shift\" and \"dep\". \n" +
"Shift may or may not be stored in the database \n\n" +

"By default, uses a shift input in the \"singleShift\" property. \n" +
"If variableShift is defined as true, then uses a rating algorithm \n" +
"from the database to find the shift. \n\n" +

"Default value for shiftTableType is \"Stage Shift\" which \n" +
"will linearly interpolate shifts from a single shift table. \n" +
"Other options are: \n" +
"Time Interpolated Shift: lookup shifts separated in time, do \n" +
"linear interpolations in time. \n" +
"Time Interpolated Variable Shift: linear interpolate shifts from stage, \n" +
"do linear interpolations in time from the resulting shifts \n\n" +
   
"The shift, if any, is added to the indep value, and then the dep value is found via \n" +
"a \"Stage Flow\" rating table type.")
public class HdbShiftRating
	extends decodes.tsdb.algo.AW_AlgorithmBase
{
	@Input
	public double indep;

	HDBRatingTable ShiftTable = null;
	HDBRatingTable RatingTable = null;
	private boolean firstCall = true;

	@Output
	public NamedVariable dep = new NamedVariable("dep", 0);
	@Output
	public NamedVariable shift = new NamedVariable("shift", 0);

	
	@PropertySpec(description = "(default=0) If variableShift==false, then this number provides a constant shift for all lookups.", value = "0")
	public double singleShift = 0;
	@PropertySpec(description = "(default=false) Set to true to use a separate shift table rather than the constant.", value = "false")
	public boolean variableShift = false;
	@PropertySpec(description = "(default=\"StageShift\") This is used as the HDB_TABLE type for the variable shifts.", value = "Stage Shift")
	public String shiftTableType = "Stage Shift";
	@PropertySpec(description = "(default=\"Stage Flow\") This is used as the HDB_TABLE type for the actual rating.", value = "Stage Flow")
	public String lookupTableType = "Stage Flow";


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
		if (firstCall)
		{
			// Find the name for the input parameter.
			// this int cast was added by M. Bogner Aug 2012 for the 3.0 CP upgrade project
//					int indep_sdi = (int) getSDI("indep");
			// this line was moded by M. Bogner March 2013 for the 5.3 CP upgrade project
			// because the surrogate keys in the CP were changed to an object
			DbKey indep_sdi = getSDI("indep");
			debug3("Constructing Shift and Rating tables for sdi " + indep_sdi);
			//default non extrapolation of lookups is fine here.
			ShiftTable = new HDBRatingTable(tsdb,shiftTableType,indep_sdi);
			RatingTable = new HDBRatingTable(tsdb,lookupTableType,indep_sdi);

			firstCall = false;
		}
		// This code will be executed once before each group of time slices.
		// For TimeSlice algorithms this is done once before all slices.
		//TODO: fix updates of rating table detection,
		//for now, just force tables to recheck the database for a new rating id
		// at every group of time slices
		ShiftTable.resetRatingId();
		RatingTable.resetRatingId();
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
