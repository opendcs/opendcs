package org.opendcs.database.impl.opendcs.dao;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.EquipmentModelDai;
import org.opendcs.database.model.mappers.equipmentmodel.EquipmentModelMapper;
import org.opendcs.database.model.mappers.equipmentmodel.EquipmentModelReducer;
import org.opendcs.database.model.mappers.properties.PropertiesMapper;

import decodes.db.EquipmentModel;
import decodes.sql.DbKey;

public class EquipmentModelImpl implements EquipmentModelDai
{
    private final OpenDcsDatabase db;

    public EquipmentModelImpl(OpenDcsDatabase db)
    {
        this.db = db;
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
                       .orElseThrow(() -> new OpenDcsDataException("Now Jdbi Handle available."));
        final var emQuerySQL = """
                select em.id e_id, em.name e_name, em.model e_model, em.description e_description,
                       em.equipmentType e_equipmentType, p.name p_name, p.value p_value
                  from equipmentmodel em 
                  join equipmentproperty p on p.equipment_id = em.id
                  where id = :id
                  sort by em.id, p.name
                """;
        try (var emQuery = handle.createQuery(emQuerySQL))
        {
            return emQuery.bind("id", id)
                          .registerRowMapper(EquipmentModelMapper.withPrefix("e"))
                          .registerRowMapper(PropertiesMapper.withPrefix("p"))
                          .reduceRows(new EquipmentModelReducer("e"))
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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'saveEquipmentModel'");
    }
}
