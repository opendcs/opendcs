package org.opendcs.database.dai;

import java.util.List;
import java.util.Optional;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDao;
import org.opendcs.database.api.OpenDcsDataException;

import decodes.db.PlatformConfig;
import decodes.sql.DbKey;

public interface DecodesConfigDao extends OpenDcsDao
{
    Optional<PlatformConfig> getById(DataTransaction tx, DbKey id) throws OpenDcsDataException;
    Optional<PlatformConfig> getByName(DataTransaction tx, String id) throws OpenDcsDataException;

    PlatformConfig save(DataTransaction tx, PlatformConfig config) throws OpenDcsDataException;
    void delete(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    List<PlatformConfig> getAll(DataTransaction tx, int limit, int offset) throws OpenDcsDataException;
}
