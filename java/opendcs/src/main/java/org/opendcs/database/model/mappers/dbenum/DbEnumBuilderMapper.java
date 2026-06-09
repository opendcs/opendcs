package org.opendcs.database.model.mappers.dbenum;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.database.sql.TableColumnDefinition;
import org.opendcs.utils.sql.GenericColumns;

import decodes.db.DbEnum.DbEnumBuilder;
import decodes.sql.DbKey;

public final class DbEnumBuilderMapper extends PrefixRowMapper<DbEnumBuilder,DbEnumBuilderMapper.Columns>
{
    private DbEnumBuilderMapper(String prefix)
    {
        super(prefix, Columns.class);
    }

    public static DbEnumBuilderMapper withPrefix(String prefix)
    {
        return new DbEnumBuilderMapper(prefix);
    }

    @Override
    public DbEnumBuilder map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        ColumnMapper<DbKey> dbKeyMapper = ctx.findColumnMapperFor(DbKey.class)
                                             .orElseThrow(() -> new SQLException("No mapper registered for DbKey class."));
        final DbKey id = dbKeyMapper.map(rs, prefix + GenericColumns.ID, ctx);
        final String name = rs.getString(prefix + GenericColumns.NAME);
        final String defaultValue = rs.getString(prefix + "defaultValue");
        final String description = rs.getString(prefix + GenericColumns.DESCRIPTION);
        return new DbEnumBuilder(id, name, defaultValue, description);
    }

    public enum Columns implements TableColumnDefinition
    {
        ID(GenericColumns.ID),
        NAME(GenericColumns.NAME),
        DEFAULT_VALUE("defaultValue"),
        DESCRIPTION(GenericColumns.DESCRIPTION)
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
            return column;
        }
    }
}
