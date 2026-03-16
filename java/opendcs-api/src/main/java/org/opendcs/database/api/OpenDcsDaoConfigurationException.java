package org.opendcs.database.api;

/**
 * Primarily for errors dealing with the setup of dependency injection in DAOs
 */
public class OpenDcsDaoConfigurationException extends OpenDcsDataRuntimeException
{

    public OpenDcsDaoConfigurationException(String msg)
    {
        super(msg);
    }

    public OpenDcsDaoConfigurationException(String msg, Throwable cause)
    {
        super(msg, cause);
    }
}
