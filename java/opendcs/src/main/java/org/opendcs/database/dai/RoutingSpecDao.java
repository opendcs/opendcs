package org.opendcs.database.dai;

import java.util.List;
import java.util.Optional;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDao;
import org.opendcs.database.api.OpenDcsDataException;

import decodes.db.RoutingSpec;
import decodes.sql.DbKey;

public interface RoutingSpecDao extends OpenDcsDao
{
    /**
     * Retrieve Routing Spec By ID. Spec will be ready for use.
     * @param tx
     * @param key
     * @return
     * @throws OpenDcsDataException
     */
    Optional<RoutingSpec> getById(DataTransaction tx, DbKey key) throws OpenDcsDataException;

    /**
     * Retrieve Routing Spec By Name. Spec will be ready for use.
     * @param tx
     * @param name
     * @return
     * @throws OpenDcsDataException
     */
    Optional<RoutingSpec> getByName(DataTransaction tx, String name) throws OpenDcsDataException;

    /**
     * Save, or update, a Spec. Returned Spec will be a new instance ready for use.
     *
     * @param tx
     * @param spec
     * @return
     * @throws OpenDcsDataException
     */
    RoutingSpec save(DataTransaction tx, RoutingSpec spec) throws OpenDcsDataException;

    /**
     * Delete routing spec.
     * @param tx
     * @param id
     * @throws OpenDcsDataException
     */
    void delete(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    /**
     * Retrieval all routing specs. Optionally preparing the spec and retrieving additional data
     * @param tx
     * @param limit
     * @param offset
     * @param includeAll if true will retrieve all additional data (properties, routing lists) and each
     *                   will be ready for use. Otherwise only the bare minimum data is returned for
     *                   each Routing Spec.
     * @return
     * @throws OpenDcsDataException
     */
    default List<RoutingSpec> getAll(DataTransaction tx, int limit, int offset, boolean includeAll) throws OpenDcsDataException
    {
        return getAll(tx, limit, offset, includeAll, null);
    }

    /**
     * As {@see getAll(DataTransaction tx, int limit, int offset, boolean includeAll)} but can filter by
     * schedule configured to use it
     * @param tx
     * @param limit
     * @param offset
     * @param IncludeAll
     * @param forSchedule
     * @return
     * @throws OpenDcsDataException
     */
    List<RoutingSpec> getAll(DataTransaction tx, int limit, int offset, boolean includeAll, String forSchedule) throws OpenDcsDataException;
}
