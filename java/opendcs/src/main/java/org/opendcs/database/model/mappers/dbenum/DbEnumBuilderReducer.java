package org.opendcs.database.model.mappers.dbenum;

import java.util.Map;
import java.util.function.BiConsumer;

import org.jdbi.v3.core.result.RowView;

import decodes.db.DbEnum.DbEnumBuilder;
import decodes.db.EnumValue;
import decodes.sql.DbKey;

public class DbEnumBuilderReducer implements BiConsumer<Map<DbKey, DbEnumBuilder>, RowView>
{
    public static final DbEnumBuilderReducer DBENUM_BUILDER_REDUCER = new DbEnumBuilderReducer();


    @Override
    public void accept(Map<DbKey, DbEnumBuilder> map, RowView rowView)
    {
        var id = rowView.getRow(DbEnumBuilder.class).getId();
        var builder = map.computeIfAbsent(id, eid -> rowView.getRow(DbEnumBuilder.class));
        var enumValue = rowView.getRow(EnumValue.class);
        builder.withValue(enumValue);
    }
}
