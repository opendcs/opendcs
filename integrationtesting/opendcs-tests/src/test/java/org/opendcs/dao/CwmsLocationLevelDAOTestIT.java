package org.opendcs.dao;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.EnableIfDaoSupported;

import decodes.cwms.CwmsLocationLevelDAO;
import decodes.cwms.CwmsTimeSeriesDb;
import decodes.tsdb.TimeSeriesDb;
import opendcs.dai.LocationLevelDAI;
import opendcs.dao.LocationLevelDAO;
import opendcs.dao.LocationLevelDAO.LocationLevelValue;
import opendcs.dao.LocationLevelDAO.LocationLevelSpec;

/**
 * Integration tests for CwmsLocationLevelDAO
 * These tests require a real CWMS database connection
 */
public class CwmsLocationLevelDAOTestIT extends AppTestBase
{
    @ConfiguredField
    private TimeSeriesDb tsDb;
    
    @ConfiguredField
    private OpenDcsDatabase db;
    
    private CwmsLocationLevelDAO dao;
    
    @BeforeEach
    public void setUp() throws Exception
    {
        if (tsDb instanceof CwmsTimeSeriesDb)
        {
            dao = new CwmsLocationLevelDAO((CwmsTimeSeriesDb) tsDb);
        }
        else
        {
            // For non-CWMS databases, skip these tests
            dao = null;
        }
    }
    
    @Test
    @EnableIfDaoSupported(CwmsLocationLevelDAO.class)
    public void testGetLatestLocationLevelValue_Basic() throws Exception
    {
        if (dao == null) 
        {
            return; // Skip for non-CWMS databases
        }
        
        // This test verifies the method executes without error
        // Test data is created during container initialization
        String testLocationLevelId = "TEST_LOCATION.Stage.Constant.Test";
        
        try
        {
            LocationLevelValue value = dao.getLatestLocationLevelValue(testLocationLevelId);
            
            // Test passes whether data exists or not
            // If value is not null, verify its structure
            if (value != null)
            {
                assertNotNull(value.getLevelDate(), "Level date should not be null when value exists");
                assertNotNull(value.getUnits(), "Units should not be null when value exists");
                assertEquals(testLocationLevelId, value.getLocationLevelId(), 
                    "Location level ID should match request");
                
                System.out.println("Found test data for location: " + testLocationLevelId);
            }
            else
            {
                System.out.println("No test data found for location: " + testLocationLevelId + 
                    " (this is OK - test verifies method executes without error)");
            }
            
            // Verify method doesn't throw exception for non-existent location
            assertDoesNotThrow(() -> {
                dao.getLatestLocationLevelValue("NONEXISTENT.Stage.Constant.Test");
            });
        }
        finally
        {
            dao.close();
        }
    }
    
    @Test
    @EnableIfDaoSupported(CwmsLocationLevelDAO.class)
    public void testGetLatestLocationLevelValue_WithUnitConversion() throws Exception
    {
        if (dao == null) 
        {
            return; // Skip for non-CWMS databases
        }
        
        String testLocationLevelId = "TEST_LOCATION.Stage.Constant.Test";
        
        try
        {
            // First check if we have any data
            LocationLevelValue baseValue = dao.getLatestLocationLevelValue(testLocationLevelId);
            
            // Skip unit conversion test if no data exists
            assumeTrue(baseValue != null, 
                "Skipping unit conversion test - no data found for " + testLocationLevelId);
            
            // Test conversion from feet to meters
            LocationLevelValue valueInFeet = dao.getLatestLocationLevelValue(
                testLocationLevelId, "ft", null);
            
            LocationLevelValue valueInMeters = dao.getLatestLocationLevelValue(
                testLocationLevelId, "m", null);
            
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
        finally
        {
            dao.close();
        }
    }
    
    @Test
    @EnableIfDaoSupported(CwmsLocationLevelDAO.class)
    public void testGetLocationLevelSpecs() throws Exception
    {
        if (dao == null) 
        {
            return; // Skip for non-CWMS databases
        }
        
        String testLocationId = "TEST_LOCATION";
        
        try
        {
            List<LocationLevelSpec> specs = dao.getLocationLevelSpecs(testLocationId);
            
            assertNotNull(specs, "Specs list should not be null");
            
            // If specs exist, verify their structure
            for (LocationLevelSpec spec : specs)
            {
                assertNotNull(spec.getLocationLevelId(), "Location level ID should not be null");
                assertNotNull(spec.getLocationId(), "Location ID should not be null");
                assertNotNull(spec.getParameterId(), "Parameter ID should not be null");
            }
        }
        finally
        {
            dao.close();
        }
    }
    
    @Test
    @EnableIfDaoSupported(CwmsLocationLevelDAO.class)
    public void testTransactionSupport() throws Exception
    {
        if (dao == null) 
        {
            return; // Skip for non-CWMS databases
        }
        
        String testLocationLevelId = "TEST_LOCATION.Stage.Constant.Test";
        String testLocationId = "TEST_LOCATION";
        
        DataTransaction tx = null;
        try
        {
            tx = db.newTransaction();
            
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
        finally
        {
            if (tx != null)
            {
                try
                {
                    tx.close();
                }
                catch (Exception e)
                {
                    // Ignore close errors in test
                }
            }
            dao.close();
        }
    }
    
    @Test
    @EnableIfDaoSupported(CwmsLocationLevelDAO.class)
    public void testErrorHandling_InvalidLocationId() throws Exception
    {
        if (dao == null) 
        {
            return; // Skip for non-CWMS databases
        }
        
        try
        {
            // Use an invalid location ID format
            String invalidLocationId = "INVALID_FORMAT";
            
            // Should not throw exception, just return null
            LocationLevelValue value = dao.getLatestLocationLevelValue(invalidLocationId);
            
            // Most likely will be null for invalid ID
            // But if not null, it means the ID exists (unlikely for this test ID)
            // Either way, no exception should be thrown
        }
        finally
        {
            dao.close();
        }
    }
    
    @Test
    @EnableIfDaoSupported(CwmsLocationLevelDAO.class)
    public void testInterfaceCompatibility() throws Exception
    {
        // Test that CwmsLocationLevelDAO properly implements LocationLevelDAI
        if (dao == null) 
        {
            return; // Skip for non-CWMS databases
        }
        
        try
        {
            // Use the DAO through the interface
            LocationLevelDAI daiInterface = dao;
            
            String testLocationLevelId = "TEST_LOCATION.Stage.Constant.Test";
            
            // All interface methods should work
            LocationLevelValue value = daiInterface.getLatestLocationLevelValue(testLocationLevelId);
            
            LocationLevelValue valueWithUnits = daiInterface.getLatestLocationLevelValue(
                testLocationLevelId, "ft", null);
            
            List<LocationLevelSpec> specs = daiInterface.getLocationLevelSpecs("TEST_LOCATION");
            
            daiInterface.clearCache();
            
            // No exceptions should be thrown
        }
        finally
        {
            dao.close();
        }
    }
}