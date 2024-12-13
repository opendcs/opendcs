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

import decodes.cwms.HecConstants;
import org.opendcs.units.Constants;

import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;

/**
 * @author RESEVAP program by Steven F. Daly (ERDC/CRREL)
 * conversion to Java by Richard Rachiele (RMA)
 * OpenDCS implementation by Oskar Hurst (HEC)
 */
public class SolarFlux {
    /* r1(4,4): = Cubic polynomial coefficients for Reflectivity
     *            for clear sky
     * r2(4,4): = Cubic relectivity coeffients for Reflectivity
     *            for cloudy skies.
     * t1(4,4): = Cubic polynomial coefficients for Transmissivty
     *            for clear skies
     * t2(4,4): = Cubic polynomial coefficients for Transmissivty
     *            for cloudy skies.
     * wtx(4,6): = Biquadratic polynomial coefficients for the Weights.
     */

    /*  rclr=a0 + a1*cosz + a2*cosz^2 + a3*cosz^3, where in the
     *  following array the rows are a0, a1, a2 and a3, and
     *  the columns correspond to
     *   1. Highest layer
     *   2. Middle layer
     *   3. Lowest layer
     *   4. Smoke/fog (Not used in this model)
     */
    private static final double[][] R1 =
            {
                    {.12395, .15325, .15946, .27436},
                    {-.34765, -.39620, -.42185, -.43132},
                    {.39478, .42095, .48800, .26920},
                    {-.14627, -.14200, -.18492, -.00447}
            };

    /* rcld=a0 + a1*cosz + a2*cosz^2 + a3*cosz^3, where in the
     * following array the rows are a0, a1, a2 and a3, and
     * the columns correspond to
     *  1. Thin Ci/Cs (denoted as cloud type 1)
     *  2. Thick Ci/Cs (denoted as cloud type 2)
     *  3. As/Ac (denoted as cloud type 3)
     *  4. Low cloud (denoted as cloud type 4 or 5)
     */
    private static final double[][] R2 =
            {
                    {.25674, .42111, .61394, .69143},
                    {-.18077, -.04002, -.01469, -.14419},
                    {-.21961, -.51833, -.17400, -.05100},
                    {.25272, .40540, .14215, .06682}
            };

    /*  tclr=b0 + b1*cosz + b2*cosz^2 + b3*cosz^3, where in the
     *  following array the rows are b0, b1, b2 and b3, and
     *  the columns correspond to
     *   1. Highest layer
     *   2. Middle layer
     *   3. Lowest layer
     *   4. Smoke/fog (Not used in this model)
     */
    private static final double[][] T1 =
            {
                    {.76977, .69318, .68679, .55336},
                    {.49407, .68227, .71012, .61511},
                    {-.44647, -.64289, -.71463, -.29816},
                    {.11558, .17910, .22339, -.06663}
            };

    /* tcld=b0 + b1*cosz + b2*cosz^2 + b3*cosz^3, where in the
     * following array the rows are b0, b1, b2 and b3, and
     * the columns correspond to
     *  1. Thin Ci/Cs (denoted as cloud type 1)
     *  2. Thick Ci/Cs (denoted as cloud type 2)
     *  3. As/Ac (denoted as cloud type 3)
     *  4. Low cloud (denoted as cloud type 4 or 5)
     */
    private static final double[][] T2 =
            {
                    {.63547, .43562, .23865, .15785},
                    {.35229, .26094, .20143, .32410},
                    {.08709, .36428, -.01183, -.14458},
                    {-.22902, -.38556, -.07892, .01457}
            };

    /* W=c0 + c1*cosz + c2*fk + c3*fk*cosz + c4*cosz^2 + c5*fk^2,
     * where in the following array the rows are co,c1,c2,c3,c4
     * and c5, and the columns correspond to
     *  1. Thin Ci/Cs (denoted as cloud type 1)
     *  2. Thick Ci/Cs (denoted as cloud type 2)
     *  3. As/Ac (denoted as cloud type 3)
     *  4. Low cloud (denoted as cloud type 4 or 5)
     */
    private static final double[][] WTX =
            {
                    {0.675, 1.552, 1.429, 1.512},
                    {-3.432, -1.957, -1.207, -1.176},
                    {1.929, -1.762, -2.008, -2.160},
                    {0.842, 2.067, 0.853, 1.420},
                    {2.693, 0.448, 0.324, -0.032},
                    {-1.354, 0.932, 1.582, 1.422}
            };


    //double gmt_offset;
    double diffuse;
    double direct;
    double sdown;

    double ZEN;
    double SOLAR;

    public void solflx(Date currentTime, double gmtOffset,
                       double longitude,
                       double latitude, CloudCover[] cloudCover) {
        EvapUtilities.DoubleContainer solzen = new EvapUtilities.DoubleContainer();
        EvapUtilities.DoubleContainer sdowndc = new EvapUtilities.DoubleContainer();
        EvapUtilities.DoubleContainer cosz = new EvapUtilities.DoubleContainer();
        double frad, dhr;
        double sdown, direct, diffuse;
        int doy, hr;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentTime);
        calendar.setTimeZone(TimeZone.getTimeZone("GMT"));
        // Local variables
        // Convert variables from res_common.f to local subroutine      
        doy = calendar.get(Calendar.DAY_OF_YEAR);
        hr = calendar.get(Calendar.HOUR_OF_DAY);

        frad = .017453292;

//        dhr = ((float)(hr))/100.0;
        dhr = hr;


        // Calculate the solar zenith angle
        if (longitude <= -180.) longitude += 360.0;
        if (longitude > 180.) longitude = longitude - 360.0;


        zenith(doy, dhr, gmtOffset,
                latitude, longitude, solzen, cosz);


        if (solzen.d >= 90.) {
            // still dark set flux to zero and return
            sdown = 0.0;
            direct = 0.0;
            diffuse = 0.0;
        } else {
            // Calculate the total, direct and diffuse flux on a horizontal
            // surface for each hour. In the future this should be
            // modified to account for a sloping surface.  The direct and diffuse
            // components are handled differently for sloping surfaces.
            int jday = doy;
            insol(jday, cloudCover, cosz.d);

            sdown = this.sdown;
        }

        SOLAR = sdown;
        ZEN = solzen.d;
    }

    /**
     * INSOL computes direct and diffuse solar radiation using
     * method of R. Shapiro
     *
     * @param jday
     * @param cloudCover
     * @param cosz
     */
    public void insol(int jday, CloudCover[] cloudCover,
                      double cosz) {
        double sdown0;
        double wgt, rcld, tcld, fr, tclr;
        double coszsq, coszcube, d3, d2, rg, d1, rclr, sdowne;
        double sdown, direct, diffuse;

        double[] rk = new double[3];
        double[] tk = new double[3];
        double[] tdk = new double[3];
        int[] icla = new int[3];
        double[] covera = new double[3];
        int j, ll;

        Objects.requireNonNull(cloudCover, "Input CloudCover cannot be null");

        // initialize sdown

        // calculate cosine of zenith angle

        // comput extra-terrestrial insolation
        // account for variation of sun/earth distance due to
        // eliptical orbit. Redone daily at 0500 hours.
        sdowne = 2. * Constants.PI * ((float) (jday - 2)) / 365.242;
        sdowne = (1.0001399 + 0.0167261 * Math.cos(sdowne));
        sdowne = sdowne * sdowne;

        // 
        coszsq = cosz * cosz;
        coszcube = coszsq * cosz;
        sdown0 = 1369.2 * sdowne * cosz;

        // Equate cloud array elements of Shapiro (high=1, midddle=2, low=3) with
        // those of CR_SMSPII (high=3, middle=2, low=1)

        covera[0] = cloudCover[0].fractionCloudCover;
        covera[1] = cloudCover[1].fractionCloudCover;
        covera[2] = cloudCover[2].fractionCloudCover;
        icla[0] = cloudCover[0].getCloudTypeFlag();
        icla[1] = cloudCover[1].getCloudTypeFlag();
        icla[2] = cloudCover[2].getCloudTypeFlag();

        // if missing cloud cover, get default values
        if (!HecConstants.isValidValue(covera[2])) {
            covera[2] = cloudCover[2].getDefaultFractionCloudCover();
        }
        if (!HecConstants.isValidValue(covera[1])) {
            covera[1] = cloudCover[1].getDefaultFractionCloudCover();
        }
        if (!HecConstants.isValidValue(covera[0])) {
            covera[0] = cloudCover[0].getDefaultFractionCloudCover();
        }

        for (int i = 0; i < 3; i++) {
            // Initialize parameters
            wgt = 0.0;
            rcld = 0.0;
            tcld = 0.0;
            rclr = 0.0;
            tclr = 0.0;

            int jj = icla[i];
            fr = covera[i];
            ll = i;

            if (jj != 0) {
                j = jj - 1;
                wgt = WTX[0][j] + WTX[1][j] * cosz + WTX[2][j] * fr + WTX[3][j] * cosz * fr
                        + WTX[4][j] * coszsq + WTX[5][j] * fr * fr;
                wgt = wgt * fr;

                if (fr < 0.05) wgt = 0.0;
                if (fr > 0.95) wgt = 1.0;

                if (wgt > 0.0) {
                    rcld = R2[0][j] + R2[1][j] * cosz + R2[2][j] * coszsq
                            + R2[3][j] * coszcube;
                    tcld = T2[0][j] + T2[1][j] * cosz + T2[2][j] * coszsq
                            + T2[3][j] * coszcube;
                }
            }

            // Compute reflectivity and transmitivity for each layer
            if (wgt < 1.) {
                rclr = R1[0][ll] + R1[1][ll] * cosz + R1[2][ll] * coszsq
                        + R1[3][ll] * coszcube;
                tclr = T1[0][ll] + T1[1][ll] * cosz + T1[2][ll] * coszsq
                        + T1[3][ll] * coszcube;
            }

            rk[i] = wgt * rcld + (1. - wgt) * rclr;
            tk[i] = wgt * tcld + (1. - wgt) * tclr;

            // Direct componenet of tk
            tdk[i] = tk[i] - rk[i];

            if (tdk[i] < 0.0) tdk[i] = 0.0;
        }

        // calculation of insolation at the ground-sdown
        rg = 0.2;       // set to the value of water
        d1 = 1.0 - (rk[0] * rk[1]);
        d2 = 1.0 - (rk[1] * rk[2]);
        d3 = 1.0 - (rk[2] * rg);

        sdown = d1 * d2 - (rk[0] * rk[2] * tk[1] * tk[1]);
        sdown = d3 * sdown - (d1 * rk[1] * rg * tk[2] * tk[2]);
        sdown = sdown - (rk[0] * rg * tk[1] * tk[1] * tk[2] * tk[2]);
        sdown = (tk[0] * tk[1] * tk[2] * sdown0) / sdown;
        if (sdown <= 0.0) {
            sdown = 0.0;
            direct = 0.0;
            diffuse = 0.0;
        } else {
            // Direct component of insolation 
            direct = tdk[0] * tdk[1] * tdk[2] * sdown0;

            //  Diffuse component of insolation
            diffuse = sdown - direct;
        }

        this.sdown = sdown;
        this.direct = direct;
        this.diffuse = diffuse;
    }


    /// public static void zenith ( int jjday, double local_hr,double gmt_offset,
    public static void zenith(int jjday, double local_hr,
                              double gmt_offset,
                              double zlat, double zlong,
                              EvapUtilities.DoubleContainer solzen, EvapUtilities.DoubleContainer coszd) {
        double gmt, phi, phir, sinp, cosp, cosz;
        double sin2p, cos2p, sig, sigr, sind, cosd, xm, hr, zlatr, sinz, saz, az;
        double xpi, h;

        // Calculate GMT
        gmt = local_hr + gmt_offset;


        double frad = .01745329252;
        xpi = 180. * frad;
        phi = 360. * ((float) (jjday - 1)) / 365.242;
        phir = phi * frad;

        sinp = Math.sin(phir);
        cosp = Math.cos(phir);
        sin2p = 2. * sinp * cosp;
        cos2p = 2. * cosp * cosp - 1.;
        sig = 279.9348 + phi + 1.914827 * sinp - 0.079525 * cosp +
                0.019938 * sin2p - 0.001639 * cos2p;

        sigr = sig * frad;

        // compute sin and cos of declination
        sind = 0.397850 * Math.sin(sigr);
        cosd = Math.sqrt(1. - sind * sind);
        xm = 12. + 0.12357 * sinp - 0.004289 * cosp + 0.153809 * sin2p
                + 0.060783 * cos2p;

        // compute local hour angle
        h = 15. * (gmt - xm) - zlong;
        hr = h * frad;
        zlatr = zlat * frad;

        // solve solar zenith
        cosz = Math.sin(zlatr) * sind + Math.cos(zlatr) * cosd * Math.cos(hr);
        sinz = Math.sqrt(1. - cosz * cosz);

        // solve solar azimuth. Equation taken from Smithsonian Meteorological
        // Tables, Sixth revised edition, Robert J. List, p.497.
        // Note: Need to redo this for Southern Hemisphere

        if (sinz == 0.) {
            saz = xpi;
        } else if (sind - Math.sin(zlatr) * cosz <= 0.) {
            az = (cosd * Math.sin(hr) / sinz);
            if (az < -1.0) az = -1.0;      // Otherwise, will have a math error.
            saz = Math.asin(az) + xpi;
        } else if (hr > 0.0) {
            saz = 2 * xpi - Math.asin(cosd * Math.sin(hr) / sinz);
        } else {
            saz = -Math.asin(cosd * Math.sin(hr) / sinz);
        }

        saz = saz / frad;

        if (saz < 0.0) {
            saz = saz + 360.0;
        }
        solzen.d = Math.acos(cosz) / 0.017453292;
        coszd.d = cosz;
    }

}
