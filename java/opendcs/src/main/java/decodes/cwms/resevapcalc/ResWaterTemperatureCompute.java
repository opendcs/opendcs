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

import java.io.BufferedWriter;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Arrays.fill;

/**
 * Class used to compute reservoir temperature profile
 */
final public class ResWaterTemperatureCompute
    {
    BufferedWriter tout = null;

    // reservoir layers, segments.
    EvapReservoir reservoir;
    //int resjOld = -1;

    // working arrays
    private double[] a;
    private double[] b;
    private double[] c;
    private double[] r;
    private double[] u;
    private double[] rhowOrig;
    private double[] cpOrig;
    private double[] wtTmp;
    private double[] peMixOut;

    // One vector of workspace, gam is needed.
    final static private int NMAX = 500;
    private double[] gam = new double[NMAX];

    public ResWaterTemperatureCompute(EvapReservoir reservoir)
        {
        this.reservoir = reservoir;

        // dimension working arrays
        a = new double[EvapReservoir.NLAYERS];
        b = new double[EvapReservoir.NLAYERS];
        c = new double[EvapReservoir.NLAYERS];
        r = new double[EvapReservoir.NLAYERS];
        u = new double[EvapReservoir.NLAYERS];
        rhowOrig = new double[EvapReservoir.NLAYERS];
        cpOrig = new double[EvapReservoir.NLAYERS];
        wtTmp = new double[EvapReservoir.NLAYERS];
        peMixOut = new double[EvapReservoir.NLAYERS];
        }

    /**
     * set output writer for diagnostic text output for reservoir
     * energy balance
     *
     * @param tout
     */
    public void setOutfile(BufferedWriter tout)
        {
        this.tout = tout;
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
    public boolean computeReservoirTemp(Date currentTime,
                                        MetComputation metComputation,
                                        double delT)
        {
        double avgRhow, totalVol, sFreq, kzx, surfAreaX;
        double fi, fiCheck, sumEnergyIn, smlDelz, sumEnergyDiff;
        double sumEnergyMix, delRhow, origWt1, origWt2;
        double al, abar, au, smlVol, zMix, zSml, zNext;
        double delZu, dhatZu, delZl, dhatZl;
        double topDepth, bottomDepth, tempDepth;
        double solarTot;
        int sml, smlTop, smlFlag, IFLAG;
        double TKE, wtin, wtout, wtMix, rhowMix;
        double origPE, newPE, w3, KEConv, Salinity;
        double peMix, uH2OStar, KEStir;

        // zero working arrays
        fill(a, 0.);
        fill(b, 0.);
        fill(c, 0.);
        fill(r, 0.);
        fill(u, 0.);
        fill(rhowOrig, 0.);
        fill(cpOrig, 0.);
        fill(wtTmp, 0.);
        fill(peMixOut, 0.);

        // Calculate the water density and heat capacity at each level. 
        // Determine average density of reservoir

        // global to local names
        double[] zd = reservoir.zd;
        double[] kz = reservoir.kz;
        double[] zarea = reservoir.zarea;
        double[] delz = reservoir.delz;
        double[] zvol = reservoir.zvol;
        double[] ztop = reservoir.ztop;

        double[] rhow = reservoir.rhow;
        double[] cp = reservoir.cp;
        double[] wt = reservoir.wt;

        WindShearMethod windShearMethod = reservoir.windShearMethod;
        double thermalDiffusivityCoefficient = reservoir.thermalDiffusivityCoefficient;

        double wsel = reservoir.getElevation();
        double surfArea = reservoir.surfArea;
        double grav = Constants.CONST_G;

        double SOLAR = metComputation.solar;
        double flxir = metComputation.flxir;
        double flxirOut = metComputation.flxirOut;
        double hs = metComputation.evapWater.hs;
        double hl = metComputation.evapWater.hl;
        double evap = metComputation.evapWater.evap;
        double ustar = metComputation.evapWater.ustar;

        double katten = reservoir.attenuationConst;

        double ur = metComputation.metData.windSpeedCurrent;
        double rh = metComputation.metData.relHumidityCurrent;
        double tr = metComputation.metData.airTempCurrent;
        double p = metComputation.metData.airPressureCurrent;

        double theta = ResEvap.THETA;
        double albedo = ResEvap.ALBEDO;
        double penfrac = ResEvap.PENFRAC;
        double etaConvective = ResEvap.ETA_CONVECTIVE;
        double windCritic = ResEvap.WIND_CRITIC;
        double etaStirring = ResEvap.ETA_STIRRING;

        int resjOld = reservoir.resjOld;
        int resj = reservoir.getResj();

        avgRhow = 0.0;
        totalVol = 0.0;
        sumEnergyIn = 0.0;
        TKE = 0.;

        // Reset water temperatures if wsel has changed enough to increase the number of layers
        if (resj != resjOld)
            {
            for (int j = 0; j < resjOld; j++)
                {
                wtTmp[j] = wt[j];
                }

            int i = resj;
            int j = resjOld;

            while (i >= 0 && j >= 0)
                {
                wt[i] = wtTmp[j];
                i = i - 1;
                j = j - 1;
                }
            if (resj > resjOld)
                {
                for (int k = i; k >= 0; k--)
                    {
                    wt[k] = wtTmp[0];
                    }
                }
            }


        for (int i = 0; i <= resj; i++)
            {
            // Prevent supercooling
            if (wt[i] < 0.) wt[i] = 0.;

            rhow[i] = EvapUtilities.calcDensityH2o(wt[i]);
            cp[i] = EvapUtilities.calcHeatCapacityH2o(wt[i]);
            avgRhow = avgRhow + rhow[i] * zvol[i];
            totalVol = totalVol + zvol[i];
            sumEnergyIn = sumEnergyIn + rhow[i] * cp[i] * zvol[i] * wt[i];
            }

        avgRhow = avgRhow / totalVol;

        // Calculate Stability Frequency and then Diffusion coefficient

        for (int i = resj; i >= 1; i--)  //TODO check
            {
            if ((rhow[i] - rhow[i - 1]) != 0.)
                {
                sFreq = grav / avgRhow * Math.abs(rhow[i] - rhow[i - 1]) /
                        (zd[i - 1] - zd[i]);
                } else
                {
                sFreq = 0.00007;
                }

            if (sFreq < 0.00007) sFreq = 0.00007;

            surfAreaX = surfArea;
            if (surfArea < 350.)
                {
                surfAreaX = surfArea;
                } else
                {
                surfAreaX = 350.;
                }

            kzx = .000817 * Math.pow(surfAreaX, 0.56) *
                    Math.pow((Math.abs(sFreq)), (-0.43)); // cm^2/s
            kzx = .0001 * kzx;                                // m^2/s
            kz[i] = thermalDiffusivityCoefficient * kzx * cp[i] * rhow[i]; // j/(s m C) factor to arbitrarly reduce diffusion
            }

        // Perform diffusion calculation
        // Develop the elements of the tridiagonal equation

        for (int i = 0; i <= resj; i++)
            {
            // Calculate areas
            if (i == 0)
                {
                au = zarea[i];
                abar = 0.5 * zarea[i];
                al = 0.0;
                } else
                {
                au = zarea[i];
                abar = 0.5 * (zarea[i] + zarea[i - 1]);
                al = zarea[i - 1];
                }

            if (i < resj)
                {
                delZu = 0.5 * (delz[i + 1] + delz[i]);
                dhatZu = (kz[i + 1] / (cp[i + 1] * rhow[i + 1]) * delz[i + 1] +
                        kz[i] / (cp[i] * rhow[i]) * delz[i]) / (delz[i + 1] + delz[i]);
                } else
                {
                dhatZu = 0.;
                delZu = 1.0;
                }

            if (i > 0)
                {
                delZl = 0.5 * (delz[i - 1] + delz[i]);
                dhatZl = (kz[i - 1] / (cp[i - 1] * rhow[i - 1]) * delz[i - 1] +
                        kz[i] / (cp[i] * rhow[i]) * delz[i]) / (delz[i - 1] + delz[i]);
                } else
                {
                dhatZl = 0.;
                delZl = 1.0;
                }
            if (i > 0)
                {
                a[i] = (-1. * delT / delz[i] * dhatZl / delZl * theta *
                        al / abar);
                } else
                {
                a[i] = 0.;
                }

            b[i] = (1. + delT / delz[i] * dhatZu / delZu * theta *
                    au / abar +
                    delT / delz[i] * dhatZl / delZl * theta *
                            al / abar);

            if (i < resj)
                {
                c[i] = (-1. * delT / delz[i] * dhatZu / delZu * theta * au / abar);
                } else
                {
                c[i] = 0.;
                }

            if (i == 0)
                {
                r[i] = wt[i] + delT / delz[i] * au / abar * (dhatZu / delZu *
                        (1. - theta) * (wt[i + 1] - wt[i]));
                }

            if (i > 0 && i < resj)
                {
                r[i] = wt[i] + delT / delz[i] * au / abar * (dhatZu / delZu *
                        (1. - theta) * (wt[i + 1] - wt[i]) -
                        dhatZl / delZl * al / abar *
                                (1. - theta) * (wt[i] - wt[i - 1]));
                }
            if (i == resj)
                {
                r[i] = wt[i] + delT / delz[i] * al / abar * (dhatZl / delZl *
                        (1. - theta) * (wt[i] - wt[i - 1]));
                }
            }

        // Estimate internal heat source due to shortwave radiation

        topDepth = 0.;
        solarTot = 0.;

        for (int i = resj; i > 1; i--)
            {
            bottomDepth = topDepth + delz[i];

            fi = SOLAR * penfrac * (1. - albedo) *
                    (Math.exp(-1. * katten * topDepth) * zarea[i] -
                            Math.exp(-1. * katten * bottomDepth) * zarea[i - 1]) / zvol[i];        //!delz[i]

            solarTot = solarTot + SOLAR * penfrac * (1. - albedo) *
                    (Math.exp(-1. * katten * topDepth) * zarea[i] -
                            Math.exp(-1. * katten * bottomDepth) * zarea[i - 1]) / zvol[i];        //!delz[i]
            r[i] = r[i] + fi * delT / (cp[i] * rhow[i]);

            topDepth = bottomDepth;
            }

        // Add shortwave, IR, sensible, and latent heat fluxes to top
        fi = (SOLAR * (1. - penfrac) * (1. - albedo) +
                flxir + flxirOut -
                hs - hl) * zarea[resj] / zvol[resj];

        solarTot = solarTot +
                SOLAR * (1. - penfrac) * (1. - albedo) * zarea[resj];
        r[resj] = r[resj] + fi * delT / (cp[resj] * rhow[resj]);

        // Calculate total flux for energy balance checking
        fiCheck = (SOLAR * (1. - albedo) +
                flxir + flxirOut -
                hs - hl) * delT;
        fiCheck = fiCheck * zarea[resj];

        // size of arrays are resj+1
        tridag(a, b, c, r, u, resj + 1);


        tempDepth = 0.25;
        sumEnergyDiff = 0.;

        for (int i = resj; i >= 0; i--)  //TODO check
            {
            wt[i] = u[i];
            rhow[i] = EvapUtilities.calcDensityH2o(wt[i]);
            rhowOrig[i] = rhow[i];
            cp[i] = EvapUtilities.calcHeatCapacityH2o(wt[i]);
            cpOrig[i] = cp[i];
            sumEnergyDiff = sumEnergyDiff +
                    wt[i] * cp[i] * rhow[i] * zvol[i];

            if (i > 0)
                {
                tempDepth = tempDepth + .5 * (delz[i] + delz[i - 1]);
                }
            }

        // Perform mixing due to Potential energy and wind mixing

        //  Update the water density and heat capacity at each level
//      smlFlag = 1
//      sml = resj
//      smlDelz = delz(resj)
//      smlVol  = zvol(resj)
//      i = resj
//      iend = 1

        EvapUtilities.DoubleContainer rhoAdc = new EvapUtilities.DoubleContainer();
        EvapUtilities.DoubleContainer rhoVdc = new EvapUtilities.DoubleContainer();
        EvapUtilities.DoubleContainer spHumdc = new EvapUtilities.DoubleContainer();

        smlFlag = 1; //TODO check these
        sml = resj;
        smlDelz = delz[resj];
        smlVol = zvol[resj];
        int i = resj;
        int iend = 1;

        while (i >= 2 && iend == 1)
            {
            rhow[i] = EvapUtilities.calcDensityH2o(wt[i]);
            cp[i] = EvapUtilities.calcHeatCapacityH2o(wt[i]);


            wtMix = (wt[i] * smlVol * cp[i] +
                    wt[i - 1] * zvol[i - 1] * cp[i - 1]) /
                    (smlVol * cp[i] + zvol[i - 1] * cp[i - 1]);
            rhowMix = EvapUtilities.calcDensityH2o(wtMix);

            // Calculate the distance to the center of mass (COM) for each of the layers
            // SML after mixing
            zMix = zcom(resj, sml - 1, rhowMix);

            // SML w/o mixing
            zSml = zcom(resj, sml, -1.);

            // Layer below SML to be mixed
            zNext = zcom(sml - 1, sml - 1, -1.);


            peMix = grav * (rhowMix * (smlVol + zvol[i - 1]) *
                    (zMix - ztop[i - 2]) -
                    (rhow[i] * smlVol * (zSml - ztop[i - 2]) +
                            rhow[i - 1] * zvol[i - 1] * (zNext - ztop[i - 2])));

            peMixOut[i] = peMix;

            // Density causes mixing
            if (peMix < 0.)
                {
                sml = i - 1;
                smlDelz = smlDelz + delz[i - 1];
                smlVol = smlVol + zvol[i - 1];
                for (int j = i - 1; j <= resj; j++)  //TODO check
                    {
                    wt[j] = wtMix;
                    }
                }

            //Density profile is stable
            else
                {
                // First time, estimate convective and wind stirring energy available
                //TKE = 0.;  // java wants this initialized

                if (smlFlag == 1)
                    {
                    smlFlag = 0;
                    origPE = 0.;
                    newPE = 0.;

                    for (int j = sml; j <= resj; j++)
                        {
                        origPE = origPE + rhowOrig[j] * delz[j] *
                                (ztop[j] + ztop[i - 1]) / 2.;
                        newPE = newPE + rhowOrig[j] * delz[j];
                        }

                    newPE = newPE * (ztop[resj] + ztop[sml - 1]) / 2.;
                    w3 = grav / (rhow[resj] * delT) * (origPE - newPE);
                    if (w3 < 0.) w3 = 0.;
                    KEConv = etaConvective * rhow[resj] * zarea[sml - 1] * w3 * delT;

                    // Calculate wind stirring
                    uH2OStar = 0.; //TODO init for msg
                    if (ur > windCritic)
                        {
                        IFLAG = 1;   // saturation over water
                        Salinity = 0.;

                        EvapUtilities.findDensityFromRh(rh, p, tr, Salinity,
                                rhoAdc, rhoVdc, spHumdc, IFLAG);

                        if (WindShearMethod.DONELAN.equals(windShearMethod))
                            {
                            uH2OStar = EvapUtilities.computeDonelanUStar(ur, rhoAdc.d, rhow[resj]);
                            } else
                            {
                            uH2OStar = EvapUtilities.computeFischerUStar(ur, rhoAdc.d, rhow[resj]);
                            }
                        KEStir = etaStirring * rhow[resj] *
                                zarea[resj] * Math.pow(uH2OStar, 3.) * delT;
                        } else
                        {
                        KEStir = 0.;
                        }

                    if (KEStir < 0.0)
                        {
                        //TODO throw execption
                        String msg = "KEStir < 0.0 " +
                                "\netaStirring =" + etaStirring +
                                "\nrhow(resj) =" + rhow[resj] +
                                "\nzarea(resj) =" + zarea[resj] +
                                "\nuH2OStar =" + uH2OStar +
                                "\ndelT =" + delT;
                        Logger.getLogger(ResWaterTemperatureCompute.class.getName()).log(Level.SEVERE, msg);

                        return false;
                        }

                    TKE = KEStir + KEConv;
                    }

                // Mix layers if sufficient energy

                if (TKE >= peMix)
                    {
                    sml = i - 1;
                    smlDelz = smlDelz + delz[i - 1];
                    smlVol = smlVol + zvol[i - 1];
                    for (int j = i - 1; j <= resj; j++)
                        {
                        wt[j] = wtMix;
                        }

                    TKE = TKE - peMix;
                    } else
                    {
                    // End calculations for this time step as not sufficient energy to mix
                    iend = 0;
                    }
                }

            // Move downward to next layer
            i = i - 1;


            }     // end of while loop


        sumEnergyMix = 0.;

        for (i = resj; i >= 0; i--)
            {
            rhow[i] = EvapUtilities.calcDensityH2o(wt[i]);
            cp[i] = EvapUtilities.calcHeatCapacityH2o(wt[i]);
            sumEnergyMix = sumEnergyMix +
                    wt[i] * cp[i] * rhow[i] * zvol[i];
            }
        wtMix = wt[resj];


        double changeEng = sumEnergyDiff - sumEnergyIn;
        double engBalance = changeEng - fiCheck;
        double efficiency = engBalance / fiCheck * 100.;


        return true;
        }

    private double zcom(int itop, int ibottom,
                          double xrhow)
        {
        double total, totalpvol, zrhow, zout;

        double[] rhow = reservoir.rhow;

        total = 0.;
        totalpvol = 0.;
        double[] zvol = reservoir.zvol;
        double[] ztop = reservoir.ztop;

        for (int j = ibottom; j <= itop; j++)
            {

            if (xrhow == -1.)
                {
                zrhow = rhow[j];
                } else
                {
                zrhow = xrhow;
                }

            if (j > 0)
                {
                total = total + zrhow * zvol[j] *
                        (ztop[j] + ztop[j - 1]) / 2.;
                } else
                {
                total = total + zrhow * zvol[j] *
                        (ztop[j] + 0.) / 2.;
                }
            totalpvol = totalpvol + zrhow * zvol[j];
            }
        if (totalpvol != 0.0)
            {
            zout = total / totalpvol;
            return zout;
            } else
            {
            return 0.0;
            }
        }

    /**
     * Solves for a vector u(1:n) of length n the tridiagonal linear set given by equation (2.4.1).
     * a(1:n), b(1:n), c(1:n), and r(1:n) are input vectors and are not modified.
     * Parameter: NMAX is the maximum expected value of n.
     */
    private boolean tridag(double[] a, double[] b, double[] c,
                           double[] r, double[] u, int n)
        {
        double bet;
        if (b[0] == 0.)
            {
            //TODO error message system (throw exception?)
            Logger.getLogger(ResWaterTemperatureCompute.class.getName()).log(Level.SEVERE, "tridag: rewrite equations");
            return false;
            }
        // If this happens then you should rewrite your equations as a set of order N - 1, with u2
        // trivially eliminated.

        bet = b[0];
        u[0] = r[0] / bet;

        for (int j = 1; j < n; j++)    // Decomposition and forward substitution.
            {
            gam[j] = c[j - 1] / bet;
            bet = b[j] - a[j] * gam[j];
            if (bet == 0.0)
                {
                //TODO error message system (throw exception?)
                Logger.getLogger(ResWaterTemperatureCompute.class.getName()).log(Level.SEVERE, " tridag failed");  // !Algorithm fails; see below.
                return false;
                }
            u[j] = (r[j] - a[j] * u[j - 1]) / bet;
            }

        for (int j = n - 2; j >= 0; j--)    // Backsubstitution.
            {
            u[j] = u[j] - gam[j + 1] * u[j + 1];
            }

        return true;
        }


    }
