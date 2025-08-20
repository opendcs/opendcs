package org.opendcs.authentication.impl;

import org.opendcs.authentication.EnvironmentAuthSource;
import org.opendcs.spi.authentication.AuthSource;
import org.opendcs.spi.authentication.AuthSourceProvider;

import ilex.util.AuthException;

public class EnvironmentAuthSourceProvider implements AuthSourceProvider
{

    @Override
    public String getType()
    {
        return "env-auth-source";
    }

    @Override
    public AuthSource process(String configurationString) throws AuthException
    {
        EnvironmentAuthSource eau = new EnvironmentAuthSource(configurationString);
        return eau;
    }
}
