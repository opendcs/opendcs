package org.opendcs.database.api.exceptions.data;

/**
 * Could also be called "Foreign Key Constraint exception"; however,
 * the intent is not to be super specific to relational database terms.
 * RelatedDataConstraintException
 */
@SuppressWarnings("java:S110")
public class RelatedDataConstraintException extends OpenDcsConstraintException
{

    public RelatedDataConstraintException(String msg)
    {
        super(msg);
    }
    
    public RelatedDataConstraintException(String msg, Throwable cause)
    {
        super(msg, cause);
    }

    public RelatedDataConstraintException(Throwable cause)
    {
        super(cause);
    }
}
