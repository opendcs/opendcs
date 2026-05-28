package org.opendcs.database.dai;

import java.util.List;
import java.util.Optional;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDao;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.model.Role;

import decodes.sql.DbKey;

public interface RolesDao extends OpenDcsDao
{
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
