package org.opendcs.spi.url.classpath;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * Support for retrieving files on the classpath using a URL.
 */
public class Handler extends URLStreamHandler
{
    @Override
    protected URLConnection openConnection(URL resource) throws IOException
    {
        return new URLConnection(resource) 
        {            
            @Override
            public InputStream getInputStream() throws IOException
            {
                String path = url.getPath();
                URL cpUrl = this.getClass().getResource(path);
                if (cpUrl == null)
                {
                    throw new IOException("Cannot file: '" + path + "' on classpath.");
                }
                return cpUrl.openStream();
            }

            @Override
            public void connect() throws IOException 
            {
            }
        };
    }
}
