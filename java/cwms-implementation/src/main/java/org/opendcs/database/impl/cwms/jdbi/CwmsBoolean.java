package org.opendcs.database.impl.cwms.jdbi;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;

import ilex.util.TextUtil;

/**
 * Maps CWMS 'T'/'F' column to Java Boolean
 */
public class CwmsBoolean implements ColumnMapper<Boolean>
{

    @Override
    public Boolean map(ResultSet rs, int columnNumber, StatementContext ctx) throws SQLException
    {
        boolean ret = false;
        final var dbValue = rs.getString(columnNumber);
        if (!rs.wasNull())
        {
            // str2boolean works with true/false and yes/no, only comparing the the first character.
            ret = TextUtil.str2boolean(dbValue);
        }

        return ret;
    }
}
