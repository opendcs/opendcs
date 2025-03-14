package org.opendcs.authentication.impl;

import java.util.Properties;

import org.opendcs.spi.authentication.AuthSource;
import org.opendcs.spi.authentication.AuthSourceProvider;

import ilex.util.AuthException;

/*
 * Primarily used by the XML fake "jdbc" connection to allow consistent
 * code.
 */
public class NoOpAuthSourceProvider implements AuthSourceProvider
{
    public static final String PROVIDER_NAME = "noop";
    @Override
    public String getType()
    {
        return PROVIDER_NAME;
    }

    @Override
    public AuthSource process(String configurationString) throws AuthException
    {
        return new AuthSource()
        {

            @Override
            public Properties getCredentials()
            {
                return new Properties();
            }
            
        };
    }
}
