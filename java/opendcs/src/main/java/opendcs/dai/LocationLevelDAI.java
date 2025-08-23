package opendcs.dai;

import decodes.tsdb.DbIoException;
import opendcs.dao.LocationLevelDAO.LocationLevelValue;
import opendcs.dao.LocationLevelDAO.LocationLevelSpec;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;

import java.util.Date;
import java.util.List;

/**
 * Abstract interface for Location Level Data Access Interface.
 * This interface defines the core operations for reading location level data,
 * which can be implemented for different database types (CWMS, HDB, OpenDCS, etc.)
 */
public interface LocationLevelDAI extends DaiBase
{
    /**
     * Get the latest location level value for a given location level
     * @param locationLevelId The location level identifier (format varies by database)
     * @return The latest LocationLevelValue or null if none found
     * @throws DbIoException on database error
     */
    LocationLevelValue getLatestLocationLevelValue(String locationLevelId) 
        throws DbIoException;
    
    /**
     * Get the latest location level value with unit conversion
     * @param locationLevelId The location level identifier
     * @param targetUnits The desired units for the value (e.g., "ft", "m", "in")
     * @param sourceUnits The source units to filter by (if null, no filter applied)
     * @return The latest LocationLevelValue or null if none found
     * @throws DbIoException on database error
     */
    LocationLevelValue getLatestLocationLevelValue(String locationLevelId, 
        String targetUnits, String sourceUnits) throws DbIoException;
    
    /**
     * Read location level specifications for a given location
     * @param locationId The location identifier (format varies by database)
     * @return List of LocationLevelSpec objects
     * @throws DbIoException on database error
     */
    List<LocationLevelSpec> getLocationLevelSpecs(String locationId) 
        throws DbIoException;
    
    /**
     * Get the latest location level value with transaction support
     * @param tx The data transaction
     * @param locationLevelId The location level identifier
     * @return The latest LocationLevelValue or null if none found
     * @throws OpenDcsDataException on database error
     */
    LocationLevelValue getLatestLocationLevelValue(DataTransaction tx, String locationLevelId)
        throws OpenDcsDataException;
    
    /**
     * Get the latest location level value with transaction support and unit conversion
     * @param tx The data transaction
     * @param locationLevelId The location level identifier
     * @param targetUnits The desired units for the value
     * @param sourceUnits The source units to filter by
     * @return The latest LocationLevelValue or null if none found
     * @throws OpenDcsDataException on database error
     */
    LocationLevelValue getLatestLocationLevelValue(DataTransaction tx, String locationLevelId,
        String targetUnits, String sourceUnits) throws OpenDcsDataException;
    
    /**
     * Read location level specifications with transaction support
     * @param tx The data transaction
     * @param locationId The location identifier
     * @return List of LocationLevelSpec objects
     * @throws OpenDcsDataException on database error
     */
    List<LocationLevelSpec> getLocationLevelSpecs(DataTransaction tx, String locationId)
        throws OpenDcsDataException;
    
    /**
     * Close the DAO and free any resources
     */
    void close();
    
    /**
     * Clear any cached data
     */
    void clearCache();
}