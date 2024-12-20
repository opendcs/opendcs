/*
 * Where Applicable, Copyright 2024 The OpenDCS Consortium or it's contributors.
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
package decodes.cwms.resevapcalc;

import decodes.db.Constants;
import decodes.tsdb.CTimeSeries;
import ilex.var.NoConversionException;

import java.util.Date;

/**
 * EvapMetData holds meteorological time series data, such as wind-speed, air-temperature.
 */
final public class EvapMetData
    {
    // input met data timeseries
    private CTimeSeries windspeedTsc;
    private CTimeSeries airTempTsc;
    private CTimeSeries relHumidityTsc;
    private CTimeSeries dewPointTsc;
    private CTimeSeries airPressureTsc;
    private CTimeSeries fractionLowClouds;
    private CTimeSeries altitudeLowClouds;
    private CTimeSeries fractionMedClouds;
    private CTimeSeries altitudeMedClouds;
    private CTimeSeries fractionHighClouds;
    private CTimeSeries altitudeHighClouds;

    // these variables hold last valid values
    //was originally RMA undefinedDouble = -FloatMax
    double windSpeedOld = Constants.undefinedDouble;
    double airTempOld = Constants.undefinedDouble;
    double relHumidityOld = Constants.undefinedDouble;
    double airPressureOld = Constants.undefinedDouble;

    // variables hold the met values for the current time
    double windSpeedCurrent = Constants.undefinedDouble;
    double airTempCurrent = Constants.undefinedDouble;
    double relHumidityCurrent = Constants.undefinedDouble;
    double airPressureCurrent = Constants.undefinedDouble;

    public double getWindSpeed(Date time) throws ResEvapException
        {
        return getMetValue(windspeedTsc, time);
        }

    public double getAirTemp(Date time) throws ResEvapException
        {
        return getMetValue(airTempTsc, time);
        }

    public double getRelHumidity(Date time) throws ResEvapException
        {
        return getMetValue(relHumidityTsc, time);
        }

    public double getDewPoint(Date time) throws ResEvapException
        {
        return getMetValue(dewPointTsc, time);
        }

    public double getAirPressure(Date time) throws ResEvapException
        {
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
    public CloudCover[] getCloudCover(Date time) throws ResEvapException
        {
        CloudCover[] cloudCover = new CloudCover[3];
        double fractionCC = getMetValue(fractionLowClouds, time);
        double altitude = getMetValue(altitudeLowClouds, time);

        cloudCover[2] = new CloudCover(fractionCC,
                altitude, CloudCover.CloudHeightType.HEIGHT_LOW);

        fractionCC = getMetValue(fractionMedClouds, time);
        altitude = getMetValue(altitudeMedClouds, time);

        cloudCover[1] = new CloudCover(fractionCC,
                altitude, CloudCover.CloudHeightType.HEIGHT_MED);

        fractionCC = getMetValue(fractionHighClouds, time);
        altitude = getMetValue(altitudeHighClouds, time);

        cloudCover[0] = new CloudCover(fractionCC,
                altitude, CloudCover.CloudHeightType.HEIGHT_HIGH);

        return cloudCover;
        }

    private double getMetValue(CTimeSeries tsc, Date time) throws ResEvapException
        {
        int idx = tsc.findNextIdx(time);
        double value;
        try
            {
            value = tsc.sampleAt(idx).getDoubleValue();
            } catch (NoConversionException ex)
            {
            throw new ResEvapException("failed to load met value from timeseries "+tsc.getNameString()+ " at "+ time, ex);
            }
        return value;
        }

    public void setAirTempTs(CTimeSeries tsc)
        {
        airTempTsc = tsc;
        }

    public void setAirPressureTs(CTimeSeries tsc)
        {
        airPressureTsc = tsc;
        }

    public void setRelHumidityTs(CTimeSeries tsc)
        {
        relHumidityTsc = tsc;
        }

    public void setWindSpeedTs(CTimeSeries tsc)
        {
        windspeedTsc = tsc;
        }

    public void setHighCloudTs(CTimeSeries tscFrac, CTimeSeries tscHeight)
        {
        fractionHighClouds = tscFrac;
        altitudeHighClouds = tscHeight;
        }

    public void setMedCloudTs(CTimeSeries tscFrac, CTimeSeries tscHeight)
        {
        fractionMedClouds = tscFrac;
        altitudeMedClouds = tscHeight;
        }

    public void setLowCloudTs(CTimeSeries tscFrac, CTimeSeries tscHeight)
        {
        fractionLowClouds = tscFrac;
        altitudeLowClouds = tscHeight;
        }

    }
