package org.opendcs.database.impl.opendcs.jdbi.column.text;

import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;

import ilex.util.Base64;

/**
 * Handles Decoding of any column that uses {@link ilex.util.Base64} encoded strings.
 * Base64ColumnMapper
 */
public class Base64ColumnMapper implements ColumnMapper<Base64String>
{

    @Override
    public Base64String map(ResultSet rs, int columnNumber, StatementContext ctx) throws SQLException
    {
        var str = rs.getString(columnNumber);
        if (str == null || rs.wasNull())
        {
            return null;
        }

        return new Base64String(Base64.decodeBase64(str.getBytes(StandardCharsets.UTF_8)));
    }

}
