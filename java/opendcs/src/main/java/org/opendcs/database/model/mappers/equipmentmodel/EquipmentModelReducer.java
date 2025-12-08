package org.opendcs.database.model.mappers.equipmentmodel;

import static org.opendcs.database.model.mappers.PrefixRowMapper.addUnderscoreIfMissing;

import java.util.Map;
import java.util.function.BiConsumer;

import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.result.RowView;
import org.opendcs.utils.sql.GenericColumns;

import decodes.db.EquipmentModel;
import ilex.util.Pair;

public class EquipmentModelReducer implements BiConsumer<Map<Long, EquipmentModel>, RowView>
{
    private final String equipmentModelPrefix;

    public EquipmentModelReducer(String equipmentModelPrefix)
    {
        this.equipmentModelPrefix = addUnderscoreIfMissing(equipmentModelPrefix);
    }

    @Override
    public void accept(Map<Long, EquipmentModel> map, RowView rowView)
    {
        var em = map.computeIfAbsent(rowView.getColumn(equipmentModelPrefix+GenericColumns.ID, Long.class), 
                                     emId -> rowView.getRow(EquipmentModel.class));
        var prop = rowView.getRow(new GenericType<Pair<String,String>>() {});
        em.properties.setProperty(prop.first, prop.second);
    }
    
}
