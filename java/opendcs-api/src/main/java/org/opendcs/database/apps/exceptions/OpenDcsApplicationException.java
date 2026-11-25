package org.opendcs.database.apps.exceptions;

/**
 * Errors creating, or during operations, of an OpenDCS Application.
 * OpenDcsApplicationException
 */
public class OpenDcsApplicationException extends Exception
{
    /**
     * This is the cause.
     * @param message informative message.
     */
    public OpenDcsApplicationException(String message)
    {
        super(message);
    }

    /**
     * With specific cause.
     * @param message informative message.
     * @param cause specific cause.
     */
    public OpenDcsApplicationException(String message, Throwable cause)
    {
        super(message, cause);
    }

    /**
     * Just wrapping the cause.
     * @param cause specific cause.
     */
    public OpenDcsApplicationException(Throwable cause)
    {
        super(cause);
    }
}
