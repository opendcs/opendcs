/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.5  2013/03/21 18:27:39  mmaloney
*  DbKey Implementation
*
*  Revision 1.4  2010/07/16 14:46:38  shweta
*  Replaced getSystemClassLoader with getContextClassLoader to load classes in getExecClass method
*
*  Revision 1.3  2009/04/30 15:22:11  mjmaloney
*  Iridium updates
*
*  Revision 1.2  2008/12/29 21:56:17  dlittle
*  Changes for Sort Number
*
*  Revision 1.1  2008/04/04 18:21:00  cvs
*  Added legacy code to repository
*
*  Revision 1.18  2007/06/27 20:57:36  mmaloney
*  dev
*
*  Revision 1.17  2005/03/15 16:51:27  mjmaloney
*  *** empty log message ***
*
*  Revision 1.16  2005/03/15 16:11:26  mjmaloney
*  Modify 'Enum' for Java 5 compat.
*
*  Revision 1.15  2004/08/26 13:29:22  mjmaloney
*  Added javadocs
*
*  Revision 1.14  2003/11/15 19:45:02  mjmaloney
*  Added sortNumber.
*
*  Revision 1.13  2003/10/20 20:22:53  mjmaloney
*  Database changes for DECODES 6.0
*
*  Revision 1.12  2002/09/24 13:13:59  mjmaloney
*  SQL dev.
*
*  Revision 1.11  2002/09/19 12:17:28  mjmaloney
*  SQL Updates.
*
*  Revision 1.10  2002/08/26 05:02:59  chris
*  This file has been superceded by the .pjava version.
*
*  Revision 1.1  2002/08/26 04:53:45  chris
*  Major SQL Database I/O development.
*
*  Revision 1.9  2002/07/15 21:42:28  chris
*  Changed enum-values so that they are always stored in lowercase.  Added
*  quite a few javadoc comments.
*
*  Revision 1.8  2002/06/24 03:39:37  chris
*  Initial changes in support of the SQL database functionality.
*
*  Revision 1.7  2001/10/05 00:52:16  mike
*  Implemented EmitAsciiFormatter
*
*  Revision 1.6  2001/07/24 13:22:41  mike
*  LrgsDataSource development.
*
*  Revision 1.5  2001/04/21 20:19:23  mike
*  Added read & write methods to all DatabaseObjects
*
*  Revision 1.4  2001/04/12 12:30:19  mike
*  dev
*
*  Revision 1.3  2001/04/02 00:42:33  mike
*  DatabaseObject is now an abstract base-class.
*
*  Revision 1.2  2000/12/29 02:42:52  mike
*  Created.
*
*  Revision 1.1  2000/12/21 14:31:27  mike
*  Created.
*
*
*/

package decodes.db;

/**
 * This class stores information about a single value of an enumeration,
 * which is encapsulated by the parent of this object, and Enum.
 * <p>
 *   Note that although Enum names <em>are case sensitive</em>,
 *   EnumValue names <em>are not case sensitive</em>.
 *   EnumValue names are always converted to lowercase before being stored.
 * </p>
 */
public class EnumValue extends DatabaseObject
{
	/** The enum of which this is a member. */
	public DbEnum dbenum;

	/** The name this EnumValue. Values are unique within a particular enum. */
	public String value;

	/** A short description. */
	public String description;

	/** The Java class that implements the functionality of this enum value. */
	public String execClassName;

	/** The Java class that implements an editor for this type of object. */
	public String editClassName;

	/** Determines position of this element within GUI pick-lists. */
	public int sortNumber;

	/**
	  Constant for undefined sort number is largest positive int,
	  causing the value to be sorted to the end.
	*/
	public final static int UNDEFINED_SORT_NUMBER = Integer.MAX_VALUE;

	// Links
	public Class execClass;
	public Class editClass;

	/**
	  Constructor setting just parent enum and this value.
	  The value is converted to lowercase before being stored.
	  @param e the Enum to add to
	  @param v The 'value' member for this object.
	*/
	public EnumValue(DbEnum e, String v)
	{
		dbenum = e;
		value = v.toLowerCase();
		description = null;
		execClassName = null;
		editClassName = null;
		execClass = null;
		editClass = null;
		sortNumber = UNDEFINED_SORT_NUMBER;
	}

	/**
	  Constructor.  The value is converted to lowercase before being
	  stored.
	  @param e the Enum to add to
	  @param v The 'value' member for this object.
	  @param d the description
	  @param ex the name of the executable class
	  @param ed the name of the editor class
	*/
	public EnumValue(DbEnum e, String v, String d, String ex, String ed)
	{
		this(e, v);
		description = d;
		execClassName = ex;
		editClassName = ed;
	}

	/**
	* This overrides the DatabaseObject.getObjectType() method, and
	* @return 'EnumValue'.
	*/
	public String getObjectType() {
		return "EnumValue";
	}

	/**
	* This returns the combination of the Enum's name and this value's
	* name, with a dot separating them; e.g. "OutputFormat.shef".
	* @return abbreviation of this value prefixed with enum name.
	*/
	public String getFullName()
	{
		return dbenum.enumName + '.' + value;
	}
	
	/**
	 * sets value for sort number
	 * @param x sort number to be set
	 */
	public void setSortNumber(int x)
	{
		sortNumber = x;
	}
	
	/**
	 * returns value of the sort number
	 * @return value of sort number
	 */
	public int getSortNumber()
	{
		return sortNumber;
	}
	

	/**
	  This resolves the name of the edit class into a Class object.
	  @return the editor Class object.
	  @throws ClassNotFoundException if editClassName hasn't been set,
	  or if the class loader cannot find the named class.
	*/
	public Class getEditClass()
		throws ClassNotFoundException
	{
		if (editClass != null)
			return editClass;

		if (editClassName == null || editClassName.length() == 0)
			throw new ClassNotFoundException("No edit class defined for '"
				+ getFullName() + "'");
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		editClass = cl.loadClass(editClassName);
		return editClass;
	}

	/**
	* This resolves the name of the exec class into a Class object.
	  @return the Exec Class object.
	* @throws ClassNotFoundException if execClassName hasn't been set
	* or if the class loader cannot find the named class.
	*/
	public Class getExecClass()
		throws ClassNotFoundException
	{
		if (execClass != null)
			return execClass;

		if (execClassName == null || execClassName.length() == 0)
			throw new ClassNotFoundException("No exec class defined for '"
				+ getFullName() + "'");
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		//ClassLoader cl = ClassLoader.getSystemClassLoader();
		execClass = cl.loadClass(execClassName);
		return execClass;
	}
	
	private static ClassLoader getClassLoader()
	{
		return ClassLoader.getSystemClassLoader();
//		return Thread.currentThread().getContextClassLoader();
	}


	/**
	* This overrides the DatabaseObject method; this always throws an
	* exception.
	*/
	public void prepareForExec()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
		throw new InvalidDatabaseException("Not implemented");
	}

	/**
	* This overrides the DatabaseObject method; this always returns false.
	*/
	public boolean isPrepared()
	{
		return false;
	}

	@Override
	public void validate()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
		throw new InvalidDatabaseException("Not implemented");
	}

	/**
	* This overrides the DatabaseObject.read() method, but it does nothing.
	* I/O for this is handled by the EnumList.
	*/
	public void read()
		throws DatabaseException
	{
	}

	/**
	* This overrides the DatabaseObject.write() method, but it does nothing.
	* I/O for this is handled by the EnumList.
	*/
	public void write()
		throws DatabaseException
	{
	}
	
	public static void main(String[] args)
		throws Exception
	{
		ClassLoader cl = ClassLoader.getSystemClassLoader();
		Class execClass = cl.loadClass(args[0]);
	}
}
