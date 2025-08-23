package opendcs.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.opendcs.database.SimpleTransaction;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.tsdb.DbIoException;
import opendcs.dai.LocationLevelDAI;
import decodes.sql.DbKey;
import opendcs.dao.CachableDbObject;
import opendcs.dao.DbObjectCache;

/**
 * Base class for Location Level Data Access Objects.
 * This abstract class provides common functionality for all Location Level DAO implementations
 * following a stateless pattern similar to EnumSqlDao.
 * 
 * Concrete implementations (CWMS, HDB, OpenDCS) should extend this class and implement
 * the abstract methods for their specific database type.
 */
public abstract class LocationLevelDAO extends DaoBase implements LocationLevelDAI
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
    
    /**
     * Cache for location level data with configurable expiry time
     * Default is 1 hour (3600000 ms)
     */
    protected static DbObjectCache<LocationLevelValue> cache = 
        new DbObjectCache<LocationLevelValue>(3600000L, false);
    
    /**
     * Cache for location level specifications with configurable expiry time
     * Default is 1 hour (3600000 ms)
     */
    protected static DbObjectCache<LocationLevelSpec> specCache = 
        new DbObjectCache<LocationLevelSpec>(3600000L, false);
    
    /**
     * Constructor
     * @param db The database connection owner
     * @param module The module name for logging
     */
    protected LocationLevelDAO(DatabaseConnectionOwner db, String module)
    {
        super(db, module);
    }
    
    /**
     * Constructor with existing connection for use in transactions
     * @param db The database connection owner
     * @param module The module name for logging
     * @param conn Existing database connection to use
     */
    protected LocationLevelDAO(DatabaseConnectionOwner db, String module, Connection conn)
    {
        super(db, module, conn);
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
     * Get the latest location level value for a given location level
     * Uses cache if available, otherwise queries database
     * @param locationLevelId The location level identifier
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
     * Get the latest location level value with unit conversion
     * Uses cache if available, otherwise queries database
     * @param locationLevelId The location level identifier
     * @param targetUnits The desired units for the value
     * @param sourceUnits The source units to filter by
     * @return The latest LocationLevelValue or null if none found
     * @throws DbIoException on database error
     */
    @Override
    public LocationLevelValue getLatestLocationLevelValue(String locationLevelId, 
        String targetUnits, String sourceUnits) throws DbIoException
    {
        synchronized(cache)
        {
            // Check cache first
            String cacheKey = buildCacheKey(locationLevelId, targetUnits, sourceUnits);
            LocationLevelValue cached = cache.getByUniqueName(cacheKey);
            
            if (cached != null && isCacheValid(cached))
            {
                log.debug("Returning cached location level value for {}", locationLevelId);
                return cached;
            }
            
            // Not in cache or expired, query database
            LocationLevelValue value = queryLatestLocationLevelValue(locationLevelId, targetUnits, sourceUnits);
            
            if (value != null)
            {
                // Set cache key for retrieval
                value.setCacheKey(cacheKey);
                cache.put(value);
            }
            
            return value;
        }
    }
    
    /**
     * Get location level value with transaction support
     * @param tx The data transaction
     * @param locationLevelId The location level identifier
     * @return The latest LocationLevelValue or null if none found
     * @throws OpenDcsDataException on database error
     */
    public LocationLevelValue getLatestLocationLevelValue(DataTransaction tx, String locationLevelId)
        throws OpenDcsDataException
    {
        return getLatestLocationLevelValue(tx, locationLevelId, null, null);
    }
    
    /**
     * Get location level value with transaction support and unit conversion
     * @param tx The data transaction
     * @param locationLevelId The location level identifier
     * @param targetUnits The desired units for the value
     * @param sourceUnits The source units to filter by
     * @return The latest LocationLevelValue or null if none found
     * @throws OpenDcsDataException on database error
     */
    public LocationLevelValue getLatestLocationLevelValue(DataTransaction tx, String locationLevelId,
        String targetUnits, String sourceUnits) throws OpenDcsDataException
    {
        Connection conn = tx.connection(Connection.class)
            .orElseThrow(() -> new OpenDcsDataException("JDBC Connection not available in this transaction."));
        
        try (DaoHelper helper = new DaoHelper(this.db, module + "-transaction", conn))
        {
            return queryLatestLocationLevelValue(helper, locationLevelId, targetUnits, sourceUnits);
        }
        catch (DbIoException ex)
        {
            throw new OpenDcsDataException("Error retrieving location level value", ex);
        }
    }
    
    /**
     * Read location level specifications for a given location
     * Default implementation throws UnsupportedOperationException.
     * Subclasses that support this operation should override this method.
     * @param locationId The location identifier
     * @return List of LocationLevelSpec objects
     * @throws DbIoException on database error
     */
    @Override
    public List<LocationLevelSpec> getLocationLevelSpecs(String locationId) 
        throws DbIoException
    {
        throw new UnsupportedOperationException(
            "getLocationLevelSpecs not implemented for " + this.getClass().getSimpleName());
    }
    
    /**
     * Read location level specifications with transaction support
     * Default implementation throws UnsupportedOperationException.
     * Subclasses that support this operation should override this method.
     * @param tx The data transaction
     * @param locationId The location identifier
     * @return List of LocationLevelSpec objects
     * @throws OpenDcsDataException on database error
     */
    @Override
    public List<LocationLevelSpec> getLocationLevelSpecs(DataTransaction tx, String locationId)
        throws OpenDcsDataException
    {
        throw new UnsupportedOperationException(
            "getLocationLevelSpecs with transaction not implemented for " + this.getClass().getSimpleName());
    }
    
    /**
     * Clear the cache
     */
    @Override
    public void clearCache()
    {
        synchronized(cache)
        {
            cache.clear();
        }
        synchronized(specCache)
        {
            specCache.clear();
        }
    }
    
    /**
     * Clear only the location level value cache
     */
    public void clearValueCache()
    {
        synchronized(cache)
        {
            cache.clear();
        }
    }
    
    /**
     * Clear only the location level spec cache
     */
    public void clearSpecCache()
    {
        synchronized(specCache)
        {
            specCache.clear();
        }
    }
    
    /**
     * Close the DAO and free resources
     */
    @Override
    public void close()
    {
        super.close();
    }
    
    /**
     * Build a cache key from the query parameters
     * @param locationLevelId The location level identifier
     * @param targetUnits The target units (may be null)
     * @param sourceUnits The source units (may be null)
     * @return A unique cache key string
     */
    protected String buildCacheKey(String locationLevelId, String targetUnits, String sourceUnits)
    {
        StringBuilder key = new StringBuilder(locationLevelId);
        if (targetUnits != null)
        {
            key.append(":").append(targetUnits);
        }
        if (sourceUnits != null)
        {
            key.append(":").append(sourceUnits);
        }
        return key.toString();
    }
    
    /**
     * Check if a cached value is still valid
     * Default implementation checks if the value is not too old
     * Subclasses can override for specific validation logic
     * @param cached The cached value
     * @return true if cache is still valid
     */
    protected boolean isCacheValid(LocationLevelValue cached)
    {
        // Default: cache is valid if it exists
        // Subclasses can implement time-based or other validation
        return true;
    }
    
    /**
     * Query the database for the latest location level value
     * This method must be implemented by concrete subclasses
     * @param locationLevelId The location level identifier
     * @param targetUnits The desired units for the value
     * @param sourceUnits The source units to filter by
     * @return The latest LocationLevelValue or null if none found
     * @throws DbIoException on database error
     */
    protected abstract LocationLevelValue queryLatestLocationLevelValue(
        String locationLevelId, String targetUnits, String sourceUnits) throws DbIoException;
    
    /**
     * Query the database for the latest location level value using a helper
     * This method must be implemented by concrete subclasses for transaction support
     * @param helper The DaoHelper with transaction connection
     * @param locationLevelId The location level identifier
     * @param targetUnits The desired units for the value
     * @param sourceUnits The source units to filter by
     * @return The latest LocationLevelValue or null if none found
     * @throws DbIoException on database error
     */
    protected abstract LocationLevelValue queryLatestLocationLevelValue(
        DaoHelper helper, String locationLevelId, String targetUnits, String sourceUnits) throws DbIoException;
    
    /**
     * Inner class representing a Location Level Value
     * Implements CachableDbObject for caching support
     */
    public static class LocationLevelValue implements CachableDbObject
    {
        private String locationLevelId;
        private double levelValue;
        private Date levelDate;
        private int qualityCode;
        private String units;
        private String comment;
        private String cacheKey;
        
        public LocationLevelValue() {}
        
        public LocationLevelValue(String locationLevelId, double levelValue, 
            Date levelDate, String units)
        {
            this.locationLevelId = locationLevelId;
            this.levelValue = levelValue;
            this.levelDate = levelDate;
            this.units = units;
            this.qualityCode = 0;
        }
        
        // CachableDbObject implementation
        @Override
        public DbKey getKey()
        {
            // Location levels don't have numeric keys, use null key
            return DbKey.NullKey;
        }
        
        @Override
        public String getUniqueName()
        {
            return cacheKey != null ? cacheKey : locationLevelId;
        }
        
        // Getters and setters
        public String getLocationLevelId() { return locationLevelId; }
        public void setLocationLevelId(String locationLevelId) { 
            this.locationLevelId = locationLevelId; 
        }
        
        public double getLevelValue() { return levelValue; }
        public void setLevelValue(double levelValue) { 
            this.levelValue = levelValue; 
        }
        
        public Date getLevelDate() { return levelDate; }
        public void setLevelDate(Date levelDate) { 
            this.levelDate = levelDate; 
        }
        
        public int getQualityCode() { return qualityCode; }
        public void setQualityCode(int qualityCode) { 
            this.qualityCode = qualityCode; 
        }
        
        public String getUnits() { return units; }
        public void setUnits(String units) { 
            this.units = units; 
        }
        
        public String getComment() { return comment; }
        public void setComment(String comment) { 
            this.comment = comment; 
        }
        
        public String getCacheKey() { return cacheKey; }
        public void setCacheKey(String cacheKey) {
            this.cacheKey = cacheKey;
        }
        
        @Override
        public String toString()
        {
            return String.format("LocationLevel[id=%s, value=%.3f %s, date=%s]",
                locationLevelId, levelValue, units, levelDate);
        }
    }
    
    /**
     * Inner class representing a Location Level Specification
     * Implements CachableDbObject for caching support
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