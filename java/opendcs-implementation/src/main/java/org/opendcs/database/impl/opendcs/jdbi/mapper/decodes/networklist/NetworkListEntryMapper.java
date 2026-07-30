package org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.networklist;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.database.sql.TableColumnDefinition;
import org.opendcs.utils.sql.GenericColumns;

import decodes.db.NetworkListEntry;

public final class NetworkListEntryMapper extends PrefixRowMapper<NetworkListEntry,NetworkListEntryMapper.Columns>
{
    protected NetworkListEntryMapper(String prefix)
    {
        super(prefix, "networklistentry", Columns.class);
    }

    @Override
    public NetworkListEntry map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        var transportId = rs.getString(column(Columns.TRANSPORT_ID));
        if (rs.wasNull())
        {
            return null;
        }
        var entry = new NetworkListEntry(null, transportId);
        entry.setDescription(rs.getString(column(Columns.DESCRIPTION)));
        entry.setPlatformName(rs.getString(column(Columns.PLATFORM_NAME)));
        return entry;
    }

    public static NetworkListEntryMapper withPrefix(String prefix)
    {
        return new NetworkListEntryMapper(prefix);
    }
    
    public enum Columns implements TableColumnDefinition<Columns>
    {
        NETWORKLIST_ID("networklistid"),
        TRANSPORT_ID("transportid"),
        PLATFORM_NAME("platform_name"),
        DESCRIPTION(GenericColumns.DESCRIPTION)
        ;

        private final String column;

        Columns(String column)
        {
            this.column = column;
        }
        Columns(GenericColumns other)
        {
            this.column = other.column();
        }

        @Override
        public String column()
        {
            return this.column;
        }

        @Override
        public Enum<Columns> getIdColumn()
        {
            return null;
        }
        
    }    
}
