package org.opendcs.database.impl.opendcs.dao;

import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.DatabaseEngine;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.EquipmentModelDao;
import org.opendcs.database.model.mappers.equipmentmodel.EquipmentModelMapper;
import org.opendcs.database.model.mappers.equipmentmodel.EquipmentModelReducer;
import org.opendcs.database.model.mappers.properties.PropertiesMapper;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlErrorMessages;
import org.opendcs.utils.sql.SqlKeywords;
import org.openide.util.lookup.ServiceProvider;

import decodes.db.DatabaseException;
import decodes.db.EquipmentModel;
import decodes.sql.DbKey;
import decodes.sql.KeyGenerator;

import static org.opendcs.utils.sql.SqlQueries.addLimitOffset;

@ServiceProvider(service = EquipmentModelDao.class)
public class EquipmentModelImpl implements EquipmentModelDao
{
    private static final String PROPERTIES_DELETE_SQL = "delete from equipmentproperty where equipmentid = :id";

    @Override
    public List<EquipmentModel> getEquipmentModels(DataTransaction tx, int limit, int offset)
            throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));

        final String queryText = """
                  with em (id, name, company, model, description, equipmentType) as (
                    select id, name, company, model, description, equipmentType
                      from equipmentmodel
                      order by id asc
                      <limit>
                  )
                  select em.id e_id, em.name e_name, em.company e_company, em.model e_model, em.description e_description,
                       em.equipmentType e_equipmentType, p.name p_name, p.prop_value p_prop_value
                  from em
                  left outer join equipmentproperty p on p.equipmentid = em.id
                  order by em.id, p.name
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
                        .registerRowMapper(EquipmentModelMapper.withPrefix("e"))
                        .registerRowMapper(PropertiesMapper.PAIR_STRING_STRING, PropertiesMapper.withPrefix("p"))
                        .reduceRows(new EquipmentModelReducer("e", "p"))
                        .map(m -> m)
                        .toList();
        }
    }

    


    @Override
    public Optional<EquipmentModel> getEquipmentModel(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        final var emQuerySQL = """
                select em.id e_id, em.name e_name, em.company e_company, em.model e_model, em.description e_description,
                       em.equipmentType e_equipmentType, p.name p_name, p.prop_value p_prop_value
                  from equipmentmodel em
                  left outer join equipmentproperty p on p.equipmentid = em.id
                  where id = :id
                  order by em.id, p.name
                """;
        try (var emQuery = handle.createQuery(emQuerySQL))
        {
            return emQuery.bind("id", id)
                          .registerRowMapper(EquipmentModelMapper.withPrefix("e"))
                          .registerRowMapper(PropertiesMapper.PAIR_STRING_STRING, PropertiesMapper.withPrefix("p"))
                          .reduceRows(new EquipmentModelReducer("e", "p"))
                          .map(m -> m)
                          .findFirst();
        }
    }

    @Override
    public Optional<EquipmentModel> getEquipmentModel(DataTransaction tx, String name) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException("No Jdbi Handle available."));
        final var emQuerySQL = """
                select em.id e_id, em.name e_name, em.company e_company, em.model e_model, em.description e_description,
                       em.equipmentType e_equipmentType, p.name p_name, p.prop_value p_prop_value
                  from equipmentmodel em
                  left outer join equipmentproperty p on p.equipmentid = em.id
                  where upper(em.name) = upper(:name)
                  order by em.id, p.name
                """;
        try (var emQuery = handle.createQuery(emQuerySQL))
        {
            return emQuery.bind(GenericColumns.NAME, name)
                          .registerRowMapper(EquipmentModelMapper.withPrefix("e"))
                          .registerRowMapper(PropertiesMapper.PAIR_STRING_STRING, PropertiesMapper.withPrefix("p"))
                          .reduceRows(new EquipmentModelReducer("e", "p"))
                          .map(m -> m)
                          .findFirst();
        }
    }

    @Override
    public void deleteEquipmentModel(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        final var deleteEquipmentModelSql = "delete from equipmentmodel where id = :id";
        try (var deleteProps = handle.createUpdate(PROPERTIES_DELETE_SQL);
             var deleteEquipmentModel = handle.createUpdate(deleteEquipmentModelSql))
        {
            deleteProps.bind(GenericColumns.ID, id).execute();
            deleteEquipmentModel.bind(GenericColumns.ID, id).execute();
        }
    }

    @Override
    public EquipmentModel saveEquipmentModel(DataTransaction tx, EquipmentModel em) throws OpenDcsDataException
    {

        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var emMergeSql = """
                merge into equipmentmodel em
                using (select :id id, :name name, :company company, :model model, :description description, :equipmentType equipmentType <dual>) input
                on (em.id = input.id)
                when matched then
                    update set name = input.name, model = input.model, description = input.description,
                               equipmentType = input.equipmentType, company = input.company
                when not matched then
                    insert(id, name, model, company, description, equipmentType)
                    values(input.id, input.name, input.model, input.company, input.description, input.equipmentType)
                """;
        var ctx = tx.getContext();
        var dbEngine = ctx.getDatabase();
        var keyGen = ctx.getGenerator(KeyGenerator.class)
                        .orElseThrow(() -> new OpenDcsDataException("No key generator configured."));
        var insertPropsSql = "insert into equipmentproperty(equipmentid, name, prop_value) values (:equipmentid, :name, :value)";
        try (var emMerge = handle.createUpdate(emMergeSql)
                                 .define("dual", dbEngine == DatabaseEngine.ORACLE ? "from dual" : "");
             var propsDelete = handle.createUpdate(PROPERTIES_DELETE_SQL);
             var insertProps = handle.prepareBatch(insertPropsSql))
        {
            final DbKey id = em.idIsSet() ? em.getId() : keyGen.getKey("equipmentmodel", handle.getConnection());
            emMerge.bind(GenericColumns.ID, id);
            emMerge.bind(GenericColumns.NAME, em.name);
            emMerge.bind("model", em.model);
            emMerge.bind("company", em.company);
            emMerge.bind(GenericColumns.DESCRIPTION, em.description);
            emMerge.bind("equipmentType", em.equipmentType);
            emMerge.execute();
            propsDelete.bind("id", id).execute();
            em.properties.forEach((k,v) ->
            {
                insertProps.bind("equipmentid", id);
                insertProps.bind(GenericColumns.NAME, k.toString());
                var toSave = v != null ? v.toString() : "";
                insertProps.bind("value", toSave);
                insertProps.add();
            });
            insertProps.execute();

            return getEquipmentModel(tx, id).orElseThrow(() -> new OpenDcsDataException("Unable to retrieve equipment model we just saved."));
        }
        catch (DatabaseException ex)
        {
            throw new OpenDcsDataException("Unable to generate key to save equipment model", ex);
        }
    }
}
