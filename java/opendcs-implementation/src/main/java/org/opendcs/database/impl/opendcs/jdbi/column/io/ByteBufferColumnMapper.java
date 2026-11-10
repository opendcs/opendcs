package org.opendcs.database.impl.opendcs.jdbi.column.io;

import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;

/**
 * Handle Mapping java.nio.ByteBuffer arrays from the database
 * ByteBufferColumnMapper
 */
public class ByteBufferColumnMapper implements ColumnMapper<ByteBuffer>
{

    @Override
    public ByteBuffer map(ResultSet rs, int columnNumber, StatementContext ctx) throws SQLException
    {
        var bytes = rs.getBytes(columnNumber);
        if (bytes == null || rs.wasNull())
        {
            return null;
        }
        return ByteBuffer.wrap(bytes);
    }
}
