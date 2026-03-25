package org.opendcs.database.impl.opendcs.dao;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.DatabaseEngine;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.DataSourceDao;
import org.opendcs.database.model.mappers.datasource.DataSourceAccumulator;
import org.opendcs.database.model.mappers.datasource.DataSourceMapper;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlErrorMessages;
import org.openide.util.lookup.ServiceProvider;

import decodes.db.DataSource;
import decodes.db.DatabaseException;
import decodes.sql.DbKey;
import decodes.sql.KeyGenerator;

@ServiceProvider(service = DataSourceDao.class)
public class DataSourceDaoImpl implements DataSourceDao
{

    @Override
    public Optional<DataSource> getDataSource(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var ctx = tx.getContext();
        var dbEngine = ctx.getDatabase();
        final String querySql = """
                    select ds.id as ds_id, ds.name ds_name, ds.datasourcetype ds_datasourcetype, ds.datasourcearg ds_datasourcearg,
                           dsgm.sequencenum as dsm_sequencenum, 
                           dsm.id as dsm_id, dsm.name dsm_name, dsm.datasourcetype dsm_datasourcetype, dsm.datasourcearg dsm_datasourcearg
                      from datasource ds
                      left outer join datasourcegroupmember dsgm on ds.id = dsgm.groupid
                      left outer join datasource dsm on dsm.id = dsgm.memberid
                     where ds.id = :id
                     order by ds.name <collate> asc, 
                              dsm_sequencenum asc
                """;
        try (var query = handle.createQuery(querySql))
        {
            return query.define("collate", dbEngine == DatabaseEngine.POSTGRES ? "COLLATE \"C\"" : "COLLATE BINARY")
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
        final String querySql = """
                    select ds.id as ds_id, ds.name ds_name, ds.datasourcetype ds_datasourcetype, ds.datasourcearg ds_datasourcearg,
                           dsgm.sequencenum as dsm_sequencenum, 
                           dsm.id as dsm_id, dsm.name dsm_name, dsm.datasourcetype dsm_datasourcetype, dsm.datasourcearg dsm_datasourcearg
                      from datasource ds
                      left outer join datasourcegroupmember dsgm on ds.id = dsgm.groupid
                      left outer join datasource dsm on dsm.id = dsgm.memberid
                      
                     where ds.name = :name
                     order by ds.name <collate> asc, 
                              dsm_sequencenum asc
                """;
        try (var query = handle.createQuery(querySql))
        {
            return query.define("collate", dbEngine == DatabaseEngine.POSTGRES ? "COLLATE \"C\"" : "COLLATE BINARY")
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
                 .bind("datasourcearg", dataSource.getArguments())
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
                    insertGroupQuery.bind("groupid", id)
                                    .bind("sequencenum", sequence)
                                    .bind("memberid", member.getId())
                                    .add();
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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getDataSources'");
    }
    
}
