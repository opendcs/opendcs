package org.opendcs.database.impl.opendcs.dao.lrgs.archive;

import java.util.List;

import org.jdbi.v3.core.Handle;
import org.opendcs.data.lrgs.archive.ArchiveEntry;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.api.exceptions.data.OpenDcsConstraintException;
import org.opendcs.database.dai.PlatformDao;
import org.opendcs.database.dao.lrgs.archive.LrgsArchiveDao;
import org.opendcs.database.impl.opendcs.jdbi.mapper.lrgs.archive.ArchiveEntryMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.lrgs.archive.ArchiveEntryMapper.Columns;
import org.opendcs.util.Result;
import org.opendcs.utils.sql.SqlErrorMessages;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;


/**
 * This implementation will use Postgres specific JSONB matching feature
 * that aren't compatible with Oracle.
 *
 * Deletion of entries is not allowed, a store trigger will handle clearing the database.
 *
 * DatabaseArchiveDaoImpl
 */                                              
@ServiceProviders({
    @ServiceProvider(service = LrgsArchiveDao.class, path = "dao/OpenDCS-Postgres"),
    @ServiceProvider(service = LrgsArchiveDao.class, path = "dao/OPENTSDB")
})

public class DatabaseArchiveDaoImpl implements LrgsArchiveDao
{
    private static final ArchiveEntryMapper mapper = ArchiveEntryMapper.withPrefix("");

    @Override
    public Result<ArchiveEntry, OpenDcsConstraintException> archive(DataTransaction tx, ArchiveEntry entry) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));

        final String insertQuery = """
            insert into
            lrgs_archive(stored_at, data_time, data_header, data_interface_id, identifier, raw_data, quality)
            values (:stored_at, :data_time, :data_header, :data_interface_id, :identifier, :raw_data, :quality)
            returning (stored_at, data_time, data_header, data_interface_id, identifier, raw_data, quality)
            )
        """;

        try (var insert = handle.createQuery(insertQuery))
        {
            var ret = insert.registerRowMapper(mapper)
                        .bind(Columns.DATA_HEADER.column(), entry.header())
                        .mapTo(ArchiveEntry.class)
                        .findOne();
            if (ret.isPresent())
            {
                return Result.success(ret.get());
            }
            else
            {
                return Result.failure(new OpenDcsConstraintException("Saved message was not returned by the archive."));
            }
        }
    }

    @Override
    public List<ArchiveEntry> getAllBy(DataTransaction tx, int limit, int offset, LrgsSearchCriteria criteria,
            OrderBy order) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        try (var query = handle.createQuery("select * from lrgs_archive order by stored_time asc"))
        {
            return query.registerRowMapper(mapper).mapTo(ArchiveEntry.class).list();
        }
    }
}
