package org.opendcs.database.exceptions;

import org.opendcs.database.api.OpenDcsDataException;

public class RequiredSiteNameMissingException extends OpenDcsDataException
{

    public RequiredSiteNameMissingException(String siteNameType)
    {
        super(String.format("A Site name of type '%s' is required but one was not provided.", siteNameType));
    
    }
    
}
