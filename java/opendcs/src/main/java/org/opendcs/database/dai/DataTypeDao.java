package org.opendcs.database.dai;

import java.util.List;
import java.util.Optional;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDao;
import org.opendcs.database.api.OpenDcsDataException;

import decodes.db.DataType;
import decodes.sql.DbKey;

/**
 * Interface for retrieving DataType information.
 */
public interface DataTypeDao extends OpenDcsDao
{
    /**
     * Retrieve data type by key
     * @param tx active transaction
     * @param id known datatype key
     * @return the DataType instance, if found, otherwise empty.
     * @throws OpenDcsDataException
     */
	Optional<DataType> getById(DataTransaction tx, DbKey id) throws OpenDcsDataException;

	/**
     * Write/Update specific data type.
     * @param tx active transaction
     * @param dataType DataType to write
     * @throws OpenDcsDataException
     */
	DataType save(DataTransaction tx, DataType dataType) throws OpenDcsDataException;
	
    /**
     * Remove a specific data type.
     * @param tx active transaction
     * @param id id of the DataType to delete.
     * @throws OpenDcsDataException
     */
    void delete(DataTransaction tx, DbKey id) throws OpenDcsDataException;

	/**
     * Given a DataType Code attempt to find a matching data type. {@see DataType for more information}
     * @param tx active transaction
     * @param dataTypeCode data type code to search for.
     * @return
     * @throws OpenDcsDataException
     */
	Optional<DataType> lookup(DataTransaction tx, String dataTypeCode) throws OpenDcsDataException;

	/**
     * Retreive all DataTypes constrained to a limit and office if desired.
     * @param tx active transaction
     * @param limit -1 for all, otherwise maximum amount
     * @param offset -1 for no offset, otherwise a valid office from the start of data
     * @return
     * @throws OpenDcsDataException
     */
	List<DataType> getDataTypes(DataTransaction tx, int limit, int offset) throws OpenDcsDataException;

}
