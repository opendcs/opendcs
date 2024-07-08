/* 
 * Copyright (c) 2018
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */
package decodes.cwms.resevapcalc;

import hec.heclib.util.HecTime;
import hec.io.TimeSeriesContainer;
import rma.util.RMAConst;

/**
 *  Class hold meteorological time series data for Resevap program.
 */
public class EvapMetData
{
    // input met data timeseries
    TimeSeriesContainer _windspeedTsc;
    TimeSeriesContainer _airTempTsc;
    TimeSeriesContainer _relHumidityTsc;
    TimeSeriesContainer _dewPointTsc;
    TimeSeriesContainer _airPressureTsc;
    TimeSeriesContainer _fractionLowClouds;
    TimeSeriesContainer _altitudeLowClouds;
    TimeSeriesContainer _fractionMedClouds;
    TimeSeriesContainer _altitudeMedClouds;
    TimeSeriesContainer _fractionHighClouds;
    TimeSeriesContainer _altitudeHighClouds;
    
    // these variables hold last valid values
    double _wsTemp_old = RMAConst.UNDEF_DOUBLE;
    double _windSpeed_old = RMAConst.UNDEF_DOUBLE;
    double _airTemp_old = RMAConst.UNDEF_DOUBLE;
    double _relHumidity_old = RMAConst.UNDEF_DOUBLE;
    double _airPressure_old = RMAConst.UNDEF_DOUBLE;
    
    // test variables hold the met values for the current time
    double _wsTemp_current = RMAConst.UNDEF_DOUBLE;
    double _windSpeed_current = RMAConst.UNDEF_DOUBLE;
    double _airTemp_current = RMAConst.UNDEF_DOUBLE;
    double _relHumidity_current = RMAConst.UNDEF_DOUBLE;
    double _airPressure_current = RMAConst.UNDEF_DOUBLE;
          
    public double getWindSpeed( HecTime hecTime )
    {
        //return _windspeedTsc.getValue(hecTime);
        return getMetValue( _windspeedTsc, hecTime);
    }
    
    public double getAirTemp( HecTime hecTime )
    {
        //return _airTempTsc.getValue(hecTime);
        return getMetValue( _airTempTsc, hecTime);
    }
    public double getRelHumidity( HecTime hecTime )
    {
        //return _relHumidityTsc.getValue(hecTime);
        return getMetValue( _relHumidityTsc, hecTime);
    }
    
    public double getDewPoint( HecTime hecTime )
    {
        //return _dewPointTsc.getValue(hecTime);
        return getMetValue( _dewPointTsc, hecTime);
    }
    
    public double getAirPressure( HecTime hecTime )
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
    public CloudCover[] getCloudCover( HecTime hecTime )
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

    private double getMetValue( TimeSeriesContainer tsc, HecTime hecTime )
    {
        // if regular interval data, use existing method.
        if ( tsc.interval > 0 )
        {
            return tsc.getValue(hecTime);
        }
        
        // looks like irregular data.  Return undef value if no data for the time
        // perform no interpolation
        long findTime = hecTime.value();
        long sTimeValue = tsc.startTime;
        long eTimeValue = tsc.endTime;

        if ( findTime < sTimeValue || findTime > eTimeValue )
        {
            return hec.lang.Const.UNDEFINED_DOUBLE;
        }
        
        for ( int i=0; i<tsc.times.length; i++)
        {
            if ( tsc.times[i] == findTime )
            {
                return tsc.values[i];
            }
            if ( tsc.times[i] > findTime )
            {
                return hec.lang.Const.UNDEFINED_DOUBLE;
            }
        }
        return hec.lang.Const.UNDEFINED_DOUBLE;
    }
    
    public void setAirTempTs( TimeSeriesContainer tsc )
    {
        _airTempTsc = tsc;
    }
    
    public void setAirPressureTs( TimeSeriesContainer tsc )
    {
        _airPressureTsc = tsc;
    }

    public void setRelHumidityTs( TimeSeriesContainer tsc )
    {
        _relHumidityTsc = tsc;
    }

    public void setWindSpeedTs( TimeSeriesContainer tsc )
    {
        _windspeedTsc = tsc;
    }
    
    public void setHighCloudTs( TimeSeriesContainer tscFrac, TimeSeriesContainer tscHeight )
    {
        _fractionHighClouds = tscFrac;
        _altitudeHighClouds = tscHeight;
    }
    
    public void setMedCloudTs( TimeSeriesContainer tscFrac, TimeSeriesContainer tscHeight )
    {
        _fractionMedClouds = tscFrac;
        _altitudeMedClouds = tscHeight;
    }
    
    public void setLowCloudTs( TimeSeriesContainer tscFrac, TimeSeriesContainer tscHeight )
    {
        _fractionLowClouds = tscFrac;
        _altitudeLowClouds = tscHeight;
    }

}
