/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.3  2013/03/21 18:27:39  mmaloney
*  DbKey Implementation
*
*  Revision 1.2  2009/01/22 00:31:32  mjmaloney
*  DB Caching improvements to make msgaccess start quicker.
*  Remove the need to cache the entire database.
*
*  Revision 1.1  2008/04/04 18:21:00  cvs
*  Added legacy code to repository
*
*  Revision 1.10  2004/08/27 18:41:30  mjmaloney
*  Platwiz work
*
*  Revision 1.9  2003/11/15 19:37:01  mjmaloney
*  Removed extraneous WARNING message.
*
*  Revision 1.8  2002/09/19 12:17:28  mjmaloney
*  SQL Updates.
*
*  Revision 1.7  2001/11/24 18:29:10  mike
*  First working DbImport!
*
*  Revision 1.6  2001/11/10 14:55:16  mike
*  Implementing sources & network list editors.
*
*  Revision 1.5  2001/11/09 14:35:11  mike
*  dev
*
*  Revision 1.4  2001/04/21 20:19:23  mike
*  Added read & write methods to all DatabaseObjects
*
*  Revision 1.3  2001/04/12 12:30:13  mike
*  dev
*
*  Revision 1.2  2001/04/02 00:42:33  mike
*  DatabaseObject is now an abstract base-class.
*
*  Revision 1.1  2001/04/01 17:01:10  mike
*  *** empty log message ***
*
*/
package decodes.db;

import java.util.ArrayList;
import java.util.Iterator;

import decodes.sql.DbKey;
import ilex.util.Logger;

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

	/**
	  From DatabaseObject, Does nothing.
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

