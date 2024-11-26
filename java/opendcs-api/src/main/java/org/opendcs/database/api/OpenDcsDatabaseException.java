package org.opendcs.database.api;

public class OpenDcsDatabaseException extends Exception
{
    public OpenDcsDatabaseException(String msg)
    {
        super(msg);
    }

    public OpenDcsDatabaseException(String msg, Throwable cause)
    {
        super(msg);
    }
}
