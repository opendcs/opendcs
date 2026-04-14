package org.opendcs.database.dai;

import java.util.List;
import java.util.Optional;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDao;
import org.opendcs.database.api.OpenDcsDataException;

import decodes.sql.DbKey;
import decodes.tsdb.CompAppInfo;

public interface LoadingAppDao extends OpenDcsDao
{
    Optional<CompAppInfo> getById(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    Optional<CompAppInfo> getByName(DataTransaction tx, String name) throws OpenDcsDataException;

    CompAppInfo save(DataTransaction tx, CompAppInfo appInfo) throws OpenDcsDataException;

    void delete(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    List<CompAppInfo> getAll(DataTransaction tx, int limit, int offset) throws OpenDcsDataException;

}
