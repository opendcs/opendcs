/*
 * Where Applicable, Copyright 2024 The OpenDCS Consortium or it's contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package decodes.cwms.resevapcalc;

import org.opendcs.units.Constants;
/**
 * @author RESEVAP program by Steven F. Daly (ERDC/CRREL)
 * conversion to Java by Richard Rachiele (RMA)
 * OpenDCS implementation by Oskar Hurst (HEC)
 */

/**
 * EvapUtilities contains static methods for computing evaporation related values.
 */

public class EvapUtilities
    {

    private EvapUtilities()
        {
        // Prevent instantiation
        throw new IllegalStateException("EvapUtilities class cannot be instantiated");
        }

    /**
     * Computes  the kinematic viscosity of air in m**2/s.
     *
     * @param airTemp - air temperature in degrees Celsius.
     * @return -  kinematic viscosity of air in m**2/s.
     */
    public static double nu(double airTemp)
        {
        double nuval = 1.326E-5 * (1.0 + airTemp * (6.542E-3 + airTemp * (8.301E-6 - 4.840E-9 * airTemp)));

        return nuval;
        }

    /**
     * Computes the specific heat of dry air in J/kg/K
     * for air temperature airTemp in degrees C between -40 and +40 C
     *
     * @param airTemp - air temperature in degrees Celsius.
     * @return
     */
    public static double computeSpecHeatAir(double airTemp)
        {
        double spcHeat = 1005.60 + airTemp * (0.017211 + 0.000392 * airTemp);

        return spcHeat;
        }

    /**
     * Computes computes the latent heat of vaporization or
     * sublimation in J/kg.
     *
     * @param airTemp - air temperature in degrees Celsius.
     * @return
     */
    public static double latent(double airTemp)
        {
        //  Sublimation if T is below 0 deg C.
        if (airTemp < 0.0)
            {
            return (28.34 - 0.00149 * airTemp) * 1.0E+5;
            }

        //   Vaporization if T is at or above 0 deg C.
        return (25.00 - 0.02274 * airTemp) * 1.0E+5;
        }

    /**
     * This method finds the air density and various water vapor
     *  quantities.
     *
     * IFLAG, which tells how to evaluate the saturation vapor pressure, ESAT:
     *  If IFLAG = 0, compute ESAT based on TC.
     *  If IFLAG = 1, compute ESAT for saturation over water.
     *  If IFLAG = 2, compute ESAT for saturation over ice.
     *
     * @param relH - (Input) the fractional relative humidity.
     * @param baroPres - (Input) the barometric pressure in mb.
     * @param tempC - (Input) the temperature in degrees Celcius.
     * @param sal - (Input) the salinity in psu.
     * @param rhoAdc - (Output) the density of moist air in kg/m**3.
     * @param rhoVdc - (Output) the water vapor density in kg/m**3.
     * @param q - (Output) the specific humidity in kg/kg.
     * @param iflag - (Input) method to evaluate the saturation vapor pressure, ESAT:
     */
    public static void findDensityFromRh(double relH, double baroPres,
                                         double tempC, double sal, DoubleContainer rhoAdc,
                                         DoubleContainer rhoVdc, DoubleContainer q, int iflag)
        {
        // convert to Kelvin
        double t = tempC + Constants.tK;

        // Density of dry air in kg/m**3
        double rhoD = 1.2923 * (Constants.tK / (tempC + Constants.tK)) * (baroPres / Constants.p0);

        // This computes saturation vapor pressure ESAT in hPa
        double eSat = satVpr(baroPres, tempC, iflag);

        double e = relH * eSat;     // Actual vapor pressure
        double fac = 1.0 - 0.000537 * sal;  //Salinity depression

        // Ideal gas law
        double rhoV = (100.0 * e * Constants.mw * fac) / (Constants.rgas * t);

        // Density of moist air
        double rhoA = rhoD + rhoV;

        // Specific humidity in kg/kg
        q.d = rhoV / rhoA;

        rhoAdc.d = rhoA;
        rhoVdc.d = rhoV;

        return;
        }

    /**
     * Compute saturation vapor pressure
     *
     *  uses Buck's (1981) equation to find the saturation vapor
     *  pressure as temperature T and atmospheric pressure P.  The equation is
     *	E = (PA*P + PB)*E0*DEXP((A*T)/(B + T)
     *
     *  where P and E are in mb, and T is in degrees C.
     *  IFLAG can suppress a calculation of E based on temperature.
     *      If IFLAG = 0, compute E based on T.
     *      If IFLAG = 1, compute saturation over water.
     *      If IFLAG = 2, compute saturation over ice.
     *
     * @param p -  atmospheric pressure (mb)
     * @param t - temperature in degrees Celcius.
     * @param iflag - method to evaluate the saturation vapor pressure, ESAT:
     *
     * @return - the saturation vapor pressure in hPa.
     */
    public static double satVpr(double p, double t, int iflag)
        {
        // In each array, the first entry is for saturation with respect to
        // water, the second for ice.       
        final double[] pa = {3.46E-6, 4.18E-6};
        final double[] pb = {1.0007, 1.0003};
        final double[] e0 = {6.1121, 6.1115};
        final double[] a = {17.502, 22.452};
        final double[] b = {240.97, 272.55};

        int k = 0;  // satureation over water

        // use temperature to determine if over ice or water
        if (iflag == 0)
            {
            // saturation over ice if true
            if (t < 0.0)
                {
                k = 1;
                }
            } else if (iflag == 2)
            {
            // choose saturation over ice 
            k = 1;
            }
        double eSat = (pa[k] * p + pb[k]) * e0[k] * Math.exp((a[k] * t) / (b[k] + t));

        return eSat;
        }

    /**
     * This function computes the roughness length for momentum, Z0, given the
     * neutral-stability drag coefficient CDNR appropriate at reference
     * height R.
     *
     * @param r
     * @param cdnr
     * @return
     */
    public static double roughness(double r, double cdnr)
        {
        return r * Math.exp(-Constants.CONST_K / Math.sqrt(cdnr));
        }

    /**
     * This function computes the neutral-stability drag coefficient
     * appropriate at height RU, given the roughness length Z0.
     *
     * @param ru
     * @param z0
     * @return
     */
    public static double cdnr(double ru, double z0)
        {
        double val = Math.log(ru / z0) / Constants.CONST_K;
        return Math.pow(val, -2.);
        }

    /**
     * Computes the momentum roughness length for an
     * aerodynamically smooth surface, Z0_SM.
     *
     * @param ustar -  is the friction velocity in m/s.
     * @param gnu  - is the kinematic viscosity in m**2/s.
     * @return
     */
    public static double smooth(double ustar, double gnu)
        {
        final double smoothCoef = 0.135;

        // This is basically the (constant) roughness Reynolds number of an
        // aerodynamically smooth surface.
        return smoothCoef * (gnu / ustar);
        }

    /**
     * This function returns the Obukhov length in meters.
     * USTAR, TSTAR, and QSTAR are flux scales, with QSTAR a scale for
     * specific humidity.
     *
     * @param ustar
     * @param tstar
     * @param qstar
     * @param tave  -  the average layer temperature in Celsius.
     * @param qave  -  the average layer specific humidity in kg/kg.
     *
     * @return
     */
    public static double obukhov(double ustar, double tstar, double qstar,
                                 double tave, double qave)
        {
        // This is basically the (constant) roughness Reynolds number of an
        // aerodynamically smooth surface.

        return (((tave + Constants.tK) * (ustar * ustar)) / (Constants.CONST_K * Constants.CONST_G)) /
                (tstar + ((0.61 * (tave + Constants.tK) * qstar) / (1.0 + (0.61 * qave))));
        }

    /**
     * This function computes the stability correction for the wind speed
     * profile
     *
     * @param zu   - is the height of the wind speed observation.
     * @param obukhovLen - is the Obukhov length.
     * @return
     */
    public static double computePsiM(double zu, double obukhovLen)
        {
        // Stability parameter
        double zeta = zu / obukhovLen;

        if (zeta < 0.0)
            {
            //  Unstable stratification (Paulson, 1970).
            double x = Math.pow((1. - 16.0 * zeta), .25);
            return 2.0 * Math.log(0.5 * (1.0 + x)) + Math.log(0.5 * (1 + x * x))
                    - 2.0 * Math.atan(x) + 1.570796;
            } else if (zeta == 0.0)
            {
            //  Neutral stratification.
            return 0.0;
            } else
            {
            // Stable stratification (Dutch formulation).
            double term1 = 0.0;
            // This test prevents underfloe errors.
            if (zeta <= 250.0)
                {
                term1 = Math.exp(-0.35 * zeta);
                }
            return -(0.7 * zeta + 0.75 * (zeta - 14.3) * term1 + 10.7);
            }

        }


    /**
     * This function computes the stability correction for either the
     * temperature or humidity profile.
     *
     * @param zs   - is the height of either the temperature or humidity  observation.
     * @param obukhovLen - is the Obukhov length.
     * @return
     */
    public static double computePsiHumidity(double zs, double obukhovLen)
        {
        // Stability parameter
        double zeta = zs / obukhovLen;

        if (zeta < 0.0)
            {
            //  Unstable stratification (Paulson, 1970).
            double x = Math.pow((1. - 16.0 * zeta), .25);
            return 2.0 * Math.log(0.5 * (1 + x * x));
            } else if (zeta == 0.0)
            {
            //  Neutral stratification.
            return 0.0;
            } else
            {
            // Stable stratification (Dutch formulation).
            double term1 = 0.0;
            // This test prevents underfloe errors.
            if (zeta <= 250.0)
                {
                term1 = Math.exp(-0.35 * zeta);
                }
            return -(0.7 * zeta + 0.75 * (zeta - 14.3) * term1 + 10.7);
            }

        }


    /**
     * This subroutine computes the roughness lengths for temperature (ZT)
     * and humidity (ZQ) from the roughness Reynolds number (RSTAR) using
     * the COARE algorithm (Fairall et al., 1996) but without the cool-skin
     * and warm-layer parameterizations.  That is, this is basically the
     * parameterization for ZT and ZQ from Liu et al. (1979).
     *
     *
     * @param rstar - roughness Reynolds number 
     * @param gnu   - is the kinematic viscosity of air in m**2/s.
     * @param ustar -  is the friction velocity in m/s.
     * @param zt
     * @param zq
     * @return
     */
    public static void coare(double rstar, double gnu, double ustar,
                             DoubleContainer zt, DoubleContainer zq)
        {
        final double[] rs = {0.135, 0.16, 1.00, 3.00, 10.0, 30.0, 100.0, 300.0, 1000.0};
        final double[] at = {0.177, 1.376, 1.376, 1.026, 1.625, 4.661, 34.904, 1667.19, 5.88E5};
        final double[] bt = {0.0, 0.929, 0.929, -0.599, -1.018, -1.475, -2.067, -2.907, -3.935};
        final double[] aq = {0.292, 1.808, 1.808, 1.393, 1.956, 4.994, 30.709, 1448.68, 2.98E5};
        final double[] bq = {0.0, 0.826, 0.826, -0.528, -0.870, -1.297, -1.845, -2.682, -3.616};

        double gu = gnu / ustar;  // Viscous length scale

        int idx = 8;
        for (int i = 0; i < 9; i++)
            {
            if (rstar < rs[i])
                {
                idx = i;
                break;
                }
            }

        //  Compute ZT and ZQ in RSTAR range identified by i
        zt.d = gu * at[idx] * (Math.pow(rstar, bt[idx]));
        zq.d = gu * aq[idx] * (Math.pow(rstar, bq[idx]));
        }

    /**
     * This subroutine computes the momentum roughness over snow (Z0) based
     * on observations on Ice Station Weddell.  It recognizes three regimes:
     * An aerodynamically smooth regime, where Z0 goes as GNU/USTAR; a
     * drifting-snow regime, where Z) goes as (USTAR**2)/G; and a
     * statistically determined intermediate regime that allows increased
     * roughness when the flow is transvers to the sastrugi.
     *
     * @param ustar
     * @param gnu
     * @return
     */
    public static double drift_snow(double ustar, double gnu)
        {
        double z0_smooth = smooth(ustar, gnu);     // Aerodynamically smooth part
        double z0_drift = (0.065 / Constants.CONST_G) * (ustar * ustar);     //Drifting snow part
        double utmp = ((ustar - 0.25) / 0.052);
        double z0_stat = 1.13E-3 * Math.exp(-(utmp * utmp));   //Statistical intermediate partt

        // Overall roughness length
        double z0 = z0_smooth + z0_stat + z0_drift;
        return z0;
        }

    /**
     * Computes the scalar roughnesses for temperature
     * (ZT) and humidity (ZQ) over now using the Andreas (1987)
     * theoretical model.
     *
     * All units are mks.
     * The predictive equation is
     * 	  ln(ZS/Z0)  =  B0  +  B1*ln(RSTAR)  +  B2*(ln(RSTAR)**2)
     *
     * @param ustar  - the friction velocity
     * @param z0     - the momentum roughness length.
     * @param rstar  - is the roughness Reynolds number.
     * @param zt     - (Output) scalar roughnesses for temperature
     * @param zq     - (Output) scalar roughnesses for humidity
     */
    public static void andreas_87(double ustar, double z0, double rstar,
                                  DoubleContainer zt, DoubleContainer zq)
        {
        final double[] rs = {0.135, 2.5, 1000.};

        final double[][] bt =
                {{1.250, 0.0, 0.0},
                        {0.149, -0.550, 0.0},
                        {0.317, -0.565, -0.183}};
        final double[][] bq =
                {{1.610, 0.0, 0.0},
                        {0.351, -0.628, 0.0},
                        {0.396, -0.512, -0.180}};

        // find rstar range
        int ind_rs = 2;
        for (int i = 0; i < 3; i++)
            {
            if (rstar <= rs[i])
                {
                ind_rs = i;
                break;
                }
            }

//TODO messange below ...        
//C	  If exit to this statement, RSTAR is beyond range of the model.
//      PRINT 700, RSTAR
//700   FORMAT(/5X,'R* = ',1PF7.1,' is beyond the snow model''s limit',
//     & ' of 1000.')
//      PAUSE 'Type CONTINUE to continue.'        

        double lnrstar = Math.log(rstar);

        // !Temperature roughness
        double lnzsz0 = bt[ind_rs][0] + bt[ind_rs][1] * lnrstar
                + bt[ind_rs][2] * (lnrstar * lnrstar);
        zt.d = z0 * Math.exp(lnzsz0);

        //   Next, for humidity.
        lnzsz0 = bq[ind_rs][0] + bq[ind_rs][1] * lnrstar
                + bq[ind_rs][2] * (lnrstar * lnrstar);
        zq.d = z0 * Math.exp(lnzsz0);

        }

    /**
     * Calculates the stability-corrected transfer coefficients
     * for momentum (CD), sensible heat (CH), and latent heat (CE) appropriate
     * for reference heights ZU, ZT, and ZQ, respectively.
     * The input variables are the Obukhov length (L) and the roughness
     * lengths for momentum (Z0), temperature (ZT), and humidity (ZQ).
     * KODE = 0 if desire calculations for neutral stability.
     *
     * @param z0  - reference height for momentum
     * @param zt  - reference height for sensible heat
     * @param zq  - reference height for latent heat
     * @param oblen   - the Obukhov length
     * @param ru
     * @param rt
     * @param rq
     * @param cd     - (Output) transfer coefficient for momentum
     * @param ch     - (Output) transfer coefficient for sensible heat
     * @param ce     - (Output) transfer coefficient for latent heat
     * @param kode
     */
    public static void calcBulkCoefs(double z0, double zt, double zq,
                                     double oblen, double ru, double rt, double rq, DoubleContainer cd,
                                     DoubleContainer ch, DoubleContainer ce, int kode)
        {
        double psim, psiht, psihq;

        if (kode == 0)
            {
            psim = 0.0;
            psiht = 0.0;
            psihq = 0.0;
            } else
            {
            if (1 == 0.0)
                {
//TODO       PAUSE '  Obukhov length L is specified as zero.  Wrong!'              
                }

            //  Normal processing for both stable and unstable stratification.           
            psim = computePsiM(ru, oblen);     // Computes phim
            psiht = computePsiHumidity(rt, oblen);    // Computes phih for temperature
            psihq = computePsiHumidity(rq, oblen);    // Computes phih for humidity
            }

        //  Compute transfer coefficients.
        cd.d = c(ru, ru, z0, z0, psim, psim);     // Drag coefficient
        ch.d = c(ru, rt, z0, zt, psim, psiht);    // Transfer coef. for sensible heat
        ce.d = c(ru, rq, z0, zq, psim, psihq);    // Transfer coef. for latent heat
        }

    public static double computeFischerUStar(double ur, double rhoAdc, double rhow)
        {
        double cd;
        if (ur <= 5)
            {
            cd = .001;
            } else if (ur >= 15)
            {
            cd = .0015;
            } else
            {
            cd = .001 + .0005 * ((ur - 5) / 10);
            }

        return Math.sqrt(cd * (rhoAdc / rhow) * ur * ur);
        }

    public static double computeDonelanUStar(double ur, double rhoAdc, double rhow)
        {
        double cd = (.37 + .137 * ur) / 1000;
        return Math.sqrt(cd * rhoAdc / rhow) * ur;
        }

    /**
     *  Compute transfer coefficients.
     *
     * @param z1
     * @param z2
     * @param z01
     * @param z02
     * @param psi1
     * @param psi2
     * @return
     */
    public static double c(double z1, double z2, double z01,
                           double z02, double psi1, double psi2)
        {
        double cval = (Constants.CONST_K * Constants.CONST_K) / ((Math.log(z1 / z01) - psi1)
                * (Math.log(z2 / z02) - psi2));

        return cval;
        }

    /**
     * This function computes a gustiness, G, to add to the wind speed (UR) in
     * light winds to maintain fluxes in nearly calm conditions.
     * We use the COARE gust factor BETA*WSTAR (Fairall et al., 1996) in
     * unstable conditions and the windless coefficient W0 of Jordan et al.
     * (1999) in stable conditions.
     *
     * @param ur     - wind speed
     * @param ustar  - the friction velocity.
     * @param zi     - the height of the inversion base.
     * @param l      - the Obukhov length.
     *
     * @return
     */
    public static double speed(double ur, double ustar,
                               double zi, double l)
        {
        //  W0 is the windless coefficient in m/s.
        final double w0 = 0.5;
        // BETA is an empirical coefficient in the COARE gustiness relation.
        final double beta = 1.25;

        if (Math.abs(l) >= 1000.0)
            {
            //  Otherwise, neutral stability for practical purposes.
            return ur;
            }

        // Check stratification.
        if (l < 0.0)
            {
            // COARE algorithm's gustiness correction.
            double wstar = ustar * (Math.pow(-zi / (Constants.CONST_K * l), .333333));
            double spd = Math.sqrt((ur * ur) + (beta * wstar) * (beta * wstar));
            return spd;
            } else
            {
            // Windless coefficient.
            double spd = ur + w0;
            return spd;
            }
        }

    /**
     *  computes the surface fluxes of momentum (TAU) and
     *  sensible (HS) and latent heat (HL) and the evaporation rate (EVAP).
     *
     * @param ustar
     * @param tstar
     * @param qstar
     * @param rhoa
     * @param cp
     * @param lv
     * @param tau   - (Output) surface flux of momentum 
     * @param hs    - (Output) surface flux of sensible heat 
     * @param hl    - (Output) surface flux of latent heat 
     * @param evap  - (Output) evaporation rate (mm/day)
     */
    public static void fluxes(double ustar, double tstar, double qstar,
                              double rhoa, double cp, double lv, DoubleContainer tau,
                              DoubleContainer hs, DoubleContainer hl, DoubleContainer evap)
        {
        //   Density of pure water.
        final double rhow = 1000.;

        tau.d = rhoa * (ustar * ustar);       // Momentum flux
        hs.d = -rhoa * cp * ustar * tstar;  // Sensible heat flux
        hl.d = -rhoa * lv * ustar * qstar;  // Latent heat flux

        //   Compute evaporation rate in water equivalent.
        evap.d = hl.d / (lv * rhow);      // in m/s
        evap.d = 1000. * 3600. * 24. * evap.d;   // in mm/day
        }

    /**
     * Calculates freshwater density as a function of temperature
     * Taken from IE Manual Chapter 2
     *
     * @param tx
     * @return
     */
    public static double calcDensityH2o(double tx)
        {
        double den = 1000. - 0.019549 *
                Math.pow(Math.abs(tx - 4.0), 1.68);
        return den;
        }

    /**
     * Calculates freshwater heat capacity as a function of temperature
     * Taken from IE Manual Chapter 2
     *
     * @param px
     * @return
     */
    public static double calcHeatCapacityH2o(double px)
        {
        double tx;
        if (px < 0.0)
            {
            tx = 0.0;
            } else
            {
            tx = px;
            }
        double rx = 34.5 - tx;
        double cp = 4174.9 + 1.6659 * (Math.exp(rx / 10.6)
                + Math.exp(-1. * rx / 10.6));
        return cp;
        }

    public static double f_bo_star(double tempC, double baroPres,
                                   double sal, int iflag)
        {
        // TK is 0 degrees C in kelvins.
        // A's and B's are constants in the saturation vapor pressure
        // relations for water and ice, respectively.
        final double tK = 273.15;
        final double[] a = {17.502, 22.452};
        final double[] b = {240.97, 272.55};

        double lv = latent(tempC);      // Latent heat of vaporization
        double cp = computeSpecHeatAir(tempC); // Specific heat of air

        double f = 1.0;     // Fractional relative humidity

        // convert to Kelvin
        DoubleContainer rhoA = new DoubleContainer();
        DoubleContainer rhoVS = new DoubleContainer();
        DoubleContainer dum = new DoubleContainer();

        // This returns the density of moist air (RHOA) and saturation
        // vapor density (RHOVS).
        findDensityFromRh(f, baroPres, tempC, sal, rhoA, rhoVS, dum, iflag);

        double rhoD = rhoA.d + rhoVS.d;

        double bo_fac = a[iflag - 1] * b[iflag - 1] /
                Math.pow(b[iflag - 1] + tempC, 2.) - 1.0 / (tempC + tK);

        return (rhoD * cp) / (lv * rhoVS.d * bo_fac);
        }

    /**
     * 10-m, neutral-stability drag coefficient from Donelan (1982).
     *
     * @param x
     * @return
     */
    public static double cdd(double x)
        {
        return (0.37 + 0.137 * x) * 1.0E-3;
        }


    public static class DoubleContainer
        {
        double d;

        public DoubleContainer()
            {
            this.d = 0.;
            }

        public DoubleContainer(double d)
            {
            this.d = d;
            }
        }

    }
