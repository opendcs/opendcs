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
package lritdcs;

import java.io.*;
import java.util.Date;
import java.util.TimeZone;
import java.text.SimpleDateFormat;
import java.util.zip.CRC32;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import lrgs.common.DcpMsg;
import lrgs.common.DcpMsgFlag;

import ilex.util.ByteUtil;

public class LritDcsFile
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	/** Priority is 'H'=High, 'M'=Medium, or 'L'=Low
	  Should use one of the priority values defined in Constants.java.
	*/
	private char priority;

	/// 'e' or 'w', use the values defined in Constants.java
	private char spacecraft;

	/// The directory in which files are to be created.
	private File parentDir;

	/// The current file-image we are building.
	private byte fileImage[];

	/// System time when first message was added to this image.
	private long fileStartTime;

	/// Number of messages currently in the image.
	private int numMessages;

	/// Current byte-length of file-image.
	private int imageSize;

	/// fileImage length is incremented by this amount when necessary.
	private static final int ImageIncrement = 20000;

	/// Delimiter at start of each DCP message in file: 2 ASCII STX chars.
	public static final byte[] MsgDelim = new byte[]{ (byte)2, (byte)2 };

	/// Formatter to use for date/time stamp in file names:
	private SimpleDateFormat domsatDateFmt = new SimpleDateFormat("yyDDDHHmmss");
	private Date lastSave;
	private char seqLetter;

	/// Used to compute file CRCs.
	private CRC32 myCRC;
	private byte crcImage[];
	private SimpleDateFormat ctimeFmt = new SimpleDateFormat("yyDDDHHmmssSSS");


	public LritDcsFile(char priority, File parentDir, char spacecraft)
		throws InitFailedException
	{
		this.priority = priority;
		this.parentDir = parentDir;
		this.spacecraft = spacecraft;
		fileImage = new byte[ImageIncrement];
		lastSave = null;
		myCRC = new CRC32();
		crcImage = new byte[4];
		clear();
		if (!parentDir.isDirectory())
		{
			log.info("Creating output directory '{}'", parentDir.getPath());
			boolean success = false;
			Exception ex = null;
			try { success = parentDir.mkdirs(); }
			catch(Exception e) { success = false; ex = e; }
			if (!success)
			{
				String msg =
					ex != null ? ex.toString() :
						("Cannot create directory '"+parentDir.getPath()+"'");
				log.error("LRIT:{}- {}", Constants.EVT_INIT_FAILED, msg);
				throw new InitFailedException(msg, ex);
			}
		}
		domsatDateFmt.setTimeZone(TimeZone.getTimeZone("UTC"));
		ctimeFmt.setTimeZone(TimeZone.getTimeZone("UTC"));
	}


	/**
	  Returns the system-time when the first message was added to this file-
	  image.
	*/
	public long getFileStartTime()
	{
		return fileStartTime;
	}

	/// Returns the number of messages currently in this file image.
	public int getNumMessages()
	{
		return numMessages;
	}

	/// Returns the current file-image size.
	public int getImageSize()
	{
		return imageSize;
	}

	/**
	  Adds a DCP message to the end of this file's image.
	  The 'buffer' passed here contains the 37-byte DOMSAT header followed
	  by the variable length message data. This method parses the
	  final 5-bytes of the header to determine the message length,
	  which may be different than the byte-length of the passed buffer.
	  @throws BadMessageException if passed buffer is invalid or inconsistent.
	*/
	public synchronized void addMessage(DcpMsg dcpMsg)
		throws BadMessageException
	{
		byte buffer[] = dcpMsg.getData();

		// Make sure buffer is at least 37 bytes.
		if (buffer.length < 37)
			throw new BadMessageException("Message header too short: "
				+ buffer.length + " bytes.");

		// Parse message length from header
		String lenfield = new String(buffer, 32, 5);
		int msglen = 0;
		for(int i=0; i<5; i++)
		{
			msglen = msglen * 10;
			char c = lenfield.charAt(i);
			if (Character.isDigit(c))
				msglen = msglen + (int)((byte)c - (byte)'0');
			else if (!Character.isWhitespace(c))
				throw new BadMessageException("Non-digit in length field '"
					+ c + "'");
		}

		// Make sure buffer length is big enough to accommodate the message.
		if (buffer.length < msglen + 37)
			throw new BadMessageException(
				"Message data is " + buffer.length
				+ " but length field says is should be " + msglen);

		// Add delimiter to my internal file image.
		addBytes(MsgDelim, MsgDelim.length);

		// Add the 16-bit flag word.
		Date cstart = dcpMsg.getCarrierStart();
		Date cstop = dcpMsg.getCarrierStop();
		if ((dcpMsg.flagbits & DcpMsgFlag.HAS_CARRIER_TIMES) != 0
		 && (cstart == null || cstop == null))
		 	dcpMsg.flagbits &= (~DcpMsgFlag.HAS_CARRIER_TIMES);

		byte flagbytes[] = new byte[2];
		flagbytes[0] = (byte)((dcpMsg.flagbits>>8) & 0xff);
		flagbytes[1] = (byte)(dcpMsg.flagbits & 0xff);
		addBytes(flagbytes, 2);

		// Copy message header & data into internal file image.
		addBytes(buffer, 37 + msglen);

		// If this msg had carrier times, add them after the msg data
		if ((dcpMsg.flagbits & DcpMsgFlag.HAS_CARRIER_TIMES) != 0)
		{
			String ctimes = ctimeFmt.format(cstart)
				+ ' ' + ctimeFmt.format(cstop);
			addBytes(ctimes.getBytes(), ctimes.length());
		}

		log.atTrace()
		   .setMessage("Added priority {} message[{}] from platform {}, channel {}, data length={}")
		   .addArgument(priority)
		   .addArgument(numMessages)
		   .addArgument(() -> (new String(buffer, 0, 8)))
		   .addArgument(() -> (new String(buffer, 26, 3)))
		   .addArgument(msglen)
		   .log();

		numMessages++;
	}

	/// Adds bytes to the end of the image, increasing image size if necessary.
	private void addBytes(byte b[], int len)
	{
		if (imageSize + len >= fileImage.length)
		{
			int newlen = fileImage.length + ImageIncrement;
			while (newlen < imageSize + len)
				newlen += ImageIncrement;

			log.debug("Increasing image size from {} to {}", fileImage.length, newlen);

			byte newImage[] = new byte[newlen];
			for(int i=0; i<imageSize; i++)
				newImage[i] = fileImage[i];
			fileImage = newImage;
		}
		for(int i=0; i<len; i++)
			fileImage[imageSize++] = b[i];
	}

	/**
	  Clears the current image, discarding any messages currently placed there.
	*/
	public void clear()
	{
		fileStartTime = System.currentTimeMillis();
		numMessages = 0;
		imageSize = 64;
	}

	/**
	  Saves the current image to a file in the specified directory.
	  File name is constructed as p[N]-[YYDDDHHMMSS]-[Q].dcs[SC], where
	  <ul>
		<li>[N] is the priority, one of 'H', 'M', or 'L'.</li>
		<li>[YYDDDHHMMSS] is the current system time when this method is
			called. It is the file-creation-time.</li>
		<li>[Q] is an ASCII letter from A...Z representing a sequence
		    number.</li>
		<li>[SC] is the GOES spacecraft, 'e' for East, 'w' for West, or
			nothing for both.</li>
	  </ul>
	  Returns the File object to which data was saved.
	*/
	public File saveFile()
		throws LritDcsFileException
	{
		// If this file within same second as last one, increment seq letter.
		Date d = new Date();
		if (lastSave != null &&
			(d.getTime()/1000) == (lastSave.getTime()/1000))
			seqLetter = (char)((int)seqLetter + 1);
		else
			seqLetter = 'A';
		lastSave = d;

		// Construct file name.
		String filename = "p" + priority + "-"
			+ domsatDateFmt.format(d) + "-" + seqLetter + ".dcs";
		if (spacecraft != Constants.SC_Both)
			filename = filename + spacecraft;

		// Place the file name in the header & fill remainder with spaces.
		for(int i=0; i<filename.length(); i++)
			fileImage[i] = (byte)filename.charAt(i);
		for(int i=filename.length(); i<32; i++)
			fileImage[i] = (byte)' ';

		// Fill in Length field - 8 right justified digits, blank filled.
		// We add 4 to imageSize to include the eventual CRC32 at the end.
		String s = "" + (imageSize + 4);
		for(int i=0; i<s.length(); i++)
			fileImage[32+i] = (byte)s.charAt(i);
		for(int i=s.length(); i<8; i++)
			fileImage[32+i] = (byte)' ';

		// Fill in source -- hard-coded to WCDA
		s = "WCDA";
		for(int i=0; i<s.length(); i++)
			fileImage[40+i] = (byte)s.charAt(i);

		// Fill in type -- hard-coded to DCSD
		s = "DCSD";
		for(int i=0; i<s.length(); i++)
			fileImage[44+i] = (byte)s.charAt(i);

		// Fill in any expansion field entries (future)
		for(int i=48; i < 60; i++)
			fileImage[i] = (byte)' ';

		// Compute CRC32 for header & put in location 60
		myCRC.reset();
		myCRC.update(fileImage, 0, 60);
		ByteUtil.putInt4_LittleEndian((int)myCRC.getValue(), fileImage, 60);

		// Compute CRC32 from entire image contents & add to end of image.
		myCRC.reset();
		myCRC.update(fileImage, 0, imageSize);
		ByteUtil.putInt4_LittleEndian((int)myCRC.getValue(), crcImage, 0);
		addBytes(crcImage, 4);

		// Write the file to disk.
		File ret = new File(parentDir, filename);
		try
		{
			FileOutputStream fos = new FileOutputStream(ret);
			fos.write(fileImage, 0, imageSize);
			fos.close();
		}
		catch(IOException ex)
		{
			throw new LritDcsFileException("Cannot create '" + ret.getPath()
				+ "': " + ex.toString(), ex);
		}
		finally
		{
			// Clear for next image.
			clear();
		}

		// Return the File object for the caller to enqueue.
		return ret;
	}
}
