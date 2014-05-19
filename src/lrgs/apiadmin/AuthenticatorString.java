/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:11  cvs
*  Added legacy code to repository
*
*  Revision 1.4  2005/07/08 20:03:29  mjmaloney
*  dev
*
*  Revision 1.3  2004/08/30 17:10:13  mjmaloney
*  Javadoc
*
*  Revision 1.2  2002/05/18 20:01:22  mjmaloney
*  Added capability to add/remove secure DDS users. This involved the
*  implementation of a session key, unique to each admin session.
*
*  Revision 1.1  2000/03/13 16:46:57  mike
*  dev
*
*
*/
package lrgs.apiadmin;

import ilex.util.ByteUtil;
import ilex.util.PasswordFileEntry;
import ilex.util.AuthException;
import java.io.*;
import java.security.*;

/**
 * This class is used for building authenticator-strings used by the LRGS
 * gate keeper service.
 */
public class AuthenticatorString
{
	private String astr;

	/**
	 * The client constructs a string from the username, time, and password
	 * entered by the user.
	 *
	 * @param username the user name
	 * @param timet the Unix time_t value
	 * @param passwd the password
	 *
	 * @throws NoSuchAlgorithmException if SHA hashing is not supported
	 * by your Java Runtime Environment.
	 * @throws AuthException if username is blank.
	 */
	public AuthenticatorString(String username, int timet, String passwd)
		throws NoSuchAlgorithmException, AuthException
	{
		this(timet, new PasswordFileEntry(username, passwd));
	}

	/**
	 * The server constructs a string from the username and time supplied
	 * by the user, and the hashed-password from the password file.
	 *
	 * @throws NoSuchAlgorithmException if SHA hashing is not supported
	 * by your Java Runtime Environment.
	 */
	public AuthenticatorString(int timet, PasswordFileEntry pfe)
		throws NoSuchAlgorithmException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		MessageDigest md = MessageDigest.getInstance("SHA");
		DigestOutputStream dos = new DigestOutputStream(baos, md);

		astr = ByteUtil.toHexString(
			makeAuthenticator(pfe.getUsername().getBytes(), 
			pfe.getShaPassword(), timet));
	}

	public static byte[] makeAuthenticator(byte b1[], byte b2[], int t)
		throws NoSuchAlgorithmException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		MessageDigest md = MessageDigest.getInstance("SHA");
		DigestOutputStream dos = new DigestOutputStream(baos, md);

		// Put the time in a byte array.
		byte timeb[] = new byte[4];
		ByteUtil.putInt4_BigEndian(t, timeb, 0);

		try
		{
			dos.write(b1);
			dos.write(b2);
			dos.write(timeb);
			dos.write(b1);
			dos.write(b2);
			dos.write(timeb);
		}
		catch(IOException e) // shouldn't happen
		{
		}

		return dos.getMessageDigest().digest();
	}

	/**
	 * @return the authenticator string that was constructed from the arguments.
	 */
	public String getString()
	{
		return astr;
	}
}
