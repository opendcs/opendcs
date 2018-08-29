/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package usace.rowcps.computation.resevap;

import rma.util.RMAConst;

/**
 * Data and functions for computing downwelling IR flux. 

 * @author RESEVAP program by Steven F. Daly  (ERDC/CRREL)
 * conversion to Java by Richard Rachiele (RMA)
 */
public class Dnirflx
{
    
    private static final double[][] COEF11 =
    {
        { 1.05 , 0.6, 5.0, 25.0 },
        {  4.1 , 0.3, 4.0, 25.0 },
        {  7.  , 1.5, 3.0, 30.0 }
    };
    
    private static final double[][] COEF12 =
    {
        { 1.05 , 0.6, 1.5, 25.0 },
        {  4.1 , 2.0, 1.7, 25.0 },
        {  7.  , 1.5, 3.0, 30.0 }
    };
   
    private static final double[][] COEF21 =
    {
        { 1.15, 0.45, 5.0, 25.0 },
        {  4.1 , 2.0, 1.7, 25.0 },
        {  7.  , 1.5, 3.0, 30.0 }
    };
    
    private static final double[][] COEF22 =
    {
        { 1.15,  0.6, 1.5, 25.0 },
        {  4.4,  1.2, 3.0, 25.0 },
        {  7.  , 1.5, 3.0, 30.0 }
    };
    
    private static final double[][][][] COEF = new double[2][2][3][4];
    static 
    {
        COEF[0][0] = COEF11;
        COEF[0][1] = COEF12;
        COEF[1][0] = COEF21;
        COEF[1][1] = COEF22;
    }
    
    public Dnirflx()
    {

    }
    
    /**
     *  Routine calculates the downwelling IR flux.  In this
     *  routine it is assumed that the major contribution from the clear
     *  flux is emitted by the atmosphere below the cloud layers.  Thus,
     *  the clear air flux is NOT weighted by (1-cloud cover).  If the
     *  cloud amount is missing a global cloud mean of 0.53 is assumed.  In
     *  this case the entire cloud amount is assigned to the low cloud type.
     *  If low cloud is not missing but middle and high cloud amounts are
     *  the middle and high cloud amount is set to 0.  The cloud base, if
     *  missing is set using climatological information. The climatological cloud
     *  base is a function of season and latitude and is based on the research work 
     *  done by G Koenig in his investigation of albedo vs. greenhouse effects of
     *  cloud on climate
     *
     * @param jday  -  Julian Day
     * @param airTemp  -  Air Temperature, deg C
     * @param rh       -  Relative Humidity
     * @param ematm    -  atmospheric emissivity
     * @param lat      -  station latitude
     * @param cloudCover  -  Cloud cover fractions and base heights (KILOMETERS)
     * @return          - downward IR flux 
     */
    public static double dnirflx( int jday, double airTemp, double rh, double ematm, 
            double lat, CloudCover[] cloudCover  )
            
    {
        double ta,doy,lcldbse,mcldbse,hcldbse,lcld;
        double mcld,hcld;
        
        int  i,j,isean,ilat;
        double a,b,c,d,zlcld,zmcld,zhcld;
        double flxcld,flxclr;
        
        // Convert variables from res_common.f to local subroutine
        doy = jday;
        ta = airTemp;
        
        lcldbse = cloudCover[2].height;
        mcldbse = cloudCover[1].height;
        hcldbse = cloudCover[0].height;
        
        lcld = cloudCover[2].fractionCloudCover;
        mcld = cloudCover[1].fractionCloudCover;
        hcld = cloudCover[0].fractionCloudCover;
        
        // if missing cloud cover, get default values
        // repeated in Solflx.insol() 
        if ( !RMAConst.isValidValue(lcld) )
        {
            lcld = cloudCover[2].getDefaultFractionCloudCover();
        }
        if ( !RMAConst.isValidValue(mcld) )
        {
            mcld = cloudCover[1].getDefaultFractionCloudCover();
        }
        if ( !RMAConst.isValidValue(hcld) )
        {
            hcld = cloudCover[0].getDefaultFractionCloudCover();
        }
        
        ilat = 0;
        if (lat >= Math.abs(25.0)) ilat = 1;
        
        isean = 1;  // not winter
        if ( lat > 0.0 && (int)doy > 330 ||  
                (int)doy < 65 )
        {
            // winter, northern hemisphere
            isean = 0;
        }
        if ( lat < 0.0 && (int)doy > 150 ||
                (int)doy < 250 )
        {
            // winter, southern hemisphere
            isean = 0;
        }
        
        // Set cloud base altitude if missing
        // if ( lcldbse == mflag && lcld != 0.0 ) 
        if ( !RMAConst.isValidValue(lcldbse) && lcld != 0.0 ) 
        {
            a = COEF[isean][ilat][0][0];            
            b = COEF[isean][ilat][0][1];            
            c = COEF[isean][ilat][0][2];            
            d = COEF[isean][ilat][0][3];  
            zlcld = a - b*(1.0 - Math.abs(Math.cos(c*(lat - d))));
        }
        else
        {
            zlcld = lcldbse;
        }
        
        //if ( mcldbse == mflag && mcld != 0.0 ) 
        if ( !RMAConst.isValidValue(mcldbse) && mcld != 0.0 ) 
        {
            a = COEF[isean][ilat][1][0];            
            b = COEF[isean][ilat][1][1];            
            c = COEF[isean][ilat][1][2];            
            d = COEF[isean][ilat][1][3];  
            zmcld = a - b*(1.0 - Math.abs(Math.cos(c*(lat - d))));
        }
        else
        {
            zmcld = mcldbse;
        }
        
        //if ( hcldbse == mflag && hcld != 0.0 ) 
        if ( !RMAConst.isValidValue(hcldbse) && hcld != 0.0 ) 
        {
            a = COEF[isean][ilat][2][0];            
            b = COEF[isean][ilat][2][1];            
            c = COEF[isean][ilat][2][2];            
            d = COEF[isean][ilat][2][3];  
            zhcld = a - b*(1.0 - Math.abs(Math.cos(c*(lat - d))));
        }
        else
        {
            zhcld = hcldbse;
        }
        
        // calculate the effective middle and high cloud amounts assuming random overlap
        hcld = hcld*(1.0 - mcld)*(1.0 - lcld);
        mcld = mcld*(1.0 - lcld);

        // Calculate the clear air flux and cloud flux
        // Replace the cloud flux parameterization with stephen Boltzman approach using 
        // a climatologically atmospheric profile based on season and latitude. The profile
        // should be adjusted by comparing the climate temperature at the surface with the
        // measured temperature. 

        if  (lcld == 0.0 ) zlcld = 0.0;
        if  (mcld == 0.0 ) zmcld = 0.0;
        if  (hcld == 0.0 ) zhcld = 0.0;
        
        flxclr = ematm* Const.SIGMA * Math.pow( ta + 273.15, 4.);
        flxcld = lcld*(94.0 - 5.8*zlcld) + mcld*(94.0 - 5.8*zmcld)
            	   + hcld*(94.0 - 5.8*zhcld);
        
        // Total downwelling flux
        double flxir = flxclr + flxcld;
        
        return flxir; 
    }
    
    /**
     * Compute the longwave atmospheric emissivity.
     * 
     * The longwave atmospheric emissivity is given as
     *     ematm=-0.792+3.161*ematmp-1.573*ematmp^2
     * where
     *     ematmp=-0.7+5.95*10**-5*vp*exp(1500/ta)
     * where vp is the vapor pressure in mbs and is obtained from
     * the relative humidity and ambient temperature using the Clausius
     * Clapeyrin equation.
     * 
     * A new atmospheric emissivity formulation has been implemented based on 
     * Todd M. Crawford & Claude E. Duchon, Am Improved parameterization for estimating
     * effective atmospheric emissivity for use in calculating daytime downwelling longwave
     * radiation, Jour of Applied meteorology, vol 18 april 1999, 474-480
     * 
     *         ematm=1.24*[ea/Ta(K)]^(1/7)
     * 
	 * @param airTemp
	 * @param relH
     * @return 
     */
    public static double emisatm(double airTemp, double relH)
    {
        // local variables
        double latent,rv,eso,ea,ta;
        
        // Convert variables from res_common.f to local subroutine
        double rh = relH;
        
        rv = 461.;
        eso = 6.13;
        
        // retrieve the temp for this hour
        ta = airTemp + 273.15;
        
        // Calculate the vapor pressure based on the RH(in %)
        latent = (-2.43e-3*ta + 3.166659) * 1.E6;
        ea = (eso* Math.exp(latent/rv*(1.0/273.15 - 1.0/ta)) )*rh*.01;
        
        // Calculate the emissivity
        double ematm = 1.24 * Math.pow(ea/ta,1./7.) ;

        return ematm;  
    }
    
}
