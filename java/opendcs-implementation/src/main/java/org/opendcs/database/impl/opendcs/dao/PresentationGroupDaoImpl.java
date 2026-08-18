
package org.opendcs.database.impl.opendcs.dao;

import static org.opendcs.utils.sql.SqlQueries.addLimitOffset;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Vector;

import org.jdbi.v3.core.Handle;
import org.opendcs.database.DatabaseQuerySettings;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.DatabaseEngine;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.PresentationGroupDao;
import org.opendcs.database.impl.opendcs.jdbi.column.numeric.NullableDoubleArgumentFactory;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.presentationgroup.DataPresentationMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.presentationgroup.PresentationGroupAccumulator;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.presentationgroup.PresentationGroupMapper;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlErrorMessages;
import org.opendcs.utils.sql.SqlKeywords;
import org.opendcs.utils.sql.SqlQueries;
import org.openide.util.lookup.ServiceProvider;
import org.slf4j.Logger;

import decodes.db.DataPresentation;
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
    static final String SELECT_QUERY = """
            with <recursive_cte> pg_limit (id, name, inheritsfrom, lastmodifytime, isproduction) as (
                select id, name, inheritsfrom, lastmodifytime, isproduction
                from presentationgroup
                <where>
                order by name <collate> asc
                <limit>
            ),  pg (id, name, inheritsfrom, lastmodifytime, isproduction, cur_level) as (
                select id, name, inheritsfrom, lastmodifytime, isproduction, 1 as cur_level
                from pg_limit

                union all
                select pgc.id, pgc.name, pgc.inheritsfrom, pgc.lastmodifytime, pgc.isproduction, p.cur_level + 1
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

    static final String DELETE_DATA_PRESENTATION = """
            delete from datapresentation where groupid = :id
            """;

    static final String INSERT_DATA_PRESENTATION = """
            insert into
             datapresentation(id, groupid, datatypeid, unitabbr, equipmentid, maxdecimals, max_value, min_value)
             values(:id, :groupid, :datatypeid, :unitabbr, :equipmentid, :maxdecimals, :max_value, :min_value)
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
        var dbEngine = ctx.getDatabaseEngine();
        try (var query = handle.createQuery(SELECT_QUERY))
        {
            query.define(SqlQueries.COLLATE_CLAUSE, SqlQueries.collateClauseFor(dbEngine))
                 .define(SqlQueries.RECURSIVE_CTE_CLAUSE, SqlQueries.recursiveCteFor(dbEngine))
                 .define(SqlQueries.WHERE_CLAUSE, "where id = :id")
                 .define(SqlQueries.LIMIT_CLAUSE, "")
                 .bind(GenericColumns.ID.column(), id);

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
        var dbEngine = ctx.getDatabaseEngine();
        try (var query = handle.createQuery(SELECT_QUERY))
        {
            query.define(SqlQueries.COLLATE_CLAUSE, SqlQueries.collateClauseFor(dbEngine))
                 .define(SqlQueries.RECURSIVE_CTE_CLAUSE, SqlQueries.recursiveCteFor(dbEngine))
                 .define(SqlQueries.WHERE_CLAUSE, "where name = :name")
                 .define(SqlQueries.LIMIT_CLAUSE, "")
                 .bind(GenericColumns.NAME.column(), name);

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
        var numericDate = ctx.getSettings(DatabaseQuerySettings.class)
                             .map(settings -> settings.numericDate())
                             .orElse(true);
        final String mergeSql = """
                merge into presentationgroup pg
                using (
                    select :id id, :name name, :inheritsfrom inheritsfrom, :lastmodifytime lastmodifytime,
                           :isproduction isproduction <dual>
                ) input
                on (pg.id = input.id)
                when matched then
                    update set
                        name = input.name, inheritsfrom = input.inheritsfrom,
                        lastmodifytime = input.lastmodifytime, isproduction = input.isproduction
                when not matched then
                    insert(id, name, inheritsfrom, lastmodifytime, isproduction)
                    values(input.id, input.name, input.inheritsfrom, input.lastmodifytime, input.isproduction)
                """;
        try (var merge = handle.createUpdate(mergeSql)
                               .define("numeric_date", numericDate)
                               .define("dual", ctx.getDatabaseEngine() == DatabaseEngine.ORACLE ? " from dual " : ""))
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
            final var bindKey = !DbKey.isNull(id) ? id : keyGen.getKey("presentationgroup", handle.getConnection());

            merge.bind(GenericColumns.ID.column(), bindKey)
                 .bind(GenericColumns.NAME.column(), group.groupName)
                 .bind("lastmodifytime", new Date())
                 .bind("isproduction", group.isProduction)
                 ;

            if (group.parent != null && !DbKey.isNull(group.parent.getId())) // check ID and maybe merge that in first?
            {
                merge.bind("inheritsfrom", group.parent.getId());
            }
            else if (group.parent != null)
            {
                final var parent = save(tx, group.parent); // Parent hasn't been saved yet? Just save it now.
                merge.bind("inheritsfrom", parent.getId());
            }
            else
            {
                merge.bindByType("inheritsfrom", null, DbKey.class);
            }
            merge.execute();

            updateDataPresentations(handle, group.dataPresentations, bindKey, keyGen);

            return getById(tx, bindKey).orElseThrow(() -> new OpenDcsDataException("Unable to retrieve Presentation Group we just saved."));
        }
        catch (DatabaseException ex)
        {
            throw new OpenDcsDataException("Unable to generate key to save presentation group", ex);
        }
    }

    private static void updateDataPresentations(Handle handle, Vector<DataPresentation> presentations, DbKey groupId, KeyGenerator keyGen) throws DatabaseException
    {
        try (var deletePresentations = handle.createUpdate(DELETE_DATA_PRESENTATION);
             var insertPresentations = handle.prepareBatch(INSERT_DATA_PRESENTATION))
        {
            deletePresentations.bind(GenericColumns.ID.column(), groupId).execute();

            if (!presentations.isEmpty())
            {
                insertPresentations.registerArgument(new NullableDoubleArgumentFactory());
                for (var dataPresentation: presentations)
                {
                    final var id = dataPresentation.idIsSet() ? dataPresentation.getId() : keyGen.getKey("datapresentation", handle.getConnection());
                    insertPresentations.bind(GenericColumns.ID.column(), id)
                                       .bind("groupid", groupId)
                                       .bind("datatypeid", dataPresentation.getDataType().getId())
                                       .bind("unitabbr", dataPresentation.getUnitsAbbr())
                                       .bindByType("equipmentid", null, DbKey.class) // not yet supported in code
                                       .bind("maxdecimals", dataPresentation.getMaxDecimals())
                                       .bind("max_value", dataPresentation.getMaxValue())
                                       .bind("min_value", dataPresentation.getMinValue())
                                       .add();
                }
                insertPresentations.execute();
            }
        }
    }


    @Override
    public void delete(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        if (DbKey.isNull(id))
        {
            return;
        }
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));

        try (var deleteGroup = handle.createUpdate("delete from presentationgroup where id = :id");
             var deletePresentations = handle.createUpdate(DELETE_DATA_PRESENTATION))
        {
            deletePresentations.bind(GenericColumns.ID.column(), id).execute();
            deleteGroup.bind(GenericColumns.ID.column(), id).execute();
        }
    }


    @Override
    public List<PresentationGroup> getAll(DataTransaction tx, int limit, int offset) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var ctx = tx.getContext();
        var dbEngine = ctx.getDatabaseEngine();
        try (var query = handle.createQuery(SELECT_QUERY))
        {
            query.define(SqlQueries.COLLATE_CLAUSE, SqlQueries.collateClauseFor(dbEngine))
                 .define(SqlQueries.RECURSIVE_CTE_CLAUSE, SqlQueries.recursiveCteFor(dbEngine))
                 .define(SqlQueries.WHERE_CLAUSE, "")
                 .define(SqlQueries.LIMIT_CLAUSE, addLimitOffset(limit, offset))
                 ;
            if (limit >= 0)
            {
                query.bind(SqlKeywords.LIMIT, limit);
            }
            if (offset >= 0)
            {
                query.bind(SqlKeywords.OFFSET, offset);
            }
            return query.reduceResultSet(new LinkedHashMap<>(),
                                         new PresentationGroupAccumulator(
                                            "pg", PresentationGroupMapper.withPrefix("pg"),
                                            DataPresentationMapper.withPrefix("dp", "dt")
                                        ))
                        .values()
                        .stream()
                        .map(v -> v)
                        .toList();
        }
    }
}
