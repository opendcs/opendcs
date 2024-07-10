/* 
 * Copyright (c) 2018
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */
package decodes.cwms.resevapcalc;

import decodes.db.Constants;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.IntervalCodes;
//import hec.heclib.util.HecTime;
//import hec.io.TimeSeriesContainer;
//import rma.util.RMAConst;

import java.util.Date;
import java.util.Objects;

/**
 *  Class hold meteorological time series data for Resevap program.
 */
public class EvapMetData
{
    // input met data timeseries
    CTimeSeries _windspeedTsc;
    CTimeSeries _airTempTsc;
    CTimeSeries _relHumidityTsc;
    CTimeSeries _dewPointTsc;
    CTimeSeries _airPressureTsc;
    CTimeSeries _fractionLowClouds;
    CTimeSeries _altitudeLowClouds;
    CTimeSeries _fractionMedClouds;
    CTimeSeries _altitudeMedClouds;
    CTimeSeries _fractionHighClouds;
    CTimeSeries _altitudeHighClouds;
    
    // these variables hold last valid values
    //was originally RMA undefinedDouble = -FloatMax
    double _wsTemp_old = Constants.undefinedDouble;
    double _windSpeed_old = Constants.undefinedDouble;
    double _airTemp_old = Constants.undefinedDouble;
    double _relHumidity_old = Constants.undefinedDouble;
    double _airPressure_old = Constants.undefinedDouble;
    
    // test variables hold the met values for the current time
    double _wsTemp_current = Constants.undefinedDouble;
    double _windSpeed_current = Constants.undefinedDouble;
    double _airTemp_current = Constants.undefinedDouble;
    double _relHumidity_current = Constants.undefinedDouble;
    double _airPressure_current = Constants.undefinedDouble;
          
    public double getWindSpeed( Date hecTime )
    {
        //return _windspeedTsc.getValue(hecTime);
        return getMetValue( _windspeedTsc, hecTime);
    }
    
    public double getAirTemp( Date hecTime )
    {
        //return _airTempTsc.getValue(hecTime);
        return getMetValue( _airTempTsc, hecTime);
    }
    public double getRelHumidity( Date hecTime )
    {
        //return _relHumidityTsc.getValue(hecTime);
        return getMetValue( _relHumidityTsc, hecTime);
    }
    
    public double getDewPoint( Date hecTime )
    {
        //return _dewPointTsc.getValue(hecTime);
        return getMetValue( _dewPointTsc, hecTime);
    }
    
    public double getAirPressure( Date hecTime )
    {
        //return _airPressureTsc.getValue(hecTime);
        return getMetValue( _airPressureTsc, hecTime);
    }
    
    /**
     * Get CloudCover array for current time 
     * These hold the fractional cloud cover and 
     * base height for the Low, Mid and High cloud
     * divisions.
     * 
     * @param hecTime
     * @return 
     */
    public CloudCover[] getCloudCover( Date hecTime )
    {
        CloudCover[] cloudCover = new CloudCover[3];
//        double fractionCC = _fractionLowClouds.getValue(hecTime);
//        double altitude = _altitudeLowClouds.getValue(hecTime);
        double fractionCC = getMetValue(_fractionLowClouds, hecTime);
        double altitude = getMetValue(_altitudeLowClouds, hecTime);
        
        cloudCover[2] = new CloudCover( fractionCC,
                altitude, CloudCover.CloudHeightType.height_low);

//        fractionCC = _fractionMedClouds.getValue(hecTime);
//        altitude = _altitudeMedClouds.getValue(hecTime);
        fractionCC = getMetValue(_fractionMedClouds, hecTime);
        altitude = getMetValue(_altitudeMedClouds, hecTime);
        
        cloudCover[1] = new CloudCover( fractionCC,
                    altitude, CloudCover.CloudHeightType.height_med);
        
//        fractionCC = _fractionHighClouds.getValue(hecTime);
//        altitude = _altitudeHighClouds.getValue(hecTime);
        fractionCC = getMetValue(_fractionHighClouds, hecTime);
        altitude = getMetValue(_altitudeHighClouds, hecTime);
        
        cloudCover[0] = new CloudCover( fractionCC,
                altitude, CloudCover.CloudHeightType.height_high);
        
        return cloudCover;
    }

    private double getMetValue( CTimeSeries tsc, Date hecTime )
    {
            return tsc.findNextIdx(hecTime);
    }
    
    public void setAirTempTs( CTimeSeries tsc )
    {
        _airTempTsc = tsc;
    }
    
    public void setAirPressureTs( CTimeSeries tsc )
    {
        _airPressureTsc = tsc;
    }

    public void setRelHumidityTs( CTimeSeries tsc )
    {
        _relHumidityTsc = tsc;
    }

    public void setWindSpeedTs( CTimeSeries tsc )
    {
        _windspeedTsc = tsc;
    }
    
    public void setHighCloudTs( CTimeSeries tscFrac, CTimeSeries tscHeight )
    {
        _fractionHighClouds = tscFrac;
        _altitudeHighClouds = tscHeight;
    }
    
    public void setMedCloudTs( CTimeSeries tscFrac, CTimeSeries tscHeight )
    {
        _fractionMedClouds = tscFrac;
        _altitudeMedClouds = tscHeight;
    }
    
    public void setLowCloudTs( CTimeSeries tscFrac, CTimeSeries tscHeight )
    {
        _fractionLowClouds = tscFrac;
        _altitudeLowClouds = tscHeight;
    }

}
