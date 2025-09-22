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

public interface UserManagement extends OpenDcsDao
{
    // todo: add paging?
    List<User> getUsers(DataTransaction tx) throws OpenDcsDataException;
    User addUser(DataTransaction tx, User user) throws OpenDcsDataException;
    Optional<User> getUser(DataTransaction tx, DbKey id) throws OpenDcsDataException;
    void updateUser(DataTransaction tx, DbKey id, User user) throws OpenDcsDataException;
    void deleteUser(DataTransaction tx, DbKey id) throws OpenDcsDataException;


    List<IdentityProvider> getIdentityProviders(DataTransaction tx) throws OpenDcsDataException;
    IdentityProvider addIdentityProvider(DataTransaction tx, IdentityProvider provider) throws OpenDcsDataException;
    Optional<IdentityProvider> getIdentityProvider(DataTransaction tx, DbKey id) throws OpenDcsDataException;
    void updateIdentityProvider(DataTransaction tx, DbKey id, IdentityProvider provider) throws OpenDcsDataException;
    void deleteIdentityProvider(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    // todo: add paging?
    List<Role> getRoles();
    Role addRole(DataTransaction tx, Role role);
    Optional<Role> getRole(DataTransaction tx, DbKey id);
    void updateRole(DataTransaction tx, DbKey id, Role role);
    void deleteRole(DataTransaction tx, DbKey id);
}
