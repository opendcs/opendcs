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
 * RoutingSpecList is a collection of all known RoutingSpec objects.
 * RoutingSpec names are not case sensitive.
 */
public class RoutingSpecList extends DatabaseObject
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
		// if spec is already in collection, do a replace.
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
		log.trace("Attempting to read routing spec '{}'", name);
		RoutingSpec rs = new RoutingSpec(name);
		try { rs.read(); }
		catch (DatabaseException ex)
		{
			if (!silentFind)
				log.atError().setCause(ex).log("Cannot read RoutingSpec '{}'", name);
			return null;
		}
		log.trace("...success");
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

