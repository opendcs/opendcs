package org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.platforms;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.opendcs.database.model.mappers.sites.OpenDcsSiteReducer;

import decodes.db.Platform;
import decodes.db.Site;
import decodes.db.TransportMedium;
import ilex.util.Pair;

public final class PlatformReducer implements LinkedHashMapRowReducer<Long,Platform>
{
    public static final GenericType<Pair<String,String>> PLATFORM_PROPERTIES = new GenericType<>()
    { /* marker */
    };

    private final PlatformMapper platformMapper;
    private final OpenDcsSiteReducer siteReducer;


    private final LinkedHashMap<Long,Site> sites = new LinkedHashMap<>();

    public PlatformReducer(PlatformMapper platformMapper, OpenDcsSiteReducer siteReducer)
    {
        this.platformMapper = platformMapper;
        this.siteReducer = siteReducer;
    }

    @Override
    public void accumulate(Map<Long, Platform> container, RowView view)
    {
        try
        {
            final var key = view.getColumn(platformMapper.column(PlatformMapper.Columns.ID), Long.class);

            var platform = container.computeIfAbsent(key, newKey -> view.getRow(Platform.class));

            var tm = view.getRow(TransportMedium.class);
            if (tm != null)
            {
                tm.platform = platform;
                platform.transportMedia.add(tm);
            }
            siteReducer.accept(sites, view);
            var siteId = view.getColumn(platformMapper.column(PlatformMapper.Columns.SITE_ID), Long.class);
            if (sites.containsKey(siteId))
            {
                platform.setSite(sites.get(siteId));
            }

            var prop = view.getRow(PLATFORM_PROPERTIES);
            if (prop != null && prop.first != null)
            {
                platform.setProperty(prop.first, prop.second);
            }
        }
        catch (SQLException ex)
        {
            throw new UnableToExecuteStatementException("Unable to process result row.", ex, null);
        }
    }
}
