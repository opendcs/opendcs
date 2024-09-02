package decodes.tsdb.algo;

import ilex.var.NamedVariable;
import decodes.tsdb.DbCompException;
import decodes.tsdb.RatingStatus;
import decodes.hdb.HDBRatingTable;
import java.lang.Math;

// Mark: Be sure to import DbKey
import decodes.sql.DbKey;
import decodes.util.PropertySpec;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;

@Algorithm(description = "Implements the ACAPS rating algorithm from tables in the database.\n" +
		"Independent value is called \"elevation\".\n" +
		"Dependent values are \"storage\" and \"area\".\n" +
		"variableM controls whether the\n" +
		"constantM value provides the exponent\n" +
		"or if it is found in a rating table.\n" +
		"Equations are\n" +
		"diff=elevation-Y0 from A0 lookup.\n" +
		"A=round((A1 + M*A2*diff),2)\n" +
		"S=round((A0 + A1*diff + A2*Math.pow(diff,M)),2)")
public class HdbACAPSRating
	extends decodes.tsdb.algo.AW_AlgorithmBase
{
	@Input
	public double elevation;

	HDBRatingTable A0RatingTable = null;
	HDBRatingTable A1RatingTable = null;
	HDBRatingTable A2RatingTable = null;
	HDBRatingTable MRatingTable = null;
	private boolean firstCall = true;


  	@Output
	public NamedVariable storage = new NamedVariable("storage", 0);
	@Output
	public NamedVariable area = new NamedVariable("area", 0);

	//Makes no sense to handle shifts, or exceedences, since ACAPS
	//does not work at all when below lowest value, and maximum value is undefined
	@org.opendcs.annotations.PropertySpec(value="2.0")
	public double constantM = 2.0;
	@org.opendcs.annotations.PropertySpec(value="false")
	public boolean variableM = false;

    private PropertySpec ACAPSRatingPropertySpecs[] =
	{
		new PropertySpec("constantM", PropertySpec.NUMBER,
			"(default=2) If variableM==false, then this number provides a constant M exponent for all equations."),
		new PropertySpec("variableM", PropertySpec.BOOLEAN,
			"(default=false) Set to true to use a separate M rating table rather than the constant.")
	};
	@Override
	protected PropertySpec[] getAlgoPropertySpecs()
	{
		return ACAPSRatingPropertySpecs;
	}

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
			DbKey elev_sdi = getSDI("elevation");
			debug3("Constructing ACAPS ratings for sdi " +
					elev_sdi);
	//default non extrapolation of lookups is fine here.
			A0RatingTable = new HDBRatingTable(tsdb,"ACAPS A0",elev_sdi);
			A1RatingTable = new HDBRatingTable(tsdb,"ACAPS A1",elev_sdi);
			A2RatingTable = new HDBRatingTable(tsdb,"ACAPS A2",elev_sdi);
			MRatingTable  = new HDBRatingTable(tsdb,"ACAPS M",elev_sdi);

			firstCall = false;
		}
		// This code will be executed once before each group of time slices.
		// For TimeSlice algorithms this is done once before all slices.
		//TODO: fix updates of rating table detection
		A0RatingTable.resetRatingId();
		A1RatingTable.resetRatingId();
		A2RatingTable.resetRatingId();
		MRatingTable.resetRatingId();
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
		if (isMissing(elevation)) {
			deleteAllOutputs();
		}
		else {
			RatingStatus A0rs = A0RatingTable.doRating(elevation,_timeSliceBaseTime);
			RatingStatus A1rs = A1RatingTable.doRating(elevation,_timeSliceBaseTime);
			RatingStatus A2rs = A2RatingTable.doRating(elevation,_timeSliceBaseTime);

			double diff = elevation - A0rs.indep;
			double A0 = A0rs.dep;
			double A1 = A1rs.dep;
			double A2 = A2rs.dep;
			debug3("Lookup ACAPS results: deltaE: " + diff + " A0: " + A0 + " A1: " + A1 + " A2: " + A2);

			double M = constantM;

			if (variableM) {
			    RatingStatus MRS = MRatingTable.doRating(elevation,_timeSliceBaseTime);
			    M = MRS.dep;
			    debug3("Lookup M result: " + M);
			}

			//See Documents from Rick Clayton on these equations.
			double loc_storage = round((A0 + A1*diff + A2*Math.pow(diff,M)),2);

			double loc_area = round((A1 + M*A2*Math.pow(diff,M-1.0)),2);

			setOutput(storage, loc_storage);
			setOutput(area, loc_area);
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
