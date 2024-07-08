package decodes.cwms.algo;

import decodes.tsdb.DbCompException;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.algo.AWAlgoType;
import decodes.tsdb.algo.AW_AlgorithmBase;
import ilex.var.NamedVariable;

//AW:IMPORTS
//AW:IMPORTS_END

//AW:JAVADOC

/**
Run ResEvap Calculations.
 */
//AW:JAVADOC_END
public class ResEvap
	extends AW_AlgorithmBase
{
//AW:INPUTS
	public double windSpeed;		//AW:TYPECODE=i
	public double AirTemp;			//AW:TYPECODE=i
	public double RelativeHumidity;	//AW:TYPECODE=i
	public double AtmPress;			//AW:TYPECODE=i
	public double PercentLowCloud;	//AW:TYPECODE=i
	public double ElevLowCloud;		//AW:TYPECODE=i
	public double PercentMidCloud;	//AW:TYPECODE=i
	public double ElevMidCloud;		//AW:TYPECODE=i
	public double PercentHighCloud;	//AW:TYPECODE=i
	public double ElevHighCloud;	//AW:TYPECODE=i
	public double Elev;				//AW:TYPECODE=i
	String _inputNames[] = { "windSpeed",
	"AirTemp",
	"RelativeHumidity",
	"AtmPress",
	"PercentLowCloud",
	"ElevLowCloud",
	"PercentMidCloud",
	"ElevMidCloud",
	"PercentHighCloud",
	"ElevHighCloud",
	"Elev"			 };
//AW:INPUTS_END

//AW:LOCALVARS
//AW:LOCALVARS_END

//AW:OUTPUTS
	public NamedVariable DailyWaterTempProfile 	= new NamedVariable("DailyWaterTempProfile", 0);
	public NamedVariable HourlySurfaceTemp 		= new NamedVariable("HourlySurfaceTemp", 0);
	public NamedVariable HourlyEvap 			= new NamedVariable("HourlyEvap", 0);
	public NamedVariable DailyEvap 				= new NamedVariable("DailyEvap", 0);
	public NamedVariable DailyEvapAsFlow 		= new NamedVariable("DailyEvapAsFlow", 0);
	public NamedVariable HourlyFluxOut 			= new NamedVariable("HourlyFluxOut", 0);
	public NamedVariable HourlyFluxIn 			= new NamedVariable("HourlyFluxIn", 0);
	public NamedVariable HourlySolar 			= new NamedVariable("HourlySolar", 0);
	public NamedVariable HourlyLatent 			= new NamedVariable("HourlyLatent", 0);
	public NamedVariable HourlySensible 		= new NamedVariable("HourlySensible", 0);
	String _outputNames[] = {
			"DailyWaterTempProfile",
			"HourlySurfaceTemp",
			"HourlyEvap",
			"DailyEvap",
			"DailyEvapAsFlow",
			"HourlyFluxOut",
			"HourlyFluxIn",
			"HourlySolar",
			"HourlyLatent",
			"HourlySensible",
	};
//AW:OUTPUTS_END

//AW:PROPERTIES
	public String depth;
	public String SecchiDepthId;
	public String MaxTempDepthId;
	public String reservior;
	public double Secchi;
	public double Zero_elevation;
	public double Lati;
	public double Longi;
	public int    GNT_Offset;
	public String Timezone;
	public String WindShear;
	public double ThermalDifCoe;
	public String Rating;


	String _propertyNames[] = {
	"SecchiDepthId",
	"MaxTempDepthId",
	"reservior",
	"Secchi",
	"Zero_elevation",
	"Lati",
	"Longi",
	"GNT_Offset",
	"Timezone",
	"WindShear",
	"ThermalDifCoe",
	"Rating",
	};
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
//AW:TIMESLICE_END
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
		throws DbCompException
	{
//AW:AFTER_TIMESLICES
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
