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
*  Revision 1.4  2004/08/30 14:50:27  mjmaloney
*  Javadocs
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

import java.io.Serializable;

/**
* Class FileException is used with FileExceptionList to collect a
* series of warning or error messages while parsing a file.
* 
* A class implementing FileParser will parse a text file as appropriate.
* When a parse error is encountered (probably by some low-level exception
* being thrown), the exception will be stored along with the line number
* in a FileException object.
*/
public class FileException implements Serializable 
{
    public int linenum;
    public Exception ex;
    
    /**
	* Constructor.
	* @param linenum the line number within the file.
	* @param ex the exception thrown on this line.
	*/
	public FileException( int linenum, Exception ex ) {
    	this.linenum = linenum;
    	this.ex = ex;
    }
    
    /**
	* @return a string containing the line number and the exception text.
	*/
	public String toString( ) 
    {
    	return "" + linenum + ": " + ex;
    }
}
