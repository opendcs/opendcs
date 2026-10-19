package org.opendcs.database.impl.opendcs.jdbi.mapper.lrgs;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;

import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.data.lrgs.LrgsDataInterfaceType;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.database.sql.TableColumnDefinition;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlErrorMessages;

import decodes.sql.DbKey;

public class LrgsDataInterfaceTypeMapper extends PrefixRowMapper<LrgsDataInterfaceType, LrgsDataInterfaceTypeMapper.Columns>
{
    private LrgsDataInterfaceTypeMapper(String prefix)
    {
        super(prefix, "lrgs_data_interface_type", Columns.class);
    }
        
    @Override
    public LrgsDataInterfaceType map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        var keyMapper = ctx.findColumnMapperFor(DbKey.class)
                           .orElseThrow(() -> new SQLException(SqlErrorMessages.DBKEY_MAPPER_NOT_FOUND));
        var zdtMapper = ctx.findColumnMapperFor(ZonedDateTime.class)
                           .orElseThrow(() -> new SQLException(SqlErrorMessages.ZDT_MAPPER_NOT_FOUND));
        var key = keyMapper.map(rs, column(Columns.ID), ctx);
        if (key == null || rs.wasNull())
        {
            return null;
        }

        var name = rs.getString(column(Columns.NAME));
        var provider = rs.getString(column(Columns.SERVICE_PROVIDER));
        var createdAt = zdtMapper.map(rs, column(Columns.CREATED_AT), ctx);
        var updatedAt = zdtMapper.map(rs, column(Columns.UPDATED_AT), ctx);

        return new LrgsDataInterfaceType(key, name, provider, createdAt, updatedAt);

    }


    public static LrgsDataInterfaceTypeMapper withPrefix(String prefix)
    {
        return new LrgsDataInterfaceTypeMapper(prefix);
    }

    public enum Columns implements TableColumnDefinition
    {
        ID(GenericColumns.ID),
        NAME(GenericColumns.NAME),
        SERVICE_PROVIDER("service_provider"),
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
