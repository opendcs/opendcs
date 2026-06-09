package org.opendcs.database.model.mappers.sites;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.impl.opendcs.jdbi.column.numeric.NullableDouble;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.database.sql.TableColumnDefinition;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlErrorMessages;

import decodes.db.Site;
import decodes.sql.DbKey;

@SuppressWarnings("java:S2143") // May happen in future work.
public class OpenDcsSiteMapper extends PrefixRowMapper<Site,OpenDcsSiteMapper.Columns>
{
    protected OpenDcsSiteMapper(String prefix)
    {
        super(prefix, Columns.class);
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

        var id = columnMapperForKey.map(rs, column(Columns.ID), ctx);
        ret.forceSetId(id);

        ret.setPublicName(rs.getString(column(Columns.PUBLIC_NAME)));
        ret.setDescription(rs.getString(column(Columns.DESCRIPTION)));

        ret.latitude = rs.getString(column(Columns.LATITUDE));
        ret.longitude = rs.getString(column(Columns.LONGITUDE));

        ret.setElevation(doubleMapper.map(rs, column(Columns.ELEVATION), ctx));
        ret.setElevationUnits(rs.getString(column(Columns.ELEVATION_UNITS)));

        ret.nearestCity = rs.getString(column(Columns.NEAREST_CITY));
        ret.state = rs.getString(column(Columns.STATE));
        ret.region = rs.getString(column(Columns.REGION));
        ret.timeZoneAbbr = rs.getString(column(Columns.TIMEZONE));
        ret.country = rs.getString(column(Columns.COUNTRY));
        ret.setActive(rs.getBoolean(column(Columns.ACTIVE_FLAG)));
        ret.setLocationType(rs.getString(column(Columns.LOCATION_TYPE)));
        ret.setLastModifyTime(dateMapper.map(rs, column(Columns.MODIFY_TIME), ctx));


        return ret;
    }

    public enum Columns implements TableColumnDefinition
    {
        ID(GenericColumns.ID),
        PUBLIC_NAME("public_name"),
        DESCRIPTION(GenericColumns.DESCRIPTION),
        LATITUDE("latitude"),
        LONGITUDE("longitude"),
        ELEVATION("elevation"),
        ELEVATION_UNITS("elevunitabbr"),
        NEAREST_CITY("nearestcity"),
        STATE("state"),
        REGION("region"),
        TIMEZONE("timezone"),
        COUNTRY("country"),
        ACTIVE_FLAG("active_flag"),
        LOCATION_TYPE("location_type"),
        MODIFY_TIME("modify_time")
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
    }

}
