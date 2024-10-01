package org.opendcs.authentication.impl;

import org.opendcs.authentication.PropertyAuthSource;
import org.opendcs.spi.authentication.AuthSource;
import org.opendcs.spi.authentication.AuthSourceProvider;

import ilex.util.AuthException;

public class PropertyAuthSourceProvider implements AuthSourceProvider
{

    @Override
    public String getType()
    {
        return "java-auth-source";
    }

    @Override
    public AuthSource process(String configurationString) throws AuthException
    {
        PropertyAuthSource pau = new PropertyAuthSource(configurationString);
        return pau;
    }
}