package decodes.cwms.algo;

import decodes.comp.HasLookupTable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This class tests the ResEvapAlgo class specifically in calculating daily evaporation flow using the frustum formula.
 * This class attempts to validate the calculations against predefined rating tables for different reservoirs.
 * The tests are using elev;area and elev;storage rating tables to validate the calculations.
 * The reservoirs with the limited range show that the tests pass when the elevation range is limited to exclude the
 * beginning and/or ending elevations.
 */

final class ResEvapAlgoRatingTableTest
{
    private static final double SECONDS_PER_DAY = 86400.0;

    enum Reservoir
    {
        YETL ("YETL", 0.01, 3214., Double.MAX_VALUE),
        OAHE("OAHE", 0.01, 1431., 1572.),
        GAPT("GAPT", 0.01, 1185., Double.MAX_VALUE);

        private final String reservoirName;
        private final double tolerance;
        private final List<TestRow> validRows;
        private final Double minElev;
        private final Double maxElev;

        Reservoir(String reservoirName, double tolerance, Double minElev, Double maxElev)
        {
            this.reservoirName = reservoirName;
            this.tolerance = tolerance;
            this.validRows = TabRatings.fromResources(reservoirName, minElev, maxElev);
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


    // Internal helper for reading TAB ratings
    static final class TabRatings {
        private static final double FT_TO_M = 0.3048;
        private static final double ACRE_FT_TO_M3 = 1233.48184;
        private static final double ACRES_TO_M2 = 4046.85642;
        private TabRatings() {}

        static List<TestRow> fromResources(String reservoirName, Double minElev, Double maxElev) {
            final String base = "decodes/algoTestData/";
            final String areaRes = base + reservoirName + ".elev_area.tab";
            final String storRes = base + reservoirName + ".elev_storage.tab";
            Map<Double, Double> elevToArea = readTabResourceToMap(areaRes);
            Map<Double, Double> elevToStor = readTabResourceToMap(storRes);
            if (elevToArea.isEmpty() || elevToStor.isEmpty()) {
                throw new IllegalStateException("Rating data missing or empty for " + reservoirName);
            }
            SortedSet<Double> commonElevFt = new TreeSet<>(elevToArea.keySet());
            commonElevFt.retainAll(elevToStor.keySet());
            if (minElev != null && maxElev != null && maxElev > minElev) {
                final double minE = minElev;
                final double maxE = maxElev;
                commonElevFt = commonElevFt.stream()
                        .filter(e -> e > minE && e < maxE)
                        .collect(Collectors.toCollection(TreeSet::new));
            }
            List<Double> elev = new ArrayList<>(commonElevFt);
            Collections.sort(elev);
            List<TestRow> out = new ArrayList<>();
            for (int i = 0; i < elev.size() - 1; i++) {
                double elevLoFt = elev.get(i);
                double elevHiFt = elev.get(i + 1);
                double areaLoM2 = elevToArea.get(elevLoFt) * ACRES_TO_M2;
                double areaHiM2 = elevToArea.get(elevHiFt) * ACRES_TO_M2;
                double storLoM3 = elevToStor.get(elevLoFt) * ACRE_FT_TO_M3;
                double storHiM3 = elevToStor.get(elevHiFt) * ACRE_FT_TO_M3;
                double elevLoM = elevLoFt * FT_TO_M;
                double elevHiM = elevHiFt * FT_TO_M;
                out.add(new TestRow(elevHiM, elevLoM, areaHiM2, areaLoM2, storHiM3, storLoM3));
            }
            return out;
        }

        private static Map<Double, Double> readTabResourceToMap(String resourcePath) {
            try {
                URL url = ResEvapAlgoRatingTableTest.class.getClassLoader().getResource(resourcePath);
                if (url == null) {
                    throw new IllegalStateException("Resource not found: " + resourcePath);
                }
                String filePath = new File(url.toURI()).getPath();
                return readTabFileToMap(new File(filePath).getName(), filePath);
            } catch (Exception e) {
                throw new RuntimeException("Failed to read TAB resource " + resourcePath, e);
            }
        }

        private static Map<Double, Double> readTabFileToMap(String nameForError, String tabPath) {
            class Collector implements HasLookupTable {
                final Map<Double, Double> points = new TreeMap<>();
                public void setProperty(String n, String v) {/* Method not used in this context */ } // unused
                public void addPoint(double indep, double dep) { points.put(indep, dep); }
                public void addShift(double indep, double shift) {/* Method not used in this context */ }
                public void setXOffset(double xo) { /* Method not used in this context */}
                public void setBeginTime(Date bt) { /* Method not used in this context */}
                public void setEndTime(Date et) { /* Method not used in this context */}
                public void clearTable() { points.clear(); }
            }
            Collector c = new Collector();
            try {
                new decodes.comp.TabRatingReader(tabPath).readRatingTable(c);
            } catch (decodes.comp.ComputationParseException e) {
                throw new RuntimeException("Failed to read TAB file: " + nameForError + " at " + tabPath, e);
            }
            return c.points;
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
        assertTrue(frustumVolumeLossM3 > 0 && aggregatedVolumeLossM3 > 0, "All volume losses should be positive");
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

