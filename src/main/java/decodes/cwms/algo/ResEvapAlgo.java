package decodes.cwms.algo;

import decodes.cwms.CwmsTimeSeriesDb;
import decodes.cwms.CwmsTsId;
import decodes.cwms.rating.CwmsRatingDao;
import decodes.cwms.resevapcalc.*;
import decodes.db.*;
import decodes.sql.DbKey;
import decodes.tsdb.*;
import decodes.tsdb.algo.AWAlgoType;
import decodes.tsdb.algo.AW_AlgorithmBase;
import decodes.util.DecodesException;
import decodes.util.PropertySpec;
import hec.data.RatingException;
import hec.data.cwmsRating.AbstractRating;
import ilex.util.TextUtil;
import ilex.var.NamedVariable;
import ilex.var.NoConversionException;
import ilex.var.TimedVariable;
import ilex.var.Variable;
import opendcs.dai.SiteDAI;
import opendcs.dai.TimeSeriesDAI;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

//AW:IMPORTS

//import hec.data.RatingException;
import hec.data.cwmsRating.RatingSet;
import opendcs.dao.DaoBase;
import org.opendcs.utils.FailableResult;


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
	double tally; //running tally of hourly Evaporation
	int count; //number of days calculated
	double previousHourlyEvap;

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
	private CTimeSeries DailyEvapTS = null;

	Site site;
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
        _awAlgoType = AWAlgoType.AGGREGATING;
		_aggPeriodVarRoleName = "DailyEvap";
		aggUpperBoundClosed = true;
		aggLowerBoundClosed = false;

//AW:INIT_END

//AW:USERINIT
//AW:USERINIT_END
	}

	//Initialized hourly water temperature profiles and return double[] of WTP of the previous timeSlice before base.
	public double[] getProfiles(String WTPID) throws Exception {
		Date untilTime = new Date(baseTimes.first().getTime() + 86400000);
		hourlyWTP = new WaterTempProfiles(timeSeriesDAO, reservoirId, WTPID, baseTimes.first(), untilTime, start_depth, depth_increment);
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

	//Saves double[] of WTP to WTP object at time step.
	public void setProfiles(WaterTempProfiles newWTP, double[] wtp, Date CurrentTime, String WTPID) throws DbCompException {
		double currentDepth = start_depth;
		for(int i = 0; i < wtp.length && currentDepth+(Zero_elevation*0.3048)<=Elev; i++){
			if(i+1>newWTP.tseries.size()){
				try{
					TimeSeriesIdentifier tsid = timeSeriesDAO.getTimeSeriesIdentifier(WTPID);
					TimeSeriesIdentifier newTSID = tsid.copyNoKey();

					Site newsite =  new Site();
					newsite.copyFrom(newTSID.getSite());
					SiteName strsite = newsite.getName(Constants.snt_CWMS);

					DecimalFormat decimalFormat = new DecimalFormat("000.0");
					String formattedNumber = decimalFormat.format(currentDepth).replace(".", ",");
					strsite.setNameValue(reservoirId+"-D"+formattedNumber+"m");
					newsite.addName(strsite);
					newTSID.setSite(newsite);
					newTSID.setSiteName(strsite.getDisplayName());

					CTimeSeries CTProfile;
					FailableResult<TimeSeriesIdentifier, TsdbException> check = timeSeriesDAO.findTimeSeriesIdentifier(newTSID.getUniqueString());
					if(check.isSuccess()) {
						CTProfile = timeSeriesDAO.makeTimeSeries(check.getSuccess());
					}
					else if(check.getFailure() instanceof NoSuchObjectException){
						timeSeriesDAO.createTimeSeries(newTSID);
						CTProfile = new CTimeSeries(newTSID);
					}
					else{
						throw new DbIoException("failed to load time series from database", check.getFailure());
					}

					TimedVariable newTV = new TimedVariable(new Variable(wtp[i]), CurrentTime);
					newTV.setFlags(VarFlags.TO_WRITE);
					CTProfile.addSample(newTV);
					newWTP.tseries.addTimeSeries(CTProfile);
				}
				catch (Exception ex){
					throw new DbCompException("failed to create new timeSeriesID"+ex);
				}
			}
			else{
				// todo might need to change
				CTimeSeries CTProfile = newWTP.tseries.getTimeSeriesAt(i);
				TimedVariable newTV = new TimedVariable(new Variable(wtp[i]), CurrentTime);
				newTV.setFlags(VarFlags.TO_WRITE);
				CTProfile.addSample(newTV);
			}
			currentDepth += depth_increment;
		}
	}

	//Saves last hourly time slice to dailyWTP object
	public void setDailyProfiles(Date CurrentTime) throws DbCompException {
		double[] arrayWTP = new double[hourlyWTP.tseries.size()];
		int i = 0;
		for(CTimeSeries CTS: hourlyWTP.tseries.getAllTimeSeries()){
			try{
				int idx = CTS.findNextIdx(CurrentTime);
				if(idx == -1){
					break;
				}
				arrayWTP[i] = CTS.sampleAt(idx).getDoubleValue();
			}catch(NoConversionException ex){
				throw new DbCompException("Failed to load value from timeseries"+ex);
			}
			i++;
		}
		setProfiles(DailyWTP, arrayWTP, CurrentTime, WtpTsid);
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

	//Converts evaporation to meters then to flow  and save value to output
	public void SetAsFlow(Double TotalEvap ,Date CurrentTime) throws NoConversionException, DecodesException, RatingException {
		double evap_to_meters = convertUnits(TotalEvap, DailyEvapTS.getUnitsAbbr(), "m");

		double elev = resEvap.reservoir.getCurrentElevation(CurrentTime);
		double areaMetersSq;
		try {
			areaMetersSq = resEvap.reservoir.intArea(elev);
		}
		catch(RatingException ex){
			throw new RatingException("failed to compute rating", ex);
		}
		double dailyEvapFlow = (areaMetersSq * evap_to_meters)/(86400.);
		setOutput(DailyEvapAsFlow, dailyEvapFlow, _timeSliceBaseTime);
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
		if (baseTimes.size() == 24) {

			//initialize database connections
			siteDAO = tsdb.makeSiteDAO();
			timeSeriesDAO = tsdb.makeTimeSeriesDAO();
			crd = new CwmsRatingDao((CwmsTimeSeriesDb) tsdb);

			try {
				DbKey siteID = siteDAO.lookupSiteID(reservoirId);
				site = siteDAO.getSiteById(siteID);
			} catch (DbIoException | NoSuchObjectException e) {
				throw new RuntimeException("Failed to load Site data", e);
			}

			if(Longi==0){
				Longi = Double.parseDouble(site.longitude);
			}
			if(Lati==0){
				Lati = Double.parseDouble(site.latitude);
			}
			if(isMissing(Timezone)){
				Timezone = site.timeZoneAbbr;
			}

			//initialized Water Temperature Profiles
			hourlyWTP = new WaterTempProfiles(timeSeriesDAO, start_depth, depth_increment);
			DailyWTP = new WaterTempProfiles(timeSeriesDAO, start_depth, depth_increment);

			//initialized input timeseries
			HourlyEvapTS = getParmRef("HourlyEvap").timeSeries;
			DailyEvapTS = getParmRef("DailyEvap").timeSeries;

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


			//initialize MetData
			EvapMetData metData = new EvapMetData();
			metData.setWindSpeedTs(windSpeedTS);
			metData.setAirTempTs(AirTempTS);
			metData.setRelHumidityTs(RelativeHumidityTS);
			metData.setAirPressureTs(AtmPressTS);
			metData.setLowCloudTs(PercentLowCloudTS, ElevLowCloudTS);
			metData.setMedCloudTs(PercentMidCloudTS, ElevMidCloudTS);
			metData.setHighCloudTs(PercentHighCloudTS, ElevHighCloudTS);

			//initialize Evaporation Reservoir
			reservoir = new EvapReservoir();
			reservoir.setName(reservoirId);
			reservoir.setThermalDiffusivityCoefficient(ThermalDifCoe);
			try {
				reservoir.setWindShearMethod(WindShearMethod.fromString(WindShear));
			} catch (Throwable ex) {
				ex.printStackTrace();
			}

			reservoir.setInputDataIsEnglish(true);
			double lonneg = -Longi; // why make longitude positive?
			reservoir.setLatLon(Lati, lonneg);
			reservoir.setSecchi(Secchi);

			RatingSet ratingSet;
			try {
				ratingSet = crd.getRatingSet(Rating);
			} catch (RatingException ex) {
				throw new DbCompException("failed to load rating table", ex);
			}

			ratingSet.setDefaultValueTime(baseTimes.first().getTime());
			reservoir.conn = tsdb.getConnection();
			reservoir.setElevAreaRating(ratingSet);
			reservoir.setInstrumentHeights(32.81, 32.81, 32.81);
			reservoir.setElevationTs(ElevTS);

			double initElev;
			try {
				initElev = ElevTS.findPrev(baseTimes.first()).getDoubleValue();
			} catch (Exception ex) {
				throw new DbCompException("failed to load initial Elevation");
			}
			reservoir.setElevation(initElev);
            reservoir.setZeroElevation(Zero_elevation);


			//initialize Reservoir Evaporation object
			resEvap = new ResEvap();
			if (!resEvap.setReservoir(reservoir)) {
				throw new DbCompException("Reservoir not in Database. Exiting Script.");
			}

			//get number of water temperature profiles
			int resj = reservoir.getResj();

			//load water temperature profiles
			double[] wtp;
			try {
				wtp = getProfiles(WtpTsid);
			} catch (Exception ex) {
				throw new DbCompException("failed to load profiles");
			}

			// reverse array order
			double[] wtpR = new double[resj + 1];
			for (int i = 0; i < resj + 1; i++) {
				wtpR[i] = wtp[resj - i];
			}

			reservoir.setInitWaterTemperatureProfile(wtpR, resj);

			resEvap.metData = metData;

			try {
				CTimeSeries cts = timeSeriesDAO.makeTimeSeries(HourlyEvapTS.getTimeSeriesIdentifier());
				cts.setUnitsAbbr("mm/hr");
				TimedVariable n = timeSeriesDAO.getPreviousValue(cts, baseTimes.first());
				previousHourlyEvap = n.getDoubleValue();
			} catch (Exception ex) {
				throw new DbCompException("Failed to initialize HourlyEvapRate");
			}
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
	@Override
	protected void doAWTimeSlice()
            throws DbCompException {
//AW:TIMESLICE
		if (baseTimes.size() == 24) {
			try {
				boolean noProblem = resEvap.compute(_timeSliceBaseTime, GMT_Offset);
				if (!noProblem) {
					throw new DbCompException("ResEvap Compute Not Successful. Exiting Script.");
				}
			} catch (Exception ex) {
				throw new DbCompException("ResEvap Compute Not Successful. Exiting Script.", ex);
			}

			List<Double> computedList = resEvap.getComputedMetTimeSeries();
			setOutput(HourlySurfaceTemp, computedList.get(0));
			setOutput(HourlySensible, -computedList.get(1));
			setOutput(HourlyLatent, -computedList.get(2));
			setOutput(HourlySolar, computedList.get(3));
			setOutput(HourlyFluxIn, computedList.get(4));
			setOutput(HourlyFluxOut, computedList.get(5));
			setOutput(HourlyEvap, computedList.get(6));

			setProfiles(hourlyWTP, resEvap.getHourlyWaterTempProfile(), _timeSliceBaseTime, WtpTsid);

			count++;
			tally += (previousHourlyEvap + computedList.get(6))/2;
			previousHourlyEvap = computedList.get(6);

		}
//AW:TIMESLICE_END
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
            throws DbCompException{
		if (count < 24)
		{
			warning("There are less than 24 hourly samples, can not compute daily sums");
		}
		else{
			setOutput(DailyEvap, tally, _timeSliceBaseTime);
			try {
				SetAsFlow(tally, _timeSliceBaseTime);
			} catch (RatingException| NoConversionException | DecodesException e) {
				throw new RuntimeException(e);
			}
			setDailyProfiles(_timeSliceBaseTime);
		}

		//testing
//		hourlyWTP.SaveProfiles(timeSeriesDAO);
		DailyWTP.SaveProfiles(timeSeriesDAO);

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
