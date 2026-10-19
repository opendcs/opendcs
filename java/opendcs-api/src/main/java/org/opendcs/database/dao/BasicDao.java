package org.opendcs.database.dao;

import java.util.List;
import java.util.Optional;

import org.opendcs.database.DatabaseKey;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;

/**
 * Baseline DAO structure as used by most DAOs.
 * BasicDao
 * @param <T>
 */
public interface BasicDao<T>
{
    /**
     * Retrieve Entry by Database identifier.
     * @param tx DataTransaction used for this operation.
     * @param id DatabaseKey to lookup.
     * @return The value, or empty if not found.
     * @throws OpenDcsDataException if any errors during data operations.
     */
    Optional<T> getById(DataTransaction tx, DatabaseKey id) throws OpenDcsDataException;

    /**
     * Retrieve Entry by Friendly name.
     * @param tx DataTransaction used for this operation.
     * @param name Friendly name
     * @return The value, or empty if not found.
     * @throws OpenDcsDataException if any errors during data operations
     */
    Optional<T> getByName(DataTransaction tx, String name) throws OpenDcsDataException;    
    
    /**
     * Save a new value of type T to the database. Returning the expanded saved value.
     * @param tx DataTransaction used for this operaiton.
     * @param entry
     * @return The saved entry read back from the database. A new object is provided, Input entry
     *         is unchanged
     * @throws OpenDcsDataException
     */
    T save(DataTransaction tx, T entry) throws OpenDcsDataException;

    /**
     * Remove an entry from the database by the given identifier.
     * If no entry exists for DatabaseKey this is not considered an error and the implementation
     * should return. An implementation can log this if so desired.
     *
     * @param tx DataTransaction used for this operation.
     * @param id DatabaseKey instance.
     * @throws OpenDcsDataException any errors during data operations.
     */
    void delete(DataTransaction tx, DatabaseKey id) throws OpenDcsDataException;

    /**
     * Retrieve all entries, expanding all possible assosciated data.
     * @param tx DataTransaction used for this operation
     * @param limit limit for paging
     * @param offset offset for paging
     * @return List of entries of type T. May be an empty list.
     * @throws OpenDcsDataException any errors during data operations.
     */
    default List<T> getAll(DataTransaction tx, int limit, int offset) throws OpenDcsDataException
    {
        return getAll(tx, limit, offset, true);
    }

    /**
     * Retrieve all entries, optionally expanding all possible assosciated data.
     * 
     * For the current REST API usage "reference" lists are created that are enough information
     * for the basic tables to then lookup individual entries. This flag allows that usage
     * and puts the burden on the DAO to include it or not.
     * 
     * For any given data set implementations are free to ignore the flag and always include
     * all data. However, consider how much that is. For example PlatformConfigs with DecodesScripts
     * or Sites can get rather large with all the joins severely reducing performance. Use a combination
     * of judgement and measurement.
     * 
     * @param tx DataTransaction used for this operation
     * @param limit limit for paging
     * @param offset offset for paging
     * @param expand if data beyond the bare minimum should be pulled from the database.
     * @return List of entries of type T. May be an empty list.
     * @throws OpenDcsDataException any errors during data operations.
     */
    List<T> getAll(DataTransaction tx, int limit, int offset, boolean expand) throws OpenDcsDataException;
}
