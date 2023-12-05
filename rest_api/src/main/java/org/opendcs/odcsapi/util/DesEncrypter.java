/*
*  $Id: DesEncrypter.java,v 1.1 2022/11/29 15:05:13 mmaloney Exp $
*/
package org.opendcs.odcsapi.util;

import java.io.UnsupportedEncodingException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.opendcs.odcsapi.start.StartException;

import java.security.spec.KeySpec;
import java.security.spec.AlgorithmParameterSpec;

/**
Encrypts a string using a passphrase.
*/
public class DesEncrypter 
{
	private Cipher ecipher;
	private Cipher dcipher;

	// 8-byte Salt
	private byte[] salt = 
	{
		(byte)0xA9, (byte)0x9B, (byte)0xC8, (byte)0x32,
		(byte)0x56, (byte)0x35, (byte)0xE3, (byte)0x03
	};

	// Iteration count
	private int iterationCount = 19;

	/**
	 * Construct an object to handle both encryption & decryption using DES
	 * with the passed passPhrase.
	 * @param passPhrase the secret pass phrase.
	 * @throws AuthException if any errors occur in the underlying DES classes.
	 */
	public DesEncrypter(String passPhrase) 
		throws StartException
	{
		try 
		{
			// Create the key
			KeySpec keySpec = 
				new PBEKeySpec(passPhrase.toCharArray(), salt, iterationCount);
			SecretKey key = SecretKeyFactory.getInstance(
				"PBEWithMD5AndDES").generateSecret(keySpec);
			ecipher = Cipher.getInstance(key.getAlgorithm());
			dcipher = Cipher.getInstance(key.getAlgorithm());

			// Prepare the parameter to the ciphers
			AlgorithmParameterSpec paramSpec = 
				new PBEParameterSpec(salt, iterationCount);

			// Create the ciphers
			ecipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);
			dcipher.init(Cipher.DECRYPT_MODE, key, paramSpec);
		}
		catch(Exception ex)
		{
			String msg = "Cannot construct DesEncrypter: " + ex;
			throw new StartException(msg);
		}
		// Note: Exceptions that might be thrown include:
	//	java.security.InvalidAlgorithmParameterException
		//	java.security.spec.InvalidKeySpecException
		//	javax.crypto.NoSuchPaddingException
		//	java.security.NoSuchAlgorithmException
		//	java.security.InvalidKeyException
	}

	/**
	 * Encrypts a string.
	 * @param str the string to encrypt.
	 * @return BASE-64 encoding of the encrypted string.
	 */
	public String encrypt(String str) 
	{
		try 
		{
			// Encode the string into bytes using utf-8
			byte[] utf8 = str.getBytes("UTF8");

			// Encrypt
			byte[] enc = ecipher.doFinal(utf8);

			// Encode bytes to base64 to get a string
			//return new sun.misc.BASE64Encoder().encode(enc);
			return new String(Base64.encodeBase64(enc), "UTF8");
		}
		catch (BadPaddingException e) 
		{
			// Won't happen -- Only applies to decryption.
		}
		catch (IllegalBlockSizeException e) 
		{
			// Won't happen -- We're not doing block encryption.
		}
		catch (UnsupportedEncodingException e) 
		{
			// Won't happen, UTF8 is always supported.
		}
		//catch (java.io.IOException e) 
		//{
		//}
		return null;
	}

	/**
	 * Decrypts a string.
	 * @param str BASE-64 encoding of the string to decrypt.
	 * @param return the decrypted string.
	 * @throws AuthException if any error occurs in the underlying DES classes.
	 */
	public String decrypt(String str) 
		throws StartException
	{
		try 
		{
			// Decode base64 to get bytes
			//byte[] dec = new sun.misc.BASE64Decoder().decodeBuffer(str);
			byte[] dec = Base64.decodeBase64(str.getBytes("UTF8"));

			// Decrypt
			byte[] utf8 = dcipher.doFinal(dec);

			// Decode using utf-8
			return new String(utf8, "UTF8");
		}
		catch (BadPaddingException ex) 
		{
			// Means padding chars were not included properly in 'str'.
			throw new StartException("Cannot decrypt: " + ex);
		}
		catch (IllegalBlockSizeException e) 
		{
			// Won't happen, we're not doing block decryption.
		}
		catch (UnsupportedEncodingException e) 
		{
			// Won't happen -- UTF8 is always supported.
		}
		//catch (java.io.IOException e) 
		//{
		//}
		return null;
	}

	public static void main(String args[])
		throws Exception
	{
		if (args.length != 2)
		{
			System.out.println("Usage DesEncrypter string password");
			System.exit(1);
		}

		// Create encrypter/decrypter class
		DesEncrypter encrypter = new DesEncrypter(args[1]);
	
		// Encrypt
		String encrypted = encrypter.encrypt(args[0]);

		System.out.println("Encrpyted '" + args[0] + "' with key '" + args[1]
			+ "': ");
		System.out.println(encrypted);
	
		// Decrypt
		String decrypted = encrypter.decrypt(encrypted);
		System.out.println(decrypted);
	}
}

