package org.opendcs.utils.properties;

public class NoSuchPropertyException extends Exception
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
