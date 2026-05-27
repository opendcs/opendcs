package org.opendcs.database.dai;

import java.util.List;
import java.util.Optional;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDao;
import org.opendcs.database.api.OpenDcsDataException;

import decodes.db.PresentationGroup;
import decodes.sql.DbKey;

public interface PresentationGroupDao extends OpenDcsDao
{
    /**
     * Retrieve specific presentation group, and it's parents by ID.
     * @param tx
     * @param id
     * @return
     * @throws OpenDcsDataException
     */
    Optional<PresentationGroup> getById(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    /**
     * Retrieve a specific presentation group, and it's parents by Name
     * @param tx
     * @param name
     * @return
     * @throws OpenDcsDataException
     */
    Optional<PresentationGroup> getByName(DataTransaction tx, String name) throws OpenDcsDataException;

    /**
     * Save or update a PresentationGroup, Parent groups will also be saved.
     * @param tx
     * @param group
     * @return
     * @throws OpenDcsDataException
     */
    PresentationGroup save(DataTransaction tx, PresentationGroup group) throws OpenDcsDataException;

    /**
     * Delete a PresentationGroup by Id. Will fail if specified group is referenced by other elements.
     * @param tx
     * @param id
     * @throws OpenDcsDataException
     */
    void delete(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    /**
     * Retrieve all PresentationGroup, optionally with given limit and offset.
     * @param tx
     * @param limit
     * @param offset
     * @return
     * @throws OpenDcsDataException
     */
    List<PresentationGroup> getAll(DataTransaction tx, int limit, int offset) throws OpenDcsDataException;
}
