package org.opendcs.database.impl.cwms.jdbi;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class CwmsBoolean implements ColumnMapper<Boolean> {

    @Override
    public Boolean map(ResultSet rs, int columnNumber, StatementContext ctx) throws SQLException
    {
        boolean ret = false;
        final var dbValue = rs.getString(columnNumber);
        if (!rs.wasNull())
        {
            ret = Boolean.parseBoolean(dbValue.toLowerCase());
        }

        return ret;
    }
    
}
