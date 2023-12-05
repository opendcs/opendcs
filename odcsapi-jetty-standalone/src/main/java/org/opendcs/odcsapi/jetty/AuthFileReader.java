/*
 *  Copyright 2023 OpenDCS Consortium
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opendcs.odcsapi.jetty;

import java.io.File;
import java.io.IOException;

import org.opendcs.odcsapi.start.StartException;
import org.opendcs.odcsapi.util.DesEncrypter;

import java.io.FileInputStream;


/**
Daemons require the SQL username and password to be placed in the user's
home directory in an encrypted file. This class provides access to an
encrypted file in the user's home directory. This file should be protected
so that only the owner has access to it. The default file name is ".db.auth".
*/
public class AuthFileReader
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

	/** 
	  @param path the full (expanded) path of the auth file.
	*/
	public AuthFileReader(String path)
	{
		authFile = new File(path);
		username = null;
		password = null;
		fileVersion = 0;
	}
	

	private static final byte[] pp = 
		{ 0x55, 0x30, 0x31, 0x65, 0x4f, 0x7e, 0x70, 0x42, 0x77, 0x51, 0x5d,
		  0x34, 0x65, 0x78, 0x3a, 0x5a, 0x33, 0x6d };

	/**
	  Read the file and decrypt the password.
	*/
	public void read()
		throws IOException, StartException
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
System.out.println("Read " + data.length + " bytes from '" 
+ authFile.getPath() + "', cv=" + cv);

		if (cv == 0)
			decryptV0(data);
		else
			decryptV1(data);
	}

	private void decryptV1(byte data[])
		throws StartException
	{
		File p = authFile.getAbsoluteFile().getParentFile();
		String pn = (p == null ? "null" : p.getName());
		String key = new String(pp) + pn;
System.out.println("Decrypting using key '" + key + "', parent file name='" + pn + "'");

		DesEncrypter de = new DesEncrypter(key);

		// MJM 20100817 we noticed some strangeness in decrypting having to
		// do with the Parent File. If file is created using current-directory
		// then it will be encoded with null. So always try twice. First with
		// the actual parent directory, and then with null.
		String ct = null;
		try { ct = de.decrypt(new String(data)); }
		catch(StartException ae1)
		{
			if (pn != "null")
			{
				pn = "null";
				key = new String(pp) + pn;
				de = new DesEncrypter(key);
//System.out.println("Trying again with pn='" + pn + "'");
				try { ct = de.decrypt(new String(data)); }
				catch(StartException ae2)
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
}
