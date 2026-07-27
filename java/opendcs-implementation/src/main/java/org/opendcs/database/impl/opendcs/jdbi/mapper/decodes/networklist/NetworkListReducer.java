package org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.networklist;

import java.sql.SQLException;
import java.util.Map;

import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;

import decodes.db.NetworkList;
import decodes.db.NetworkListEntry;

public final class NetworkListReducer implements LinkedHashMapRowReducer<Long,NetworkList>
{

    private final NetworkListMapper listMapper;
    private final NetworkListEntryMapper listEntryMapper;

    public NetworkListReducer(NetworkListMapper listMapper, NetworkListEntryMapper listEntryMapper)
    {
        this.listMapper = listMapper;
        this.listEntryMapper = listEntryMapper;
    }

    @Override
    public void accumulate(Map<Long, NetworkList> container, RowView view)
    {
        try
        {
            final var key = view.getColumn(listMapper.column(NetworkListMapper.Columns.ID), Long.class);
            var list = container.computeIfAbsent(key, newKey -> view.getRow(NetworkList.class));

            if (listEntryMapper != null)
            {
                var entry = view.getRow(NetworkListEntry.class);

                if (entry != null)
                {
                    entry.parent = list;
                    list.addEntry(entry);
                }
            }
        }
        catch (SQLException ex)
        {
            throw new UnableToExecuteStatementException("Unable to process result row.", ex, null);
        }
        
    }
    
}
