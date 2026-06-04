package org.opendcs.database.impl.opendcs.jdbi.mapper.timeseries;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlErrorMessages;

import decodes.sql.DbKey;
import decodes.tsdb.IntervalCodes;
import opendcs.opentsdb.Interval;

public class IntervalMapper extends PrefixRowMapper<Interval> {

    protected IntervalMapper(String prefix)
    {
        super(prefix);
    }

    @Override
    public Interval map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        final var ret = new Interval(rs.getString(prefix + GenericColumns.NAME));
        ColumnMapper<DbKey> columnMapperForKey = ctx.findColumnMapperFor(DbKey.class)
                                                    .orElseThrow(() -> new SQLException(SqlErrorMessages.DBKEY_MAPPER_NOT_FOUND));
        ret.setKey(columnMapperForKey.map(rs, prefix + "interval_id", ctx));
        final var calConstant = rs.getString(prefix + "cal_constant");
        ret.setCalConstant(IntervalCodes.getCalConstant(calConstant));

        ret.setCalMultiplier(rs.getInt(prefix + "cal_multiplier"));
        return ret;
    }
    

    public static IntervalMapper withPrefix(String prefix)
    {
        return new IntervalMapper(prefix);
    }
}
