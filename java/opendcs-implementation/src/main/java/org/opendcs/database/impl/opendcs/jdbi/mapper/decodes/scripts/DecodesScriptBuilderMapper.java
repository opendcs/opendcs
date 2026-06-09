package org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.scripts;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.impl.opendcs.jdbi.decodesscript.InMemoryDecodesScriptReader;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.database.sql.TableColumnDefinition;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlErrorMessages;

import decodes.db.DecodesScript.DecodesScriptBuilder;
import decodes.sql.DbKey;

public class DecodesScriptBuilderMapper extends PrefixRowMapper<DecodesScriptBuilder, org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.scripts.DecodesScriptBuilderMapper.Columns>
{

    protected DecodesScriptBuilderMapper(String prefix)
    {
        super(prefix, Columns.class);
    }

    @Override
    public DecodesScriptBuilder map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        ColumnMapper<DbKey> columnMapperForKey = ctx.findColumnMapperFor(DbKey.class)
                                                    .orElseThrow(() -> new SQLException(SqlErrorMessages.DBKEY_MAPPER_NOT_FOUND));
        var builder = new DecodesScriptBuilder(new InMemoryDecodesScriptReader());
        final var id = columnMapperForKey.map(rs, column(Columns.ID), ctx);

        if (rs.wasNull() || DbKey.isNull(id))
        {
            return null;
        }
        final var name = rs.getString(column(Columns.NAME));
        final var scriptType = rs.getString(column(Columns.SCRIPT_TYPE));
        final var dataOrder = rs.getString(column(Columns.DATA_ORDER));

        builder.withId(id);
        builder.scriptName(name);
        builder.scriptType(scriptType);
        builder.withDataOrder(dataOrder.charAt(0));

        return builder;

    }

    public static DecodesScriptBuilderMapper withPrefix(String prefix)
    {
        return new DecodesScriptBuilderMapper(prefix);
    }

    public static enum Columns implements TableColumnDefinition
    {
        ID(GenericColumns.ID),
        NAME(GenericColumns.NAME),
        SCRIPT_TYPE("script_type"),
        DATA_ORDER("dataorder")
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
