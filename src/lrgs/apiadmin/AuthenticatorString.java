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
	
	public static final String ALGO_SHA = "SHA";
	public static final String ALGO_SHA256 = "SHA-256";
	private String algorithm = ALGO_SHA;

	/**
	 * The server constructs a string from the username and time supplied
	 * by the user, and the hashed-password from the password file.
	 *
	 * @param timet the Unix time_t value
	 * @pfe The PasswordFileEntry containing the user's hashed password.
	 * @throws NoSuchAlgorithmException if SHA hashing is not supported
	 * by your Java Runtime Environment.
	 */
	public AuthenticatorString(int timet, PasswordFileEntry pfe)
		throws NoSuchAlgorithmException
	{
		astr = ByteUtil.toHexString(
			makeAuthenticator(pfe.getUsername().getBytes(), 
			pfe.getShaPassword(), timet));
	}
	
	public AuthenticatorString(int timet, PasswordFileEntry pfe, String algorithm)
		throws NoSuchAlgorithmException
	{
		this.algorithm = algorithm;
		astr = ByteUtil.toHexString(
			makeAuthenticator(pfe.getUsername().getBytes(), 
			pfe.getShaPassword(), timet, algorithm));
	}


	/**
	 * Makes authenticator with SHA
	 * @param b1
	 * @param b2
	 * @param t
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
	public static byte[] makeAuthenticator(byte b1[], byte b2[], int t)
		throws NoSuchAlgorithmException
	{
		return makeAuthenticator(b1, b2, t, ALGO_SHA);
	}
		
	/**
	 * Makes authenticator. Pass one of the constants ALGO_SHA or ALGO_SHA256
	 * @param b1 username as byte array
	 * @param b2 password hash as byte array
	 * @param t time_t
	 * @param algorithm one of the constants ALGO_SHA or ALGO_SHA256
	 * @return byte array authenticator
	 * @throws NoSuchAlgorithmException
	 */
	public static byte[] makeAuthenticator(byte b1[], byte b2[], int t, String algorithm)
		throws NoSuchAlgorithmException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		MessageDigest md = MessageDigest.getInstance(algorithm);
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

	public String getAlgorithm()
	{
		return algorithm;
	}
}
