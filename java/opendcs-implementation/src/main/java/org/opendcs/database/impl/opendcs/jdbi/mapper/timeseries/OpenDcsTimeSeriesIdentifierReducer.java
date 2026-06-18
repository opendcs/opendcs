package org.opendcs.database.impl.opendcs.jdbi.mapper.timeseries;

import java.util.Map;

import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;
import org.opendcs.database.model.mappers.PrefixRowMapper;

import decodes.db.DataType;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.tsdb.TimeSeriesIdentifier;

public class OpenDcsTimeSeriesIdentifierReducer implements LinkedHashMapRowReducer<Long, TimeSeriesIdentifier>
{
    private final String rootPrefix;

    public OpenDcsTimeSeriesIdentifierReducer(String rootPrefix)
    {
        this.rootPrefix = PrefixRowMapper.addUnderscoreIfMissing(rootPrefix);
    }

    @Override
    public void accumulate(Map<Long, TimeSeriesIdentifier> container, RowView rowView)
    {
        final var tsi = container.computeIfAbsent(rowView.getColumn(rootPrefix + "id", Long.class), 
                                            id -> rowView.getRow(TimeSeriesIdentifier.class));

        final var dt = rowView.getRow(DataType.class);
        if (dt != null)
        {
            tsi.setDataType(dt);
        }

        if(!tsi.getSite().idIsSet())
        {
            var site = rowView.getRow(Site.class);
            if (site != null)
            {
                tsi.setSite(site);
            }
        }

        var siteName = rowView.getRow(SiteName.class);
        if (siteName != null)
        {
            tsi.getSite().addName(siteName);
        }
    }
    
}
