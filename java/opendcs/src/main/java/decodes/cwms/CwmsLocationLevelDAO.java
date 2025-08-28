package decodes.cwms;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import opendcs.dao.DatabaseConnectionOwner;
import opendcs.dao.DaoHelper;
import org.opendcs.database.dai.SiteReferenceMetaData;
import org.opendcs.database.SimpleTransaction;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.slf4j.Logger;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;

import decodes.db.Database;
import decodes.db.EngineeringUnit;
import decodes.db.UnitConverter;
import decodes.util.DecodesException;
import ilex.var.NoConversionException;

import org.opendcs.model.cwms.CwmsSiteReferenceValue;
import org.opendcs.model.cwms.CwmsSiteReferenceSpecification;

import opendcs.dao.DaoBase;

/**
 * Data Access Object for CWMS Location Level data.
 * This class provides methods to read and store location level data
 * directly from/to the CWMS database.
 *
 * Implements LocationLevelDAI interface following stateless pattern.
 * All operations require a DataTransaction for true stateless behavior.
 */
public class CwmsLocationLevelDAO extends DaoBase implements SiteReferenceMetaData
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
    private static final String MODULE = "CwmsLocationLevelDAO";
    
    private final DatabaseConnectionOwner db;
    private final String dbOfficeId;
    
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
     * @param db The database connection owner
     */
    public CwmsLocationLevelDAO(DatabaseConnectionOwner db)
    {
        super(db, MODULE);
        this.db = db;
        if (db instanceof CwmsTimeSeriesDb)
        {
            this.dbOfficeId = ((CwmsTimeSeriesDb)db).getDbOfficeId();
        }
        else
        {
            this.dbOfficeId = null;
        }
    }
    
    /**
     * Get a transaction for database operations
     * @return A new DataTransaction
     * @throws OpenDcsDataException if unable to get connection
     */
    @Override
    public DataTransaction getTransaction() throws OpenDcsDataException
    {
        try
        {
            return new SimpleTransaction(db.getConnection());
        }
        catch (SQLException ex)
        {
            throw new OpenDcsDataException("Unable to get connection.", ex);
        }
    }
    
    /**
     * Get location level value with transaction support
     * @param tx The data transaction
     * @param locationLevelId The location level identifier
     * @return The latest LocationLevelValue or null if none found
     * @throws OpenDcsDataException on database error
     */
    @Override
    public CwmsSiteReferenceValue getLatestLocationLevelValue(DataTransaction tx, String locationLevelId)
        throws OpenDcsDataException
    {
        return getLatestLocationLevelValue(tx, locationLevelId, null);
    }
    
    /**
     * Get location level value with transaction support and unit conversion
     * @param tx The data transaction
     * @param locationLevelId The location level identifier
     * @param targetUnits The desired units for the value
     * @return The latest LocationLevelValue or null if none found
     * @throws OpenDcsDataException on database error
     */
    @Override
    public CwmsSiteReferenceValue getLatestLocationLevelValue(DataTransaction tx, String locationLevelId,
                                                              String targetUnits) throws OpenDcsDataException
    {
        Connection conn = tx.connection(Connection.class)
            .orElseThrow(() -> new OpenDcsDataException("JDBC Connection not available in this transaction."));
        
        try (DaoHelper helper = new DaoHelper(this.db, MODULE + "-transaction", conn))
        {
            return executeLocationLevelQuery(helper, locationLevelId, targetUnits);
        }
        catch (SQLException ex)
        {
            throw new OpenDcsDataException("Error retrieving location level value", ex);
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
    public List<CwmsSiteReferenceSpecification> getLocationLevelSpecs(DataTransaction tx, String locationId)
        throws OpenDcsDataException
    {
        Connection conn = tx.connection(Connection.class)
            .orElseThrow(() -> new OpenDcsDataException("JDBC Connection not available in this transaction."));
        
        try (DaoHelper helper = new DaoHelper(this.db, MODULE + "-transaction", conn))
        {
            return executeLocationLevelSpecsQuery(helper, locationId);
        }
        catch (SQLException ex)
        {
            throw new OpenDcsDataException("Error reading location level specs for " + locationId, ex);
        }
    }
    
    
    /**
     * Common helper method to execute location level specs query
     * @param helper The DaoHelper to use for the query
     * @param locationId The CWMS location identifier
     * @return List of LocationLevelSpec objects
     * @throws SQLException on database error
     */
    private List<CwmsSiteReferenceSpecification> executeLocationLevelSpecsQuery(DaoHelper helper, String locationId)
        throws SQLException
    {
        List<CwmsSiteReferenceSpecification> specs = new ArrayList<>();
        
        helper.doQuery(LOCATION_LEVEL_SPEC_QUERY, rs -> {
            CwmsSiteReferenceSpecification spec = new CwmsSiteReferenceSpecification();
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
        }, locationId);
        
        return specs;
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
     * Common helper method to execute location level query using DaoHelper
     * @param helper The DaoHelper to use for the query
     * @param locationLevelId The location level identifier
     * @param targetUnits The desired units for the value
     * @return The latest LocationLevelValue or null if none found
     * @throws SQLException on database error
     */
    private CwmsSiteReferenceValue executeLocationLevelQuery(DaoHelper helper, String locationLevelId,
                                                             String targetUnits) throws SQLException
    {
        // Build query based on whether we're filtering by source units
        String query;
        Object[] params;

        query =
            "SELECT CONSTANT_LEVEL AS LOCATION_LEVEL_VALUE, " +
            "LEVEL_DATE AS LOCATION_LEVEL_DATE, LEVEL_UNIT " +
            "FROM CWMS_20.AV_LOCATION_LEVEL " +
            "WHERE LOCATION_LEVEL_ID = ? AND UNIT_SYSTEM = 'SI' " +
            "ORDER BY LEVEL_DATE DESC " +
            "FETCH FIRST 1 ROWS ONLY";
        params = new Object[] { locationLevelId };

        final String[] sourceUnits = new String[] { null };
        
        return helper.getSingleResultOr(query, rs -> {
            CwmsSiteReferenceValue value = new CwmsSiteReferenceValue();
            double levelValue = rs.getDouble("LOCATION_LEVEL_VALUE");
            String dbUnits = rs.getString("LEVEL_UNIT");

            sourceUnits[0] = dbUnits;
            
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
    
    
    /**
     * Close the DAO and free resources
     */
    @Override
    public void close()
    {
        // Nothing to close in stateless implementation
        throw new UnsupportedOperationException("CwmsLocationLevelDAO is stateless and does not need to be closed.");
    }
}