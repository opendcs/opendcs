package org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.platforms;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.database.sql.TableColumnDefinition;
import org.opendcs.utils.sql.SqlErrorMessages;

import decodes.db.PlatformStatus;
import decodes.sql.DbKey;

public final class PlatformStatusMapper extends PrefixRowMapper<PlatformStatus, PlatformStatusMapper.Columns>
{
    private PlatformStatusMapper(String prefix)
    {
        super(prefix, "platform_status", Columns.class);
    }
    
    @Override
    public PlatformStatus map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        var keyMapper = ctx.findColumnMapperFor(DbKey.class)
                                                    .orElseThrow(() -> new SQLException(SqlErrorMessages.DBKEY_MAPPER_NOT_FOUND));
        var dateMapper = ctx.findColumnMapperFor(Date.class)
                                .orElseThrow(() -> new SQLException(SqlErrorMessages.TIME_MAPPER_NOT_FOUND));

        var id = keyMapper.map(rs, column(Columns.PLATFORM_ID), ctx);
        if (DbKey.isNull(id) || rs.wasNull())
        {
            return null;
        }

        var status = new PlatformStatus(id);
        status.setAnnotation(rs.getString(column(Columns.ANNOTATION)));
        status.setLastFailureCodes(rs.getString(column(Columns.LAST_FAILURE_CODES)));
        status.setLastScheduleEntryStatusId(keyMapper.map(rs, column(Columns.LAST_SCHEDULE_ENTRY_STATUS_ID), ctx));
        status.setLastContactTime(dateMapper.map(rs, column(Columns.LAST_CONTACT_TIME), ctx));
        status.setLastMessageTime(dateMapper.map(rs, column(Columns.LAST_MESSAGE_TIME), ctx));
        status.setLastErrorTime(dateMapper.map(rs, column(Columns.LAST_ERROR_TIME), ctx));
        status.setLastRoutingSpecName(rs.getString(column(Columns.LAST_ROUTING_SPEC_NAME)));
        return status;

    }

    public static PlatformStatusMapper withPrefix(String prefix)
    {
        return new PlatformStatusMapper(prefix);
    }

    public enum Columns implements TableColumnDefinition
    {
        PLATFORM_ID("platform_id"),
        LAST_CONTACT_TIME("last_contact_time"),
        LAST_MESSAGE_TIME("last_message_time"),
        LAST_FAILURE_CODES("last_failure_codes"),
        LAST_ERROR_TIME("last_error_time"),
        LAST_SCHEDULE_ENTRY_STATUS_ID("last_schedule_entry_status_id"),
        ANNOTATION("annotation"),
        LAST_ROUTING_SPEC_NAME("last_routing_spec_name")
        ;

        private final String column;

        Columns(String column)
        {
            this.column = column;
        }

        @Override
        public String column()
        {
            return column;
        }
    }
}
