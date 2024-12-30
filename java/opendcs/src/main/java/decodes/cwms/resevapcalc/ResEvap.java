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

import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DbCompException;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.util.*;


/**
 * Program to estimate evaporation from reservoirs
 * Program estimates 1-D water temperature profile in reservoir
 * Calculates evaporation
 *
 * @author RESEVAP program by Steven F. Daly (ERDC/CRREL)
 * Version 1.1  31 August 2009
 * VERSION 2.0 7 July 2010
 * conversion to Java by Richard Rachiele (RMA)
 * OpenDCS implementation by Oskar Hurst (HEC)
 */
final public class ResEvap
{
    // Some global constant parameter vaiues set here
    public static final double EMITTANCE_H20 = 0.98;
    public static final double PENFRAC = 0.4;
    public static final double ALBEDO = 0.08;
    public static final double THETA = 1.0;
    public static final double SIGMA = 5.67e-8;
    public static final double ETA_CONVECTIVE = .50;
    public static final double ETA_STIRRING = .40;
    public static final double WIND_CRITIC = 1.0;
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MetComputation.class.getName());
    public EvapReservoir reservoir;
    public EvapMetData metData;
    // store Water temperature profile data
    // one profile for each hour
    double[] wtempProfiles;
    // output computed time series
    private double solarRadTsc;
    private double IRDownTsc;
    private double IROutTsc;
    private double sensibleHeatTsc;
    private double latentHeatTsc;
    private double evapRateHourlyTsc;
    private double surfaceTempTsc;
    private File workDir;

    public ResEvap()
    {
    }

    public ResEvap(EvapReservoir reservoir, EvapMetData metData, Connection conn) throws DbCompException
    {
        setReservoir(reservoir, conn);
        setMetData(metData);
    }

    /**
     * This is the directory location output of results to textfiles
     *
     * @param workDir
     * @return
     */
    public boolean setOutputDirectory(File workDir)
    {
        this.workDir = workDir;

        return true;
    }

    /**
     * set the reservoir data for the compute and initialize
     * the reservoir layers
     *
     * @param reservoir
     * @return
     */
    public boolean setReservoir(EvapReservoir reservoir, Connection conn) throws DbCompException
    {
        this.reservoir = reservoir;

        // check data

        // Initialize reservoir layers
        if (!reservoir.initRes(conn))
        {
            return false;
        }

        if (!reservoir.resSetup(conn))
        {
            return false;
        }
        return true;

    }

    /**
     * set the meteorological data for the compute
     *
     * @param metData
     * @return
     */
    public boolean setMetData(EvapMetData metData)
    {
        this.metData = metData;
        return true;
    }

    /**
     * compute reservoir evaporation for the time window defined by the
     * startDateTime and endDateTime.  This method loops over the hours
     * of the simulation to call computes for the meteorological factors
     * (surface heat balance) and the heat balance and temperature structure
     * of the reservoir.
     * <p>
     * The reservoir and met data object need to have previously been set.
     *
     * @param currentTime
     * @param gmtOffset
     * @return
     */
    public boolean compute(Date currentTime,
                           double gmtOffset, Connection conn) throws DbCompException
    {
        if (reservoir == null)
        {
            throw new ResEvapException("ResEvap.compute: No reservoir has been set");
        }

        if (metData == null)
        {
            throw new ResEvapException("ResEvap.compute: No Meteorological data has been set");
        }

        // diagnostic output
        File outfil = null;
        File metoutfil = null;
        File toutfil = null;
        File xoutfil = null;

        if (workDir != null)
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
        if (toutfil != null)
        {
            try
            {
                tout = new BufferedWriter(new FileWriter(toutfil));
                resWtCompute.setOutfile(tout);
            } catch (IOException ex)
            {
                LOGGER.info("Unable to read {}", toutfil.getAbsolutePath(), ex);
            }
        }

        // debug reservoir layers
        BufferedWriter xout = null;
        if (xoutfil != null)
        {
            try
            {
                xout = new BufferedWriter(new FileWriter(xoutfil));
                reservoir.setDebugFile(xout);
            } catch (IOException ex)
            {
                LOGGER.info("Unable to read {}", xoutfil.getAbsolutePath(), ex);
            }
        }

        // file for reservoir temperature profile
        BufferedWriter out = null;
        if (outfil != null)
        {
            try
            {
                out = new BufferedWriter(new FileWriter(outfil));
            } catch (IOException ex)
            {
                LOGGER.info("Unable to read {}", outfil.getAbsolutePath(), ex);
            }
        }

        // met and surface heat exchange
        BufferedWriter metout = null;
        if (outfil != null)
        {
            try
            {
                metout = new BufferedWriter(new FileWriter(metoutfil));

                if (metout != null)
                {
                    String heading =
                            "    Date    JD  GMT     U       T      RH       P       Ts      K       u*        R*       L          Hs        HL        Qs        IR       IR_out     Evap";
                    metout.write(heading);
                    metout.newLine();

                    heading =
                            "                       m/s    deg C     %      mb      deg C           m/s                m      ********** W/m**2 ***********************     mm/d";
                    metout.write(heading);
                    metout.newLine();
                }
            } catch (IOException ex)
            {
                LOGGER.info("Unable to read {}", metoutfil.getAbsolutePath(), ex);
            }
        }

        // Do compute
        try
        {
            // init stuff

            // call resSetup to printout reservoir layer info
            if (xout != null)
            {
                reservoir.resSetup(conn);
            }

            int resj = reservoir.getResj();
            reservoir.resjOld = resj;

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

            double surfaceTemp = reservoir.wt[resj];

            // compute solar and longwave down radiation
            // and evaporation for reservoir location
            metComputation.computeMetAndEvap(currentTime, surfaceTemp,
                    resLocationInfo);

            boolean noProblem = true;

            // if no valid met data yet skip reservoir temp compute
            if (!metComputation.metFailed)
            {
                // compute temperature profile in reservoir
                noProblem = resWtCompute.computeReservoirTemp(
                        currentTime, metComputation, deltT);
            }

            if (noProblem)
            {

                solarRadTsc = metComputation.solar;
                IRDownTsc = metComputation.flxir;
                IROutTsc = metComputation.flxirOut;
                latentHeatTsc = metComputation.evapWater.hl;
                sensibleHeatTsc = metComputation.evapWater.hs;
                surfaceTempTsc = surfaceTemp;  // BOP surface temp
                // evap is in mm/day.  Divide by 24 to get instantaneous
                // hourly value
                evapRateHourlyTsc = metComputation.evapWater.evap / 24.;

                // store wt profile
                int numLayers = reservoir.getResj() + 1;
                wtempProfiles = new double[EvapReservoir.NLAYERS];
                for (int ilyr = 0; ilyr < numLayers; ilyr++)
                {
                    wtempProfiles[ilyr] = reservoir.wt[numLayers - 1 - ilyr];
                }
            }
            try
            {
                if (out != null) out.close();
                if (metout != null) metout.close();
                if (tout != null) tout.close();
                if (xout != null) xout.close();
            } catch (IOException ioe)
            {
                LOGGER.error("IOException occurred while closing files", ioe);
            }
        } catch (RuntimeException ex)
        {
            try
            {
                if (out != null) out.close();
                if (metout != null) metout.close();
                if (tout != null) tout.close();
                if (xout != null) xout.close();
            } catch (IOException ioe)
            {
                LOGGER.error("IOException occurred while closing files", ioe);
            }
            LOGGER.error("Error within computation", ex);
            throw new ResEvapException(ex);
        }

        return true;
    }

    /**
     * Get a List containing ResEvap computed TimeSeriesContainers
     *
     * @return
     */
    public List<Double> getComputedMetTimeSeries()
    {
        List<Double> computedTsList = new ArrayList<Double>();
        computedTsList.add(surfaceTempTsc);
        computedTsList.add(sensibleHeatTsc);
        computedTsList.add(latentHeatTsc);
        computedTsList.add(solarRadTsc);
        computedTsList.add(IRDownTsc);
        computedTsList.add(IROutTsc);
        computedTsList.add(evapRateHourlyTsc);

        return computedTsList;
    }

    /**
     * Get the computed hourly evaporation for the reservoir
     *
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
