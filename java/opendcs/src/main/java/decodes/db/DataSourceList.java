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

import java.util.ArrayList;
import java.util.Iterator;

import decodes.sql.DbKey;

/**
DataSourceList is a collection of all known DataSource objects.
*/
public class DataSourceList extends DatabaseObject
{
	/** Internal Vector of sources */
	private ArrayList<DataSource> dataSources;

	/** Cross reference of SQL IDs to data sources. */
	private IdRecordList dataSourceIdList;

	/** default constructor */
	public DataSourceList()
	{
		dataSources = new ArrayList<DataSource>();
		dataSourceIdList = new IdRecordList("DataSource");
	}

	/** @return "DataSourceList" */
	public String getObjectType() { return "DataSourceList"; }

	/**
	  Adds a dataSource object to the collection.
	  @param ds the DataSource to be added
	*/
	public void add(DataSource ds)
	{
		DataSource tmp = get(ds.getName());
		if (tmp != null)
		{
			dataSources.remove(tmp);
			if (tmp.idIsSet())
				dataSourceIdList.remove(tmp);
		}
		dataSources.add(ds);
		if (ds.idIsSet())
			dataSourceIdList.add(ds);
	}

	/**
	  @return a DataSource by name, or null if no match.
	*/
	public DataSource get(String name)
	{
		for(Iterator<DataSource> it = dataSources.iterator(); it.hasNext(); )
		{
			DataSource ds = it.next();
			if (name.equalsIgnoreCase(ds.getName()))
				return ds;
		}
		return null;
	}

	/**
	  @return a DataSource by database ID, or null if no match.
	*/
	public DataSource getById(DbKey id)
	{
		return (DataSource)dataSourceIdList.get(id);
	}

	/**
	  Removes a DataSource object from the list.
	*/
	public void remove(DataSource ds)
	{
		dataSources.remove(ds);
		dataSourceIdList.remove(ds);
	}

	/** @return the internal list as a Vector */
	public ArrayList<DataSource> getList()
	{
		return dataSources;
	}

	/** @return an interator into the internal list of DataSource objects. */
	public Iterator<DataSource> iterator()
	{
		return dataSources.iterator();
	}

	/** @return number of DataSource objects in the list. */
	public int size()
	{
		return dataSources.size();
	}

	/**
	 * Used by DB Editor, counts the number of routing specs using each
	 * data source.
	 */
	public void countUsedBy()
	{
		for(Iterator<DataSource> it = dataSources.iterator(); it.hasNext(); )
		{
			DataSource ds = it.next();
			ds.countUsedBy();
		}
	}

	@Override
	public void prepareForExec()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
		throw new InvalidDatabaseException("Not implemented");
	}

	@Override
	public boolean isPrepared()
	{
		return false;
	}

	/**
	  Reads the DataSource list from the database.
	  @throws DatabaseException if error.
	*/
	public void read()
		throws DatabaseException
	{
		myDatabase.getDbIo().readDataSourceList(this);
	}

	/**
	  Writes the DataSource list to the database.
	  @throws DatabaseException if error.
	*/
	public void write()
		throws DatabaseException
	{
		for(DataSource ds : dataSources)
			ds.write();
	}
	
	public void clear()
	{
		dataSources.clear();
		dataSourceIdList.clear();
	}
}

