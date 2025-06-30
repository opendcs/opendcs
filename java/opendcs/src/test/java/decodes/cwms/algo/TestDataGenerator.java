package decodes.cwms.algo;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TestDataGenerator
{
    private TestDataGenerator() { /* Prevent instantiation */ }

    public static List<TestRow> generateTestRows()
    {
        double initialElevStart = 1000.0;
        double initialAreaStart = 5000.0;
        double initialStorStart = 1_000_000.0;

        double elevStep = -0.5; // Elevation decreases by 0.5 per step
        double areaStep = -10.0; // Area decreases by 10 per step

        return IntStream.range(0, 24)
                .mapToObj(step -> {
                    double elevStart = initialElevStart + step * elevStep;
                    double elevEnd = elevStart + elevStep;

                    double areaStart = initialAreaStart + step * areaStep;
                    double areaEnd = areaStart + areaStep;

                    double depth = elevStart - elevEnd;

                    double frustumVolume = calculateFrustumVolume(areaStart, areaEnd, depth);

                    double storEnd = initialStorStart - (step + 1) * frustumVolume;
                    double storStart = initialStorStart - step * frustumVolume;

                    return new TestRow(elevStart, elevEnd, areaStart, areaEnd, storStart, storEnd);
                })
                .collect(Collectors.toList());
    }

    ///  this generates a single row of data for daily time step testing where the volume is calculated
    ///  as the frustum volume between the start and end elevations and not aggregated hourly volumes.
    public static TestRow generateTestRowForDailyData()
    {
        double elevStart = 1000.0;
        double elevEnd = 988.0;
        double depth = elevStart - elevEnd;
        double areaStart = 5000.0;
        double areaEnd = 4700.0;
        double storStart = 1_000_000.0;
        double frustumVolume  = calculateFrustumVolume(areaStart, areaEnd, depth);
        double storEnd = storStart - frustumVolume;

        return new TestRow(elevStart, elevEnd,
                areaStart, areaEnd, storStart, storEnd);

    }


    // Duplicated math logic for test data independence
    private static double calculateFrustumVolume(double area1, double area2, double depth)
    {
        return (area1 + area2 + Math.sqrt(area1 * area2)) / 3.0 * depth;
    }
}
