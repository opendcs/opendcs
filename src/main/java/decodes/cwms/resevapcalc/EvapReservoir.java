/* 
 * Copyright (c) 2018
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */
package decodes.cwms.resevapcalc;

import decodes.cwms.HecConstants;
import decodes.db.UnitConverter;
import decodes.tsdb.CTimeSeries;
import decodes.util.DecodesException;
import hec.data.RatingException;
import hec.data.cwmsRating.RatingSet;
//import hec.data.Units;
//import hec.data.UnitsConversionException;
//import hec.heclib.util.HecTime;
//import hec.io.TimeSeriesContainer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

//import rma.util.RMAConst;

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
    
    // package access to these variables
    public    int   _numberLayers;    //TODO, not current set
    public    double _secchiDepth;
    public    double _attenuationConst;
    public    double _zeroElevaton = -1;
    public    double _lat;
    public    double _long;
    public WindShearMethod _windShearMethod = WindShearMethod.DONELAN;
    public double _thermalDiffusivityCoefficient = 1.2;
    
    // instrument height
    protected double _ru;
    protected double _rt;
    protected double _rq;
    
    double _gmtOffset;
    String _tzString;
    public double _elev;
    public double _instrumentHeight;

    public RatingSet _RatingAreaElev;

    public double[] _elevA;		//TODO public for jython testing
    public double[] _surfA;
    public int _nSurfArea;
    public int _nResBottom;
    public double _depth;
   // public double _wsel;	// this is a copy of _elev used for layering setup
    public double _surfArea;
    
    // reservoir layers
    public double[] _zd = new double[NLAYERS];
    public double[] _delz = new double[NLAYERS];
    public double[] _ztop = new double[NLAYERS];
    public double[] _zarea = new double[NLAYERS];
    public double[] _zelev = new double[NLAYERS];
    public double[] _zvol = new double[NLAYERS];
    
    // water temperature 
    public double[] _rhow = new double[NLAYERS];
    public double[] _cp = new double[NLAYERS];
    public double[] _wt = new double[NLAYERS];    //TODO public for jython testing
    public double[] _kz = new double[NLAYERS];
    
    public CTimeSeries _elevationTsc;
    
    //  is the index for the last layer 
    // resj = _numberLayers - 1
    int _resj;
    int _resj_old;
    
    int _resj1;
    boolean _isEnglish = true;
    boolean _isDewpoint = false;
    
    boolean _elevTS_flag = false; //TODO
       
    String _name;
    BufferedWriter _debugout = null;
    
    public EvapReservoir()
    {
        
    }
    
    public void setInputDataIsEnglish( boolean tf)
    {
        _isEnglish = tf;
    }
    
    public void setName( String name )
    {
        _name = name;
    }
    
    public String getName()
    {
        return _name;
    }
    
    public void setTimeZoneString( String tzStr )
    {
        _tzString = tzStr;
    }
    public void setLatLon( double lat, double lon )
    {
        _lat = lat;
        _long = lon;
    }
    public void setGmtOffset( double gmtOffset )
    {
        _gmtOffset = gmtOffset;
    }
    
    public double getGmtOffset()
    {
        return _gmtOffset;
    }
    
    /**
     * set output for writing debug info on layer structure
     * @param debugout
     */
    public void setDebugFile( BufferedWriter debugout )
    {
    	_debugout = debugout;
    }
    
    /**
     * Return elevation for time series if available, else 
     * return the start time elevation.
     * 
     * @param hecTime
     * @return   the reservoir elevation at hecTime in meters
     */
    public double getCurrentElevation( Date hecTime )
    {
    	if ( _elevationTsc == null )
    	{
    		return _elev;
    	}
        double elev = _elevationTsc.findNextIdx(hecTime);
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
        if ( _isEnglish )
        {
            sclfct = FT_TO_M;
        }
        
        _ru = windHeight * sclfct;
        _rt = tempHeigth * sclfct;
        _rq = relHeight * sclfct;      
        _instrumentHeight = windHeight * sclfct;
    }
    
    public void setSecchi(double secchi )
    {
        _secchiDepth = secchi;
        if ( _isEnglish )
        {
        	_secchiDepth *= FT_TO_M;
        }
        
        _attenuationConst =  1.70/_secchiDepth;
    }

    public void setWindShearMethod(WindShearMethod method)
    {
        _windShearMethod = method;
    }

    public void setThermalDiffusivityCoefficient(double thermalDiffusivityCoefficient)
    {
        _thermalDiffusivityCoefficient = thermalDiffusivityCoefficient;
    }
    
    /**
     * Return lat, lon, gmt offset and instrument height for reservoir
     * 
     * @return
     */
    public ReservoirLocationInfo getReservoirLocationInfo()
    {
    	ReservoirLocationInfo resInfo = new ReservoirLocationInfo(
    	_lat,
    	_long,
    	_instrumentHeight,
        _gmtOffset,
    	_ru,
    	_rt,
    	_rq);
    	
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
            _wt[i] = wt[i];
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
        _RatingAreaElev = AreaElevRating;

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
    	_elev = elev;
    	if ( _isEnglish )
    	{
    		_elev *= FT_TO_M;
    	}

        double surfArea = 0;
        try {
            surfArea = intArea(_elev);
        } catch (RatingException e) {
            throw new RuntimeException(e);
        }
        _surfArea = surfArea/(1000.*1000.);
        
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
    	_elev = elev;
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
    	return _elev;
    }
    
    public boolean setZeroElevation( double elev )
    {
    	_zeroElevaton = elev;
    	if ( _isEnglish && elev > -1. )
    	{
    		_zeroElevaton *= FT_TO_M;
    	}
    	return true;
    }

    /**
     * Set the reservoir elevation time series
     * @param tsc
     */
    public void setElevationTs( CTimeSeries tsc )
    {
    	_elevationTsc = tsc;
    }
    
    /**
     * process some reservoir physical data for very start of run
     * 
     * @return 
     */
    public boolean initRes()
    {

    	// Set wsel to entered elevation
        double wsel = _elev;

        if(_zeroElevaton == -1){
            return false;
        }
        double depth = wsel - _zeroElevaton;

        if ( depth <= 0. )
        {
            String msg = "Water Elev less than Reservoir Bottom Elevation";
			LOGGER.log(Level.WARNING, msg);
            return false;
        }

        // save in global variables
        _depth = depth;
        // compute surface area
        double surfArea = 0;
        try {
            surfArea = intArea(_elev);
        } catch (RatingException e) {
            throw new RuntimeException(e);
        }
        _surfArea = surfArea/(1000.*1000.);
        
        return true;
    }

    public int getResj()
    {
    	return _resj;
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
        double wsel = _elev;

        // Estimate depth. If constant, already known. If wsel is read in, calculate now
        if ( updateDepth )
        {
            _depth = wsel - _zeroElevaton;
        }
        
        // Use .5m steps for entire depth
        double zdepth = .25;
        int j = 0;
        while ( zdepth <= _depth)
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
		LOGGER.log(Level.FINE, " resj,  depth, wsel {0}  {1}  {2}", new Object[]{resj, _depth, wsel});
   
        if ( zdx[resj] < _depth )
        {
            zdx[resj] = _depth;
            delzx[resj] = (zdx[resj]-zdx[resj-1])-0.5*delzx[resj-1];
            ztop_depth[resj] = _depth-delzx[resj];
            zelevx[resj] = _zeroElevaton + delzx[resj];
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
            _zd[i]    = zdx[j];
            _delz[i]  = delzx[j];
            _ztop[i]  = _depth-ztop_depth[j];
            _zarea[i] = zareax[j];
            _zelev[i] = zelevx[j];
            _zvol[i]  = zvolx[j];
            i++;
        }     

        _resj_old = _resj;
        _resj = resj;
        
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
            return _RatingAreaElev.rate(el);
        }
        catch(RatingException ex){
            throw new RatingException("failed to compute rating", ex);
        }
    }

}
