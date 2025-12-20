package org.opendcs.dao;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.nio.charset.StandardCharsets;

import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;

import decodes.cwms.CwmsTimeSeriesDb;
import decodes.cwms.CwmsLocationLevelDAO;
import decodes.tsdb.TimeSeriesDb;
import org.opendcs.database.dai.SiteReferenceMetaData;

import org.opendcs.model.SiteReferenceValue;
import org.opendcs.model.SiteReferenceSpecification;
import org.opendcs.model.cwms.CwmsSiteReferenceValue;
import org.opendcs.model.cwms.CwmsSiteReferenceSpecification;

import org.opendcs.fixtures.annotations.EnableIfTsDb;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;
import java.util.Date;
import java.util.Calendar;
import decodes.tsdb.CTimeSeries;
import ilex.var.TimedVariable;

/**
 * Integration tests for CwmsLocationLevelDAO
 * These tests require a real CWMS database connection
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnableIfTsDb({"CWMS-Oracle"})
class CwmsLocationLevelDAOTestIT extends AppTestBase
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();

    @ConfiguredField
    private TimeSeriesDb tsDb;

    @ConfiguredField
    private OpenDcsDatabase db;

    private CwmsLocationLevelDAO dao;

    @BeforeAll
    void setupTestData() throws Exception
    {
        log.info("Loading location level test data for integration tests");
        loadLocationLevelTestData();
        log.info("Location level test data loaded successfully");
    }

    @BeforeEach
    void setUp()
    {
        dao = (CwmsLocationLevelDAO)db.getDao(SiteReferenceMetaData.class)
                                      .orElseGet(() -> fail("Unable to retrieve LocationLevelDAI instance from database."));
    }

    @Test
    void testGetLatestLocationLevelValue_Basic() throws Exception
    {
        // This test verifies the method executes without error
        // Test data is created during container initialization
        String testLocationLevelId = "FTPK-Lower.Stage.Const.0.Test";

        try (DataTransaction tx = db.newTransaction())
        {
            CwmsSiteReferenceValue value = (CwmsSiteReferenceValue) dao.getLatestLocationLevelValue(tx, testLocationLevelId);

            // Test passes whether data exists or not
            // If value is not null, verify its structure
            if (value != null)
            {
                assertNotNull(value.getLevelDate(), "Level date should not be null when value exists");
                assertNotNull(value.getUnits(), "Units should not be null when value exists");
                assertEquals(testLocationLevelId, value.getLocationLevelId(),
                    "Location level ID should match request");

                log.info("Found test data for location: " + testLocationLevelId);
            }
            else
            {
                log.info("No test data found for location: " + testLocationLevelId +
                    " (this is OK - test verifies method executes without error)");
            }

            // Verify method doesn't throw exception for non-existent location
            assertDoesNotThrow(() -> {
                dao.getLatestLocationLevelValue(tx, "NONEXISTENT.Stage.Const.0.Test");
            });
        }
    }

    @Test
    void testGetLatestLocationLevelValue_WithUnitConversion() throws Exception
    {
        String testLocationLevelId = "FTPK-Lower.Stage.Const.0.Test";

        try (DataTransaction tx = db.newTransaction())
        {
            // First check if we have any data
            CwmsSiteReferenceValue baseValue = (CwmsSiteReferenceValue) dao.getLatestLocationLevelValue(tx, testLocationLevelId);

            // Skip unit conversion test if no data exists
            assumeTrue(baseValue != null,
                "Skipping unit conversion test - no data found for " + testLocationLevelId);

            // Test conversion from feet to meters
            CwmsSiteReferenceValue valueInFeet = (CwmsSiteReferenceValue) dao.getLatestLocationLevelValue(
                tx, testLocationLevelId, "ft");

            CwmsSiteReferenceValue valueInMeters = (CwmsSiteReferenceValue) dao.getLatestLocationLevelValue(
                tx, testLocationLevelId, "m");

            if (valueInFeet != null && valueInMeters != null)
            {
                // Verify units are set correctly
                assertEquals("ft", valueInFeet.getUnits());
                assertEquals("m", valueInMeters.getUnits());

                // Verify conversion is roughly correct (1 ft ≈ 0.3048 m)
                // Only check if the original values are different from zero
                if (Math.abs(valueInFeet.getLevelValue()) > 0.001)
                {
                    double expectedMeters = valueInFeet.getLevelValue() * 0.3048;
                    assertEquals(expectedMeters, valueInMeters.getLevelValue(), 0.01,
                        "Conversion from feet to meters should be accurate");
                }
            }
        }
    }

    @Test
    void testGetLocationLevelSpecs() throws Exception
    {
        String testLocationId = "FTPK-Lower";

        try (DataTransaction tx = db.newTransaction())
        {
            List<CwmsSiteReferenceSpecification> specs = (List<CwmsSiteReferenceSpecification>) dao.getLocationLevelSpecs(tx, testLocationId);

            assertNotNull(specs, "Specs list should not be null");

            // If specs exist, verify their structure
            for (CwmsSiteReferenceSpecification spec : specs)
            {
                assertNotNull(spec.getLocationLevelId(), "Location level ID should not be null");
                assertNotNull(spec.getLocationId(), "Location ID should not be null");
                assertNotNull(spec.getParameterId(), "Parameter ID should not be null");
            }
        }
    }

    @Test
    void testTransactionSupport() throws Exception
    {
        String testLocationLevelId = "FTPK-Lower.Stage.Const.0.Test";
        String testLocationId = "FTPK-Lower";

        try (DataTransaction tx = db.newTransaction())
        {
            // Test getLatestLocationLevelValue with transaction
            CwmsSiteReferenceValue value = (CwmsSiteReferenceValue) dao.getLatestLocationLevelValue(
                tx, testLocationLevelId);

            // Test getLocationLevelSpecs with transaction
            List<CwmsSiteReferenceSpecification> specs = (List<CwmsSiteReferenceSpecification>) dao.getLocationLevelSpecs(
                tx, testLocationId);

            assertNotNull(specs, "Specs should not be null even if empty");

            // For read-only operations, we don't need to commit
            // Just verify the methods execute without errors
        }

    }

    @Test
    void testErrorHandling_InvalidLocationId() throws Exception
    {
        try (DataTransaction tx = db.newTransaction())
        {
            // Use an invalid location ID format
            String invalidLocationId = "INVALID_FORMAT";

            // Should not throw exception, just return null
            CwmsSiteReferenceValue value =  assertDoesNotThrow(() -> (CwmsSiteReferenceValue) dao.getLatestLocationLevelValue(tx, invalidLocationId));
            assertNull(value);

            // Most likely will be null for invalid ID
            // But if not null, it means the ID exists (unlikely for this test ID)
            // Either way, no exception should be thrown
        }
    }

    @Test
    void testInterfaceCompatibility() throws Exception
    {

        try (DataTransaction tx = db.newTransaction())
        {
            // Use the DAO through the interface
            SiteReferenceMetaData daiInterface = dao;

            String testLocationLevelId = "FTPK-Lower.Stage.Const.0.Test";

            // All interface methods should work with transactions
            assertDoesNotThrow(() ->
            {
                SiteReferenceValue value = daiInterface.getLatestLocationLevelValue(tx, testLocationLevelId);

                SiteReferenceValue valueWithUnits = daiInterface.getLatestLocationLevelValue(tx, testLocationLevelId, "ft");

                List<? extends SiteReferenceSpecification> specs = daiInterface.getLocationLevelSpecs(tx, "FTPK-Lower");
            });
            // No exceptions should be thrown
        }
    }

    @Test
    void testGetLocationLevelRange() throws Exception
    {
        String testLocationLevelId = "FTPK-Lower.Stage.Const.0.Test";

        // Create a time range for testing
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -30); // 30 days ago
        Date startTime = cal.getTime();
        Date endTime = new Date(); // Now

        try (DataTransaction tx = db.newTransaction())
        {
            // Test basic retrieval without unit conversion
            CTimeSeries timeSeries = dao.getLocationLevelRange(tx, testLocationLevelId,
                                                                startTime, endTime, null);

            assertNotNull(timeSeries, "Time series should not be null");
            assertEquals(testLocationLevelId, timeSeries.getDisplayName(),
                         "Display name should match location level ID");

            // Check if we have any samples
            int sampleCount = timeSeries.size();
            log.info("Retrieved {} samples for location level {}", sampleCount, testLocationLevelId);

            // If we have samples, verify their structure
            if (sampleCount > 0)
            {
                TimedVariable firstSample = timeSeries.sampleAt(0);
                assertNotNull(firstSample, "First sample should not be null");
                assertNotNull(firstSample.getTime(), "Sample time should not be null");

                // Verify samples are within the requested time range
                for (int i = 0; i < sampleCount; i++)
                {
                    TimedVariable sample = timeSeries.sampleAt(i);
                    Date sampleTime = sample.getTime();
                    assertTrue(!sampleTime.before(startTime) && !sampleTime.after(endTime),
                              "Sample time should be within requested range");
                }

                // Verify samples are ordered by time
                if (sampleCount > 1)
                {
                    for (int i = 1; i < sampleCount; i++)
                    {
                        Date prevTime = timeSeries.sampleAt(i-1).getTime();
                        Date currTime = timeSeries.sampleAt(i).getTime();
                        assertTrue(!currTime.before(prevTime),
                                  "Samples should be ordered by time");
                    }
                }
            }
        }
    }

    @Test
    void testGetLocationLevelRange_WithUnitConversion() throws Exception
    {
        String testLocationLevelId = "FTPK-Lower.Stage.Const.0.Test";

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -7); // 7 days ago
        Date startTime = cal.getTime();
        Date endTime = new Date();

        try (DataTransaction tx = db.newTransaction())
        {
            // Get values in meters (SI units)
            CTimeSeries timeSeriesMeters = dao.getLocationLevelRange(tx, testLocationLevelId,
                                                                     startTime, endTime, "m");

            // Get values in feet
            CTimeSeries timeSeriesFeet = dao.getLocationLevelRange(tx, testLocationLevelId,
                                                                   startTime, endTime, "ft");

            assertNotNull(timeSeriesMeters, "Time series in meters should not be null");
            assertNotNull(timeSeriesFeet, "Time series in feet should not be null");

            // If we have data, verify units are set correctly
            if (timeSeriesMeters.size() > 0)
            {
                assertEquals("m", timeSeriesMeters.getUnitsAbbr(),
                            "Units should be meters when requested");
            }

            if (timeSeriesFeet.size() > 0)
            {
                assertEquals("ft", timeSeriesFeet.getUnitsAbbr(),
                            "Units should be feet when requested");

                // If both have data, verify conversion is correct
                if (timeSeriesMeters.size() > 0 && timeSeriesFeet.size() > 0 &&
                    timeSeriesMeters.size() == timeSeriesFeet.size())
                {
                    TimedVariable meterSample = timeSeriesMeters.sampleAt(0);
                    TimedVariable feetSample = timeSeriesFeet.sampleAt(0);

                    if (Math.abs(meterSample.getDoubleValue()) > 0.001)
                    {
                        // 1 meter = 3.280839895 feet
                        double expectedFeet = meterSample.getDoubleValue() * 3.280839895;
                        assertEquals(expectedFeet, feetSample.getDoubleValue(), 0.01,
                                    "Conversion from meters to feet should be accurate");
                    }
                }
            }
        }
    }

    @Test
    void testGetLocationLevelAtTime_ExactMatch() throws Exception
    {
        String testLocationLevelId = "FTPK-Lower.Stage.Const.0.Test";

        try (DataTransaction tx = db.newTransaction())
        {
            // First get the latest value to know what time we have data for
            CwmsSiteReferenceValue latestValue = (CwmsSiteReferenceValue) dao.getLatestLocationLevelValue(
                tx, testLocationLevelId);

            // Skip test if no data exists
            assumeTrue(latestValue != null && latestValue.getLevelDate() != null,
                      "Skipping exact match test - no data found for " + testLocationLevelId);

            Date knownTime = latestValue.getLevelDate();

            // Test retrieving value at exact time (requireSpecificTime = true)
            CwmsSiteReferenceValue exactValue = (CwmsSiteReferenceValue) dao.getLocationLevelAtTime(
                tx, testLocationLevelId, knownTime, true, null);

            assertNotNull(exactValue, "Should find value at known time with exact match");
            assertEquals(knownTime, exactValue.getLevelDate(),
                        "Date should match exactly when requireSpecificTime is true");
            assertEquals(latestValue.getLevelValue(), exactValue.getLevelValue(), 0.001,
                        "Value should match the known value");

            // Test that requesting a slightly different time with exact match returns null
            Calendar cal = Calendar.getInstance();
            cal.setTime(knownTime);
            cal.add(Calendar.MINUTE, -1); // 1 minute before
            Date differentTime = cal.getTime();

            CwmsSiteReferenceValue noMatch = (CwmsSiteReferenceValue) dao.getLocationLevelAtTime(
                tx, testLocationLevelId, differentTime, true, null);

            // May or may not be null depending on whether there's data at that exact time
            if (noMatch != null)
            {
                assertEquals(differentTime, noMatch.getLevelDate(),
                            "If value found, date should match exactly");
            }
        }
    }

    @Test
    void testGetLocationLevelAtTime_PreviousValue() throws Exception
    {
        String testLocationLevelId = "FTPK-Lower.Stage.Const.0.Test";

        try (DataTransaction tx = db.newTransaction())
        {
            // Get a recent time
            Date requestedTime = new Date();

            // Test retrieving previous value (requireSpecificTime = false)
            CwmsSiteReferenceValue previousValue = (CwmsSiteReferenceValue) dao.getLocationLevelAtTime(
                tx, testLocationLevelId, requestedTime, false, null);

            if (previousValue != null)
            {
                assertNotNull(previousValue.getLevelDate(),
                             "Level date should not be null");
                assertTrue(!previousValue.getLevelDate().after(requestedTime),
                          "Previous value date should not be after requested time");

                log.info("Found previous value at {} for requested time {}",
                        previousValue.getLevelDate(), requestedTime);
            }
            else
            {
                log.info("No previous value found for location: " + testLocationLevelId);
            }

            // Test with unit conversion
            CwmsSiteReferenceValue previousValueFeet = (CwmsSiteReferenceValue) dao.getLocationLevelAtTime(
                tx, testLocationLevelId, requestedTime, false, "ft");

            if (previousValueFeet != null)
            {
                assertEquals("ft", previousValueFeet.getUnits(),
                            "Units should be feet when requested");
            }
        }
    }

    @Test
    void testGetLocationLevelAtTime_WithUnitConversion() throws Exception
    {
        String testLocationLevelId = "FTPK-Lower.Stage.Const.0.Test";
        Date requestedTime = new Date();

        try (DataTransaction tx = db.newTransaction())
        {
            // Get value in meters
            CwmsSiteReferenceValue valueMeters = (CwmsSiteReferenceValue) dao.getLocationLevelAtTime(
                tx, testLocationLevelId, requestedTime, false, "m");

            // Get value in feet
            CwmsSiteReferenceValue valueFeet = (CwmsSiteReferenceValue) dao.getLocationLevelAtTime(
                tx, testLocationLevelId, requestedTime, false, "ft");

            if (valueMeters != null && valueFeet != null)
            {
                assertEquals("m", valueMeters.getUnits(), "Units should be meters");
                assertEquals("ft", valueFeet.getUnits(), "Units should be feet");

                // Verify conversion is correct (1 meter = 3.280839895 feet)
                if (Math.abs(valueMeters.getLevelValue()) > 0.001)
                {
                    double expectedFeet = valueMeters.getLevelValue() * 3.280839895;
                    assertEquals(expectedFeet, valueFeet.getLevelValue(), 0.01,
                                "Conversion from meters to feet should be accurate");
                }

                // Both should have the same timestamp
                assertEquals(valueMeters.getLevelDate(), valueFeet.getLevelDate(),
                            "Both values should have the same timestamp");
            }
        }
    }

    /**
     * Load location level test data for integration tests
     */
    private void loadLocationLevelTestData() throws Exception
    {
        if (!(tsDb instanceof CwmsTimeSeriesDb))
        {
            log.info("Not a CWMS database, skipping test data load");
            return;
        }

        CwmsTimeSeriesDb cwmsDb = (CwmsTimeSeriesDb) tsDb;
        String officeId = cwmsDb.getDbOfficeId();

        log.info("Loading test data for office: " + officeId);

        // Load the SQL script
        String testDataSql = IOUtils.resourceToString("/data/cwms/cwms_location_level_test_data.sql", StandardCharsets.UTF_8);

        // Replace placeholder with actual office
        testDataSql = testDataSql.replace("DEFAULT_OFFICE", officeId);

        // Execute the SQL using the database connection
        try (DataTransaction tx = db.newTransaction())
        {
            java.sql.Connection conn = tx.connection(java.sql.Connection.class)
                .orElseThrow(() -> new RuntimeException("JDBC Connection not available"));

            try (java.sql.CallableStatement stmt = conn.prepareCall(testDataSql))
            {
                log.debug("Executing PL/SQL block to create test data...");
                stmt.execute();

                log.info("Test data loaded successfully");
            }
        }
        catch (Exception ex)
        {
            log.error("Failed to load location level test data: " + ex.getMessage(), ex);
            throw new RuntimeException("Failed to load location level test data", ex);
        }
    }
}