/* 
 * Copyright (c) 2018
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */
package decodes.cwms.resevapcalc;

import decodes.cwms.HecConstants;
import decodes.tsdb.CTimeSeries;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


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
        
    // output computed time series
    double solarRadTsc;
    double IR_DownTsc;
    double IR_OutTsc;
    double sensibleHeatTsc;
    double latentHeatTsc;
    double evapRateHourlyTsc;
    double evapDailyTsc;
    double surfaceTempTsc;
    NavigableMap<Integer, Integer> _timeMap;

    CTimeSeries[] _inputTimeSeries;
    
    // FPart of output Ts
    String _versionName;
            
    // store Water temperature profile data
    // one profile for each hour
    double[] wtempProfiles;
            
    public EvapReservoir reservoir;
    public EvapMetData metData;
    
    private File workDir;
    
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
        this.workDir = workDir;
        
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
        this.reservoir = reservoir;
        
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
        this.metData = metData;
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
     * @param currentTime
     * @param gmtOffset
     * @return 
     */
    public boolean compute(Date currentTime,
            double gmtOffset ) throws ResEvapException
    {
    	if ( reservoir == null )
    	{
    		throw new ResEvapException ("ResEvap.compute: No reservoir has been set");
    	}
    	
    	if ( metData == null )
    	{
    		throw new ResEvapException ("ResEvap.compute: No Meteorological data has been set");
    	}
    	
        // diagnostic output
        File outfil = null;
        File metoutfil = null;
        File toutfil = null;
        File xoutfil = null;
        
        if ( workDir != null )
        {
            outfil = new File(workDir.getAbsolutePath() + "/" + "wtout_java.dat");
            metoutfil = new File(workDir.getAbsolutePath() + "/" + "testtout_java.dat");
            toutfil = new File(workDir.getAbsolutePath() + "/" + "xout2_java.dat");
            xoutfil = new File(workDir.getAbsolutePath() + "/" + "xout_java.dat");
        }

        int intervalMinutes = 60;

        double deltT = 3600.;
        
        // MetComputation handles the surface water heat exchange and evap 
        MetComputation metComputation = new MetComputation();
        metComputation.setMetData(metData);
        
        // ResWtCompute computes the water temperature profile for the reservoir.
        ResWaterTemperatureCompute resWtCompute = new ResWaterTemperatureCompute(reservoir);

        
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
            	reservoir.setDebugFile(xout);
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
	        	reservoir.resSetup();
	        }

	        int resj = reservoir.getResj();
	        reservoir._resj_old = resj;
	        
	        // loop through compute period
//	        HecTime currentTime = new HecTime(hecStartTime);
	        
	        // reservoir location info (lat, lon, gmtOffset) for met compute
	        ReservoirLocationInfo resLocationInfo = reservoir.getReservoirLocationInfo();
	        resLocationInfo.gmtOffset = gmtOffset;
	        
	        // use elevation time series for reservoir temperature profile
	        // calculation, else use starting elevation
	        boolean useElevTS = false;  //TODO make an option
	        double wselCurrent = reservoir.getElevation();

//            if ( useElevTS )
//            {
//                double newElev = reservoir.getCurrentElevation(currentTime);
//                if ( HecConstants.isValidValue(newElev) )
//                {
//                    wselCurrent =  newElev;
//                    wselOld = wselCurrent;
//
//                    reservoir.setElevationMeters(wselCurrent);
//                    reservoir.resSetup( true );
//                }
//            }

                double surfaceTemp = reservoir._wt[resj];

                // compute solar and longwave down radiation
                // and evaporation for reservoir location
                metComputation.computeMetAndEvap(currentTime, surfaceTemp,
                        resLocationInfo);

                boolean noProblem = true;

                // if no valid met data yet skip reservoir temp compute
                if (!metComputation.metFailed) {
                    // compute temperature profile in reservoir
                    noProblem = resWtCompute.computeReservoirTemp(
                            currentTime, metComputation, deltT);
                }

                if (noProblem) {

                    solarRadTsc = metComputation.solar;
                    IR_DownTsc = metComputation.flxir;
                    IR_OutTsc = metComputation.flxir_out;
                    latentHeatTsc = metComputation.evapWater.hl;
                    sensibleHeatTsc = metComputation.evapWater.hs;
                    surfaceTempTsc = surfaceTemp;  // BOP surface temp
                    // evap is in mm/day.  Divide by 24 to get instantaneous
                    // hourly value
                    evapRateHourlyTsc = metComputation.evapWater.evap/ 24.;

                    // store wt profile
                    int numLayers = reservoir.getResj() + 1;
                    wtempProfiles = new double[EvapReservoir.NLAYERS];
                    for (int ilyr = 0; ilyr < numLayers; ilyr++) {
                        wtempProfiles[ilyr] = reservoir._wt[numLayers-1-ilyr];
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
		catch (RuntimeException ex)
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
     * Get a List containing ResEvap computed TimeSeriesContainers 
     * @return
     */
    public List<Double> getComputedMetTimeSeries()
    {
    	List<Double> computedTsList = new ArrayList<Double>();
    	computedTsList.add(surfaceTempTsc);
    	computedTsList.add(sensibleHeatTsc);
    	computedTsList.add(latentHeatTsc);
    	computedTsList.add(solarRadTsc);
    	computedTsList.add(IR_DownTsc);
    	computedTsList.add(IR_OutTsc);
    	computedTsList.add(evapRateHourlyTsc);

    	return computedTsList;
    }
    
    /**
     * Get the computed hourly evaporation for the reservoir
     * @return
     */
    public double getHourlyEvapRateTimeSeries()
    {
    	return evapRateHourlyTsc;
    }
    public double[] getHourlyWaterTempProfile()
    {
        return wtempProfiles;
    }
}
