/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package org.opendcs.database.model;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

import org.opendcs.authentication.IdentityProviderCredentials;
import org.opendcs.authentication.OpenDcsAuthException;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDatabase;

import decodes.sql.DbKey;

/**
 * Source of information regarding user identities.
 */
public interface IdentityProvider
{
    DbKey getId();
    String getName();
    String getType();
    ZonedDateTime getUpdatedAt();
    Map<String, Object> configToMap();

    /**
     * Perform appropriate user verification procedures and return a constructed User object.
     * @param db Instance of the database to retrieve DAOs required.
     * @param tx Existing transaction to use for this operation.
     * @param credentials Credentials object appropriate to the provider.
     * @return User if authentication succeeded, empty otherwise.
     * @throws OpenDcsAuthException Any error with authentication
     * @throws
     */
    Optional<User> login(OpenDcsDatabase db, DataTransaction tx,
                         IdentityProviderCredentials credentials) throws OpenDcsAuthException;

    /**
     * If a provider supports the operation, update the user credentials.
     * @param db Instance of the database to retrieve DAOs required.
     * @param tx Existing transaction to use for this operation.
     * @param credentials Credentials object appropriate to the provider.
     * @throws OpenDcsAuthException Any errors updating the credentials
     */
    void updateUserCredentials(OpenDcsDatabase db, DataTransaction tx,
                               User user, IdentityProviderCredentials credentials) throws OpenDcsAuthException;

    /**
    * Simple check if users can update their credentials through our interface.
    * @return
    */
    default boolean canUpdateCredentials()
    {
        return false;
    }
}
