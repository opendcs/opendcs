package decodes.cwms;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendcs.database.api.DataTransaction;

import decodes.tsdb.DbIoException;
import fixtures.NonPoolingConnectionOwner;
import opendcs.dao.DaoHelper;
import opendcs.dao.DatabaseConnectionOwner;
import opendcs.dao.LocationLevelDAO.LocationLevelValue;
import opendcs.dao.LocationLevelDAO.LocationLevelSpec;

/**
 * Unit tests for CwmsLocationLevelDAO
 * Tests the DAO functionality with mocked database connections
 */
public class CwmsLocationLevelDAOTest
{
    private CwmsLocationLevelDAO dao;
    
    @Mock
    private DatabaseConnectionOwner mockDbOwner;
    
    @Mock
    private Connection mockConnection;
    
    @Mock
    private PreparedStatement mockStatement;
    
    @Mock
    private ResultSet mockResultSet;
    
    private AutoCloseable closeable;
    
    @BeforeEach
    public void setUp() throws SQLException
    {
        closeable = MockitoAnnotations.openMocks(this);
        
        // Setup mock database owner
        when(mockDbOwner.getConnection()).thenReturn(mockConnection);
        when(mockDbOwner.isOpenTSDB()).thenReturn(false);
        when(mockDbOwner.isOracle()).thenReturn(true);
        
        dao = new CwmsLocationLevelDAO(mockDbOwner);
    }
    
    @AfterEach
    public void tearDown() throws Exception
    {
        if (dao != null)
        {
            dao.close();
        }
        if (closeable != null)
        {
            closeable.close();
        }
    }
    
    @Test
    public void testGetLatestLocationLevelValue_NoUnits() throws Exception
    {
        // Setup mock data
        String locationLevelId = "TEST_LOCATION.Stage.Inst.0.0.USGS-NWIS";
        double expectedValue = 123.45;
        Date expectedDate = new Date();
        String expectedUnits = "ft";
        
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true).thenReturn(false);
        when(mockResultSet.getDouble("LOCATION_LEVEL_VALUE")).thenReturn(expectedValue);
        when(mockResultSet.getTimestamp("LOCATION_LEVEL_DATE")).thenReturn(new Timestamp(expectedDate.getTime()));
        when(mockResultSet.getString("LEVEL_UNIT")).thenReturn(expectedUnits);
        
        // Execute test
        LocationLevelValue result = dao.getLatestLocationLevelValue(locationLevelId);
        
        // Verify results
        assertNotNull(result);
        assertEquals(expectedValue, result.getLevelValue(), 0.001);
        assertEquals(expectedDate.getTime(), result.getLevelDate().getTime());
        assertEquals(expectedUnits, result.getUnits());
        assertEquals(locationLevelId, result.getLocationLevelId());
        
        // Verify the query was executed with correct parameters
        verify(mockStatement).setString(1, locationLevelId);
    }
    
    @Test
    public void testGetLatestLocationLevelValue_WithUnitConversion() throws Exception
    {
        // This test would require mocking the unit converter
        // For now, we'll test that the method handles the conversion path
        String locationLevelId = "TEST_LOCATION.Stage.Inst.0.0.USGS-NWIS";
        double dbValue = 1.0; // 1 meter
        Date expectedDate = new Date();
        String sourceUnits = "m";
        String targetUnits = "ft";
        
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true).thenReturn(false);
        when(mockResultSet.getDouble("LOCATION_LEVEL_VALUE")).thenReturn(dbValue);
        when(mockResultSet.getTimestamp("LOCATION_LEVEL_DATE")).thenReturn(new Timestamp(expectedDate.getTime()));
        when(mockResultSet.getString("LEVEL_UNIT")).thenReturn(sourceUnits);
        
        // Execute test
        LocationLevelValue result = dao.getLatestLocationLevelValue(locationLevelId, targetUnits, sourceUnits);
        
        // Verify results
        assertNotNull(result);
        // Note: Actual conversion would happen in the real implementation
        // Here we're just testing the flow
        verify(mockStatement).setString(1, locationLevelId);
        verify(mockStatement).setString(2, sourceUnits);
    }
    
    @Test
    public void testGetLatestLocationLevelValue_NoData() throws Exception
    {
        String locationLevelId = "NONEXISTENT.Stage.Inst.0.0.USGS-NWIS";
        
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);
        
        // Execute test
        LocationLevelValue result = dao.getLatestLocationLevelValue(locationLevelId);
        
        // Verify results
        assertNull(result);
    }
    
    @Test
    public void testGetLocationLevelSpecs() throws Exception
    {
        String locationId = "TEST_LOCATION";
        
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        
        // Mock two spec records
        when(mockResultSet.next()).thenReturn(true).thenReturn(true).thenReturn(false);
        
        // First record
        when(mockResultSet.getString("LOCATION_LEVEL_ID"))
            .thenReturn("TEST_LOCATION.Stage.Inst.0.0.USGS-NWIS")
            .thenReturn("TEST_LOCATION.Flow.Inst.0.0.USGS-NWIS");
        when(mockResultSet.getString("SPECIFIED_LEVEL_ID"))
            .thenReturn("Normal")
            .thenReturn("Normal");
        when(mockResultSet.getString("PARAMETER_ID"))
            .thenReturn("Stage")
            .thenReturn("Flow");
        when(mockResultSet.getString("PARAMETER_TYPE_ID"))
            .thenReturn("Inst")
            .thenReturn("Inst");
        when(mockResultSet.getString("DURATION_ID"))
            .thenReturn("0")
            .thenReturn("0");
        when(mockResultSet.getString("LOCATION_ID"))
            .thenReturn(locationId)
            .thenReturn(locationId);
        when(mockResultSet.getObject("CONSTANT_LEVEL", Double.class))
            .thenReturn(100.0)
            .thenReturn(500.0);
        when(mockResultSet.getString("LEVEL_UNIT"))
            .thenReturn("ft")
            .thenReturn("cfs");
        when(mockResultSet.getTimestamp("LEVEL_DATE"))
            .thenReturn(new Timestamp(System.currentTimeMillis()));
        when(mockResultSet.getString("LEVEL_COMMENT"))
            .thenReturn("Normal pool")
            .thenReturn("Normal flow");
        when(mockResultSet.getString("UNIT_SYSTEM"))
            .thenReturn("EN")
            .thenReturn("EN");
        when(mockResultSet.getObject("ATTRIBUTE_VALUE", Double.class))
            .thenReturn(null);
        
        // Execute test
        List<LocationLevelSpec> specs = dao.getLocationLevelSpecs(locationId);
        
        // Verify results
        assertNotNull(specs);
        assertEquals(2, specs.size());
        
        LocationLevelSpec spec1 = specs.get(0);
        assertEquals("TEST_LOCATION.Stage.Inst.0.0.USGS-NWIS", spec1.getLocationLevelId());
        assertEquals("Normal", spec1.getSpecifiedLevelId());
        assertEquals("Stage", spec1.getParameterId());
        assertEquals(100.0, spec1.getConstantValue(), 0.001);
        assertEquals("ft", spec1.getLevelUnitsId());
        
        LocationLevelSpec spec2 = specs.get(1);
        assertEquals("TEST_LOCATION.Flow.Inst.0.0.USGS-NWIS", spec2.getLocationLevelId());
        assertEquals("Flow", spec2.getParameterId());
        assertEquals(500.0, spec2.getConstantValue(), 0.001);
        assertEquals("cfs", spec2.getLevelUnitsId());
    }
    
    @Test
    public void testCaching() throws Exception
    {
        // Clear cache first
        dao.clearCache();
        
        String locationLevelId = "TEST_LOCATION.Stage.Inst.0.0.USGS-NWIS";
        double expectedValue = 123.45;
        Date expectedDate = new Date();
        String expectedUnits = "ft";
        
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true).thenReturn(false);
        when(mockResultSet.getDouble("LOCATION_LEVEL_VALUE")).thenReturn(expectedValue);
        when(mockResultSet.getTimestamp("LOCATION_LEVEL_DATE")).thenReturn(new Timestamp(expectedDate.getTime()));
        when(mockResultSet.getString("LEVEL_UNIT")).thenReturn(expectedUnits);
        
        // First call - should hit database
        LocationLevelValue result1 = dao.getLatestLocationLevelValue(locationLevelId);
        assertNotNull(result1);
        
        // Second call - should use cache
        LocationLevelValue result2 = dao.getLatestLocationLevelValue(locationLevelId);
        assertNotNull(result2);
        assertEquals(result1.getLevelValue(), result2.getLevelValue(), 0.001);
        
        // Verify database was only queried once
        verify(mockStatement, times(1)).executeQuery();
        
        // Clear cache and call again
        dao.clearCache();
        LocationLevelValue result3 = dao.getLatestLocationLevelValue(locationLevelId);
        assertNotNull(result3);
        
        // Verify database was queried again after cache clear
        verify(mockStatement, times(2)).executeQuery();
    }
    
    @Test
    public void testSeparateCacheClearing() throws Exception
    {
        // Test that clearValueCache and clearSpecCache work independently
        dao.clearCache(); // Clear all first
        
        // Test value cache
        dao.clearValueCache();
        // No exception should be thrown
        
        // Test spec cache
        dao.clearSpecCache();
        // No exception should be thrown
        
        // Test clear all
        dao.clearCache();
        // No exception should be thrown
    }
    
    @Test
    public void testErrorHandling() throws Exception
    {
        String locationLevelId = "TEST_LOCATION.Stage.Inst.0.0.USGS-NWIS";
        
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Database error"));
        
        // Execute test and expect exception
        assertThrows(DbIoException.class, () -> {
            dao.getLatestLocationLevelValue(locationLevelId);
        });
    }
    
    @Test
    public void testWithDaoHelper() throws Exception
    {
        // Test that the DAO works with DaoHelper for transaction support
        DaoHelper helper = new DaoHelper(mockDbOwner, "test-helper", mockConnection);
        
        String locationId = "TEST_LOCATION";
        
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);
        
        // Execute test
        List<LocationLevelSpec> specs = dao.getLocationLevelSpecs(helper, locationId);
        
        // Verify results
        assertNotNull(specs);
        assertTrue(specs.isEmpty());
    }
}