package org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.scripts;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.impl.opendcs.jdbi.decodesscript.InMemoryDecodesScriptReader;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlErrorMessages;

import decodes.db.DecodesScript.DecodesScriptBuilder;
import decodes.sql.DbKey;

public class DecodesScriptBuilderMapper extends PrefixRowMapper<DecodesScriptBuilder>
 {

    protected DecodesScriptBuilderMapper(String prefix)
    {
        super(prefix);
    }

    @Override
    public DecodesScriptBuilder map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        ColumnMapper<DbKey> columnMapperForKey = ctx.findColumnMapperFor(DbKey.class)
                                                    .orElseThrow(() -> new SQLException(SqlErrorMessages.DBKEY_MAPPER_NOT_FOUND));
        var builder = new DecodesScriptBuilder(new InMemoryDecodesScriptReader());
        final var id = columnMapperForKey.map(rs, prefix + GenericColumns.ID, ctx);
        final var name = rs.getString(prefix + GenericColumns.NAME);
        final var scriptType = rs.getString(prefix + "script_type");
        final var dataOrder = rs.getString(prefix + "dataorder");

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
}
