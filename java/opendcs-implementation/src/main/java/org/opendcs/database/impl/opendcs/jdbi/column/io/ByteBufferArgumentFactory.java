package org.opendcs.database.impl.opendcs.jdbi.column.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.statement.StatementContext;

public class ByteBufferArgumentFactory extends AbstractArgumentFactory<ByteBuffer>
{

    public ByteBufferArgumentFactory()
    {
        super(Types.BINARY);
    }

    @Override
    protected Argument build(ByteBuffer value, ConfigRegistry config)
    {
        return (position, statement, ctx) ->
        {
            if (value == null)
            {
                statement.setNull(position, Types.BINARY);
            }
            else
            {
                if (value.hasArray())
                {
                    mapByArray(value, position, statement, ctx);
                }
                else
                {
                    mapByStream(value, position, statement, ctx);
                }
            }
        };
    }

    private void mapByStream(ByteBuffer value, int position, PreparedStatement statement, StatementContext ctx) throws SQLException
    {
        try (var stream = new ByteBufferOutputStream(value))
        {
            statement.setBinaryStream(position, stream, value.remaining());
        }
        catch (IOException ex)
        {
            throw new SQLException("Unable to process ByteBuffer input value.", ex);
        }
    }

    private void mapByArray(ByteBuffer value, int position, PreparedStatement statement, StatementContext ctx) throws SQLException
    {
        statement.setBytes(position, value.array());
    }
    
}
