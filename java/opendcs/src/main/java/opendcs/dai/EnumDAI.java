/**
 * $Id$
 * 
 * $Log$
 */
package opendcs.dai;

import java.util.Collection;
import java.util.Optional;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;

import decodes.db.DbEnum;
import decodes.db.EnumList;
import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;

/**
 * Data Access Interface for database-resident enumerations
 * @author mmaloney Mike Maloney
 */
public interface EnumDAI extends DaiBase
{
	@Deprecated
	public DbEnum getEnum(String enumName) throws DbIoException;

	@Deprecated
	public void readEnumList(EnumList top) throws DbIoException;
	
	@Deprecated
	public void writeEnumList(EnumList enumList) throws DbIoException;
	
	@Deprecated
	public void writeEnum(DbEnum dbenum) throws DbIoException;
	
	public void close();

	/**
	 * Get all enums
	 * @param tx
	 * @return
	 * @throws OpenDcsDataException
	 */
	public Collection<DbEnum> getEnums(DataTransaction tx) throws OpenDcsDataException;

	/**
	 * Get Enum by name
	 * @param tx transaction used to actually retrieve data
	 * @param enumName text name of the enum
	 * @return The enum if found, empty otherwise
	 * @throws OpenDcsDataException
	 */
	public Optional<DbEnum> getEnum(DataTransaction tx, String enumName) throws OpenDcsDataException;
	/**
	 * Get Enum by DbKey
	 * @param tx trancation used to actually retrieve data
	 * @param id DbKey of the Enum instance
	 * @return The enum if found ,empty otherwise
	 * @throws OpenDcsDataException
	 */
	public Optional<DbEnum> getEnum(DataTransaction tx, DbKey id) throws OpenDcsDataException;


	/**
	 * Write an enum to the database, a new enum is returned that contains any information
	 * set during the write operations, like the DbKey
	 * @param tx
	 * @param dbEnum
	 * @return DbEnum with additional information filled in.
	 * @throws OpenDcsDataException
	 */
	public DbEnum writeEnum(DataTransaction tx, DbEnum dbEnum) throws OpenDcsDataException;	
}
