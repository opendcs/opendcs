package decodes.hdb.algo;

import java.util.Date;
import java.util.GregorianCalendar;

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

//AW:IMPORTS
import decodes.tsdb.RatingStatus;
import decodes.hdb.HDBRatingTable;
//AW:IMPORTS_END

//AW:JAVADOC
/**
Implements the Powell Evaporation Computation.

net evap = gross evap - river evap - streamside evap - terrace evap - remaining evap
areas for river, streamside, and terrace cover from separate ratings
river evap is river area * gross evap coeff
streamside evap is streamside area * streamside evap coeff * ave temp
terrace evap is terrace area * terrace evap coeff * ave temp
remaining evap is (area - all areas) * ave precip

Inputs are:
area from reservoir elevation lookup
reservoir elevation

These are SDI pointers to lookup coefficients
from the stat tables.
gross evap coeff
streamside evap coeff
terrace evap coeff
average monthly rainfall
average monthly temperature

Output is net evaporation as a volume, plus the component evaporations of
gross, river, streamside, terrace, remaining.

<p>Properties include: 
<ul> 
<li>ignoreTimeSeries - completely ignore changes to any timeseries value from evapCoeff, and always lookup from database.
</li>
</ul>


 */
//AW:JAVADOC_END
public class GLDAEvap
	extends decodes.tsdb.algo.AW_AlgorithmBase
{
//AW:INPUTS
	public double area;	//AW:TYPECODE=i
	public double elev;	//AW:TYPECODE=i
	public double grossEvapCoeff;	//AW:TYPECODE=i
	public double streamEvapCoeff;	//AW:TYPECODE=i
	public double terraceEvapCoeff;	//AW:TYPECODE=i
	public double aveTemp;	//AW:TYPECODE=i
	public double avePrecip;	//AW:TYPECODE=i
	
	String _inputNames[] = { "area", "elev", "grossEvapCoeff",
			"streamEvapCoeff", "terraceEvapCoeff", "aveTemp", "avePrecip"};
//AW:INPUTS_END

//AW:LOCALVARS
	HDBRatingTable riverRatingTable = null;
	HDBRatingTable streamRatingTable = null;
	HDBRatingTable terraceRatingTable = null;
	GregorianCalendar cal = new GregorianCalendar();
	private boolean firstCall = true;
//AW:LOCALVARS_END

//AW:OUTPUTS
	public NamedVariable netEvap = new NamedVariable("netEvap", 0);
	public NamedVariable grossEvap = new NamedVariable("grossEvap", 0);
	public NamedVariable riverEvap = new NamedVariable("riverEvap", 0);
	public NamedVariable streamsideEvap = new NamedVariable("streamsideEvap", 0);
	public NamedVariable terraceEvap = new NamedVariable("terraceEvap", 0);
	public NamedVariable remainingEvap = new NamedVariable("remainingEvap", 0);
	String _outputNames[] = { "netEvap", "grossEvap", "riverEvap", 
			"streamsideEvap", "terraceEvap", "remainingEvap" };
//AW:OUTPUTS_END

//AW:PROPERTIES
	public boolean ignoreTimeSeries = true;
	public String area_missing = "fail";
	public String elev_missing = "fail";
	public String grossEvapCoeff_missing = "ignore";
	public String streamEvapCoeff_missing = "ignore";
	public String terraceEvapCoeff_missing = "ignore";
	public String aveTemp_missing = "ignore";
	public String avePrecip_missing = "ignore";
	
	String _propertyNames[] = { "ignoreTimeSeries", "area_missing", "elev_missing",
			"grossEvapCoeff_missing", "streamEvapCoeff_missing", "terraceEvapCoeff_missing",
			"aveTemp_missing", "avePrecip_missing" };
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
		if (firstCall)
		{
			firstCall = false;
			
			// Find the name for the input parameter.
			// Cast to int was added by M. Bogner Aug 2012 for the 3.0 CP upgrade project
			// Cast to int was moded by M. Bogner March 2013 for the 5.3 CP upgrade project
			// because the surrogate keys where changed to a dbkey object 
			//int elev_sdi = (int) getSDI("elev").getValue();
			DbKey elev_sdi = getSDI("elev");
			debug3("Constructing HDB ratings for evap ratings");
			riverRatingTable = new HDBRatingTable(tsdb,"Lake Powell River Area",elev_sdi);
			streamRatingTable = new HDBRatingTable(tsdb,"Lake Powell Streamside Area",elev_sdi);
			terraceRatingTable = new HDBRatingTable(tsdb,"Lake Powell Terrace Area",elev_sdi);

		}
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
		cal.setTime(_timeSliceBaseTime);
		int daysInMonth = cal.getActualMaximum(GregorianCalendar.DAY_OF_MONTH);
		if (_sliceInputsDeleted) { //handle deleted values, since we have more than one output
			deleteAllOutputs();
			return;
		}
		if (ignoreTimeSeries) {	
			grossEvapCoeff = getCoeff("grossEvapCoeff");
			streamEvapCoeff= getCoeff("streamEvapCoeff");
			terraceEvapCoeff = getCoeff("terraceEvapCoeff");
			aveTemp = getCoeff("aveTemp");
			avePrecip = getCoeff("avePrecip");
		}
		//gross evap is whole area times coeff divided by length of current month in days * 12 in/ft
		double gevap = area*grossEvapCoeff/(daysInMonth*12); 

		//river evap is river area times coeff divided by length of current month in days * 12 in/ft
		RatingStatus rs = riverRatingTable.doRating(elev, _timeSliceBaseTime);
		double riverArea = rs.dep;
		double rivevap = riverArea*grossEvapCoeff/(daysInMonth*12);
		
		//streamside evap is streamside area times coeff times ave temp divided by length of current month in days * 12 in/ft
		rs = streamRatingTable.doRating(elev, _timeSliceBaseTime);
		double streamArea = rs.dep;
		double sevap =  streamArea*streamEvapCoeff*aveTemp/(daysInMonth*12);
		
		//terrace evap is terrace area times coeff times ave temp divided by length of current month in days * 12 in/ft
		rs = terraceRatingTable.doRating(elev, _timeSliceBaseTime);
		double terraceArea = rs.dep;
		double tevap = terraceArea*terraceEvapCoeff*aveTemp/(daysInMonth*12);
		
		//remaining evap is remaining area * avePrecip divided by length of current month in days * 12 in/ft,
		//all assumed to evaporate
		double remevap = (area - riverArea - streamArea- terraceArea)*
		                 avePrecip/(daysInMonth*12);

		debug3("doAWTimeSlice gevap=" + gevap+ ", rivevap=" + rivevap+
				", sevap=" + sevap+ ", tevap=" + tevap+ ", remevap=" + remevap);
				
		setOutput(grossEvap, gevap);
		setOutput(riverEvap, rivevap);
		setOutput(streamsideEvap, sevap);
		setOutput(terraceEvap, tevap);
		setOutput(remainingEvap, remevap);
		
		setOutput(netEvap, gevap - rivevap - sevap - tevap - remevap);		
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
