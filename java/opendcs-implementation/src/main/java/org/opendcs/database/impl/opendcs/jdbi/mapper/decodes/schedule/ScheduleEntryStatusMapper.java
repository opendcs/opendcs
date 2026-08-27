package org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.schedule;

import decodes.db.ScheduleEntryStatus;
import decodes.sql.DbKey;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.database.sql.TableColumnDefinition;
import org.opendcs.utils.sql.SqlErrorMessages;

public class ScheduleEntryStatusMapper extends PrefixRowMapper<ScheduleEntryStatus, ScheduleEntryStatusMapper.Columns>
{
    ScheduleEntryStatusMapper(String prefix)
    {
        super(prefix, "schedule_entry_status", Columns.class);
    }

    @Override
    public ScheduleEntryStatus map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        ColumnMapper<DbKey> keyMapper = ctx.findColumnMapperFor(DbKey.class)
                                                    .orElseThrow(() -> new SQLException(SqlErrorMessages.DBKEY_MAPPER_NOT_FOUND));
        ColumnMapper<Date> dateMapper = ctx.findColumnMapperFor(Date.class)
                                .orElseThrow(() -> new SQLException(SqlErrorMessages.TIME_MAPPER_NOT_FOUND));
        var id = keyMapper.map(rs, column(Columns.ID), ctx);

        if (DbKey.isNull(id) || rs.wasNull())
        {
            return null;
        }

        var status = new ScheduleEntryStatus(id);
        status.setScheduleEntryId(keyMapper.map(rs, column(Columns.SCHEDULE_ENTRY_ID), ctx));
        status.setRunStart(dateMapper.map(rs, column(Columns.RUN_START_TIME), ctx));
        status.setLastMessageTime(dateMapper.map(rs, column(Columns.LAST_MESSAGE_TIME), ctx));
        status.setRunStop(dateMapper.map(rs, column(Columns.RUN_COMPLETE_TIME), ctx));
        status.setLastModified(dateMapper.map(rs, column(Columns.LAST_MODIFIED), ctx));
        status.setHostname(rs.getString(column(Columns.HOSTNAME)));
        status.setRunStatus(rs.getString(column(Columns.RUN_STATUS)));
        status.setNumMessages(rs.getInt(column(Columns.NUM_MESSAGES)));
        status.setNumDecodesErrors(rs.getInt(column(Columns.NUM_DECODE_ERRORS)));
        status.setNumPlatforms(rs.getInt(column(Columns.NUM_PLATFORMS)));
        status.setLastSource(rs.getString(column(Columns.LAST_SOURCE)));
        status.setLastConsumer(rs.getString(column(Columns.LAST_CONSUMER)));
        status.setScheduleEntryName(rs.getString(column(Columns.SCHEDULE_ENTRY_NAME)));

        return status;
    }

    public static ScheduleEntryStatusMapper withPrefix(String prefix)
    {
        return new ScheduleEntryStatusMapper(prefix);
    }

    public enum Columns implements TableColumnDefinition
    {
        ID("schedule_entry_status_id"),
        SCHEDULE_ENTRY_ID("schedule_entry_id"),
        SCHEDULE_ENTRY_NAME("schedule_entry_name"),
        RUN_START_TIME("run_start_time"),
        LAST_MESSAGE_TIME("last_message_time"),
        RUN_COMPLETE_TIME("run_complete_time"),
        HOSTNAME("hostname"),
        RUN_STATUS("run_status"),
        NUM_MESSAGES("num_messages"),
        NUM_DECODE_ERRORS("num_decode_errors"),
        NUM_PLATFORMS("num_platforms"),
        LAST_SOURCE("last_source"),
        LAST_CONSUMER("last_consumer"),
        LAST_MODIFIED("last_modified")
        ;

        private final String column;

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
