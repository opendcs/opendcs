package org.opendcs.spi.authentication;

public class UnsupportedProviderException extends Exception
{
    public UnsupportedProviderException(String msg)
    {
        super(msg);
    }

    public UnsupportedProviderException(String msg, Throwable cause)
    {
        super(msg, cause);
    }

}
