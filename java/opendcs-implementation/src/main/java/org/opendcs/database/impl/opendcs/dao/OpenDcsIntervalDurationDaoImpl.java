package org.opendcs.database.impl.opendcs.dao;

import java.util.List;
import java.util.Optional;


import org.jdbi.v3.core.Handle;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.DatabaseEngine;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.IntervalDurationDao;
import org.opendcs.database.impl.opendcs.jdbi.mapper.timeseries.IntervalMapper;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlErrorMessages;
import org.opendcs.utils.sql.SqlQueries;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;
import org.slf4j.Logger;

import decodes.db.DatabaseException;
import decodes.sql.DbKey;
import decodes.sql.KeyGenerator;
import decodes.tsdb.IntervalCodes;
import opendcs.opentsdb.Interval;

@ServiceProviders({
    @ServiceProvider(service = IntervalDurationDao.class, path = "dao/OpenDCS-Postgres"),
    @ServiceProvider(service = IntervalDurationDao.class, path = "dao/OpenDCS-Oracle"),
    @ServiceProvider(service = IntervalDurationDao.class, path = "dao/OPENTSDB")
})
public class OpenDcsIntervalDurationDaoImpl implements IntervalDurationDao
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();

    private static final String SELECT_INTERVAL = """
            select interval_id, name, cal_constant, cal_multiplier
              from interval_code
              <where>
            """;

    @Override
    public Optional<Interval> findIntervalByName(DataTransaction tx, String name) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        
        try (var query = handle.createQuery(SELECT_INTERVAL)
                               .define(SqlQueries.WHERE_CLAUSE, " where name = :name ")
                             )
        {
            return query.bind(GenericColumns.NAME, name)
                        .registerRowMapper(IntervalMapper.withPrefix(null))
                        .mapTo(Interval.class)
                        .findOne();
        }
        
    }

    @Override
    public Optional<Interval> findIntervalById(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        
        try (var query = handle.createQuery(SELECT_INTERVAL)
                               .define(SqlQueries.WHERE_CLAUSE, " where interval_id = :id ")
                             )
        {
            return query.bind(GenericColumns.ID, id)
                        .registerRowMapper(IntervalMapper.withPrefix(null))
                        .mapTo(Interval.class)
                        .findOne();
        }
        
    }

    @Override
    public Interval saveInterval(DataTransaction tx, Interval interval) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        final String MERGE_SQL = """
            merge into interval_code ic
            using (select :id id, :name name, :cal_constant cal_constant, :cal_multiplier cal_multiplier <dual>) input
            on (ic.interval_id = input.id)
            when matched then
                update set 
                    name = input.name,
                    cal_constant = input.cal_constant,
                    cal_multiplier = input.cal_multiplier
            when not matched then
                insert (interval_id, name, cal_constant, cal_multiplier)
                values (input.id, input.name, input.cal_constant, input.cal_multiplier)
        """;
        var ctx = tx.getContext();
        var keyGen = ctx.getGenerator(KeyGenerator.class)
                        .orElseThrow(() -> new OpenDcsDataException("No key generator configured."));
        
        try (var query = handle.createUpdate(MERGE_SQL)
                               .define("dual", ctx.getDatabase() == DatabaseEngine.ORACLE ? " from dual " : ""))
        {
            DbKey id = interval.getKey();
            var existing = findIntervalByName(tx, interval.getName());
            if (existing.isPresent())
            {
                // If there's an existing app with this name, we'll just assume the provided id, if any, was in error
                id = existing.get().getKey();
                log.trace("""
                    Using ID from existing Interval, id={}, that was found. Provided ID was {}.
                    """,
                    id, interval.getKey());
            }
            final var bindKey = !DbKey.isNull(id) ? id : keyGen.getKey("interval_code", handle.getConnection());

            query.bind(GenericColumns.ID, bindKey)
                 .bind(GenericColumns.NAME, interval.getName())
                 .bind("cal_constant", IntervalCodes.getCalConstName(interval.getCalConstant()))
                 .bind("cal_multiplier", interval.getCalMultiplier())
                 .execute();

            return findIntervalById(tx, bindKey).orElseThrow(() -> new OpenDcsDataException("Unable to retrieve Interval we just saved."));
        }
        catch (DatabaseException ex)
        {
            throw new OpenDcsDataException("Unable to generate surrogate key for new code.", ex);
        }
    }

    @Override
    public Interval saveDuration(DataTransaction tx, Interval interval) throws OpenDcsDataException
    {
        return saveInterval(tx, interval);
    }

    @Override
    public List<Interval> getAllIntervals(DataTransaction tx) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        
        try (var query = handle.createQuery(SELECT_INTERVAL)
                               .define(SqlQueries.WHERE_CLAUSE, "")
                             )
        {
            return query.registerRowMapper(IntervalMapper.withPrefix(null))
                        .mapTo(Interval.class)
                        .list();
        }
    }

    @Override
    public List<Interval> getAllDurations(DataTransaction tx) throws OpenDcsDataException
    {
        return getAllIntervals(tx);
    }

    @Override
    public void deleteInterval(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        try (var deleteQuery = handle.createUpdate("delete from interval_code where interval_id = :id"))
        {
            deleteQuery.bind(GenericColumns.ID, id).execute();
        }
    }

    @Override
    public void deleteDuration(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        deleteInterval(tx, id);
    }

    @Override
    public Optional<Interval> findDurationById(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        return findIntervalById(tx, id);
    }

    @Override
    public Optional<Interval> findDurationByName(DataTransaction tx, String name) throws OpenDcsDataException
    {
        return findIntervalByName(tx, name);
    }
    
}
