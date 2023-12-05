package org.opendcs.odcsapi.lrgsclient;

import java.io.*;
import java.security.*;

import org.opendcs.odcsapi.errorhandling.ErrorCodes;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.util.ApiByteUtil;

/**
 * This class is used for building authenticator-strings used by the LRGS
 * gate keeper service.
 */
public class AuthenticationUtils
{
	public static final String ALGO_SHA = "SHA";
	public static final String ALGO_SHA256 = "SHA-256";

	public static byte[] makeShaPassword(String username, String password, String algorithm)
		throws WebAppException
	{
		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			MessageDigest md = MessageDigest.getInstance(algorithm);
			DigestOutputStream dos = new DigestOutputStream(baos, md);

			dos.write(username.getBytes());
			dos.write(password.getBytes());
			dos.write(username.getBytes());
			dos.write(password.getBytes());

			return dos.getMessageDigest().digest();
		}
		catch(Exception ex)
		{
			throw new WebAppException(ErrorCodes.AUTH_FAILED, 
				"Cannot construct authenticator string: " + ex);
		}
	}

	public static byte[] makeAuthenticator(int timet, String username, 
		byte[] shaPassword, String algorithm)
		throws WebAppException
	{
		// Put the time in a byte array.
		byte timeb[] = new byte[4];
		ApiByteUtil.putInt4_BigEndian(timet, timeb, 0);
		byte userb[] = username.getBytes();

		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			MessageDigest md = MessageDigest.getInstance(algorithm);
			DigestOutputStream dos = new DigestOutputStream(baos, md);
			
			dos.write(userb);
			dos.write(shaPassword);
			dos.write(timeb);
			dos.write(userb);
			dos.write(shaPassword);
			dos.write(timeb);
			return dos.getMessageDigest().digest();
		}
		catch(Exception ex)
		{
			throw new WebAppException(ErrorCodes.AUTH_FAILED, "Could not make authenticator: " + ex);
		}
	}
}
