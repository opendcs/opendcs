/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package usace.rowcps.computation.resevap;

import hec.heclib.util.HecTime;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *  Class used to compute reservoir temperature profile
 */
public class ResWtCompute
{
    BufferedWriter _tout = null;

    // reservoir layers, segments.
    EvapReservoir _reservoir;
    int _resj_old = -1;

    // working arrays
    private double[] a;
    private double[] b;
    private double[] c;
    private double[] r;
    private double[] u;
    private double[] rhow_org;
    private double[] cp_org;
    private double[] wt_tmp;
    private double[] pe_mix_out;

    public ResWtCompute( EvapReservoir reservoir )
    {
        _reservoir = reservoir;
        
        // dimension working arrays
        a = new double[EvapReservoir.NLAYERS];
        b = new double[EvapReservoir.NLAYERS];
        c = new double[EvapReservoir.NLAYERS];
        r = new double[EvapReservoir.NLAYERS];
        u = new double[EvapReservoir.NLAYERS];
        rhow_org = new double[EvapReservoir.NLAYERS];
        cp_org = new double[EvapReservoir.NLAYERS];
        wt_tmp = new double[EvapReservoir.NLAYERS];
        pe_mix_out = new double[EvapReservoir.NLAYERS];
   }
    
    /**
     * set output writer for diagnostic text output for reservoir 
     * energy balance
     *  
     * @param tout
     */
    public void setOutfile( BufferedWriter tout )
    {
        _tout = tout;
    }

    /**
     * Compute the updated reservoir temperature profile
     * from updated heat exchange values.
     * 
     * @param currentTime
     * @param metComputation
     * @param delT
     * @return 
     */
    public boolean computeReservoirTemp( HecTime currentTime,
            MetComputation metComputation,
                         double delT)
    {
        double avg_rhow,total_vol,sFreq,kzx,surfArea_x;
        double fi,fi_check,sum_energy_in,sml_delz,sum_energy_diff;
        double sum_energy_mix,del_rhow,org_wt1,org_wt2;
        double al,abar,au,sml_vol,z_mix,z_sml,z_next;
        double delZu,dhatZu,delZl,dhatZl;
        double topDepth,bottomDepth,tempDepth;
        double solar_tot;
        int sml,sml_top,sml_flag,IFLAG;
        double TKE,wtin,wtout,wt_mix, rhow_mix;
        double org_PE,new_PE,w_3,KE_conv,Salinity;
        double pe_mix, u_H2O_star, KE_stir;
        
        // zero working arrays
        java.util.Arrays.fill(a, 0.);
        java.util.Arrays.fill(b, 0.);
        java.util.Arrays.fill(c, 0.);
        java.util.Arrays.fill(r, 0.);
        java.util.Arrays.fill(u, 0.);
        java.util.Arrays.fill(rhow_org, 0.);
        java.util.Arrays.fill(cp_org, 0.);
        java.util.Arrays.fill(wt_tmp, 0.);
        java.util.Arrays.fill(pe_mix_out, 0.);
        
        // Calculate the water density and heat capacity at each level. 
        // Determine average density of reservoir

        // global to local names
        double[] zd = _reservoir._zd;
        double[] kz = _reservoir._kz;
        double[] zarea = _reservoir._zarea;
        double[] delz = _reservoir._delz;
        double[] zvol = _reservoir._zvol;
        double[] ztop = _reservoir._ztop;

        double[] rhow = _reservoir._rhow;
        double[] cp = _reservoir._cp;
        double[] wt = _reservoir._wt;

        double wsel = _reservoir.getElevation();
        double surfArea = _reservoir._surfArea;
        double grav = Const.GRAV;

        double SOLAR= metComputation._solar;
        double flxir = metComputation._flxir;
        double flxir_out = metComputation._flxir_out;
        double hs = metComputation._evapWater._hs;
        double hl = metComputation._evapWater._hl;
        double evap = metComputation._evapWater._evap;
        double ustar = metComputation._evapWater._ustar;

        double katten= _reservoir._attenuationConst;

        double ur = metComputation._metData._windSpeed_current;
        double rh = metComputation._metData._relHumidity_current;
        double tr = metComputation._metData._airTemp_current;
        double p = metComputation._metData._airPressure_current;

        double theta = ResEvap.THETA;
        double albedo = ResEvap.ALBEDO;
        double penfrac = ResEvap.PENFRAC;
        double eta_conv = ResEvap.ETA_CONVECTIVE;
        double wind_critic = ResEvap.WIND_CRITIC;
        double eta_stir = ResEvap.ETA_STIRRING;
 
        int resj_old = _reservoir._resj_old;
        int resj = _reservoir.getResj();        
        
        avg_rhow = 0.0;
        total_vol = 0.0;
        sum_energy_in = 0.0;
        TKE = 0.;
        
        // Reset water temperatures if wsel has changed enough to increase the number of layers
        if ( resj != resj_old)
        {
            for ( int j=0; j<resj_old; j++)
            {
                wt_tmp[j] = wt[j];
            }

            int i = resj;
            int j = resj_old;
            
            while ( i >= 0 && j >= 0 )
            {
                wt[i] = wt_tmp[j];
                i = i -1;
                j = j -1;
            }
            if ( resj > resj_old )
            {
                for ( int k=i; k>=0; k--)
                {
                    wt[k] = wt_tmp[0];
                }
            }
        }        

        
        for ( int i=0; i<=resj; i++)
        {
            // Prevent supercooling
            if (wt[i] < 0.)  wt[i] = 0.;
            
            rhow[i] = EvapUtilities.den_h2o(wt[i]);
            cp[i] = EvapUtilities.cp_h2o(wt[i]);
            avg_rhow = avg_rhow + rhow[i]*zvol[i];
            total_vol = total_vol + zvol[i];
            sum_energy_in = sum_energy_in + rhow[i]*cp[i]*zvol[i]*wt[i];  
        }
        
        avg_rhow = avg_rhow/total_vol;
        
        // Calculate Stability Frequency and then Diffusion coefficient
        
        for ( int i=resj; i>=1; i--)  //TODO check 
        {
            if ( (rhow[i]-rhow[i-1]) != 0.)
            {
                sFreq = grav/avg_rhow*Math.abs(rhow[i]-rhow[i-1])/
                            (zd[i-1]-zd[i]);
            }
            else
            {
                sFreq = 0.00007;
            }
            
            if (sFreq < 0.00007)  sFreq = 0.00007;
            
            surfArea_x = surfArea;
            if (surfArea < 350.)
            {
                  surfArea_x = surfArea;
            }
            else
            {
                 surfArea_x = 350.;
            }

            kzx = .000817* Math.pow( surfArea_x , 0.56 ) *
                     Math.pow( (Math.abs(sFreq)), (-0.43)); // cm^2/s
            kzx = .0001*kzx;                                // m^2/s
            kz[i] = 1.2*kzx*cp[i]*rhow[i];                  // j/(s m C) factor to arbitrarly reduce diffusion       
        }
        
        // Perform diffusion calculation
        // Develop the elements of the tridiagonal equation
        
        for ( int i=0; i<=resj; i++)
        {
            // Calculate areas
            if ( i == 0 )
            {
                au = zarea[i];
                abar =  0.5*zarea[i];
                al = 0.0;
            }
            else
            {
                au = zarea[i];
                abar = 0.5*(zarea[i]+zarea[i-1]);
                al = zarea[i-1];
            }
            
             if (i < resj)
            {
                delZu = 0.5*(delz[i+1]+delz[i]);
                dhatZu = (kz[i+1]/(cp[i+1]*rhow[i+1])*delz[i+1]+
                          kz[i]/(cp[i]*rhow[i])*delz[i])/(delz[i+1]+delz[i]);
            }
            else
            {
                dhatZu = 0.;
                delZu = 1.0;
            }

            if (i > 0 )
            {
                delZl = 0.5*(delz[i-1]+delz[i]);
                dhatZl = (kz[i-1]/(cp[i-1]*rhow[i-1])*delz[i-1]+
                          kz[i]/(cp[i]*rhow[i])*delz[i])/(delz[i-1]+delz[i]);
            }
            else
            {
                dhatZl = 0.;
                delZl = 1.0;
            }
            if (i > 0 )
            {
                a[i] = (-1.*delT/delz[i]*dhatZl/delZl*theta*
                    al/abar);
            }
            else
            {
                a[i] = 0.;
            }
          
            b[i] = (1.+delT/delz[i]*dhatZu/delZu*theta*
                  au/abar+
                  delT/delz[i]*dhatZl/delZl*theta*
                  al/abar);
            
            if (i < resj)
            {
                c[i] =(-1.*delT/delz[i]*dhatZu/delZu*theta*au/abar);
            }
            else
            {
                c[i] = 0.;
            }
            
            if ( i == 0 )
            {
                r[i] = wt[i] + delT/delz[i]*au/abar*(dhatZu/delZu*
                      (1.-theta)*(wt[i+1]-wt[i]));
            }
            
            if (i > 0 && i < resj)
            {
                r[i] = wt[i] + delT/delz[i]*au/abar*(dhatZu/delZu*
                    (1.-theta)*(wt[i+1]-wt[i])-
                    dhatZl/delZl*al/abar*
                    (1.-theta)*(wt[i]-wt[i-1]));
            }
            if ( i == resj )
            {
                r[i] = wt[i] + delT/delz[i]*al/abar*(dhatZl/delZl*
                  (1.-theta)*(wt[i]-wt[i-1]));
            }
        }
        
        // Estimate internal heat source due to shortwave radiation
        
        topDepth = 0.;
        solar_tot = 0.;       
        
        for ( int i=resj; i>1; i--)  
        {
            bottomDepth = topDepth + delz[i];
            
           fi = SOLAR* penfrac *(1.- albedo )*
              ( Math.exp(-1.*katten*topDepth)*zarea[i]-
                Math.exp(-1.*katten*bottomDepth)*zarea[i-1])/zvol[i] ;        //!delz[i]
            
            solar_tot = solar_tot + SOLAR * penfrac *(1.- albedo )*
              ( Math.exp(-1.*katten*topDepth)*zarea[i]-
                Math.exp(-1.*katten*bottomDepth)*zarea[i-1])/zvol[i] ;        //!delz[i]
            r[i] = r[i] + fi*delT/(cp[i]*rhow[i]);
            
            topDepth = bottomDepth;
        }
        
        // Add shortwave, IR, sensible, and latent heat fluxes to top
        fi = (SOLAR*(1.-penfrac)*(1.-albedo)+
                flxir + flxir_out -
                hs - hl)*zarea[resj]/zvol[resj];
        
        solar_tot = solar_tot + 
                       SOLAR*(1.-penfrac)*(1.-albedo)*zarea[resj];
        r[resj] = r[resj] + fi*delT/(cp[resj]*rhow[resj]);
        
        // Calculate total flux for energy balance checking
        fi_check = (SOLAR*(1.-albedo)+
                flxir + flxir_out -
                hs - hl)*delT;
        fi_check = fi_check*zarea[resj];
             
        // size of arrays are resj+1
        tridag(a,b,c,r,u,resj+1);
             
 
        tempDepth = 0.25 ;
        sum_energy_diff = 0.;
    
        for ( int i=resj; i>=0; i--)  //TODO check 
        {
            wt[i] = u[i];
            rhow[i] = EvapUtilities.den_h2o(wt[i]);
            rhow_org[i] = rhow[i];
            cp[i] = EvapUtilities.cp_h2o(wt[i]);
            cp_org[i] = cp[i];
            sum_energy_diff = sum_energy_diff + 
                             wt[i]*cp[i]*rhow[i]*zvol[i];

            if ( i >  0 )
            {
                tempDepth = tempDepth + .5*(delz[i]+delz[i-1]);
            }      
        }
        
        // Perform mixing due to Potential energy and wind mixing
        
        //  Update the water density and heat capacity at each level
//      sml_flag = 1
//      sml = resj
//      sml_delz = delz(resj)
//      sml_vol  = zvol(resj)
//      i = resj
//      iend = 1

        EvapUtilities.DoubleContainer rhoAdc = new EvapUtilities.DoubleContainer();
        EvapUtilities.DoubleContainer rhoVdc = new EvapUtilities.DoubleContainer();                  
        EvapUtilities.DoubleContainer spHumdc = new EvapUtilities.DoubleContainer();                  
         
        sml_flag = 1; //TODO check these
        sml = resj;
        sml_delz = delz[resj];
        sml_vol  = zvol[resj];
        int i = resj;
        int iend = 1;     
        
        while ( i >= 2 && iend == 1 )
        {
            rhow[i] = EvapUtilities.den_h2o(wt[i]);
            cp[i] = EvapUtilities.cp_h2o(wt[i]);


            wt_mix = (wt[i]*sml_vol*cp[i]+
                      wt[i-1]*zvol[i-1]*cp[i-1])/
                      (sml_vol*cp[i]+zvol[i-1]*cp[i-1]);
            rhow_mix = EvapUtilities.den_h2o(wt_mix);

            // Calculate the distance to the center of mass (COM) for each of the layers
            // SML after mixing
            z_mix = zcom(resj,sml-1,rhow_mix);

            // SML w/o mixing
            z_sml = zcom(resj,sml,-1.);

            // Layer below SML to be mixed
            z_next = zcom(sml-1,sml-1,-1.);


            pe_mix = grav*(rhow_mix*(sml_vol+zvol[i-1])*
                        (z_mix-ztop[i-2]) -
                        (rhow[i]*sml_vol*(z_sml-ztop[i-2]) +
                        rhow[i-1]*zvol[i-1]*(z_next-ztop[i-2])));

            pe_mix_out[i] = pe_mix;
   
           // Density causes mixing
            if ( pe_mix < 0.)
            {
                sml = i-1;
                sml_delz = sml_delz + delz[i-1];
                sml_vol  = sml_vol + zvol[i-1];
                for (int j = i-1; j<=resj; j++ )  //TODO check
                {
                    wt[j] = wt_mix;
                }
            }
            
            //Density profile is stable
            else
            {
                // First time, estimate convective and wind stirring energy available
                //TKE = 0.;  // java wants this initialized
                
                if ( sml_flag == 1)
                {
                    sml_flag = 0;
                    org_PE = 0.;
                    new_PE = 0.;
                    
                    for ( int j = sml; j<=resj; j++ )
                    {
                        org_PE = org_PE + rhow_org[j]*delz[j]*
                                      (ztop[j]+ztop[i-1])/2.;
                        new_PE = new_PE + rhow_org[j]*delz[j];
                    }
               
                    new_PE = new_PE *(ztop[resj]+ztop[sml-1])/2.;
                    w_3 = grav/(rhow[resj]*delT)*(org_PE-new_PE);
                    if (w_3 < 0.)  w_3 = 0.;
                    KE_conv = eta_conv*rhow[resj]*zarea[sml-1]*w_3*delT;
                    
                    // Calculate wind stirring
                    u_H2O_star = 0.; //TODO init for msg
                    if ( ur > wind_critic )
                    {
                        IFLAG = 1;   // saturation over water
                        Salinity = 0.;
                        
                        EvapUtilities.den_from_rh( rh, p, tr, Salinity,   
                                rhoAdc, rhoVdc, spHumdc, IFLAG );
                        
                        u_H2O_star = ustar*Math.sqrt(rhoAdc.d/rhow[resj]);
                        KE_stir = eta_stir*rhow[resj]*
                            zarea[resj]* Math.pow( u_H2O_star, 3.)*delT;
                    }
                    else
                    {
                        KE_stir = 0.;
                    }
                    
                    if ( KE_stir <  0.0 )
                    {
                        //TODO throw execption
                        String msg = "KE_stir < 0.0 " + 
                                "\neta_stir =" + eta_stir +
                                "\nrhow(resj) =" + rhow[resj] +
                                "\nzarea(resj) =" + zarea[resj] +                                
                                "\nu_H2O_star =" + u_H2O_star +                                
                                "\ndelT =" + delT ;   
						Logger.getLogger(ResWtCompute.class.getName()).log(Level.SEVERE, msg);
                        
                        return false;
                    }

                    TKE = KE_stir + KE_conv;
                }
                
                // Mix layers if sufficient energy

                if ( TKE >= pe_mix )
                {
                    sml = i-1;
                    sml_delz = sml_delz + delz[i-1];
                    sml_vol  = sml_vol + zvol[i-1];
                    for ( int j = i-1; j<=resj; j++ )
                    {
                        wt[j] = wt_mix;
                    }
              
                    TKE = TKE - pe_mix;
                }
                else
                {
                    // End calculations for this time step as not sufficient energy to mix
                    iend = 0;
                }  
            }   
            
            // Move downward to next layer
            i = i-1;

            
        }     // end of while loop
        
        
        sum_energy_mix = 0.;
            
        for ( i=resj; i>=0; i--)
        {
            rhow[i] = EvapUtilities.den_h2o(wt[i]);
            cp[i] = EvapUtilities.cp_h2o(wt[i]);
            sum_energy_mix = sum_energy_mix + 
                             wt[i]*cp[i]*rhow[i]*zvol[i];
        }
        wt_mix = wt[resj];


        double changeEng = sum_energy_diff- sum_energy_in;
        double engBalance = changeEng-fi_check;
        double efficiency = engBalance/fi_check*100.;
        
        String dateTimeStr = currentTime.date(4) + "    "
                            + " " + currentTime.getTime(false);
        rma.util.NumberFormat nfi_4 = new rma.util.NumberFormat("%4d");
        rma.util.NumberFormat nf9_2 = new rma.util.NumberFormat("%9.2f");
        rma.util.NumberFormat nfe14_8 = new rma.util.NumberFormat("%14.8E");
        String vals1 = nfi_4.form(sml+1) + " " + nfi_4.form(resj+1) + " "
                + nf9_2.form(wsel) + " ";
        String vals2 = nfe14_8.form(sum_energy_in) + " "
                + nfe14_8.form(sum_energy_diff) + " "
                + nfe14_8.form(fi_check) + " "
                + nfe14_8.form(changeEng) + " "
                + nfe14_8.form(engBalance) + " "
                + nfe14_8.form(efficiency) + " "
                + nfe14_8.form(zarea[resj]) + " ";
        
        
        if ( _tout != null )
        {
            try
            {
                    _tout.write(dateTimeStr+vals1+vals2); _tout.newLine();
            }
            catch ( IOException ioe )
            {
            	
            	
            	String msg =  dateTimeStr + "\n";
            	msg += "resj " + resj + "\n";
            	msg += "sum_energy_in " + sum_energy_in + "\n";
            	msg += "sum_energy_diff " + sum_energy_diff + "\n";
            	msg += "fi_check " + fi_check + "\n";
            	msg += "changeEng " + changeEng + "\n";
            	msg += "engBalance " + engBalance + "\n";
            	msg += "efficiency " + efficiency + "\n";
            	msg += "zarea[resj] " + zarea[resj] + "\n";
            	
                for ( i=resj; i>=0; i--)
                {            	
                	msg += " i, wt[i], zvol[i] " + wt[i] + "  " + zvol[i]+ "\n";
                }
            	Logger logger = Logger.getLogger(ResWtCompute.class.getName());
            	logger.log(Level.SEVERE, msg, ioe);
            	
            	return false;
            }
        }

        
        return true;
    }
    
    
    protected double zcom(int itop,int ibottom,
            double xrhow)
    {
        double total,totalpvol,zrhow, zout;
        
        double[] rhow = _reservoir._rhow;
        
        total = 0.;
        totalpvol = 0.;
        double[] zvol = _reservoir._zvol;
        double[] ztop = _reservoir._ztop;
        
        for ( int j=ibottom; j<=itop; j++ ) 
        {
            
            if(xrhow == -1.)
            {
                zrhow = rhow[j];
            }
            else
            {
                zrhow = xrhow;
            }

            if (j > 0)
            {
                total = total + zrhow*zvol[j]*
                        (ztop[j]+ztop[j-1])/2.;
            }
            else
            {
                total = total + zrhow*zvol[j]*
                        (ztop[j]+0.)/2.;
            }
            totalpvol = totalpvol + zrhow*zvol[j];                 
        }
		if(totalpvol != 0.0)
		{
			zout = total/totalpvol;
			return zout;
		}
        else
		{
			return 0.0;
		}
    }
        
        
        
    // One vector of workspace, gam is needed.
    final int NMAX = 500;
    double[] gam = new double[NMAX]; 
    /**
     * Solves for a vector u(1:n) of length n the tridiagonal linear set given by equation (2.4.1).
     * a(1:n), b(1:n), c(1:n), and r(1:n) are input vectors and are not modified.
     * Parameter: NMAX is the maximum expected value of n.
     */
    private boolean tridag( double[] a, double[] b, double[] c,
            double[] r, double[] u, int n)
    {
        double bet;
        if ( b[0] == 0. )
        {
            //TODO error message system (throw exception?)
			Logger.getLogger(ResWtCompute.class.getName()).log(Level.SEVERE, "tridag: rewrite equations");
            return false;
        }
        // If this happens then you should rewrite your equations as a set of order N - 1, with u2
        // trivially eliminated.
        
        bet=b[0];
        u[0]=r[0]/bet;      
        
        for ( int j=1; j<n; j++)    // Decomposition and forward substitution.
        {
            gam[j]=c[j-1]/bet;
            bet=b[j]-a[j]*gam[j];
            if ( bet == 0.0 )
            {
                //TODO error message system (throw exception?)
                Logger.getLogger(ResWtCompute.class.getName()).log(Level.SEVERE, " tridag failed");  // !Algorithm fails; see below.
                return false;             
            }
            u[j]=(r[j]-a[j]*u[j-1])/bet;
        }
        
        for ( int j=n-2; j>=0; j--)    // Backsubstitution.
        {
            u[j] = u[j]-gam[j+1]*u[j+1];
        }
        
        return true;
    }


    
    
}
