/*
* Where Applicable, Copyright 2026 OpenDCS Consortium and/or its contributors
* 
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software 
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations 
* under the License.
*/
package ilex.util;

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Properties;

import org.opendcs.spi.authentication.AuthSource;

import decodes.util.CmdLineArgs;
import decodes.util.DecodesSettings;
import ilex.cmdline.BooleanToken;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;

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

	/** Used to pad the encrypted file so its structure is not obvious. */
	private static final SecureRandom secureRandom = new SecureRandom();

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
			data[i] = (byte)(0x30 + secureRandom.nextInt(63));

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
		final CmdLineArgs cla = new CmdLineArgs(false, "$DCSTOOL_USERDIR/password-update.log");
		final BooleanToken showPassword = new BooleanToken("s", "Show user name and password", "", TokenOptions.optSwitch, false);
		final StringToken authFileName = new StringToken("f", "auth file name", "", TokenOptions.optArgument, null);
		final BooleanToken overwrite = new BooleanToken("y", "Assert that the file should be overwritten.", "", TokenOptions.optSwitch, false);
		cla.addToken(showPassword);
		cla.addToken(authFileName);

		cla.parseArgs(args);

		if (cla.getProfileSet() && authFileName.getValue() != null)
		{
			System.err.println("Please specify either the auth file name OR the profile. Not both.");
			System.exit(1);
		}

		File authFile = null;
		if (cla.getProfileSet())
		{
			DecodesSettings settings = DecodesSettings.fromProfile(cla.getProfile());
			authFile = new File(EnvExpander.expand(settings.DbAuthFile));
		}
		else if (authFileName.getValue() != null )
		{
			authFile = new File(EnvExpander.expand(authFileName.getValue()));
		}

		if (authFile == null)
		{
			System.err.println("Please specify either the auth file name or the profile.");
			System.exit(1);
		}
		else
		{
			if (showPassword.getValue() && authFile.exists())
			{
				UserAuthFile userAuthFile = new UserAuthFile(authFile);
				try
				{
					userAuthFile.read();
					System.out.println("Username '" + userAuthFile.getUsername()
					+ "', password '" + userAuthFile.getPassword() + "'"
					+ ", file version = " + userAuthFile.getFileVersion());
				}
				catch(Exception ex)
				{
					System.err.println(String.format("Error reading '%s'", userAuthFile.getAuthFile().getAbsolutePath()));
					ex.printStackTrace(System.err);
					System.exit(3);
				}
			}
			else if (showPassword.getValue() && !authFile.exists())
			{
				System.err.println(String.format("No auth file at '%s'. Please write initial data first.", authFile.getAbsolutePath()));
				System.exit(2);
			}
			else
			{
				System.out.println(String.format("Creating or Updating: %s", authFile.getAbsolutePath()));
				Console console = System.console();
				if (authFile.exists() && !overwrite.getValue())
				{
					final String response = console.readLine("Overwrite existing file? (y/N)");
					if (!response.toLowerCase().startsWith("y"))
					{
						System.err.println("Not overwriting existing file.");
						System.exit(0);
					}
				}
				UserAuthFile userAuthFile = new UserAuthFile(authFile);
				final String userName = console.readLine("Please enter a username: ");
				boolean match = true;
				String password;
				do
				{
					if(!match)
					{
						console.writer().println("Passwords did not match, try again.");
					}
					char[] pw_chars =  console.readPassword("Please provide a password:");
					char[] pw2_chars = console.readPassword("Please repeat the password:");
					String pw = new String(pw_chars);
					String pw2 = new String(pw2_chars);
					password = pw;
					match = pw.equals(pw2);
				}
				while (!match);
				userAuthFile.write(userName, password);
			}
		}
	}

	/**
	 * @returns Properties file with username and password fields.
	 */
	@Override
	public Properties getCredentials()
	{
		Properties props = new Properties();
		props.put("username",getUsername());
		props.put("password",getPassword());
		return props;
	}

	@Override
	public boolean canWrite()
	{
		return true;
	}
}
