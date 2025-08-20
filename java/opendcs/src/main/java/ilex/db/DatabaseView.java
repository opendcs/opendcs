/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:08  cvs
*  Added legacy code to repository
*
*  Revision 1.1  2000/12/18 02:59:20  mike
*  *** empty log message ***
*
*
*/
package ilex.db;

import java.util.Vector;

public class DatabaseView
{
	private String name;     // Name of this view.
	private Vector tables;   // Top level tables.

	/**
	  Constructs a new Database View with the given name.
	*/
	public DatabaseView(String name)
	{
		this.name = name;
	}

	/** Retrieves the name of this view. */
	public String getName()
	{
		return name;
	}

	/** Adds a new DatabaseViewTable object to this view at the top level. */
	public void addTable(DatabaseViewTable tab)
	{
		tables.add(tab);
	}

	/** Returns the number of top-level tables in this view. */
	public int getNumTables()
	{
		return tables.size();
	}

	/** Returns the nth top-level table in this view. */
	public DatabaseViewTable getTable(int idx)
	{
		return (DatabaseViewTable)tables.elementAt(idx);
	}

	/** Returns the top-level table with the given name, or null if not found.*/
	public DatabaseViewTable getTable(String tableName)
	{
		for(int i = 0; i<tables.size(); i++)
		{
			DatabaseViewTable dbvt = (DatabaseViewTable)tables.elementAt(i);
			if (dbvt.getTableName().equals(tableName))
				return dbvt;
		}
		return null;
	}
}

