package org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.platforms;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.database.model.platforms.PlatformSensorProperty;
import org.opendcs.database.sql.TableColumnDefinition;
import org.opendcs.utils.sql.SqlErrorMessages;

import decodes.sql.DbKey;

public class PlatformSensorPropertyMapper extends PrefixRowMapper<PlatformSensorProperty, PlatformSensorPropertyMapper.Columns>
{
    protected PlatformSensorPropertyMapper(String prefix)
    {
        super(prefix, "platformsensorproperty", Columns.class);
    }

    @Override
    public PlatformSensorProperty map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        ColumnMapper<DbKey> columnMapperForKey = ctx.findColumnMapperFor(DbKey.class)
                                                    .orElseThrow(() -> new SQLException(SqlErrorMessages.DBKEY_MAPPER_NOT_FOUND));
        var name = rs.getString(column(Columns.PROP_NAME));
        if (rs.wasNull())
        {
            return null;
        }
        var value = rs.getString(column(Columns.PROP_VALUE));
        var platformId = columnMapperForKey.map(rs, column(Columns.PLATFORM_ID), ctx);
        var sensorNumber = rs.getInt(column(Columns.SENSOR_NUMBER));
        return new PlatformSensorProperty(platformId, sensorNumber, name, value);
    }

    public static PlatformSensorPropertyMapper withPrefix(String prefix)
    {
        return new PlatformSensorPropertyMapper(prefix);
    }

    public enum Columns implements TableColumnDefinition
    {
        PLATFORM_ID("platformid"),
        SENSOR_NUMBER("sensornumber"),
        PROP_NAME("prop_name"),
        PROP_VALUE("prop_value")
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
<<<<<<< HEAD
}
=======
}
>>>>>>> 1ac112e3 (Initial setup to get platform sensors.)
