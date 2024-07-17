package decodes.cwms.algo;

import decodes.cwms.resevapcalc.EvapMetData;
import decodes.cwms.resevapcalc.EvapReservoir;
import decodes.cwms.resevapcalc.ResEvap;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DbCompException;
import decodes.tsdb.algo.AWAlgoType;
import decodes.tsdb.algo.AW_AlgorithmBase;
import ilex.var.NamedVariable;

import java.util.List;

//AW:IMPORTS
//AW:IMPORTS_END

//AW:JAVADOC

/**
Run ResEvap Calculations.
 */
//AW:JAVADOC_END
public class ResEvapAlgo
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
	private double start_depth = 0.;
	private double depth_increment = .5;
	private ResEvap resEvap;
	private CTimeSeries windSpeedTS = null;
	private CTimeSeries AirTempTS = null;
	private CTimeSeries RelativeHumidityTS = null;
	private CTimeSeries AtmPressTS = null;
	private CTimeSeries PercentLowCloudTS = null;
	private CTimeSeries ElevLowCloudTS = null;
	private CTimeSeries PercentMidCloudTS = null;
	private CTimeSeries ElevMidCloudTS = null;
	private CTimeSeries PercentHighCloudTS = null;
	private CTimeSeries ElevHighCloudTS = null;
	private CTimeSeries ElevTS = null;
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
	public String reservoirId;
	public double Secchi;
	public double Zero_elevation;
	public double Lati;
	public double Longi;
	public int    GMT_Offset;
	public String Timezone;
	public String WindShear;
	public double ThermalDifCoe;
	public String Rating;


	String _propertyNames[] = {
	"SecchiDepthId",
	"MaxTempDepthId",
	"reservoirId",
	"Secchi",
	"Zero_elevation",
	"Lati",
	"Longi",
	"GMT_Offset",
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

	//TODO read database
	public double getStartElevation(){
		return 0;
	}
	//TODO read database
	public double[] getWTPElevation(){
		return new double[]{0};
	}
	//TODO read database
	public double getMaxTempDepthMeters(){
		return 0;
	}
	
	/**
	 * This method is called once before iterating all time slices.
	 */
	protected void beforeTimeSlices()
		throws DbCompException
	{
//AW:BEFORE_TIMESLICES
		windSpeedTS = getParmRef("windSpeed").timeSeries;
		AirTempTS = getParmRef("AirTemp").timeSeries;
		RelativeHumidityTS = getParmRef("RelativeHumidity").timeSeries;
		AtmPressTS = getParmRef("AtmPress").timeSeries;
		PercentLowCloudTS = getParmRef("PercentLowCloud").timeSeries;
		ElevLowCloudTS = getParmRef("ElevLowCloud").timeSeries;
		PercentMidCloudTS = getParmRef("PercentMidCloud").timeSeries;
		ElevMidCloudTS = getParmRef("ElevMidCloud").timeSeries;
		PercentHighCloudTS = getParmRef("PercentHighCloud").timeSeries;
		ElevHighCloudTS = getParmRef("ElevHighCloud").timeSeries;
		ElevTS = getParmRef("Elev").timeSeries;


		EvapMetData metData = new EvapMetData();
		metData.setWindSpeedTs(windSpeedTS);
		metData.setAirTempTs(AirTempTS);
		metData.setRelHumidityTs(RelativeHumidityTS);
		metData.setAirPressureTs(AtmPressTS);
		metData.setLowCloudTs(PercentLowCloudTS, ElevLowCloudTS);
		metData.setMedCloudTs(PercentMidCloudTS, ElevMidCloudTS);
		metData.setHighCloudTs(PercentHighCloudTS, ElevHighCloudTS);

		EvapReservoir reservoir = new EvapReservoir();
		reservoir.setName(reservoirId);

		//  This need to be done early on
		reservoir.setInputDataIsEnglish(true);
		double lonneg = -Longi;
		reservoir.setLatLon(Lati, lonneg);
		reservoir.setSecchi(Secchi);

		reservoir.setInstrumentHeights(32.81, 32.81, 32.81);

		double startElev = getStartElevation();

		reservoir.setElevation(startElev);
		reservoir.setZeroElevation(Zero_elevation);

		resEvap = new ResEvap();

		if (!resEvap.setReservoir(reservoir)) {
			throw new DbCompException("Reservoir not in Database. Exiting Script.");
		}

		// reservoir structure setup so now set wtemp profile
		int resj = reservoir.getResj();

//		print("ResJ: " + str(resj));
//		print("WTP len: " + str(len(wtp)));

		double[] wtp = getWTPElevation();
		// reverse array order
		double[] wtpR = new double[resj];
		for (int i = 0; i<resj+1; i++){
			wtpR[i] = wtp[resj-i];
		}

		reservoir.setInitWaterTemperatureProfile(wtpR, resj);

		resEvap._metData = metData;
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
		try {
			boolean noProblem = resEvap.compute(_timeSliceBaseTime, GMT_Offset);
			if (!noProblem){
				throw new DbCompException("ResEvap Compute Not Successful. Exiting Script.");
			}
		}
		catch(Exception ex){
			throw new DbCompException("ResEvap Compute Not Successful. Exiting Script.", ex);
		}

		List<Double> computedList = resEvap.getComputedMetTimeSeries();
		setOutput(HourlySurfaceTemp, computedList.get(0));
		setOutput(HourlySensible, computedList.get(1));
		setOutput(HourlyLatent, computedList.get(2));
		setOutput(HourlySolar, computedList.get(3));
		setOutput(HourlyFluxIn, computedList.get(4));
		setOutput(HourlyFluxOut, computedList.get(5));
		setOutput(HourlyEvap, computedList.get(6));

		setOutput(DailyEvap, resEvap.getDailyEvapTimeSeries());
		setOutput(DailyEvapAsFlow, resEvap.getDailyEvapFlowTimeSeries());
		//setOutput(DailyWaterTempProfile, resEvap.getDailyTemperatureProfileTs(start_depth, getMaxTempDepthMeters(), depth_increment));
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
