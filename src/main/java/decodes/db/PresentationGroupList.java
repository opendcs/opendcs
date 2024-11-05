/*
*	$Id$
*
*	$State$
*
*	$Log$
*	Revision 1.3  2013/03/21 18:27:39  mmaloney
*	DbKey Implementation
*	
*	Revision 1.2  2009/01/22 00:31:33  mjmaloney
*	DB Caching improvements to make msgaccess start quicker.
*	Remove the need to cache the entire database.
*	
*	Revision 1.1  2008/04/04 18:21:00  cvs
*	Added legacy code to repository
*	
*	Revision 1.13  2004/08/27 12:23:10  mjmaloney
*	Added javadocs
*	
*	Revision 1.12  2003/11/17 14:54:02  mjmaloney
*	dev.
*	
*	Revision 1.11  2002/09/19 17:18:02  mjmaloney
*	SQL dev.
*	
*	Revision 1.10  2002/08/26 04:53:46  chris
*	Major SQL Database I/O development.
*
*	Revision 1.9  2001/11/07 21:37:59  mike
*	dev
*
*	Revision 1.8  2001/09/27 00:57:23  mike
*	Work on presentation elements.
*
*	Revision 1.7  2001/09/25 12:58:15  mike
*	PresentationGroup prepareForExec() written.
*
*	Revision 1.6  2001/04/21 20:19:23  mike
*	Added read & write methods to all DatabaseObjects
*
*	Revision 1.5  2001/04/13 16:46:38  mike
*	dev
*
*	Revision 1.4  2001/04/12 12:30:40  mike
*	dev
*
*	Revision 1.3  2001/04/02 00:42:33  mike
*	DatabaseObject is now an abstract base-class.
*
*	Revision 1.2  2001/03/23 20:09:25  mike
*	Collection classes are no longer monostate static collections.
*
*	Revision 1.1  2001/03/20 03:43:24  mike
*	Implement final parsers
*
*/
package decodes.db;

import ilex.util.Logger;

import java.util.Vector;
import java.util.Iterator;

import decodes.sql.DbKey;

/**
* The PresentationGroupList is the collection of all known
* PresentationGroup objects for a particular database.
*/
public class PresentationGroupList 
	extends DatabaseObject
{
	/**
	* The list of PresentationGroup objects.  This data member is
	* never null.
	*/
	private Vector<PresentationGroup> presentationGroupVec;


	/** Cross reference of PresentationGroups to SQL ID. */
	private IdRecordList pgIdList;

	private boolean _wasRead = false;
	public boolean wasRead() { return _wasRead; }
	
	/**
	* Constructor.
	*/
	public PresentationGroupList()
	{
		presentationGroupVec = new Vector<PresentationGroup>();
		pgIdList = new IdRecordList("PresentationGroup");
	}

	/**
	* This overrides the DatabaseObject's method; this returns
	* "PresentationGroupList".
	*/
	public String getObjectType() {
		return "PresentationGroupList";
	}

	/**
	  Adds a PresentationGroup object to the collection.
	  Note: SQL ID (if one is used) must be set prior to adding.
	  @param spec the PG to add.
	*/
	public void add(PresentationGroup spec)
	{
		if (!presentationGroupVec.contains(spec))
			presentationGroupVec.add(spec);
		pgIdList.add(spec);
	}

	/**
	  Retrieves a PresentationGroup object by name.
	  Returns null if the group is not found or if there's an error reading
	  it.
	  @param groupName the name of the group to find.
	  @return PresentationGroup or null if no match.
	*/
	public PresentationGroup find(String groupName)
	{
		Logger.instance().log(Logger.E_DEBUG3,
			"PresentationGroupList.find(" + groupName + ")");
		PresentationGroup thePg = null;
		for(PresentationGroup pg : presentationGroupVec)
			if (pg.groupName.equalsIgnoreCase(groupName))
			{
				thePg = pg;
				if (thePg.wasRead())
					return thePg;
				else break;
			}

		// Not found in current list. Try to read it from database.
		Logger.instance().log(Logger.E_DEBUG3,
			"Attempting to read '" + groupName + "'");
		boolean newPg = thePg == null;
		if (newPg)
			thePg = new PresentationGroup(groupName, this.getDatabase());
		try
		{
			thePg.read();
		}
		catch (DatabaseException e)
		{
			Logger.instance().log(Logger.E_DEBUG1,
				"Cannot read presentation group '" + groupName + "': " + e);
			return null;
		}
		Logger.instance().log(Logger.E_DEBUG3, "...success");
		if (newPg)
			add(thePg);
		return thePg;
	}

	/* @return PresentationGroup by database ID or null if no match. */
	public PresentationGroup getById(DbKey id)
	{
		return (PresentationGroup)pgIdList.get(id);
	}

	/**
	* @return an Iterator over the list of PresentationGroups.
	*/
	public Iterator<PresentationGroup> iterator()
	{
		return presentationGroupVec.iterator();
	}

	/**
	* @return the number of PresentationGroups in the list.
	*/
	public int size()
	{
		return presentationGroupVec.size();
	}

	/**
	  Retrieves a PresentationGroup by its index in the list.  This
	  returns null if the index is out of range.
	  @param r (row) or index into list
	  @return PresentationGroup or null if no such row.
	*/
	public PresentationGroup getPGAt(int r)
	{
		if (r < 0 || r >= size())
			return null;
		return presentationGroupVec.get(r);
	}

	/**
	  Remove a PresentationGroup from the list.
	  @param pg the group to delete
	*/
	public void remove(PresentationGroup pg)
	{
		presentationGroupVec.remove(pg);
		pgIdList.remove(pg);
	}

	/**
	* @return the underlying Vector used to store the PresentationGroups.
	*/
	public Vector<PresentationGroup> getVector() 
	{
		return presentationGroupVec;
	}

	/**
	* Overrides the DatabaseObject method.  This is not implemented yet.
	*/
	public void prepareForExec()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
		throw new InvalidDatabaseException("Not implemented");
	}

	/**
	* Overrides the DatabaseObject method.  This always returns false.
	*/
	public boolean isPrepared()
	{
		return false;
	}

	/**
	* Overrides the DatabaseObject method.  This is not implemented yet.
	*/
	public void validate()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
		throw new InvalidDatabaseException("Not implemented");
	}

	/**
	* Overrides the DatabaseObject method.  This reads the entire list
	* from the database.
	*/
	public void read()
		throws DatabaseException
	{
		presentationGroupVec.clear();
		pgIdList.clear();
		myDatabase.getDbIo().readPresentationGroupList(this);
		_wasRead = true;
	}

	/**
	* Overrides the DatabaseObject method.  This writes the entire list
	* back out to the database.
	*/
	public void write()
		throws DatabaseException
	{
		for(Iterator<PresentationGroup> it = iterator(); it.hasNext(); )
		{
			PresentationGroup ob = it.next();
			ob.write();
		}
	}
}
