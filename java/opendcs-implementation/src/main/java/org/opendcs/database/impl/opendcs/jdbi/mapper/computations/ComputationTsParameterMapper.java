package org.opendcs.database.impl.opendcs.jdbi.mapper.computations;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.OpenDcsTableMapper;
import org.opendcs.database.sql.TableColumnDefinition;
import org.opendcs.utils.sql.SqlErrorMessages;

import decodes.sql.DbKey;
import decodes.tsdb.DbCompParm;

/**
 * Mapper for the DbCompParm (cp_comp_ts_parm) table.
 * 
 * NOTE: <b>this</b> is likely the correct location for future hooks and implementation
 * for improved variation. Not by puttingit here, but by creating entirely new implementations
 * that return the correct derived type, if any.
 * ComputationTsParameterMapper
 */
public class ComputationTsParameterMapper extends OpenDcsTableMapper<DbCompParm, ComputationTsParameterMapper.Columns>
{
    private ComputationTsParameterMapper(String prefix)
    {
        super(prefix, "cp_comp_ts_parm", Columns.class);
    }
    
    @Override
    public DbCompParm map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        var keyMapper = ctx.findColumnMapperFor(DbKey.class)
                           .orElseThrow(() -> new SQLException(SqlErrorMessages.DBKEY_MAPPER_NOT_FOUND));
       
        var key = keyMapper.map(rs, column(Columns.COMPUTATION_ID), ctx);
        if (key == null || rs.wasNull())
        {
            return null;
        }
        
        var ret = new DbCompParm(
            rs.getString(column(Columns.ALGO_ROLE_NAME)),
            keyMapper.map(rs, column(Columns.SITE_DATATYPE_ID), ctx),
            rs.getString(column(Columns.INTERVAL_ABBR)),
            rs.getString(column(Columns.TABLE_SELECTOR)),
            rs.getInt(column(Columns.DELTA_T))
        );

        ret.setDeltaTUnits(rs.getString(column(Columns.DELTA_T_UNITS)));
        ret.setModelId(rs.getInt(column(Columns.MODEL_ID)));
        ret.setDataTypeId(keyMapper.map(rs, column(Columns.DATATYPE_ID), ctx));
        ret.setSiteId(keyMapper.map(rs, column(Columns.SITE_ID), ctx));
        /** should we decide this always pull things in or join elements? */
        return ret;
    }

    public static enum Columns implements TableColumnDefinition
    {
        COMPUTATION_ID,
        ALGO_ROLE_NAME,
        SITE_DATATYPE_ID,
        INTERVAL_ABBR,
        TABLE_SELECTOR,
        DELTA_T,
        MODEL_ID,
        DATATYPE_ID,
        DELTA_T_UNITS,
        SITE_ID
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
