package org.opendcs.utils.properties;

public class NoSuchPropertyException extends RuntimeException
{
    public NoSuchPropertyException(String msg)
    {
        super(msg);
    }

    public NoSuchPropertyException(String msg, Throwable cause)
    {
        super(msg, cause);
    }
}
