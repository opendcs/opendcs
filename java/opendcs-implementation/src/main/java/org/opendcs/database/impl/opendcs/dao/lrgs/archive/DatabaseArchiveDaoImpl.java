package org.opendcs.database.impl.opendcs.dao.lrgs.archive;

import java.util.List;

import org.opendcs.data.lrgs.archive.ArchiveEntry;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.api.exceptions.data.OpenDcsConstraintException;
import org.opendcs.database.dao.lrgs.archive.LrgsArchiveDao;
import org.opendcs.util.Result;

import mil.army.usace.hec.services.annotations.ServiceProvider;

/**
 * This implementation will use Postgres specific JSONB matching feature
 * that aren't compatible with Oracle.
 *
 * Deletion of entries is not allowed, a store trigger will handle clearing the database.
 *
 * DatabaseArchiveDaoImpl
 */
@ServiceProvider(service = LrgsArchiveDao.class, path = "dao/OpenDCS-Postgres")
public class DatabaseArchiveDaoImpl implements LrgsArchiveDao
{
    @Override
    public Result<ArchiveEntry, OpenDcsConstraintException> archive(DataTransaction tx, ArchiveEntry entry) throws OpenDcsDataException
    {


        return Result.failure(new OpenDcsConstraintException("Not yet implemented."));
    }

    @Override
    public List<ArchiveEntry> getAllBy(DataTransaction tx, int limit, int offset, LrgsSearchCriteria criteria,
            OrderBy order) throws OpenDcsDataException
    {
        return List.of();
    }
}
