/* 
 * Copyright (c) 2018
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */
package decodes.cwms.resevapcalc;

import decodes.cwms.HecConstants;
import decodes.tsdb.CTimeSeries;
import decodes.util.DecodesException;
import hec.data.RatingException;
import hec.data.cwmsRating.RatingSet;

import java.io.BufferedWriter;
import java.sql.Connection;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class holds reservoir specific values (lat, lon, elev-area)
 * and the layers info for computing the
 * water temperature profile.
 * 
 */
public class EvapReservoir
{

	private static final Logger LOGGER = Logger.getLogger(EvapReservoir.class.getName());
    public static final int MAX_RES_AREAS = 50000;
    public static final int NLAYERS = 1000;
    public static final double FT_TO_M = 0.3048;
    public static final double ACRES_TO_M2 = 4046.8564224;

    public Connection conn;
    // package access to these variables
    public    int numberLayers;    //TODO, not current set
    public    double secchiDepth;
    public    double attenuationConst;
    public    double zeroElevaton = -1;
    public    double latitude;
    public    double longitude;
    public WindShearMethod windShearMethod = WindShearMethod.DONELAN;
    public double thermalDiffusivityCoefficient = 1.2;
    
    // instrument height
    protected double ru;
    protected double rt;
    protected double rq;
    
    double gmtOffset;
    String tzString;
    public double elev;
    public double instrumentHeight;

    public RatingSet ratingAreaElev;

    public double[] elevA;
    public double[] surfA;
    public int nSurfArea;
    public int nResBottom;
    public double depth;
   // public double _wsel;	// this is a copy of _elev used for layering setup
    public double surfArea;
    
    // reservoir layers
    public double[] zd = new double[NLAYERS];
    public double[] delz = new double[NLAYERS];
    public double[] ztop = new double[NLAYERS];
    public double[] zarea = new double[NLAYERS];
    public double[] zelev = new double[NLAYERS];
    public double[] zvol = new double[NLAYERS];
    
    // water temperature 
    public double[] rhow = new double[NLAYERS];
    public double[] cp = new double[NLAYERS];
    public double[] wt = new double[NLAYERS];
    public double[] kz = new double[NLAYERS];
    
    public CTimeSeries elevationTsc;
    
    //  is the index for the last layer 
    // resj = numberLayers - 1
    int resj;
    int resjOld;
    
    int resj1;
    boolean isEnglish = true;
    boolean isDewpoint = false;
    
    boolean elevTS_flag = false; //TODO
       
    String name;
    BufferedWriter debugout = null;
    
    public EvapReservoir()
    {
        
    }
    
    public void setInputDataIsEnglish( boolean tf)
    {
        isEnglish = tf;
    }
    
    public void setName( String name )
    {
        this.name = name;
    }
    
    public String getName()
    {
        return name;
    }
    
    public void setTimeZoneString( String tzStr )
    {
        tzString = tzStr;
    }
    public void setLatLon( double lat, double lon )
    {
        this.latitude = lat;
        longitude = lon;
    }
    public void setGmtOffset( double gmtOffset )
    {
        this.gmtOffset = gmtOffset;
    }
    
    public double getGmtOffset()
    {
        return gmtOffset;
    }
    
    /**
     * set output for writing debug info on layer structure
     * @param debugout
     */
    public void setDebugFile( BufferedWriter debugout )
    {
    	this.debugout = debugout;
    }
    
    /**
     * Return elevation for time series if available, else 
     * return the start time elevation.
     * 
     * @param hecTime
     * @return   the reservoir elevation at hecTime in meters
     */
    public double getCurrentElevation( Date hecTime ) throws DecodesException {
    	if ( elevationTsc == null )
    	{
    		return elev;
    	}
        int elevIdx = elevationTsc.findNextIdx(hecTime);
        double elev;
        try {
            elev = elevationTsc.sampleAt(elevIdx).getDoubleValue();
        }
        catch(Exception ex){
            throw new DecodesException("failed to load current elevation", ex);
        }
        if ( !HecConstants.isValidValue(elev) )
        {
        	// return undef value
        	return elev;
        }
        
        return elev;
    }
       
    public void setInstrumentHeights( double windHeight,
            double tempHeigth, double relHeight )
    {
        double sclfct = 1.;
        if (isEnglish)
        {
            sclfct = FT_TO_M;
        }
        
        ru = windHeight * sclfct;
        rt = tempHeigth * sclfct;
        rq = relHeight * sclfct;
        instrumentHeight = windHeight * sclfct;
    }
    
    public void setSecchi(double secchi )
    {
        secchiDepth = secchi;
        if (isEnglish)
        {
        	secchiDepth *= FT_TO_M;
        }
        
        attenuationConst =  1.70/ secchiDepth;
    }

    public void setWindShearMethod(WindShearMethod method)
    {
        windShearMethod = method;
    }

    public void setThermalDiffusivityCoefficient(double thermalDiffusivityCoefficient)
    {
        this.thermalDiffusivityCoefficient = thermalDiffusivityCoefficient;
    }
    
    /**
     * Return lat, lon, gmt offset and instrument height for reservoir
     * 
     * @return
     */
    public ReservoirLocationInfo getReservoirLocationInfo()
    {
    	ReservoirLocationInfo resInfo = new ReservoirLocationInfo(
                latitude,
                longitude,
                instrumentHeight,
                gmtOffset,
                ru,
                rt,
                rq);
    	
    	return resInfo;
    }
    
    /**
     * Initialize temperature profile to a single value
     *
     * @return 
     */
    public boolean setInitWaterTemperatureProfile( double[] wt, int resj )
    {
        for ( int i=0; i<=resj; i++)
        {
            this.wt[i] = wt[i];
        }
        return true;
    }
    
    /**
     * Set elevation area curve.  Values should be in SI units
     * ( m and m2 )
     * 
     * @param elev
     * @param area
     * @param nvals
     * @return
     */


    /**
     * Set elevation area curve.  Values should be in SI units
     * ( m and m2 )
     *
     * @param AreaElevRating
     * @return
     */

    public boolean setElevAreaRating ( RatingSet AreaElevRating)
    {
        ratingAreaElev = AreaElevRating;

        return true;
    }
    
    /** set the elevation to be used by the reservoir for 
     * the water temperature profile computations
     * 
     * @param elev
     * @return
     */
    public boolean setElevation( double elev )
    {
    	this.elev = elev;
        double surfArea = 0;
        try {
            surfArea = intArea(this.elev);
        } catch (RatingException e) {
            throw new RuntimeException(e);
        }
        this.surfArea = surfArea/(1000.*1000.);
        
    	return true;
    }
    
    /** 
     * User knows the elevation value is in meters
     * 
     * @param elev
     * @return
     */
    public boolean setElevationMeters( double elev )
    {
    	this.elev = elev;
    	return true;
    }
    
    /**
     * get the present elevation used by the reservoir
     * for the water temperature profile computations
     * 
     * @return
     */
    public double getElevation( )
    {
    	return elev;
    }
    
    public boolean setZeroElevation( double elev )
    {
    	zeroElevaton = elev;
    	if ( isEnglish && elev > -1. )
    	{
    		zeroElevaton *= FT_TO_M;
    	}
    	return true;
    }

    /**
     * Set the reservoir elevation time series
     * @param tsc
     */
    public void setElevationTs( CTimeSeries tsc )
    {
    	elevationTsc = tsc;
    }
    
    /**
     * process some reservoir physical data for very start of run
     * 
     * @return 
     */
    public boolean initRes()
    {

    	// Set wsel to entered elevation
        double wsel = elev;

        if(zeroElevaton == -1){
            return false;
        }
        double depth = wsel - zeroElevaton;

        if ( depth <= 0. )
        {
            String msg = "Water Elev less than Reservoir Bottom Elevation";
			LOGGER.log(Level.WARNING, msg);
            return false;
        }

        // save in global variables
        this.depth = depth;
        // compute surface area
        double surfArea = 0;
        try {
            surfArea = intArea(elev);
        } catch (RatingException e) {
            throw new RuntimeException(e);
        }
        this.surfArea = surfArea/(1000.*1000.);
        
        return true;
    }

    public int getResj()
    {
    	return resj;
    }
    
    public boolean resSetup()
    {
    	return resSetup(false );
    }
    
    public boolean resSetup( boolean updateDepth )
    {
        double[] zdx, delzx, ztop_depth;
        double[] zelevx, zareax, zvolx;
        
        // init arrays
        zdx = new double[NLAYERS];
        delzx = new double[NLAYERS];
        ztop_depth = new double[NLAYERS];
        zelevx = new double[NLAYERS];
        zareax = new double[NLAYERS];
        zvolx = new double[NLAYERS];
        double wsel = elev;

        // Estimate depth. If constant, already known. If wsel is read in, calculate now
        if ( updateDepth )
        {
            depth = wsel - zeroElevaton;
        }
        
        // Use .5m steps for entire depth
        double zdepth = .25;
        int j = 0;
        while ( zdepth <= depth)
        {
            zdx[j] = zdepth;
            delzx[j] = 0.5;
            zdepth = zdepth + 0.5;
            ztop_depth[j] = ((double)j)*0.5;
            zelevx[j] = wsel - ((double)j)*0.5;
            try {
                zareax[j] = intArea(zelevx[j]);
            } catch (RatingException e) {
                throw new RuntimeException(e);
            }
            j++;
        } 

        int resj = j-1;
        
        // Put last step at bottom
		LOGGER.log(Level.FINE, " resj,  depth, wsel {0}  {1}  {2}", new Object[]{resj, depth, wsel});
   
        if ( zdx[resj] < depth)
        {
            zdx[resj] = depth;
            delzx[resj] = (zdx[resj]-zdx[resj-1])-0.5*delzx[resj-1];
            ztop_depth[resj] = depth -delzx[resj];
            zelevx[resj] = zeroElevaton + delzx[resj];
            try {
                zareax[resj] = intArea(zelevx[resj]);
            } catch (RatingException e) {
                throw new RuntimeException(e);
            }
        }
        
        // Calculate volume of each layer
        for ( j = 0; j<resj; j++ )
        {
            zvolx[j] = 0.5*(zareax[j] + zareax[j+1])*delzx[j];
        }
        zvolx[resj] = 0.5*zareax[resj]*delzx[resj];       
        
        // Now renumber so 1 is at bottom

        int i = 0;
        for ( j=resj; j>=0; j--)
        {
            zd[i]    = zdx[j];
            delz[i]  = delzx[j];
            ztop[i]  = depth -ztop_depth[j];
            zarea[i] = zareax[j];
            zelev[i] = zelevx[j];
            zvol[i]  = zvolx[j];
            i++;
        }     

        resjOld = this.resj;
        this.resj = resj;
        
        return true;
    }


    
    /**
     * interpolate reservoir surface area for elevation el.
     * 
     * @param el
     * @return 
     */
    public double intArea(double el) throws RatingException {
        try {
            double ftElvation = el / FT_TO_M;
            double ftSquared = ratingAreaElev.rate(conn, ftElvation);
            return ftSquared*Math.pow(FT_TO_M,2);
        }
        catch(RatingException ex){
            throw new RatingException("failed to compute rating", ex);
        }
    }

}
