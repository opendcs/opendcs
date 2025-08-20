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
*  Revision 1.3  2005/09/07 17:19:13  mjmaloney
*  *** empty log message ***
*
*  Revision 1.2  2000/12/18 02:59:20  mike
*  *** empty log message ***
*
*  Revision 1.1  2000/12/15 23:31:02  mike
*  created
*
*
*/
package ilex.db;

import java.util.HashMap;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileInputStream;

public class DatabaseViewSet
{
	private HashMap views;

	/** Constructs a new empty DatabaseViewSet. */
	public DatabaseViewSet()
	{
		views = new HashMap();
	}

	/** 
	  Parses XML input and populates this set.
	  Note that this does not clear the set first, so that several files
	  can be loaded by making multiple calls to this method.
	  This method also does not close the passed input stream when finished.
	  @throws IOException if error occurs reading the passed stream.
	*/
	public void parseXml(InputStream is)
		throws IOException
	{
	}

	/**
	  Parses XML input and populates this set.
	  Note that this does not clear the set first, so that several files
	  can be loaded by making multiple calls to this method.
	  @throws FileNotFoundException if the passed file cannot be opened
	  for reading.
	  @throws IOException if error occurs reading the file.
	*/
	public void parseXml(File input)
		throws FileNotFoundException, IOException
	{
		FileInputStream fis = new FileInputStream(input);
		parseXml(fis);
		fis.close();
	}

	/** Clears this set. */
	public void clear()
	{
		views.clear();
	}

	/**
	  Adds the passed database view to the set. 
	  This is normally only called by internal functions when parsing XML.
	*/
	public void addDatabaseView(DatabaseView dbv)
	{
		views.put(dbv.getName(), dbv);
	}

	/**
	  Retrieves a DatabaseView object by name.
	  Throws NoSuchViewException if there is no view with the passed name.
	*/
	public DatabaseView getDatabaseView(String viewName)
		throws NoSuchViewException
	{
		Object obj = views.get(viewName);
		if (obj == null)
			throw new NoSuchViewException(
				"Cannot find database view named '" + viewName + "'");
		return (DatabaseView)obj;
	}

	/**
	  Delete the passed DatabaseView object from this set.
	  No action is taken if an instance of the passed object is not contained
	  within this set.
	*/
	public void deleteDatabaseView(DatabaseView dbv)
	{
		views.remove(dbv.getName());
	}
}

