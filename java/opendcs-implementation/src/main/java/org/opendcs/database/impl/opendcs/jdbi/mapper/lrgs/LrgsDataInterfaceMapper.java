package org.opendcs.database.impl.opendcs.jdbi.mapper.lrgs;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.Map;

import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.data.lrgs.LrgsDataInterface;
import org.opendcs.data.lrgs.LrgsDataInterfaceType;
import org.opendcs.database.DatabaseKey;
import org.opendcs.database.impl.opendcs.jdbi.column.json.ConfigColumnMapper;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.database.sql.TableColumnDefinition;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlErrorMessages;

import decodes.sql.DbKey;

public class LrgsDataInterfaceMapper extends PrefixRowMapper<LrgsDataInterface, LrgsDataInterfaceMapper.Columns>
{
    private LrgsDataInterfaceMapper(String prefix)
    {
        super(prefix, "lrgs_data_interface", Columns.class);
    }
        
    @Override
    public LrgsDataInterface map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        var keyMapper = ctx.findColumnMapperFor(DbKey.class)
                           .orElseThrow(() -> new SQLException(SqlErrorMessages.DBKEY_MAPPER_NOT_FOUND));
        var zdtMapper = ctx.findColumnMapperFor(ZonedDateTime.class)
                           .orElseThrow(() -> new SQLException(SqlErrorMessages.ZDT_MAPPER_NOT_FOUND));
        var configMapper = ctx.findColumnMapperFor(ConfigColumnMapper.CONFIG_TYPE)
                              .orElseThrow(() -> new SQLException(SqlErrorMessages.CONFIG_MAPPER_NOT_FOUND));
        var key = keyMapper.map(rs, column(Columns.ID), ctx);
        if (key == null || rs.wasNull())
        {
            return null;
        }
        var name = rs.getString(column(Columns.NAME));
        var config = (Map<String, Object>)configMapper.map(rs, column(Columns.CONFIG), ctx);
        var createdAt = zdtMapper.map(rs, column(Columns.CREATED_AT), ctx);
        var updatedAt = zdtMapper.map(rs, column(Columns.UPDATED_AT), ctx);
        var typeMapper = ctx.findRowMapperFor(LrgsDataInterfaceType.class);
        LrgsDataInterfaceType interfaceType = null;
        if (typeMapper.isPresent()) {
            var mapper = typeMapper.get();
            interfaceType = mapper.map(rs, ctx);
        }
        
        return new LrgsDataInterface(key, name, interfaceType, config, createdAt, updatedAt);

    }


    public static LrgsDataInterfaceMapper withPrefix(String prefix)
    {
        return new LrgsDataInterfaceMapper(prefix);
    }

    public enum Columns implements TableColumnDefinition
    {
        ID(GenericColumns.ID),
        NAME(GenericColumns.NAME),
        TYPE_ID("type_id"),
        CONFIG(GenericColumns.CONFIG),
        CREATED_AT(GenericColumns.CREATED_AT),
        UPDATED_AT(GenericColumns.UPDATED_AT)
        ;

        private final String column;

        Columns(GenericColumns other)
        {
            this.column = other.column();
        }

        Columns(String column)
        {
            this.column = column;
        }

        @Override
        public String column()
        {
            return this.column;
        }
        
    }

}
