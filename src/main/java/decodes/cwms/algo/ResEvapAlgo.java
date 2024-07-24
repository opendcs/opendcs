package decodes.cwms.algo;

import decodes.cwms.CwmsTimeSeriesDb;
import decodes.cwms.rating.CwmsRatingDao;
import decodes.cwms.resevapcalc.EvapMetData;
import decodes.cwms.resevapcalc.EvapReservoir;
import decodes.cwms.resevapcalc.ResEvap;
import decodes.cwms.resevapcalc.WindShearMethod;
import decodes.db.*;
import decodes.tsdb.*;
import decodes.tsdb.algo.AWAlgoType;
import decodes.tsdb.algo.AW_AlgorithmBase;
import decodes.util.DecodesException;
import ilex.util.Logger;
import ilex.util.TextUtil;
import ilex.var.NamedVariable;
import ilex.var.NoConversionException;
import ilex.var.TimedVariable;
import ilex.var.Variable;
import opendcs.dai.SiteDAI;
import opendcs.dai.TimeSeriesDAI;
import opendcs.util.functional.ThrowingFunction;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

//AW:IMPORTS

//import hec.data.RatingException;
import hec.data.cwmsRating.RatingSet;
import hec.data.cwmsRating.RatingValue;
import hec.data.cwmsRating.TableRating;


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

	SiteDAI siteDAO;
	TimeSeriesDAI timeSeriesDAO;
	WaterTempProfiles hourlyWTP;
	WaterTempProfiles DailyWTP;

//AW:LOCALVARS_END

//AW:OUTPUTS
	public NamedVariable DailyWaterTempProfile 	= new NamedVariable("DailyWaterTempProfile", 0);
	public NamedVariable HourlyWaterTempProfile	= new NamedVariable("HourlyWaterTempProfile", 0);
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
	public String WtpTsid;

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

        siteDAO =  tsdb.makeSiteDAO();
        timeSeriesDAO = tsdb.makeTimeSeriesDAO();

		hourlyWTP = new WaterTempProfiles(timeSeriesDAO, start_depth, depth_increment);
		DailyWTP = new WaterTempProfiles(timeSeriesDAO, start_depth, depth_increment);

        _awAlgoType = AWAlgoType.TIME_SLICE;
//AW:INIT_END

//AW:USERINIT
//AW:USERINIT_END
	}
	public TimeSeriesIdentifier makeTSID(String tsIdStr) throws DbIoException {
		try {
			return timeSeriesDAO.getTimeSeriesIdentifier(tsIdStr);
		} catch (NoSuchObjectException ex) {
			//log.warn("No existing time series. Will attempt to create.");
			try {
				TimeSeriesIdentifier tsId = tsdb.makeEmptyTsId();
				tsId.setUniqueString(tsIdStr);
				Site site = tsdb.getSiteById(siteDAO.lookupSiteID(tsId.getSiteName()));
				if (site == null) {
					site = new Site();
					site.addName(new SiteName(site, Constants.snt_CWMS, tsId.getSiteName()));
					siteDAO.writeSite(site);
				}
				tsId.setSite(site);
				//log.info("Calling createTimeSeries");
				timeSeriesDAO.createTimeSeries(tsId);
				//log.info("After createTimeSeries, ts key = {}", tsId.getKey());
				return tsId;
			} catch (Exception ex2) {
				throw new DbIoException(String.format("No such time series and cannot create for '%'", tsIdStr), ex);
			}
		}
	}
	//TODO read database
	public double getStartElevation(){
		return 0;
	}

	public double[] getProfiles() throws Exception {
		Date LastTime = HourlyEvapTS.findPrev(_timeSliceBaseTime).getTime();
		hourlyWTP = new WaterTempProfiles(timeSeriesDAO, WtpTsid, LastTime, _timeSliceBaseTime, start_depth, depth_increment);
		double[] arrayWTP = new double[hourlyWTP.size()];
		for(int i = 0; i < hourlyWTP.size(); i++){
			try {
				arrayWTP[i] = hourlyWTP.getTimeSeriesAt(i).findPrev(_timeSliceBaseTime).getDoubleValue();
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
			if(i+1>newWTP.size()){
				try{
					TimeSeriesIdentifier newTSID = makeTSID(WtpTsid+currentDepth);
					CTimeSeries CTProfile = new CTimeSeries(newTSID);
					CTProfile.addSample(new TimedVariable(new Variable(wtp[i]), CurrentTime));
					newWTP.addTimeSeries(CTProfile);
				}
					catch (Exception ex){
					throw new DbCompException("failed to create new timeSeriesID"+ex);
				}
			}
			else{
				CTimeSeries CTProfile = newWTP.getTimeSeriesAt(i);
				CTProfile.addSample(new TimedVariable(new Variable(wtp[i]), CurrentTime));
			}
			currentDepth += depth_increment;
		}
	}

	public void setDailyProfiles(Date CurrentTime) throws DbCompException {
		double[] arrayWTP = new double[hourlyWTP.size()];
		int i = 0;
		Date newTime = CurrentTime;
		for(CTimeSeries CTS: hourlyWTP.getAllTimeSeries()){
			try{
				arrayWTP[i] = CTS.findPrev(CurrentTime).getDoubleValue();
				newTime = CTS.findPrev(CurrentTime).getTime();
			}catch(NoConversionException ex){
				throw new DbCompException("Failed to load value from timeseries"+ex);
			}
			i++;
		}
		setProfiles(DailyWTP, arrayWTP, newTime);
	}

	public void calcDailyEvap(NamedVariable output, CTimeSeries tsc,  Date CurrentTime) throws DbCompException, NoConversionException, DecodesException {
		double TotalEvap = calcDailyACC(tsc, CurrentTime);
		Date LastTime = tsc.findPrev(CurrentTime).getTime();
		setOutput(output, TotalEvap, LastTime);
		SetAsFlow(TotalEvap, LastTime);
	}

	public double calcDailyACC(CTimeSeries tsc,  Date CurrentTime) throws DbCompException {
		TimedVariable loopVar = tsc.findPrev(CurrentTime);
		Date loopTime = loopVar.getTime();
		Date pastTime;
		double total = 0;
		do{
			try {
				total += loopVar.getDoubleValue();
				pastTime = loopTime;
				loopVar = tsc.findPrev(loopTime);
				loopTime = loopVar.getTime();

			}
			catch(Exception ex){
				throw new DbCompException("Failed to load past Evap values"+ex);
			}
		}while(loopTime.getDate() == pastTime.getDate());

		return total;
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

	public void SetAsFlow(Double TotalEvap ,Date CurrentTime) throws NoConversionException, DecodesException {
		double evap_to_meters = convertUnits(TotalEvap, HourlyEvapTS.getUnitsAbbr(), "m");

		double elev = resEvap._reservoir.getCurrentElevation(CurrentTime);
		double areaMetersSq = resEvap._reservoir.intArea(elev);
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
		throws DbCompException
	{
//AW:BEFORE_TIMESLICES
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

		EvapReservoir reservoir = new EvapReservoir();
		reservoir.setName(reservoirId);
		reservoir.setThermalDiffusivityCoefficient(ThermalDifCoe);
		reservoir.setWindShearMethod(WindShearMethod.valueOf(WindShear));

		reservoir.setInputDataIsEnglish(true);
		double lonneg = -Longi;
		reservoir.setLatLon(Lati, lonneg);
		reservoir.setSecchi(Secchi);

		CwmsRatingDao crd = new CwmsRatingDao((CwmsTimeSeriesDb)tsdb);
		RatingSet ratingSet = crd.getRatingSet(Rating);
		TableRating[] ratings = (TableRating[]) ratingSet.getRatings();
		int rcount = ratingSet.getRatingCount();
		TableRating rate0 = ratings[rcount-1];
		RatingValue[] rt = rate0.getRatingValues();

		// Use area units to determine unit system
		String[] runits = rate0.getRatingUnits();
		boolean is_english = false;
//		unit_system = Units.getUnitSystemForUnits(runits[1]);
//		if (unit_system == Units.ENGLISH_ID){
//			is_english = true;
//		}

		double[] elev_array = new double[rcount];
		double[] area_array = new double[rcount];
		for ( int i =0; i< rcount; i++){
			RatingValue rv = rt[i];
			elev_array[i] = rv.getIndValue();
			area_array[i] = rv.getDepValue();
		}
		reservoir.setElevAreaCurve(elev_array, area_array, rcount, is_english);



		reservoir.setInstrumentHeights(32.81, 32.81, 32.81);

		//double startElev = getStartElevation();

		reservoir.setElevationTs(ElevTS);
		reservoir.setElevation(ElevTS.findNextIdx(_timeSliceBaseTime));
		reservoir.setZeroElevation(Zero_elevation);

		resEvap = new ResEvap();

		if (!resEvap.setReservoir(reservoir)) {
			throw new DbCompException("Reservoir not in Database. Exiting Script.");
		}

		// reservoir structure setup so now set wtemp profile
		int resj = reservoir.getResj();

//		print("ResJ: " + str(resj));
//		print("WTP len: " + str(len(wtp)));
		double[] wtp;
		try{
			wtp = getProfiles();
		}
		catch(Exception ex){
			throw new DbCompException("failed to load profiles");
		}
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

		if(_timeSliceBaseTime.getDate() != LastDate.getDate()){
            try {
                calcDailyEvap(DailyEvap, HourlyEvapTS, _timeSliceBaseTime);
            } catch (NoConversionException | DecodesException e) {
                throw new RuntimeException(e);
            }
            setDailyProfiles(_timeSliceBaseTime);
		}
		LastDate = _timeSliceBaseTime;
//AW:TIMESLICE_END
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
		throws DbCompException
	{
		hourlyWTP.SaveProfiles();
		DailyWTP.SaveProfiles();
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
