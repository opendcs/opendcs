/*
*  $Id$
*
*  $Log$
*  Revision 1.3  2013/03/21 18:27:39  mmaloney
*  DbKey Implementation
*
*  Revision 1.2  2009/01/03 20:31:23  mjmaloney
*  Added Routing Spec thread-specific database connections for synchronization.
*
*  Revision 1.1  2008/04/04 18:21:04  cvs
*  Added legacy code to repository
*
*  Revision 1.3  2006/03/10 21:30:42  mmaloney
*  dev
*
*  Revision 1.2  2004/09/02 12:15:27  mjmaloney
*  javadoc
*
*  Revision 1.1  2003/11/15 20:28:35  mjmaloney
*  Mods to transparently support either V5 or V6 database.
*
*/
package decodes.sql;

import java.sql.Connection;
import decodes.db.DatabaseException;

/**
This defines the interface used by DECODES to generate surrogate keys
for several of the tables in the DECODES database.
<p>
Surrogate keys are problematic for some database architectures, especially
where replication to a remote site is involved. This interface allows you
to plug-in a suitable generator for your database environment.
<p>
To install a generator, add a property to your decodes.properties file
called 'SqlKeyGenerator'. The value should be the complete path name to
a class that implements the decodes.sql.KeyGenerator interface.
<p>
In addition to the methods defined here, the instantiating class must 
also implement a no-arguments constructor. This will be used to dynamically
create the object.
*/
public interface KeyGenerator
{
	/**
	  Generates a database-unique key suitable for adding a new entry
	  to the table.
	  The 'tableName' argument should be the name of the table to which
	  a new record will be added.
	  @param tableName name of SQL table for which a new key is needed.
	*/
	public DbKey getKey(String tableName, Connection conn)
		throws DatabaseException;
}
