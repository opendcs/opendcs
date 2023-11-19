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
            String path = url.getPath();
            URL resourceURL = this.getClass().getResource(path);
            @Override
            public InputStream getInputStream() throws IOException
            {
                if (resourceURL == null)
                {
                    throw new IOException("Cannot file: '" + path + "' on classpath.");
                }
                return resourceURL.openStream();
            }

            @Override
            public void connect() throws IOException
            {
            }

            @Override
            public URL getURL()
            {
                return resourceURL;
            }
        };
    }
}
