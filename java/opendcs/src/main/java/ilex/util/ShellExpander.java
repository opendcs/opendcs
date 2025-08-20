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
*  Revision 1.3  2004/09/22 23:00:23  mjmaloney
*  Solaris port, Release 4.0 preparation
*
*  Revision 1.2  2004/08/30 14:50:31  mjmaloney
*  Javadocs
*
*  Revision 1.1  1999/11/18 17:04:57  mike
*  Added ShellExpander class to expand tilde & shell variables.
*
*
*/
package ilex.util;

import java.io.InputStream;

/**
This class uses a native shell to expand strings. All expansions that
are performed by the native shell (e.g. tilde, environment variable)
are supported.
WARNING: This only works in Unix environments.
@see EnvExpander which uses a the JRE system properties.
*/
public class ShellExpander
{
	static private String shell =
		System.getProperty("os.name").toLowerCase().startsWith("sun") ?
		"/bin/ksh" : "/bin/sh";

	/**
	* Expand the passed string according to the local shell's conventions.
	* If an exception is thrown, return the passed string unchanged.
	* @param str the string to expand
	* @return the expanded string
	*/
	public static String expand( String str )
	{
		try
		{
			String execstr[] = new String[3];
			execstr[0] = shell;
			execstr[1] = "-c";
			execstr[2] = "echo " + str;
			Process p = Runtime.getRuntime().exec(execstr);
			InputStream is = p.getInputStream();
			StringBuffer ret = new StringBuffer();
			int c;
			while((c = is.read()) > 0 && c != (int)'\n' && c != (int)'\r')
				ret.append((char)c);
			return new String(ret);
		}
		catch(Exception e)
		{
			return str;
		}
	}

	/**
	* Test main
	* @param args arg[0] is expanded and echoed.
	*/
	public static void main( String[] args )
	{
		System.out.println("Expanded '" + args[0] + "': " + expand(args[0]));
	}
}
