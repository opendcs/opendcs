package org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.platforms;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.opendcs.database.model.mappers.sites.OpenDcsSiteReducer;

import decodes.db.Platform;
import decodes.db.Site;
import decodes.db.TransportMedium;

public final class PlatformReducer implements LinkedHashMapRowReducer<Long,Platform>
{
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
        }
        catch (SQLException ex)
        {
            throw new UnableToExecuteStatementException("Unable to process result row.", ex, null);
        }
    }
}
