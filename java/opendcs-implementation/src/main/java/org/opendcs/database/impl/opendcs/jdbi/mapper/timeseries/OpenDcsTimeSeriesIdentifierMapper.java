package org.opendcs.database.impl.opendcs.jdbi.mapper.timeseries;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlErrorMessages;

import decodes.cwms.CwmsTsId;
import decodes.sql.DbKey;
import decodes.tsdb.TimeSeriesIdentifier;
import opendcs.opentsdb.OffsetErrorAction;

public class OpenDcsTimeSeriesIdentifierMapper extends PrefixRowMapper<TimeSeriesIdentifier>
{

    protected OpenDcsTimeSeriesIdentifierMapper(String prefix)
    {
        super(prefix);
    }

    @Override
    public TimeSeriesIdentifier map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        ColumnMapper<DbKey> columnMapperForKey = ctx.findColumnMapperFor(DbKey.class)
                                                    .orElseThrow(() -> new SQLException(SqlErrorMessages.DBKEY_MAPPER_NOT_FOUND));
        ColumnMapper<Date> dateMapper = ctx.findColumnMapperFor(Date.class)
                                .orElseThrow(() -> new SQLException(SqlErrorMessages.TIME_MAPPER_NOT_FOUND));
        var ret = new CwmsTsId();
        ret.setKey(columnMapperForKey.map(rs, prefix + GenericColumns.ID, ctx));
        ret.setUniqueString(rs.getString(prefix + "unique_string"));
        ret.setSiteName(rs.getString(prefix + "site_name"));
        ret.setDescription(rs.getString(prefix + GenericColumns.DESCRIPTION));
        ret.setActive(rs.getBoolean(prefix + "active_flag"));
        ret.setStorageTable(rs.getInt(prefix + "storage_table"));
        ret.setStorageType(rs.getString(prefix + "storage_type").charAt(0));
        ret.setStorageUnits(rs.getString(prefix + "storage_units"));
        ret.setInterval(rs.getString(prefix + "interval"));
        ret.setDuration(rs.getString(prefix + "duration"));
        ret.setLastModified(dateMapper.map(rs, prefix + "modify_time", ctx));
        ret.setUtcOffset(rs.getInt(prefix + "utc_offset"));
        ret.setAllowDstOffsetVariation(rs.getBoolean(prefix + "allow_dst_offset_variation"));
        ret.setOffsetErrorAction(OffsetErrorAction.fromString(rs.getString(prefix + "offset_error_action")));

        return ret;
    }
    

    public static OpenDcsTimeSeriesIdentifierMapper withPrefix(String prefix)
    {
        return new OpenDcsTimeSeriesIdentifierMapper(prefix);
    }
}
