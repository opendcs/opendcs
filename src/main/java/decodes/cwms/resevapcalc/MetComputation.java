/* 
 * Copyright (c) 2018
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */
package decodes.cwms.resevapcalc;

import decodes.cwms.HecConstants;
//import hec.heclib.util.HecTime;

import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
//import rma.util.RMAConst;

/**
 * Compute surface heat exchange and evaporation rate from
 * meteorological values.
 * 
 * @author richard
 */
public class MetComputation
{
	private static final Logger LOGGER = Logger.getLogger(MetComputation.class.getName());
	/** input meteorological data */
    EvapMetData metData;
    
    /** surface evap and convective and latent energy exchange */
    EvapWater evapWater;
    
    // computed energy exchange values
    double solar;
    double flxir;
    double flxir_out;
    double hs;
    double hl;
    
    boolean metFailed;
    
    // there must be at least old met data available
    boolean metDefined = false;
    
    public MetComputation()
    {
        evapWater = new EvapWater();
    }
    
    public void setMetData( EvapMetData metData )
    {
        this.metData = metData;
    }
    
    public void setEvapWater( EvapWater evapWater )
    {
        this.evapWater = evapWater;
    }

    public void computeMetAndEvap( Date currentTime, double surfaceTemp,
                                  ReservoirLocationInfo resLocationInfo) throws ResEvapException {
        // get met values from time series for current time
        double airPressure = metData.getAirPressure(currentTime);
        // some rounding problems with kPa to mb conversion
        if ( airPressure < 0.0 )
        {
        	airPressure = -901.;
        }
        double windSpeed = metData.getWindSpeed(currentTime);
        double relHumidity = metData.getRelHumidity(currentTime);
        double airTemp = metData.getAirTemp(currentTime);
        
        CloudCover[] cloudCover = metData.getCloudCover(currentTime);
       
        // check values
        boolean missingMetData = false;
        metFailed = false;
        
        if ( !HecConstants.isValidValue(windSpeed) )
        {
            String msg = "Wind speed missing or bad " + currentTime;
			LOGGER.log(Level.SEVERE, msg );
            missingMetData = true;
        }
        if ( !HecConstants.isValidValue(airTemp) )
        {
            String msg = "Air temp missing or bad " + currentTime;
            LOGGER.log(Level.SEVERE, msg );
            missingMetData = true;
        }
        if ( !HecConstants.isValidValue(relHumidity) )
        {
            String msg = "RH missing or bad " + currentTime;
            LOGGER.log(Level.SEVERE, msg );
            missingMetData = true;
        }
        if ( !HecConstants.isValidValue(airPressure) )
        {
            String msg = "PRESSURE missing or bad " + currentTime;
            LOGGER.log(Level.SEVERE, msg );
            missingMetData = true;
        }

        // there is at least old met data now available
        // that can be used for missing data in next time step
        if ( !missingMetData )
        {
            metDefined = true;
        }
        
        if ( !metDefined)
        {
            metFailed = true;
            return;
        }
        
        // check cloud cover
        boolean cloudFailed = false;
        for ( int ic=0; ic<3; ic++)
        {
            CloudCover cld = cloudCover[ic];
            if ( !HecConstants.isValidValue(cld.fractionCloudCover) )
            {
                int ic1 = ic+1;
                String msg = " Cover (" + ic1 + ") missing or bad"
                        + currentTime;
                LOGGER.log(Level.SEVERE, msg );    
            }
            if ( !HecConstants.isValidValue(cld.height) )
            {
                String heightStr = cld.getTypeName();
                String msg = heightStr + " missing or bad " + currentTime;
                LOGGER.log(Level.SEVERE, msg );    
            }
        }
        
        // If a met parameter is missing fill in with the previous hour value
        // Store copy of previous good value
        if ( !HecConstants.isValidValue(windSpeed) )
        {
            windSpeed = metData.windSpeed_old;
        }
        else
        {
            metData.windSpeed_old = windSpeed;
        }
        
        if ( !HecConstants.isValidValue(airTemp) )
        {
            airTemp = metData.airTemp_old;
        }
        else
        {
            metData.airTemp_old = airTemp;
        }
        
        if ( !HecConstants.isValidValue(relHumidity) )
        {
            relHumidity = metData.relHumidity_old;
        }
        else
        {
            metData.relHumidity_old = relHumidity;
        }
        
        if ( !HecConstants.isValidValue(airPressure) )
        {
            airPressure = metData.airPressure_old;
        }
        else
        {
            metData.airPressure_old = airPressure;
        }

        // store values for later output
        metData.windSpeed_current = windSpeed;
        metData.airTemp_current = airTemp;
        metData.relHumidity_current = relHumidity;
        metData.airPressure_current = airPressure;
        

        // compute solar radiation
        double longitude = resLocationInfo.lon;
        double latitude = resLocationInfo.lat;
        double gmtOffset = resLocationInfo.gmtOffset;
        
        computeMet( currentTime, gmtOffset, surfaceTemp, 
                windSpeed, airTemp, airPressure, relHumidity,
                cloudCover, latitude, longitude);
      
        
        // the global value UR was overriden in evap_wat.f if < .10
        if ( windSpeed < .10 )
        {
            windSpeed = .10;
            metData.windSpeed_current = windSpeed;
        }
        // the global value TS was overriden in evap_wat.f TS < .0
        if ( surfaceTemp < .0 )
        {
            surfaceTemp = 0.0;
        }
        
        // compute evaporation, and latent and sensible heat exchange
        computeEvap( 
        		resLocationInfo,
                surfaceTemp,
                windSpeed, airTemp, airPressure, relHumidity );

    }
    
	/**
	 * Compute downward solar, IR and outgoing IR
	 *  
	 * @param currentTime
	 * @param gmtOffset
	 * @param surfaceTemp
	 * @param windSpeed
	 * @param airTemp
	 * @param airPressure
	 * @param relHumidity
	 * @param cloudCover
	 * @param lat
	 * @param lon
	 */
    public void computeMet( Date currentTime, double gmtOffset,
            double surfaceTemp,
            double windSpeed, double airTemp, 
            double airPressure, double relHumidity,
            CloudCover[] cloudCover,
            double lat, double lon)
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentTime);
        int jday = calendar.get(Calendar.DAY_OF_YEAR);
        
        // scale cloud cover altitude
        for ( CloudCover cloud : cloudCover)
        {
            if ( HecConstants.isValidValue(cloud.height) )
            {
                cloud.height /= 1000.;
            }
        }
        // compute incoming solar radiation
        SolarFlux solarFlx = new SolarFlux();
        solarFlx.solflx( currentTime, 0,//gmt_offset todo
                lon, lat, cloudCover );

        solar = solarFlx.SOLAR;
        
                    // compute incoming Long Wave radiation
        if ( HecConstants.isValidValue(surfaceTemp) )
        {
            // compute atmospheric emissivity
            double ematm = DownwellingInfraRedFlux.emisatm( airTemp, relHumidity);
            double flxir = DownwellingInfraRedFlux.dnirflx( jday, airTemp,
                    relHumidity, ematm, lat, cloudCover );

            this.flxir = flxir;
        }

        // Calculate outgoing Long Wave radiation Flux
        double ws_temp = surfaceTemp + 273.15;
        double flxir_out = -1.*ResEvap.EMITTANCE_H20 * ResEvap.SIGMA
                * Math.pow( ws_temp, 4. );
        
        this.flxir_out = flxir_out;
        
        return;

    }


    /**
	 * Compute surface evaporation rate, convective and latent heat flux 
     * 
     * @param resLocationInfo
     * @param surfaceTemp
     * @param windSpeed
     * @param airTemp
     * @param airPressure
     * @param relHumidity
     */
    public void computeEvap( ReservoirLocationInfo resLocationInfo, 
            double surfaceTemp,
            double windSpeed, double airTemp, 
            double airPressure, double relHumidity )
    {
        //
        double rt = resLocationInfo.rt;
        double ru = resLocationInfo.ru;
        double rq = resLocationInfo.rq;
        
        if ( evapWater == null )
        {
            evapWater = new EvapWater();
        }
        evapWater.evap_water( surfaceTemp,
            airTemp, relHumidity,
            windSpeed, airPressure, 
            rt, ru, rq  );

    }
       
}
