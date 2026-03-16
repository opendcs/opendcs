package org.opendcs.database.api;

/**
 * For situtations where if we got here, things are really messy.
 */
public class OpenDcsDataRuntimeException extends RuntimeException
{
    public OpenDcsDataRuntimeException(String msg)
    {
        super(msg);
    }

    public OpenDcsDataRuntimeException(String msg, Throwable cause)
    {
        super(msg, cause);
    }
}
