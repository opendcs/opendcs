package org.opendcs.database;

import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;

public class ExceptionHelpers
{
    private ExceptionHelpers()
    {
    }
   
    public static <T> T throwDbIoNoSuchObject(Throwable cause) throws DbIoException, NoSuchObjectException
    {
        if (cause instanceof NoSuchObjectException)
        {
            throw (NoSuchObjectException)cause;
        }
        else if (cause instanceof DbIoException)
        {
            throw (DbIoException)cause;
        }
        else
        {
            throw new DbIoException("Database Error", cause);
        }
    }
}
