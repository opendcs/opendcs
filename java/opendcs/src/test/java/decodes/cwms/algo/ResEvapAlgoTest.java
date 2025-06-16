package decodes.cwms.algo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This class tests the ResEvapAlgo class for calculating daily evaporation flow using the frustum formula.
 * It uses parameterized tests to validate the calculations against predefined rating tables for different reservoirs.
 * It also includes manual and edge case tests
 */

final class ResEvapAlgoTest {
    private static final double ACRE_FT_TO_M3 = 1233.48184;
    private static final double ACRES_TO_M2 = 4046.85642;

    enum Reservoir {
        YETL ("YETL", 0.01, 3214., Double.MAX_VALUE),  ///  this is ok to use after elev - 3214
        OAHE("OAHE", 0.01, 1431., 1572.), /// this is ok to use after elev - 1431 and before 1572
        GAPT("GAPT", 0.01, 1185., Double.MAX_VALUE); ///  this is ok to use after elev - 1185

        private final String reservoirName;
        private final double tolerance;
        private final List<TestRow> validRows;
        private final Double minElev;
        private final Double maxElev;


        Reservoir(String reservoirName, double tolerance, Double minElev, Double maxElev) {
            this.reservoirName = reservoirName;
            this.tolerance = tolerance;
            this.validRows = getValidRatingTableRows(reservoirName, minElev, maxElev);
            this.minElev = minElev;
            this.maxElev = maxElev;
        }

        @Override
        public String toString() {
            return "Reservoir: " + reservoirName + '\'' +
                    "tolerance=" + tolerance +
                    ", acceptable elevation range= " + minElev + " - " + maxElev +
                    '}';
        }
    }


    @Test
    void testManualFrustumCalculation() {
        // Manual test for a specific case
        double areaAtElev1m2 = 200.0;
        double areaAtElev2m2 = 100.0;
        double depthMeters = 1.0;

        double expectedVolumeM3 = 147.14;
        double expectedFlowLostCMS = expectedVolumeM3 / 86400.0;

        double actualFlowLostCMS = ResEvapAlgo.getDailyEvapFlow(areaAtElev1m2, areaAtElev2m2, depthMeters);

        assertEquals(expectedFlowLostCMS, actualFlowLostCMS, 0.0001, "Manual frustum test failed");
    }


    @ParameterizedTest
    @EnumSource(Reservoir.class)
    void testFrustumFlowWithRatingsTables(Reservoir reservoir) {

        double depthMeters = 0.3048;

        double tolerance = reservoir.tolerance;
        List<Executable> executables = new ArrayList<>();

        for (TestRow row : reservoir.validRows) {
            // expected flow lost in cms based on the rating tables
            double expectedVolumeM3 = (row.stor2 - row.stor1);
            double expectedFlowLostCMS = expectedVolumeM3 / 86400.0;

            // Method call for actual flow lost in cms
            double actualFlowLostCMS = ResEvapAlgo.getDailyEvapFlow(row.area1, row.area2, depthMeters);

            // Calculate absolute and relative errors
            double absoluteError = Math.abs(expectedFlowLostCMS - actualFlowLostCMS);
            double relativeError = absoluteError / expectedFlowLostCMS;
            // collect the executables for later assertion
            executables.add(() -> assertTrue(relativeError <  tolerance, (String.format("%s, Elev1: %.2f, Elev2: %.2f, Relative Error: %.4f, Absolute Error: %.4f",
                    reservoir, row.elev1, row.elev2, relativeError, absoluteError))));
        }
        assertAll(executables);
    }

    @Test
    void edgeCaseTestEvapDepths() {
        // Edge case test for a specific case
        double areaAtElev1m2 = 200.0;
        double areaAtElev2m2 = 100.0;

        // Edge case: evaporation depth is zero
        double depthMeters = 0.0;
        double expectedFlowLostCMS = 0.0;
        double actualFlowLostCMS = ResEvapAlgo.getDailyEvapFlow(areaAtElev1m2, areaAtElev2m2, depthMeters);

        assertEquals(expectedFlowLostCMS, actualFlowLostCMS, 0.0001, "Edge case: evapDepth= 0.0, frustum test failed");

        // Edge case: Very small evaporation depth
        depthMeters = 0.0001;
        double expectedVolumeM3 = 0.014714045; // Adjust this based on your expectations for very small evaporation depth
        expectedFlowLostCMS = expectedVolumeM3 / 86400.0;
        actualFlowLostCMS = ResEvapAlgo.getDailyEvapFlow(areaAtElev1m2, areaAtElev2m2, depthMeters);

        assertEquals(expectedFlowLostCMS, actualFlowLostCMS, 0.0001, "Edge case: very small evapDepth, frustum test failed");


        // Edge case: Very large evaporation depth
        depthMeters = 1000.0; // This is an arbitrary large value for testing
        expectedVolumeM3 = 147140.45; // Adjust this based on your expectations for very large evaporation depth
        expectedFlowLostCMS = expectedVolumeM3 / 86400.0;

        actualFlowLostCMS = ResEvapAlgo.getDailyEvapFlow(areaAtElev1m2, areaAtElev2m2, depthMeters);
        assertEquals(expectedFlowLostCMS, actualFlowLostCMS, 0.0001, "Edge case: very large evapDepth, frustum test failed");


    }

    static List<TestRow> getValidRatingTableRows(String reservoirName, Double minElev, Double maxElev)
    {
        Map<Double, Double> areaMap = new TreeMap<>();
        Map<Double, Double> storageMap = new TreeMap<>();

        String filename = "decodes/algoTestData/" + reservoirName + "RatingTables.csv";

        InputStream inputStream = ResEvapAlgoTest.class.getClassLoader().getResourceAsStream(filename);
        if (inputStream == null) {
            throw new IllegalStateException("Resource file not found: " + filename);
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            reader.readLine(); // Skip header
            reader.readLine(); // Skip units line

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 4) {
                    continue;
                }

                if (!parts[0].isEmpty() && !parts[1].isEmpty()) {
                    double elevArea = Double.parseDouble(parts[0]);
                    double area = Double.parseDouble(parts[1]);
                    if( (minElev != null && maxElev != null) && (elevArea > minElev && elevArea < maxElev)) {
                        areaMap.put(elevArea, area);
                    }
                }

                if (!parts[2].isEmpty() && !parts[3].isEmpty()) {
                    double elevStor = Double.parseDouble(parts[2]);
                    double storage = Double.parseDouble(parts[3]);
                    if ((minElev != null && maxElev != null) && ( elevStor > minElev && elevStor < maxElev)) {
                        storageMap.put(elevStor, storage);
                    }
                }

            }
            reader.close();
            // Ensure both maps are not empty
            if (areaMap.isEmpty() || storageMap.isEmpty()) {
                throw new IOException("Rating table data is incomplete for reservoir: " + reservoirName);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading rating table file: " + filename, e);
        }

        return getSharedElevations(areaMap, storageMap);

    }

    /**
     * Find common elevations present in both areaMap and storageMap, avoiding elevation increment discrepancies.
     * Uses Java streams to avoid modifying a collection while iterating over it, which would cause a ConcurrentModificationException.
     **/
    private static List<TestRow> getSharedElevations(Map<Double, Double> areaMap, Map<Double, Double> storageMap) {
        List<Double> commonElevations = areaMap.keySet().stream()
                .filter(storageMap::containsKey)
                .sorted()
                .collect(Collectors.toList());
        List<TestRow> validRows = new ArrayList<>();
        for(int i = 0; i< commonElevations.size()-1; i++){
            double elev1 = commonElevations.get(i);
            double elev2 = commonElevations.get(i+1);

            double area1 = areaMap.get(elev1) * ACRES_TO_M2;
            double area2 = areaMap.get(elev2) * ACRES_TO_M2;
            double stor1 = storageMap.get(elev1) * ACRE_FT_TO_M3;
            double stor2 = storageMap.get(elev2) * ACRE_FT_TO_M3;

            validRows.add(new TestRow(elev1, elev2, area1, area2, stor1, stor2));

        }
        return validRows;
    }

    static class TestRow {
        final double elev1, elev2;
        final double area1, area2;
        final double stor1, stor2;

        TestRow(double elev1, double elev2, double area1, double area2, double stor1, double stor2) {
            this.elev1 = elev1;
            this.elev2 = elev2;
            this.area1 = area1;
            this.area2 = area2;
            this.stor1 = stor1;
            this.stor2 = stor2;
        }
    }

}

