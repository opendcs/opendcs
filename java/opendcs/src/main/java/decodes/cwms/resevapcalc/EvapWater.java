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
 * @author RESEVAP program by Steven F. Daly  (ERDC/CRREL)
 * conversion to Java by Richard Rachiele (RMA)
 * OpenDCS implementation by Oskar Hurst (HEC)
 * <p>
 * Class used to compute and hold evaporation rate, latent and sensible heat
 * fluxes.
 */
final public class EvapWater
    {
    //these variables have package access
    double evap;
    double hs;
    double hl;
    double ustar;
    double tstar;
    double pstar;
    double qstar;
    double rstar;
    double obukhovLen;

    /**
     * This function iteratively
     * computes the surface stress (momentum flux) and the sensible and
     * latent heat fluxes over open reservoirs.
     * It requires no surface temperature but estimates that from the
     * bootstrap algorithm.
     * For the drag coefficient, the routine uses Donelan's (1982), with a
     * smooth-flow parameterization for light winds.  For the scalar
     * roughness lengths, the program uses the COARE algorithm (Fairall et al.,
     * 1996).  The wind speed also includes the COARE gustiness factor in
     * unstable stratification and the windless coefficient of Jordan et al.
     * (1999) in stable stratification.
     *
     * @param surfaceTemp - surface water temp (deg C)
     * @param airTemp     - air temp (deg C)
     * @param relHumidity - relative humidity (%)
     * @param windspeed   - wind speed (m/s)
     * @param airPressure - air pressure (mb)
     * @param rt          - height of temperature observation
     * @param ru          - height of wind observation
     * @param rq          - height of humidity observation
     */
    public void evap_water(double surfaceTemp,
                           double airTemp, double relHumidity,
                           double windspeed, double airPressure,
                           double rt, double ru, double rq)
        {
        // S is salinity of the surface water
        // IFLAG means to compute vapor variables for water saturation.
        // R10 is the standard reference height, 10 m.
        // ZI is a guess as to the inversion height.
        // ICE = 1 means the surface is presumed to be open water.
        // CONV is the convergence limit for iterations on USTAR, TSTAR,
        //           and QSTAR.
        // ICE = 1 means the surface is presumed to be open water.
        // ERR_LIM is an input data error code.

        double ustar, rstar, qstar, tstar;

        final double s = 0.0;
        int iflag = 1;

        final double r10 = 10.;
        final double zi = 600.0;
        final double conv = 0.001;
        final int ice = 1;
        final double err_lim = -900.;

        double thetar, del_theta, tave, rhs, dum, f, dum1, rhoa;
        double cd, ch, ce, zq, zt, z0, cdnr, gnu, qave, del_q;
        double qs, qr;
        double ustar_old, qstar_old, u10, cdn10, wind, testu, testt, testq, cp_air;
        double tau, elevation, tstar_old, g;
        double lv;
        int kode, its;

        // ICLD is cloud type.  2 means cirrus high clouds, 3 means
        // altocumulus middle clouds, and 3 means stratus low clouds.
        // int[] icld = { 2, 3, 4 };
        // Note.  This is now set in class CloudCover

        // copy to local names
        double ur = windspeed;
        double tr = airTemp;
        double rh = relHumidity;
        double p = airPressure;
        double ts = surfaceTemp;

        // check for missing data.
        if (ur < err_lim || tr < err_lim || rh < err_lim
                || p < err_lim)
            {
            return;
            }

        // Compute potential temperature.
        thetar = tr + (Constants.CONST_G / EvapUtilities.computeSpecHeatAir(tr) * rt);

        // Do not want subfreezing surface temperature in a model that assumes open water
        if (ts < 0.0)
            {
            ts = 0.0;
            }

        //   Compute necessary meteorological variables.

        // Prevent UR = 0.0
        if (ur < 0.10) ur = 0.10;    // prevent ur = 0.0
        del_theta = ts - thetar;    // temperature difference
        tave = 0.5 * (ts + tr);        // layer-averaged temperature

        rhs = 1.0;            // surface at saturation
        EvapUtilities.DoubleContainer rhoAdc = new EvapUtilities.DoubleContainer();
        EvapUtilities.DoubleContainer qsdc = new EvapUtilities.DoubleContainer();
        EvapUtilities.DoubleContainer qrdc = new EvapUtilities.DoubleContainer();
        EvapUtilities.DoubleContainer dumdc = new EvapUtilities.DoubleContainer();
        EvapUtilities.DoubleContainer dum1dc = new EvapUtilities.DoubleContainer();

        // returns density of moist air at surface (rhoa) and specific
        // humidity at surface (qs).
        EvapUtilities.findDensityFromRh(rhs, p, ts, s, rhoAdc, dumdc, qsdc, iflag);
        qs = qsdc.d;
        rhoa = rhoAdc.d;

        f = rh / 100.0;            // fractional relative humidity
        // returns specific humidity at height rq.
        EvapUtilities.findDensityFromRh(f, p, tr, s, dumdc, dum1dc, qrdc, iflag);
        qr = qrdc.d;
        del_q = qs - qr;        // humidity difference
        qave = 0.5 * (qs + qr);        // layer-averaged q

        gnu = EvapUtilities.nu(ts);     // computes kinematic viscosity

        // first iteration.
        its = 1;            // count iterations
        cdnr = EvapUtilities.cdd(ur);    // first estimate of drag coefficient
        ustar = ur * Math.sqrt(cdnr);

        if (ustar < 0.01) ustar = 0.01;  // andreas added to insure stability

        //   first estimate of momentum roughness length.
        z0 = EvapUtilities.roughness(ru, cdnr) + EvapUtilities.smooth(ustar, gnu);


        EvapUtilities.DoubleContainer ztdc = new EvapUtilities.DoubleContainer();
        EvapUtilities.DoubleContainer zqdc = new EvapUtilities.DoubleContainer();

        rstar = (ustar * z0) / gnu;        // roughness reynolds number

        // returns roughness lengths for temperature (zt) and humidity (zq). 
        EvapUtilities.coare(rstar, gnu, ustar, ztdc, zqdc);
        zt = ztdc.d;
        zq = zqdc.d;

        EvapUtilities.DoubleContainer cddc = new EvapUtilities.DoubleContainer();
        EvapUtilities.DoubleContainer cedc = new EvapUtilities.DoubleContainer();
        EvapUtilities.DoubleContainer chdc = new EvapUtilities.DoubleContainer();

        kode = 0;        // identifies neutral stability
        dum = 0.;
        // returns first estimates of cd, ch, ce.
        EvapUtilities.calcBulkCoefs(z0, zt, zq, dum, ru, rt, rq,
                cddc, chdc, cedc, kode);

        cd = cddc.d;
        ch = chdc.d;
        ce = cedc.d;

        // make first estimates of flux scales.
        ustar = ur * Math.sqrt(cd);

        if (ustar < 0.01) ustar = 0.01;     // andreas added to insure stability

        tstar = -(ch * ur * del_theta) / ustar;
        qstar = -(ce * ur * del_q) / ustar;

        //  compute obukhov length.
        double l = EvapUtilities.obukhov(ustar, tstar, qstar, tave, qave);
        kode = 2;        // future values use stratification


        // **** loop to continue iterations.  ****************************
        int maxiter = 20;
        for (its = 2; its <= maxiter; its++)
            {

            // save previous values for comparison.
            ustar_old = ustar;
            tstar_old = tstar;
            qstar_old = qstar;

            EvapUtilities.calcBulkCoefs(z0, zt, zq, l, r10, rt, rq,
                    cddc, chdc, cedc, kode);

            cd = cddc.d;
            ch = chdc.d;
            ce = cedc.d;

            // do this just to get stability-corrected cd at 10 m.
            u10 = ustar / Math.sqrt(cd);
            // need u10 to use in donelan's (1982) relation for cdn10.
            cdn10 = EvapUtilities.cdd(u10);


            // make new estimate of z0.
            z0 = EvapUtilities.roughness(r10, cdn10) + EvapUtilities.smooth(ustar, gnu);
            rstar = (ustar * z0) / gnu;    // roughness reynolds number

            // provides new values of zt and zq.
            EvapUtilities.coare(rstar, gnu, ustar, ztdc, zqdc);
            zt = ztdc.d;
            zq = zqdc.d;

            // provides new values of cd, ch, and ce.
            EvapUtilities.calcBulkCoefs(z0, zt, zq, l, ru, rt, rq, cddc, chdc, cedc, kode);
            cd = cddc.d;
            ch = chdc.d;
            ce = cedc.d;

            wind = EvapUtilities.speed(ur, ustar, zi, l);     //speed modified for gustiness
            ustar = wind * Math.sqrt(cd);


            if (ustar < 0.01) ustar = 0.01;     // andreas added to insure stability

            tstar = -(ch * wind * del_theta) / ustar;
            qstar = -(ce * wind * del_q) / ustar;
            l = EvapUtilities.obukhov(ustar, tstar, qstar, tave, qave);

            // test for convergence.
            testu = Math.abs((ustar - ustar_old) / ustar);

            // avoid dividing by zero
            if (tstar != 0.)
                {
                testt = Math.abs((tstar - tstar_old) / tstar);
                } else
                {
                testt = Math.abs(tstar - tstar_old);
                }

            // avoid dividing by zero
            if (qstar != 0.)
                {
                testq = Math.abs((qstar - qstar_old) / qstar);
                } else
                {
                testq = Math.abs(qstar - qstar_old);
                }

            // quit if all parameters converged.
            if (testu < conv && testt < conv && testq < conv)
                {
                break;
                }
            }
        // **** end iteration loop  ****************************

        // compute the fluxes.
        cp_air = EvapUtilities.computeSpecHeatAir(ts);  // specific heat of air
        lv = EvapUtilities.latent(ts);        // latent heat of vaporization

        // returns all the fluxes.  
        EvapUtilities.DoubleContainer taudc = new EvapUtilities.DoubleContainer();
        EvapUtilities.DoubleContainer hsdc = new EvapUtilities.DoubleContainer();
        EvapUtilities.DoubleContainer hldc = new EvapUtilities.DoubleContainer();
        EvapUtilities.DoubleContainer evapdc = new EvapUtilities.DoubleContainer();

        EvapUtilities.fluxes(ustar, tstar, qstar, rhoa, cp_air, lv,
                taudc, hsdc, hldc, evapdc);

        // store values in global variables.
        tau = taudc.d;
        hl = hldc.d;
        hs = hsdc.d;
        evap = evapdc.d;

        this.ustar = ustar;
        this.qstar = qstar;
        this.rstar = rstar;
        obukhovLen = l;

        }
    }
