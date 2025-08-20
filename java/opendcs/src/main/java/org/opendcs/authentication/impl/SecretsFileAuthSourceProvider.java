package org.opendcs.authentication.impl;

import org.opendcs.spi.authentication.AuthSource;
import org.opendcs.spi.authentication.AuthSourceProvider;

import ilex.util.AuthException;

public class SecretsFileAuthSourceProvider implements AuthSourceProvider
{

    @Override
    public String getType()
    {
        return "secrets-auth-source";
    }

    @Override
    public AuthSource process(String configurationString) throws AuthException
    {
        SecretsFileAuthSource sfau = new SecretsFileAuthSource(configurationString);        
        return sfau;
    }
}
