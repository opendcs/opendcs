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

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.util.Iterator;

import decodes.sql.DbKey;

/**
* The PresentationGroupList is the collection of all known
* PresentationGroup objects for a particular database.
*/
public class PresentationGroupList extends DatabaseObject
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
		log.trace("PresentationGroupList.find({})", groupName);
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
		log.trace("Attempting to read '{}'", groupName);
		boolean newPg = thePg == null;
		if (newPg)
			thePg = new PresentationGroup(groupName, this.getDatabase());
		try
		{
			thePg.read();
		}
		catch (DatabaseException ex)
		{
			log.atDebug().setCause(ex).log("Cannot read presentation group '{}'", groupName);
			return null;
		}
		log.trace("...success");
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
