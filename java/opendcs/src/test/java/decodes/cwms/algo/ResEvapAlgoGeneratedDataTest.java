package decodes.cwms.algo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for ResEvapAlgo
 * Includes tests for frustum volume and flow calculations using calculated test data
 * frustum formula for reference: (area1 + area2 + Math.sqrt(area1 * area2)) / 3.0 * depth;
 */

final class ResEvapAlgoGeneratedDataTest
{

    private static final double SECONDS_PER_DAY = 86400.0;
    private final List<TestRow> calculatedTestRows = Arrays.asList(
        new TestRow(1000.00, 999.50, 5000.00, 4990.00, 1000000.00000, 997502.50042),
        new TestRow(999.50, 999.00, 4990.00, 4980.00, 997507.50042, 995015.00084),
        new TestRow(999.00, 998.50, 4980.00, 4970.00, 995025.00084, 992537.50126),
        new TestRow(998.50, 998.00, 4970.00, 4960.00, 992552.50126, 990070.00168),
        new TestRow(998.00, 997.50, 4960.00, 4950.00, 990090.00168, 987612.50210),
        new TestRow(997.50, 997.00, 4950.00, 4940.00, 987637.50211, 985165.00253),
        new TestRow(997.00, 996.50, 4940.00, 4930.00, 985195.00253, 982727.50296),
        new TestRow(996.50, 996.00, 4930.00, 4920.00, 982762.50296, 980300.00338),
        new TestRow(996.00, 995.50, 4920.00, 4910.00, 980340.00339, 977882.50381),
        new TestRow(995.50, 995.00, 4910.00, 4900.00, 977927.50382, 975475.00425),
        new TestRow(995.00, 994.50, 4900.00, 4890.00, 975525.00426, 973077.50468),
        new TestRow(994.50, 994.00, 4890.00, 4880.00, 973132.50469, 970690.00512),
        new TestRow(994.00, 993.50, 4880.00, 4870.00, 970750.00513, 968312.50556),
        new TestRow(993.50, 993.00, 4870.00, 4860.00, 968377.50557, 965945.00600),
        new TestRow(993.00, 992.50, 4860.00, 4850.00, 966015.00601, 963587.50644),
        new TestRow(992.50, 992.00, 4850.00, 4840.00, 963662.50645, 961240.00688),
        new TestRow(992.00, 991.50, 4840.00, 4830.00, 961320.00689, 958902.50733),
        new TestRow(991.50, 991.00, 4830.00, 4820.00, 958987.50734, 956575.00777),
        new TestRow(991.00, 990.50, 4820.00, 4810.00, 956665.00779, 954257.50822),
        new TestRow(990.50, 990.00, 4810.00, 4800.00, 954352.50824, 951950.00867),
        new TestRow(990.00, 989.50, 4800.00, 4790.00, 952050.00869, 949652.50912),
        new TestRow(989.50, 989.00, 4790.00, 4780.00, 949757.50914, 947365.00958),
        new TestRow(989.00, 988.50, 4780.00, 4770.00, 947475.00960, 945087.51003),
        new TestRow(988.50, 988.00, 4770.00, 4760.00, 945202.51006, 942820.01049)
    );

    @Test
    void testSingleFrustumVolumeAndFlow()
    {
        double area1 = 200.0;
        double area2 = 100.0;
        double depth = 1.0;

        double expectedVolume = (area1 + area2 + Math.sqrt(area1 * area2)) / 3.0 * depth;
        double expectedFlow = expectedVolume / SECONDS_PER_DAY;

        double actualVolume = ResEvapAlgo.getFrustumVolumeM3(area1, area2, depth);
        double actualFlow = ResEvapAlgo.getVolumeM3AsFlowCMS(actualVolume, SECONDS_PER_DAY);

        assertEquals(expectedVolume, actualVolume, 0.0001, "Frustum volume mismatch");
        assertEquals(expectedFlow, actualFlow, 0.0001, "Flow conversion mismatch");
    }

    @Test
    void testHourlyFrustumVolumeWithGeneratedData()
    {
        List<Executable> assertions = new ArrayList<>();

        double expectedTotalDailyVolumeM3 = 0.0;
        double actualTotalDailyVolumeM3 = 0.0;
        int hours = calculatedTestRows.size();

        for (TestRow row : calculatedTestRows)
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
    void testHourlyFrustumFlowWithGeneratedData()
    {
        List<Executable> assertions = new ArrayList<>();

        double expectedTotalFlowCMS = 0.0;
        double actualTotalDailyFlowCMS = 0.0;
        int hours = calculatedTestRows.size();

        for (TestRow row : calculatedTestRows)
        {
            double depthM = row.elevStart - row.elevEnd;

            double expectedVolumeM3 = row.storStart - row.storEnd;
            double expectedFlowCMS = expectedVolumeM3 / SECONDS_PER_DAY;

            double actualVolumeM3 = ResEvapAlgo.getFrustumVolumeM3(row.areaStart, row.areaEnd, depthM);
            double actualFlowCMS = ResEvapAlgo.getVolumeM3AsFlowCMS(actualVolumeM3, SECONDS_PER_DAY);

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


    /**
     * Test edge cases for frustum volume and flow calculations.
     * Includes zero depth, tiny depth, and large depth scenarios.
     */


    @Test
    void testEdgeCases_ZeroAndExtremeDepth()
    {
        double area1 = 200.0;
        double area2 = 100.0;

        double zeroDepth = 0.0;
        assertEquals(0.0, ResEvapAlgo.getFrustumVolumeM3(area1, area2, zeroDepth), 1e-9, "Zero depth should yield zero volume");
        assertEquals(0.0, ResEvapAlgo.getVolumeM3AsFlowCMS(0.0, SECONDS_PER_DAY), 1e-9, "Zero volume should yield zero flow");

        System.out.println("Zero depth test results: " +
                "\nFrustum Volume = " + ResEvapAlgo.getFrustumVolumeM3(area1, area2, zeroDepth) +
                "\nFrustum Flow = " + ResEvapAlgo.getVolumeM3AsFlowCMS(ResEvapAlgo.getFrustumVolumeM3(area1, area2, zeroDepth), SECONDS_PER_DAY)
        );

        double tinyDepth = 1e-6;
        assertTrue(ResEvapAlgo.getFrustumVolumeM3(area1, area2, tinyDepth) > 0, "Tiny depth should yield small positive volume");
        assertTrue(ResEvapAlgo.getVolumeM3AsFlowCMS(ResEvapAlgo.getFrustumVolumeM3(area1, area2, tinyDepth), SECONDS_PER_DAY) > 0, "Tiny depth should yield small positive flow");

        System.out.println("Tiny depth test results: " +
                "\nFrustum Volume = " + ResEvapAlgo.getFrustumVolumeM3(area1, area2, tinyDepth) +
                "\nFrustum Flow = " + ResEvapAlgo.getVolumeM3AsFlowCMS(ResEvapAlgo.getFrustumVolumeM3(area1, area2, tinyDepth), SECONDS_PER_DAY)
        );

        double largeDepth = 1000.0;
        assertTrue(ResEvapAlgo.getFrustumVolumeM3(area1, area2, largeDepth) > 0, "Large depth should yield positive volume");
        assertTrue(ResEvapAlgo.getVolumeM3AsFlowCMS(ResEvapAlgo.getFrustumVolumeM3(area1, area2, largeDepth), SECONDS_PER_DAY) > 0, "Large depth should yield positive flow");

        System.out.println("Large depth test results: " +
                "\nFrustum Volume = " + ResEvapAlgo.getFrustumVolumeM3(area1, area2, largeDepth) +
                "\nFrustum Flow = " + ResEvapAlgo.getVolumeM3AsFlowCMS(ResEvapAlgo.getFrustumVolumeM3(area1, area2, largeDepth), SECONDS_PER_DAY)
        );
    }

}
