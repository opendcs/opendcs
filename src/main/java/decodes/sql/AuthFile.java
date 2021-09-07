/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:04  cvs
*  Added legacy code to repository
*
*  Revision 1.3  2006/03/15 13:52:21  mmaloney
*  AuthFile in the decodes.sql package is now deprecated.
*
*  Revision 1.2  2004/09/02 12:15:25  mjmaloney
*  javadoc
*
*  Revision 1.1  2002/09/19 12:18:05  mjmaloney
*  SQL Updates
*
*
*/
package decodes.sql;

import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.FileInputStream;

/**
DECODES expects the SQL username and password to be placed in the user's
home directory in a lightly-encrypted file. This file should be protected
so that only the owner has ANY access to it.
<p>
This class provides utilities for reading the current user's authorization file.
@deprecated Use ilex.util.UserAuthFile("$HOME/.decodes.auth");
*/
@Deprecated
class AuthFile
{
	/** The file to read */
	File authFile;
	/** The database user name extracted from the file */
	String username;
	/** The database password extracted from the file */
	String password;

	/** default constructor */
	public AuthFile()
	{
		this(System.getProperty("user.home")
			+ System.getProperty("file.separator") + ".decodes.auth");
	}

	/**
	  Construct with filename.
	  @param fn the filename
	*/
	public AuthFile(String fn)
	{
		authFile = new File(fn);
	}

	private static final int[] seed = { 5, 192, 31, 65, 255, 84, 21, 9, 111 };

	/**
	  Writes the file containing the passed name and password.
	  @param nm the database user name (may not be null or zero length)
	  @param pw the database password (may not be null or zero length)
	*/
	public void write(String nm, String pw)
		throws IOException
	{
		if (nm.length() == 0 || pw.length() == 0)
			throw new IOException("Cannot have zero length username or password");
		username = nm;
		password = pw;
		byte data[] = new byte[128];
		int i=0;
		for(i=0; i<username.length(); i++)
			data[i] = (byte)username.charAt(i);
		for(; i<63; i++)
			data[i] = (byte)0;
		data[63] = (byte)pw.length();
		for(i=0; i<pw.length(); i++)
		{
			int v = (int)password.charAt(i)
				+ (int)username.charAt(i%username.length());
			v ^= seed[i%seed.length];
			data[64+i] = (byte)v;
		}
		for(; i<64; i++)
			data[i+64] = (byte)(Math.random() * 256);

		authFile.delete();
		FileOutputStream fos = new FileOutputStream(authFile);
		fos.write(data);
		fos.close();
	}

	/**
	  Read the file and decrypt the password.
	*/
	public void read()
		throws IOException
	{
		byte data[] = new byte[128];
		FileInputStream fis = new FileInputStream(authFile);
		fis.read(data);
		fis.close();

		int i=0;
		for(i=0; i<63 && data[i] != 0; i++);
		username = new String(data, 0, i);
		int pwlen = (int)data[63];
		if (pwlen > 64)
			pwlen = 64;
		for(i=0; i<pwlen; i++)
		{
			int v = data[i+64];
			v ^= seed[i%seed.length];
			v -= (int)username.charAt(i%username.length());
			data[i+64] = (byte)v;
		}
		password = new String(data,64, pwlen);
	}

	/** @return the user name after reading the file. */
	public String getUsername() { return username; }

	/** @return the decrypted password after reading the file. */
	public String getPassword() { return password; }

	/**
	  The main method is used for writing the file from the DOS or Unix
	  command line. Pass it two arguments: username password.
	  @param args command line args.
	*/
	public static void main(String args[])
		throws IOException
	{
		AuthFile authFile = new AuthFile();
		if (args.length != 2)
		{
			System.err.println("Usage: setDecodesUser <username> <password>");
			System.exit(1);
		}
		System.out.println("writing...");
		authFile.write(args[0], args[1]);
// debug only:
//		authFile.read();
//		System.out.println("Username '" + authFile.getUsername()
//			+ "', password '" + authFile.getPassword() + "'");
	}
}
