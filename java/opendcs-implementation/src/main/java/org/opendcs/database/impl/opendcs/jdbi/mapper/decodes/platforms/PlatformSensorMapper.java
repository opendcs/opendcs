package org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.platforms;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.database.sql.TableColumnDefinition;

import decodes.db.PlatformSensor;

public class PlatformSensorMapper extends PrefixRowMapper<PlatformSensor, PlatformSensorMapper.Columns>
{
    protected PlatformSensorMapper(String prefix)
    {
        super(prefix, "platformsensor", Columns.class);
    }

    @Override
    public PlatformSensor map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        var num = rs.getInt(column(Columns.SENSOR_NUMBER));
        var platformSensor = new PlatformSensor(null, num);
        var ddNo = rs.getInt(column(Columns.DD_NU));
        if (!rs.wasNull())
        {
            platformSensor.setUsgsDdno(ddNo);
        }
        return platformSensor;
    }

    public static PlatformSensorMapper withPrefix(String prefix)
    {
        return new PlatformSensorMapper(prefix);
    }

    public enum Columns implements TableColumnDefinition
    {
        PLATFORM_ID("platformid"),
        SITE_ID("siteid"),
        SENSOR_NUMBER("sensornumber"),
        DD_NU("dd_nu")
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
