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
*  Revision 1.4  2004/08/30 14:50:27  mjmaloney
*  Javadocs
*
*  Revision 1.3  2000/03/20 20:00:20  mike
*  dev
*
*  Revision 1.2  2000/03/17 22:44:56  mike
*  dev
*
*  Revision 1.1  2000/02/17 13:48:14  mike
*  Created prototypes - not fully implemented
*
*
*/
package ilex.util;

import java.security.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
This class is used for synchronizing files with remote systems.
Call hasChanged() to determine if the file has changed locally. It
simply checks the last modify time.
<p>
Call getDigest() to return a 4-byte-integer hash of the file contents.
A file server can use this to return the digest to clients on request.
A file client can use this to compare the local digest with the one
returned by the server.
<p>
If the digests are different, the client should download a fresh copy
of the file. It should then call getDigest() again to reset the chached
values for the next periodic check.
*/
public class FileDigest
{
	private File file;            // Represents the file being synchronized
	private int digest;           // Cached digest
	private long lastModified;    // Cached modify time to detect changes.

	/**
	* Construct a FileDigest object attached to the named file.
	* @param file the file to read
	* @param mustExist if true, digest is computed right away.
	* @throws IOException if digest could not be computed
	*/
	public FileDigest( File file, boolean mustExist ) throws IOException
	{
		this.file = file;
		lastModified = file.lastModified();
		if (mustExist)
			digest = computeDigest();
		else
			digest = -1;
	}

	/**
	* If file has changed, recompute the digest. 
	* @return the digest
	*/
	public int getDigest( )
	{
		if (hasChanged())
		{
			lastModified = file.lastModified();
			try { digest = computeDigest(); }
			catch (IOException ioe) { digest = -1; }
		}

		return digest;
	}

	/**
	* Computes and returns a 4-byte digest for this file.
	* This method should typically only be called internally. It
	* resets the internal cached digest value, and then returns it.
	* 
	* Returns integer digest of file contents or -1 if IO error attempting
	* to read the file.
	* @return  the digest
	* @throws IOException if IO error
	*/
	public int computeDigest( ) throws IOException
	{
		byte digestbytes[];
		try
		{
			FileInputStream fis = new FileInputStream(file);
			MessageDigest md = MessageDigest.getInstance("SHA");
			DigestInputStream dis = new DigestInputStream(fis, md);
			byte[] data = new byte[1024];
			int len = (int)file.length();
			for(int i=0; i < len; i += 1024)
				dis.read(data, 0, 1024);
			dis.close();
			fis.close();

			digestbytes = dis.getMessageDigest().digest();
			return ByteUtil.getInt4_BigEndian(digestbytes, 0);
		}
		catch(NoSuchAlgorithmException e)
		{
			return -1;
		}
		// IOException will propegate up the call stack.
	}

	/**
	* Check last-modify-times to determine if the local copy of this file
	* has changed.
	* @return true if file has changed.
	*/
	public boolean hasChanged( )
	{
		return file.lastModified() > lastModified;
	}
}
