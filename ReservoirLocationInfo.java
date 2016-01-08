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
