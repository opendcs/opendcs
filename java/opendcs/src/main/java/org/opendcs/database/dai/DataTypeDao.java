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
	Optional<DataType> getDataType(DataTransaction tx, DbKey id) throws OpenDcsDataException;

	/**
     * Write/Update specific data type.
     * @param tx active transaction
     * @param dataType DataType to write
     * @throws OpenDcsDataException
     */
	DataType saveDataType(DataTransaction tx, DataType dataType) throws OpenDcsDataException;
	
    /**
     * Remove a specific data type.
     * @param tx active transaction
     * @param id id of the DataType to delete.
     * @throws OpenDcsDataException
     */
    void deleteDataType(DataTransaction tx, DbKey id) throws OpenDcsDataException;

	/**
     * Given a DataType Code attempt to find a matching data type. {@see DataType for more information}
     * @param tx active transaction
     * @param dataTypeCode data type code to search for.
     * @return
     * @throws OpenDcsDataException
     */
	Optional<DataType> lookupDataType(DataTransaction tx, String dataTypeCode) throws OpenDcsDataException;

	/**
     * Retrei
     * @param tx active transaction
     * @param limit
     * @param offset
     * @return
     * @throws OpenDcsDataException
     */
	List<DataType> getDataTypes(DataTransaction tx, int limit, int offset) throws OpenDcsDataException;

}
