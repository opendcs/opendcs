package org.opendcs.database.impl.opendcs.dao;

import static org.opendcs.utils.sql.SqlQueries.addLimitOffset;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.DatabaseEngine;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.DataSourceDao;
import org.opendcs.database.model.mappers.datasource.DataSourceAccumulator;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlErrorMessages;
import org.opendcs.utils.sql.SqlKeywords;
import org.opendcs.utils.sql.SqlQueries;
import org.openide.util.lookup.ServiceProvider;

import decodes.db.DataSource;
import decodes.db.DatabaseException;
import decodes.sql.DbKey;
import decodes.sql.KeyGenerator;

@ServiceProvider(service = DataSourceDao.class)
public class DataSourceDaoImpl implements DataSourceDao
{

    private static final String SELECT_QUERY = """
            with primaryDs (id, name, datasourcetype, datasourcearg) as (
                select id, name, datasourcetype, datasourcearg
                from datasource
                <where>
                order by name <collate> asc
                <limit>
            )
            select ds.id as ds_id, ds.name ds_name, ds.datasourcetype ds_datasourcetype, ds.datasourcearg ds_datasourcearg,
                           dsgm.sequencenum as dsm_sequencenum,
                           dsm.id as dsm_id, dsm.name dsm_name, dsm.datasourcetype dsm_datasourcetype, dsm.datasourcearg dsm_datasourcearg
                      from primaryDs ds
                      left outer join datasourcegroupmember dsgm on ds.id = dsgm.groupid
                      left outer join datasource dsm on dsm.id = dsgm.memberid
                     order by ds.name <collate> asc,
                              dsm_sequencenum asc
            """;

    @Override
    public Optional<DataSource> getDataSource(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var ctx = tx.getContext();
        var dbEngine = ctx.getDatabase();
        try (var query = handle.createQuery(SELECT_QUERY))
        {
            return query.define(SqlQueries.COLLATE_CLAUSE, SqlQueries.collateClauseFor(dbEngine))
                        .define(SqlQueries.WHERE_CLAUSE, "where id = :id")
                        .define(SqlQueries.LIMIT_CLAUSE, "")
                        .bind(GenericColumns.ID, id)
                        .reduceResultSet(new LinkedHashMap<>(), DataSourceAccumulator.DATA_SOURCE_ACCUMULATOR)
                        .values()
                        .stream()
                        .findFirst();
        }
    }

    @Override
    public Optional<DataSource> getDataSource(DataTransaction tx, String name) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var ctx = tx.getContext();
        var dbEngine = ctx.getDatabase();
        try (var query = handle.createQuery(SELECT_QUERY))
        {
            return query.define(SqlQueries.COLLATE_CLAUSE, SqlQueries.collateClauseFor(dbEngine))
                        .define(SqlQueries.WHERE_CLAUSE, "where name = :name")
                        .define(SqlQueries.LIMIT_CLAUSE, "")
                        .bind(GenericColumns.NAME, name)
                        // .registerRowMapper(DataSource.class, DataSourceMapper.withPrefix("ds"))
                        // .registerRowMapper(DataSource.class, DataSourceMapper.withPrefix("dsm"))
                        .reduceResultSet(new LinkedHashMap<>(), DataSourceAccumulator.DATA_SOURCE_ACCUMULATOR)
                        .values()
                        .stream()
                        .findFirst();
        }
    }

    @Override
    public DataSource save(DataTransaction tx, DataSource dataSource) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var ctx = tx.getContext();
        var dbEngine = ctx.getDatabase();
        var keyGen = ctx.getGenerator(KeyGenerator.class)
                .orElseThrow(() -> new OpenDcsDataException("No key generator configured."));
        final String insertSql = """
                merge into datasource ds
                using (select :id id, :name name, :datasourcetype datasourcetype, :datasourcearg datasourcearg <dual>) input
                on (ds.id = input.id)
                when matched then
                    update set name = input.name, datasourcetype = input.datasourcetype, datasourcearg = input.datasourcearg
                when not matched then
                    insert(id, name, datasourcetype, datasourcearg)
                    values(input.id, input.name, input.datasourcetype, input.datasourcearg)
                """;
        try (var mergeQuery = handle.createUpdate(insertSql)
                               .define("dual", dbEngine == DatabaseEngine.ORACLE ? "from dual" : "");
             var deleteGroupQuery = handle.createUpdate("delete from datasourcegroupmember where groupid = :id");
             var insertGroupQuery = handle.prepareBatch("""
                     insert into datasourcegroupmember(groupid, sequencenum, memberid)
                      values (:groupid, :sequencenum, :memberid)
                     """)
             )
        {
            final DbKey id = dataSource.idIsSet() ? dataSource.getId() : keyGen.getKey("datasource", handle.getConnection());
            mergeQuery.bind("id", id)
                 .bind(GenericColumns.NAME, dataSource.getName())
                 .bind("datasourcetype", dataSource.dataSourceType)
                 .bind("datasourcearg", dataSource.getDataSourceArg())
                 .execute();

            deleteGroupQuery.bind(GenericColumns.ID, id).execute();

            if (!dataSource.groupMembers.isEmpty())
            {
                int sequence = 0;
                for (final var member: dataSource.groupMembers)
                {

                    if (member == null)
                    {
                        continue; // This logic is in DataSOurceListIO, keeping it for now.
                    }
                    DbKey memberId = member.getId();
                    if (DbKey.isNull(memberId))
                    {
                        memberId = this.getDataSource(tx, member.getName())
                                       .orElseThrow(() -> new OpenDcsDataException("DataSource " + member.getName() + " does not exist"))
                                       .getId();
                    }

                    insertGroupQuery.bind("groupid", id)
                                    .bind("sequencenum", sequence)
                                    .bind("memberid", memberId)
                                    .add();
                    sequence++;
                }
            }
            insertGroupQuery.execute();

            return getDataSource(tx, id).orElseThrow(() -> new OpenDcsDataException("Unable to retrieve DataSource we just saved."));
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
        try (var deleteMembers = handle.createUpdate("delete from datasourcegroupmember where groupid = :id");
             var deleteSource = handle.createUpdate("delete from datasource where id = :id")
            )
        {
            deleteMembers.bind(GenericColumns.ID, id).execute();
            deleteSource.bind(GenericColumns.ID, id).execute();
        }
    }

    @Override
    public List<DataSource> getDataSources(DataTransaction tx, int limit, int offset) throws OpenDcsDataException
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

            return query.reduceResultSet(new LinkedHashMap<>(), DataSourceAccumulator.DATA_SOURCE_ACCUMULATOR)
                        .values()
                        .stream()
                        .toList();
        }
    }

}
