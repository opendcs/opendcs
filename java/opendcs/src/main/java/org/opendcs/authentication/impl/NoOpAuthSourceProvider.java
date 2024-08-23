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
    @Override
    public String getType()
    {
        return "noop";
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
