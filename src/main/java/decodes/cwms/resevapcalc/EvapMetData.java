/* 
 * Copyright (c) 2018
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */
package decodes.cwms.resevapcalc;

import decodes.db.Constants;
import decodes.tsdb.CTimeSeries;

import java.util.Date;

/**
 *  Class hold meteorological time series data for Resevap program.
 */
public class EvapMetData
{
    // input met data timeseries
    CTimeSeries windspeedTsc;
    CTimeSeries airTempTsc;
    CTimeSeries relHumidityTsc;
    CTimeSeries dewPointTsc;
    CTimeSeries airPressureTsc;
    CTimeSeries fractionLowClouds;
    CTimeSeries altitudeLowClouds;
    CTimeSeries fractionMedClouds;
    CTimeSeries altitudeMedClouds;
    CTimeSeries fractionHighClouds;
    CTimeSeries altitudeHighClouds;
    
    // these variables hold last valid values
    //was originally RMA undefinedDouble = -FloatMax
    double wsTempOld = Constants.undefinedDouble;
    double windSpeedOld = Constants.undefinedDouble;
    double airTempOld = Constants.undefinedDouble;
    double relHumidityOld = Constants.undefinedDouble;
    double airPressureOld = Constants.undefinedDouble;
    
    // test variables hold the met values for the current time
    double wsTempCurrent = Constants.undefinedDouble;
    double windSpeedCurrent = Constants.undefinedDouble;
    double airTempCurrent = Constants.undefinedDouble;
    double relHumidityCurrent = Constants.undefinedDouble;
    double airPressureCurrent = Constants.undefinedDouble;
          
    public double getWindSpeed( Date time ) throws ResEvapException {
        return getMetValue(windspeedTsc, time);
    }
    
    public double getAirTemp( Date time ) throws ResEvapException {
        return getMetValue(airTempTsc, time);
    }
    public double getRelHumidity( Date time ) throws ResEvapException {
        return getMetValue(relHumidityTsc, time);
    }
    
    public double getDewPoint( Date time ) throws ResEvapException {
        return getMetValue(dewPointTsc, time);
    }
    
    public double getAirPressure( Date time ) throws ResEvapException {
        return getMetValue(airPressureTsc, time);
    }
    
    /**
     * Get CloudCover array for current time 
     * These hold the fractional cloud cover and 
     * base height for the Low, Mid and High cloud
     * divisions.
     * 
     * @param time
     * @return 
     */
    public CloudCover[] getCloudCover( Date time ) throws ResEvapException {
        CloudCover[] cloudCover = new CloudCover[3];
        double fractionCC = getMetValue(fractionLowClouds, time);
        double altitude = getMetValue(altitudeLowClouds, time);
        
        cloudCover[2] = new CloudCover( fractionCC,
                altitude, CloudCover.CloudHeightType.HEIGHT_LOW);

        fractionCC = getMetValue(fractionMedClouds, time);
        altitude = getMetValue(altitudeMedClouds, time);
        
        cloudCover[1] = new CloudCover( fractionCC,
                    altitude, CloudCover.CloudHeightType.HEIGHT_MED);

        fractionCC = getMetValue(fractionHighClouds, time);
        altitude = getMetValue(altitudeHighClouds, time);
        
        cloudCover[0] = new CloudCover( fractionCC,
                altitude, CloudCover.CloudHeightType.HEIGHT_HIGH);
        
        return cloudCover;
    }

    private double getMetValue( CTimeSeries tsc, Date time ) throws ResEvapException {
            int idx = tsc.findNextIdx(time);
            double value;
            try{
                value = tsc.sampleAt(idx).getDoubleValue();
            }
            catch (Exception ex){
                throw new ResEvapException("failed to load met value from timeseries", ex);
            }
            return value;
    }
    
    public void setAirTempTs( CTimeSeries tsc )
    {
        airTempTsc = tsc;
    }
    
    public void setAirPressureTs( CTimeSeries tsc )
    {
        airPressureTsc = tsc;
    }

    public void setRelHumidityTs( CTimeSeries tsc )
    {
        relHumidityTsc = tsc;
    }

    public void setWindSpeedTs( CTimeSeries tsc )
    {
        windspeedTsc = tsc;
    }
    
    public void setHighCloudTs( CTimeSeries tscFrac, CTimeSeries tscHeight )
    {
        fractionHighClouds = tscFrac;
        altitudeHighClouds = tscHeight;
    }
    
    public void setMedCloudTs( CTimeSeries tscFrac, CTimeSeries tscHeight )
    {
        fractionMedClouds = tscFrac;
        altitudeMedClouds = tscHeight;
    }
    
    public void setLowCloudTs( CTimeSeries tscFrac, CTimeSeries tscHeight )
    {
        fractionLowClouds = tscFrac;
        altitudeLowClouds = tscHeight;
    }

}
