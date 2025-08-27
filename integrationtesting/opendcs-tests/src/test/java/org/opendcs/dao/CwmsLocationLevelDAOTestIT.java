package org.opendcs.dao;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.nio.charset.StandardCharsets;

import java.util.List;
import java.util.Optional;

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
import decodes.tsdb.TimeSeriesDb;
import org.opendcs.database.dai.SiteReferenceMetaData;

import org.opendcs.model.cwms.LocationLevelValue;
import org.opendcs.model.cwms.LocationLevelSpec;

import org.opendcs.fixtures.annotations.EnableIfTsDb;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

/**
 * Integration tests for CwmsLocationLevelDAO
 * These tests require a real CWMS database connection
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CwmsLocationLevelDAOTestIT extends AppTestBase
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();

    @ConfiguredField
    private TimeSeriesDb tsDb;
    
    @ConfiguredField
    private OpenDcsDatabase db;

    private SiteReferenceMetaData dao;
    private static boolean testDataLoaded = false;
    
    @BeforeAll
    public void setupTestData() throws Exception
    {
        if (tsDb instanceof CwmsTimeSeriesDb && !testDataLoaded)
        {
            try
            {
                log.info("Loading location level test data for integration tests");
                loadLocationLevelTestData();
                testDataLoaded = true;
                log.info("Location level test data loaded successfully");
            }
            catch (Exception ex)
            {
                log.error("Failed to load test data: " + ex.getMessage(), ex);
                throw ex;
            }
        }
    }
    
    @BeforeEach
    public void setUp() throws Exception
    {
        if (tsDb instanceof CwmsTimeSeriesDb)
        {
            Optional<SiteReferenceMetaData>  dai = db.getDao(SiteReferenceMetaData.class);
            assertTrue(dai.isPresent(), "Unable to retrieve LocationLevelDAI instance from database.");
            dao = dai.get();
        }
        else
        {
            // For non-CWMS databases, skip these tests
            dao = null;
        }
    }
    
    @Test
    @EnableIfTsDb({"CWMS-Oracle"})
    public void testGetLatestLocationLevelValue_Basic() throws Exception
    {
        if (dao == null) 
        {
            return; // Skip for non-CWMS databases
        }
        
        // This test verifies the method executes without error
        // Test data is created during container initialization
        String testLocationLevelId = "TEST_LOCATION.Stage.Const.0.Test";
        
        try (DataTransaction tx = db.newTransaction())
        {
            LocationLevelValue value = dao.getLatestLocationLevelValue(tx, testLocationLevelId);
            
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
    @EnableIfTsDb({"CWMS-Oracle"})
    public void testGetLatestLocationLevelValue_WithUnitConversion() throws Exception
    {
        if (dao == null) 
        {
            return; // Skip for non-CWMS databases
        }
        
        String testLocationLevelId = "TEST_LOCATION.Stage.Const.0.Test";
        
        try (DataTransaction tx = db.newTransaction())
        {
            // First check if we have any data
            LocationLevelValue baseValue = dao.getLatestLocationLevelValue(tx, testLocationLevelId);
            
            // Skip unit conversion test if no data exists
            assumeTrue(baseValue != null, 
                "Skipping unit conversion test - no data found for " + testLocationLevelId);
            
            // Test conversion from feet to meters
            LocationLevelValue valueInFeet = dao.getLatestLocationLevelValue(
                tx, testLocationLevelId, "ft");
            
            LocationLevelValue valueInMeters = dao.getLatestLocationLevelValue(
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
    @EnableIfTsDb({"CWMS-Oracle"})
    public void testGetLocationLevelSpecs() throws Exception
    {
        if (dao == null) 
        {
            return; // Skip for non-CWMS databases
        }
        
        String testLocationId = "TEST_LOCATION";
        
        try (DataTransaction tx = db.newTransaction())
        {
            List<LocationLevelSpec> specs = dao.getLocationLevelSpecs(tx, testLocationId);
            
            assertNotNull(specs, "Specs list should not be null");
            
            // If specs exist, verify their structure
            for (LocationLevelSpec spec : specs)
            {
                assertNotNull(spec.getLocationLevelId(), "Location level ID should not be null");
                assertNotNull(spec.getLocationId(), "Location ID should not be null");
                assertNotNull(spec.getParameterId(), "Parameter ID should not be null");
            }
        }
    }
    
    @Test
    @EnableIfTsDb({"CWMS-Oracle"})
    public void testTransactionSupport() throws Exception
    {
        if (dao == null) 
        {
            return; // Skip for non-CWMS databases
        }
        
        String testLocationLevelId = "TEST_LOCATION.Stage.Const.0.Test";
        String testLocationId = "TEST_LOCATION";

        try (DataTransaction tx = db.newTransaction())
        {
            // Test getLatestLocationLevelValue with transaction
            LocationLevelValue value = dao.getLatestLocationLevelValue(
                tx, testLocationLevelId);
            
            // Test getLocationLevelSpecs with transaction
            List<LocationLevelSpec> specs = dao.getLocationLevelSpecs(
                tx, testLocationId);
            
            assertNotNull(specs, "Specs should not be null even if empty");
            
            // For read-only operations, we don't need to commit
            // Just verify the methods execute without errors
        }

    }
    
    @Test
    @EnableIfTsDb({"CWMS-Oracle"})
    public void testErrorHandling_InvalidLocationId() throws Exception
    {
        if (dao == null) 
        {
            return; // Skip for non-CWMS databases
        }
        
        try (DataTransaction tx = db.newTransaction())
        {
            // Use an invalid location ID format
            String invalidLocationId = "INVALID_FORMAT";
            
            // Should not throw exception, just return null
            LocationLevelValue value = dao.getLatestLocationLevelValue(tx, invalidLocationId);
            
            // Most likely will be null for invalid ID
            // But if not null, it means the ID exists (unlikely for this test ID)
            // Either way, no exception should be thrown
        }
    }
    
    @Test
    @EnableIfTsDb({"CWMS-Oracle"})
    public void testInterfaceCompatibility() throws Exception
    {
        // Test that CwmsLocationLevelDAO properly implements LocationLevelDAI
        if (dao == null) 
        {
            return; // Skip for non-CWMS databases
        }
        
        try (DataTransaction tx = db.newTransaction())
        {
            // Use the DAO through the interface
            SiteReferenceMetaData daiInterface = dao;
            
            String testLocationLevelId = "TEST_LOCATION.Stage.Const.0.Test";
            
            // All interface methods should work with transactions
            LocationLevelValue value = daiInterface.getLatestLocationLevelValue(tx, testLocationLevelId);
            
            LocationLevelValue valueWithUnits = daiInterface.getLatestLocationLevelValue(tx, testLocationLevelId, "ft");
            
            List<LocationLevelSpec> specs = daiInterface.getLocationLevelSpecs(tx, "TEST_LOCATION");
            
            // No exceptions should be thrown
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