package org.opendcs.database.model.mappers.sites;

import static org.opendcs.database.model.mappers.PrefixRowMapper.addUnderscoreIfMissing;

import java.util.Map;
import java.util.function.BiConsumer;

import org.jdbi.v3.core.result.RowView;
import org.opendcs.database.model.mappers.properties.PropertiesMapper;

import decodes.db.Site;
import decodes.db.SiteName;

public class OpenDcsSiteReducer implements BiConsumer<Map<Long, Site>, RowView>
{
    private String prefixSite;

    public OpenDcsSiteReducer()
    {
        this("s");
    }

    public OpenDcsSiteReducer(String prefixSite)
    {
        this.prefixSite = addUnderscoreIfMissing(prefixSite);
    }

    @Override
    public void accept(Map<Long, Site> map, RowView rowView)
    {
        var site = map.computeIfAbsent(rowView.getColumn(prefixSite + "id", Long.class),
                qid -> rowView.getRow(Site.class)
        );
        var prop = rowView.getRow(PropertiesMapper.PAIR_STRING_STRING);
        if (prop.first != null && prop.second != null)
        {
            site.setProperty(prop.first, prop.second);
        }

        var siteName = rowView.getRow(SiteName.class);
        if (siteName != null)
        {
            siteName.setSite(site);
            site.addName(siteName);
        }
    }

}
