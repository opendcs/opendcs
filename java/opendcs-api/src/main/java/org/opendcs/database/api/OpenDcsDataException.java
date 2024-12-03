package org.opendcs.database.api;

public class OpenDcsDataException extends Exception
{
    public OpenDcsDataException(String msg)
    {
        super(msg);
    }

    public OpenDcsDataException(String msg, Throwable cause)
    {
        super(msg);
    }
}
