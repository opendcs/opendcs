package org.opendcs.authentication;

public class InvalidCredentialsException extends OpenDcsAuthException
{
    public InvalidCredentialsException()
    {
        super("Invalid Credentials Provided.");
    }
}
