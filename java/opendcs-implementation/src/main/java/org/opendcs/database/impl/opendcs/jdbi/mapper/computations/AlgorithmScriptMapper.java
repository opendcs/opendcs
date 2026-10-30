package org.opendcs.database.impl.opendcs.jdbi.mapper.computations;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.data.computations.algorithms.AlgorithmScriptPart;
import org.opendcs.database.impl.opendcs.jdbi.column.text.Base64String;
import org.opendcs.database.model.mappers.OpenDcsTableMapper;
import org.opendcs.database.sql.TableColumnDefinition;
import org.opendcs.utils.sql.SqlErrorMessages;

import decodes.sql.DbKey;
import decodes.tsdb.ScriptType;

public class AlgorithmScriptMapper extends OpenDcsTableMapper<AlgorithmScriptPart, AlgorithmScriptMapper.Columns>
{
    private AlgorithmScriptMapper(String prefix)
    {
        super(prefix, "cp_algo_script", Columns.class);
    }

    @Override
    public AlgorithmScriptPart map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        var keyMapper = ctx.findColumnMapperFor(DbKey.class)
                           .orElseThrow(() -> new SQLException(SqlErrorMessages.DBKEY_MAPPER_NOT_FOUND));
        var scriptMapper = ctx.findColumnMapperFor(Base64String.class)
                           .orElseThrow(() -> new SQLException(SqlErrorMessages.BASE64STRING_MAPPER_NOT_FOUND));
        var key = keyMapper.map(rs, column(Columns.ALGORITHM_ID), ctx);
        if (key == null || rs.wasNull())
        {
            return null;
        }

        return new AlgorithmScriptPart(
            key,
            ScriptType.fromDbChar(rs.getString(column(Columns.SCRIPT_TYPE)).charAt(0)),
            rs.getInt(column(Columns.BLOCK_NUM)),
            scriptMapper.map(rs, column(Columns.SCRIPT_DATA), ctx).value()
        );

    }

    public static AlgorithmScriptMapper withPrefix(String prefix)
    {
        return new AlgorithmScriptMapper(prefix);
    }

    public enum Columns implements TableColumnDefinition
    {
        ALGORITHM_ID,
        SCRIPT_TYPE,
        BLOCK_NUM,
        SCRIPT_DATA
        ;

        private final String column;

        Columns()
        {
            this.column = this.name().toLowerCase();
        }

        @Override
        public String column()
        {
            return this.column;
        }
    }
}
