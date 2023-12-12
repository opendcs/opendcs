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

/*
*  $Id: LddsInputStream.java,v 1.1 2023/05/15 18:33:56 mmaloney Exp $
*/
package org.opendcs.odcsapi.lrgsclient;

import java.io.InputStream;
import java.io.IOException;

/**
This stream reads bytes from the socket and builds LddsMessage objects.
*/
public class LddsInputStream
{
	private InputStream istrm = null;

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
	  @throws DdsProtocolError if a bad message header was received.
	*/
	public LddsMessage getMessage() 
		throws IOException, DdsProtocolError
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
		throws IOException, DdsProtocolError
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
			throw new DdsProtocolError("Could not read valid sync pattern ("
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
	}

	public void close()
	{
		try { istrm.close(); }
		catch(IOException ex) { }
	}

}
