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
// new class handles surrogate keys as an object
import decodes.sql.DbKey;
import org.opendcs.annotations.PropertySpec;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;
import decodes.tsdb.RatingStatus;
import decodes.hdb.HDBRatingTable;

@Algorithm(description = "Implements the Shift and Rating Table Lookups from the database. \n" +
"Independent value is called \"indep\". \n" +
"Dependent values are \"shift\" and \"dep\". \n" +
"Shift may or may not be stored in the database")
public class HdbLookupTimeShiftRating
	extends decodes.tsdb.algo.AW_AlgorithmBase
{
// Code modified September 2013 to include Reno Nevada customization logic
// Code modified by M. Bogner for inclusion into main source code tree
	@Input
	public double indep;

	HDBRatingTable ShiftTable = null;
	HDBRatingTable RatingTable = null;
	private boolean firstCall = true;

	@Output
	public NamedVariable dep = new NamedVariable("dep", 0);
    @Output
	public NamedVariable shift = new NamedVariable("shift", 0);

	@PropertySpec(value = "false") 
	public boolean variableShift = false;
	@PropertySpec(value = "0") 
	public double singleShift = 0;
	@PropertySpec(value = "Time Interpolated Shift") 
	public String shiftTableType = "Time Interpolated Shift";

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
			firstCall = false;
			// Find the name for the input parameter.
			// this int cast was added by M. Bogner Aug 2012 for the 3.0 CP upgrade project
			// this int cast with getvalue method was added by M. Bogner March 2013 for the 5.3 CP upgrade project
			// the surrogate keys were changed to a dbkey object
			DbKey indep_sdi = getSDI("indep");
			debug3("Constructing Shift and Rating tables for sdi " +
					indep_sdi);
			//default non extrapolation of lookups is fine here.
			ShiftTable = new HDBRatingTable(tsdb,shiftTableType,indep_sdi);
			RatingTable = new HDBRatingTable(tsdb,"Stage Flow",indep_sdi);
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
                else if(indep <= 0.0D)
                // this mod by M. Bogner Sept 2013 was for Water Masters in Reno
                // seems they are the only ones using this alg and this code was in place
                // there for many years but source code did not reflect this cutomization
                {
                   double def_zero = 0.0D;
                   setOutput(shift, def_zero);
                   setOutput(dep, def_zero);
                }
		else {
			if (variableShift) {
				RatingStatus shiftRS = ShiftTable.doRating(indep,_timeSliceBaseTime);
				indep = indep + shiftRS.dep;
				shiftRS.dep = round(shiftRS.dep,2);
				debug3("Lookup Shift result:" +
						shiftRS.dep + " resulting indep: "+ indep);
				setOutput(shift, shiftRS.dep);
			} else
			{
				indep = indep + singleShift;
				setOutput(shift, singleShift);
			}
			RatingStatus rs = RatingTable.doRating(indep,_timeSliceBaseTime);
			rs.dep = round(rs.dep,2);
			setOutput(dep, rs.dep);
		}
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	@Override
	protected void afterTimeSlices()
	{
		// This code will be executed once after each group of time slices.
		// For TimeSlice algorithms this is done once after all slices.
	}
}
