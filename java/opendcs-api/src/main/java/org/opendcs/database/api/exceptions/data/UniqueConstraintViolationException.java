package org.opendcs.database.api.exceptions.data;

public class UniqueConstraintViolationException extends OpenDcsConstraintException
{

    public UniqueConstraintViolationException(String msg)
    {
        super(msg);
    }
    
    public UniqueConstraintViolationException(String msg, Throwable cause)
    {
        super(msg, cause);
    }

    public UniqueConstraintViolationException(Throwable cause)
    {
        super(cause);
    }
}
