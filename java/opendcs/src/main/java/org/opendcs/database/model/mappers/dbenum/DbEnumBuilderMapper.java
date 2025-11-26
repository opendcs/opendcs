package org.opendcs.database.model.mappers.dbenum;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.utils.sql.GenericColumns;

import decodes.db.DbEnum.DbEnumBuilder;
import decodes.sql.DbKey;

public class DbEnumBuilderMapper extends PrefixRowMapper<DbEnumBuilder>
{

    private DbEnumBuilderMapper(String prefix) {
        super(prefix);
    }

    public static DbEnumBuilderMapper withPrefix(String prefix)
    {
        return new DbEnumBuilderMapper(prefix);
    }

    @Override
    public DbEnumBuilder map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        ColumnMapper<DbKey> dbKeyMapper = ctx.findColumnMapperFor(DbKey.class).get();
        final DbKey id = dbKeyMapper.map(rs, prefix + GenericColumns.ID, ctx);
        final String name = rs.getString(prefix + GenericColumns.NAME);
        final String defaultValue = rs.getString(prefix + "defaultValue");
        final String description = rs.getString(prefix + GenericColumns.DESCRIPTION);
        return new DbEnumBuilder(id, name, defaultValue, description);
    }
}
