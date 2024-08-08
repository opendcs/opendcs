/* 
 * Copyright (c) 2018
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */
package decodes.cwms.resevapcalc;

/**
 * This class holds some location reservoir specific values (lat, lon, instrument height)
 * used for the meteorological computations.
 * 
 */
public class ReservoirLocationInfo 
{
    // package access to these variables
    public final double lat;
    public final double lon;
    public final double instrumentHeight;
    public double gmtOffset;

    public final double rt;
    public final double ru;
    public final double rq;

    public ReservoirLocationInfo(double lat, double lon, double instrumentHeight, double gmtOffset, double rt, double ru, double rq) {
        this.lat = lat;
        this.lon = lon;
        this.instrumentHeight = instrumentHeight;
        this.gmtOffset = gmtOffset;
        this.rt = rt;
        this.ru = ru;
        this.rq = rq;
    }
}
