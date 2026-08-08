package org.opendcs.lrgs.dds;

import java.security.Principal;

/**
 * updated replacement of {@link lrgs.ldds.LddsUser that doesn't assume directories on disk.
 * DdsUserPrincipal
 */
public class DdsUserPrincipal implements Principal
{
    private final String username;
    private final int version;

    public DdsUserPrincipal(String username, int version)
    {
        this.username = username;
        this.version = version;
    }

    @Override
    public String getName()
    {
        return username;
    }

    public int getDdsVersion()
    {
        return version;
    }
}
