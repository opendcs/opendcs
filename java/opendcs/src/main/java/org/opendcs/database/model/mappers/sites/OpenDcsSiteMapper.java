package org.opendcs.database.model.mappers.sites;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.impl.opendcs.jdbi.column.numeric.NullableDouble;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.utils.sql.SqlErrorMessages;

import decodes.db.Site;
import decodes.sql.DbKey;

public class OpenDcsSiteMapper extends PrefixRowMapper<Site>
{


    
    protected OpenDcsSiteMapper(String prefix)
    {
        super(prefix);
    }

    public static OpenDcsSiteMapper withPrefix(String prefix)
    {
        return new OpenDcsSiteMapper(prefix);
    }

    @Override
    public Site map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        ColumnMapper<DbKey> columnMapperForKey = ctx.findColumnMapperFor(DbKey.class)
                                                    .orElseThrow(() -> new SQLException(SqlErrorMessages.DBKEY_MAPPER_NOT_FOUND));
        ColumnMapper<Double> doubleMapper = new NullableDouble();
        ColumnMapper<Date> dateMapper = ctx.findColumnMapperFor(Date.class)
                                .orElseThrow(() -> new SQLException(SqlErrorMessages.TIME_MAPPER_NOT_FOUND));
        var ret = new Site();

        var id = columnMapperForKey.map(rs, prefix + "id", ctx);
        ret.forceSetId(id);
        
        ret.setPublicName(rs.getString(prefix + "public_name"));        
        ret.setDescription(rs.getString(prefix + "description"));

        ret.latitude = rs.getString(prefix + "latitude");
        ret.longitude = rs.getString(prefix + "longitude");

        ret.setElevation(doubleMapper.map(rs, prefix + "elevation", ctx));
        ret.setElevationUnits(rs.getString(prefix + "elevunitabbr"));

        ret.nearestCity = rs.getString(prefix + "nearestcity");
        ret.state = rs.getString(prefix + "state");
        ret.region = rs.getString(prefix + "region");
        ret.timeZoneAbbr = rs.getString(prefix + "timezone");
        ret.country = rs.getString(prefix + "country");
        ret.setActive(rs.getBoolean(prefix + "active_flag"));
        ret.setLocationType(rs.getString(prefix + "location_type"));
        ret.setLastModifyTime(dateMapper.map(rs, prefix + "modify_time", ctx));


        return ret;
    }
    
}
