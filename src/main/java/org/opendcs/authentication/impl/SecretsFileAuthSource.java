package org.opendcs.authentication.impl;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.opendcs.spi.authentication.AuthSource;

import ilex.util.AuthException;
import ilex.util.FileUtil;

/**
 * Pull credentials from a set of files.
 *
 */
public class SecretsFileAuthSource implements AuthSource
{
    private Properties credentials = new Properties();

    public SecretsFileAuthSource(String configString) throws AuthException
    {
        for(String pair: configString.split(",")) {
            final String parts[] = pair.split("=");
            if (parts.length != 2)
            {
                throw new AuthException(String.format("Config element %s does not contain equals sign.",pair));
            }
            final String propName = parts[0];
            final String fileName = parts[1];

            String envValue;
            try
            {
                envValue = FileUtil.getFileContents(new File(fileName));
                credentials.setProperty(propName, envValue);
            }
            catch (IOException ex)
            {
                throw new AuthException("Unable to read secret contents.", ex);
            }
            
        }
        if (credentials.isEmpty())
        {
            throw new AuthException("Configuration for environment source empty or not parseable.");
        }
    }

    @Override
    public Properties getCredentials() {
        Properties props = new Properties();
        props.putAll(credentials);
        return props; // defensive copy
    }
}
