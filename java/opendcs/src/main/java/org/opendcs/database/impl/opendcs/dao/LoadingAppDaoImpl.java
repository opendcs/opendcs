package org.opendcs.database.impl.opendcs.dao;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.DatabaseEngine;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.LoadingAppDao;
import org.opendcs.database.model.mappers.compapp.CompAppInfoMapper;
import org.opendcs.database.model.mappers.compapp.CompAppInfoReducer;
import org.opendcs.database.model.mappers.properties.PropertiesMapper;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlErrorMessages;
import org.opendcs.utils.sql.SqlKeywords;
import org.opendcs.utils.sql.SqlQueries;
import org.openide.util.lookup.ServiceProvider;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.db.DatabaseException;
import decodes.sql.DbKey;
import decodes.sql.KeyGenerator;
import decodes.tsdb.CompAppInfo;

import static org.opendcs.utils.sql.SqlQueries.addLimitOffset;

@ServiceProvider(service = LoadingAppDao.class)
public final class LoadingAppDaoImpl implements LoadingAppDao
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
    private static final String SELECT_QUERY = """
        with app (loading_application_id, loading_application_name, cmmnt, manual_edit_app) as (
            select loading_application_id, loading_application_name, cmmnt, manual_edit_app
              from hdb_loading_application
            <where>
            order by loading_application_name <collate> asc
            <limit>
        )
        select app.loading_application_id a_loading_application_id, app.loading_application_name a_loading_application_name,
               app.cmmnt a_cmmnt, app.manual_edit_app a_manual_edit_app, p.prop_name p_prop_name, p.prop_value p_prop_value
        from app
        left outer join ref_loading_application_prop p on p.loading_application_id = app.loading_application_id
        order by app.loading_application_name <collate> asc, p.prop_name <collate> asc
    """;

    private static final String DELETE_APP_PROPERTIES = "delete from ref_loading_application_prop where loading_application_id = :id";

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
                        .define(SqlQueries.WHERE_CLAUSE, "where loading_application_id = :id")
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
                        .define(SqlQueries.WHERE_CLAUSE, "where upper(loading_application_name) = upper(:name)")
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
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var ctx = tx.getContext();
        var dbEngine = ctx.getDatabase();
        var keyGen = ctx.getGenerator(KeyGenerator.class)
                .orElseThrow(() -> new OpenDcsDataException("No key generator configured."));
        final String insertSql = """
                merge into hdb_loading_application app
                using (select :id id, :name name, :comment comment, :manual_edit_app manual_edit_app <dual>) input
                on (app.loading_application_id = input.id)
                when matched then
                    update set loading_application_name = input.name, cmmnt = input.comment, manual_edit_app = input.manual_edit_app
                when not matched then
                    insert(loading_application_id, loading_application_name, cmmnt, manual_edit_app)
                    values(input.id, input.name, input.comment, input.manual_edit_app)
                """;
        try (var mergeQuery = handle.createUpdate(insertSql)
                               .define("dual", dbEngine == DatabaseEngine.ORACLE ? "from dual" : "");
             var deleteProps = handle.createUpdate(DELETE_APP_PROPERTIES);
             var insertProps = handle.prepareBatch("""
                     insert into ref_loading_application_prop(loading_application_id, prop_name, prop_value)
                      values (:id, :name, :value)
                     """)
             )
        {
            DbKey id = appInfo.getAppId();
            var existing = getByName(tx, appInfo.getAppName());
            if (existing.isPresent())
            {
                // If there's an existing app with this name, we'll just assume the provided id, if any, was in error
                id = existing.get().getAppId();
                log.trace("""
                    Using ID from existing App, id={}, that was found. Provided ID was {}.
                    Existing Name is {}. New Name is {}.
                    """,
                    id, appInfo.getId(), existing.get().getAppName(), appInfo.getAppName());
            }
            final var bindKey = !DbKey.isNull(id) ? id : keyGen.getKey("hdb_loading_application", handle.getConnection());
            
            mergeQuery.bind(GenericColumns.ID, bindKey)
                 .bind(GenericColumns.NAME, appInfo.getAppName())
                 .bind("manual_edit_app", appInfo.getManualEditApp() ? "Y" : "N")
                 .bind("comment", appInfo.getComment())
                 .execute();

            deleteProps.bind(GenericColumns.ID, bindKey).execute();
            
            insertProps.bind(GenericColumns.ID, bindKey)
                       .bind(GenericColumns.NAME, "LastModified")
                       .bind("value", CompAppInfoReducer.LAST_MODIFIED_SDF.format(new Date().toInstant()))
                       .add();
            appInfo.getProperties()
                   .forEach((k,v) ->
                   {
                        insertProps.bind(GenericColumns.ID, bindKey);
                        insertProps.bind(GenericColumns.NAME, k.toString());
                        var toSave = v != null ? v.toString() : "";
                        insertProps.bind("value", toSave);
                        insertProps.add();
                   });
            insertProps.execute();

            return getById(tx, bindKey).orElseThrow(() -> new OpenDcsDataException("Unable to retrieve computation app info that we just saved."));
        }
        catch (DatabaseException ex)
        {
            throw new OpenDcsDataException("Unable to generator new for DataType", ex);
        }
    }

    @Override
    public void delete(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        try (var deleteApp = handle.createUpdate("delete from hdb_loading_application where loading_application_id = :id");
             var deleteProperties = handle.createUpdate(DELETE_APP_PROPERTIES)
            )
        {
            deleteProperties.bind(GenericColumns.ID, id).execute();
            deleteApp.bind(GenericColumns.ID, id).execute();
        }
    }

    @Override
    public List<CompAppInfo> getAll(DataTransaction tx, int limit, int offset) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var ctx = tx.getContext();
        var dbEngine = ctx.getDatabase();
        try (var query = handle.createQuery(SELECT_QUERY))
        {
            query.define(SqlQueries.COLLATE_CLAUSE, SqlQueries.collateClauseFor(dbEngine))
                 .define(SqlQueries.WHERE_CLAUSE, "")
                 .define(SqlQueries.LIMIT_CLAUSE, addLimitOffset(limit, offset));
            if (limit >= 0)
            {
                query.bind(SqlKeywords.LIMIT, limit);
            }
            if (offset >= 0)
            {
                query.bind(SqlKeywords.OFFSET, offset);
            }

            return query.registerRowMapper(CompAppInfoMapper.withPrefix("a"))
                        .registerRowMapper(PropertiesMapper.withPrefix("p", true))
                        .reduceRows(new CompAppInfoReducer())
                        .map(m -> m)
                        .toList();
        }
    }
    
}
