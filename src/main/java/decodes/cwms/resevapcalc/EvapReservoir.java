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
    public    double _zeroElevaton;
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
        
        // convert units on timeseries to meters
        try
        {
            elev = new UnitConverter(_elevationTsc.getUnitsAbbr(), "m")
        	//elev = Units.convertUnits( elev, _elevationTsc.units.toString(), "m");
        }
        catch ( UnitsConversionException ue )
        {
            LOGGER.log(Level.SEVERE, "Exception occurred while transforming hourly temperature profile data to daily for .", ue);
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
     * Read reservoir info from the standard Resevap reservoir text file 
     * 
     * @param textFile
     * @return 
     */
    public boolean readTextFile( File textFile )
    {
        List<String> inputLines = new ArrayList<String>();
        
        if ( textFile == null || !textFile.exists())
        {
            return false;
        }
        int icnt = 0;
            
        // read and store the lines from the text file.
        try(BufferedReader in = new BufferedReader( new FileReader( textFile ) ))
        {
            String line;
           
            line = in.readLine();

            while ( line != null )
            {
                    icnt++;
                    inputLines.add( line );
                    line = in.readLine();
            }
        }
        catch (IOException e)
        {
			LOGGER.log(Level.SEVERE, "Exception:  Error Reading bc file ",  e);
            return false;
        }
        
        // parse the reservoir text file
        for ( int i=0; i<inputLines.size(); i++ )
        {
            String line = inputLines.get(i);
            
            if ( line.trim().length() > 1 )
            {
                String[] parts = line.split(":");
                if ( parts != null && parts.length == 2 )
                {
                    if ( parts[0].toUpperCase().contains("RESERVOIR") )
                    {
                        _name = parts[1];
                    }
                    else if ( parts[0].toUpperCase().contains("METRIC") )
                    {
                        _isEnglish = false;
                    }
                    else if ( parts[0].toUpperCase().contains("DEWPOINT") )
                    {
                        _isDewpoint = true;
                    }
                    else if ( parts[0].toUpperCase().contains("SECCHI") )
                    {
                        _secchiDepth = Double.parseDouble( parts[1]);
                        if ( _isEnglish )
                        {
                            _secchiDepth *= FT_TO_M;
                        }
                        _attenuationConst =  1.70/_secchiDepth;
                    }
                    else if ( parts[0].toUpperCase().contains("ZERO ELEV") )
                    {
                        _zeroElevaton = Double.parseDouble( parts[1]);
                    }
                    else if ( parts[0].toUpperCase().contains("LAT") )
                    {
                        _lat = Double.parseDouble( parts[1]);
                    }
                    else if ( parts[0].toUpperCase().contains("LONG") )
                    {
                        double lon = Double.parseDouble( parts[1]);
                        _long = Math.abs(lon);
                    }
                    else if ( parts[0].toUpperCase().contains("GMT") )
                    {
                        _gmtOffset = Double.parseDouble( parts[1]);
                    }
                    else if ( parts[0].toUpperCase().contains("ELEV") )
                    {
                        _elev = Double.parseDouble( parts[1]);
                    }
                    else if ( parts[0].toUpperCase()
							.contains("INSTRUMENT HEIGHT") )
                    {
                        _instrumentHeight = Double.parseDouble( parts[1]);
                        _ru = _instrumentHeight;
                        _rt = _instrumentHeight;
                        _rq = _instrumentHeight;
                    }
                    
                    else if ( parts[0].toUpperCase().contains("SURFACE AREA") )
                    {
                        double npairs = Double.parseDouble(parts[1]);
                        _nSurfArea = (int)npairs;
                        
                        if ( _nSurfArea  > MAX_RES_AREAS )
                        {
                            return false;
                        }
                        double[] resElevations = new double[_nSurfArea];
                        double[] resAreas  = new double[_nSurfArea];
                        
                        i++;
                        line = inputLines.get(i);
                        //parts = line.split("\\s+");
                        parts = line.split(",");
                        if ( parts.length != _nSurfArea )
                        {
                            return false;
                        }
                        for ( int j=0; j<parts.length; j++ )
                        {
                            resElevations[j] = Double.parseDouble(parts[j]);
                        }
                        i++;
                        line = inputLines.get(i);
                        parts = line.split(",");
                        if ( parts.length != _nSurfArea )
                        {
                            return false;
                        }
                        for ( int j=0; j<parts.length; j++ )
                        {
                            resAreas[j] = Double.parseDouble(parts[j]);
                        }
                        
                        _elevA = resElevations;
                        _surfA = resAreas;
                        
                        break;
                    }
                }
            }
        }
        
        // English to metric conversion
        if ( _isEnglish )
        {
            int npts = _elevA.length;
            for ( int i=0; i<npts; i++ )
            {
                _elevA[i] *= FT_TO_M;
                _surfA[i] *= ACRES_TO_M2;
            }
            _ru *= FT_TO_M;
            _rt *= FT_TO_M;
            _rq *= FT_TO_M;
            _instrumentHeight *= FT_TO_M;
            if ( _zeroElevaton > -1.) _zeroElevaton *= FT_TO_M;
            _elev *= FT_TO_M;
        }
        
        double surfArea = intArea(_elev);
        _surfArea = surfArea/(1000.*1000.);

        return true;        
    }
    
    /**
     * Return lat, lon, gmt offset and instrument height for reservoir
     * 
     * @return
     */
    public ReservoirLocationInfo getReservoirLocationInfo()
    {
    	ReservoirLocationInfo resInfo = new ReservoirLocationInfo();
    	resInfo.lat = _lat;
    	resInfo.lon = _long;
    	resInfo.instrumentHeight = _instrumentHeight;
    	resInfo.ru = _ru;
    	resInfo.rt = _rt;
    	resInfo.rq = _rq;
    	resInfo.gmtOffset = _gmtOffset;
    	
    	return resInfo;
    }
    
    /**
     * read initial water temperature profile from standard Resevap
     * *.strtup file.
     * 
     * @param textFile
     * @return 
     */
    public boolean readInitTemperature( File textFile )
    {
        List<String> inputLines = new ArrayList<String>();
        
        if ( textFile == null || !textFile.exists())
        {
            return false;
        }
        int icnt = 0;
            
        // read and store the lines from the text file.
        try(BufferedReader in = new BufferedReader( new FileReader( textFile ) ))
        {
            String line;
           
            line = in.readLine();

            while ( line != null )
            {
                    icnt++;
                    inputLines.add( line );
                    line = in.readLine();
            }
        }
        catch (IOException e)
        {
			LOGGER.log(Level.SEVERE, "Exception:  Error Reading intial water temperature file", e);
            return false;
        }
        // parse the text
        icnt = 0;
        double[] wt = new double[NLAYERS];
        
        for ( int i=0; i<inputLines.size(); i++ )
        {
            String line = inputLines.get(i);
            if ( line.trim().length() > 1 )
            {
                wt[icnt] = Double.parseDouble(line);
                icnt++;
            }
        }

        _wt = wt;
        return true;
    }
    
    /**
     * Initialize temperature profile to a single value
     * 
     * @param waterTemp
     * @return 
     */
    public boolean setInitWaterTemperature( double waterTemp, int resj )
    {
    	//_resj = resj;
        for ( int i=0; i<=resj; i++)
        {
            _wt[i] = waterTemp;
        }
        return true;
    }
    
    /**
     * Initialize temperature profile to a single value
     * 
     * @param waterTemp
     * @return 
     */
    public boolean setInitWaterTemperatureProfile( double[] wt, int resj )
    {
    	//_resj = resj;
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

    public boolean setElevAreaCurve ( double[] elev, double[] area, int nvals, boolean isEnglish )
    {
        _elevA = new double[MAX_RES_AREAS];
        _surfA = new double[MAX_RES_AREAS];
        java.util.Arrays.fill(_elevA, 0.0);
        java.util.Arrays.fill(_surfA, 0.0);

        double elevSclfct = 1.;
        double areaSclfct = 1.;
        if ( isEnglish )
        {
        	elevSclfct = FT_TO_M;
        	areaSclfct = ACRES_TO_M2;
        }
    	for ( int i=0; i<nvals; i++)
    	{
    		_elevA[i] = elev[i] * elevSclfct;
    		_surfA[i] = area[i] * areaSclfct;
    	}
    	_nSurfArea = nvals;
    	
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
    	
        double surfArea = intArea(_elev);
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

        // Find bottom of Reservoir - highest elevation zero area
        int nResBottom = -1;
        for ( int i = 0; i<_nSurfArea; i++ )
        {
        	LOGGER.log(Level.FINE, "i, surfA, nResBottom  " + i + ", " + nResBottom  + ", " +  _surfA[i]);
            if (_surfA[i] > 0.) 
            {
                if ( i == 0 )
                {
                    nResBottom = 0;
                }
                else
                {
                    nResBottom = i-1;
                }
	            if ( nResBottom > - 1)
	            {
	                break;
	            }
            }
        }
        double depth;
        
        if ( _zeroElevaton == -1.)
        {
             depth = wsel - _elevA[nResBottom];
        }
        else
        {
            int nResTest = locate( _zeroElevaton );
            if ( nResTest > nResBottom )
            {
                nResBottom = nResTest;
            }
            else
            {
                _zeroElevaton = _elevA[nResBottom];
            }
            depth = wsel - _zeroElevaton;
        }

        if ( depth <= 0. )
        {
            String msg = "Water Elev less than Reservoir Bottom Elevation";
			LOGGER.log(Level.WARNING, msg);
            return false;
        }

        // save in global variables
        _depth = depth;
        _nResBottom = nResBottom;
		LOGGER.log(Level.FINE, "nResBottom  {0}, {1}", new Object[]{_nResBottom, _nSurfArea});
        // compute surface area
        double surfArea = intArea(_elev);
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
            _depth = wsel - _elevA[_nResBottom];
        }
        
        // Use .5m steps for entire depth
        double zdepth = .25;
        int j = 0;
        while ( zdepth <= _depth)
        {
            zdx[j] = zdepth;
            delzx[j] = 0.5;
            zdepth = zdepth + 0.5;
//            ztop_depth[j] = ((double)j-1.)*0.5;
//            zelevx[j] = _wsel - ((double)j-1.)*0.5;
            ztop_depth[j] = ((double)j)*0.5;
            zelevx[j] = wsel - ((double)j)*0.5;
            zareax[j] = intArea(zelevx[j]);
            j++;
        } 
        
        int jcount = j;
        int resj = j-1;
        
        // Put last step at bottom
		LOGGER.log(Level.FINE, " resj,  depth, wsel {0}  {1}  {2}", new Object[]{resj, _depth, wsel});
   
        if ( zdx[resj] < _depth )
        {
            zdx[resj] = _depth;
            delzx[resj] = (zdx[resj]-zdx[resj-1])-0.5*delzx[resj-1];
            ztop_depth[resj] = _depth-delzx[resj];
            zelevx[resj] = _elevA[_nResBottom] + delzx[resj];
            zareax[resj] = intArea(zelevx[resj]);
        }
        
        // Calculate volume of each layer
        //for ( j = 0; j<resj-1; j++ )
        for ( j = 0; j<resj; j++ )
        {
            zvolx[j] = 0.5*(zareax[j] + zareax[j+1])*delzx[j];
        }
        zvolx[resj] = 0.5*zareax[resj]*delzx[resj];       
        
        // Now renumber so 1 is at bottom

        int i = 0;
        //for ( j=resj-1; j>=0; j--)
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

//        if ( _debugout != null )
//        {
//        	writeLayerDebugInfo();
//        }
        
        return true;
    }
    
    /** reservoir layer info for tracking changes with changing wsel
     * 
     */
//    public void writeLayerDebugInfo( )
//    {
//    	if ( _debugout == null )
//    	{
//    		return;
//    	}
//    	rma.util.NumberFormat nf10_3 = new rma.util.NumberFormat("%10.3f");
//    	rma.util.NumberFormat nf13_4 = new rma.util.NumberFormat("%13.4f");
//    	rma.util.NumberFormat nfi_3 = new rma.util.NumberFormat("%3d");
//
//    	try
//    	{
//	    	String depthstr = "resj+1 : " +  _resj+1 + "      wsel_ft" + nf10_3.form(_elev/.3048)
//	    			+ "      wsel" + nf10_3.form(_elev) + "     depth" +  nf10_3.form(_depth);
//	    	_debugout.write( depthstr );
//	    	_debugout.newLine();
//
//	    	String heading = " j               zd            delz            ztop           zarea           zelev            zvol";
//	    	StringBuffer strbuf = new StringBuffer();
//
//	    	_debugout.write(heading );
//	    	_debugout.newLine();
//	    	for ( int j=0; j<=_resj; j++)
//	    	{
//	    		strbuf.setLength(0);
//	    		strbuf.append( nfi_3.form(j+1) );
//	    		strbuf.append( nf13_4.form( _zd[j]) + "  " );
//	    		strbuf.append( nf13_4.form( _delz[j]) + "  " );
//	    		strbuf.append( nf13_4.form( _ztop[j]) + "  " );
//	    		strbuf.append( nf13_4.form( _zarea[j]) + "  " );
//	    		strbuf.append( nf13_4.form( _zelev[j]) + "  " );
//	    		strbuf.append( nf13_4.form( _zvol[j]) + "  " );
//	    		_debugout.write( strbuf.toString() );
//		    	_debugout.newLine();
//	    	}
//    	}
//    	catch ( IOException ioe )
//    	{
//            LOGGER.log(Level.FINE, "Exception occurred while writing layer debug info.", ioe);
//    	}
//    }
    
    public int locate(double x)
    {
        int n = _nSurfArea-1;
        int jl =-1 ;
        int ju = n+1;
        int jm,j;

        while ( ju-jl > 1)
        {
            jm = (ju+jl)/2;
            if( (_elevA[n] >= _elevA[0]) && (x >= _elevA[jm]) )
            {
                jl = jm;
            }
            else
            {
                ju = jm;
            }
        }

        if ( x == _elevA[0] )
        {
            j = 0;
        }
        else if ( x == _elevA[n] )
        {
            j = n-1;
        }
        else
        {
            j = jl;
        }

        return j;
    }

    
    /**
     * interpolate reservoir surface area for elevation el.
     * 
     * @param el
     * @return 
     */
    public double intArea(double el)
    {
        int j = locate(el);
        double w = _surfA[j] + (el-_elevA[j])/(_elevA[j+1]-_elevA[j])*
                    (_surfA[j+1]-_surfA[j]);

        return w;
    }
}
