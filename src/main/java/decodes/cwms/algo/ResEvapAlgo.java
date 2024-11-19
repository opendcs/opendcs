package decodes.cwms.algo;

import decodes.cwms.CwmsTimeSeriesDb;
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
import ilex.util.TextUtil;
import ilex.var.NamedVariable;
import ilex.var.NoConversionException;
import ilex.var.TimedVariable;
import opendcs.dai.SiteDAI;
import opendcs.dai.TimeSeriesDAI;

import java.util.Date;
import java.util.List;

//AW:IMPORTS

import hec.data.cwmsRating.RatingSet;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;


//AW:IMPORTS_END

//AW:JAVADOC

/**
Run ResEvap Calculations.
 */
//AW:JAVADOC_END
@Algorithm(
		description ="Preform Reservoir Evaporation calculation based on an algorithm developed by NWDM," +
				" Which utilizes air temp, air speed, solar radiation, and water temperature profiles to return" +
				" evaporation rates and total evaporation as flow" )
public class ResEvapAlgo
	extends AW_AlgorithmBase
{
//AW:INPUTS
	@Input
	public double windSpeed;		//AW:TYPECODE=i
	@Input
	public double airTemp;			//AW:TYPECODE=i
	@Input
	public double relativeHumidity;	//AW:TYPECODE=i
	@Input
	public double atmPress;			//AW:TYPECODE=i
	@Input
	public double percentLowCloud;	//AW:TYPECODE=i
	@Input
	public double elevLowCloud;		//AW:TYPECODE=i
	@Input
	public double percentMidCloud;	//AW:TYPECODE=i
	@Input
	public double elevMidCloud;		//AW:TYPECODE=i
	@Input
	public double percentHighCloud;	//AW:TYPECODE=i
	@Input
	public double elevHighCloud;	//AW:TYPECODE=i
	@Input
	public double elev;				//AW:TYPECODE=i
//AW:INPUTS_END

//AW:LOCALVARS
	double tally; //running tally of hourly Evaporation
	int count; //number of days calculated
	double previousHourlyEvap;

	private double startDepth = 0.;
	private double depthIncrement = .5;
	private ResEvap resEvap;
	private CTimeSeries windSpeedTS = null;
	private CTimeSeries airTempTS = null;
	private CTimeSeries relativeHumidityTS = null;
	private CTimeSeries atmPressTS = null;
	private CTimeSeries percentLowCloudTS = null;
	private CTimeSeries elevLowCloudTS = null;
	private CTimeSeries percentMidCloudTS = null;
	private CTimeSeries elevMidCloudTS = null;
	private CTimeSeries percentHighCloudTS = null;
	private CTimeSeries elevHighCloudTS = null;
	private CTimeSeries elevTS = null;

	private CTimeSeries hourlyEvapTS = null;
	private CTimeSeries dailyEvapTS = null;

	Site site;
	CwmsRatingDao crd;
	SiteDAI siteDAO;
	TimeSeriesDAI timeSeriesDAO;
	WaterTempProfiles hourlyWTP;
	WaterTempProfiles dailyWTP;

	EvapReservoir reservoir;

//AW:LOCALVARS_END

//AW:OUTPUTS
	@Output
	public NamedVariable hourlySurfaceTemp = new NamedVariable("hourlySurfaceTemp", 0);
	@Output
	public NamedVariable hourlyEvap = new NamedVariable("hourlyEvap", 0);
	@Output
	public NamedVariable dailyEvap = new NamedVariable("dailyEvap", 0);
	@Output
	public NamedVariable dailyEvapAsFlow = new NamedVariable("dailyEvapAsFlow", 0);
	@Output
	public NamedVariable hourlyFluxOut = new NamedVariable("hourlyFluxOut", 0);
	@Output
	public NamedVariable hourlyFluxIn = new NamedVariable("hourlyFluxIn", 0);
	@Output
	public NamedVariable hourlySolar = new NamedVariable("hourlySolar", 0);
	@Output
	public NamedVariable hourlyLatent = new NamedVariable("hourlyLatent", 0);
	@Output
	public NamedVariable hourlySensible = new NamedVariable("hourlySensible", 0);
//AW:OUTPUTS_END

//AW:PROPERTIES
// TODO Implement Location Levels
//	public String SecchiDepthId;
//	public String MaxTempDepthId;

	@org.opendcs.annotations.PropertySpec(name = "wtpTsId", propertySpecType = PropertySpec.STRING,
			description = "Base String for water Temperature Profiles, Example FTPK-Lower-D000,0m.Temp-Water.Inst.1Day.0.Rev-NWO-Evap")
	public String wtpTsId;
	@org.opendcs.annotations.PropertySpec(name = "reservoirId", propertySpecType = PropertySpec.STRING,
			description = "Location ID of reservoir")
	public String reservoirId;
	@org.opendcs.annotations.PropertySpec(name = "secchi", propertySpecType = PropertySpec.NUMBER,
			description = "Average secchi depth of reservoir in feet")
	public double secchi;
	@org.opendcs.annotations.PropertySpec(name = "zeroElevation", propertySpecType = PropertySpec.NUMBER,
			description = "Streambed elevation of reservoir in feet")
	public double zeroElevation;
	@org.opendcs.annotations.PropertySpec(name = "latitude", propertySpecType = PropertySpec.NUMBER,
			description = "Latitude of reservoir")
	public double latitude;
	@org.opendcs.annotations.PropertySpec(name = "longitude", propertySpecType = PropertySpec.NUMBER,
			description = "Longitude of reservoir")
	public double longitude;
	@org.opendcs.annotations.PropertySpec(name = "windShear", propertySpecType = PropertySpec.STRING,
			description = "Windshear equation to be utilized in computation")
	public String windShear;
	@org.opendcs.annotations.PropertySpec(name = "thermalDifCoe", propertySpecType = PropertySpec.NUMBER,
			description = "Thermal diffusivity coefficient to be utilized in computation")
	public double thermalDifCoe;
	@org.opendcs.annotations.PropertySpec(name = "rating", propertySpecType = PropertySpec.STRING,
			description = "Rating Curve specification for Elevation-Area curve, Example: FTPK.Elev;Area.Linear.Step")
	public String rating;
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
		_aggPeriodVarRoleName = "dailyEvap";
		aggUpperBoundClosed = true;
		aggLowerBoundClosed = false;

//AW:INIT_END

//AW:USERINIT
//AW:USERINIT_END
	}

	//Initialized hourly water temperature profiles and return double[] of WTP of the previous timeSlice before base.
	private double[] getProfiles(String WTPID) throws Exception {
		Date untilTime = new Date(baseTimes.first().getTime() + 86400000);
		hourlyWTP = new WaterTempProfiles(timeSeriesDAO, reservoirId, WTPID, baseTimes.first(), untilTime, startDepth, depthIncrement);
		double[] arrayWTP = new double[hourlyWTP.getTimeSeries().size()];
		for(int i = 0; i < hourlyWTP.getTimeSeries().size(); i++){
			try {
				arrayWTP[i] = hourlyWTP.getTimeSeries().getTimeSeriesAt(i).findPrev(baseTimes.first()).getDoubleValue();
			}
			catch (Exception ex){
				throw new Exception("failed to load data from WTP");
			}
		}
		return arrayWTP;
	}

	//Saves last hourly time slice to dailyWTP object
	private void setDailyProfiles(Date CurrentTime) throws DbCompException {
		double[] arrayWTP = new double[hourlyWTP.getTimeSeries().size()];
		int i = 0;
		for(CTimeSeries CTS: hourlyWTP.getTimeSeries().getAllTimeSeries()){
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
		dailyWTP.setProfiles(arrayWTP, CurrentTime, wtpTsId, reservoirId, zeroElevation, elev, timeSeriesDAO);
	}

	//Returns Converted double of cts from currUnits space to NewUnits
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
	private void setAsFlow(Double TotalEvap , Date CurrentTime) throws NoConversionException, DecodesException, RatingException {
		double evap_to_meters = convertUnits(TotalEvap, dailyEvapTS.getUnitsAbbr(), "m");

		double elev = resEvap.reservoir.getCurrentElevation(CurrentTime);
		double areaMetersSq;
		try {
			areaMetersSq = resEvap.reservoir.intArea(elev);
		}
		catch(RatingException ex){
			throw new RatingException("failed to compute rating", ex);
		}
		double dailyEvapFlow = (areaMetersSq * evap_to_meters)/(86400.);
		setOutput(dailyEvapAsFlow, dailyEvapFlow, _timeSliceBaseTime);
	}

	//TODO Implement Location Levels
	private double getMaxTempDepthMeters(){
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

			//Get site Data from Database
			try {
				DbKey siteID = siteDAO.lookupSiteID(reservoirId);
				site = siteDAO.getSiteById(siteID);
			} catch (DbIoException | NoSuchObjectException e) {
				throw new RuntimeException("Failed to load Site data", e);
			}

			//If missing data overwrite with site info
			if(longitude ==0){
				longitude = Double.parseDouble(site.longitude);
			}
			if(latitude ==0){
				latitude = Double.parseDouble(site.latitude);
			}

			//initialized Water Temperature Profiles
			hourlyWTP = new WaterTempProfiles(timeSeriesDAO, startDepth, depthIncrement);
			dailyWTP = new WaterTempProfiles(timeSeriesDAO, startDepth, depthIncrement);

			//initialized input timeseries
			hourlyEvapTS = getParmRef("hourlyEvap").timeSeries;
			dailyEvapTS = getParmRef("dailyEvap").timeSeries;

			windSpeedTS = getParmRef("windSpeed").timeSeries;
			airTempTS = getParmRef("airTemp").timeSeries;
			relativeHumidityTS = getParmRef("relativeHumidity").timeSeries;
			atmPressTS = getParmRef("atmPress").timeSeries;
			percentLowCloudTS = getParmRef("percentLowCloud").timeSeries;
			elevLowCloudTS = getParmRef("elevLowCloud").timeSeries;
			percentMidCloudTS = getParmRef("percentMidCloud").timeSeries;
			elevMidCloudTS = getParmRef("elevMidCloud").timeSeries;
			percentHighCloudTS = getParmRef("percentHighCloud").timeSeries;
			elevHighCloudTS = getParmRef("elevHighCloud").timeSeries;
			elevTS = getParmRef("elev").timeSeries;


			//initialize MetData
			EvapMetData metData = new EvapMetData();
			metData.setWindSpeedTs(windSpeedTS);
			metData.setAirTempTs(airTempTS);
			metData.setRelHumidityTs(relativeHumidityTS);
			metData.setAirPressureTs(atmPressTS);
			metData.setLowCloudTs(percentLowCloudTS, elevLowCloudTS);
			metData.setMedCloudTs(percentMidCloudTS, elevMidCloudTS);
			metData.setHighCloudTs(percentHighCloudTS, elevHighCloudTS);

			//initialize Evaporation Reservoir
			reservoir = new EvapReservoir();
			reservoir.setName(reservoirId);
			reservoir.setThermalDiffusivityCoefficient(thermalDifCoe);
			try {
				reservoir.setWindShearMethod(WindShearMethod.fromString(windShear));
			} catch (Throwable ex) {
				ex.printStackTrace();
			}

			reservoir.setInputDataIsEnglish(true);
			double longitudeNeg = -longitude; // why make longitude positive?
			reservoir.setLatLon(latitude, longitudeNeg);
			reservoir.setSecchi(secchi);

			RatingSet ratingSet;
			try {
				ratingSet = crd.getRatingSet(rating);
			} catch (RatingException ex) {
				throw new DbCompException("Failed to load rating table", ex);
			}

			ratingSet.setDefaultValueTime(baseTimes.first().getTime());
			reservoir.conn = tsdb.getConnection();
			reservoir.setElevAreaRating(ratingSet);
			reservoir.setInstrumentHeights(32.81, 32.81, 32.81);
			reservoir.setElevationTs(elevTS);

			double initElev;
			try {
				initElev = elevTS.findPrev(baseTimes.first()).getDoubleValue();
			} catch (Exception ex) {
				throw new DbCompException("Failed to load initial Elevation");
			}
			reservoir.setElevation(initElev);
            reservoir.setZeroElevation(zeroElevation);


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
				wtp = getProfiles(wtpTsId);
			} catch (Exception ex) {
				throw new DbCompException("Failed to load profiles");
			}

			// reverse array order
			double[] wtpR = new double[resj + 1];
			for (int i = 0; i < resj + 1; i++) {
				wtpR[i] = wtp[resj - i];
			}

			reservoir.setInitWaterTemperatureProfile(wtpR, resj);

			resEvap.metData = metData;

			//retrieve Evaporation Rate from Previous Timestep to be used to calculate average instantaneous EvapRate over the hour
			try {
				CTimeSeries cts = timeSeriesDAO.makeTimeSeries(hourlyEvapTS.getTimeSeriesIdentifier());
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
				boolean noProblem = resEvap.compute(_timeSliceBaseTime, 0.0);
				if (!noProblem) {
					throw new DbCompException("ResEvap Compute Not Successful. Exiting Script.");
				}
			} catch (Exception ex) {
				throw new DbCompException("ResEvap Compute Not Successful. Exiting Script.", ex);
			}

			List<Double> computedList = resEvap.getComputedMetTimeSeries();
			setOutput(hourlySurfaceTemp, computedList.get(0));
			setOutput(hourlySensible, -computedList.get(1)); //HourlySensible is a negative output energy convert to positive
			setOutput(hourlyLatent, -computedList.get(2)); //HourlyLatent is a negative output energy convert to positive
			setOutput(hourlySolar, computedList.get(3));
			setOutput(hourlyFluxIn, computedList.get(4));
			setOutput(hourlyFluxOut, computedList.get(5));
			setOutput(hourlyEvap, computedList.get(6));

			hourlyWTP.setProfiles(resEvap.getHourlyWaterTempProfile(), _timeSliceBaseTime, wtpTsId, reservoirId, zeroElevation, elev, timeSeriesDAO);

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
			setOutput(dailyEvap, tally, _timeSliceBaseTime);
			try {
				setAsFlow(tally, _timeSliceBaseTime);
			} catch (RatingException| NoConversionException | DecodesException e) {
				throw new RuntimeException(e);
			}
			setDailyProfiles(_timeSliceBaseTime);
		}

		//TODO save HourlyWTP
//		hourlyWTP.SaveProfiles(timeSeriesDAO);
		dailyWTP.SaveProfiles(timeSeriesDAO);

		tsdb.freeConnection(reservoir.conn);
		crd.close();
		siteDAO.close();
		timeSeriesDAO.close();

//AW:AFTER_TIMESLICES
//AW:AFTER_TIMESLICES_END
	}

}
