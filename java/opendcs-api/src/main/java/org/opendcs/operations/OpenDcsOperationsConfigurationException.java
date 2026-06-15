package org.opendcs.operations;

public class OpenDcsOperationsConfigurationException extends RuntimeException
{
    public OpenDcsOperationsConfigurationException(String msg)
    {
        super(msg);
    }

    public OpenDcsOperationsConfigurationException(String msg, Throwable cause)
    {
        super(msg, cause);
    }
}
