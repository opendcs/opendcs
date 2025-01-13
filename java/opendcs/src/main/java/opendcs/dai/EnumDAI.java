/**
 * $Id$
 * 
 * $Log$
 */
package opendcs.dai;

import decodes.db.DbEnum;
import decodes.db.EnumList;
import decodes.decoder.Season;
import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;

/**
 * Data Access Interface for database-resident enumerations
 * @author mmaloney Mike Maloney
 */
public interface EnumDAI
	extends AutoCloseable
{
	DbEnum getEnum(String enumName)
		throws DbIoException;

	void readEnumList(EnumList top)
		throws DbIoException;
	
	void writeEnumList(EnumList enumList)
		throws DbIoException;
	
	void writeEnum(DbEnum dbenum)
		throws DbIoException;

	void deleteEnumList(DbKey refListId)
		throws DbIoException;

	Season getSeason(String abbr)
		throws DbIoException;

	void deleteSeason(String abbr)
		throws DbIoException;

	void writeSeason(Season season, String fromAbbr, int sortNum)
		throws DbIoException;
	
	void close();
}
