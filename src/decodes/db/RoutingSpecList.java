/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.3  2013/03/21 18:27:39  mmaloney
*  DbKey Implementation
*
*  Revision 1.2  2009/08/23 14:54:55  mjmaloney
*  SFWMD Import
*
*  Revision 1.1  2008/04/04 18:21:00  cvs
*  Added legacy code to repository
*
*  Revision 1.15  2004/08/27 12:23:11  mjmaloney
*  Added javadocs
*
*  Revision 1.14  2004/02/05 21:50:20  mjmaloney
*  final release prep for 6.0
*
*  Revision 1.13  2002/09/30 18:54:34  mjmaloney
*  SQL dev.
*
*  Revision 1.12  2002/09/24 13:13:59  mjmaloney
*  SQL dev.
*
*  Revision 1.11  2002/08/26 04:53:46  chris
*  Major SQL Database I/O development.
*
*  Revision 1.10  2002/08/04 17:45:39  chris
*  Updated javadoc comments.
*
*  Revision 1.9  2001/11/12 01:49:35  mike
*  dev
*
*  Revision 1.8  2001/10/02 15:28:53  mike
*  Implemented default DataPresentation
*
*  Revision 1.7  2001/07/24 02:17:10  mike
*  dev
*
*  Revision 1.6  2001/04/21 20:19:23  mike
*  Added read & write methods to all DatabaseObjects
*
*  Revision 1.5  2001/04/13 16:46:38  mike
*  dev
*
*  Revision 1.4  2001/04/12 12:30:42  mike
*  dev
*
*  Revision 1.3  2001/04/02 00:42:33  mike
*  DatabaseObject is now an abstract base-class.
*
*  Revision 1.2  2001/03/23 20:09:25  mike
*  Collection classes are no longer monostate static collections.
*
*  Revision 1.1  2001/03/20 03:43:24  mike
*  Implement final parsers
*
*/
package decodes.db;

import java.util.Vector;
import java.util.Iterator;

import decodes.sql.DbKey;

import ilex.util.Logger;

/**
 * RoutingSpecList is a collection of all known RoutingSpec objects.
 * RoutingSpec names are not case sensitive.
 */
public class RoutingSpecList extends DatabaseObject
{
	/** Internal vector of specs */
	private Vector<RoutingSpec> routingSpecVec;

	/** Cross reference of RoutingSpec objects to SQL ID. */
	private IdRecordList rsIdList;

	/**
	If true, then find() that doesn't work is silent.
	Used by import utility. Don't want to issue warnings when checking for
	an existing list.
	*/
	public static boolean silentFind = false;

	/**
	* Constructor.
	*/
	public RoutingSpecList()
	{
		routingSpecVec = new Vector<RoutingSpec>();
		rsIdList = new IdRecordList("RoutingSpec");
	}

	/** @return "RoutingSpecList" */
	public String getObjectType() { return "RoutingSpecList"; }

	/**
	  Adds a routingSpec object to the collection.
	  The name (and ID if used) must be set prior to adding.
	  @param spec the spec to add
	*/
	public void add(RoutingSpec spec)
	{
		// MJM 20020922 - if spec is already in collection, do a replace.
		RoutingSpec rs = null;
		for(Iterator<RoutingSpec> it = iterator(); it.hasNext(); )
		{
			RoutingSpec trs = it.next();
			if (trs.getName().equalsIgnoreCase(spec.getName()))
			{
				rs = trs;
				break;
			}
		}
		if (rs != null)
			remove(rs);	

		routingSpecVec.add(spec);
		rsIdList.add(spec);
	}

	/**
	  Retrieves a routing spec by name.  The names are not case-sensitive.
	  Returns null if not found.
	  @param name the name to look for
	  @return RoutingSpec or null if no match
	*/
	public RoutingSpec find(String name)
	{
		for(Iterator<RoutingSpec> it = iterator(); it.hasNext(); )
		{
			RoutingSpec rs = it.next();
			if (rs.getName().equalsIgnoreCase(name))
				return rs;
		}

		// Not found in current list. Try to read it from database.
		Logger.instance().log(Logger.E_DEBUG3,
			"Attempting to read routing spec '" + name + "'");
		RoutingSpec rs = new RoutingSpec(name);
		try { rs.read(); }
		catch (DatabaseException e)
		{
			if (!silentFind)
				Logger.instance().log(Logger.E_FAILURE,
					"Cannot read RoutingSpec '" + name + "': " + e);
			return null;
		}
		Logger.instance().log(Logger.E_DEBUG3, "...success");
		add(rs);
		return rs;
	}

	/** Retrieve the routing spec by SQL ID. */
	public RoutingSpec getById(DbKey id)
	{
		return (RoutingSpec)rsIdList.get(id);
	}

	/**
	* @return an iterator over the list of RoutingSpec objects.
	*/
	public Iterator<RoutingSpec> iterator()
	{
		return routingSpecVec.iterator();
	}

	/**
	* @return the number of RoutingSpecs in the list.
	*/
	public int size()
	{
		return routingSpecVec.size();
	}

	/**
	  Get the list of RoutingSpec objects, as a Vector.
	  Note - caller should not modify the returned vector directly.
	  @return Vector containing RoutingSpecs
	*/
	public Vector<RoutingSpec> getList() 
	{
		return routingSpecVec;
	}

	/**
	  Remove a RoutingSpec from the list.
	  @param ob the spec to remove
	*/
	public void remove(RoutingSpec ob)
	{
		routingSpecVec.remove(ob);
		rsIdList.remove(ob);
	}

	/**
	* This overrides the DatabaseObject method.
	*/
	public void prepareForExec()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
		throw new InvalidDatabaseException("Not implemented");
	}

	/**
	* This overrides the DatabaseObject method.
	*/
	public boolean isPrepared()
	{
		return false;
	}

	/**
	* This overrides the DatabaseObject method.
	*/
	public void validate()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
		throw new InvalidDatabaseException("Not implemented");
	}

	/**
	* Read the entire list from the database.
	*/
	public void read()
		throws DatabaseException
	{
		myDatabase.getDbIo().readRoutingSpecList(this);
	}

	/**
	* Write the entire list back out to the database.
	*/
	public void write()
		throws DatabaseException
	{
		for(Iterator<RoutingSpec> it = iterator(); it.hasNext(); )
		{
			RoutingSpec ob = it.next();
			ob.write();
		}
	}
	
	public void clear()
	{
		routingSpecVec.clear();
		rsIdList.clear();
	}
}

