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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

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
		String libname = "ilexjni." + getOsSuffix();
		Logger.instance().info("Loading native library " + libname);
		System.loadLibrary(libname);
	}
	
	public static String getOsSuffix()
	{
		String osn = System.getProperty("os.name");
		if (osn == null)
			return "unknown";
		osn = osn.toLowerCase();
		if (osn.startsWith("win"))
			return "win";
		else if (osn.startsWith("sunos"))
			return "sol10";
		try
		{
			Process uname = Runtime.getRuntime().exec("uname -rp");
			InputStreamReader isr = new InputStreamReader(
				uname.getInputStream());
			BufferedReader bis = new BufferedReader(isr);
			String line = bis.readLine();

			// RHEL3 is Kernel version 2.4.xxxxx
			if (line.startsWith("2.4")) 
				return "el3.32";
			int bits = 32;
			String n = System.getProperty("sun.arch.data.model");
			if (n != null && n.contains("64"))
				bits = 64;
			int rhelVersion = line.contains("el5") ? 5 : 4;
			return "el" + rhelVersion + "." + bits;
		}
		catch(IOException ex)
		{
			return "unknown";
		}
	}

}
