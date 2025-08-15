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
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;


@Algorithm(
        description = "Perform Reservoir Evaporation calculation based on an algorithm developed by NWDM," +
                " Which utilizes air temp, air speed, solar radiation, and water temperature profiles to return" +
                " evaporation rates and total evaporation as flow")
final public class ResEvapAlgo extends AW_AlgorithmBase
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();

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


  
    private double tally; //running tally of hourly Evaporation
    private int count; //number of days calculated
    private boolean isDayLightSavings;
    private double previousHourlyEvap;

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

    // Allow javac to generate a no-args constructor.

    /**
     * Algorithm-specific initialization provided by the subclass.
     */
    protected void initAWAlgorithm()
            throws DbCompException
    {
        _awAlgoType = AWAlgoType.AGGREGATING;
        _aggPeriodVarRoleName = "dailyEvap";
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

    //Converts evaporation to meters then to flow  and save value to output
    private void setAsFlow(Double TotalEvap, Date CurrentTime) throws NoConversionException, DecodesException, RatingException, ResEvapException
    {
        double evap_to_meters = convertUnits(TotalEvap, dailyEvapTS.getUnitsAbbr(), "m");

        double elev = resEvap.reservoir.getCurrentElevation(CurrentTime);
        double areaMetersSq;
        try
        {
            areaMetersSq = resEvap.reservoir.intArea(elev, conn);
        }
        catch (RatingException ex)
        {
            throw new RatingException("failed to compute rating for evaporation to flow", ex);
        }
        double dailyEvapFlow = (areaMetersSq * evap_to_meters) / (86400.);
        setOutput(dailyEvapAsFlow, dailyEvapFlow, _timeSliceBaseTime);
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

        //Get site Data from Database
        try
        {
            conn = tsdb.getConnection();
            DbKey siteID = siteDAO.lookupSiteID(reservoirId);
            site = siteDAO.getSiteById(siteID);
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
        tally = 0.0;
        count = 0;

        String evapInterval = getParmRef("dailyEvap").tsid.getInterval();
        String flowInterval = getParmRef("dailyEvapAsFlow").tsid.getInterval();
        boolean evapIR = Objects.equals(evapInterval, IntervalCodes.int_one_day_dst);
        boolean flowIR = Objects.equals(flowInterval, IntervalCodes.int_one_day_dst);


        if (aggTZ.useDaylightTime() && !(evapIR && flowIR))
        {
            throw new DbCompException("Aggregating timezone used daylight savings and dailyEvapAsFlow or dailyEvap is not an irregular daily timeseries");
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
                setOutputUnitsAbbr("hourlyEvap", "mm/hr");
                setOutputUnitsAbbr("dailyEvap", "mm");
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
                hourlyEvapTS = getParmRef("hourlyEvap").timeSeries;
                dailyEvapTS = getParmRef("dailyEvap").timeSeries;

                //initialized input timeseries
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

                double initElev;
                try
                {
                    initElev = tsdb.getPreviousValue(elevTS, baseTimes.first()).getDoubleValue();
                    reservoir.setElevation(initElev, conn);
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
                    CTimeSeries cts = timeSeriesDAO.makeTimeSeries(hourlyEvapTS.getTimeSeriesIdentifier());
                    cts.setUnitsAbbr("mm/hr");
                    TimedVariable PrevTV = tsdb.getPreviousValue(cts, baseTimes.first());
                    previousHourlyEvap = PrevTV.getDoubleValue();
                }
                catch (Exception ex)
                {
                    throw new DbCompException("Failed to initialize HourlyEvapRate for Evaporate from compute time window", ex);
                }
            }
        }
    }

    /**
     * Do the algorithm for a single time slice.
     * AW will fill in user-supplied code here.
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
            setOutput(hourlyEvap, computedList.get(6));

            hourlyWTP.setProfiles(resEvap.getHourlyWaterTempProfile(), _timeSliceBaseTime, wtpTsId, zeroElevation, elev, timeSeriesDAO);

            count++;
            tally += (previousHourlyEvap + computedList.get(6)) / 2;
            previousHourlyEvap = computedList.get(6);

        }
    }

    /**
     * This method is called once after iterating all time slices.
     */
    @Override
    protected void afterTimeSlices()
            throws DbCompException
    {
        if (baseTimes.size() == 24 || (baseTimes.size() == 23 && isDayLightSavings))
        {
            setOutput(dailyEvap, tally, _timeSliceBaseTime);
            try
            {
                setAsFlow(tally, _timeSliceBaseTime);
            }
            catch (RatingException | NoConversionException | DecodesException ex)
            {
                throw new DbCompException("Failed to compute flow values from evap rate", ex);
            }
            dailyWTP.append(hourlyWTP, _timeSliceBaseTime, timeSeriesDAO);
        }
        else
        {
            log.warn("There are less than 24 hourly samples, can not compute daily sums");
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
