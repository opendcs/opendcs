package org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.platforms;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.database.sql.TableColumnDefinition;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlErrorMessages;

import decodes.db.Platform;
import decodes.sql.DbKey;

public class PlatformMapper extends PrefixRowMapper<Platform,PlatformMapper.Columns>
{

    protected PlatformMapper(String prefix)
    {
        super(prefix, "platform", Columns.class);
    }

    @Override
    public Platform map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        ColumnMapper<DbKey> columnMapperForKey = ctx.findColumnMapperFor(DbKey.class)
                                                    .orElseThrow(() -> new SQLException(SqlErrorMessages.DBKEY_MAPPER_NOT_FOUND));
        ColumnMapper<Date> dateMapper = ctx.findColumnMapperFor(Date.class)
                                .orElseThrow(() -> new SQLException(SqlErrorMessages.TIME_MAPPER_NOT_FOUND));        
        var platform = new Platform(columnMapperForKey.map(rs, column(Columns.ID), ctx));
        platform.setAgency(rs.getString(column(Columns.AGENCY)));
        platform.setDescription(rs.getString(column(Columns.DESCRIPTION)));
        platform.lastModifyTime = dateMapper.map(rs, column(Columns.LAST_MODIFY_TIME), ctx);
        platform.expiration = dateMapper.map(rs, column(Columns.EXPIRATION), ctx);
        platform.isProduction = rs.getBoolean(column(Columns.IS_PRODUCTION));
        platform.setPlatformDesignator(rs.getString(column(Columns.DESIGNATOR)));
        return platform;
    }

    public static PlatformMapper withPrefix(String prefix)
    {
        return new PlatformMapper(prefix);
    }
    
    public enum Columns implements TableColumnDefinition
    {
        ID(GenericColumns.ID),
        AGENCY("agency"),
        IS_PRODUCTION("isproduction"),
        SITE_ID("siteid"),
        CONFIG_ID("configid"),
        DESCRIPTION(GenericColumns.DESCRIPTION),
        LAST_MODIFY_TIME("lastmodifytime"),
        EXPIRATION("expiration"),
        DESIGNATOR("platformdesignator")
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
