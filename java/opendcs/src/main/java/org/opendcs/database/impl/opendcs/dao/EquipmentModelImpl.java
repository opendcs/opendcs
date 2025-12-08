package org.opendcs.database.impl.opendcs.dao;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.DatabaseEngine;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.EquipmentModelDao;
import org.opendcs.database.model.mappers.equipmentmodel.EquipmentModelMapper;
import org.opendcs.database.model.mappers.equipmentmodel.EquipmentModelReducer;
import org.opendcs.database.model.mappers.properties.PropertiesMapper;
import org.opendcs.utils.sql.GenericColumns;

import decodes.db.DatabaseException;
import decodes.db.EquipmentModel;
import decodes.sql.DbKey;
import decodes.sql.KeyGenerator;
import decodes.tsdb.DbIoException;

public class EquipmentModelImpl implements EquipmentModelDao
{
    private final OpenDcsDatabase db;
    private final KeyGenerator keyGen;

    public EquipmentModelImpl(OpenDcsDatabase db)
    {
        this.db = db;
        this.keyGen = db.getGenerator(KeyGenerator.class)
                        .orElseThrow(() -> new IllegalStateException("No key generator configured."));
    }

    @Override
    public List<EquipmentModel> getEquipmentModels(DataTransaction tx, int limit, int offset)
            throws OpenDcsDataException 
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getEquipmentModels'");
    }

    @Override
    public Optional<EquipmentModel> getEquipmentModel(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException("No Jdbi Handle available."));
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
                          .registerRowMapper(PropertiesMapper.withPrefix("p"))
                          .reduceRows(new EquipmentModelReducer("e", "p"))
                          .map(m -> m)
                          .findFirst();
        }
    }

    @Override
    public Optional<EquipmentModel> getEquipmentModel(DataTransaction tx, Map<String, Object> search)
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getEquipmentModel'");
    }

    @Override
    public void deleteEquipmentModel(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteEquipmentModel'");
    }

    @Override
    public EquipmentModel saveEquipmentModel(DataTransaction tx, EquipmentModel em) throws OpenDcsDataException
    {
        DbKey id = DbKey.NullKey;
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException("No Jdbi Handle available."));

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
        try (var emMerge = handle.createUpdate(emMergeSql)
                                 .define("dual", db.getDatabase() == DatabaseEngine.ORACLE ? "from dual" : ""))
        {
            id = em.idIsSet() ? em.getId() : keyGen.getKey("equipmentmodel", handle.getConnection());
            emMerge.bind(GenericColumns.ID, id);
            emMerge.bind(GenericColumns.NAME, em.name);
            emMerge.bind("model", em.model);
            emMerge.bind("company", em.company);
            emMerge.bind(GenericColumns.DESCRIPTION, em.description);
            emMerge.bind("equipmentType", em.equipmentType);
            int updated = emMerge.execute();
            return getEquipmentModel(tx, id).orElseThrow(() -> new OpenDcsDataException("Unable to retrieve equipment model we just saved."));
        }
        catch (DatabaseException ex)
        {
            throw new OpenDcsDataException("Unable to generate key to save enum", ex);
        }
    }
}
