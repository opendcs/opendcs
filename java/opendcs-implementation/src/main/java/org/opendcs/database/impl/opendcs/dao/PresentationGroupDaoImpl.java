
package org.opendcs.database.impl.opendcs.dao;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.PresentationGroupDao;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.presentationgroup.DataPresentationMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.presentationgroup.PresentationGroupAccumulator;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.presentationgroup.PresentationGroupMapper;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlErrorMessages;
import org.opendcs.utils.sql.SqlQueries;
import org.openide.util.lookup.ServiceProvider;
import org.slf4j.Logger;

import decodes.db.DatabaseException;
import decodes.db.PresentationGroup;
import decodes.sql.DbKey;
import decodes.sql.KeyGenerator;

@ServiceProvider(service = PresentationGroupDao.class)
public class PresentationGroupDaoImpl implements PresentationGroupDao
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();

    /**
     * This particular query is rather complex, beyond all of the joins a recursive common table expression is 
     * also used. This allows the query for the given presentation group to also pull in the data for their 
     * parent group in a single step.
     */
    final static String SELECT_QUERY = """
            with recursive pg_limit (id, name, inheritsfrom, lastmodifytime, isproduction) as (
                select id, name, inheritsfrom, lastmodifytime, isproduction
                from presentationgroup
                <where>
                order by name <collate> asc
                <limit>
            ),  pg (id, name, inheritsfrom, lastmodifytime, isproduction) as (
                select id, name, inheritsfrom, lastmodifytime, isproduction, 1 as level
                from pg_limit

                union all
                select pgc.id, pgc.name, pgc.inheritsfrom, pgc.lastmodifytime, pgc.isproduction, p.level + 1
                  from presentationgroup pgc
                  join pg p on pgc.id = p.inheritsfrom
            )
            select 
                pg.id pg_id, pg.name pg_name, pgc.name pg_inheritsfrom, 
                pg.lastmodifytime pg_lastmodifytime, pg.isproduction pg_isproduction,

                dp.id dp_id, dp.unitabbr dp_unitabbr, dp.maxdecimals dp_maxdecimals,
                dp.max_value dp_max_value, dp.min_value dp_min_value,

                dt.id dt_id, dt.standard dt_standard, dt.code dt_code, dt.display_name dt_display_name

            from pg
            left outer join datapresentation dp on dp.groupid = pg.id
            left outer join datatype dt on dt.id = dp.datatypeid
            left outer join presentationgroup pgc on pg.inheritsfrom = pgc.id
            
            
            order by pg.name <collate> asc
            

            """;

    final static String DELETE_DATA_PRESENTATION = """
            delete from datapresentation where datapresentationid = :id
            """;


    @Override
    public Optional<PresentationGroup> getById(DataTransaction tx, DbKey id)  throws OpenDcsDataException
    {
        if (DbKey.isNull(id))
        {
            return Optional.empty();
        }
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var ctx = tx.getContext();
        var dbEngine = ctx.getDatabase();
        try (var query = handle.createQuery(SELECT_QUERY))
        {
            query.define(SqlQueries.COLLATE_CLAUSE, SqlQueries.collateClauseFor(dbEngine))
                 .define(SqlQueries.WHERE_CLAUSE, "where id = :id")
                 .define(SqlQueries.LIMIT_CLAUSE, "")
                 .bind(GenericColumns.ID, id);

            return query.reduceResultSet(new LinkedHashMap<>(),
                                         new PresentationGroupAccumulator(
                                            "pg", PresentationGroupMapper.withPrefix("pg"),
                                            DataPresentationMapper.withPrefix("dp", "dt")
                                        ))
                        .values()
                        .stream()
                        // any child groups will also be in this list and may come first in the order
                        .filter(v -> id.equals(v.getId()))
                        .map(v -> v)
                        .findFirst();
        }
    }

    @Override
    public Optional<PresentationGroup> getByName(DataTransaction tx, String name) throws OpenDcsDataException
    {
        if (name == null)
        {
            return Optional.empty();
        }
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var ctx = tx.getContext();
        var dbEngine = ctx.getDatabase();
        try (var query = handle.createQuery(SELECT_QUERY))
        {
            query.define(SqlQueries.COLLATE_CLAUSE, SqlQueries.collateClauseFor(dbEngine))
                 .define(SqlQueries.WHERE_CLAUSE, "where name = :name")
                 .define(SqlQueries.LIMIT_CLAUSE, "")
                 .bind(GenericColumns.NAME, name);

            return query.reduceResultSet(new LinkedHashMap<>(),
                                         new PresentationGroupAccumulator(
                                            "pg", PresentationGroupMapper.withPrefix("pg"),
                                            DataPresentationMapper.withPrefix("dp", "dt")
                                        ))
                        .values()
                        .stream()
                        // any child groups will also be in this list and may come first in the order
                        .filter(v -> name.equals(v.groupName))
                        .map(v -> v)
                        .findFirst();
        }
    }

    @Override
    public PresentationGroup save(DataTransaction tx, PresentationGroup group) throws OpenDcsDataException
    {
        Objects.requireNonNull(group, "Cannot save null group");
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var ctx = tx.getContext();
        var keyGen = ctx.getGenerator(KeyGenerator.class)
                        .orElseThrow(() -> new OpenDcsDataException("No key generator configured."));
        final String mergeSql = """
                merge into         

                """;
        try (var merge = handle.createUpdate(mergeSql);
             var deleteProps = handle.createUpdate(DELETE_DATA_PRESENTATION))
        {
            DbKey id = group.getId();
            var existing = getByName(tx, group.groupName);
            if (existing.isPresent())
            {
                // If there's an existing app with this name, we'll just assume the provided id, if any, was in error
                id = existing.get().getId();
                log.trace("""
                    Using ID from existing Presentation, id={}, that was found. Provided ID was {}.
                    """,
                    id, group.getId());
            }
            final var bindKey = !DbKey.isNull(id) ? id : keyGen.getKey("site", handle.getConnection());

            return null;
        }
        catch (DatabaseException ex)
        {
            throw new OpenDcsDataException("Unable to generate key to save site", ex);
        }
    }

    @Override
    public void delete(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        
    }


    @Override
    public List<PresentationGroup> getAll(DataTransaction tx, int limit, int offset) throws OpenDcsDataException
    {
        return List.of();
    }
}