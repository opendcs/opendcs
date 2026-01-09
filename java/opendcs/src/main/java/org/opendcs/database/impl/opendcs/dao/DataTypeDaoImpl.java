package org.opendcs.database.impl.opendcs.dao;

import static org.opendcs.utils.sql.SqlQueries.addLimitOffset;

import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.DatabaseEngine;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.DataTypeDao;
import org.opendcs.database.model.mappers.datatype.DataTypeMapper;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlErrorMessages;
import org.opendcs.utils.sql.SqlKeywords;
import org.openide.util.lookup.ServiceProvider;

import decodes.db.DataType;
import decodes.db.DatabaseException;
import decodes.sql.DbKey;
import decodes.sql.KeyGenerator;
import decodes.util.DecodesSettings;

@ServiceProvider(service = DataTypeDao.class)
public class DataTypeDaoImpl implements DataTypeDao
{
    @Override
    public Optional<DataType> getDataType(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        final String querySql = """
                    select id, standard, code, display_name from datatype where id =:id
                """;
        try (var query = handle.createQuery(querySql))
        {
            return query.bind("id", id)
                        .registerRowMapper(DataType.class, DataTypeMapper.withPrefix(""))
                        .mapTo(DataType.class)
                        .findOne();
        }

    }

    @Override
    public DataType saveDataType(DataTransaction tx, DataType dataType) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var ctx = tx.getContext();
        var dbEngine = ctx.getDatabase();
        var keyGen = ctx.getGenerator(KeyGenerator.class)
                .orElseThrow(() -> new OpenDcsDataException("No key generator configured."));
        final String insertSql = """
                merge into datatype dt
                using (select :id id, :standard standard, :code code, :display_name display_name <dual>) input
                on (dt.id = input.id)
                when matched then
                    update set standard = input.standard, code = input.code, display_name = input.display_name                               
                when not matched then
                    insert(id, standard, code, display_name)
                    values(input.id, input.standard, input.code, input.display_name)
                """;
        try (var query = handle.createUpdate(insertSql)
                               .define("dual", dbEngine == DatabaseEngine.ORACLE ? "from dual" : ""))
        {
            final DbKey id = dataType.idIsSet() ? dataType.getId() : keyGen.getKey("datatype", handle.getConnection());
            query.registerRowMapper(DataType.class, DataTypeMapper.withPrefix(""))
                 .bind("id", id)
                 .bind("standard", dataType.getStandard())
                 .bind("code", dataType.getCode())
                 .bind("display_name", dataType.getDisplayName())
                 .execute();
            return getDataType(tx, id).orElseThrow(() -> new OpenDcsDataException("Unable to retrieve DataType we just saved."));
        }
        catch (DatabaseException ex)
        {
            throw new OpenDcsDataException("Unable to generator new for DataType", ex);
        }
    }

    @Override
    public Optional<DataType> lookupDataType(DataTransaction tx, String dataTypeCode) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));

        final String standardPreference = tx.getContext()
                                            .getSettings(DecodesSettings.class)
                                            .orElseThrow(() -> new OpenDcsDataException("No Decodes Settings?"))
                                            .dataTypeStdPreference;
        final String querySql = """
                    select id, standard, code, display_name 
                      from datatype
                     where upper(code) = upper(:code)

                """;
        Optional<DataType> ret = Optional.empty();
        try (var query = handle.createQuery(querySql);
             var iter = query.bind("code", dataTypeCode)
                             .registerRowMapper(DataType.class,
                                             DataTypeMapper.withPrefix(""))
                             .mapTo(DataType.class)
                             .iterator())
        {
            if (iter.hasNext())
            {
                ret = Optional.of(iter.next());
            }

            while (iter.hasNext())
            {
                var tmp = iter.next();
                if (tmp.getStandard().equals(standardPreference))
                {
                    ret = Optional.of(tmp);
                    break; // we got what we came for.
                }
            }
        }
        return ret;
    }

    @Override
    public List<DataType> getDataTypes(DataTransaction tx, int limit, int offset) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        final String dataTypeSelect = """
                    select id, standard, code, display_name from datatype
                """ +
                addLimitOffset(limit, offset);
        try (var q = handle.createQuery(dataTypeSelect))
        {
            if (limit != -1)
            {
                q.bind(SqlKeywords.LIMIT, limit);
            }

            if (offset != -1)
            {
                q.bind(SqlKeywords.OFFSET, offset);
            }
            return q.registerRowMapper(DataType.class, DataTypeMapper.withPrefix(""))
                    .mapTo(DataType.class)
                    .collectIntoList();
        }
    }

    @Override
    public void deleteDataType(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        final var deleteDataTypeSql = "delete from datatype where id = :id";
        try (var deleteDataType = handle.createUpdate(deleteDataTypeSql))
        {
            deleteDataType.bind(GenericColumns.ID, id).execute();
        }
    }
}
