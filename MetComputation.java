/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package usace.rowcps.computation.resevap;

import hec.heclib.util.HecTime;
import rma.util.RMAConst;

/**
 * Compute surface heat exchange and evaporation rate from
 * meteorological values.
 * 
 * @author richard
 */
public class MetComputation
{
	/** input meteorological data */
    EvapMetData _metData;
    
    /** surface evap and convective and latent energy exchange */
    EvapWater _evapWater;
    
    // computed energy exchange values
    double _solar;
    double _flxir;
    double _flxir_out;
    double _hs;
    double _hl;
    
    boolean _metFailed;
    
    // there must be at least old met data available
    boolean _metDefined = false;
    
    public MetComputation()
    {
        _evapWater = new EvapWater();
    }
    
    public void setMetData( EvapMetData metData )
    {
        _metData = metData;
    }
    
    public void setEvapWater( EvapWater evapWater )
    {
        _evapWater = evapWater;
    }

    public void computeMetAndEvap( HecTime currentTime, double surfaceTemp,
    		ReservoirLocationInfo resLocationInfo)
    {
        // get met values from time series for current time
        double airPressure = _metData.getAirPressure(currentTime);
        // some rounding problems with kPa to mb conversion
        if ( airPressure < 0.0 )
        {
        	airPressure = -901.;
        }
        double windSpeed = _metData.getWindSpeed(currentTime);
        double relHumidity = _metData.getRelHumidity(currentTime);
        double airTemp = _metData.getAirTemp(currentTime);
        
        CloudCover[] cloudCover = _metData.getCloudCover(currentTime);
       
        // check values
        boolean missingMetData = false;
        _metFailed = false;
        
        if ( !RMAConst.isValidValue(windSpeed) )
        {
            String msg = "Wind speed missing or bad " + currentTime;
            System.out.println( msg );
            missingMetData = true;
        }
        if ( !RMAConst.isValidValue(airTemp) )
        {
            String msg = "Air temp missing or bad " + currentTime;
            System.out.println( msg );
            missingMetData = true;
        }
        if ( !RMAConst.isValidValue(relHumidity) )
        {
            String msg = "RH missing or bad " + currentTime;
            System.out.println( msg );
            missingMetData = true;
        }
        if ( !RMAConst.isValidValue(airPressure) )
        {
            String msg = "PRESSURE missing or bad " + currentTime;
            System.out.println( msg );
            missingMetData = true;
        }

        // there is at least old met data now available
        // that can be used for missing data in next time step
        if ( !missingMetData )
        {
            _metDefined = true;
        }
        
        if ( !_metDefined )
        {
            _metFailed = true;
            return;
        }
        
        // check cloud cover
        boolean cloudFailed = false;
        for ( int ic=0; ic<3; ic++)
        {
            CloudCover cld = cloudCover[ic];
            if ( !RMAConst.isValidValue(cld.fractionCloudCover) )
            {
                int ic1 = ic+1;
                String msg = " Cover (" + ic1 + ") missing or bad"
                        + currentTime;
                System.out.println( msg );    
            }
            if ( !RMAConst.isValidValue(cld.height) )
            {
                String heightStr = cld.getTypeName();
                String msg = heightStr + " missing or bad " + currentTime;
                System.out.println( msg );    
            }
        }
        
        // If a met parameter is missing fill in with the previous hour value
        // Store copy of previous good value
        if ( !RMAConst.isValidValue(windSpeed) )
        {
            windSpeed = _metData._windSpeed_old;
        }
        else
        {
            _metData._windSpeed_old = windSpeed;
        }
        
        if ( !RMAConst.isValidValue(airTemp) )
        {
            airTemp = _metData._airTemp_old;
        }
        else
        {
            _metData._airTemp_old = airTemp;
        }
        
        if ( !RMAConst.isValidValue(relHumidity) )
        {
            relHumidity = _metData._relHumidity_old;
        }
        else
        {
            _metData._relHumidity_old = relHumidity;
        }
        
        if ( !RMAConst.isValidValue(airPressure) )
        {
            airPressure = _metData._airPressure_old;
        }
        else
        {
            _metData._airPressure_old = airPressure;
        }

        // store values for later output
        _metData._windSpeed_current = windSpeed;
        _metData._airTemp_current = airTemp;
        _metData._relHumidity_current = relHumidity;
        _metData._airPressure_current = airPressure;
        

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
            _metData._windSpeed_current = windSpeed;
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
    public void computeMet( HecTime currentTime, double gmtOffset,
            double surfaceTemp,
            double windSpeed, double airTemp, 
            double airPressure, double relHumidity,
            CloudCover[] cloudCover,
            double lat, double lon)
    {

        int jday = currentTime.dayOfYear();
        
        // scale cloud cover altitude
        for ( CloudCover cloud : cloudCover)
        {
            if ( RMAConst.isValidValue(cloud.height) )
            {
                cloud.height /= 1000.;
            }
        }
        // compute incoming solar radiation
        Solflx solarFlx = new Solflx();
        solarFlx.solflx( currentTime, gmtOffset,
                lon, lat, cloudCover );

        _solar = solarFlx.SOLAR;
        
                    // compute incoming Long Wave radiation
        if ( RMAConst.isValidValue(surfaceTemp) )
        {
            // compute atmospheric emissivity
            double ematm = Dnirflx.emisatm( airTemp, relHumidity);
            double flxir = Dnirflx.dnirflx( jday, airTemp,
                    relHumidity, ematm, lat, cloudCover );

            _flxir = flxir;
        }

        // Calculate outgoing Long Wave radiation Flux
        double ws_temp = surfaceTemp + 273.15;
        double flxir_out = -1.*ResEvap.EMITTANCE_H20 * ResEvap.SIGMA
                * Math.pow( ws_temp, 4. );
        
        _flxir_out = flxir_out;
        
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
        
        if ( _evapWater == null )
        {
            _evapWater = new EvapWater();
        }
        _evapWater.evap_water( surfaceTemp,
            airTemp, relHumidity,
            windSpeed, airPressure, 
            rt, ru, rq  );

    }
       
}
