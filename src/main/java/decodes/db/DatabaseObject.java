/*
*  $Id$
*/
package decodes.db;

import org.cobraparser.html.domimpl.HTMLElementBuilder.P;

/**
DatabaseObject is a base class for most objects which are read from the
database (or XML files). It provides a common interface for validation
and preparing the object for executing (inside the decoder).

It also provides an association back to the database to which this object
belongs, and indirectly to the interface (XML or SQL) which read this
object and all of its peers.
*/
public abstract class DatabaseObject
{
	/** The database where this object originated */
	protected Database myDatabase;

	/** The time this object was read from the database, as reported
	    by System.currentTimeMillis().
	*/
	private long timeLastRead;

  /**
   * This no-args constructor is called implicitly.
   * It sets myDatabase to the 'current' database in use.
   * This is usually what we want. It may
   * be changed explicitly after creation.
   */
	public DatabaseObject()
	{
		this(Database.getDb());
	}

	/**
	 * Initialize a database objected as owned by a given database
	 * @param db
	 */
	public DatabaseObject(Database db)
	{

		timeLastRead = 0L;
	}

	/**
	  Sets this objects internal database reference. This is usually
	  called only when the object is instantiated.
	  @param db the database
	*/
	public void setDatabase(Database db)
	{
		myDatabase = db;
	}

	/**
	  @return the database that this object belongs to.
	*/
	public Database getDatabase()
	{
		return myDatabase;
	}

	/**
	  Sets timeLastRead to current time.
	*/
	public void setTimeLastRead()
	{
		timeLastRead = System.currentTimeMillis();
	}

	/**
	  @return the time this object was read from the database.
	*/
	public long getTimeLastRead()
	{
		return timeLastRead;
	}

	/**
	  @return a string containing the object type.
	*/
	public abstract String getObjectType();

	/**
	  Prepares this object for execution by the decoding engine.
	  This means establishing links to other run-time classes, instantiating
	  delegates, building run-time data structures, and recursively calling
	  prepareForExec in any subordinate objects.
	  @throws IncompleteDatabaseException if records which are necessary
	  or referenced are missing.
	  @throws InvalidDatabaseException if information in this object
	  is invalid or inconsistent.
	*/
	public abstract void prepareForExec()
		throws IncompleteDatabaseException, InvalidDatabaseException;

	/**
	  @return true if this object is ready for execution.
	  The database cache may contain objects which have not yet been
	  prepared for execution. This allows the run-time programs to test
	  and only prepare an object once, when it is needed.
	  If this method returns false, the caller should then call
	  prepareForExec().
	  <p>
	  The contract is that after a successful completion of prepareForExec,
	  the concrete object implementing this interface must return true
	  for isPrepared().
	  @return true on success.
	*/
	public abstract boolean isPrepared();

	/**
	  Validates this database object.
	  This method may be called by database editors, which have no intention
	  of 'executing' the object. If all the information in this object is
	  consistent and valid, this method should return sucessfully. Otherwise,
	  it will throw an exception.
	  @throws IncompleteDatabaseException if records which are necessary
	  or referenced are missing.
	  @throws InvalidDatabaseException if information in this object
	  is invalid or inconsistent.
	*/
	public void validate()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
	}

	/**
	  Reads this object from the database. This method may be called on
	  collection-type objects to populate the collection from the database.
	  It may be called on non-collection objects to ensure that the object
	  is completely fetched from the database.
	*/
	public abstract void read()
		throws DatabaseException;

	/**
	  Writes this object back to the database.
	*/
	public abstract void write()
		throws DatabaseException;

	/**
	 * @return true if two objects are non-null, have the same class, and
	 * return true to the classes equals() method; or if both objects are null.
	 * This method is used by the various equals() methods in the subclasses
	 * to check for equality in members.
	 */
	protected static boolean eqChk(Object obj1, Object obj2)
	{
		if (obj1 == obj2)
			return true;

		if (obj1 != null)
		{
			if (obj2 == null)
				return false;
			if (!obj1.getClass().equals(obj2.getClass()))
				return false;
			return obj1.equals(obj2);
		}
		else if (obj2 != null)
			return false;
		return true; // both are null
	}
}
