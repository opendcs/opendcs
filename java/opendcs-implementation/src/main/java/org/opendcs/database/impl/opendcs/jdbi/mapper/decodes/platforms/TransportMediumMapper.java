package org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.platforms;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.database.sql.TableColumnDefinition;

import decodes.db.TransportMedium;

public class TransportMediumMapper extends PrefixRowMapper<TransportMedium,TransportMediumMapper.Columns>
{
    protected TransportMediumMapper(String prefix)
    {
        super(prefix, "transportmedium", Columns.class);
    }

    @Override
    public TransportMedium map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        TransportMedium tm = null;
        var mediumType = rs.getString(column(Columns.MEDIUM_TYPE));
        if (!rs.wasNull())
        {
            tm = new TransportMedium(null);
            tm.setMediumType(mediumType);
            tm.setMediumId(rs.getString(column(Columns.MEDIUM_ID)));
            tm.scriptName = rs.getString(column(Columns.SCRIPT_NAME));
            tm.setTimeZone(rs.getString(column(Columns.TIME_ZONE)));            
        }
        return tm;
    }

    public static TransportMediumMapper withPrefix(String prefix)
    {
        return new TransportMediumMapper(prefix);
    }

    public enum Columns implements TableColumnDefinition
    {
        PLATFORM_ID("platformid"),
        MEDIUM_TYPE("mediumtype"),
        MEDIUM_ID("mediumid"),
        SCRIPT_NAME("scriptname"),
        CHANNEL_NUMBER("channelnum"),
        ASSIGNED_TIME("assignedtime"),
        TRANSMIT_WINDOW("transmitwindow"),
        TRANSMIT_INTERVAL("transmitinterval"),
        EQUIPMENT_ID("equipmentid"),
        TIME_ADJUSTMENT("timeadjustment"),
        PREAMPLE("preamble"),
        TIME_ZONE("timezone"),
        LOGGER_TYPE("loggertype"),
        BAUD("baud"),
        STOP_BITS("stopbits"),
        PARITY("parity"),
        DATABITS("databits"),
        DO_LOGIN("dologin"),
        USERNAME("username"),
        PASSWORD("password")
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
