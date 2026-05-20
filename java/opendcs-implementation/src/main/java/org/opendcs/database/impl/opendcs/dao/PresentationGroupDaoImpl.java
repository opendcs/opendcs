
package org.opendcs.database.impl.opendcs.dao;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.PresentationGroupDao;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.presentationgroup.DataPresentationMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.presentationgroup.PresentationGroupAccumulator;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.presentationgroup.PresentationGroupMapper;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlErrorMessages;
import org.opendcs.utils.sql.SqlQueries;
import org.openide.util.lookup.ServiceProvider;

import decodes.db.PresentationGroup;
import decodes.sql.DbKey;

@ServiceProvider(service = PresentationGroupDao.class)
public class PresentationGroupDaoImpl implements PresentationGroupDao
{
    final static String SELECT_QUERY = """
            with pg (id, name, inheritsfrom, lastmodifytime, isproduction) as (
                select id, name, inheritsfrom, lastmodifytime, isproduction
                from presentationgroup
                <where>
                order by name <collate> asc
                <limit>
            )
            select 
                pg.id pg_id, pg.name pg_name, pg.inheritsfrom pg_inheritsfrom, 
                pg.lastmodifytime pg_lastmodifytime, pg.isproduction pg_isproduction,

                dp.id dp_id, dp.unitabbr dp_unitabbr, dp.maxdecimals dp_maxdecimals,
                dp.max_value dp_max_value, dp.min_value dp_min_value,

                dt.id dt_id, dt.standard dt_standard, dt.code dt_code, dt.display_name dt_display_name

            from pg
            left outer join datapresentation dp on dp.groupid = pg.id
            left outer join datatype dt on dt.id = dp.datatypeid
            
            
            order by pg.name <collate> asc
            

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
                        .map(v -> v)
                        .findFirst();
        }
    }

    @Override
    public PresentationGroup save(DataTransaction tx, PresentationGroup group) throws OpenDcsDataException
    {
        return null;
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