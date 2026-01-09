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
package org.opendcs.database.dai;

import java.util.List;
import java.util.Optional;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDao;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.model.IdentityProvider;
import org.opendcs.database.model.Role;
import org.opendcs.database.model.User;

import decodes.sql.DbKey;

/**
 * Access to user information
 */
public interface UserManagementDao extends OpenDcsDao
{
    /**
     * Return list of users given the provided limit and offset.
     * @param tx
     * @param limit -1 for no limit
     * @param offset -1 for no offset
     * @return
     */
    List<User> getUsers(DataTransaction tx, int limit, int offset) throws OpenDcsDataException;

    /**
     * Add a user to the database
     * @param tx
     * @param user
     * @return
     * @throws OpenDcsDataException
     */
    User addUser(DataTransaction tx, User user) throws OpenDcsDataException;

    /**
     * Retrieve a specific user from the database
     * @param tx
     * @param id
     * @return
     * @throws OpenDcsDataException
     */
    Optional<User> getUser(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    /**
     * Update particular user in the database
     * @param tx
     * @param id
     * @param user
     * @return
     * @throws OpenDcsDataException
     */
    User updateUser(DataTransaction tx, DbKey id, User user) throws OpenDcsDataException;

    /**
     * Remove a user from the database
     * @param tx
     * @param id
     * @throws OpenDcsDataException
     */
    void deleteUser(DataTransaction tx, DbKey id) throws OpenDcsDataException;

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
     * Get information for a particular identity provider
     * @param tx
     * @param id
     * @return
     * @throws OpenDcsDataException
     */
    Optional<IdentityProvider> getIdentityProvider(DataTransaction tx, DbKey id) throws OpenDcsDataException;

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

    /**
     * Retrieve all configured roles given limit and offset
     * @param tx
     * @param limit -1 for no limit
     * @param offset -1 for no offset
     * @return
     */

    List<Role> getRoles(DataTransaction tx, int limit, int offset) throws OpenDcsDataException;
    /**
     * Add a new role to this instance.
     * @param tx
     * @param role
     * @return The role that was created, include the id that was generated. If this database does not
     * support the creation of additional roles an exception will be thrown.
     */

    Role addRole(DataTransaction tx, Role role) throws OpenDcsDataException;
    /**
     * Retrieve details of a specific role.
     * @param tx
     * @param id
     * @return
     * @throws OpenDcsDataException
     */
    Optional<Role> getRole(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    /**
     * Update an existing role.
     * @param tx
     * @param id
     * @param role
     * @return the updated role
     * @throws OpenDcsDataException
     */
    Role updateRole(DataTransaction tx, DbKey id, Role role) throws OpenDcsDataException;

    /**
     * Delete a role from this instance. If implementation does not support removal of roles
     * an exception will be thrown.
     * @param tx
     * @param id
     * @throws OpenDcsDataException
     */
    void deleteRole(DataTransaction tx, DbKey id) throws OpenDcsDataException;
}
