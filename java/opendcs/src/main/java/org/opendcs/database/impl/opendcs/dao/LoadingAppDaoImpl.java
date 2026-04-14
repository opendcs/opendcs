package org.opendcs.database.impl.opendcs.dao;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.LoadingAppDao;
import org.opendcs.database.model.mappers.compapp.CompAppInfoMapper;
import org.opendcs.database.model.mappers.compapp.CompAppInfoReducer;
import org.opendcs.database.model.mappers.properties.PropertiesMapper;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlErrorMessages;
import org.opendcs.utils.sql.SqlQueries;
import org.openide.util.lookup.ServiceProvider;

import decodes.sql.DbKey;
import decodes.tsdb.CompAppInfo;

import static org.opendcs.utils.sql.SqlQueries.addLimitOffset;

@ServiceProvider(service = LoadingAppDao.class)
public final class LoadingAppDaoImpl implements LoadingAppDao
{

    private static final String SELECT_QUERY = """
        with app (loading_application_id, loading_appliation_name, cmmnt, manual_edit_app) as (
            select loading_application_id, loading_application_name, cmmnt, manual_edit_app
              from hdb_loading_application_id
            <where>
            <limit>
            order by name <collate> asc

        )
        select app.loading_application_id a_loading_application_id, app.loading_application_name a_loading_application_name,
               app.cmmnt a_cmmnt, app.manual_edit_app a_manual_edit_app, p.prop_name p_prop_name, p.prop_value p_prop_value
        from app
        left outer join ref_loading_application_prod p on p.loading_application_id = app.loading_application_id
        order by app.name <collate> asc, p.prop_name <collate> asc
    """;



    @Override
    public Optional<CompAppInfo> getById(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var ctx = tx.getContext();
        var dbEngine = ctx.getDatabase();
        try (var query = handle.createQuery(SELECT_QUERY))
        {
            return query.define(SqlQueries.COLLATE_CLAUSE, SqlQueries.collateClauseFor(dbEngine))
                        .define(SqlQueries.WHERE_CLAUSE, "where location_application_id = :id")
                        .define(SqlQueries.LIMIT_CLAUSE, "")
                        .bind(GenericColumns.ID, id)
                        .registerRowMapper(CompAppInfoMapper.withPrefix("a"))
                        .registerRowMapper(PropertiesMapper.withPrefix("p", true))
                        .reduceRows(new CompAppInfoReducer())
                        .map(m -> m)
                        .findFirst();
        }
    }

    @Override
    public Optional<CompAppInfo> getByName(DataTransaction tx, String name) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var ctx = tx.getContext();
        var dbEngine = ctx.getDatabase();
        try (var query = handle.createQuery(SELECT_QUERY))
        {
            return query.define(SqlQueries.COLLATE_CLAUSE, SqlQueries.collateClauseFor(dbEngine))
                        .define(SqlQueries.WHERE_CLAUSE, "where upper(location_application_name) = upper(:nmae)")
                        .define(SqlQueries.LIMIT_CLAUSE, "")
                        .bind(GenericColumns.NAME, name)
                        .registerRowMapper(CompAppInfoMapper.withPrefix("a"))
                        .registerRowMapper(PropertiesMapper.withPrefix("p", true))
                        .reduceRows(new CompAppInfoReducer())
                        .map(m -> m)
                        .findFirst();
        }
    }

    @Override
    public CompAppInfo save(DataTransaction tx, CompAppInfo appInfo) throws OpenDcsDataException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'save'");
    }

    @Override
    public void delete(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'delete'");
    }

    @Override
    public List<CompAppInfo> getAll(DataTransaction tx, int limit, int offset) throws OpenDcsDataException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAll'");
    }
    
}
