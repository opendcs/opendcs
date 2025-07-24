package decodes.cwms.algo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This class tests the ResEvapAlgo class specifically in calculating daily evaporation flow using the frustum formula.
 * This class attempts to validate the calculations against predefined rating tables for different reservoirs.
 * the tests are using elev;area and elev;storage rating tables to validate the calculations.
 * the reservoirs with the full sets are included to show where the calculations fail for each reservoir.
 * the reservoirs with the limited range show that the tests pass when the elevation range is limited to exclude the
 * beginning and/or ending elevations.
 */

final class ResEvapAlgoRatingTableTest
{
    private static final double SECONDS_PER_DAY = 86400.0;

    enum Reservoir
    {
        YETL ("YETL", 0.01, 3214., Double.MAX_VALUE),  ///  this is ok to use after elev - 3214
//        YETL_1 ("YETL", 0.01, 3204., Double.MAX_VALUE),  ///  test the full set

        OAHE("OAHE", 0.01, 1431., 1572.), /// this is ok to use after elev - 1431 and before 1572
//        OAHE_1("OAHE", 0.01, 1418., 1619.), ///  test the full set

        GAPT("GAPT", 0.01, 1185., Double.MAX_VALUE); ///  this is ok to use after elev - 1185
//        GAPT_1("GAPT", 0.01, 1167., Double.MAX_VALUE); ///  test the full set

        private final String reservoirName;
        private final double tolerance;
        private final List<TestRow> validRows;
        private final Double minElev;
        private final Double maxElev;

        Reservoir(String reservoirName, double tolerance, Double minElev, Double maxElev)
        {
            this.reservoirName = reservoirName;
            this.tolerance = tolerance;
            this.validRows = CsvRatingTableReader.getValidRatingTableRows(reservoirName, minElev, maxElev);
            this.minElev = minElev;
            this.maxElev = maxElev;
        }

        @Override
        public String toString()
        {
            if (maxElev == Double.MAX_VALUE)
            {
                return reservoirName + " -- Tolerance: " + tolerance +
                        "| Elevation range: {" + minElev + " to N/A}";
            }
            else
            {
                return reservoirName + " -- Tolerance: " + tolerance +
                        "| Elevation range: {" + minElev + " to " + maxElev + '}';
            }
        }
    }


/**
 * This test provides a diagnostic comparison of three approaches to calculating evaporation loss:
 * 1. Legacy Vertical wall:
 *    - Uses area at the start-of-day elevation.
 *    - Multiplies by total daily evaporation depth to estimate volume loss.
 * 2. Frustum Daily Calculation:
 *    - Uses total daily evaporation depth to calculate the frustum volume loss using just the starting elevation.
 * 3. Hourly Aggregation (Frustum-based):
 *    - Breaks the daily elevation/area change into 24 hourly steps.
 *    - Calculates hourly frustum volumes and sums them.
 *    - Provides the most geometry-driven estimate of daily evaporation loss.
 * This test prints the volume loss (m³) and corresponding flow rate (cms) for all three approaches.
 * It does not assert which is more accurate, simply provides expected insight
 * Minimal assertions ensure all volume and flow losses are positive.
 */

    @Test
    void testDiagnosticCompareLegacyVsFrustumVsHourlyEvapLoss() {
        // ----- Setup -----
        double elevStart = 100.0;
        double elevEnd = 98.7;
        double areaStart = 5000.0;
        double areaEnd = 4500.0;
        //  using a constant evap depth for simplicity.
        double totalDailyEvapDepth = elevStart - elevEnd;

        // ----- Legacy Vertical Wall Calculation -----
        double legacyVolumeLossM3 = areaStart * totalDailyEvapDepth;
        double legacyFlowCMS = ResEvapAlgo.getVolumeM3AsFlowCMS(legacyVolumeLossM3, SECONDS_PER_DAY);

        // ----- Frustum Daily Calculation -----
        double frustumVolumeLossM3 = ResEvapAlgo.getFrustumVolumeM3(areaStart, areaEnd, totalDailyEvapDepth);
        double frustumFlowCMS = ResEvapAlgo.getVolumeM3AsFlowCMS(frustumVolumeLossM3, SECONDS_PER_DAY);

        // ----- Frustum Hourly Aggregation Calculation -----
        // if not using a constant depth, this should be an avgHourlyEvapDepth ((prev + curr) / 2.0)
        double hourlyEvapDepth = totalDailyEvapDepth / 24.0;
        double aggregatedVolumeLossM3 = simulateHourlyEvapVolume(elevStart, elevEnd, areaStart, areaEnd, hourlyEvapDepth);
        double aggregatedFlowCMS = ResEvapAlgo.getVolumeM3AsFlowCMS(aggregatedVolumeLossM3, SECONDS_PER_DAY);

        // ----- Output Comparison -----
        System.out.printf("%n--- Evaporation Loss Comparison ---%n");
        System.out.printf("Legacy Vertical wall:%n" +
                          "  Volume Loss: %.2f m³ |  Flow: %.4f cms%n",
                            legacyVolumeLossM3, legacyFlowCMS);
        System.out.printf("Frustum Daily:%n" +
                          "  Volume Loss: %.2f m³ |  Flow: %.4f cms%n",
                            frustumVolumeLossM3, frustumFlowCMS);
        System.out.printf("Frustum Hourly Aggregated:%n" +
                          "  Volume Loss: %.2f m³ |  Flow: %.4f cms%n",
                            aggregatedVolumeLossM3, aggregatedFlowCMS);

        // ----- Minimal Sanity Assertions -----
        assertTrue(legacyVolumeLossM3 > 0 && frustumVolumeLossM3 > 0 && aggregatedVolumeLossM3 > 0, "All volume losses should be positive");
        assertTrue(legacyFlowCMS > 0 && frustumFlowCMS > 0 && aggregatedFlowCMS > 0, "All flows should be positive");
    }


    /**
     * Simulates hourly evaporation volume loss for a reservoir by:
     *  - Breaking the total elevation change evenly across 24 hours.
     *  - Interpolating area based on average hourly elevation.
     *  - Applying hourly evaporation depth.
     *  - Using frustum volume formula to compute hourly volume loss.
     * Returns the total aggregated evaporation volume loss across 24 hours.
     */
    private static double simulateHourlyEvapVolume(double elevStart, double elevEnd, double areaStart, double areaEnd, double hourlyEvapDepth)
    {
        double totalVolumeLoss = 0.0;
        double hourlyElevStep = (elevStart - elevEnd) / 24.0;

        for (int hour = 0; hour < 24; hour++) {
            double startElev = elevStart - hour * hourlyElevStep;
            double endElev = startElev - hourlyElevStep;

            // Area at average elevation for this hour
            double avgElev = (startElev + endElev) / 2.0;
            double areaBeforeEvap = interpolateArea(elevStart, elevEnd, areaStart, areaEnd, avgElev);
            double areaAfterEvap = interpolateArea(elevStart, elevEnd, areaStart, areaEnd, avgElev - hourlyEvapDepth);

            double hourlyVolumeLoss = ResEvapAlgo.getFrustumVolumeM3(areaBeforeEvap, areaAfterEvap, hourlyEvapDepth);
            totalVolumeLoss += hourlyVolumeLoss;
        }
        return totalVolumeLoss;
    }

    // Helper - simple linear interpolation between two elevation-area points
    private static double interpolateArea(double elevStart, double elevEnd,
                                          double areaStart, double areaEnd, double targetElev)
    {
        double fraction = (elevStart - targetElev) / (elevStart - elevEnd);
        return areaStart + fraction * (areaEnd - areaStart);
    }


    /**
     * This test validates the frustum evaporation volume calculations using hourly aggregation against known rating table data.
     * It assumes the total daily evaporation depth is the step between the start and end elevations in the rating tables,
     * and a constant evaporation depth per hour (total daily depth / 24).
     * The test checks that the relative error between the expected flow loss (from rating tables)
     * and the calculated flow loss (from hourly aggregation) is within the specified tolerance for each reservoir.
     * @param reservoir The reservoir enum containing name, tolerance, and valid rating table rows.
     */
    @ParameterizedTest
    @EnumSource(Reservoir.class)
    void testHourlyAggregatedFrustumFlowWithRatingTables(Reservoir reservoir)
    {
        double tolerance = reservoir.tolerance;
        List<Executable> executables = new ArrayList<>();

        for (TestRow row : reservoir.validRows)
        {
            double totalDailyEvapDepth = row.elevStart - row.elevEnd;
            double hourlyEvapDepthMeters = totalDailyEvapDepth / 24.0;

            double expectedVolumeM3 = row.storStart - row.storEnd;
            double expectedFlowLostCMS = expectedVolumeM3 / SECONDS_PER_DAY;

            double aggregatedVolumeLossM3 = simulateHourlyEvapVolume(row.elevStart, row.elevEnd, row.areaStart, row.areaEnd, hourlyEvapDepthMeters);
            double aggregatedFlowLossCMS = ResEvapAlgo.getVolumeM3AsFlowCMS(aggregatedVolumeLossM3, SECONDS_PER_DAY);

            double absoluteError = Math.abs(expectedFlowLostCMS - aggregatedFlowLossCMS);
            double relativeError = absoluteError / expectedFlowLostCMS;

            String msg = String.format(
                    "%s: %n" +
                    "    ElevStart: %.2f | ElevEnd: %.2f%n" +
                    "    RelErr:    %.4f | AbsErr:  %.4f%n" +
                    "    AggregatedFlowCMS: %.4f | ExpectedFlowCMS: %.4f",
                    reservoir, (row.elevStart/ 0.3048), (row.elevEnd/ 0.3048),
                    relativeError, absoluteError,
                    aggregatedFlowLossCMS, expectedFlowLostCMS
            );

            executables.add(() -> assertTrue(relativeError < tolerance, msg));

        }
        assertAll(executables);
    }

}

