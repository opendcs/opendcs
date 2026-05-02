/**
 * $Id$
 *
 * $Log$
 */
package opendcs.dai;

import decodes.db.DbEnum;
import decodes.db.EnumList;
import decodes.db.EnumValue;
import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;

/**
 * Data Access Interface for database-resident enumerations
 * @author mmaloney Mike Maloney
 */
public interface EnumDAI extends DaiBase
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

	
}
