/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package decodes.cwms.algo;

import decodes.cwms.CwmsLocationLevelDAO;
import decodes.cwms.CwmsTimeSeriesDb;
import decodes.cwms.rating.CwmsRatingDao;
import decodes.cwms.resevapcalc.*;
import decodes.db.Database;
import decodes.db.EngineeringUnit;
import decodes.db.Site;
import decodes.db.UnitConverter;
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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import hec.data.cwmsRating.RatingSet;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;
import org.opendcs.annotations.PropertyRequirements;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.model.cwms.CwmsSiteReferenceValue;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;


@Algorithm(
        description = "Perform Reservoir Evaporation calculation based on an algorithm developed by NWDM," +
                " Which utilizes air temp, air speed, solar radiation, and water temperature profiles to return" +
                " evaporation rates and total evaporation as flow")
@PropertyRequirements(
        groups = {
            @PropertyRequirements.RequirementGroup(
                name = "Location",
                type = PropertyRequirements.RequirementType.AT_LEAST_ONE,
                properties = {"latitude", "longitude"}
            ),
            @PropertyRequirements.RequirementGroup(
                name = "Location2",
                type = PropertyRequirements.RequirementType.ALL_REQUIRED,
                properties = {"zeroElevation", "longitude"}
            )
        }
)
final public class ResEvapAlgo extends AW_AlgorithmBase
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
    private static final String DAILY_EVAP_DEPTH = "dailyEvapDepth";
    private static final String HOURLY_EVAP_RATE = "hourlyEvapRate";

    @Input
    public double windSpeed;
    @Input
    public double airTemp;
    @Input
    public double relativeHumidity;
    @Input
    public double atmPress;
    @Input
    public double percentLowCloud;
    @Input
    public double elevLowCloud;
    @Input
    public double percentMidCloud;
    @Input
    public double elevMidCloud;
    @Input
    public double percentHighCloud;
    @Input
    public double elevHighCloud;
    @Input
    public double elev;


    private double previousElev;
    private double prevInstHrEvapRate;
    private double totalDailyEvapVolumeM3 = 0.0; // aggregated hourly evaporation frustum volume
    private double totalDailyEvapDepthM = 0.0;
    private int count; //number of days calculated
    private boolean isDayLightSavings;

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

    private CTimeSeries hourlyEvapRateTS = null;

    private Site site;
    private CwmsRatingDao crd;
    private SiteDAI siteDAO;
    private Connection conn;
    private TimeSeriesDAI timeSeriesDAO;
    private WaterTempProfiles hourlyWTP;
    private WaterTempProfiles dailyWTP;

    private EvapReservoir reservoir;
    private RatingSet ratingSet;




    @Output
    public NamedVariable hourlySurfaceTemp = new NamedVariable("hourlySurfaceTemp", 0);
    @Output
    public NamedVariable hourlyEvapRate = new NamedVariable(HOURLY_EVAP_RATE, 0);
    @Output
    public NamedVariable hourlyEvapDepth = new NamedVariable("hourlyEvapDepth", 0);
    @Output
    public NamedVariable dailyEvapDepth = new NamedVariable(DAILY_EVAP_DEPTH, 0);
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


// TODO Implement Location Levels
//	public String SecchiDepthId;
//	public String MaxTempDepthId;

    @org.opendcs.annotations.PropertySpec(name = "wtpTsId", propertySpecType = PropertySpec.STRING,
            description = "Base String for water Temperature Profiles, Example FTPK-Lower-D000,0m.Temp-Water.Inst.1Day.0.Rev-NWO-Evap", required = true)
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
    @org.opendcs.annotations.PropertySpec(name = "LocationLevel", propertySpecType = PropertySpec.STRING,
            description = "Location Level ID for Secchi Depth, Example: FTPK.Depth.Const.0.Secchi Depth")
    public String LocationLevel;

    // Allow javac to generate a no-args constructor.

    /**
     * Algorithm-specific initialization provided by the subclass.
     */
    protected void initAWAlgorithm()
            throws DbCompException
    {
        _awAlgoType = AWAlgoType.AGGREGATING;
        _aggPeriodVarRoleName = DAILY_EVAP_DEPTH;
        //aggPeriodInterval = IntervalCodes.int_one_day;
        aggUpperBoundClosed = true;
        aggLowerBoundClosed = false;
    }

    //Initialized hourly water temperature profiles and return double[] of WTP of the previous timeSlice before base.
    private double[] getProfiles(String WTPID) throws DbCompException
    {
        Date sinceTime = new Date(baseTimes.first().getTime() - 86400000);
        if(hourlyWTP == null)
        {
            try
            {
                hourlyWTP = new WaterTempProfiles(timeSeriesDAO, WTPID, sinceTime, baseTimes.first(), startDepth, depthIncrement);
            }
            catch (DbIoException ex)
            {
                throw new DbCompException("Failed to generate Hourly Water temperature profiles", ex);
            }
        }
        double[] arrayWTP = new double[hourlyWTP.getTimeSeries().size()];
        for (int i = 0; i < hourlyWTP.getTimeSeries().size(); i++)
        {
            try
            {
                arrayWTP[i] = hourlyWTP.getTimeSeries().getTimeSeriesAt(i).findPrev(baseTimes.first()).getDoubleValue();
            }
            catch (RuntimeException | NoConversionException ex)
            {
                throw new DbCompException("failed to load data from WTP", ex);
            }
        }
        return arrayWTP;
    }

    //Saves last hourly time slice to dailyWTP object
    private void setDailyProfiles(Date CurrentTime) throws DbCompException
    {
        double[] arrayWTP = new double[hourlyWTP.getTimeSeries().size()];
        int i = 0;
        for (CTimeSeries CTS : hourlyWTP.getTimeSeries().getAllTimeSeries())
        {
            try
            {
                int idx = CTS.findNextIdx(CurrentTime);
                if (idx == -1)
                {
                    break;
                }
                arrayWTP[i] = CTS.sampleAt(idx).getDoubleValue();
            }
            catch (NoConversionException ex)
            {
                throw new DbCompException("Failed to load value from timeseries " + CTS.getNameString() + " at " + CurrentTime.toString(), ex);
            }
            i++;
        }
        dailyWTP.setProfiles(arrayWTP, CurrentTime, wtpTsId, zeroElevation, elev, timeSeriesDAO);
    }

    //Returns Converted double of cts from currUnits space to NewUnits
    public double convertUnits(double cts, String currUnits, String newUnits) throws NoConversionException, DecodesException
    {
        if (TextUtil.strEqualIgnoreCase(currUnits, newUnits) || newUnits == null || currUnits == null)
        {
            return cts;
        }

        EngineeringUnit euOld = EngineeringUnit.getEngineeringUnit(currUnits);
        EngineeringUnit euNew = EngineeringUnit.getEngineeringUnit(newUnits);
        UnitConverter converter = null;
        converter = Database.getDb().unitConverterSet.get(euOld, euNew);
        if (converter == null)
        {
            throw new NoConversionException("failed to load converter between" + currUnits + " to " + newUnits);
        }
        double newValue;
        try
        {
            newValue = converter.convert(cts);
        }
        catch (DecodesException ex)
        {
            throw new DecodesException("failed to preform conversion between" + currUnits + " to " + newUnits, ex);
        }

        return newValue;
    }

    /**
     * Convert hourly evap depth to volume using the frustum formula.
     * Calculates volume of water evaporated based on the average elevation before and after evaporation loss.
     *
     * @param hourlyEvapDepthM  (m)
     * @param elevation (m)
     * @return frustum volume of water evaporated (m3)
     */
    private double convertDepthMToVolumeM3(double hourlyEvapDepthM, double elevation) throws RatingException
    {
        // get elevation before and after evaporation loss
        double elevAfter = elevation - hourlyEvapDepthM;

        // get area at elevation before and after evaporation loss
        double areaAtElevBeforeEvapLoss = getAreaAtElevation(resEvap.reservoir, elevation);
        double areaAtElevAfterEvapLoss = getAreaAtElevation(resEvap.reservoir, elevAfter);

        return getFrustumVolumeM3(areaAtElevBeforeEvapLoss, areaAtElevAfterEvapLoss, hourlyEvapDepthM);
    }


    /**
     * Get the area at a given elevation from the reservoir's rating curve.
     *
     * @param reservoir  EvapReservoir object
     * @param elevation  (m)
     * @return area at elevation (m2)
     */
    double getAreaAtElevation(EvapReservoir reservoir, double elevation) throws RatingException
    {
        return reservoir.intArea(elevation, conn);
    }


    /**
     * Calculate the volume of a frustum given the areas at two elevations and the depth between them.
     *
     * @param areaAtElev1 (m2)
     * @param areaAtElev2 (m2)
     * @param depth       (m)
     * @return volume of the frustum (m3)
     */
    static double getFrustumVolumeM3(double areaAtElev1, double areaAtElev2, double depth)
    {
        double frustumAvgArea = (areaAtElev1 + areaAtElev2 + Math.sqrt(areaAtElev1 * areaAtElev2)) / 3.0;
        return frustumAvgArea * depth;
    }


    /**
     * Convert a volume in cubic meters to a flow rate in cubic meters per second.
     *
     * @param volume (m3)
     * @param secondsPerDay number of seconds in a day, accounts for daylight savings if applicable
     * @return flow rate in cubic meters per second
     */
    static double getVolumeM3AsFlowCMS(double volume, double secondsPerDay)
    {
        return volume / secondsPerDay;
    }



    //TODO Implement Location Levels
    private double getMaxTempDepthMeters()
    {
        return 0;
    }

    @Override
    public void beforeAllTimeSlices() throws DbCompException
    {
        //initialize database connections
        siteDAO = tsdb.makeSiteDAO();
        timeSeriesDAO = tsdb.makeTimeSeriesDAO();
        crd = new CwmsRatingDao((CwmsTimeSeriesDb) tsdb);
        CwmsLocationLevelDAO locLevDAO = null;

        //Get site Data from Database
        try
        {
            conn = tsdb.getConnection();
            DbKey siteID = siteDAO.lookupSiteID(reservoirId);
            site = siteDAO.getSiteById(siteID);
            locLevDAO = new CwmsLocationLevelDAO((CwmsTimeSeriesDb) tsdb);
        }
        catch (DbIoException | NoSuchObjectException ex)
        {
            throw new DbCompException("Failed to load Site data", ex);
        }
        catch (SQLException ex)
        {
            throw new DbCompException("Unable to acquire required connection.", ex);
        }

        try
        {
            ratingSet = crd.getRatingSet(rating);
        }
        catch (RatingException ex)
        {
            throw new DbCompException("Failed to load rating table", ex);
        }

        if (secchi == 0){
            try (org.opendcs.database.api.DataTransaction tx = locLevDAO.getTransaction())
            {
                secchi = ((CwmsSiteReferenceValue) locLevDAO.getLatestLocationLevelValue(tx, LocationLevel, "ft")).getLevelValue();
            }
            catch (OpenDcsDataException ex)
            {
                throw new DbCompException("Failed to load Location Level " + LocationLevel, ex);
            }
        }

        //initialized Water Temperature Profiles
        dailyWTP = new WaterTempProfiles(startDepth, depthIncrement);

    }

    /**
     * This method is called once before iterating all time slices.
     */
    @Override
    protected void beforeTimeSlices()
            throws DbCompException
    {
        totalDailyEvapVolumeM3 = 0.0;
        totalDailyEvapDepthM = 0.0;
        count = 0;

        String evapInterval = getParmRef(DAILY_EVAP_DEPTH).tsid.getInterval();
        String flowInterval = getParmRef("dailyEvapAsFlow").tsid.getInterval();
        boolean evapIR = Objects.equals(evapInterval, IntervalCodes.int_one_day_dst);
        boolean flowIR = Objects.equals(flowInterval, IntervalCodes.int_one_day_dst);


        if (aggTZ.useDaylightTime() && !(evapIR && flowIR))
        {
            throw new DbCompException("Aggregating timezone used daylight savings and dailyEvapAsFlow or dailyEvapDepth is not an irregular daily timeseries");
        }

        int offsetBefore = aggTZ.getOffset(baseTimes.first().getTime() - 12 * 3600 * 1000);
        int offsetAfter = aggTZ.getOffset(baseTimes.first().getTime() + 12 * 3600 * 1000);
        isDayLightSavings = offsetBefore < offsetAfter;

        if (baseTimes.size() == 24 || (baseTimes.size() == 23 && isDayLightSavings))
        {
            if(resEvap == null || reservoir == null)
            {
                setOutputUnitsAbbr("windSpeed", "m/s");
                setOutputUnitsAbbr("airTemp", "C");
                setOutputUnitsAbbr("relativeHumidity", "%");
                setOutputUnitsAbbr("atmPress", "mbar");
                setOutputUnitsAbbr("percentLowCloud", "%");
                setOutputUnitsAbbr("elevLowCloud", "m");
                setOutputUnitsAbbr("percentMidCloud", "%");
                setOutputUnitsAbbr("elevMidCloud", "m");
                setOutputUnitsAbbr("percentHighCloud", "%");
                setOutputUnitsAbbr("elevHighCloud", "m");
                setOutputUnitsAbbr("elev", "m");

                setOutputUnitsAbbr("hourlySurfaceTemp", "C");
                setOutputUnitsAbbr(HOURLY_EVAP_RATE, "mm/hr");
                setOutputUnitsAbbr("hourlyEvapDepth", "mm");
                setOutputUnitsAbbr(DAILY_EVAP_DEPTH, "mm");
                setOutputUnitsAbbr("dailyEvapAsFlow", "cms");
                setOutputUnitsAbbr("hourlyFluxOut", "W/m2");
                setOutputUnitsAbbr("hourlyFluxIn", "W/m2");
                setOutputUnitsAbbr("hourlySolar", "W/m2");
                setOutputUnitsAbbr("hourlyLatent", "W/m2");
                setOutputUnitsAbbr("hourlySensible", "W/m2");

                //If missing data overwrite with site info
                if (longitude == 0)
                {
                    longitude = Double.parseDouble(site.longitude);
                }
                if (latitude == 0)
                {
                    latitude = Double.parseDouble(site.latitude);
                }

                //initialize output timeseries
                hourlyEvapRateTS = getParmRef(HOURLY_EVAP_RATE).timeSeries;

                //initialize input timeseries
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
                try
                {
                    reservoir.setWindShearMethod(WindShearMethod.fromString(windShear));
                }
                catch (RuntimeException ex)
                {
                    // If this failure is acceptable change to a log message that explains what default will
                    // be used but also contains the cause for diagnostics.
                    throw new DbCompException("Unable to set wind shear method.", ex);
                }

                reservoir.setInputDataIsEnglish(true);
                double longitudeNeg = -longitude; // why make longitude positive?
                reservoir.setLatLon(latitude, longitudeNeg);
                reservoir.setSecchi(secchi);

                ratingSet.setDefaultValueTime(baseTimes.first().getTime());
                reservoir.setElevAreaRating(ratingSet);
                reservoir.setInstrumentHeights(32.81, 32.81, 32.81);
                reservoir.setElevationTs(elevTS);

                //retrieve Elevation from Previous Timestep to be used to calculate average instantaneous EvapRate over the hour
                try
                {
                    previousElev = tsdb.getPreviousValue(elevTS, baseTimes.first()).getDoubleValue();
                    reservoir.setElevation(previousElev, conn);
                }
                catch (RuntimeException | NoConversionException | DbIoException | BadTimeSeriesException ex)
                {
                    throw new DbCompException("Failed to load initial elevation before time window of compute", ex);
                }
                catch (RatingException ex)
                {
                    throw new DbCompException("Failed to set the initial elevation", ex);
                }
                reservoir.setZeroElevation(zeroElevation);


                //initialize Reservoir Evaporation object
                resEvap = new ResEvap();
                if (!resEvap.setReservoir(reservoir, conn))
                {
                    throw new DbCompException("Reservoir " + reservoir.getName() + " not in Database. Exiting Script.");
                }

                //get number of water temperature profiles
                int resj = reservoir.getResj();

                //load water temperature profiles
                double[] wtp;
                try
                {
                    wtp = getProfiles(wtpTsId);
                }
                catch (Exception ex)
                {
                    throw new DbCompException("Failed to load initial profiles " + wtpTsId, ex);
                }

                // reverse array order
                double[] wtpR = new double[resj + 1];
                for (int i = 0; i < resj + 1; i++)
                {
                    wtpR[i] = wtp[resj - i];
                }

                reservoir.setInitWaterTemperatureProfile(wtpR, resj);

                resEvap.metData = metData;

                //retrieve Evaporation Rate from Previous Timestep to be used to calculate average instantaneous EvapRate over the hour
                try
                {
                    CTimeSeries cts = timeSeriesDAO.makeTimeSeries(hourlyEvapRateTS.getTimeSeriesIdentifier());
                    cts.setUnitsAbbr("mm/hr");
                    TimedVariable prevTv = tsdb.getPreviousValue(cts, baseTimes.first());
                    if (prevTv != null)
                    {
                        prevInstHrEvapRate = prevTv.getDoubleValue();
                    }
                    else
                    {
                        prevInstHrEvapRate = Double.NaN; // sentinel meaning "unknown"
                        log.debug("No previous hourlyEvapRate value found before {} - will use first-hour fallback.", baseTimes.first());
                    }
                }
                catch (RuntimeException | NoConversionException | DbIoException | BadTimeSeriesException | NoSuchObjectException ex)
                {
                    // Don’t abort the run; log and fall back.
                    prevInstHrEvapRate = Double.NaN;
                    log.warn("Failed to initialize previous hourlyEvapRate; using first-hour fallback.", ex);
                }
            }
        }
    }

    /**
     * Do the algorithm for a single time slice.
     * Automated Workflow (AW) will fill in user-supplied code here.
     * Base class will set inputs prior to calling this method.
     * User code should call one of the setOutput methods for a time-slice
     * output variable.
     *
     * @throws DbCompException (or subclass thereof) if execution of this
     *                         algorithm is to be aborted.
     */
    @Override
    protected void doAWTimeSlice()
            throws DbCompException
    {
        if (baseTimes.size() == 24 || (baseTimes.size() == 23 && isDayLightSavings))
        {
            boolean successful;
            try
            {
                successful = resEvap.compute(_timeSliceBaseTime, 0.0, conn);
            }
            catch (ResEvapException ex)
            {
                throw new DbCompException("ResEvap Compute Not Successful. Exiting Script.", ex);
            }
            if (!successful)
            {
                throw new DbCompException("ResEvap Compute Not Successful. Exiting Script.");
            }

            List<Double> computedList = resEvap.getComputedMetTimeSeries();
            setOutput(hourlySurfaceTemp, computedList.get(0));
            setOutput(hourlySensible, -computedList.get(1)); //HourlySensible is a negative output energy convert to positive
            setOutput(hourlyLatent, -computedList.get(2)); //HourlyLatent is a negative output energy convert to positive
            setOutput(hourlySolar, computedList.get(3));
            setOutput(hourlyFluxIn, computedList.get(4));
            setOutput(hourlyFluxOut, computedList.get(5));
            setOutput(hourlyEvapRate, computedList.get(6)); // inst mm/hr

            hourlyWTP.setProfiles(resEvap.getHourlyWaterTempProfile(), _timeSliceBaseTime, wtpTsId, zeroElevation, elev, timeSeriesDAO);
            count++;

            double currInstHrEvapRate = resEvap.getHourlyEvapRateTimeSeries(); // mm/hr
            double currHrEvapDepthM = convertRateToDepth(currInstHrEvapRate);
            setOutput(hourlyEvapDepth, currHrEvapDepthM * 1000.0);

            double avgHourlyElevation = (elev + previousElev) / 2;

            // aggregate hourly evap volume
            try
            {
                double currHrVolumeM3 = convertDepthMToVolumeM3(currHrEvapDepthM, avgHourlyElevation);
                totalDailyEvapVolumeM3 += currHrVolumeM3;
            }
            catch (RatingException ex)
            {
                throw new DbCompException("Failed to convert hourly evaporation depth to volume", ex);
            }

            totalDailyEvapDepthM += currHrEvapDepthM;
            prevInstHrEvapRate = currInstHrEvapRate;
            previousElev = elev;
        }
    }

    /**
     * Convert hourlyEvapRate rate to depth.
     * Use trapezoid average rate over the hour based on current and previous hourlyEvapRate rates.
     * If the previous hourlyEvapRate rate is unknown, treat it as equal to the current hourlyEvapRate rate.
     *
     * @param currHrEvapRate (mm/hr)
     * @return depth in (m)
     */
    private double convertRateToDepth(double currHrEvapRate)
    {
        // First-hour fallback: if previous is unknown, treat it as equal to current.
        double prevHrEvapRate = Double.isNaN(prevInstHrEvapRate)
                ? currHrEvapRate
                : prevInstHrEvapRate;

        double currHrEvapRateMPerHr = currHrEvapRate / 1000.0;
        double prevHrEvapRateMPerHr = prevHrEvapRate / 1000.0;

        // Average rate over 1 hour to return depth (m)
        return (currHrEvapRateMPerHr + prevHrEvapRateMPerHr) / 2.0;

    }

    /**
     * Finalizes daily evaporation outputs after processing all time slices.
     * Converts total evap volume to flow,
     * Sets outputs: dailyEvapDepth(mm) and dailyEvapAsFlow(cms)
     * Appends the hourly temperature profile.
     * If the number of time steps is not 23 or 24, log a warning and skip daily computation.
     */
    @Override
    protected void afterTimeSlices() throws DbCompException
    {
        // Get the start and end times for aggregation
        long startTimeMillis = baseTimes.first().getTime();
        long endTimeMillis = baseTimes.last().getTime();

        double secondsPerDay = (endTimeMillis - startTimeMillis) / 1000.0;

        if (baseTimes.size() == 24 || (baseTimes.size() == 23 && isDayLightSavings))
        {
            // Convert units to match expected output variable definitions
            double dailyEvapFlowCms = getVolumeM3AsFlowCMS(totalDailyEvapVolumeM3, secondsPerDay);
            double dailyEvapMM = totalDailyEvapDepthM * 1000.0;

            setOutput(dailyEvapDepth, dailyEvapMM, _timeSliceBaseTime);
            setOutput(dailyEvapAsFlow, dailyEvapFlowCms, _timeSliceBaseTime);

            dailyWTP.append(hourlyWTP, _timeSliceBaseTime, timeSeriesDAO);
        }
        else
        {
            log.warn("Found {} hourly values — daily totals require exactly 23 or 24 values. Skipping daily computation.", baseTimes.size());
        }
    }

    @Override
    public void afterAllTimeSlices(){
        try
        {
            //TODO save HourlyWTP
            //		hourlyWTP.SaveProfiles(timeSeriesDAO);
            dailyWTP.SaveProfiles(timeSeriesDAO);
        }
        finally
        {
            tsdb.freeConnection(conn);
            crd.close();
            siteDAO.close();
            timeSeriesDAO.close();
        }
    }


}
