package org.opendcs.database.impl.opendcs.jdbi.mapper.timeseries;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.database.sql.TableColumnDefinition;
import org.opendcs.utils.sql.SqlErrorMessages;

import decodes.sql.DbKey;
import decodes.tsdb.CpDependsNotify;

@SuppressWarnings("java:S2143")
public class CpDependsNotifyMapper extends PrefixRowMapper<CpDependsNotify, CpDependsNotifyMapper.Columns>
{
    private static final CpDependsNotifyMapper DEFAULT_INSTANCE = new CpDependsNotifyMapper("");

    protected CpDependsNotifyMapper(String prefix)
    {
        super(prefix, Columns.class);
    }

    @Override
    public CpDependsNotify map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        ColumnMapper<DbKey> columnMapperForKey = ctx.findColumnMapperFor(DbKey.class)
                                                    .orElseThrow(() -> new SQLException(SqlErrorMessages.DBKEY_MAPPER_NOT_FOUND));
        ColumnMapper<Date> dateMapper = ctx.findColumnMapperFor(Date.class)
                                .orElseThrow(() -> new SQLException(SqlErrorMessages.TIME_MAPPER_NOT_FOUND));
        var ret = new CpDependsNotify();

        ret.setRecordNum(rs.getLong(column(Columns.RECORD_NUM)));
        ret.setEventType(rs.getString(column(Columns.EVENT_TYPE)).charAt(0));
        ret.setKey(columnMapperForKey.map(rs, column(Columns.KEY), ctx));
        ret.setDateTimeLoaded(dateMapper.map(rs, column(Columns.DATE_TIME_LOADED), ctx));
        return ret;
    }

    public static CpDependsNotifyMapper withNoPrefix()
    {
        return withPrefix(null);
    }

    public static CpDependsNotifyMapper withPrefix(String prefix)
    {
        if (prefix == null || prefix.isBlank())
        {
            return DEFAULT_INSTANCE;
        }
        else
        {
            return new CpDependsNotifyMapper(prefix);
        }
    }

    public enum Columns implements TableColumnDefinition
    {
        RECORD_NUM("record_num"),
        EVENT_TYPE("event_type"),
        KEY("key"),
        DATE_TIME_LOADED("date_time_loaded")
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
