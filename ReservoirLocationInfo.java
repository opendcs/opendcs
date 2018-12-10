/* 
 * Copyright (c) 2018
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */
package usace.rowcps.computation.resevap;

/**
 * This class holds some location reservoir specific values (lat, lon, instrument height)
 * used for the meteorological computations.
 * 
 */
public class ReservoirLocationInfo 
{
    // package access to these variables
    public double lat;
    public double lon;
    public double instrumentHeight;
    public double gmtOffset;
    
    public double rt;
    public double ru;
    public double rq;
}
