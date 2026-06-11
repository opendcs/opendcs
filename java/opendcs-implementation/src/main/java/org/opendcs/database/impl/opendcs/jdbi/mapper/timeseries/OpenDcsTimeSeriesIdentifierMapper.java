package org.opendcs.database.impl.opendcs.jdbi.mapper.timeseries;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.database.sql.TableColumnDefinition;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlErrorMessages;

import decodes.cwms.CwmsTsId;
import decodes.sql.DbKey;
import decodes.tsdb.TimeSeriesIdentifier;
import opendcs.opentsdb.OffsetErrorAction;

public class OpenDcsTimeSeriesIdentifierMapper extends PrefixRowMapper<TimeSeriesIdentifier,OpenDcsTimeSeriesIdentifierMapper.Columns>
{

    protected OpenDcsTimeSeriesIdentifierMapper(String prefix)
    {
        super(prefix, Columns.class);
    }

    @Override
    public TimeSeriesIdentifier map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        ColumnMapper<DbKey> columnMapperForKey = ctx.findColumnMapperFor(DbKey.class)
                                                    .orElseThrow(() -> new SQLException(SqlErrorMessages.DBKEY_MAPPER_NOT_FOUND));
        ColumnMapper<Date> dateMapper = ctx.findColumnMapperFor(Date.class)
                                .orElseThrow(() -> new SQLException(SqlErrorMessages.TIME_MAPPER_NOT_FOUND));
        var ret = new CwmsTsId();
        ret.setKey(columnMapperForKey.map(rs, column(Columns.ID), ctx));
        ret.setUniqueString(rs.getString(column(Columns.UNIQUE_STRING)));
        ret.setSiteName(rs.getString(column(Columns.SITE_NAME)));
        ret.setDescription(rs.getString(column(Columns.DESCRIPTION)));
        ret.setActive(rs.getBoolean(column(Columns.ACTIVE_FLAG)));
        ret.setVersion(rs.getString(column(Columns.VERSION)));
        ret.setStorageTable(rs.getInt(column(Columns.STORAGE_TABLE)));
        ret.setStorageType(rs.getString(column(Columns.STORAGE_TYPE)).charAt(0));
        ret.setStorageUnits(rs.getString(column(Columns.STORAGE_UNITS)));
        ret.setInterval(rs.getString(column(Columns.INTERVAL)));
        ret.setDuration(rs.getString(column(Columns.DURATION)));
        ret.setLastModified(dateMapper.map(rs, column(Columns.MODIFY_TIME), ctx));
        ret.setUtcOffset(rs.getInt(column(Columns.UTC_OFFSET)));
        ret.setAllowDstOffsetVariation(rs.getBoolean(column(Columns.ALLOW_DST_OFFSET_VARIATION)));
        ret.setOffsetErrorAction(OffsetErrorAction.fromString(rs.getString(column(Columns.OFFSET_ERROR_ACTION))));
        return ret;
    }

    public static OpenDcsTimeSeriesIdentifierMapper withPrefix(String prefix)
    {
        return new OpenDcsTimeSeriesIdentifierMapper(prefix);
    }

    public enum Columns implements TableColumnDefinition
    {
        ID(GenericColumns.ID),
        UNIQUE_STRING("unique_string"),
        SITE_NAME("site_name"),
        DESCRIPTION(GenericColumns.DESCRIPTION),
        ACTIVE_FLAG("active_flag"),
        STATISTICS_CODE("statistics_code"),
        STORAGE_TABLE("storage_table"),
        STORAGE_TYPE("storage_type"),
        STORAGE_UNITS("storage_units"),
        INTERVAL("interval"),
        DURATION("duration"),
        MODIFY_TIME("modify_time"),
        UTC_OFFSET("utc_offset"),
        ALLOW_DST_OFFSET_VARIATION("allow_dst_offset_variation"),
        OFFSET_ERROR_ACTION("offset_error_action"),
        VERSION("version"),
        // Columns for MERGE
        DATA_TYPE_ID("datatype_id"),
        INTERVAL_ID("interval_id"),
        DURATION_ID("duration_id"),
        SITE_ID("site_id")

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
            return column;
        }
    }
}
