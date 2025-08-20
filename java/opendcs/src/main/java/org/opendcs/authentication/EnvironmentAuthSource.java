package org.opendcs.authentication;

import java.util.Properties;

import org.opendcs.spi.authentication.AuthSource;

import ilex.util.AuthException;

/**
 * Pull credentials off the environment. Uses
 * simple key=value[,key=value] pairs to map environment
 * variables to an internal name.
 *
 * Key is the internal property name, value is the environment variable name.
 *
 */
public class EnvironmentAuthSource implements AuthSource
{
    private Properties credentials = new Properties();

    public EnvironmentAuthSource(String configString) throws AuthException
    {
        for(String pair: configString.split(",")) {
            final String parts[] = pair.split("=");
            if (parts.length != 2)
            {
                throw new AuthException(String.format("Config element %s does not contain equals sign.",pair));
            }
            final String propName = parts[0];
            final String envName = parts[1];

            String envValue = System.getenv(envName);
            if (envValue == null)
            {
                throw new AuthException(String.format("Variable '%s' is not defined in this processes environment.",envName));
            }
            credentials.setProperty(propName, envValue);
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
