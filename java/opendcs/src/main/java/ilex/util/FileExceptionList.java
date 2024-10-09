/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:10  cvs
*  Added legacy code to repository
*
*  Revision 1.7  2004/08/30 14:50:27  mjmaloney
*  Javadocs
*
*  Revision 1.6  2000/01/25 01:58:35  mike
*  Added toString() method to print entire list.
*
*  Revision 1.5  1999/09/16 14:52:43  mike
*  9/16/1999
*
*  Revision 1.4  1999/09/15 15:57:20  mike
*  Changed inheritence - used to inherit from java.util.LinkedList. I changed
*  this to java.util.Vector so it would be compatible with JDK 1.1.7 on Linux.
*
*  Revision 1.3  1999/09/03 17:22:23  mike
*  Put in new package hierarchy
*
*  Revision 1.2  1999/09/03 15:20:57  mike
*  Added headers
*
*
*/

package ilex.util;

import java.util.LinkedList;
import java.util.ListIterator;
import java.io.File;

/**
* Class FileExceptionList holds a collection of FileException objects.
* It is used by classes that implement the FileParser interface.
* This class class can also hold the name of the file being parsed, if
* needed.
*/
public class FileExceptionList extends LinkedList
{
    public File file;
    
    /**
	* Instantiate an empty FileExceptionList object.
	*/
	public FileExceptionList( ) 
	{
    }
    
    /**
	* Store name of file being parsed inside this object for future
	* reference.
	* @param file the file
	*/
	public FileExceptionList( File file ) 
	{
    	this();
    	this.file = new File(file.toString());
    }

	/**
	* Add a FileException object to the list. Pass this function
	* the exception that was thrown and the line number being parsed.
	* @param line the line number
	* @param e the exception
	* @return true
	*/
	public boolean add( int line, Exception e ) 
	{
		try { add(new FileException(line, e)); }
		catch(IllegalArgumentException iae) {}
		return true;
	}

	/**
	* Add a FileException object to the list.
	* @param o the FileException object
	* @return true
	*/
	public boolean add( Object o ) throws IllegalArgumentException 
	{
    	if (o instanceof FileException)
    		super.add(o);
    	else
    		throw new IllegalArgumentException(
    			"FileExceptionList can only hold FileException objects.");
		return true;
    }

	/**
	* The toString function, prints the entire list suitable for display.
	* @return String
	*/
	public String toString( )
	{
		if (size() == 0)
			return "";

		StringBuffer ret = new StringBuffer("File '" + file.getName() 
			+ "' exceptions:\n");
		for(ListIterator li = listIterator(0); li.hasNext(); )
		{
			FileException fe = (FileException)li.next();
			ret.append(fe);
			ret.append("\n");
		}
		return new String(ret);
	}
}
