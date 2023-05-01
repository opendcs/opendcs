/*
 * $Id$
 */
package decodes.db;

import decodes.sql.DbKey;

/**
Extension of DatabaseObject for those objects that use a surrogate key
in the SQL database.
*/
public abstract class IdDatabaseObject extends DatabaseObject
{
	/** The ID */
	private DbKey _id = Constants.undefinedId;

	/** Default constructor. */
	public IdDatabaseObject()
	{
	}

	/**
	  Construct with pre-set ID.
	  @param id the ID.
	*/
	public IdDatabaseObject(DbKey id)
	{
		_id = id;
	}

	/**
	  Get the ID value.  The ID value will be a non-negative integer.
	  @return the ID
	  @throws DatabaseException  if the ID value hasn't been set.
	*/
	public DbKey getId()
	{
		return _id;
	}
	
	public DbKey getKey()
	{
		return _id;
	}

	/**
	  @return true if the ID has been set; false if not.
	*/
	public boolean idIsSet()
	{
		return !_id.isNull();
	}

	/**
	* Clears the ID so that this record will be added as new.
	*/
	public void clearId()
	{
		_id = Constants.undefinedId;
	}

	/**
	  Set the SQL Database ID value.
	  This also inserts this object into the corresponding IdRecordList.
	  @param id the ID value.
	  @throws DatabaseException if the ID on this object is already set,
	  or if the specified ID is taken by another object in the same 
	  IdRecordList.
	*/
	public void setId(DbKey id)
		throws DatabaseException
	{
		if (idIsSet() && _id.getValue() != id.getValue())
			throw new DatabaseException(
				"Cannot set ID on " + getObjectType() + " to " + id
				+ ": ID on this object already set to " + _id);
		_id = id;
	}

	/**
	 * Forces ID setting, regardles of whether it's already set.
	 * @param id the ID value.
	 */
	public void forceSetId(DbKey id)
	{
		_id = id;
	}

	/**
	 * Many high-level object have a unique display name.
	 * This method should be overloaded if appropriate.
	 * The default implementation returns the empty string.
	 */
	public String getDisplayName()
	{
		return "";
	}
}
