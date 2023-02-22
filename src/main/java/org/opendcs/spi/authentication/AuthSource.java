package org.opendcs.spi.authentication;

import java.util.Properties;

public interface AuthSource
{
    /**
     * For most implementations this will be just username and password.
     * But it is open for anything and downstream users should throw appropriate errors on
     * credentials that can't be used.
     * @return Properties object with appropriate parameters for the given source
     */
    public Properties getCredentials();
}
