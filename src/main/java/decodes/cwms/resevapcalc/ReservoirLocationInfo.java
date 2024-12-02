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
 */
public class ReservoirLocationInfo {
    // package access to these variables
    public final double latitude;
    public final double longitude;
    public final double instrumentHeight;
    public double gmtOffset;

    public final double ru; //sensor windHeight
    public final double rt; //sensor tempHeigth
    public final double rq; //sensor relHeight

    public ReservoirLocationInfo(double latitude, double longitude, double instrumentHeight, double gmtOffset, double ru, double rt, double rq) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.instrumentHeight = instrumentHeight;
        this.gmtOffset = gmtOffset;
        this.rt = rt;
        this.ru = ru;
        this.rq = rq;
    }
}
