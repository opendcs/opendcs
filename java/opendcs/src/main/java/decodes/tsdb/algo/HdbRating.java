/**
 * $Id$
 * 
 * Open Source Software
 * 
 * $Log
 */
package decodes.tsdb.algo;

import ilex.var.NamedVariable;
import decodes.sql.DbKey;
import decodes.tsdb.DbCompException;
import decodes.tsdb.RatingStatus;
import decodes.util.PropertySpec;
import decodes.hdb.HDBRatingTable;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;
@Algorithm(description = "Implements rating table lookups in the database.\n" +
		"Independent (e.g. STAGE) value is called \"indep\".\n" +
		"Dependent (e.g. FLOW) is called \"dep\".\n" +
		"\n" +
		"While this algorithm can apply shifts, the shift applied is not separately stored in the database.\n" +
		"See HdbShiftRating for an algorithm that does that.\n" +
		"<p>Properties include: \n" +
		"<ul> \n" +
		"<li>ratingType - default=\"Shift Adjusted Stage Flow\". Supports several others in HDB_RATING_TYPE\n" +
		"</li>\n" +
		"<li>applyShifts - default=false Whether or not to add the shift value to the independent value before doing the rating\n" +
		"</li>\n" +
		"<li>shift - default=0.0. Value to add to independent value before rating\n" +
		"</li>\n" +
		"<li>exceedLowerBound - default= false. Whether to do ratings below the lowest value in the rating table</li>\n" +
		"<li>exceedUpperBound - default= false. Whether to do ratings above the highest value in the rating table</li>\n" +
		"</ul></p>")
public class HdbRating
	extends decodes.tsdb.algo.AW_AlgorithmBase
{
	@Input
	public double indep;

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

	@Output
	public NamedVariable dep = new NamedVariable("dep", 0);

	@org.opendcs.annotations.PropertySpec(value="false")
	public boolean exceedLowerBound = false;
	@org.opendcs.annotations.PropertySpec(value="Shift Adjusted Stage Flow")
	public String ratingType = "Shift Adjusted Stage Flow";
	@org.opendcs.annotations.PropertySpec(value="false")
	public boolean exceedUpperBound = false;
	@org.opendcs.annotations.PropertySpec(value="false")
	public boolean applyShifts = false;
	@org.opendcs.annotations.PropertySpec(value="0.0")
	public double shift = 0.0;

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
		if (!isMissing(indep)) {
			if(applyShifts) {
				indep += shift;
			}
            RatingStatus rs = ratingTable.doRating(indep, _timeSliceBaseTime);
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
