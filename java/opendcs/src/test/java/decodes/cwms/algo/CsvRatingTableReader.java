package decodes.cwms.algo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

public class CsvRatingTableReader
{
    private static final double FT_TO_M = 0.3048;
    private static final double ACRE_FT_TO_M3 = 1233.48184;
    private static final double ACRES_TO_M2 = 4046.85642;

    public static List<TestRow> getValidRatingTableRows(String reservoirName, Double minElev, Double maxElev)
    {
        Map<Double, Double> areaMap = new TreeMap<>();
        Map<Double, Double> storageMap = new TreeMap<>();

        String filename = "decodes/algoTestData/" + reservoirName + "RatingTables.csv";

        InputStream inputStream = CsvRatingTableReader.class.getClassLoader().getResourceAsStream(filename);
        if (inputStream == null) {
            throw new IllegalStateException("Resource file not found: " + filename);
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try  {
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
                    if ((minElev != null && maxElev != null) && (elevArea > minElev && elevArea < maxElev)) {
                        areaMap.put(elevArea, area);
                    }
                }

                if (!parts[2].isEmpty() && !parts[3].isEmpty()) {
                    double elevStor = Double.parseDouble(parts[2]);
                    double storage = Double.parseDouble(parts[3]);
                    if ((minElev != null && maxElev != null) && (elevStor > minElev && elevStor < maxElev)) {
                        storageMap.put(elevStor, storage);
                    }
                }
            }

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
     * This method assumes that the elevations in both maps are in feet and converts them to meters for consistency.
     * It also converts areas from acres to square meters and storage from acre-feet to cubic meters.
     **/
    private static List<TestRow> getSharedElevations(Map<Double, Double> areaMap, Map<Double, Double> storageMap) {
        List<Double> commonElevations = areaMap.keySet().stream()
                .filter(storageMap::containsKey)
                .sorted()
                .collect(Collectors.toList());
        List<TestRow> validRows = new ArrayList<>();

        for (int i = 0; i < commonElevations.size() - 1; i++)
        {
            double elevLower = commonElevations.get(i); // Convert to meters
            double elevHigher = commonElevations.get(i + 1); // Convert to meters
            double areaLower = areaMap.get(elevLower) * ACRES_TO_M2; // Convert to square meters
            double areaHigher = areaMap.get(elevHigher) * ACRES_TO_M2; // Convert to square meters
            double storLower = storageMap.get(elevLower) * ACRE_FT_TO_M3; // Convert to cubic meters
            double storHigher = storageMap.get(elevHigher) * ACRE_FT_TO_M3; // Convert to cubic meters

            elevLower *= FT_TO_M;
            elevHigher *= FT_TO_M;

            validRows.add(new TestRow(elevHigher, elevLower, areaHigher, areaLower, storHigher, storLower));
        }
        return validRows;
    }

    private static int getCurrentRowIndex(List<TestRow> validRows, double elev)
    {
        for (int i = 0; i < validRows.size(); i++)
        {
            TestRow row = validRows.get(i);
            if (elev <= row.elevStart && elev >= row.elevEnd)
                return i;
        }
        return -1;
    }
}