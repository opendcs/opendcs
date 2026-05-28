package org.opendcs.database.dai;

import java.util.List;
import java.util.Optional;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDao;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.model.IdentityProvider;

import decodes.sql.DbKey;

public interface IdentityProviderDao extends OpenDcsDao
{
        /**
     * Retrieve list of identity providers that this implementation and instance supports
     * @param tx
     * @param limit -1 for no limit
     * @param offset -1 for no offset
     * @return
     * @throws OpenDcsDataException
     */
    List<IdentityProvider> getIdentityProviders(DataTransaction tx, int limit, int offset) throws OpenDcsDataException;

    /**
     * Retreieve a list of identity providers that can verify this subject
     * @param tx
     * @param subject username, email, subject provided by the user and listed in the mappings table.
     * @return All valid providers or empty list if the subject is not registered.
     * @throws OpenDcsDataException
     */
    List<IdentityProvider> getIdentityProvidersForSubject(DataTransaction tx, String subject) throws OpenDcsDataException;

    /**
     * Add a new Identity provider configuration to the system
     * @param tx
     * @param provider
     * @return
     * @throws OpenDcsDataException
     */
    IdentityProvider addIdentityProvider(DataTransaction tx, IdentityProvider provider) throws OpenDcsDataException;

    /**
     * Get information for a particular identity provider by id.
     * @param tx
     * @param id
     * @return
     * @throws OpenDcsDataException
     */
    Optional<IdentityProvider> getIdentityProvider(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    /**
     * Get information for a particular identity provider by name.
     * @param tx
     * @param name
     * @return
     * @throws OpenDcsDataException
     */
    Optional<IdentityProvider> getIdentityProvider(DataTransaction tx, String name) throws OpenDcsDataException;

    /**
     * Update settings of a particular identity provider
     * @param tx
     * @param id
     * @param provider
     * @return
     * @throws OpenDcsDataException
     */
    IdentityProvider updateIdentityProvider(DataTransaction tx, DbKey id, IdentityProvider provider) throws OpenDcsDataException;

    /**
     * Remove an identity provider
     * @param tx
     * @param id
     * @throws OpenDcsDataException
     */
    void deleteIdentityProvider(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    
}
