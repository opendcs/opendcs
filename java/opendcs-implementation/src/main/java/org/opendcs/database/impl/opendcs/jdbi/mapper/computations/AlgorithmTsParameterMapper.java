package org.opendcs.database.impl.opendcs.jdbi.mapper.computations;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.OpenDcsTableMapper;
import org.opendcs.database.sql.TableColumnDefinition;
import org.opendcs.utils.sql.SqlErrorMessages;

import decodes.sql.DbKey;
import decodes.tsdb.DbAlgoParm;

public class AlgorithmTsParameterMapper extends OpenDcsTableMapper<DbAlgoParm, AlgorithmTsParameterMapper.Columns>
{
    private AlgorithmTsParameterMapper(String prefix)
    {
        super(prefix, "cp_algo_ts_parm", Columns.class);
    }

    @Override
    public DbAlgoParm map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        var keyMapper = ctx.findColumnMapperFor(DbKey.class)
                           .orElseThrow(() -> new SQLException(SqlErrorMessages.DBKEY_MAPPER_NOT_FOUND));
        var algoKey = keyMapper.map(rs, column(Columns.ALGORITHM_ID), ctx);
        if (algoKey == null || rs.wasNull())
        {
            return null;
        }
        return new DbAlgoParm(
            rs.getString(column(Columns.ALGO_ROLE_NAME)),
            rs.getString(column(Columns.PARM_TYPE))
        );
    }

    public static AlgorithmTsParameterMapper withPrefix(String prefix)
    {
        return new AlgorithmTsParameterMapper(prefix);
    }

    public enum Columns implements TableColumnDefinition
    {
        ALGORITHM_ID,
        ALGO_ROLE_NAME,
        PARM_TYPE
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
