package org.opendcs.database.dai;

import java.util.List;
import java.util.Optional;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDao;
import org.opendcs.database.api.OpenDcsDataException;

import decodes.sql.DbKey;
import decodes.tsdb.CompAppInfo;

/**
 * Loading (Computation) Application interface to database.
 * 
 * Note Locks {@see opendcs.dai.LoadingApplicationDAI} are intentionally excluded from this interface.
 * Future design work will likely not require the Locks so they will get their own Dao in the new system.
 */
public interface LoadingAppDao extends OpenDcsDao
{
    /**
     * Retrieve Computation App (Loading App) information by ID
     * @param tx Transaction object for the request.
     * @param id Computation App Surrogate Key
     * @return The Computation App information, or empty.
     * @throws OpenDcsDataException
     */
    Optional<CompAppInfo> getById(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    /**
     * Retrieve Computation App (Loading App) information by Name (case insensitive comparison)
     * @param tx Transaction object for the request.
     * @param id Computation App Name
     * @return The Computation App information, or empty.
     * @throws OpenDcsDataException
     */
    Optional<CompAppInfo> getByName(DataTransaction tx, String name) throws OpenDcsDataException;

    /**
     * Save, or update, a Computation App.
     * @param tx Transaction object for the request.
     * @param appInfo If new, don't set the ID. 
     * @return New instance with updated information from database.
     * @throws OpenDcsDataException
     */
    CompAppInfo save(DataTransaction tx, CompAppInfo appInfo) throws OpenDcsDataException;

    /**
     * Remove a computation app from the database. Will fail if any computations depend on it.
     * @param tx Transaction object for the request.
     * @param id 
     * @throws OpenDcsDataException
     */
    void delete(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    /**
     * Retrieve list of computation (loading) applications configured in this database.
     * @param tx Transaction object for the request.
     * @param limit max number of rows. (-1 for all).
     * @param offset start row. (-1 for no offset).
     * @return
     * @throws OpenDcsDataException
     */
    List<CompAppInfo> getAll(DataTransaction tx, int limit, int offset) throws OpenDcsDataException;

}
