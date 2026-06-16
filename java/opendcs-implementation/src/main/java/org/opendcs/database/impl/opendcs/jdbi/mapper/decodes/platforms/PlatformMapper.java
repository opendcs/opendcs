package org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.platforms;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.database.sql.TableColumnDefinition;
import org.opendcs.utils.sql.GenericColumns;

import decodes.db.Platform;

public class PlatformMapper extends PrefixRowMapper<Platform,PlatformMapper.Columns>
{

    protected PlatformMapper(String prefix)
    {
        super(prefix, "platform", Columns.class);
    }

    @Override
    public Platform map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        return null;
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
