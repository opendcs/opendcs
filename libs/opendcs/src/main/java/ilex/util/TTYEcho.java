/*
*  $Id$
*/
package ilex.util;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

/**
This class provides a mechanism for turning on and off the terminal echo.
It only works in unix environments.
It is used by editPasswd to prevent passwords from being echoed as the user
enters them.
*/
public class TTYEcho
{
	private static boolean isWindows
		= System.getProperty("os.name").toLowerCase().startsWith("win");

	/** Turns echo on. */
	public static void on( )
	{
		if (!isWindows)
			echoCtl(true);
	}

	/** Turns echo off. */
	public static void off( )
	{
		if (!isWindows)
			echoCtl(false);
	}

	/**
	* Turns echo on or off.
	* @param on true if you want echo on.
	*/
	public static void echoCtl( boolean on )
	{
		if (isWindows)
			return;

		try 
		{
			Runtime rt = Runtime.getRuntime();
			Process p = rt.exec(
				new String[] { "/bin/sh", "-c", 
					"stty " + (on?"":"-") + "echo < /dev/tty" } );
			p.waitFor();
		}
		catch (IOException e) { } 
		catch (InterruptedException e) { }
	}

	/**
	* Turns echo off, reads password, then turns it back on.
	* @param prompt the prompt to display
	* @return the entered password string.
	*/
	public static String readPassword(String prompt, Reader in) 
	{
		echoCtl(false);
		System.out.print(prompt); 
		System.out.flush();
		String password = readLn(in);
		System.out.println("");
		echoCtl(true);
		return password;
	}

	/**
	 * @return a line of text from stdin, discarding any newlines.
	 */
	public static String readLn(Reader in)
	{
		try
		{
			
			StringBuffer sb = new StringBuffer();
			int c;
			while ((c=in.read()) != -1 && c != '\n' && c != '\r')
				sb.append((char)c);
			//while(in.ready())
			//	in.read();
			return sb.toString();
		}
		catch(IOException e) 
		{
			return null;
		}
	}
}
