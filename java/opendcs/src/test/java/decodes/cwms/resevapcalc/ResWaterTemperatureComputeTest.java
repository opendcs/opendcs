package decodes.cwms.resevapcalc;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Testing vertically averaged density within the vertical temp profile computation using average density.
 * Volumetric average is included to compare results.
 */
class ResWaterTemperatureComputeTest
{
    private static final double[] RHOW_VALUES = {997.0, 998.5, 1000.0, 1001.0, 1003.0}; // Example densities in kg/m^3
    private static final double[] DEPTH_VALUES = {0.5, 1.0, 2.0, 3.0, 4.0}; // Example depths in meters
    private static final double[] VOLUME_VALUES = {1000.0, 2500.0, 6000.0, 12000.0, 20000.0};
    private static final double[] RHOW_DEPTH_PRODUCTS = new double[RHOW_VALUES.length];
    private static final double[] RHOW_VOLUME_PRODUCTS = new double[RHOW_VALUES.length];

    static
    {
        for (int i = 0; i < RHOW_VALUES.length; i++)
        {
            RHOW_DEPTH_PRODUCTS[i] = RHOW_VALUES[i] * DEPTH_VALUES[i];
            RHOW_VOLUME_PRODUCTS[i] = RHOW_VALUES[i] * VOLUME_VALUES[i];
        }
    }


    @MethodSource("getLayerIndexValues")
    @ParameterizedTest
    void testAvgRhow(int index)
    {
        // using depth average
        double avgRhowDepth = ResWaterTemperatureCompute.getAvgRhow(index, RHOW_VALUES, DEPTH_VALUES);
        double totalDepth = Arrays.stream(DEPTH_VALUES).limit(index + 1).sum();
        double totalDensityDepth = Arrays.stream(RHOW_DEPTH_PRODUCTS).limit(index + 1).sum();
        double expectedDepth = totalDensityDepth / totalDepth;

        assertEquals(expectedDepth, avgRhowDepth, 0.0001, "Depth Average: Average Rhow should match the set value");

        // using volumetric average
        double avgRhowVol = ResWaterTemperatureCompute.getAvgRhow(index, RHOW_VALUES, VOLUME_VALUES);
        double totalVolume = Arrays.stream(VOLUME_VALUES).limit(index + 1).sum();
        double totalDensityVolume = Arrays.stream(RHOW_VOLUME_PRODUCTS).limit(index + 1).sum();
        double expectedVol = totalDensityVolume / totalVolume;

        System.out.printf("layer %d: Rhow=%f, Depth=%f, Volume=%f%n AvgRhowDepth=%f, AvgRhowVolume=%f%n",
                index, RHOW_VALUES[index], DEPTH_VALUES[index], VOLUME_VALUES[index], avgRhowDepth, avgRhowVol);

        assertEquals(expectedVol, avgRhowVol, 0.0001, "Volume Average: Average Rhow should match the set value");

    }


    static Stream<Integer> getLayerIndexValues()
    {
        return IntStream.range(0, RHOW_VALUES.length)
                .boxed();
    }

}