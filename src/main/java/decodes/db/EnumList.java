/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.3  2013/03/21 18:27:39  mmaloney
*  DbKey Implementation
*
*  Revision 1.2  2009/01/22 00:31:33  mjmaloney
*  DB Caching improvements to make msgaccess start quicker.
*  Remove the need to cache the entire database.
*
*  Revision 1.1  2008/04/04 18:21:00  cvs
*  Added legacy code to repository
*
*  Revision 1.19  2005/03/15 16:51:27  mjmaloney
*  *** empty log message ***
*
*  Revision 1.18  2005/03/15 16:11:26  mjmaloney
*  Modify 'Enum' for Java 5 compat.
*
*  Revision 1.17  2004/08/26 13:29:22  mjmaloney
*  Added javadocs
*
*  Revision 1.16  2003/10/20 20:22:53  mjmaloney
*  Database changes for DECODES 6.0
*
*  Revision 1.15  2002/10/31 15:56:15  mjmaloney
*  DataType.isEquivalent return true if target==this
*
*  Revision 1.14  2002/09/30 18:54:33  mjmaloney
*  SQL dev.
*
*  Revision 1.13  2002/09/24 13:13:59  mjmaloney
*  SQL dev.
*
*  Revision 1.12  2002/07/15 21:42:28  chris
*  Changed enum-values so that they are always stored in lowercase.  Added
*  quite a few javadoc comments.
*
*  Revision 1.11  2001/11/24 18:29:10  mike
*  First working DbImport!
*
*  Revision 1.10  2001/11/23 21:18:22  mike
*  dev
*
*  Revision 1.9  2001/11/21 21:19:06  mike
*  Implemented working DbInstall
*
*  Revision 1.8  2001/04/21 20:19:23  mike
*  Added read & write methods to all DatabaseObjects
*
*  Revision 1.7  2001/04/12 12:30:18  mike
*  dev
*
*  Revision 1.6  2001/04/02 00:42:33  mike
*  DatabaseObject is now an abstract base-class.
*
*  Revision 1.5  2001/03/23 20:09:25  mike
*  Collection classes are no longer monostate static collections.
*
*  Revision 1.4  2001/03/20 03:43:24  mike
*  Implement final parsers
*
*  Revision 1.3  2001/01/03 02:54:51  mike
*  dev
*
*  Revision 1.2  2000/12/23 21:56:18  mike
*  First parsing/writing version.
*
*  Revision 1.1  2000/12/22 03:52:24  mike
*  *** empty log message ***
*
*/
package decodes.db;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import decodes.sql.DbKey;

/**
 * EnumList holds a collection of all known Enum (enumeration) objects.
 * When creating a new Enum object, it is not necessary to explicitly add
 * a new Enum to the set, because the Enum constructor will automatically
 * add itself to the EnumList in the current database.
 * <p>
 *   Each Enum holds a list of EnumValues.  For example, a portion of this
 *   list might be:
 *   <dl>
 *	 <dt>Enum EquipmentType</dt>
 *	 <dd>
 *	   EnumValues 'transportmedium', 'dcp', and 'sensor'
 *	 </dd>
 *	 <dt>Enum DataOrder</dt>
 *	 <dd>
 *	   EnumValues 'a' and 'd'.
 *	 </dd>
 *	 <dt><b> . . . </b></dt>
 *   </dl>
 * </p>
 * <p>
 *   Enum names <em>are case sensitive</em>, and
 *   EnumValue names <em>are not case sensitive</em>.
 *   By convention, the
 *   capitalization of Enums is the same as Java's convention for classes,
 *   e.g. "OutputFormat".
 * </p>
 */
public class EnumList extends DatabaseObject implements HasIterator<DbEnum>
{
	/** Enums stored in a hash map by enum name. */
	private HashMap<String, DbEnum> enums;

	/** Cross reference of SQL IDs to Enums (if SQL Database is used). */
	private IdRecordList enumIdList;
	
	private boolean _readAllEnums = false;
	public boolean haveReadAllEnums() { return _readAllEnums; }
	

	/** Default constructor. */
	public EnumList()
	{
		enums = new HashMap<String, DbEnum>();
		enumIdList = new IdRecordList("Enum");
	}

	/** @return "EnumList" */
	public String getObjectType() { return "EnumList"; }

	/**
	  Adds an enum to the list.
	  SQL ID (if used) must be set prior to calling this method.
	  @param dbenum The Enum object to add
	*/
	public void addEnum(DbEnum dbenum)
	{
		enums.put(dbenum.enumName.toLowerCase(), dbenum);
		enumIdList.add(dbenum);
	}

	/**
	  Removes an enum from the list. This is only used by the DB Import
	  utility to remove the old version of an enum before adding a new one.
	  @param dbenum the Enum object to remove
	*/
	public void remove(DbEnum dbenum)
	{
		enums.remove(dbenum.enumName.toLowerCase());
		enumIdList.remove(dbenum);
	}

	/**
	  Retrieves an Enum by name.  
	  Note that the Enum name is case-sensitive.
	  @return Enum object or null if the name can't be found.  
	*/
	public DbEnum getEnum(String enumName)
	{
		return enums.get(enumName.toLowerCase());
	}

	/**
	  Returns Enum from database ID.
	  @param id the unique ID.
	  @return Enum from database ID.
	*/
	public DbEnum getById(DbKey id)
	{
		return (DbEnum)enumIdList.get(id);
	}

	/** @return internal list of Enum objects */
	public Collection<DbEnum> getEnumList()
	{
		return enums.values();
	}

	/** @return internal list of Enum objects */
	public Iterator<DbEnum> iterator()
	{
		return enums.values().iterator();
	}

	/** @return number of Enum objects */
	public int size()
	{
		return enums.size();
	}

	/**
	  From DatabaseObject interface,
	*/
	public void prepareForExec()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
	}

	/**
	  From DatabaseObject interface,
	*/
	public boolean isPrepared()
	{
		return true;
	}

	/**
	  From DatabaseObject interface,
	*/
	public void validate()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
	}

	/**
	  Reads the list of all enumerations from the database and sorts each one.
	*/
	public void read()
		throws DatabaseException
	{
		myDatabase.getDbIo().readEnumList(this);
		for(DbEnum en : enums.values())
			en.sort();
		_readAllEnums = true;
	}

	public void write()
		throws DatabaseException
	{
		myDatabase.getDbIo().writeEnumList(this);
	}


	public void clear()
	{
		enums.clear();
		enumIdList.clear();
	}
}
