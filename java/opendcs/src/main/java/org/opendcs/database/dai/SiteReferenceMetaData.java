package org.opendcs.database.dai;

import org.opendcs.model.SiteReferenceValue;
import org.opendcs.model.SiteReferenceSpecification;

import java.sql.SQLException;
import java.util.List;

/**
 * Abstract interface for Location Level Data Access Interface.
 * This interface defines the core operations for reading location level data,
 * which can be implemented for different database types (CWMS, HDB, OpenDCS, etc.)
 */
public interface SiteReferenceMetaData
{
    
    /**
     * Get the latest location level value
     * @param locationLevelId The location level identifier
     * @return The latest LocationLevelValue or null if none found
     * @throws SQLException on database error
     */
    SiteReferenceValue getLatestLocationLevelValue(String locationLevelId)
        throws SQLException;
    
    /**
     * Get the latest location level value with unit conversion
     * @param locationLevelId The location level identifier
     * @param targetUnits The desired units for the value
     * @return The latest LocationLevelValue or null if none found
     * @throws SQLException on database error
     */
    SiteReferenceValue getLatestLocationLevelValue(String locationLevelId,
                                                       String targetUnits) throws SQLException;
    
    /**
     * Read location level specifications
     * @param locationId The location identifier
     * @return List of LocationLevelSpec objects
     * @throws SQLException on database error
     */
    List<? extends SiteReferenceSpecification> getLocationLevelSpecs(String locationId)
        throws SQLException;
    
    /**
     * Close the DAO and free any resources
     */
    void close();
}