/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.5  2013/03/21 18:27:39  mmaloney
*  DbKey Implementation
*
*  Revision 1.4  2012/09/27 03:07:21  mmaloney
*  Fixed duplicate entries in GUI problem.
*
*  Revision 1.3  2008/09/29 00:22:08  mjmaloney
*  Network List Maintenance GUI Improvements
*
*  Revision 1.2  2008/09/26 20:49:02  mjmaloney
*  Added <all> and <production> network lists
*
*  Revision 1.1  2008/04/04 18:21:00  cvs
*  Added legacy code to repository
*
*  Revision 1.14  2004/11/01 14:43:22  mjmaloney
*  Reload platforms on change
*
*  Revision 1.13  2004/08/26 13:29:25  mjmaloney
*  Added javadocs
*
*  Revision 1.12  2002/09/30 18:54:33  mjmaloney
*  SQL dev.
*
*  Revision 1.11  2002/09/20 12:59:07  mjmaloney
*  SQL Dev.
*
*  Revision 1.10  2002/08/26 04:53:45  chris
*  Major SQL Database I/O development.
*
*  Revision 1.9  2001/11/10 14:55:16  mike
*  Implementing sources & network list editors.
*
*  Revision 1.8  2001/07/23 14:56:13  mike
*  dev
*
*  Revision 1.7  2001/06/12 14:14:27  mike
*  dev
*
*  Revision 1.6  2001/04/21 20:19:23  mike
*  Added read & write methods to all DatabaseObjects
*
*  Revision 1.5  2001/04/13 16:46:38  mike
*  dev
*
*  Revision 1.4  2001/04/12 12:30:31  mike
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
 * NetworkListList is a collection of all known NetworkList objects.
 */
public class NetworkListList extends DatabaseObject
{
	/** Vector of all known network lists. */
	private Vector<NetworkList> networkListVec;

	/** Cross reference of network lists by SQL ID. */
	private IdRecordList nlIdList;

	/**
	* Constructor.
	*/
	public NetworkListList()
	{
		networkListVec = new Vector<NetworkList>();
		nlIdList = new IdRecordList("NetworkList");
	}

	/**
	* This overrides the DatabaseObject method.  This returns
	* "NetworkListList".
	*/
	public String getObjectType() { return "NetworkListList"; }

	/**
	* Adds a networkList object to the collection.
	* The name (and ID if used) must be set prior to adding to the collection.
	* Guaranteed not to duplicate reference in the list.
	  @param spec the network list to add
	*/
	public void add(NetworkList spec)
	{
		for(Iterator<NetworkList> nlit = networkListVec.iterator(); nlit.hasNext(); )
		{
			NetworkList nl = nlit.next();
			if (nl.name.equalsIgnoreCase(spec.name))
			{
				nlit.remove();
				break;
			}
		}
		networkListVec.add(spec);
		nlIdList.add(spec);
	}

	/** 
	  Retrieves a network list by name 
	  @param name the name to search for
	  @return NetworkList or null if no match
	*/
	public NetworkList find(String name)
	{
		if (name.equalsIgnoreCase("<all>"))
			return NetworkList.dummy_all;
		else if (name.equalsIgnoreCase("<production>"))
			return NetworkList.dummy_production;
		for(NetworkList nl : networkListVec)
			if (nl.name.equalsIgnoreCase(name))
				return nl;
		return null;
	}

	/**
	  Gets a network list either from the cache or from the physical database.
	  @param name NetworkList name
	  @return NetworkList object or null if no match.
	*/
	public NetworkList getNetworkList(String name)
	{
		NetworkList ret = find(name);
		if (ret != null)
			return ret;
		ret = new NetworkList(name);
		try 
		{
			ret.read();
			add(ret);
			return ret;
		}
		catch(DatabaseException ex)
		{
			Logger.instance().warning("Attempt to retrieve non-existant "
				+ "network list '" + name + "': " + ex);
			return null;
		}
	}

	/** 
	  Removes a network list.  
	  @param nl the list to remove
	*/
	public void remove(NetworkList nl)
	{
		networkListVec.remove(nl);
		nlIdList.remove(nl);
	}

	/** @return an iterator over the list of network lists.  */
	public Iterator<NetworkList> iterator()
	{
		return networkListVec.iterator();
	}

	/** @return the number of network lists in this list.  */
	public int size()
	{
		return networkListVec.size();
	}

	/** @return internal Vector of NetworkList objects */
	public Vector<NetworkList> getList()
	{
		return networkListVec;
	}

	/**
	  Finds a network list given a database ID.
	  @param id the database ID
	  @return the NetworkList or null if no match
	*/
	public NetworkList getById(DbKey id)
	{
		return (NetworkList)nlIdList.get(id);
	}

	/**
	  From DatabaseObject
	*/
	public void prepareForExec()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
		throw new InvalidDatabaseException("Not implemented");
	}

	/**
	  From DatabaseObject
	  @return false
	*/
	public boolean isPrepared()
	{
		return false;
	}

	/**
	  From DatabaseObject
	*/
	public void validate()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
		throw new InvalidDatabaseException("Not implemented");
	}

	/**
	  Reads all network lists from the database.
	*/
	public void read()
		throws DatabaseException
	{
		myDatabase.getDbIo().readNetworkListList(this);
	}

	/** Writes all network lists back to the database */
	public void write()
		throws DatabaseException
	{
		for(NetworkList ob : networkListVec)
			ob.write();
	}
	
	public void clear()
	{
		networkListVec.clear();
		nlIdList.clear();
	}
}

