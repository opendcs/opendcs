package org.opendcs.utils.properties.conversion;

public class NoPropertyConverterException extends RuntimeException
{
    public NoPropertyConverterException(String msg)
    {
        super(msg);
    }
    
    public NoPropertyConverterException(String msg, Throwable cause)
    {
        super(msg, cause);
    }
}
