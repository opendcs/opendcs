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
import org.opendcs.model.SiteReferenceSpecification;
import org.opendcs.model.SiteReferenceValue;
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
import java.util.Date;
import java.sql.Timestamp;
import java.sql.CallableStatement;
import java.sql.Types;
import java.sql.ResultSet;
import decodes.tsdb.CTimeSeries;
import ilex.var.TimedVariable;
import decodes.sql.DbKey;
import decodes.db.Constants;
import oracle.jdbc.OracleTypes;

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
    public SiteReferenceValue getLatestLocationLevelValue(DataTransaction tx, String locationLevelId)
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
    public SiteReferenceValue getLatestLocationLevelValue(DataTransaction tx, String locationLevelId,
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
    public List<? extends SiteReferenceSpecification> getLocationLevelSpecs(DataTransaction tx, String locationId)
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
     * Read a range of location level values in time and store them in a CTimeSeries object
     * Uses CWMS_LEVEL.RETRIEVE_LOCATION_LEVEL_VALUES procedure
     * @param tx The data transaction
     * @param locationLevelId The location level identifier
     * @param startTime The start time for the range
     * @param endTime The end time for the range
     * @param targetUnits Optional target units for conversion
     * @return CTimeSeries containing the location level values
     * @throws OpenDcsDataException on database error
     */
    public CTimeSeries getLocationLevelRange(DataTransaction tx, String locationLevelId,
                                              Date startTime, Date endTime, String targetUnits)
        throws OpenDcsDataException
    {
        Connection conn = tx.connection(Connection.class)
            .orElseThrow(() -> new OpenDcsDataException("JDBC Connection not available in this transaction."));
        
        CTimeSeries timeSeries = new CTimeSeries(Constants.undefinedId, "inst", "R_");
        timeSeries.setDisplayName(locationLevelId);
        
        try (CallableStatement cstmt = conn.prepareCall(
            "{ call CWMS_20.CWMS_LEVEL.RETRIEVE_LOCATION_LEVEL_VALUES(?, ?, ?, ?, ?, ?, ?) }"))
        {
            // Set input parameters
            cstmt.setString(1, locationLevelId);                           // p_location_level_id
            cstmt.setString(2, "SI");                                      // p_unit_system
            cstmt.setTimestamp(3, new Timestamp(startTime.getTime()));     // p_start_time
            cstmt.setTimestamp(4, new Timestamp(endTime.getTime()));       // p_end_time
            cstmt.setString(5, null);                                      // p_timezone (null for database default)
            cstmt.setString(6, dbOfficeId);                                // p_office_id
            
            // Register output parameter for cursor
            cstmt.registerOutParameter(7, OracleTypes.CURSOR);             // p_results
            
            // Execute the procedure
            cstmt.execute();
            
            // Process the result set
            try (ResultSet rs = (ResultSet) cstmt.getObject(7))
            {
                if (rs != null)
                {
                    String dbUnits = null;
                    while (rs.next())
                    {
                        Timestamp levelDate = rs.getTimestamp("DATE_TIME");
                        double levelValue = rs.getDouble("VALUE");
                        int qualityCode = rs.getInt("QUALITY_CODE");
                        
                        // Get units from first record
                        if (dbUnits == null)
                        {
                            dbUnits = rs.getString("UNIT_ID");
                            if (dbUnits == null) dbUnits = "m"; // Default to meters for SI
                        }
                        
                        // Perform unit conversion if needed
                        double finalValue = levelValue;
                        if (targetUnits != null && !targetUnits.isEmpty() && 
                            !dbUnits.equalsIgnoreCase(targetUnits))
                        {
                            try
                            {
                                finalValue = convertUnits(levelValue, dbUnits, targetUnits);
                                timeSeries.setUnitsAbbr(targetUnits);
                            }
                            catch (Exception ex)
                            {
                                log.warn("Failed to convert units from {} to {}: {}",
                                        dbUnits, targetUnits, ex.getMessage());
                                timeSeries.setUnitsAbbr(dbUnits);
                            }
                        }
                        else
                        {
                            timeSeries.setUnitsAbbr(dbUnits);
                        }
                        
                        // Create TimedVariable and add to time series
                        TimedVariable tv = new TimedVariable(finalValue);
                        tv.setTime(levelDate);
                        tv.setFlags(qualityCode);
                        timeSeries.addSample(tv);
                    }
                }
            }
            
            return timeSeries;
        }
        catch (SQLException ex)
        {
            throw new OpenDcsDataException("Error retrieving location level range for " + locationLevelId, ex);
        }
    }
    
    /**
     * Get location level value at a specific time or the previous value if not found
     * Uses CWMS_LEVEL.RETRIEVE_LOCATION_LEVEL__2 procedure
     * @param tx The data transaction
     * @param locationLevelId The location level identifier  
     * @param requestedTime The time to retrieve the value for
     * @param requireSpecificTime If true, only return value at exact time; if false, return previous value if no exact match
     * @param targetUnits Optional target units for conversion
     * @return LocationLevelValue at the requested time or null if none found
     * @throws OpenDcsDataException on database error
     */
    public SiteReferenceValue getLocationLevelAtTime(DataTransaction tx, String locationLevelId,
                                                      Date requestedTime, boolean requireSpecificTime,
                                                      String targetUnits)
        throws OpenDcsDataException
    {
        Connection conn = tx.connection(Connection.class)
            .orElseThrow(() -> new OpenDcsDataException("JDBC Connection not available in this transaction."));
        
        try (CallableStatement cstmt = conn.prepareCall(
            "{ call CWMS_20.CWMS_LEVEL.RETRIEVE_LOCATION_LEVEL__2(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }"))
        {
            // Set input parameters
            cstmt.setString(1, null);                                            // p_location_level_value (OUT)
            cstmt.registerOutParameter(1, Types.DOUBLE);
            
            cstmt.setString(2, null);                                            // p_level_unit (OUT)
            cstmt.registerOutParameter(2, Types.VARCHAR);
            
            cstmt.setString(3, null);                                            // p_date_time (OUT)
            cstmt.registerOutParameter(3, Types.TIMESTAMP);
            
            cstmt.setString(4, null);                                            // p_quality_code (OUT)
            cstmt.registerOutParameter(4, Types.INTEGER);
            
            cstmt.setString(5, null);                                            // p_location_id (OUT)
            cstmt.registerOutParameter(5, Types.VARCHAR);
            
            cstmt.setString(6, null);                                            // p_parameter_id (OUT)
            cstmt.registerOutParameter(6, Types.VARCHAR);
            
            cstmt.setString(7, null);                                            // p_parameter_type_id (OUT)
            cstmt.registerOutParameter(7, Types.VARCHAR);
            
            cstmt.setString(8, null);                                            // p_duration_id (OUT)
            cstmt.registerOutParameter(8, Types.VARCHAR);
            
            cstmt.setString(9, null);                                            // p_specified_level_id (OUT)
            cstmt.registerOutParameter(9, Types.VARCHAR);
            
            cstmt.setString(10, locationLevelId);                                // p_location_level_id (IN)
            cstmt.setString(11, "SI");                                          // p_unit_system (IN)
            cstmt.setTimestamp(12, new Timestamp(requestedTime.getTime()));     // p_level_date (IN)
            cstmt.setString(13, null);                                          // p_timezone (IN)
            cstmt.setString(14, requireSpecificTime ? "T" : "F");              // p_match_time (IN) T=exact, F=previous
            cstmt.setString(15, dbOfficeId);                                    // p_office_id (IN)
            
            // Execute the procedure
            cstmt.execute();
            
            // Get the output values
            Double levelValue = cstmt.getObject(1, Double.class);
            if (levelValue == null)
            {
                return null; // No value found
            }
            
            String dbUnits = cstmt.getString(2);
            Timestamp actualLevelDate = cstmt.getTimestamp(3);
            Integer qualityCode = cstmt.getObject(4, Integer.class);
            
            // Create the result object
            CwmsSiteReferenceValue value = new CwmsSiteReferenceValue();
            value.setLocationLevelId(locationLevelId);
            value.setLevelDate(actualLevelDate);
            value.setQualityCode(qualityCode != null ? qualityCode : 0);
            
            // Perform unit conversion if needed
            double finalValue = levelValue;
            String finalUnits = dbUnits != null ? dbUnits : "m";
            
            if (targetUnits != null && !targetUnits.isEmpty() && 
                dbUnits != null && !dbUnits.equalsIgnoreCase(targetUnits))
            {
                try
                {
                    finalValue = convertUnits(levelValue, dbUnits, targetUnits);
                    finalUnits = targetUnits;
                }
                catch (Exception ex)
                {
                    log.warn("Failed to convert units from {} to {}: {}",
                            dbUnits, targetUnits, ex.getMessage());
                }
            }
            
            value.setLevelValue(finalValue);
            value.setUnits(finalUnits);
            
            return value;
        }
        catch (SQLException ex)
        {
            throw new OpenDcsDataException("Error retrieving location level at time for " + locationLevelId, ex);
        }
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