/**
 * $Id$
 * 
 * $Log$
 */
package opendcs.dai;

import java.util.Collection;

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
	public DbEnum getEnum(String enumName) throws DbIoException;

	public void readEnumList(EnumList top) throws DbIoException;
	
	public void writeEnumList(EnumList enumList) throws DbIoException;
	
	public void writeEnum(DbEnum dbenum) throws DbIoException;
	
	public void close();


	public Collection<DbEnum> getEnums(DataTransaction tx) throws OpenDcsDataException;
	public DbEnum getEnum(DataTransaction tx, String enumName) throws OpenDcsDataException;
	public DbEnum getEnum(DataTransaction tx, DbKey id) throws OpenDcsDataException;
	public DbEnum writeEnum(DataTransaction tx, DbEnum dbEnum) throws OpenDcsDataException;	
}
