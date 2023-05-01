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
*  Revision 1.4  2004/08/30 15:44:00  mjmaloney
*  Removed import statements for classes within ilex.util.
*
*  Revision 1.3  2004/08/30 14:50:28  mjmaloney
*  Javadocs
*
*  Revision 1.2  2000/03/19 22:25:02  mike
*  dev
*
*  Revision 1.1  1999/09/14 17:18:08  mike
*  09/14/1999
*
*
*/

package ilex.util;

import java.io.File;
import java.io.IOException;

/**
* Interface FileParser encapsulates the notion of a persistent
* object that can be loaded by parsing a text file. It provides
* an operation for parsing a named file.
* 
* Parsing may result in one or more 'warnings' and 'errors'.
* Warnings are considered non-fatal, that is, the parse was successful.
* Errors mean that the file could not be successfully parsed.
*/
public abstract interface FileParser
{
	/**
	* Parse the passed file and store the contents in the local object.
	* It returns a boolean value indicating whether or not the parse
	* was successful. After parsing, the caller should examine the
	* warning and error list.
	* @param input the file to parse
	* @return true if parse was successful
	* @throws IOException on IO error.
	*/
	boolean parseFile( File input ) throws IOException;
	
	/**
	* @return warnings encountered during parse.
	*/
	FileExceptionList getWarnings( );
}

