/*
*  $Id$
*/
package lrgs.ldds;

import java.io.InputStream;
import java.io.IOException;
import ilex.util.Logger;

/**
This stream reads bytes from the socket and builds LddsMessage objects.
*/
public class LddsInputStream
{
	private InputStream istrm = null;

	private static final int ReadLimit = LddsMessage.ValidHdrLength 
		+ LddsMessage.ValidMaxDataLength;

	static final byte[] validSync = { (byte)'F', (byte)'A', (byte)'F', (byte)'0' };


	/**
	  Constructor.
	  @param ins the socket input stream
	*/
	public LddsInputStream(InputStream ins)
	{
		istrm = ins;
	}

	/**
	  Block waiting for a new message. 
	  @return LddsMessage
	  @throws IOException if stream or socket problem. 
	  @throws ProtocolError if a bad message header was received.
	*/
	public LddsMessage getMessage() 
		throws IOException, ProtocolError
	{
		LddsMessage ret = readHeader();         // Block waiting for header
		ret.MsgData = new byte[ret.MsgLength];  // Allocate bytes for msg body

 		// Block waiting for body
		int done = 0;
		while( done < ret.MsgLength )
		{
			int n = istrm.read(ret.MsgData, done, ret.MsgLength-done);

			if (n <= 0)
				throw new IOException("Socket closed.");

			done += n;
		}

		return ret;
	}

	/**
	  Block waiting for complete header.
	*/
	private LddsMessage readHeader()
		throws IOException, ProtocolError
	{
		byte hdr[] = new byte[LddsMessage.ValidHdrLength];

		// Read the 4-byte sync header & error out if it doesn't match.
		int n = istrm.read(hdr, 0, 4);
		if (n < 0)
			throw new IOException("Socket closed");
		if (n != 4
		 || hdr[0] != validSync[0]
		 || hdr[1] != validSync[1]
		 || hdr[2] != validSync[2]
		 || hdr[3] != validSync[3])
			throw new ProtocolError("Could not read valid sync pattern ("
				+ n + " bytes read)");

		// Now have sync, block for rest of header.
		n = istrm.read(hdr, 4, LddsMessage.ValidHdrLength - 4);
		if (n == -1)
			throw new IOException("Socket closed");
		return new LddsMessage(hdr);
	}

	/**
	  Look ahead on the stream to see if a complete message is available,
	  and if so, return its ID. Return (char)0 if no message is available.
	  @return code message type if message is available, (char)0 if not.
	*/
	public boolean isMsgAvailable() 
		throws IOException
	{
		return istrm.available() >= LddsMessage.ValidHdrLength;

//		try 
//		{
//			if (istrm.available() < LddsMessage.ValidHdrLength)
//				return (char)0;
//
//			istrm.mark(ReadLimit);  // Mark current position in input stream.
//			LddsMessage msg = readHeader();
//
//			int avail = istrm.available();
//
//			// See if complete body is available.
//			char ret = (avail >= msg.MsgLength ? msg.MsgId : (char)0);
//
//			istrm.reset();                // Move back to marked position.
//			return ret;
//		}
//		catch (IOException ioe)
//		{
//			return (char)0;
//		}
	}

	public void close()
	{
		try { istrm.close(); }
		catch(IOException ex) { }
	}

}
