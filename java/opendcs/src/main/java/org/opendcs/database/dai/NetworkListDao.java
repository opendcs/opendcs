package org.opendcs.database.dai;

import java.util.List;
import java.util.Optional;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDao;
import org.opendcs.database.api.OpenDcsDataException;

import decodes.db.NetworkList;
import decodes.sql.DbKey;

public interface NetworkListDao extends OpenDcsDao
{
    Optional<NetworkList> getById(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    Optional<NetworkList> getByName(DataTransaction tx, String name) throws OpenDcsDataException;
    
    NetworkList save(DataTransaction tx, NetworkList networkList) throws OpenDcsDataException;

    void delete(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    default List<NetworkList> getAll(DataTransaction tx, int limit, int offset)  throws OpenDcsDataException
    {
        return getAll(tx, limit, offset, null, true);
    }

    List<NetworkList> getAll(DataTransaction tx, int limit, int offset, String mediumType, boolean includeEntries)  throws OpenDcsDataException;
}
