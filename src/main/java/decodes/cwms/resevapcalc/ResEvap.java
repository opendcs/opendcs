/* 
 * Copyright (c) 2018
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */
package decodes.cwms.resevapcalc;

import hec.data.ParameterType;
import hec.data.Units;
import hec.data.UnitsConversionException;
import hec.heclib.dss.DssDataType;
import hec.heclib.util.HecTime;
import hec.heclib.util.Heclib;
import hec.hecmath.HecMathException;
import hec.hecmath.TimeSeriesMath;
import hec.io.TimeSeriesContainer;
import hec.lang.DSSPathString;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import rma.lang.RmaMath;
import rma.util.RMAConst;

/**
 * Program to estimate evaporation from reservoirs
 * Program estimates 1-D water temperature profile in reservoir  
 * Calculates evaporation
 * 
 * @author RESEVAP program by Steven F. Daly (ERDC/CRREL)
 * Version 1.1  31 August 2009 
 * VERSION 2.0 7 July 2010
 * conversion to Java by Richard Rachiele (RMA)
 */
public class ResEvap
{
	private static final Logger LOGGER = Logger.getLogger(ResEvap.class.getName());

    // Some global constant parameter vaiues set here
    public static final double EMITTANCE_H20 = 0.98;
    public static final double PENFRAC = 0.4;
    public static final double ALBEDO = 0.08;
    public static final double THETA = 1.0;
    public static final double SIGMA = 5.67e-8;
    public static final double ETA_CONVECTIVE = .50;
    public static final double ETA_STIRRING = .40;
    public static final double WIND_CRITIC = 1.0;
    
    // some print formats
    rma.util.NumberFormat nf8_3 = new rma.util.NumberFormat("%8.3f");
    rma.util.NumberFormat nf8_2 = new rma.util.NumberFormat("%8.2f");
    rma.util.NumberFormat nf9_3 = new rma.util.NumberFormat("%9.3f");
    rma.util.NumberFormat nf10_3 = new rma.util.NumberFormat("%10.3f");
    rma.util.NumberFormat nfe11_3 = new rma.util.NumberFormat("%11.3E");    
        
    // output computed time series
    TimeSeriesContainer _solarRadTsc;
    TimeSeriesContainer _IR_DownTsc;
    TimeSeriesContainer _IR_OutTsc;
    TimeSeriesContainer _sensibleHeatTsc;
    TimeSeriesContainer _latentHeatTsc;
    TimeSeriesContainer _evapRateHourlyTsc;
    TimeSeriesContainer _evapDailyTsc;
    TimeSeriesContainer _surfaceTempTsc;
    NavigableMap<Integer, Integer> _timeMap;
    
    TimeSeriesContainer[] _inputTimeSeries;
    
    // FPart of output Ts
    String _versionName;
            
    // store Water temperature profile data
    // one profile for each hour
    double[][] _wtempProfiles;
            
    public EvapReservoir _reservoir;
    public EvapMetData _metData;
    
    private File _workDir;
    
    public ResEvap()
    {
    }
    
    public ResEvap( EvapReservoir reservoir, EvapMetData metData )
    {
    	setReservoir(reservoir);
    	setMetData(metData);
    }
    
    /**
     * This is the directory location output of results to textfiles
     * 
     * @param workDir
     * @return
     */
    public boolean setOutputDirectory( File workDir )
    {
        _workDir = workDir;
        
        return true;
    }
    
    /** set the reservoir data for the compute and initialize
     * the reservoir layers 
     * 
     * @param reservoir
     * @return
     */
    public boolean setReservoir( EvapReservoir reservoir )
    {
        _reservoir = reservoir;
        
        // check data

        // Initialize reservoir layers
        if ( !reservoir.initRes() )
        {
            return false;
        }      
        
        if ( !reservoir.resSetup() )
        {
            return false;
        }         
        return true;
                
    }

    /** set the meteorological data for the compute 
     * 
     * @param metData
     * @return
     */
    public boolean setMetData( EvapMetData metData )
    {
        _metData = metData;
        return true;
    }
    
    /**
     * compute reservoir evaporation for the time window defined by the
     * startDateTime and endDateTime.  This method loops over the hours
     * of the simulation to call computes for the meteorological factors
     *  (surface heat balance) and the heat balance and temperature structure
     *  of the reservoir. 
     * 
     * The reservoir and met data object need to have previously been set.
     * 
     * @param startDateTime
     * @param endDateTime
     * @param gmtOffset
     * @return 
     */
    public boolean compute(String startDateTime, String endDateTime,
            double gmtOffset, String versionName ) throws ResEvapException
    {
    	if ( _reservoir == null )
    	{
    		throw new ResEvapException ("ResEvap.compute: No reservoir has been set");
    	}
    	
    	if ( _metData == null )
    	{
    		throw new ResEvapException ("ResEvap.compute: No Meteorological data has been set");
    	}
    	
        // diagnostic output
        File outfil = null;
        File metoutfil = null;
        File toutfil = null;
        File xoutfil = null;
        
        if ( _workDir != null )
        {
            outfil = new File(_workDir.getAbsolutePath() + "/" + "wtout_java.dat");
            metoutfil = new File(_workDir.getAbsolutePath() + "/" + "testtout_java.dat");
            toutfil = new File(_workDir.getAbsolutePath() + "/" + "xout2_java.dat");
            xoutfil = new File(_workDir.getAbsolutePath() + "/" + "xout_java.dat");
        }

        // setup output time series objects
        _versionName = versionName;
        initializeOutputTsc( _reservoir._name, versionName, startDateTime, endDateTime);
        
        // determine number of periods
        HecTime hecStartTime = new HecTime( startDateTime, HecTime.MINUTE_INCREMENT);
        HecTime hecEndTime = new HecTime( endDateTime, HecTime.MINUTE_INCREMENT);
        int intervalMinutes = 60;
        int nper = HecTime.nopers( intervalMinutes, hecStartTime.julian(),
                hecStartTime.minutesSinceMidnight(),
                hecEndTime.julian(), hecEndTime.minutesSinceMidnight() );

        double deltT = 3600.;
        
        // MetComputation handles the surface water heat exchange and evap 
        MetComputation metComputation = new MetComputation();
        metComputation.setMetData(_metData);
        
        // ResWtCompute computes the water temperature profile for the reservoir.
        ResWtCompute resWtCompute = new ResWtCompute( _reservoir );

        
        // open files for text output  ...

        // file for reservoir energy balance
        BufferedWriter tout = null;
        if ( toutfil != null )
        {
            try
            {
                tout = new BufferedWriter( new FileWriter( toutfil ) );
                resWtCompute.setOutfile( tout );
            }
            catch ( IOException ex )
            {
				LOGGER.log(Level.FINE, "Unable to read " + toutfil.getAbsolutePath(), ex);
            }
        }

        // debug reservoir layers
        BufferedWriter xout = null;
        if ( xoutfil != null )
        {
            try
            {
            	xout = new BufferedWriter( new FileWriter( xoutfil ) );
            	_reservoir.setDebugFile(xout);
            }
            catch (IOException ex)
            {
				LOGGER.log(Level.FINE, "Unable to read " + xoutfil.getAbsolutePath(), ex);
            }
        }

        // file for reservoir temperature profile
        BufferedWriter out = null;
        if ( outfil != null )
        {
            try
            {
                out = new BufferedWriter( new FileWriter( outfil ) );
            }
            catch ( IOException ex )
            {
				LOGGER.log(Level.FINE, "Unable to read " + outfil.getAbsolutePath(), ex);
            }
        }
        
        // met and surface heat exchange
        BufferedWriter metout = null;
        if ( outfil != null )
        {
            try
            {
                metout = new BufferedWriter( new FileWriter( metoutfil ) );
            
                if ( metout != null )
                {
                    String heading =
                    "    Date    JD  GMT     U       T      RH       P       Ts      K       u*        R*       L          Hs        HL        Qs        IR       IR_out     Evap";
                    metout.write(heading); metout.newLine();

                    heading =
                    "                       m/s    deg C     %      mb      deg C           m/s                m      ********** W/m**2 ***********************     mm/d";
                    metout.write(heading); metout.newLine();
                }
            }
            catch (IOException ex)
            {
				LOGGER.log(Level.FINE, "Unable to read " + metoutfil.getAbsolutePath(), ex);
            }
        }
        
        // Do compute
		try
		{        
	        // init stuff
	        
	        // call resSetup to printout reservoir layer info
	        if (  xout != null )
	        {
	        	_reservoir.resSetup();   
	        }
	        
	        int resj = _reservoir.getResj();
	        _reservoir._resj_old = resj;
	        
	        // loop through compute period
	        HecTime currentTime = new HecTime(hecStartTime);
	        
	        // reservoir location info (lat, lon, gmtOffset) for met compute
	        ReservoirLocationInfo resLocationInfo = _reservoir.getReservoirLocationInfo();
	        resLocationInfo.gmtOffset = gmtOffset;
	        
	        // use elevation time series for reservoir temperature profile
	        // calculation, else use starting elevation
	        boolean useElevTS = false;  //TODO make an option
	        double wselCurrent = _reservoir.getElevation();
	        double wselOld = wselCurrent;
	        
	        for ( int jhour=0; jhour<=nper; jhour++)
	        {
	        	if ( useElevTS )
	        	{
	        		double newElev =_reservoir.getCurrentElevation(currentTime);
	        	
		        	if ( RMAConst.isValidValue(newElev) )
		        	{
		        		wselCurrent =  newElev;
		        		wselOld = wselCurrent;
		        		
		        		if ( xout != null )
		        		{
							xout.write (currentTime.date(4) + " " + currentTime.getTime(false) +   "  wselCurrent  " + (float)wselCurrent);
		        		}
		        		_reservoir.setElevationMeters(wselCurrent);
		        		_reservoir.resSetup( true );
		        	}        	
	        	}
	        	
	            if ( jhour > 0 )
	            {
	                currentTime.addMinutes(intervalMinutes);
	            }
	            double surfaceTemp = _reservoir._wt[resj];
	            
	            // compute solar and longwave down radiation
	            // and evaporation for reservoir location
	            metComputation.computeMetAndEvap(currentTime, surfaceTemp,
	            		resLocationInfo );
	            
	            boolean noProblem = true;
	            
	            // if no valid met data yet skip reservoir temp compute
	            if ( !metComputation._metFailed )
	            {
	            	// compute temperature profile in reservoir
	                noProblem = resWtCompute.computeReservoirTemp( 
	                        currentTime, metComputation, deltT);               
	            }
	            
	
	            // write out diagnostic text files if opened 
	            String strval, s;
	            double val;
	            if ( noProblem )
	            {
	            	if ( out != null )
	            	{
		            	// write computed water temperature profile 
		                resj = _reservoir.getResj();
		                String dateTimeStr = currentTime.date(4) + "            "
		                            + currentTime.getTime(false);
		                try
		                {
		                    out.write(dateTimeStr);
		                    for ( int i=resj; i>=0; i--)
		                    {
		                        strval = nf9_3.form(_reservoir._wt[i]);
		                        out.write(strval);
		                    }
			                out.newLine();
		                }
		                catch ( IOException ex )
		                {
							LOGGER.log(Level.FINE, "Unable to read " + outfil.getAbsolutePath(), ex);
		                }

		                // write met and computed evap + surface heat exchange
		                outputMetComputation(  currentTime,  _metData,  metComputation,
								 surfaceTemp,  metout );               
		            }
	            	
	                // put results into time series arrays
	                int idx = _timeMap.get( currentTime.value() );
	                if ( idx >= 0 && idx < _solarRadTsc.times.length )
	                {
	                    _solarRadTsc.values[idx] =  metComputation._solar;
	                    _IR_DownTsc.values[idx] =  metComputation._flxir;
	                    _IR_OutTsc.values[idx] =  metComputation._flxir_out;
	                    _latentHeatTsc.values[idx] =  metComputation._evapWater._hl;
	                    _sensibleHeatTsc.values[idx] =  metComputation._evapWater._hs;
	                    _surfaceTempTsc.values[idx] =  surfaceTemp;  // BOP surface temp
	                    
	                    // evap is in mm/day.  Divide by 24 to get instantaneous
	                    // hourly value
	                    _evapRateHourlyTsc.values[idx] 
	                    		=  metComputation._evapWater._evap / 24.;
	                    
	                    // store wt profile
	                    int numLayers = _reservoir.getResj() + 1;
	                    //_wtempProfiles[idx] = new double[numLayers];
	                    _wtempProfiles[idx] = new double[EvapReservoir.NLAYERS];
	                    for ( int ilyr = 0; ilyr<numLayers; ilyr++ )
	                    {
	                        _wtempProfiles[idx][ilyr] = _reservoir._wt[ilyr];
	                    }
	                }
	            }
	        }
	        
	        try
	        {
	        	if ( out != null )  out.close();
				if ( metout != null )  metout.close();
	        	if ( tout != null )  tout.close();
	        	if ( xout != null )  xout.close();
	        }
	        catch ( IOException ioe )
	        {
				LOGGER.log(Level.SEVERE, "IOException occurred while closing files", ioe);
	        }
		}
		catch (IOException | RuntimeException ex)
		{
		    try
		    {
		    	if ( out != null )  out.close();
				if ( metout != null )  metout.close();
		    	if ( tout != null )  tout.close();
		    	if ( xout != null )  xout.close();
		    }
		    catch ( IOException ioe )
		    {
				LOGGER.log(Level.SEVERE, "IOException occurred while closing files", ioe);
		    }
			LOGGER.log(Level.SEVERE, "Error within computation", ex);
	    	throw new ResEvapException (ex);
		}

	    return true;
    }
    
    /**
     * write met data and surface heat exchange
     * 
     * @param currentTime
     * @param metComputation
     * @param surfaceTemp
     * @param metout
     */
    private void outputMetComputation( HecTime currentTime, EvapMetData metData,  
    		MetComputation metComputation, double surfaceTemp, BufferedWriter metout )
    {                
        try
        {
            String strval;
            double val;
            
            String dateTimeStr = currentTime.date(4) + " " + currentTime.dayOfYear()
                    + " " + currentTime.getTime(false);
        
            metout.write(dateTimeStr);
            
            // these are met input data ...
            
            val = metData._windSpeed_current;
            if ( !RMAConst.isValidValue(val))  val = -901.;
            strval = nf8_2.form(val);
            metout.write(strval);
            
            val = metData._airTemp_current;
            if ( !RMAConst.isValidValue(val)) val = -901.;
            strval = nf8_2.form(val);
            metout.write(strval);
            
            val = metData._relHumidity_current;
            if ( !RMAConst.isValidValue(val)) val = -901.;
            strval = nf8_2.form(val);
            metout.write(strval);
            
            val = metData._airPressure_current;
            if ( !RMAConst.isValidValue(val)) val = -901.;
            strval = nf8_2.form(val);
            metout.write(strval);
            
            val = surfaceTemp;
            if ( !RMAConst.isValidValue(val)) val = -901.;
            strval = nf9_3.form(val);
            metout.write(strval);
            
//                    strval = mkFormatter.format(-901.);  // float(bo_kode)
//                    s = strval.replaceAll("\\G0", " ");
            metout.write("  -901.");
            
            // these are computed values ...
            
            val = metComputation._evapWater._ustar;
            if ( !RMAConst.isValidValue(val)) val = -901.;
            strval = nf9_3.form(val);
            metout.write(strval);
            
            val = metComputation._evapWater._rstar;
            if ( !RMAConst.isValidValue(val)) val = -901.;
            strval = nf9_3.form(val);
            metout.write(strval);
             
            val = metComputation._evapWater._obukhovLen;
            if ( !RMAConst.isValidValue(val)) val = -901.;
            if ( val == 0.0 ) val = -901.;
            float fval = (float)val;
            strval = nfe11_3.form(fval);
            metout.write(strval);
             
            val = metComputation._evapWater._hs;
            if ( !RMAConst.isValidValue(val)) val = -901.;
            strval = nf10_3.form(-val);  
            metout.write(strval);
            
            val = metComputation._evapWater._hl;
            if ( !RMAConst.isValidValue(val)) val = -901.;
            strval = nf10_3.form(-val);
            metout.write(strval);      
            
            val = metComputation._solar;
            if ( !RMAConst.isValidValue(val)) val = -901.;
            strval = nf10_3.form(val);
            metout.write(strval);
            
            val = metComputation._flxir;
            if ( !RMAConst.isValidValue(val)) val = -901.;
            strval = nf10_3.form(val);
            metout.write(strval);
            
            val = metComputation._flxir_out;
            if ( !RMAConst.isValidValue(val)) val = -901.;
            strval = nf10_3.form(val);
            metout.write(strval);
            
            val = metComputation._evapWater._evap;
            if ( !RMAConst.isValidValue(val)) val = -901.;
            strval = nf9_3.form(val);
            metout.write(strval);
            
            metout.newLine();
        }
        catch ( IOException ioe )
        {
			LOGGER.log(Level.FINE, "Unable to write output to ", ioe);
        }
    
    }
    
    
    /**
     * Set up TimeSeriesContaier s for computed values
     * 
     * @param locationName
     * @param versionName
     * @param startDateTime
     * @param endDateTime
     * @return
     */
    protected boolean initializeOutputTsc( String locationName, String versionName,
            String startDateTime, String endDateTime)
    {
        try
        {
            // generate a template for hourly time series data
            int intervalMinutes = 60;
            int offsetMinutes = 0;
            TimeSeriesMath tsMath = (TimeSeriesMath)TimeSeriesMath.generateRegularIntervalTimeSeries(startDateTime, endDateTime, 
                        intervalMinutes, offsetMinutes, Heclib.UNDEFINED_DOUBLE);
            
            TimeSeriesContainer hourlyTsc = tsMath.getContainer();
            hourlyTsc.location = locationName;
            hourlyTsc.type = ParameterType.INST;
            hourlyTsc.version = versionName;
            DSSPathString dsspath = new DSSPathString("", locationName, "", "", "1HOUR", versionName);
            
            _solarRadTsc = (TimeSeriesContainer)hourlyTsc.clone();
            _solarRadTsc.parameter = "Irrad-Flux-Solar";
            _solarRadTsc.units = "W/m2";
            dsspath.setCPart(_solarRadTsc.parameter);
            _solarRadTsc.fullName = dsspath.toString();
            
            _IR_DownTsc = (TimeSeriesContainer)hourlyTsc.clone();
            _IR_DownTsc.parameter = "Irrad-Flux-IR";
            _IR_DownTsc.units = "W/m2";
            dsspath.setCPart(_IR_DownTsc.parameter);
            _IR_DownTsc.fullName = dsspath.toString();
            
            _IR_OutTsc = (TimeSeriesContainer)hourlyTsc.clone();
            _IR_OutTsc.parameter = "Irrad-Flux-Out";
            _IR_OutTsc.units = "W/m2";
            dsspath.setCPart(_IR_OutTsc.parameter);
            _IR_OutTsc.fullName = dsspath.toString();
            
            _surfaceTempTsc = (TimeSeriesContainer)hourlyTsc.clone();
            _surfaceTempTsc.parameter = "Temp-Water-Surface";
            _surfaceTempTsc.units = "C";
            dsspath.setCPart(_surfaceTempTsc.parameter);
            _surfaceTempTsc.fullName = dsspath.toString();
            
            _sensibleHeatTsc = (TimeSeriesContainer)hourlyTsc.clone();
            _sensibleHeatTsc.parameter = "Irrad-Heat-Sensible";
            _sensibleHeatTsc.units = "W/m2";
            dsspath.setCPart(_sensibleHeatTsc.parameter);
            _sensibleHeatTsc.fullName = dsspath.toString();
            
            _latentHeatTsc = (TimeSeriesContainer)hourlyTsc.clone();
            _latentHeatTsc.parameter = "Irrad-Heat-Latent";
            _latentHeatTsc.units = "W/m2";
            dsspath.setCPart(_latentHeatTsc.parameter);
            _latentHeatTsc.fullName = dsspath.toString();
            
            _evapRateHourlyTsc = (TimeSeriesContainer)hourlyTsc.clone();
            _evapRateHourlyTsc.parameter = "EvapRate";
            _evapRateHourlyTsc.units = "mm/hr";
            dsspath.setCPart(_evapRateHourlyTsc.parameter);
            _evapRateHourlyTsc.fullName = dsspath.toString();
                           
            // put indices to tsc.times[] into a treemap by date
            _timeMap = new TreeMap<Integer, Integer>();
            HecTime hectim = new HecTime(hourlyTsc.times[0], HecTime.MINUTE_INCREMENT );
			for (int i = 0; i < hourlyTsc.times.length; i++)
			{
			            hectim.set(hourlyTsc.times[i]);
			            _timeMap.put( hourlyTsc.times[i], i);
			}                
            
            int nprofiles = hourlyTsc.times.length;
            _wtempProfiles = new double[nprofiles][];
        }
        catch ( HecMathException hme )
        {
			LOGGER.log(Level.SEVERE, "IOException occurred while closing files", hme);
            return false;
        }
        
        return true;
    }

    /**
     * Get a List containing ResEvap computed TimeSeriesContainers 
     * @return
     */
    public List<TimeSeriesContainer> getComputedMetTimeSeries()
    {
    	List<TimeSeriesContainer> computedTsList = new ArrayList<TimeSeriesContainer>();
    	computedTsList.add( (TimeSeriesContainer)_surfaceTempTsc.clone() );
    	computedTsList.add( (TimeSeriesContainer)_sensibleHeatTsc.clone() );
    	computedTsList.add( (TimeSeriesContainer)_latentHeatTsc.clone() );
    	computedTsList.add( (TimeSeriesContainer)_solarRadTsc.clone() );
    	computedTsList.add( (TimeSeriesContainer)_IR_DownTsc.clone() );
    	computedTsList.add( (TimeSeriesContainer)_IR_OutTsc.clone() );
    	computedTsList.add((TimeSeriesContainer)_evapRateHourlyTsc.clone() );
    	
    	return computedTsList;
    }
    
    /**
     * Get the computed hourly evaporation for the reservoir
     * @return
     */
    public TimeSeriesContainer getHourlyEvapRateTimeSeries()
    {
    	return _evapRateHourlyTsc;
    }
	
	public TimeSeriesContainer getHourlyEvapTimeSeries()
	{
		TimeSeriesMath math = null;
		try
		{
			TimeSeriesMath tsMath = new TimeSeriesMath( _evapRateHourlyTsc );
			math = (TimeSeriesMath) tsMath.transformTimeSeries("1HOUR", "", "AVE", false);
		}
		catch (HecMathException ex)
		{
			LOGGER.log(Level.SEVERE, "HecMathException occurred while converting hourly evap rate to hourly total evap", ex);
		}
		TimeSeriesContainer output = null;
		if (math != null)
		{
			output = math.getContainer();
			output.units = "mm";
			output.parameter = "Evap";
			output.type = DssDataType.PER_CUM.toString();
		}
		return output;
	}
    
    /**
     * Get the computed daily evaporation for the reservoir
     *  
     * @return
     */
    public TimeSeriesContainer getDailyEvapTimeSeries()
    {
		TimeSeriesContainer evapTs = getHourlyEvapTimeSeries();
    	TimeSeriesMath tsAcc = null;
    	try 
    	{
    		TimeSeriesMath tsMath = new TimeSeriesMath( evapTs );
    		tsAcc = (TimeSeriesMath)tsMath.transformTimeSeries("1DAY", "", "ACC", false);
    	}

        catch ( HecMathException hme )
        {
			LOGGER.log(Level.SEVERE, "HecMathException occurred while converting hourly total evap to daily total evap", hme);
        }
  	
    	if ( tsAcc != null )
    	{
    		return tsAcc.getContainer();
    	}
    	return null;
    }
    
    /**
     * Get the computed daily evaporation flow for the reaservoir
     * 
     * @return
     */
    public TimeSeriesContainer getDailyEvapFlowTimeSeries()
    {
    	// get daily evap
    	TimeSeriesContainer dailyEvapTs = getDailyEvapTimeSeries();
    	
    	TimeSeriesContainer dailyEvapFlowTs = (TimeSeriesContainer)dailyEvapTs.clone();
    	
    	// scale evap to meters
    	double evap_to_meters = 1.;
        try
        {
        	evap_to_meters = Units.convertUnits( 1., dailyEvapTs.units.toString(), "m");
        }
        catch ( UnitsConversionException ue )
        {
			LOGGER.log(Level.SEVERE, "Unable to convert " + dailyEvapTs.units + " to m", ue);
        	return null;
        }
    	
    	// multiply daily evap by surface area
    	int itime;
    	double undef = Heclib.UNDEFINED_DOUBLE;
    	double lastValidElev = undef;
    	double currentElev = undef;
    	double dailyEvap, dailyEvapFlow, areaMetersSq;
    	HecTime hecTime = new HecTime( HecTime.MINUTE_INCREMENT );
    	for ( int i=0; i<dailyEvapFlowTs.numberValues; i++)
    	{
    		itime = dailyEvapFlowTs.times[i];
    		hecTime.set(itime);
    		dailyEvap = dailyEvapTs.values[i];
            if ( RMAConst.isValidValue(dailyEvap) )
            {
            	// elevation returned in meters
            	double elev = _reservoir.getCurrentElevation(hecTime);
            	if ( RMAConst.isValidValue(elev) )
            	{
            		lastValidElev = elev;
            		currentElev = elev;
            	}
            	else if ( RMAConst.isValidValue(lastValidElev) )
            	{
            		currentElev = lastValidElev;
            	}
            	else
            	{
            		dailyEvapFlowTs.values[i] = undef;
            		continue;
            	}
            	
            	areaMetersSq = _reservoir.intArea(currentElev);
                dailyEvapFlow = areaMetersSq * dailyEvap * evap_to_meters;
                dailyEvapFlowTs.values[i] = dailyEvapFlow/(86400.);
            }
            else
            {
            	dailyEvapFlowTs.values[i] = undef;
            }

    	}
    	
    	dailyEvapFlowTs.units = "cms";
    	dailyEvapFlowTs.parameter = "FLOW-EVAP";
    	DSSPathString dsspath = new DSSPathString( dailyEvapFlowTs.fullName );
    	dsspath.setCPart(dailyEvapFlowTs.parameter);
    	dailyEvapFlowTs.fullName = dsspath.getPathname();
    	dailyEvapFlowTs.type = ParameterType.AVE;
    	
    	return dailyEvapFlowTs;
    }
    
    /** Fill array of  TimeSeriesContainer with results
     * of reservoir temperature computations
     * 
     * @param surfaceDepth
     * @param bottomDepth
     * @param intervalDepth
     * @return
     */
    public TimeSeriesContainer[] getTemperatureProfileTs(double surfaceDepth, 
    		double bottomDepth, double intervalDepth)
    {
    	if ( intervalDepth < .01 )
    	{
    		return null;
    	}
    	
    	// check for no data
    	if ( _wtempProfiles == null || _wtempProfiles.length < 1 )
    	{
    		return null;
    	}
    	
    	if ( bottomDepth < .0 )
    	{
    		bottomDepth = -bottomDepth;
    	}
    	double rprofs = (bottomDepth - surfaceDepth)/intervalDepth;
    	int nprofs = (int)( rprofs + .01) + 1; 
    	
    	// clone the surfaceTemperature time series
    	TimeSeriesContainer wtTemplate = (TimeSeriesContainer)_surfaceTempTsc.clone();
    	DSSPathString dsspath = new DSSPathString( _surfaceTempTsc.fullName );
    	dsspath.setCPart("Temp-Water");
    	wtTemplate.parameter = "Temp-Water";
    	wtTemplate.fullName = dsspath.getPathname();
    	
    	TimeSeriesContainer[] wtprofTs = new TimeSeriesContainer[nprofs];
    	double dep = surfaceDepth;
    	int iint,ifrac;
    	double frac;
    	String fstr, paramstr;
    	for ( int i=0; i<nprofs; i++)
    	{
    		wtprofTs[i] = (TimeSeriesContainer)wtTemplate.clone();
    		iint = (int)dep;
    		frac = dep % 1.;
    		ifrac = (int)Math.round(frac*10.);
    		fstr = Integer.toString(iint) + "," + Integer.toString(ifrac);
    		paramstr = wtTemplate.parameter + "-" + fstr + "m";
    		wtprofTs[i].parameter = paramstr;
    		dsspath.setCPart(paramstr);
    		wtprofTs[i].fullName = dsspath.getPathname();
    		
    		dep += intervalDepth;
    	}
    	
    	// initial values with missing data
    	double undef = Heclib.UNDEFINED_DOUBLE;
    	for ( int i=0; i<nprofs; i++)
    	{
    		Arrays.fill( wtprofTs[i].values, undef );
    	}
    	
    	// transfer computed values.  Surface is at resj
		int nvals = _wtempProfiles.length;
		int layers = _wtempProfiles[0].length;
		double tval;
		
    	int resj = _reservoir._resj;
    	for ( int i=resj; i>=0; i--)
    	{
    		int ilayer = resj-i;
    		
    		for ( int itime=0; itime<nvals; itime++)
    		{
    			tval = _wtempProfiles[itime][i];	
    			wtprofTs[ilayer].values[itime] = tval;
    		}
    	}
    	
    	return wtprofTs;
    }

    /**
     * Create daily interval TimeSeriesContainers of water temperature
     * for reservoir layers
     * 
     * @param surfaceDepth
     * @param bottomDepth
     * @param intervalDepth
     * @return
     */
    public TimeSeriesContainer[] getDailyTemperatureProfileTs(double surfaceDepth, 
    		double bottomDepth, double intervalDepth)
    {
    	LOGGER.log(Level.SEVERE, "getDailyTemperatureProfileTs");
    	TimeSeriesContainer[] hourlyTsArray = getTemperatureProfileTs(
    			surfaceDepth, bottomDepth, intervalDepth );
    	
    	int nlayers = hourlyTsArray.length;
    	TimeSeriesContainer[] dayTsArray = new TimeSeriesContainer[nlayers];

		int icnt = 0;
		for ( TimeSeriesContainer hourlyTsc : hourlyTsArray )
		{
			if (Arrays.stream(hourlyTsc.values).noneMatch(RMAConst::isValidValue))
			{
				LOGGER.log(Level.FINE, () -> "No data found for " + hourlyTsc.parameter);
				icnt++;
				continue;
			}

			try 
			{
				TimeSeriesMath tsMath = new TimeSeriesMath( hourlyTsc );
	    		// need to set to dss type INST-VAL for transformTimeSeries to work
	    		tsMath.getContainer().type = "INST-VAL";
    			TimeSeriesMath tsMath24 =
    					(TimeSeriesMath)tsMath.transformTimeSeries("1DAY", "", "INT", false);
    			
    			TimeSeriesContainer dailyTsc = tsMath24.getContainer();

    			verifyHourlyAndDailyMatch(hourlyTsc, dailyTsc);

				dayTsArray[icnt] = dailyTsc;
	    	}
	        catch ( HecMathException ex )
	        {
				LOGGER.log(Level.SEVERE, ex, () -> "Exception occurred while transforming hourly temperature profile data to daily for " + hourlyTsc.parameter + ".");
	        	dayTsArray[icnt] = null;
	        }

			icnt++;
		}
    		
    	return dayTsArray;
    }

    private void verifyHourlyAndDailyMatch(TimeSeriesContainer hourlyTsc, TimeSeriesContainer dailyTsc)
	{
		if (LOGGER.isLoggable(Level.FINE))
		{
			LOGGER.log(Level.SEVERE, () -> "Checking Daily and Hourly timeseries " + dailyTsc.fullName + " for discrepancies in values and times");

			//Only verify when the log level is fine
			List<Integer> hourlyTimes = Arrays.stream(hourlyTsc.times).boxed().collect(Collectors.toList());
			int dailyIndex = 0;

			for (int dailyTime : dailyTsc.times)
			{
				HecTime dailyHecTime = dailyTsc.getTimes().elementAt(dailyIndex);
				int hourlyIndex = hourlyTimes.indexOf(dailyTime);
				if (hourlyIndex == -1)
				{
					//Can't find an index for this time in the hourly data.
					LOGGER.log(Level.SEVERE, () -> "Hourly data doesn't contain a time for " + dailyHecTime.dateAndTime());
					continue;
				}
				double hourlyValue = hourlyTsc.values[hourlyIndex];
				double dailyValue = dailyTsc.values[dailyIndex];

				if (!RmaMath.equals(hourlyValue, dailyValue, 0.00001f))
				{
					LOGGER.log(Level.SEVERE, () -> "Hourly and Daily value don't match at " + dailyHecTime.dateAndTime()
							+ System.lineSeparator() + "\tExpected: " + hourlyValue
							+ System.lineSeparator() + "\tReceived: " + dailyValue);
				}

				dailyIndex++;
			}
		}
	}

    /** make some data for starting test run 
     * 
     * @param surfaceDepth
     * @param bottomDepth
     * @param intervalDepth
     * @return
     */
    public static TimeSeriesContainer[] generateDailyTemperatureProfileTs(
    		String locationName, String versionName,
    		String startDate, String endDate, double tval,
    		double surfaceDepth, double bottomDepth, double intervalDepth )   		
    {
    	if ( intervalDepth < .01 )
    	{
    		return null;
    	}
    	
    	if ( bottomDepth < .0 )
    	{
    		bottomDepth = -bottomDepth;
    	}
    	double rprofs = (bottomDepth - surfaceDepth)/intervalDepth;
    	int nprofs = (int)( rprofs + .01) + 1; 
    	
    	int intervalMinutes = 1440;
    	int offsetMinutes = 0;
    	TimeSeriesContainer baseTsc = null;
    	try
    	{
    		TimeSeriesMath tsMath = (TimeSeriesMath)TimeSeriesMath.generateRegularIntervalTimeSeries(
    				startDate, endDate, 
    				intervalMinutes, offsetMinutes, tval );
    		baseTsc = tsMath.getContainer();
        }
        catch ( HecMathException hme )
        {
			LOGGER.log(Level.SEVERE, "Exception occurred while generating daily temperature profile data.", hme);
            return null;
        }
        
    	baseTsc.location = locationName;
    	baseTsc.version = versionName;
    	baseTsc.interval = intervalMinutes;
    	baseTsc.units = "C";
    	baseTsc.type = ParameterType.INST;
    	baseTsc.units = "C";
    	
        DSSPathString dsspath = new DSSPathString("", locationName, "", "", "1DAY", versionName);
    	dsspath.setCPart("Temp-Water");
    	baseTsc.parameter = "Temp-Water";
    	baseTsc.fullName = dsspath.getPathname();
  	
    	
    	// clone the base time series
    	TimeSeriesContainer wtTemplate = (TimeSeriesContainer)baseTsc.clone();
    	
    	TimeSeriesContainer[] wtprofTs = new TimeSeriesContainer[nprofs];
    	double dep = surfaceDepth;
    	int iint,ifrac;
    	double frac;
    	String fstr, paramstr;
    	for ( int i=0; i<nprofs; i++)
    	{
    		wtprofTs[i] = (TimeSeriesContainer)wtTemplate.clone();
    		iint = (int)dep;
    		frac = dep % 1.;
    		ifrac = (int)Math.round(frac*10.);
    		fstr = Integer.toString(iint) + "," + Integer.toString(ifrac);
    		paramstr = wtTemplate.parameter + "-" + fstr + "m";
    		wtprofTs[i].parameter = paramstr;
    		dsspath.setCPart(paramstr);
    		wtprofTs[i].fullName = dsspath.getPathname();
    		
    		dep += intervalDepth;
    	}
    
    	return wtprofTs;
    }
}
