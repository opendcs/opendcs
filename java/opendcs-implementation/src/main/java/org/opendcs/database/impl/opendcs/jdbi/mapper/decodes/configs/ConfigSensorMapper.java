package org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.configs;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.database.sql.TableColumnDefinition;

import decodes.db.ConfigSensor;

public class ConfigSensorMapper extends PrefixRowMapper<ConfigSensor,ConfigSensorMapper.Columns>
{
    protected ConfigSensorMapper(String prefix)
    {
        super(prefix, Columns.class);
    }

    @Override
    public ConfigSensor map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        final var num = rs.getInt(column(Columns.SENSOR_NUMBER));
        if (rs.wasNull())
        {
            return null;
        }
        final var cs = new ConfigSensor(null, num);

        final var name = rs.getString(column(Columns.SENSOR_NAME));
        cs.sensorName = name;

        cs.recordingInterval = rs.getInt(column(Columns.RECORDING_INTERVAL));
        cs.recordingMode = rs.getString(column(Columns.RECORDING_MODE)).charAt(0);

        final var absoluteMin = rs.getDouble(column(Columns.ABSOLUTE_MIN));
        if (!rs.wasNull())
        {
            cs.absoluteMin = absoluteMin;
        }

        cs.timeOfFirstSample = rs.getInt(column(Columns.TIME_OF_FIRST_SAMPLE));

        final var absoluteMax = rs.getDouble(column(Columns.ABSOLUTE_MAX));
        if (!rs.wasNull())
        {
            cs.absoluteMax = absoluteMax;
        }

        cs.setUsgsStatCode(rs.getString(column(Columns.STATISTICS_CODE)));

        return cs;
    }


    public static ConfigSensorMapper withPrefix(String prefix)
    {
        return new ConfigSensorMapper(prefix);
    }

    public enum Columns implements TableColumnDefinition
    {
        CONFIG_ID("configid"),
        SENSOR_NUMBER("sensornumber"),
        SENSOR_NAME("sensorname"),
        RECORDING_INTERVAL("recordinginterval"),
        RECORDING_MODE("recordingMode"),
        ABSOLUTE_MIN("absolutemin"),
        TIME_OF_FIRST_SAMPLE("timeoffirstsample"),
        ABSOLUTE_MAX("absolutemax"),
        STATISTICS_CODE("stat_cd")

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
