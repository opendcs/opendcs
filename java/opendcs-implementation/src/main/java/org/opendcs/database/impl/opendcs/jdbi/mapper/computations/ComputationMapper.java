package org.opendcs.database.impl.opendcs.jdbi.mapper.computations;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.OpenDcsTableMapper;
import org.opendcs.database.sql.TableColumnDefinition;
import org.opendcs.utils.sql.SqlErrorMessages;

import decodes.sql.DbKey;
import decodes.tsdb.DbComputation;

public class ComputationMapper extends OpenDcsTableMapper<DbComputation, ComputationMapper.Columns>
{
    private ComputationMapper(String prefix)
    {
        super(prefix, "cp_computation", Columns.class);
    }

    @Override
    public DbComputation map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        var keyMapper = ctx.findColumnMapperFor(DbKey.class)
                           .orElseThrow(() -> new SQLException(SqlErrorMessages.DBKEY_MAPPER_NOT_FOUND));
        var dateMapper = ctx.findColumnMapperFor(Date.class)
                            .orElseThrow(() -> new SQLException(SqlErrorMessages.TIME_MAPPER_NOT_FOUND));
        var key = keyMapper.map(rs, column(Columns.ID), ctx);
        if (key == null || rs.wasNull())
        {
            return null;
        }

        var ret = new DbComputation(key, rs.getString(column(Columns.COMPUTATION_NAME)));
        ret.setAlgorithmId(keyMapper.map(rs, column(Columns.ALGORITHM_ID), ctx));
        ret.setAppId(keyMapper.map(rs, column(Columns.LOADING_APPLICATION_ID), ctx));
        ret.setAlgorithmName(rs.getString(column(Columns.ALGORITHM_NAME)));
        ret.setApplicationName(rs.getString(column(Columns.APP_NAME)));
        ret.setEnabled(rs.getBoolean(column(Columns.ENABLED)));
        ret.setGroupId(keyMapper.map(rs, column(Columns.GROUP_ID), ctx));
        ret.setLastModified(dateMapper.map(rs, column(Columns.DATE_TIME_LOADED), ctx));
        ret.setValidStart(dateMapper.map(rs, column(Columns.EFFECTIVE_START), ctx));
        ret.setValidEnd(dateMapper.map(rs, column(Columns.EFFECTIVE_END), ctx));
        ret.setComment(rs.getString(column(Columns.COMMENT)));

        return ret;
    }

    public static ComputationMapper withPrefix(String prefix)
    {
        return new ComputationMapper(prefix);
    }
    
    public static enum Columns implements TableColumnDefinition
    {
        ID("computation_id"),
        COMPUTATION_NAME,
        ALGORITHM_ID,
        ALGORITHM_NAME(false),
        COMMENT("cmmnt"),
        LOADING_APPLICATION_ID,
        APP_NAME(false),
        DATE_TIME_LOADED,
        ENABLED,
        EFFECTIVE_START("effective_start_date_time"),
        EFFECTIVE_END("effective_start_date_time"),
        GROUP_ID
        ;

        private final String column;
        private boolean include;

        Columns()
        {
            this.column = this.name().toLowerCase();
            include = true;
        }

        Columns(boolean includeInSelect)
        {
            this.column = this.name().toLowerCase();
            include = includeInSelect;
        }

        Columns(String column)
        {
            this.column = column;
            include = true;
        }

        @Override
        public String column()
        {
            return this.column;
        }
        
        @Override
        public boolean includeInSelect()
        {
            return include;
        }
    }
}
