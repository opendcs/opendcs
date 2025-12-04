package org.opendcs.authentication;

/**
 * Should the system passing credentials into a given provider get confused the provider
 * will throw an exception of this type.
 */
public class InvalidCredentialsTypeException extends OpenDcsAuthException
{

    public InvalidCredentialsTypeException(String msg)
    {
        super(msg);
    }

    public InvalidCredentialsTypeException(String msg, Throwable cause)
    {
        super(msg, cause);
    }
}
