package decodes.cwms.algo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for ResEvapAlgo
 * Includes tests for frustum volume and flow calculations using generated test data.
 */

final class ResEvapAlgoGeneratedDataTest
{

    private static final double SECONDS_PER_DAY = 86400.0;

    @Test
    void testSingleFrustumVolumeAndFlow()
    {
        double area1 = 200.0;
        double area2 = 100.0;
        double depth = 1.0;

        double expectedVolume = (area1 + area2 + Math.sqrt(area1 * area2)) / 3.0 * depth;
        double expectedFlow = expectedVolume / SECONDS_PER_DAY;

        double actualVolume = ResEvapAlgo.getFrustumVolumeM3(area1, area2, depth);
        double actualFlow = ResEvapAlgo.getVolumeM3AsFlowCMS(actualVolume);

        assertEquals(expectedVolume, actualVolume, 0.0001, "Frustum volume mismatch");
        assertEquals(expectedFlow, actualFlow, 0.0001, "Flow conversion mismatch");
    }


    @Test
    void testGenerateTestRows()
    {
        List<TestRow> testRows = TestDataGenerator.generateTestRows();

        assertNotNull(testRows, "Generated test rows should not be null");
        assertEquals(24, testRows.size(), "There should be 24 test rows generated");

        // Additional assertions to verify the content of the test rows
        TestRow firstRow = testRows.get(0);
        assertEquals(1000.0, firstRow.elevStart, 0.0001, "First row elevStart mismatch");
        assertEquals(999.5, firstRow.elevEnd, 0.0001, "First row elevEnd mismatch");
        assertEquals(5000.0, firstRow.areaStart, 0.0001, "First row areaStart mismatch");
        assertEquals(4990.0, firstRow.areaEnd, 0.0001, "First row areaEnd mismatch");
        assertEquals(1_000_000.0, firstRow.storStart, 0.0001, "First row storStart mismatch");
        assertEquals(997502.5004, firstRow.storEnd, 0.0001, "First row storEnd mismatch");
    }


    @Test
    void testHourlyFustumVolumeWithGeneratedData()
    {
        List<Executable> assertions = new ArrayList<>();
        List<TestRow> testRows = TestDataGenerator.generateTestRows();

        double expectedTotalDailyVolumeM3 = 0.0;
        double actualTotalDailyVolumeM3 = 0.0;
        int hours = testRows.size();

        for (TestRow row : testRows)
        {
            double depthM = row.elevStart - row.elevEnd;

            double expectedVolumeM3 = row.storStart - row.storEnd; // frustum volumes in m^3
            expectedTotalDailyVolumeM3 += expectedVolumeM3;

            double actualVolumeM3 = ResEvapAlgo.getFrustumVolumeM3(row.areaStart, row.areaEnd, depthM);
            actualTotalDailyVolumeM3 += actualVolumeM3;

            String rowMsg = String.format("Elev %.2f -> %.2f | Expected Vol: %.2f, Actual: %.2f",
                    row.elevStart, row.elevEnd, expectedVolumeM3, actualVolumeM3);

            assertions.add(() -> assertEquals(expectedVolumeM3, actualVolumeM3, 0.0001, rowMsg));
        }
        assertAll(assertions);

        String totalMsg = String.format("Aggregated Daily Total Volume from %d hours in generated test data: Expected Vol: %.4f | Actual: %.4f",
                hours, expectedTotalDailyVolumeM3, actualTotalDailyVolumeM3);

        System.out.println(totalMsg);

        assertEquals(expectedTotalDailyVolumeM3, actualTotalDailyVolumeM3, 0.0001, totalMsg);
    }


    @Test
    void testHourlyFustumFlowWithGeneratedData()
    {
        List<Executable> assertions = new ArrayList<>();
        List<TestRow> testRows = TestDataGenerator.generateTestRows();

        double expectedTotalFlowCMS = 0.0;
        double actualTotalDailyFlowCMS = 0.0;
        int hours = testRows.size();

        for (TestRow row : testRows)
        {
            double depthM = row.elevStart - row.elevEnd;

            double expectedVolumeM3 = row.storStart - row.storEnd;
            double expectedFlowCMS = expectedVolumeM3 / SECONDS_PER_DAY;

            double actualVolumeM3 = ResEvapAlgo.getFrustumVolumeM3(row.areaStart, row.areaEnd, depthM);
            double actualFlowCMS = ResEvapAlgo.getVolumeM3AsFlowCMS(actualVolumeM3);

            expectedTotalFlowCMS += expectedFlowCMS;
            actualTotalDailyFlowCMS += actualFlowCMS;

            String rowMsg = String.format("Elev %.2f -> %.2f | Expected Flow: %.2f, Actual: %.2f",
                    row.elevStart, row.elevEnd, expectedFlowCMS, actualFlowCMS);

            assertions.add(() -> assertEquals(expectedFlowCMS, actualFlowCMS, 0.0001, rowMsg));
        }
        assertAll(assertions);

        String totalMsg = String.format("Aggregated Daily Total Flow from %d hours in generated test data:Expected Flow: %.4f | Actual Flow: %.4f",
                hours, expectedTotalFlowCMS, actualTotalDailyFlowCMS);

        System.out.println(totalMsg);

        assertEquals(expectedTotalFlowCMS, actualTotalDailyFlowCMS, 0.0001, totalMsg);
    }


    @Test
    void testHourlyVsDailyFustumFlowWithGeneratedData()
    {
        List<TestRow> hourlyRows = TestDataGenerator.generateTestRows();
        int hours = hourlyRows.size();

        // ----- Frustum Hourly Aggregation Calculation -----
        double expectedHourlyTotalFlowCMS = 0.0;
        double actualHourlyTotalFlowCMS = 0.0;

        for (TestRow row : hourlyRows)
        {
            double depthM = row.elevStart - row.elevEnd;

            double expectedFlow = (row.storStart - row.storEnd) / SECONDS_PER_DAY;
            double actualVolume = ResEvapAlgo.getFrustumVolumeM3(row.areaStart, row.areaEnd, depthM);
            double actualFlow = ResEvapAlgo.getVolumeM3AsFlowCMS(actualVolume);

            expectedHourlyTotalFlowCMS += expectedFlow;
            actualHourlyTotalFlowCMS += actualFlow;
        }

        // ----- Frustum Daily Calculation -----
        TestRow dailyRow = TestDataGenerator.generateTestRowForDailyData();
        double dailyDepth = dailyRow.elevStart - dailyRow.elevEnd;

        double expectedDailyFlowCMS = (dailyRow.storStart - dailyRow.storEnd) / SECONDS_PER_DAY;
        double actualDailyFlowCMS = ResEvapAlgo.getDailyEvapFlow(dailyRow.areaStart, dailyRow.areaEnd, dailyDepth);

        // ----- Output Comparison -----
        String summary = String.format(
            "--- Daily Frustum Flow Comparison (%d hourly steps vs single daily step) ---%n" +
            "Hourly Aggregated Flow:%n" +
            "    Expected: %.4f cms%n" +
            "    Actual:   %.4f cms%n" +
            "Single Daily Step Flow:%n" +
            "    Expected: %.4f cms%n" +
            "    Actual:   %.4f cms%n" +
            "Difference Hourly-Daily:%n" +
            "    Expected: %.4f cms%n" +
            "    Actual:   %.4f cms%n",
            hours,
                expectedHourlyTotalFlowCMS, actualHourlyTotalFlowCMS,
                expectedDailyFlowCMS, actualDailyFlowCMS,
                (expectedHourlyTotalFlowCMS - expectedDailyFlowCMS), (actualHourlyTotalFlowCMS - actualDailyFlowCMS)
        );

        System.out.println(summary);

        assertEquals(expectedHourlyTotalFlowCMS, actualHourlyTotalFlowCMS, 0.0001, "Hourly aggregated flow mismatch");
        assertEquals(expectedDailyFlowCMS, actualDailyFlowCMS, 0.0001, "Single daily flow mismatch");

    }


    @Test
    void testEdgeCases_ZeroAndExtremeDepth() {
        double area1 = 200.0;
        double area2 = 100.0;

        double zeroDepth = 0.0;
        assertEquals(0.0, ResEvapAlgo.getFrustumVolumeM3(area1, area2, zeroDepth), 1e-9, "Zero depth should yield zero volume");
        assertEquals(0.0, ResEvapAlgo.getVolumeM3AsFlowCMS(0.0), 1e-9, "Zero volume should yield zero flow");
        assertEquals(0.0, ResEvapAlgo.getDailyEvapFlow(area1, area2, 0.0), 1e-9, "Zero depth should yield zero flow");

        System.out.println("Zero depth test results: " +
                "Frustum Volume = " + ResEvapAlgo.getFrustumVolumeM3(area1, area2, zeroDepth) +
                "%nFrustum Flow = " + ResEvapAlgo.getVolumeM3AsFlowCMS(ResEvapAlgo.getFrustumVolumeM3(area1, area2, zeroDepth)) +
                "%nOld Daily Evap Flow = " + ResEvapAlgo.getDailyEvapFlow(area1, area2, zeroDepth));

        double tinyDepth = 1e-6;
        assertTrue(ResEvapAlgo.getFrustumVolumeM3(area1, area2, tinyDepth) > 0, "Tiny depth should yield small positive volume");
        assertTrue(ResEvapAlgo.getVolumeM3AsFlowCMS(ResEvapAlgo.getFrustumVolumeM3(area1, area2, tinyDepth)) > 0, "Tiny depth should yield small positive flow");
        assertTrue(ResEvapAlgo.getDailyEvapFlow(area1, area2, tinyDepth) > 0, "Tiny depth should yield small positive flow");

        System.out.println("Tiny depth test results: " +
                "Frustum Volume = " + ResEvapAlgo.getFrustumVolumeM3(area1, area2, tinyDepth) +
                "%nFrustum Flow = " + ResEvapAlgo.getVolumeM3AsFlowCMS(ResEvapAlgo.getFrustumVolumeM3(area1, area2, tinyDepth)) +
                "%nOld Daily Evap Flow = " + ResEvapAlgo.getDailyEvapFlow(area1, area2, tinyDepth));

        double largeDepth = 1000.0;
        assertTrue(ResEvapAlgo.getFrustumVolumeM3(area1, area2, largeDepth) > 0, "Large depth should yield positive volume");
        assertTrue(ResEvapAlgo.getVolumeM3AsFlowCMS(ResEvapAlgo.getFrustumVolumeM3(area1, area2, largeDepth)) > 0, "Large depth should yield positive flow");
        assertTrue(ResEvapAlgo.getDailyEvapFlow(area1, area2, largeDepth) > 0, "Large depth should yield positive flow");

        System.out.println("Large depth test results: " +
                "Frustum Volume = " + ResEvapAlgo.getFrustumVolumeM3(area1, area2, largeDepth) +
                "%nFrustum Flow = " + ResEvapAlgo.getVolumeM3AsFlowCMS(ResEvapAlgo.getFrustumVolumeM3(area1, area2, largeDepth)) +
                "%nOld Daily Evap Flow = " + ResEvapAlgo.getDailyEvapFlow(area1, area2, largeDepth));
    }

}
