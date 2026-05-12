package org.opendcs.database.impl.opendcs.jdbi.column.chrono;

import java.sql.ResultSet;
import java.sql.Types;
import java.sql.SQLException;
import java.util.Date;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;

public final class OpenDcsTimeColumn implements ColumnMapper<Date>
{

    @Override
    public Date map(ResultSet rs, int columnNumber, StatementContext ctx) throws SQLException
    {
        var meta = rs.getMetaData();
        int type = meta.getColumnType(columnNumber);
        Date ret = null;
        switch (type) 
        {
            case Types.DATE, Types.TIMESTAMP:
            {
                var tmp = rs.getTimestamp(columnNumber);
                if (!rs.wasNull())
                {
                    ret = new Date(tmp.getTime());
                }
                break;
            }
            case Types.NUMERIC, Types.BIGINT:
            {
                var tmp = rs.getLong(columnNumber);
                if (!rs.wasNull())
                {
                    ret = new Date(tmp);
                }
                break;
            }
            
            default: throw new SQLException("Unable to determine how to convert column " + meta.getColumnName(columnNumber) + " to a Date");
        }
        return ret;
    }
    
}
