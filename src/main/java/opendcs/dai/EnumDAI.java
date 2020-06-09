/**
 * $Id$
 * 
 * $Log$
 */
package opendcs.dai;

import decodes.db.DbEnum;
import decodes.db.EnumList;
import decodes.tsdb.DbIoException;

/**
 * Data Access Interface for database-resident enumerations
 * @author mmaloney Mike Maloney
 */
public interface EnumDAI
{
	public DbEnum getEnum(String enumName)
		throws DbIoException;

	public void readEnumList(EnumList top)
		throws DbIoException;
	
	public void writeEnumList(EnumList enumList)
		throws DbIoException;
	
	public void writeEnum(DbEnum dbenum)
		throws DbIoException;
	
	public void close();
}
