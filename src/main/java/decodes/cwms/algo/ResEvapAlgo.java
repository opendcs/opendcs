package decodes.cwms.algo;

import decodes.cwms.CwmsTimeSeriesDb;
import decodes.cwms.rating.CwmsRatingDao;
import decodes.cwms.resevapcalc.*;
import decodes.db.*;
import decodes.tsdb.*;
import decodes.tsdb.algo.AWAlgoType;
import decodes.tsdb.algo.AW_AlgorithmBase;
import decodes.util.DecodesException;
import decodes.util.PropertySpec;
import hec.data.RatingException;
import ilex.util.TextUtil;
import ilex.var.NamedVariable;
import ilex.var.NoConversionException;
import ilex.var.TimedVariable;
import ilex.var.Variable;
import opendcs.dai.SiteDAI;
import opendcs.dai.TimeSeriesDAI;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;

//AW:IMPORTS

//import hec.data.RatingException;
import hec.data.cwmsRating.RatingSet;


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
	double tally;
	int count;

	private Date LastDate = null;
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

	private CTimeSeries HourlyEvapTS = null;

	CwmsRatingDao crd;
	SiteDAI siteDAO;
	TimeSeriesDAI timeSeriesDAO;
	WaterTempProfiles hourlyWTP;
	WaterTempProfiles DailyWTP;

	EvapReservoir reservoir;

//AW:LOCALVARS_END

//AW:OUTPUTS
//	public NamedVariable DailyWaterTempProfile 	= new NamedVariable("DailyWaterTempProfile", 0);
//	public NamedVariable HourlyWaterTempProfile	= new NamedVariable("HourlyWaterTempProfile", 0);
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
//			"DailyWaterTempProfile",
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
	private PropertySpec copyPropertySpecs[] =
			{
					new PropertySpec("WtpTsid", PropertySpec.STRING,
							"Base String for water Temperature Profiles"),
					new PropertySpec("depth", PropertySpec.STRING,
							"Depth format for compuatation output"),
					new PropertySpec("reservoirId", PropertySpec.STRING,
							"Location ID of reservoir"),
					new PropertySpec("Secchi", PropertySpec.NUMBER,
							"Average secchi depth of reservoir"),
					new PropertySpec("Zero_elevation", PropertySpec.NUMBER,
							"Streambed elevation of reservoir"),
					new PropertySpec("Lati", PropertySpec.NUMBER,
							"Latitude of reservoir"),
					new PropertySpec("Longi", PropertySpec.NUMBER,
							"Longitude of reservoir"),
					new PropertySpec("GMT_Offset", PropertySpec.NUMBER,
							"GMT offset at reservoir location"),
					new PropertySpec("Timezone", PropertySpec.STRING,
							"Time zone at reservoir location"),
					new PropertySpec("WindShear", PropertySpec.STRING,
							"Windshear equation to be utilized in computation"),
					new PropertySpec("ThermalDifCoe", PropertySpec.NUMBER,
							"Thermal diffusivity coefficient to be utilized in computation"),
					new PropertySpec("Rating", PropertySpec.STRING,
							"Rating Curve specification for Elevation-Area curve")
			};
	@Override
	protected PropertySpec[] getAlgoPropertySpecs()
	{
		return copyPropertySpecs;
	}

//AW:PROPERTIES
	//new prop
	public String WtpTsid;

	public String depth;
	//remove todo
//	public String SecchiDepthId;
//	public String MaxTempDepthId;
	public String reservoirId;
	public double Secchi;
	public double Zero_elevation;
	public double Lati;
	public double Longi;
	public double GMT_Offset;
	public String Timezone;
	public String WindShear;
	public double ThermalDifCoe;
	public String Rating;


	String _propertyNames[] = {
//	"SecchiDepthId",
//	"MaxTempDepthId",
	"WtpTsid",
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
//		boolean canCompute =
//		getParmRef("windSpeed").missingAction == MissingAction.IGNORE &&
//		getParmRef("RelativeHumidity").missingAction == MissingAction.IGNORE &&
//		getParmRef("AtmPress").missingAction == MissingAction.IGNORE &&
//		getParmRef("PercentLowCloud").missingAction == MissingAction.IGNORE &&
//		getParmRef("ElevLowCloud").missingAction == MissingAction.IGNORE &&
//		getParmRef("PercentMidCloud").missingAction == MissingAction.IGNORE &&
//		getParmRef("ElevMidCloud").missingAction == MissingAction.IGNORE &&
//		getParmRef("PercentHighCloud").missingAction == MissingAction.IGNORE &&
//		getParmRef("ElevHighCloud").missingAction == MissingAction.IGNORE &&
//		getParmRef("Elev").missingAction == MissingAction.IGNORE ;
//
//		if (!canCompute){
//			throw new DbCompException("One of algorithms inputs missing action is set to ignore. All inputs required");
//		}
        _awAlgoType = AWAlgoType.AGGREGATING;
		_aggPeriodVarRoleName = "DailyEvap";
		aggUpperBoundClosed = true;
		aggLowerBoundClosed = false;
//		aggPeriodInterval = IntervalCodes.int_one_day;

//AW:INIT_END

//AW:USERINIT
//AW:USERINIT_END
	}

	public double[] getProfiles() throws Exception {
		Date LastTime = ElevTS.findPrev(baseTimes.first()).getTime();
		hourlyWTP = new WaterTempProfiles(timeSeriesDAO, reservoirId, WtpTsid, LastTime, baseTimes.first(), start_depth, depth_increment);
		double[] arrayWTP = new double[hourlyWTP.tseries.size()];
		for(int i = 0; i < hourlyWTP.tseries.size(); i++){
			try {
				arrayWTP[i] = hourlyWTP.tseries.getTimeSeriesAt(i).findPrev(baseTimes.first()).getDoubleValue();
			}
			catch (Exception ex){
				throw new Exception("failed to load data from WTP");
			}
		}
		return arrayWTP;
	}

	public void setProfiles(WaterTempProfiles newWTP, double[] wtp, Date CurrentTime) throws DbCompException {
		double currentDepth = start_depth;
		for(int i = 0; i < wtp.length; i++){
			if(i+1>newWTP.tseries.size()){
				try{
					TimeSeriesIdentifier tsid = timeSeriesDAO.getTimeSeriesIdentifier(WtpTsid);
//					TimeSeriesIdentifier newTSID = tsdb.makeTsId(WtpTsid);
					TimeSeriesIdentifier newTSID= tsid.copyNoKey();

					Site newsite =  new Site();
					newsite.copyFrom(newTSID.getSite());
					SiteName strsite = newsite.getName(Constants.snt_CWMS);

					DecimalFormat decimalFormat = new DecimalFormat("000,0");
					String formattedNumber = decimalFormat.format(currentDepth);
					strsite.setNameValue(strsite.getNameValue()+"-D"+formattedNumber+"m");
					newTSID.setSite(newsite);

					CTimeSeries CTProfile = new CTimeSeries(newTSID);
					CTProfile.addSample(new TimedVariable(new Variable(wtp[i]), CurrentTime));
					newWTP.tseries.addTimeSeries(CTProfile);
				}
					catch (Exception ex){
					throw new DbCompException("failed to create new timeSeriesID"+ex);
				}
			}
			else{
				// todo might need to change
				CTimeSeries CTProfile = newWTP.tseries.getTimeSeriesAt(i);
				CTProfile.addSample(new TimedVariable(new Variable(wtp[i]), CurrentTime));
			}
			currentDepth += depth_increment;
		}
	}

	public void setDailyProfiles(Date CurrentTime) throws DbCompException {
		double[] arrayWTP = new double[hourlyWTP.tseries.size()];
		int i = 0;
		for(CTimeSeries CTS: hourlyWTP.tseries.getAllTimeSeries()){
			try{
				arrayWTP[i] = CTS.sampleAt(CTS.findNextIdx(CurrentTime)).getDoubleValue();
			}catch(NoConversionException ex){
				throw new DbCompException("Failed to load value from timeseries"+ex);
			}
			i++;
		}
		setProfiles(DailyWTP, arrayWTP, CurrentTime);
	}

	public double convertUnits(double cts, String currUnits, String newUnits) throws NoConversionException, DecodesException {
		if (TextUtil.strEqualIgnoreCase(currUnits, newUnits) || newUnits == null || currUnits == null){
			return cts;
		}

		EngineeringUnit euOld =	EngineeringUnit.getEngineeringUnit(currUnits);
		EngineeringUnit euNew = EngineeringUnit.getEngineeringUnit(newUnits);
		UnitConverter converter = null;
		converter = Database.getDb().unitConverterSet.get(euOld, euNew);
		if (converter == null)
		{
			throw new NoConversionException("failed to load converter");
        }
		double newValue;
		try
		{
			newValue = converter.convert(cts);
		} catch (DecodesException e)
		{
			throw new DecodesException("failed to run converter");
		}

		return newValue;
	}

	public void SetAsFlow(Double TotalEvap ,Date CurrentTime) throws NoConversionException, DecodesException, RatingException {
		double evap_to_meters = convertUnits(TotalEvap, HourlyEvapTS.getUnitsAbbr(), "m");

		double elev = resEvap.reservoir.getCurrentElevation(CurrentTime);
		double areaMetersSq;
		try {
			areaMetersSq = resEvap.reservoir.intArea(elev);
		}
		catch(RatingException ex){
			throw new RatingException("failed to compute rating", ex);
		}
		double dailyEvapFlow = (areaMetersSq * evap_to_meters)/(86400.);
		setOutput(DailyEvapAsFlow, dailyEvapFlow, CurrentTime);
	}

	//TODO read database
	public double getMaxTempDepthMeters(){
		return 0;
	}
	
	/**
	 * This method is called once before iterating all time slices.
	 */
	protected void beforeTimeSlices()
            throws DbCompException {
//AW:BEFORE_TIMESLICES
		tally = 0.0;
		count = 0;

		siteDAO =  tsdb.makeSiteDAO();
		timeSeriesDAO = tsdb.makeTimeSeriesDAO();
		crd = new CwmsRatingDao((CwmsTimeSeriesDb)tsdb);

		hourlyWTP = new WaterTempProfiles(timeSeriesDAO, start_depth, depth_increment);
		DailyWTP = new WaterTempProfiles(timeSeriesDAO, start_depth, depth_increment);

		HourlyEvapTS = getParmRef("HourlyEvap").timeSeries;

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

		reservoir = new EvapReservoir();
		reservoir.setName(reservoirId);
		reservoir.setThermalDiffusivityCoefficient(ThermalDifCoe);
		try {
			reservoir.setWindShearMethod(WindShearMethod.fromString(WindShear));
		}
		catch(Throwable ex){
			ex.printStackTrace();
		}

		reservoir.setInputDataIsEnglish(true);
		double lonneg = -Longi;
		reservoir.setLatLon(Lati, lonneg);
		reservoir.setSecchi(Secchi);

		RatingSet ratingSet;
		try {
			ratingSet = crd.getRatingSet(Rating);
		}
		catch(RatingException ex){
			throw new DbCompException("failed to load rating table", ex);
		}
		ratingSet.setDefaultValueTime(baseTimes.first().getTime());
		reservoir.conn = tsdb.getConnection();

		reservoir.setElevAreaRating(ratingSet);



		reservoir.setInstrumentHeights(32.81, 32.81, 32.81);

		reservoir.setElevationTs(ElevTS);
		double initElev;
		try{
			initElev = ElevTS.sampleAt(ElevTS.findNextIdx(baseTimes.first())).getDoubleValue();
		}
		catch(Exception ex){
			throw new DbCompException("failed to load initial Elevation");
		}
		reservoir.setElevation(initElev);
		reservoir.setZeroElevation(Zero_elevation);

		resEvap = new ResEvap();

		if (!resEvap.setReservoir(reservoir)) {
			throw new DbCompException("Reservoir not in Database. Exiting Script.");
		}

		// reservoir structure setup so now set wtemp profile
		int resj = reservoir.getResj();

		double[] wtp;
		try{
			wtp = getProfiles();
		}
		catch(Exception ex){
			throw new DbCompException("failed to load profiles");
		}
		// reverse array order
		double[] wtpR = new double[resj];
		for (int i = 0; i<resj; i++){
			wtpR[i] = wtp[resj-i-1];
		}

		reservoir.setInitWaterTemperatureProfile(wtpR, resj);

		resEvap.metData = metData;
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
	@Override
	protected void doAWTimeSlice()
            throws DbCompException {
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

		setProfiles(hourlyWTP, resEvap.getHourlyWaterTempProfile(), _timeSliceBaseTime);

		count++;
		tally += computedList.get(6);
//AW:TIMESLICE_END
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
            throws DbCompException{
		if (count < 24)
		{
			throw new DbCompException("There are less than 24 hourly samples, can not compute daily sums");
		}

		setOutput(DailyEvap, tally);
		try {
			SetAsFlow(tally, _timeSliceBaseTime);
		} catch (RatingException| NoConversionException | DecodesException e) {
			throw new RuntimeException(e);
		}
		setDailyProfiles(_timeSliceBaseTime);

		//testing
//		hourlyWTP.SaveProfiles();
//		DailyWTP.SaveProfiles();

		tsdb.freeConnection(reservoir.conn);
		crd.close();
		siteDAO.close();
		timeSeriesDAO.close();

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
