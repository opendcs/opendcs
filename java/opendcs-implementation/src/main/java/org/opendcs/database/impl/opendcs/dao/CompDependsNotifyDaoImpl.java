package org.opendcs.database.impl.opendcs.dao;

import static org.opendcs.utils.sql.SqlQueries.LIMIT_CLAUSE;
import static org.opendcs.utils.sql.SqlQueries.addLimitOffset;

import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.CompDependsNotifyDao;
import org.opendcs.database.impl.opendcs.jdbi.mapper.timeseries.CpDependsNotifyMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.timeseries.CpDependsNotifyMapper.Columns;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlErrorMessages;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

import decodes.db.DatabaseException;
import decodes.sql.KeyGenerator;
import decodes.tsdb.CpDependsNotify;
@ServiceProviders({
    @ServiceProvider(service = CompDependsNotifyDao .class, path = "dao/OpenDCS-Postgres"),
    @ServiceProvider(service = CompDependsNotifyDao .class, path = "dao/OpenDCS-Oracle"),
    @ServiceProvider(service = CompDependsNotifyDao .class, path = "dao/OPENTSDB")
})
public class CompDependsNotifyDaoImpl implements CompDependsNotifyDao
{
    private static final String SELECT_QUERY = """
            select RECORD_NUM, EVENT_TYPE, KEY, DATE_TIME_LOADED from CP_DEPENDS_NOTIFY

            order by date_time_loaded
            <limit>
            """;

    @Override
    public void deleteNotifyRecord(DataTransaction tx, CpDependsNotify notificationRecord) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        try (var deleteRecord = handle.createUpdate("delete from cp_depends_notify = record_num = :id"))
        {
            deleteRecord.bind(GenericColumns.ID.column(), notificationRecord.getRecordNum());
        }
    }

    @Override
    public List<CpDependsNotify> getAllNotifyRecords(DataTransaction tx) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        try (var selectAll = handle.createQuery(SELECT_QUERY).define(LIMIT_CLAUSE, ""))
        {
            return selectAll.registerRowMapper(CpDependsNotifyMapper.withNoPrefix())
                            .mapTo(CpDependsNotify.class)
                            .list();
        }
    }

    @Override
    public Optional<CpDependsNotify> getNextRecord(DataTransaction tx) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        try (var selectAll = handle.createQuery(SELECT_QUERY).define(LIMIT_CLAUSE, addLimitOffset(1, 0)))
        {
            return selectAll.registerRowMapper(CpDependsNotifyMapper.withNoPrefix())
                            .bind(LIMIT_CLAUSE, 1)
                            .mapTo(CpDependsNotify.class)
                            .findFirst();
        }
    }

    @Override
    public void saveRecord(DataTransaction tx, CpDependsNotify notificationRecord) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var ctx = tx.getContext();
        var keyGen = ctx.getGenerator(KeyGenerator.class)
                        .orElseThrow(() -> new OpenDcsDataException("No key generator configured."));
        try (var insert = handle.createUpdate("""
                insert into cp_depends_notify(record_num, event_type, key, date_time_loaded)
                 values(:record_num, :event_type, :key, :date_time_loaded)
            """))
        {
            insert.bind(Columns.RECORD_NUM.column(), keyGen.getKey("cp_depends_notify", handle.getConnection()))
                  .bind(Columns.DATE_TIME_LOADED.column(), System.currentTimeMillis())
                  .bind(Columns.KEY.column(), notificationRecord.getKey())
                  .bind(Columns.EVENT_TYPE.column(), notificationRecord.getEventType())
                  .execute();
        }
        catch (DatabaseException ex)
        {
            throw new OpenDcsDataException("Unable to save notification record.", ex);
        }
    }

}
