package org.opendcs.database.dai;

import java.util.List;
import java.util.Optional;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDao;
import org.opendcs.database.api.OpenDcsDataException;

import decodes.db.PlatformConfig;
import decodes.sql.DbKey;

/**
 * Access to Decodes Configuration (Also know as PlatformConfig)
 */
public interface DecodesConfigDao extends OpenDcsDao
{
    /**
     * Retrieve a given PlatformConfig by Id
     * @param tx
     * @param id
     * @return
     * @throws OpenDcsDataException
     */
    Optional<PlatformConfig> getById(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    /**
     * Retrieve a given PlatformConfig by Name
     * @param tx
     * @param id
     * @return
     * @throws OpenDcsDataException
     */
    Optional<PlatformConfig> getByName(DataTransaction tx, String id) throws OpenDcsDataException;

    /**
     * Save or update a platform config
     * @param tx
     * @param config PlatformConfiguration that will be saved as-is to the database.
     * @return a NEW instance of PlatformConfig containing all of the elements, and the generated identifier. Never null
     * @throws OpenDcsDataException
     */
    PlatformConfig save(DataTransaction tx, PlatformConfig config) throws OpenDcsDataException;

    /**
     * Delete a PlatformConfig from the database, including all sensors and scripts.
     * @param tx
     * @param id
     * @throws OpenDcsDataException
     */
    void delete(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    /**
     * Retrieve all, or desired limit and offset, Decodes Configurations from the database.
     * @param tx
     * @param limit
     * @param offset
     * @return
     * @throws OpenDcsDataException
     */
    List<PlatformConfig> getAll(DataTransaction tx, int limit, int offset) throws OpenDcsDataException;
}
