/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*
*  $Log$
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.4  2013/03/21 18:27:39  mmaloney
*  DbKey Implementation
*
*  Revision 1.3  2010/12/21 19:20:52  mmaloney
*  group computations
*
*  Revision 1.2  2009/01/03 20:31:23  mjmaloney
*  Added Routing Spec thread-specific database connections for synchronization.
*
*  Revision 1.1  2008/04/04 18:21:04  cvs
*  Added legacy code to repository
*
*  Revision 1.2  2007/06/27 20:57:37  mmaloney
*  dev
*
*  Revision 1.1  2006/03/20 20:21:45  mmaloney
*  Segregate key generator creation to separate factory class.
*
*/
package decodes.sql;

import java.sql.Connection;
import ilex.util.Logger;
import decodes.db.DatabaseException;

/**
This class contains a static method that can create a key-generator
given a class name.
*/
public class KeyGeneratorFactory
{
	/**
	 * Makes & returns a key generator, given a class name.
	 * Also calls the generator's init method with the passed connection object.
	 * @param clsname the class name.
	 * @param con the database connection
	 * @return the key generator object.
	 */
	public static KeyGenerator makeKeyGenerator(String clsname, Connection con)
		throws DatabaseException
	{
		Logger.instance().debug3("Making KeyGenerator for class '" + clsname + "'");
		try
		{
//			Class cls = ClassLoader.getSystemClassLoader().loadClass(clsname);
			Class cls = 
			  Thread.currentThread().getContextClassLoader().loadClass(clsname);
			KeyGenerator keyGenerator = (KeyGenerator)cls.newInstance();
			return keyGenerator;
		}
		catch(ClassNotFoundException ex)
		{
			String err = "Cannot load KeyGenerator from class name '"
			  + clsname + "' (Check configuration and CLASSPATH setting) " + ex;
			Logger.instance().failure(err);
			throw new DatabaseException(err);
		}
		catch(InstantiationException ex)
		{
			String err = "Cannot instantiate KeyGenerator of type '"
			  + clsname + "': (Check configuration and CLASSPATH setting) + ex";
			Logger.instance().failure(err);
			throw new DatabaseException(err);
		}
		catch(IllegalAccessException ex)
		{
			String err = "Cannot instantiate KeyGenerator of type '"
			  + clsname + "': (Does class have public no-arg constructor?)+ex"; 
			Logger.instance().failure(err);
			throw new DatabaseException(err);
		}
	}
}
