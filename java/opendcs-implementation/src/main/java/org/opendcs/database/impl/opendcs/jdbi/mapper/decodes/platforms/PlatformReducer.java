package org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.platforms;

import java.sql.SQLException;
import java.util.Map;

import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;

import decodes.db.Platform;

public final class PlatformReducer implements LinkedHashMapRowReducer<Long,Platform>
{
    private final PlatformMapper platformMapper;

    public PlatformReducer(PlatformMapper platformMapper)
    {
        this.platformMapper = platformMapper;
    }    

    @Override
    public void accumulate(Map<Long, Platform> container, RowView view)
    {
        try
        {
            final var key = view.getColumn(platformMapper.column(PlatformMapper.Columns.ID), Long.class);

            container.computeIfAbsent(key, newKey -> view.getRow(Platform.class));

        }
        catch (SQLException ex)
        {
            throw new UnableToExecuteStatementException("Unable to process result row.", ex, null);
        }
    }

    
}
