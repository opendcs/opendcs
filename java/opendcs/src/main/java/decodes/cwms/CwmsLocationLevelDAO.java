package decodes.cwms;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import opendcs.dao.*;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.slf4j.Logger;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;

import decodes.db.Database;
import decodes.db.EngineeringUnit;
import decodes.db.UnitConverter;
import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;
import decodes.util.DecodesException;
import ilex.var.NoConversionException;

/**
 * Data Access Object for CWMS Location Level data.
 * This class provides methods to read and store location level data
 * directly from/to the CWMS database.
 *
 * Extends LocationLevelDAO base class and implements stateless pattern.
 */
public class CwmsLocationLevelDAO extends LocationLevelDAO
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
    
    private String dbOfficeId = null;
    
    // Base queries for location level operations using AV_LOCATION_LEVEL view
    private static final String LOCATION_LEVEL_SPEC_QUERY = 
        "SELECT LOCATION_LEVEL_ID, SPECIFIED_LEVEL_ID, PARAMETER_ID, " +
        "PARAMETER_TYPE_ID, DURATION_ID, LOCATION_ID, CONSTANT_LEVEL, " +
        "LEVEL_UNIT, LEVEL_DATE, LEVEL_COMMENT, UNIT_SYSTEM, " +
        "ATTRIBUTE_VALUE, ATTRIBUTE_UNIT, ATTRIBUTE_PARAMETER_ID " +
        "FROM CWMS_20.AV_LOCATION_LEVEL " +
        "WHERE LOCATION_ID = ?";
    
    /**
     * Constructor
     * @param tsdb The database connection owner
     */
    public CwmsLocationLevelDAO(DatabaseConnectionOwner tsdb)
    {
        super(tsdb, "CwmsLocationLevelDAO");
        if (tsdb instanceof CwmsTimeSeriesDb)
        {
            this.dbOfficeId = ((CwmsTimeSeriesDb)tsdb).getDbOfficeId();
        }
    }
    
    /**
     * Constructor with existing connection for transactions
     * @param tsdb The database connection owner
     * @param conn Existing database connection to use
     */
    public CwmsLocationLevelDAO(DatabaseConnectionOwner tsdb, Connection conn)
    {
        super(tsdb, "CwmsLocationLevelDAO", conn);
        if (tsdb instanceof CwmsTimeSeriesDb)
        {
            this.dbOfficeId = ((CwmsTimeSeriesDb)tsdb).getDbOfficeId();
        }
    }
    
    /**
     * Read location level specification for a given location
     * @param locationId The CWMS location identifier
     * @return List of LocationLevelSpec objects
     * @throws DbIoException on database error
     */
    @Override
    public List<LocationLevelSpec> getLocationLevelSpecs(String locationId) 
        throws DbIoException
    {
        try
        {
            return executeLocationLevelSpecsQuery(this, locationId);
        }
        catch(SQLException ex)
        {
            throw new DbIoException("Error reading location level specs for " + locationId, ex);
        }
    }
    
    /**
     * Read location level specification for a given location with transaction support
     * @param tx The data transaction
     * @param locationId The CWMS location identifier
     * @return List of LocationLevelSpec objects
     * @throws OpenDcsDataException on database error
     */
    @Override
    public List<LocationLevelSpec> getLocationLevelSpecs(DataTransaction tx, String locationId)
        throws OpenDcsDataException
    {
        Connection conn = tx.connection(Connection.class)
            .orElseThrow(() -> new OpenDcsDataException("JDBC Connection not available in this transaction."));
        
        try (DaoHelper helper = new DaoHelper(this.db, module + "-transaction", conn))
        {
            return executeLocationLevelSpecsQuery(helper, locationId);
        }
        catch (SQLException ex)
        {
            throw new OpenDcsDataException("Error reading location level specs for " + locationId, ex);
        }
    }
    
    /**
     * Read location level specification for a given location with DaoHelper
     * This method is kept for backward compatibility and internal use
     * @param helper The DaoHelper with transaction connection
     * @param locationId The CWMS location identifier
     * @return List of LocationLevelSpec objects
     * @throws DbIoException on database error
     */
    public List<LocationLevelSpec> getLocationLevelSpecs(DaoHelper helper, String locationId) 
        throws DbIoException
    {
        try
        {
            return executeLocationLevelSpecsQuery(helper, locationId);
        }
        catch(SQLException ex)
        {
            throw new DbIoException("Error reading location level specs for " + locationId, ex);
        }
    }
    
    /**
     * Common helper method to execute location level specs query
     * @param dao The DAO to use for the query (can be this or a DaoHelper)
     * @param locationId The CWMS location identifier
     * @return List of LocationLevelSpec objects
     * @throws SQLException on database error
     */
    private List<LocationLevelSpec> executeLocationLevelSpecsQuery(DaoBase dao, String locationId)
        throws SQLException
    {
        List<LocationLevelSpec> specs = new ArrayList<>();
        
        dao.doQuery(LOCATION_LEVEL_SPEC_QUERY, rs -> {
            LocationLevelSpec spec = new LocationLevelSpec();
            spec.setLocationLevelId(rs.getString("LOCATION_LEVEL_ID"));
            spec.setSpecifiedLevelId(rs.getString("SPECIFIED_LEVEL_ID"));
            spec.setParameterId(rs.getString("PARAMETER_ID"));
            spec.setParameterTypeId(rs.getString("PARAMETER_TYPE_ID"));
            spec.setDurationId(rs.getString("DURATION_ID"));
            spec.setLocationId(rs.getString("LOCATION_ID"));
            
            // Handle potential null CONSTANT_LEVEL
            Double constantLevel = rs.getObject("CONSTANT_LEVEL", Double.class);
            spec.setConstantValue(constantLevel != null ? constantLevel : 0.0);
            
            spec.setLevelUnitsId(rs.getString("LEVEL_UNIT"));
            spec.setLevelDate(rs.getTimestamp("LEVEL_DATE"));
            spec.setLevelComment(rs.getString("LEVEL_COMMENT"));
            spec.setUnitSystem(rs.getString("UNIT_SYSTEM"));
            
            // Handle attribute fields if needed
            Double attrValue = rs.getObject("ATTRIBUTE_VALUE", Double.class);
            if (attrValue != null) {
                spec.setAttributeValue(attrValue);
                spec.setAttributeUnit(rs.getString("ATTRIBUTE_UNIT"));
                spec.setAttributeParameterId(rs.getString("ATTRIBUTE_PARAMETER_ID"));
            }
            
            specs.add(spec);
            specCache.put(spec);
        }, locationId);
        
        return specs;
    }

    
    /**
     * Query the database for the latest location level value
     * Implementation of abstract method from LocationLevelDAO
     * @param locationLevelId The location level identifier
     * @param targetUnits The desired units for the value
     * @param sourceUnitsIn The source units to filter by
     * @return The latest LocationLevelValue or null if none found
     * @throws DbIoException on database error
     */
    @Override
    protected LocationLevelValue queryLatestLocationLevelValue(
        String locationLevelId, String targetUnits, String sourceUnitsIn)
        throws DbIoException
    {
        try
        {
            return executeLocationLevelQuery(this, locationLevelId, targetUnits, sourceUnitsIn);
        }
        catch(SQLException ex)
        {
            throw new DbIoException("Error getting latest location level value for " + locationLevelId, ex);
        }
    }
    
    /**
     * Convert value from one unit to another using OpenDCS unit converter
     * @param value The value to convert
     * @param fromUnits The source units
     * @param toUnits The target units
     * @return The converted value
     * @throws NoConversionException if conversion is not possible
     * @throws DecodesException if there's an error in the conversion process
     */
    private double convertUnits(double value, String fromUnits, String toUnits) 
        throws NoConversionException, DecodesException
    {
        if (fromUnits == null || toUnits == null || fromUnits.equalsIgnoreCase(toUnits))
        {
            return value;
        }
        
        EngineeringUnit euFrom = EngineeringUnit.getEngineeringUnit(fromUnits);
        EngineeringUnit euTo = EngineeringUnit.getEngineeringUnit(toUnits);
        
        UnitConverter converter = Database.getDb().unitConverterSet.get(euFrom, euTo);
        if (converter == null)
        {
            throw new NoConversionException("No converter available from " + fromUnits + " to " + toUnits);
        }
        
        return converter.convert(value);
    }
    
    /**
     * Query the database for the latest location level value using a helper
     * Implementation of abstract method from LocationLevelDAO for transaction support
     * @param helper The DaoHelper with transaction connection
     * @param locationLevelId The location level identifier
     * @param targetUnits The desired units for the value
     * @param sourceUnitsIn The source units to filter by
     * @return The latest LocationLevelValue or null if none found
     * @throws DbIoException on database error
     */
    @Override
    protected LocationLevelValue queryLatestLocationLevelValue(
        DaoHelper helper, String locationLevelId, String targetUnits, String sourceUnitsIn)
        throws DbIoException
    {
        try
        {
            return executeLocationLevelQuery(helper, locationLevelId, targetUnits, sourceUnitsIn);
        }
        catch(SQLException ex)
        {
            throw new DbIoException("Error getting latest location level value for " + locationLevelId, ex);
        }
    }
    
    /**
     * Common helper method to execute location level query using either DaoBase or DaoHelper
     * @param dao The DAO to use for the query (can be this or a DaoHelper)
     * @param locationLevelId The location level identifier
     * @param targetUnits The desired units for the value
     * @param sourceUnitsIn The source units to filter by
     * @return The latest LocationLevelValue or null if none found
     * @throws SQLException on database error
     */
    private LocationLevelValue executeLocationLevelQuery(DaoBase dao, String locationLevelId,
                                                         String targetUnits, String sourceUnitsIn) throws SQLException
    {
        // Build query based on whether we're filtering by source units
        String query;
        Object[] params;
        
        if (sourceUnitsIn != null && !sourceUnitsIn.isEmpty())
        {
            query = 
                "SELECT CONSTANT_LEVEL AS LOCATION_LEVEL_VALUE, " +
                "LEVEL_DATE AS LOCATION_LEVEL_DATE, LEVEL_UNIT " +
                "FROM CWMS_20.AV_LOCATION_LEVEL " +
                "WHERE LOCATION_LEVEL_ID = ? " +
                "AND LEVEL_UNIT = ? " +
                "ORDER BY LEVEL_DATE DESC " +
                "FETCH FIRST 1 ROWS ONLY";
            params = new Object[] { locationLevelId, sourceUnitsIn };
        }
        else
        {
            query = 
                "SELECT CONSTANT_LEVEL AS LOCATION_LEVEL_VALUE, " +
                "LEVEL_DATE AS LOCATION_LEVEL_DATE, LEVEL_UNIT " +
                "FROM CWMS_20.AV_LOCATION_LEVEL " +
                "WHERE LOCATION_LEVEL_ID = ? " +
                "ORDER BY LEVEL_DATE DESC " +
                "FETCH FIRST 1 ROWS ONLY";
            params = new Object[] { locationLevelId };
        }
        
        final String[] sourceUnits = {sourceUnitsIn};
        
        return dao.getSingleResultOr(query, rs -> {
            LocationLevelValue value = new LocationLevelValue();
            double levelValue = rs.getDouble("LOCATION_LEVEL_VALUE");
            String dbUnits = rs.getString("LEVEL_UNIT");
            
            // Use database units if sourceUnits not specified
            if (sourceUnits[0] == null || sourceUnits[0].isEmpty())
            {
                sourceUnits[0] = dbUnits;
            }
            
            // Perform unit conversion if needed
            if (targetUnits != null && !targetUnits.isEmpty() && 
                sourceUnits[0] != null && !sourceUnits[0].equalsIgnoreCase(targetUnits))
            {
                try
                {
                    levelValue = convertUnits(levelValue, sourceUnits[0], targetUnits);
                    value.setUnits(targetUnits);
                }
                catch (Exception ex)
                {
                    log.warn("Failed to convert units from {} to {}: {}",
                            sourceUnits[0], targetUnits, ex.getMessage());
                    value.setUnits(sourceUnits[0]);
                }
            }
            else
            {
                value.setUnits(sourceUnits[0]);
            }
            
            value.setLevelValue(levelValue);
            value.setLevelDate(rs.getTimestamp("LOCATION_LEVEL_DATE"));
            value.setLocationLevelId(locationLevelId);
            value.setQualityCode(0);
            return value;
        }, null, params);
    }
    
    
}