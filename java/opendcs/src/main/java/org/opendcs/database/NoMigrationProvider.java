package org.opendcs.database;

public class NoMigrationProvider extends Exception
{
    public NoMigrationProvider(String msg)
    {
        super(msg);
    }

    public NoMigrationProvider(String msg, Throwable cause)
    {
        super(msg,cause);
    }
}
