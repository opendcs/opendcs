/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
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
package lritdcs.recv;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.EOFException;
import java.util.Date;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import lrgs.common.DcpMsg;

/**
IO Methods for reading & writing periodic files contining DCP messages.
*/
public class MsgFile
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	/** Used for the I/O */
	private RandomAccessFile raf;
	private byte buf[];
	private File file;

	private static final byte MSG_STARTPATTERN[] =
		{ (byte)0x1f, (byte)0x2e, (byte)0x3d, (byte)0x4c} ;

	public MsgFile(File file, boolean writable)
		throws FileNotFoundException
	{
		this.file = file;
		log.debug("Opening '{}'", file.getPath());

		raf = new RandomAccessFile(file, writable ? "rw" : "r");
		buf = new byte[4];
	}

	/**
	 * Archives a message at the end of the file and returns the location.
	 * The header is:
	 *	startpattern (4 bytes)
	 *	flagbits (int)
	 *	recvtime (int) (aka 'drot' time.)
	 *	sequenceNum (int)
	 *	data-length (int)
	 *	data (variable length byte array)
	 *
	 * @param msg the DcpMsg to archive.
	 * @return byte location that message was saved at.
	 */
	public synchronized long archive( DcpMsg msg)
		throws IOException
	{
		long offset = raf.length();
		raf.seek(offset);
		raf.write(MSG_STARTPATTERN);
		raf.writeInt(msg.flagbits);
		raf.writeInt((int)(msg.getLocalReceiveTime().getTime()/1000L));
		raf.writeInt(msg.getSequenceNum());
		int len = msg.getMsgLength();
		raf.writeInt(len);
		if (len > 0)
			raf.write(msg.getData());

		return offset;
	}

	public void close( )
	{
		try { raf.close(); }
		catch(Exception ex) {}
	}

	/**
	 * Reads a message from the file at a specific location.
	 * If the location does not start with a valid start-pattern, this
	 * method will slide forward until it finds one.
	 */
	public synchronized DcpMsg readMsg( long loc )
		throws EOFException, IOException
	{
		long fileLen = raf.length();
		int n = 0;
		boolean startPatFound = false;
		raf.seek(loc);
		int slide = 0;
		while(! startPatFound )
		{
			if ((fileLen-loc) < (20-n))
				throw new EOFException("EOF-Header");
			raf.read(buf, n, 4-n);
			loc += (4-n);
			if (matchStartPattern(buf))
				startPatFound = true;
			else
			{
				int x = shift(buf);
				n = 4 - x;
				slide += x;
			}
		}
		if (slide > 0)
		{
			log.warn("Skipped {} bytes in MsgFile '{}', loc={}", slide, file.getPath(), loc);
		}
		DcpMsg msg = new DcpMsg();
		msg.flagbits = raf.readInt();
		msg.setLocalReceiveTime(new Date(raf.readInt()*1000L));
		msg.setSequenceNum(raf.readInt());
		int len = raf.readInt();
		loc += 16;
		if (fileLen - loc < len)
			throw new EOFException("EOF-Data");
		byte msgdata[] = new byte[len];
		raf.read(msgdata);
		msg.setData(msgdata);

		return msg;
	}

	/**
	 * Returns the current location in the file.
	 * If reading a file sequentially, call this method after readMsg to
	 * get the first byte position after the file just read.
	 * @return current location in message file.
	 */
	public long getLocation()
	{
		try { return raf.getFilePointer(); }
		catch(IOException ex) { return 0L; }
	}

	private boolean matchStartPattern(byte buf[])
	{
		return buf[0] == MSG_STARTPATTERN[0]
		 && buf[1] == MSG_STARTPATTERN[1]
		 && buf[2] == MSG_STARTPATTERN[2]
		 && buf[3] == MSG_STARTPATTERN[3];
	}

	private int shift(byte buf[])
	{
		if (buf[1] == MSG_STARTPATTERN[0]
		 && buf[2] == MSG_STARTPATTERN[1]
		 && buf[3] == MSG_STARTPATTERN[2])
		{
			buf[0] = buf[1];
			buf[1] = buf[2];
			buf[2] = buf[3];
			return 1;
		}
		else if (buf[2] == MSG_STARTPATTERN[0]
		      && buf[3] == MSG_STARTPATTERN[1])
		{
			buf[0] = buf[2];
			buf[1] = buf[3];
			return 2;
		}
		else if (buf[3] == MSG_STARTPATTERN[0])
		{
			buf[0] = buf[3];
			return 3;
		}
		else
			return 4;
	}
}