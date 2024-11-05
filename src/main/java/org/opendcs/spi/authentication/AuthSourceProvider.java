package org.opendcs.spi.authentication;

import java.io.IOException;

import ilex.util.AuthException;

public interface AuthSourceProvider
{
    /**
     * The type this provider represents. Comparison is case-insensitive.
     * @return
     */
    public String getType();
    /**
     *
     * @param configurationString provider defined configuration data
     * @return initialized and ready to use auth source of the provider's type.
     * @throws AuthException Any issues initializing the AuthSource
     */
    public AuthSource process(String configurationString) throws AuthException;
}
