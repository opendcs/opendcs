package org.opendcs.database.dai;

import java.util.List;
import java.util.Optional;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDao;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.model.User;

import decodes.sql.DbKey;

public interface UsersDao extends OpenDcsDao {
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

}