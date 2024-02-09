/*
*  $Id: UserAuthFile.java,v 1.5 2020/04/28 18:18:58 mmaloney Exp $
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
*  $Log: UserAuthFile.java,v $
*  Revision 1.5  2020/04/28 18:18:58  mmaloney
*  Added -p pwfile feature for USACE NWP
*
*  Revision 1.4  2016/08/05 14:46:06  mmaloney
*  Was using the wrong Console class.
*
*  Revision 1.3  2014/11/19 16:13:22  mmaloney
*  Added constructor taking File object.
*
*  Revision 1.2  2014/07/03 12:25:41  mmaloney
*  Bug fix: use authFile.getAbsoluteFile().getParentFile(). That way it should never be null.
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.6  2010/10/29 15:13:45  mmaloney
*  debugs
*
*  Revision 1.5  2010/08/17 17:31:22  mmaloney
*  Handle case where file was created from CWD and encoded with null parent dir.
*
*  Revision 1.4  2010/08/17 17:19:34  mmaloney
*  Temporary debugs.
*
*  Revision 1.3  2009/11/01 20:36:03  mjmaloney
*  Updated docs
*
*  Revision 1.2  2009/09/24 17:02:07  mjmaloney
*  dev
*
*  Revision 1.1  2008/04/04 18:21:10  cvs
*  Added legacy code to repository
*
*  Revision 1.7  2007/11/01 20:07:51  mmaloney
*  dev
*
*  Revision 1.6  2006/12/23 18:16:05  mmaloney
*  dev
*
*  Revision 1.5  2006/09/29 17:52:51  mmaloney
*  release prep
*
*  Revision 1.4  2006/04/22 21:02:13  mmaloney
*  Removed debugs.
*
*  Revision 1.3  2006/03/23 15:56:15  mmaloney
*  version must be un-encrypted.
*
*  Revision 1.2  2006/03/15 13:53:15  mmaloney
*  UserAuthFile now handled file version 0 (old unsecure decodes format)
*  as well as version 1 (DES encryption).
*
*  Revision 1.1  2006/03/15 13:31:12  mmaloney
*  dev
*
*/
package ilex.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.opendcs.spi.authentication.AuthSource;

import java.io.FileWriter;
import java.io.FileInputStream;
import java.io.FileReader;

/**
Daemons require the SQL username and password to be placed in the user's
home directory in an encrypted file. This class provides access to an
encrypted file in the user's home directory. This file should be protected
so that only the owner has access to it. The default file name is ".db.auth".
*/
public class UserAuthFile implements AuthSource
{
	/** The file to read */
	private File authFile;
	/** The database user name extracted from the file */
	private String username;
	/** The database password extracted from the file */
	private String password;
	/** The version of the file just read. */
	private int fileVersion;

	/** The version of this code */
	private static final int codeVersion = 1;

	private static final int[] v0seed = { 5, 192, 31, 65, 255, 84, 21, 9, 111 };

	/** Default constructor looks for .db.auth in current user's home. */
	public UserAuthFile()
	{
		this(System.getProperty("user.home") 
			+ System.getProperty("file.separator") + ".decodes.auth");
	}

	/** 
	  Construct with filename other than the default.
	  @param authFilename the filename
	*/
	public UserAuthFile(String authFilename)
	{
		this(new File(EnvExpander.expand(authFilename)));
	}
	
	public UserAuthFile(File af)
	{
		authFile = af;
		username = null;
		password = null;
		fileVersion = 0;
	}

	private static final byte[] pp = 
		{ 0x55, 0x30, 0x31, 0x65, 0x4f, 0x7e, 0x70, 0x42, 0x77, 0x51, 0x5d,
		  0x34, 0x65, 0x78, 0x3a, 0x5a, 0x33, 0x6d };

	/**
	  Writes the file containing the passed name and password.
	  @param nm the database user name (may not be null or zero length)
	  @param pw the database password (may not be null or zero length)
	*/
	public void write(String nm, String pw)
		throws IOException, AuthException
	{
		if (nm.length() == 0 || pw.length() == 0)
			throw new IOException(
				"Cannot have zero length username or password");
		username = nm;
		password = pw;

		// pre-fill data with random printable characters
		byte data[] = new byte[256];
		int i=0;
		for(i=0; i<data.length; i++)
			data[i] = (byte)(0x30 + (int)(Math.random() * 63));

		// put int version number, name length, and password length.
		data[5] = (byte)(nm.length() + 0x30);
		data[12] = (byte)(pw.length() + 0x30);

		// fill in username & password
		for(i=0; i<nm.length(); i++)
			data[i+14] = (byte)nm.charAt(i);
		for(i=0; i<pw.length(); i++)
			data[i + 14 + nm.length() + 21] = (byte)pw.charAt(i);

		// Encrypt it using the canned key plus the user name
		File p = authFile.getAbsoluteFile().getParentFile();
		String pn = (p == null ? "null" : p.getName());
		String key = new String(pp) + pn;
//System.out.println("Writing using key '" + key + "', parent file name='" + pn + "'");
		DesEncrypter de = new DesEncrypter(key);

		authFile.delete();
		FileWriter fos = new FileWriter(authFile);
		fos.write(0x30 + codeVersion);
		fos.write(de.encrypt(new String(data)));
		fos.close();
	}

	/**
	  Read the file and decrypt the password.
	*/
	public void read()
		throws IOException, AuthException
	{
		int len = (int)authFile.length();
		int cv = (len == 128 ? 0 : 1);
		FileInputStream fis = new FileInputStream(authFile);
		byte data[];
		if (cv == 0)
			data = new byte[128];
		else
		{
			cv = fis.read() - 0x30;
			data = new byte[len-1];
		}

		fis.read(data);
		fis.close();
//System.out.println("Read " + data.length + " bytes from '" 
//+ authFile.getPath() + "', cv=" + cv);

		if (cv == 0)
			decryptV0(data);
		else
			decryptV1(data);
	}

	private void decryptV1(byte data[])
		throws AuthException
	{
		File p = authFile.getAbsoluteFile().getParentFile();
		String pn = (p == null ? "null" : p.getName());
		String key = new String(pp) + pn;
//System.out.println("Decrypting using key '" + key + "', parent file name='" + pn + "'");

		DesEncrypter de = new DesEncrypter(key);

		// MJM 20100817 we noticed some strangeness in decrypting having to
		// do with the Parent File. If file is created using current-directory
		// then it will be encoded with null. So always try twice. First with
		// the actual parent directory, and then with null.
		String ct = null;
		try { ct = de.decrypt(new String(data)); }
		catch(AuthException ae1)
		{
			if (pn != "null")
			{
				pn = "null";
				key = new String(pp) + pn;
				de = new DesEncrypter(key);
//System.out.println("Trying again with pn='" + pn + "'");
				try { ct = de.decrypt(new String(data)); }
				catch(AuthException ae2)
				{
					// throw the first error
					throw ae1;
				}
			}
			else
				throw ae1;
		}

		fileVersion = (int)ct.charAt(0) - 0x30;
		int ul = (int)ct.charAt(5) - 0x30;
		int pl = (int)ct.charAt(12) - 0x30;

		username = ct.substring(14, 14+ul);
		password = ct.substring(14 + ul + 21, 14 + ul + 21 + pl);

		// depending on version, other data may be stored in the data.
	}

	private void decryptV0(byte data[])
	{
		fileVersion = 0;
		int i=0;
		for(i=0; i<63 && data[i] != 0; i++);
		username = new String(data, 0, i);
		int pwlen = (int)data[63];
		if (pwlen > 64)
			pwlen = 64;
		for(i=0; i<pwlen; i++)
		{
			int v = data[i+64];
			v ^= v0seed[i%v0seed.length];
			v -= (int)username.charAt(i%username.length());
			data[i+64] = (byte)v;
		}
		password = new String(data,64, pwlen);
	}

	/** @return the user name after reading the file. */
	public String getUsername() { return username; }

	/** @return the decrypted password after reading the file. */
	public String getPassword() { return password; }

	/** @return the file version after reading the file. */
	public int getFileVersion() { return fileVersion; }
	
	/** @return the File object being used. */
	public File getAuthFile() { return authFile; }

	/**
	  The main method is used for writing the file from the DOS or Unix
	  command line.
		java ilex.util.UserAuthFile <filename>
	  @param args command line args.
	*/
	public static void main(String args[])
		throws Exception
	{
		UserAuthFile authFile = null;
		String authFileArg = null;
		String pwFileArg = null;
		
		int n = 0;
		if (args.length > 0 && args[0].startsWith("-p"))
		{
			if (args[0].length() > 2)
			{
				pwFileArg = args[0].substring(2);
				n++;
			}
			else
			{
				pwFileArg = args[1];
				n += 2;
			}
		}

		authFileArg = n >= args.length ? null : args[n];
		boolean q = false;
		
		if (authFileArg == null)
			authFile = new UserAuthFile();
		else if (authFileArg.startsWith("-"))
		{
			q = true;
			authFileArg = authFileArg.substring(1);
			authFile = new UserAuthFile(authFileArg);
		}
		else
		{
			authFile = new UserAuthFile(authFileArg);
		}

		if (!q)
		{
			String user = null, password = null;
			
			if (pwFileArg == null)
			{
				System.out.print("User Name: ");
				user = System.console().readLine();
				System.out.print("Password: ");
				password = new String(System.console().readPassword());
			}
			else
			{
				try
				{
					BufferedReader br = new BufferedReader(new FileReader(pwFileArg));
					user = br.readLine();
					password = br.readLine();
					br.close();
				}
				catch(IOException ex)
				{
					System.err.println("Cannot open password file '" + pwFileArg + "': " + ex);
				}
			}
			System.out.println("writing to " + pwFileArg +"...");
			authFile.write(user, password);
		}
		else // query only
		{
			try { authFile.read(); }
			catch(Exception ex)
			{
				System.err.println("Error reading '" + 
					authFile.getAuthFile().getPath() + "': " + ex);
				ex.printStackTrace(System.err);
				System.exit(1);
			}
			System.out.println("Username '" + authFile.getUsername()
				+ "', password '" + authFile.getPassword() + "'"
				+ ", file version = " + authFile.getFileVersion());
		}
	}

	/**
	 * @returns Properties file with username and password fields.
	 */
	@Override
	public Properties getCredentials() {
		Properties props = new Properties();
		props.put("username",getUsername());
		props.put("password",getPassword());
		return props;
	}
}
