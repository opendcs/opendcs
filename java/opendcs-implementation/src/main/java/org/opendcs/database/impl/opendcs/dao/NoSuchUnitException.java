package org.opendcs.database.impl.opendcs.dao;

import org.opendcs.database.api.OpenDcsDataException;

public class NoSuchUnitException extends OpenDcsDataException
{

    public NoSuchUnitException(String units)
    {
        super("No unit named '" + units + "' is currently available.");
    }
    
}
