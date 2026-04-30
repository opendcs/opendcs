package org.opendcs.database.dai;

import java.util.Collection;
import java.util.Optional;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDao;
import org.opendcs.database.api.OpenDcsDataException;

import decodes.db.DbEnum;
import decodes.sql.DbKey;

public interface EnumDao extends OpenDcsDao
{
    /**
	 * Get all enums
	 * @param tx
	 * @return
	 * @throws OpenDcsDataException
	 */
	Collection<DbEnum> getEnums(DataTransaction tx, int limit, int offset) throws OpenDcsDataException;

	/**
	 * Get Enum by name
	 * @param tx transaction used to actually retrieve data
	 * @param enumName text name of the enum
	 * @return The enum if found, empty otherwise
	 * @throws OpenDcsDataException
	 */
	Optional<DbEnum> getEnum(DataTransaction tx, String enumName) throws OpenDcsDataException;

	/**
	 * Get Enum by DbKey
	 * @param tx trancation used to actually retrieve data
	 * @param id DbKey of the Enum instance
	 * @return The enum if found ,empty otherwise
	 * @throws OpenDcsDataException
	 */
	Optional<DbEnum> getEnum(DataTransaction tx, DbKey id) throws OpenDcsDataException;


	/**
	 * Write an enum to the database, a new enum is returned that contains any information
	 * set during the write operations, like the DbKey
	 * @param tx
	 * @param dbEnum
	 * @return DbEnum with additional information filled in.
	 * @throws OpenDcsDataException
	 */
	DbEnum writeEnum(DataTransaction tx, DbEnum dbEnum) throws OpenDcsDataException;

	/**
	 * Remove an enum from the database
	 * @param tx
	 * @param dbEnumId
	 * @throws OpenDcsDataException
	 */
	void deleteEnum(DataTransaction tx, DbKey dbEnumId) throws OpenDcsDataException;
}
