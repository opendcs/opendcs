/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.2  2008/10/14 12:04:39  mjmaloney
*  dev
*
*  Revision 1.1  2008/04/04 18:21:09  cvs
*  Added legacy code to repository
*
*  Revision 1.3  2004/08/30 14:50:20  mjmaloney
*  Javadocs
*
*  Revision 1.2  2000/03/16 19:50:42  mike
*  Implemented Event handling wrappers.
*
*  Revision 1.1  2000/01/21 13:27:47  mike
*  Created
*
*
*/
package ilex.jni;

import ilex.util.Logger;

/**
* class Debug contains various native methods useful for debugging.
*/
public class Debug
{
	/**
	* The checkFD method is useful for tracing problems where files
	* are being left open. It periodically creates and writes a file
	* called "/tmp/checkFD". The file will contain a short message
	* with the file descriptor used.
	* @return number of open file descriptors
	*/
	public static native int checkFD( );

	static // Static initializer to load native library
	{
		String libname = "ilexjni." + OsSuffix.getOsSuffix();
		Logger.instance().info("Loading native library " + libname);
		System.loadLibrary(libname);
	}
}
