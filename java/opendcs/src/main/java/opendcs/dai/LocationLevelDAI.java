package opendcs.dai;

import decodes.tsdb.DbIoException;
import decodes.db.LocationLevelValue;
import decodes.db.LocationLevelSpec;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDao;
import org.opendcs.database.api.OpenDcsDataException;

import java.util.Date;
import java.util.List;

/**
 * Abstract interface for Location Level Data Access Interface.
 * This interface defines the core operations for reading location level data,
 * which can be implemented for different database types (CWMS, HDB, OpenDCS, etc.)
 * 
 * All operations are stateless and require a DataTransaction.
 */
public interface LocationLevelDAI extends DaiBase, OpenDcsDao
{
    
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
     * @return The latest LocationLevelValue or null if none found
     * @throws OpenDcsDataException on database error
     */
    LocationLevelValue getLatestLocationLevelValue(DataTransaction tx, String locationLevelId,
        String targetUnits) throws OpenDcsDataException;
    
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
}