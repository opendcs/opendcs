/**
 * $Id$
 * 
 * Open Source Software
 * 
 * $Log
 */
package decodes.tsdb.algo;

import java.util.Date;

import ilex.var.NamedVariableList;
import ilex.var.NamedVariable;
import decodes.sql.DbKey;
import decodes.tsdb.DbAlgorithmExecutive;
import decodes.tsdb.DbCompException;
import decodes.tsdb.DbIoException;
import decodes.tsdb.VarFlags;

//AW:IMPORTS
import decodes.tsdb.RatingStatus;
import decodes.util.PropertySpec;
import decodes.hdb.HDBRatingTable;

import java.util.Date;
//AW:IMPORTS_END

//AW:JAVADOC
/**
Implements rating table lookups in the database.
Independent (e.g. STAGE) value is called "indep".
Dependent (e.g. FLOW) is called "dep".

While this algorithm can apply shifts, the shift applied is not separately stored in the database.
See HdbShiftRating for an algorithm that does that.
<p>Properties include: 
<ul> 
<li>ratingType - default="Shift Adjusted Stage Flow". Supports several others in HDB_RATING_TYPE
</li>
<li>applyShifts - default=false Whether or not to add the shift value to the independent value before doing the rating
</li>
<li>shift - default=0.0. Value to add to independent value before rating
</li>
<li>exceedLowerBound - default= false. Whether to do ratings below the lowest value in the rating table</li>
<li>exceedUpperBound - default= false. Whether to do ratings above the highest value in the rating table</li>
</ul>
 */
//AW:JAVADOC_END
public class HdbRating
	extends decodes.tsdb.algo.AW_AlgorithmBase
{
//AW:INPUTS
	public double indep;	//AW:TYPECODE=i
	String _inputNames[] = { "indep" };
//AW:INPUTS_END

//AW:LOCALVARS
	HDBRatingTable ratingTable = null;
	private PropertySpec ratingPropertySpecs[] = 
	{
		new PropertySpec("exceedLowerBound", PropertySpec.BOOLEAN,
			"(default=false) Set to true to allow rating to interpolate below the lowest table value."),
		new PropertySpec("exceedUpperBound", PropertySpec.BOOLEAN,
			"(default=false) Set to true to allow rating to interpolate above the highest table value."),
		new PropertySpec("ratingType", PropertySpec.STRING,
			"(default=\"Shift Adjusted Stage Flow\") This is used as the HDB_TABLE type for the rating."),
		new PropertySpec("applyShifts", PropertySpec.BOOLEAN,
			"(default=false) Set to true to use a add the 'shift' property value before table lookup."),
		new PropertySpec("shift", PropertySpec.NUMBER,
			"(default=0) Constant shift for all lookups.")
	};
	@Override
	protected PropertySpec[] getAlgoPropertySpecs()
	{
		return ratingPropertySpecs;
	}
	private boolean firstCall = true;

//AW:LOCALVARS_END

//AW:OUTPUTS
	public NamedVariable dep = new NamedVariable("dep", 0);
	String _outputNames[] = { "dep" };
//AW:OUTPUTS_END

//AW:PROPERTIES
	public boolean exceedLowerBound = false;
	public String ratingType = "Shift Adjusted Stage Flow";
	public boolean exceedUpperBound = false;
	public boolean applyShifts = false;
	public double shift = 0.0;
	String _propertyNames[] = { "exceedLowerBound", "ratingType", "exceedUpperBound", "applyShifts", "shift" };
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
		if (firstCall)
		{
			// Find the name for the input parameter.
			DbKey indep_sdi = getSDI("indep");
			debug3("Constructing HDB rating for '" + ratingType + "' sdi " +
					indep_sdi);
			ratingTable = new HDBRatingTable(tsdb,ratingType,indep_sdi);
			ratingTable.setExceedLowerBound(exceedLowerBound);
			ratingTable.setExceedUpperBound(exceedUpperBound);

			firstCall = false;
		}
		// This code will be executed once before each group of time slices.
		// For TimeSlice algorithms this is done once before all slices.
		//TODO: fix updates of rating table detection
		ratingTable.resetRatingId();
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
		if (!isMissing(indep)) {
			if(applyShifts) {
				indep += shift;
			}
            RatingStatus rs = ratingTable.doRating(indep, _timeSliceBaseTime);
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
