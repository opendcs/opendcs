package org.opendcs.database.impl.opendcs.jdbi.mapper.computations;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.OpenDcsTableMapper;
import org.opendcs.database.sql.TableColumnDefinition;
import org.opendcs.utils.sql.SqlErrorMessages;

import decodes.sql.DbKey;
import decodes.tsdb.DbCompAlgorithm;

public final class AlgorithmMapper extends OpenDcsTableMapper<DbCompAlgorithm, AlgorithmMapper.Columns>
{
    private AlgorithmMapper(String prefix)
    {
        super(prefix, "cp_algorithm", Columns.class);
    }

    @Override
    public DbCompAlgorithm map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        var keyMapper = ctx.findColumnMapperFor(DbKey.class)
                           .orElseThrow(() -> new SQLException(SqlErrorMessages.DBKEY_MAPPER_NOT_FOUND));
        var key = keyMapper.map(rs, column(Columns.ID), ctx);
        if (key == null || rs.wasNull())
        {
            return null;
        }
        var name = rs.getString(column(Columns.ALGORITHM_NAME));
        var comment = rs.getString(column(Columns.COMMENT));
        var execClass = rs.getString(column(Columns.EXEC_CLASS));

        var ret = new DbCompAlgorithm(name);
        ret.setId(key);
        ret.setComment(comment);
        ret.setExecClass(execClass);
        return ret;
    }

    public static AlgorithmMapper withPrefix(String prefix)
    {
        return new AlgorithmMapper(prefix);
    }

    public enum Columns implements TableColumnDefinition
    {
        ID("algorithm_id"),
        ALGORITHM_NAME,
        EXEC_CLASS,
        COMMENT("cmmnt")
        ;

        private final String column;

        Columns()
        {
            this.column = this.name().toLowerCase();
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
