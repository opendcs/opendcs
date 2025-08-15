package decodes.cwms;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import opendcs.dao.LocationLevelDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import decodes.db.Database;
import decodes.db.EngineeringUnit;
import decodes.db.UnitConverter;
import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;
import decodes.util.DecodesException;
import ilex.var.NoConversionException;
import opendcs.dao.CachableDbObject;
import opendcs.dao.DaoBase;
import opendcs.dao.DbObjectCache;

/**
 * Data Access Object for CWMS Location Level data.
 * This class provides methods to read and store location level data
 * directly from/to the CWMS database.
 *
 * Implements the LocationLevelDAO interface for database abstraction.
 */
public class CwmsLocationLevelDAO extends DaoBase implements LocationLevelDAO
{
    private final Logger log = LoggerFactory.getLogger(CwmsLocationLevelDAO.class);
    
    // Cache for location level specs with 1 hour expiry
    private static final DbObjectCache<LocationLevelSpec> specCache = 
        new DbObjectCache<LocationLevelSpec>(60 * 60 * 1000L, false);
    
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
    public CwmsLocationLevelDAO(CwmsTimeSeriesDb tsdb)
    {
        super(tsdb, "CwmsLocationLevelDAO");
        this.dbOfficeId = tsdb.getDbOfficeId();
    }
    
    /**
     * Constructor with existing connection
     * @param tsdb The database connection owner
     * @param conn Existing database connection to use
     */
    public CwmsLocationLevelDAO(CwmsTimeSeriesDb tsdb, Connection conn)
    {
        super(tsdb, "CwmsLocationLevelDAO", conn);
        this.dbOfficeId = tsdb.getDbOfficeId();
    }
    
    /**
     * Read location level specification for a given location
     * @param locationId The CWMS location identifier
     * @return List of LocationLevelSpec objects
     * @throws DbIoException on database error
     */
    public List<LocationLevelSpec> getLocationLevelSpecs(String locationId) 
        throws DbIoException
    {
        List<LocationLevelSpec> specs = new ArrayList<>();
        
        try
        {
            doQuery(LOCATION_LEVEL_SPEC_QUERY, rs -> {
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
        }
        catch(SQLException ex)
        {
            throw new DbIoException("Error reading location level specs for " + locationId, ex);
        }
        
        return specs;
    }

    
    /**
     * Get the latest location level value for a given location level
     * @param locationLevelId The location level identifier (e.g., "LOCATION.Physical.Inst.0.0.LEVEL-ID")
     * @return The latest LocationLevelValue or null if none found
     * @throws DbIoException on database error
     */
    @Override
    public LocationLevelValue getLatestLocationLevelValue(String locationLevelId) 
        throws DbIoException
    {
        return getLatestLocationLevelValue(locationLevelId, null, null);
    }
    
    /**
     * Get the latest location level value for a given location level with unit conversion
     * @param locationLevelId The location level identifier (e.g., "LOCATION.Physical.Inst.0.0.LEVEL-ID")
     * @param targetUnits The desired units for the value (e.g., "ft", "m", "in")
     * @param sourceUnitsIn The source units to filter by in database (if null, no filter applied)
     * @return The latest LocationLevelValue or null if none found
     * @throws DbIoException on database error
     */
    @Override
    public LocationLevelValue getLatestLocationLevelValue(String locationLevelId, 
        String targetUnits, String sourceUnitsIn)
        throws DbIoException
    {
        // Read directly from AV_LOCATION_LEVEL CONSTANT_LEVEL column
        // No join needed for constant levels
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
        try
        {
            return getSingleResultOr(query, rs -> {
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
                value.setQualityCode(0);
                return value;
            }, null, params);
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
     * Clear the location level spec cache
     */
    @Override
    public void clearCache()
    {
        specCache.clear();
    }
    
    /**
     * Inner class representing a Location Level Specification
     */
    public static class LocationLevelSpec implements CachableDbObject
    {
        private DbKey key = DbKey.NullKey;
        private String locationLevelId;
        private String specifiedLevelId;
        private String parameterId;
        private String parameterTypeId;
        private String durationId;
        private String locationId;
        private double constantValue;  // The constant level value
        private String levelUnitsId;
        private Date levelDate;
        private String levelComment;
        private String unitSystem;
        private Double attributeValue;
        private String attributeUnit;
        private String attributeParameterId;
        
        public LocationLevelSpec()
        {
        }
        
        @Override
        public DbKey getKey()
        {
            return key;
        }
        
        public void setKey(DbKey key)
        {
            this.key = key;
        }
        
        @Override
        public String getUniqueName()
        {
            // Return the location level ID as the unique name
            return locationLevelId;
        }
        
        // Getters and setters
        public String getLocationLevelId() { return locationLevelId; }
        public void setLocationLevelId(String locationLevelId) { this.locationLevelId = locationLevelId; }
        
        public String getSpecifiedLevelId() { return specifiedLevelId; }
        public void setSpecifiedLevelId(String specifiedLevelId) { this.specifiedLevelId = specifiedLevelId; }
        
        public String getParameterId() { return parameterId; }
        public void setParameterId(String parameterId) { this.parameterId = parameterId; }
        
        public String getParameterTypeId() { return parameterTypeId; }
        public void setParameterTypeId(String parameterTypeId) { this.parameterTypeId = parameterTypeId; }
        
        public String getDurationId() { return durationId; }
        public void setDurationId(String durationId) { this.durationId = durationId; }
        
        public String getLocationId() { return locationId; }
        public void setLocationId(String locationId) { this.locationId = locationId; }
        
        public double getConstantValue() { return constantValue; }
        public void setConstantValue(double constantValue) { this.constantValue = constantValue; }
        
        public String getLevelUnitsId() { return levelUnitsId; }
        public void setLevelUnitsId(String levelUnitsId) { this.levelUnitsId = levelUnitsId; }
        
        public Date getLevelDate() { return levelDate; }
        public void setLevelDate(Date levelDate) { this.levelDate = levelDate; }
        
        public String getLevelComment() { return levelComment; }
        public void setLevelComment(String levelComment) { this.levelComment = levelComment; }
        
        public String getUnitSystem() { return unitSystem; }
        public void setUnitSystem(String unitSystem) { this.unitSystem = unitSystem; }
        
        public Double getAttributeValue() { return attributeValue; }
        public void setAttributeValue(Double attributeValue) { this.attributeValue = attributeValue; }
        
        public String getAttributeUnit() { return attributeUnit; }
        public void setAttributeUnit(String attributeUnit) { this.attributeUnit = attributeUnit; }
        
        public String getAttributeParameterId() { return attributeParameterId; }
        public void setAttributeParameterId(String attributeParameterId) { this.attributeParameterId = attributeParameterId; }
    }
    
}