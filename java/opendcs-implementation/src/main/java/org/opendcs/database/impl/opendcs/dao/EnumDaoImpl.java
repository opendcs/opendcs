package org.opendcs.database.impl.opendcs.dao;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jdbi.v3.core.Handle;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.DatabaseEngine;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.EnumDao;
import org.opendcs.database.model.mappers.dbenum.DbEnumBuilderMapper;
import org.opendcs.database.model.mappers.dbenum.DbEnumBuilderReducer;
import org.opendcs.database.model.mappers.dbenum.EnumValueMapper;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlKeywords;
import org.openide.util.lookup.ServiceProvider;

import decodes.db.DatabaseException;
import decodes.db.DbEnum;
import decodes.db.DbEnum.DbEnumBuilder;
import decodes.db.EnumValue;
import decodes.sql.DbKey;
import decodes.sql.KeyGenerator;

import static org.opendcs.utils.sql.SqlQueries.addLimitOffset;

@ServiceProvider(service = EnumDao.class)
public class EnumDaoImpl implements EnumDao 
{
    
    @Override
    public Collection<DbEnum> getEnums(DataTransaction tx, int limit, int offset) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException("Unable to get Database connection object."));

        final String queryText = """
                with e (id, name, defaultValue, description) as (
                    select id, name, defaultValue, description from enum
                      order by id asc
                      <limit>
                    )
                  select e.id e_id, e.name e_name, e.defaultValue e_defaultValue, e.description e_description,
                       v.enumvalue v_enumvalue, v.description v_description, v.execclass v_execclass,
                       v.editclass v_editclass, v.sortnumber v_sortnumber
                  from e
                  join enumvalue v on e.id = v.enumid
                  order by e.id asc
                """;

        try (var query = handle.createQuery(queryText))
        {
            if (limit != -1)
            {
                query.bind(SqlKeywords.LIMIT, limit);
            }

            if (offset != -1)
            {
                query.bind(SqlKeywords.OFFSET, offset);
            }
            return query.define("limit", addLimitOffset(limit, offset))
                        .registerRowMapper(DbEnumBuilder.class, DbEnumBuilderMapper.withPrefix("e"))
                         .registerRowMapper(EnumValue.class, EnumValueMapper.withPrefix("v"))
                         .reduceRows(DbEnumBuilderReducer.DBENUM_BUILDER_REDUCER)
                         .map(DbEnumBuilder::build)
                         .collect(Collectors.toList());
        }
    }

    @Override
    public Optional<DbEnum> getEnum(DataTransaction tx, String enumName) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException("Unable to get Database connection object."));
        try (var query = handle.createQuery("select id from enum where upper(name) = upper(:name)"))
        {
            var id = query.bind(GenericColumns.NAME, enumName)
                          .mapTo(DbKey.class)
                          .findOne();
            return getEnum(tx, id.orElse(DbKey.NullKey));
        }
    }

    @Override
    public Optional<DbEnum> getEnum(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        if (DbKey.isNull(id))
        {
            throw new OpenDcsDataException("Unable to search for enum with null ID");
        }
        final String queryText ="""
                select e.id e_id, e.name e_name, e.defaultValue e_defaultValue, e.description e_description,
                       v.enumvalue v_enumvalue, v.description v_description, v.execclass v_execclass,
                       v.editclass v_editclass, v.sortnumber v_sortnumber
                  from enum e
                  join enumvalue v on e.id = v.enumid
                 where e.id = :id
                """;
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException("Unable to get Database connection object."));
        try (var query = handle.createQuery(queryText))
        {
            return query.bind(GenericColumns.ID, id)
                        .registerRowMapper(DbEnumBuilder.class, DbEnumBuilderMapper.withPrefix("e"))
                        .registerRowMapper(EnumValue.class, EnumValueMapper.withPrefix("v"))
                        .reduceRows(DbEnumBuilderReducer.DBENUM_BUILDER_REDUCER)
                        .map(DbEnumBuilder::build)
                        .findFirst();
        }
    }

    @Override
    public DbEnum writeEnum(DataTransaction tx, DbEnum dbEnum) throws OpenDcsDataException
    {
        var context = tx.getContext();
        var keyGen = context.getGenerator(KeyGenerator.class)
                            .orElseThrow(() -> new OpenDcsDataException("No Keygenerator configured."));
        var dbEngine = context.getDatabase();

        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException("Unable to get Database connection object."));
        final String enumMergeText = """
                    MERGE into enum e
                    USING (select :id id, :name name, :defaultValue defaultValue, :description description <dual>) s
                    ON (e.id = s.id)
                    WHEN MATCHED THEN
                        update set name = s.name, defaultValue = s.defaultValue, description = s.description
                    WHEN NOT MATCHED THEN
                        insert(id, name, defaultValue, description)
                        values(s.id, s.name, s.defaultValue, s.description)
                """;
        final String removeValuesText = "delete from enumvalue where enumid = :id";
        final String addValueText = """
                insert into enumvalue(enumid, enumvalue, description, execclass, editclass, sortnumber)
                values (:id, :enumvalue, :description, :execclass, :editclass, :sortnumber)
                """;

        try (var enumMerge = handle.createUpdate(enumMergeText)
                                   .define("dual", dbEngine == DatabaseEngine.ORACLE ? "from dual" : "");
             var removeValues = handle.createUpdate(removeValuesText);
             var addValues = handle.prepareBatch(addValueText))
        {
            final DbKey id = dbEnum.idIsSet() ? dbEnum.getId() : keyGen.getKey("enum", handle.getConnection());
            enumMerge.bind(GenericColumns.ID, id)
                     .bind(GenericColumns.NAME, dbEnum.enumName)
                     .bind("defaultValue", dbEnum.getDefault())
                     .bind(GenericColumns.DESCRIPTION, dbEnum.getDescription())
                     .execute();

            removeValues.bind(GenericColumns.ID, id).execute();

            for (var enumValue: dbEnum.values())
            {
                addValues.bind(GenericColumns.ID, id)
                         .bind("enumvalue", enumValue.getValue())
                         .bind(GenericColumns.DESCRIPTION, enumValue.getDescription())
                         .bind("execclass", enumValue.getExecClassName())
                         .bind("editclass", enumValue.getEditClassName())
                         .bind("sortnumber", enumValue.getSortNumber())
                         .add();
            }
            addValues.execute();
            var dbEnumDb = getEnum(tx, id).orElseThrow(() -> new OpenDcsDataException("Could not retrieve the enum we just saved."));
			return dbEnumDb;
        }
        catch (DatabaseException ex)
        {
            throw new OpenDcsDataException("Unable to generate new key for Enum", ex);
        }
    }

    @Override
    public void deleteEnum(DataTransaction tx, DbKey dbEnumId) throws OpenDcsDataException
    {
        if (DbKey.isNull(dbEnumId))
        {
            throw new OpenDcsDataException("Cannot delete an enum that doesn't exist.");
        }
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException("Unable to get Database connection object."));
        final String deleteValuesSql = "delete from enumvalue where enumid = :id";
        final String deleteEnumSql = "delete from enum where id = :id";
        try (var deleteValues = handle.createUpdate(deleteValuesSql);
             var deleteEnum = handle.createUpdate(deleteEnumSql))
        {
            deleteValues.bind("id", dbEnumId).execute();
            deleteEnum.bind("id", dbEnumId).execute();
        }
    }
}
