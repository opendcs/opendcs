package org.opendcs.database.impl.opendcs.jdbi.column.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Wrapper around ByteBuffer to writing to the database
 * ByteBufferOutputStream
 */
public class ByteBufferOutputStream extends InputStream
{
    private final ByteBuffer data;

    public ByteBufferOutputStream(ByteBuffer data)
    {
        this.data = data;
    }

    @Override
    public int read() throws IOException 
    {
        return data.hasRemaining() ? -1 : data.get() & 0xFF;
    }
    

    @Override
    public int read(byte[] b, int off, int len) throws IOException
    {
        if (!data.hasRemaining())
        {
            return -1;
        }
        int toRead = Math.min(len, data.remaining());
        data.get(b, off, toRead);
        return toRead;
    }
}
