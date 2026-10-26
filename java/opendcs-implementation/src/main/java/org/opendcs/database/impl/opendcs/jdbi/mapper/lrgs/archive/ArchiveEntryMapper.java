package org.opendcs.database.impl.opendcs.jdbi.mapper.lrgs.archive;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.Map;

import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.data.lrgs.archive.ArchiveEntry;
import org.opendcs.data.lrgs.archive.ArchiveEntry.Quality;
import org.opendcs.database.impl.opendcs.jdbi.column.json.ConfigColumnMapper;
import org.opendcs.database.model.mappers.OpenDcsTableMapper;
import org.opendcs.database.sql.TableColumnDefinition;
import org.opendcs.utils.sql.SqlErrorMessages;

public class ArchiveEntryMapper extends OpenDcsTableMapper<ArchiveEntry,ArchiveEntryMapper.Columns>
{
    protected ArchiveEntryMapper(String prefix)
    {
        super(prefix, "lrgs_archive", Columns.class);
    }

    @Override
    public ArchiveEntry map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        var zdtMapper = ctx.findColumnMapperFor(ZonedDateTime.class)
                           .orElseThrow(() -> new SQLException(SqlErrorMessages.ZDT_MAPPER_NOT_FOUND));
        var configMapper = ctx.findColumnMapperFor(ConfigColumnMapper.CONFIG_TYPE)
                              .orElseThrow(() -> new SQLException(SqlErrorMessages.CONFIG_MAPPER_NOT_FOUND));
        var storedAt = zdtMapper.map(rs, column(Columns.STORED_AT), ctx);
        var dateTime = zdtMapper.map(rs, column(Columns.DATA_TIME), ctx);
        var id = rs.getString(column(Columns.IDENTIFIER));
        var rawData = rs.getBytes(column(Columns.IDENTIFIER));
        var quality = rs.getString(column(Columns.QUALITY));
        var header = (Map<String,Object>)configMapper.map(rs, column(Columns.DATA_HEADER), ctx);
        // var interface = // todo
        return new ArchiveEntry(null, id, header, rawData, storedAt, dateTime, Quality.valueOf(quality));
    }

    public static ArchiveEntryMapper withPrefix(String prefix)
    {
        return new ArchiveEntryMapper(prefix);
    }

    public enum Columns implements TableColumnDefinition
    {
        STORED_AT,
        DATA_TIME,
        DATA_HEADER,
        DATA_INTERFACE,
        IDENTIFIER,
        RAW_DATA,
        QUALITY
        ;

        private final String column;

        Columns()
        {
            this.column = this.name().toLowerCase();
        }

        Columns(String column)
        {
            this.column = column;
        }

        @Override
        public String column()
        {
            return this.column;
        }
    }
}
