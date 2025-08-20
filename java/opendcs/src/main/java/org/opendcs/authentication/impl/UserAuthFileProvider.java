package org.opendcs.authentication.impl;

import java.io.IOException;

import org.opendcs.spi.authentication.AuthSource;
import org.opendcs.spi.authentication.AuthSourceProvider;

import ilex.util.AuthException;
import ilex.util.UserAuthFile;

/**
 * Provider Implementation for the Default UserAuthFile.
 *
 */
public class UserAuthFileProvider implements AuthSourceProvider
{
    @Override
    public String getType()
    {
        return "UserAuthFile";
    }

    /**
     *
     * @param configurationString Path to the auth file. Variable expansion is done internally.
     * @returns UserAuthFile that has been read in.
     * @throws AuthException Unable to find the file, Can't read file contents.
     */
    @Override
    public AuthSource process(String configurationString) throws AuthException
    {
        try
        {
            UserAuthFile uaf = new UserAuthFile(configurationString);
            uaf.read();
            return uaf;
        }
        catch(IOException ex)
        {
            throw new AuthException("Unable to read auth file",ex);
        }
    }
}
