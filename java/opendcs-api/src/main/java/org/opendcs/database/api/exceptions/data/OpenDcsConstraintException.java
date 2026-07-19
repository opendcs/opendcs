package org.opendcs.database.api.exceptions.data;

import org.opendcs.database.api.OpenDcsDataRuntimeException;

/**
 * Root exception for specific data operations errors related
 * to constraints such as conflicts or duplicate entries.
 * 
 * Similar to the ConstraintException in the main OpenDCS code, Intentionally duplicate
 * to start improvement the hierarchy of exceptions.
 * 
 * OpenDcsConstraintException
 */
public class OpenDcsConstraintException extends OpenDcsDataRuntimeException
{

    public OpenDcsConstraintException(String msg)
    {
        super(msg);
    }
    
    public OpenDcsConstraintException(String msg, Throwable cause)
    {
        super(msg, cause);
    }

    public OpenDcsConstraintException(Throwable cause)
    {
        super(cause);
    }
}
