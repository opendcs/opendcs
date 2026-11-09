package org.opendcs.database.model.mappers.compapp;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import java.util.function.BiConsumer;


import org.jdbi.v3.core.result.RowView;
import org.opendcs.database.model.mappers.properties.PropertiesMapper;
import org.opendcs.utils.sql.PropertyBindings;

import decodes.tsdb.CompAppInfo;

import static org.opendcs.database.model.mappers.OpenDcsTableMapper.addUnderscoreIfMissing;


public final class CompAppInfoReducer implements BiConsumer<Map<Long, CompAppInfo>, RowView>
{
    public static final DateTimeFormatter LAST_MODIFIED_SDF = PropertyBindings.LAST_MODIFIED_FORMATTER;

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
            if (PropertyBindings.LAST_MODIFIED.equalsIgnoreCase(prop.first))
            {
                var tmp = LAST_MODIFIED_SDF.parse(prop.second);
                appInfo.setLastModified(Date.from(Instant.from(tmp)));
            }
            else
            {
            appInfo.setProperty(prop.first, prop.second);
            }
        }
    }
}
