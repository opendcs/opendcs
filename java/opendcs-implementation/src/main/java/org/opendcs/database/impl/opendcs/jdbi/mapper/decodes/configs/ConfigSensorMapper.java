package org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.configs;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.PrefixRowMapper;

import decodes.db.ConfigSensor;

public class ConfigSensorMapper extends PrefixRowMapper<ConfigSensor>
{

    protected ConfigSensorMapper(String prefix)
    {
        super(prefix);
    }

    @Override
    public ConfigSensor map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        final var num = rs.getInt(prefix + "sensornumber");
        if (rs.wasNull())
        {
            return null;
        }
        final var cs = new ConfigSensor(null, num);

        final var name = rs.getString(prefix + "sensorname");
        cs.sensorName = name;

        cs.recordingInterval = rs.getInt(prefix + "recordinginterval");
        cs.recordingMode = rs.getString(prefix + "recordingMode").charAt(0);

        final var absoluteMin = rs.getDouble(prefix + "absolutemin");
        if (!rs.wasNull())
        {
            cs.absoluteMin = absoluteMin;
        }

        final var absoluteMax = rs.getDouble(prefix + "absolutemax");
        if (!rs.wasNull())
        {
            cs.absoluteMax = absoluteMax;
        }

        return cs;
    }


    public static ConfigSensorMapper withPrefix(String prefix)
    {
        return new ConfigSensorMapper(prefix);
    }
}
