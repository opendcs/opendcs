/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
* 
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software 
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations 
* under the License.
*/
package decodes.db;

import java.util.Vector;
import java.util.Iterator;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.sql.DbKey;

/**
 * NetworkListList is a collection of all known NetworkList objects.
 */
public class NetworkListList extends DatabaseObject
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
			log.atWarn().setCause(ex).log("Attempt to retrieve non-existent network list '{}'", name);
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

