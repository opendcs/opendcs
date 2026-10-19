package org.opendcs.database.dao.lrgs;

import java.util.Optional;

import org.opendcs.data.lrgs.LrgsDataInterfaceType;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dao.BasicDao;

public interface LrgsInterfaceTypeDao extends BasicDao<LrgsDataInterfaceType>
{
    /**
     * Lookup the entry for a given service provider, if it exists.
     * 
     * This will primarily be used by the backend to determine if a new entry should
     * be added to the database.
     *
     * @param tx DataTransaction to use for this operation.
     * @param serviceProvider name of the service provider instance to lookup
     * @return The LrgsDataInterfaceType value, or empty if one has not been stored.
     * @throws OpenDcsDataException
     */
    Optional<LrgsDataInterfaceType> getByServiceProvider(DataTransaction tx, String serviceProvider) throws OpenDcsDataException;    
}
