package org.opendcs.authentication;

public class InvalidCredentials extends OpenDcsAuthException
{
    public InvalidCredentials()
    {
        super("Invalid Credentials Provided.");
    }
}
