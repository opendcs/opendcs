/**
 * $Id$
 *
 * $Log$
 */
package opendcs.dai;

import java.util.Collection;
import java.util.Optional;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDao;
import org.opendcs.database.api.OpenDcsDataException;

import decodes.db.DbEnum;
import decodes.db.EnumList;
import decodes.db.EnumValue;
import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;

/**
 * Data Access Interface for database-resident enumerations
 * @author mmaloney Mike Maloney
 */
public interface EnumDAI extends DaiBase, OpenDcsDao
{
	@Deprecated
	DbKey getEnumId(String enumName) throws DbIoException;

	@Deprecated
	void deleteEnumList(DbKey refListId) throws DbIoException;

	@Deprecated
	EnumValue getEnumValue(DbKey id, String enumVal) throws DbIoException;

	@Deprecated
	void deleteEnumValue(DbKey id, String enumVal) throws DbIoException;

	@Deprecated
	void writeEnumValue(DbKey enumId, EnumValue enumVal, String fromAbbr, int sortNum) throws DbIoException;

	@Deprecated
	DbEnum getEnumById(DbKey enumId) throws DbIoException;


	/**
	 *
	 * @param enumName
	 * @return
	 * @throws DbIoException
	 * @deprecated new code should use the DataTransaction based methods method
	 */
	@Deprecated
	DbEnum getEnum(String enumName) throws DbIoException;

	/**
	 *
	 * @param top EnumList
	 * @throws DbIoException
	 * @deprecated new code should use the DataTransaction based methods method
	 */
	@Deprecated
	void readEnumList(EnumList top) throws DbIoException;

	/**
	 *
	 * @param enumList
	 * @throws DbIoException
	 * @deprecated new code should use the DataTransaction based methods method
	 */
	@Deprecated
	void writeEnumList(EnumList enumList) throws DbIoException;

	/**
	 *
	 * @param dbenum
	 * @throws DbIoException
	 * @deprecated new code should use the DataTransaction based methods method
	 */
	@Deprecated
	void writeEnum(DbEnum dbenum) throws DbIoException;

	void close();

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
}
