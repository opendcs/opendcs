/*
* $Id$
*
* $Log$
* Revision 1.2  2013/03/21 18:27:39  mmaloney
* DbKey Implementation
*
* Revision 1.1  2008/04/04 18:21:00  cvs
* Added legacy code to repository
*
* Revision 1.3  2004/08/26 13:29:23  mjmaloney
* Added javadocs
*
* Revision 1.2  2003/01/27 14:33:47  mjmaloney
* Fix bugs associated with duplicat transport medium ids.
* This should be allowed by the code to support historical versions.
*
* Revision 1.1  2002/09/22 18:50:24  mjmaloney
* Added.
*
*/

package decodes.db;

import java.util.HashMap;

import decodes.sql.DbKey;

/**
* This class stores a collection of of IdDatabaseObjects, each of which
* has a unique ID number.
* <p>
* The SQL database interface, and in some cases (Platform) the XML
* interface uses unique IDs to reference objects.
* <p>
* This includes methods for putting
* new objects into the list, and for retrieving objects by their
* ID number.
*/
public class IdRecordList
{
	private HashMap<DbKey, IdDatabaseObject> _bunch 
		= new HashMap<DbKey, IdDatabaseObject>();
	String objectType;

	/** Construct record list for a specified type of object
	  @param objectType specifies the object type
	*/
	public IdRecordList(String objectType)
	{
		this.objectType = objectType;
	}

	/**
	  Get the object with the indicated ID number.
	  @param id the ID.
	  @return the object with the indicated ID number, or null if none.
	*/
	public IdDatabaseObject get(DbKey id)
	{
		return _bunch.get(id);
	}

	/**
	  Removes object.
	  @param obj the object to remove
	*/
	public void remove(IdDatabaseObject obj)
	{
		remove(obj.getId());
	}

	/**
	* Remove the object with the given ID from the list, if any exists.
	  @param id the ID
	*/
	public void remove(DbKey id)
	{
		_bunch.remove(id);
	}

	/**
	  Adds an object
	  @param obj the object to add
	*/
	public void add(IdDatabaseObject obj)
	{
		if (!obj.idIsSet())
			return;

		_bunch.put(obj.getId(), obj);
	}

	/**
	* @return the number of IdDatabaseObjects in this list.
	*/
	public int size()
	{
		return _bunch.size();
	}
	
	public void clear()
	{
		_bunch.clear();
	}
}
