package org.opendcs.authentication;

/**
 * Should the system passing credentials into a given provider get confused the provider
 * will throw an exception of this type.
 */
public class InvalidCredentialsType extends OpenDcsAuthException
{

    public InvalidCredentialsType(String msg)
    {
        super(msg);
    }

    public InvalidCredentialsType(String msg, Throwable cause)
    {
        super(msg, cause);
    }
}
