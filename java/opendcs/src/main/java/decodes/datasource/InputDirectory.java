/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.2  2014/05/28 13:09:30  mmaloney
*  dev
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.2  2008/09/26 14:56:53  mjmaloney
*  Added <all> and <production> network lists
*
*  Revision 1.1  2008/04/04 18:21:00  cvs
*  Added legacy code to repository
*
*  Revision 1.4  2007/12/11 01:05:15  mmaloney
*  javadoc cleanup
*
*  Revision 1.3  2004/08/24 23:52:44  mjmaloney
*  Added javadocs.
*
*  Revision 1.2  2001/07/08 21:16:35  mike
*  Replaced DataSourceInterface with abstract base class DataSourceExec.
*  The base class contains a link back to the Database DataSource object.
*
*  Revision 1.1  2001/07/01 18:48:37  mike
*  Moved data source stuff to separate 'datasource' package.
*
*/
package decodes.datasource;

import java.util.Properties;
import java.util.Vector;

import decodes.db.DataSource;
import decodes.db.Database;
import decodes.db.InvalidDatabaseException;
import decodes.db.NetworkList;


/**
  DataSourceInterface realization for reading files from a specified directory.
*/
public class InputDirectory
	extends DataSourceExec
{
	/**
	 * @see decodes.datasource.DataSourceExec#DataSourceExec(DataSource, Database) DataSourceExec Constructor
	 *
	 * @param dataSource
	 * @param decodesDatabase
	 */
	public InputDirectory(DataSource ds, Database db)
	{
		super(ds,db);
	}

	/** Does nothing. */
	public void processDataSource()
		throws InvalidDatabaseException
	{
	}

	/**
	  Constructs a File object for the specified directory.
	  The 'since' time is used to filter old files: Only files with
	  a last-modify-time more recent than this will be processed.
	  If a 'timeout' property is present, subsequent calls to getRawMessage
	  will throw a timeout exception if no files appear after this
	  many seconds.
	  @param props the routing spec properties.
	  @param since the since time from the routing spec.
	  @param until the until time from the routing spec.
	  @param networkLists contains NetworkList objects.
	  <p>
	  @throws DataSourceException if the directory does not exist.
	*/
	public void init(Properties props, String since, String until, 
		Vector<NetworkList> networkLists)
		throws DataSourceException
	{
		//TODO: Add code to open directory & make sure it exists.
	}

	/**
	  Closes the directory.
	*/
	public void close()
	{
		//TODO: Close directory
	}

	/**
	  Scans the directory for a new file at the specified period.
	  If a file appears that is more recent then the specified 'since'
	  time, open it and process the data contained therein.

	  @return next raw message.

	  @throws DataSourceTimeoutException if the data source is still
	  waiting for a message and the timeout (as defined in the properties
	  when init was called) has expired.
	  @throws DataSourceException if some other problem arises.
	*/
	public RawMessage getRawMessage()
		throws DataSourceException
	{
		throw new DataSourceException("InputDirectory not implemented");
	}
}
