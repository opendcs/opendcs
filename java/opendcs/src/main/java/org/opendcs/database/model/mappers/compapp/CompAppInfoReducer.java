package org.opendcs.database.model.mappers.compapp;

import java.util.Map;
import java.util.function.BiConsumer;


import org.jdbi.v3.core.result.RowView;
import org.opendcs.database.model.mappers.properties.PropertiesMapper;

import decodes.tsdb.CompAppInfo;

import static org.opendcs.database.model.mappers.PrefixRowMapper.addUnderscoreIfMissing;

public final class CompAppInfoReducer implements BiConsumer<Map<Long, CompAppInfo>, RowView>
{
    private final String prefixAppInfo;

    public CompAppInfoReducer()
    {
        this("a");
    }

    public CompAppInfoReducer(String prefixAppInfo)
    {
        this.prefixAppInfo = addUnderscoreIfMissing(prefixAppInfo);
    }

    @Override
    public void accept(Map<Long, CompAppInfo> map, RowView rowView)
    {
        var appInfo = map.computeIfAbsent(rowView.getColumn(prefixAppInfo + "loading_application_id", Long.class),
                qid -> rowView.getRow(CompAppInfo.class)
        );
        var prop = rowView.getRow(PropertiesMapper.PAIR_STRING_STRING);
        if (prop.first != null)
        {
            appInfo.setProperty(prop.first, prop.second);
        }
    }
}
