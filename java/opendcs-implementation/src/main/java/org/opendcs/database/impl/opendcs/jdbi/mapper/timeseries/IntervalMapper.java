package org.opendcs.database.impl.opendcs.jdbi.mapper.timeseries;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.database.sql.TableColumnDefinition;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlErrorMessages;

import decodes.sql.DbKey;
import decodes.tsdb.IntervalCodes;
import opendcs.opentsdb.Interval;

public class IntervalMapper extends PrefixRowMapper<Interval, org.opendcs.database.impl.opendcs.jdbi.mapper.timeseries.IntervalMapper.Columns>
 {

    protected IntervalMapper(String prefix)
    {
        super(prefix, Columns.class);
    }

    @Override
    public Interval map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        final var ret = new Interval(rs.getString(column(Columns.NAME)));
        ColumnMapper<DbKey> columnMapperForKey = ctx.findColumnMapperFor(DbKey.class)
                                                    .orElseThrow(() -> new SQLException(SqlErrorMessages.DBKEY_MAPPER_NOT_FOUND));
        ret.setKey(columnMapperForKey.map(rs, column(Columns.ID), ctx));
        final var calConstant = rs.getString(column(Columns.CALENDAR_CONSTANT));
        ret.setCalConstant(IntervalCodes.getCalConstant(calConstant));

        ret.setCalMultiplier(rs.getInt(column(Columns.CALENDAR_CONSTANT)));
        return ret;
    }
    

    public static IntervalMapper withPrefix(String prefix)
    {
        return new IntervalMapper(prefix);
    }

    public static enum Columns implements TableColumnDefinition
    {
        NAME(GenericColumns.NAME),
        ID("interval_id"),
        CALENDAR_CONSTANT("cal_constant"),
        CALENDAR_MULTIPLIER("cal_multiplier")
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