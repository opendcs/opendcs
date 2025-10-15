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
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.zip.CRC32;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.cmdline.*;
import ilex.util.ByteUtil;
import ilex.util.CRC16;
import lrgs.common.DcpAddress;
import lrgs.common.DcpMsg;
import lrgs.common.DcpMsgFlag;

/**
 * @deprecated Comment above stated abandoned. Class is still referenced in several places.
 * 			   May be worth keeping around for posterity; however, I don't think any countries
 * 			   satellites use this format anymore.
 */
@Deprecated
public class LritDcsFileReader implements LritFileReaderIF
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	boolean lritHeader;
	byte image[];
	File file;
	int currentOffset;
	int fileStartOffset;
	String origFileName;
	private static final int PRIMARY_TYPE = 0;
	private static final int PRIMARY_LENGTH = 16;
	private static final int DCS_FILE_TYPE = 130;
	private SimpleDateFormat ctimeFmt = new SimpleDateFormat("yyDDDHHmmssSSS");
	private boolean hritFileType = true;
	private DcpMsg dummyMsg = new DcpMsg();

	public LritDcsFileReader(String filename, boolean lritHeader)
	{
		this.lritHeader = lritHeader;
		file = new File(filename);
		currentOffset = 64;
		ctimeFmt.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	@Override
	public void load()
		throws IOException, BadMessageException
	{
		try (FileInputStream fis = new FileInputStream(file))
		{
			int len = (int)file.length();
			if (len < 64)
				throw new IOException("Invalid LRIT DCS file length=" + len);
			image = new byte[len];
			fis.read(image);
		}
		if (lritHeader)
		{
			int htype = (int)image[0];
			int hlen = ByteUtil.getInt2_BigEndian(image, 1);
			int ftype = (int)image[3] & 0xff;
			if (htype != PRIMARY_TYPE || hlen != PRIMARY_LENGTH)
			{
				throw new BadMessageException(
					"Invalid LRIT File Header: headerType=" + htype
					+ ", headerLen=" + hlen);
			}
			else if (ftype != DCS_FILE_TYPE)
			{
				throw new BadMessageException(
					"LRIT File Header with non-DCS file type ("
					+ DCS_FILE_TYPE + "), fileType='" + ftype + "'");
			}
			fileStartOffset = ByteUtil.getInt4_BigEndian(image, 4);

		}
		else
			fileStartOffset = 0;
	}

	@Override
	public boolean checkHeader()
	{
		if (image.length-fileStartOffset < 64)
			return false;

		CRC32 crc32 = new CRC32();
		crc32.reset();
		crc32.update(image, fileStartOffset, 60);

		hritFileType = this.getType().equals("DCSH");
		int fileCRC = ByteUtil.getInt4_LittleEndian(image, fileStartOffset+60);
		return fileCRC == (int)crc32.getValue();
	}

	@Override
	public boolean checkLength()
	{
		if (image.length-fileStartOffset < 64)
			return false;
		String s = new String(image, fileStartOffset+32, 8).trim();
		try { return Long.parseLong(s) == (image.length - fileStartOffset); }
		catch (NumberFormatException ex)
		{
			log.atWarn().setCause(ex).log("LRIT File '{}' invalid length field '{}'", origFileName, s);
			return false;
		}
	}

	@Override
	public boolean checkCRC()
	{
		CRC32 crc32 = new CRC32();
		crc32.reset();

		crc32.update(image, fileStartOffset,
			image.length - fileStartOffset - 4);

		int fileCRC = ByteUtil.getInt4_LittleEndian(image, image.length-4);
		int computedCRC = (int)crc32.getValue();
		return fileCRC == computedCRC;
	}

	public String origFileName()
	{
		int i = 0;
		for(i=0; i<32 && image[i + fileStartOffset] != (byte)' '; i++);
		return new String(image, fileStartOffset, i);
	}

	/// Returns the source identifier in the file.
	public String getSource()
	{
		String s = new String(image, fileStartOffset+40, 4);
		return s;
	}

	/// Returns the type identifier in the file.
	public String getType()
	{
		String s = new String(image, fileStartOffset+44, 4);
		return s;
	}

	public void listMessages(PrintStream out)
	{
		currentOffset = fileStartOffset + 64;
		DcpMsg msg;
		try
		{
			out.println("\n------");
			while((msg = getNextMsg()) != null)
			{
				out.println("FLAGS=0x" + Integer.toHexString(msg.flagbits)
					+ ", seq=" + msg.getSequenceNum()
					+ ", baud=" + msg.getBaud());
				if ((msg.flagbits & DcpMsgFlag.HAS_CARRIER_TIMES) != 0)
					out.println("Carrier: "
						+ ctimeFmt.format(msg.getCarrierStart()) + " "
						+ ctimeFmt.format(msg.getCarrierStop()));

				out.println(msg.toString());
				out.println("\n------");
			}
		}
		catch(IOException ex)
		{
			log.atError().setCause(ex).log("Unable to list messages.");
		}
	}

	@Override
	public DcpMsg getNextMsg()
		throws IOException
	{
		if (hritFileType)
		{
			DcpMsg ret;
			while ((ret = getNextMsgHrit()) != null)
				if (ret != dummyMsg)
					return ret;
			return null;
		}

		int skipped = 0;
		if (currentOffset >= image.length - 4)
			return null;

		while(currentOffset < image.length - 2)
		{
			if (image[currentOffset] == (byte)2
			 && image[currentOffset+1] == (byte)2)
			{
				currentOffset += 2;
				break;
			}
			currentOffset++;
			skipped++;
		}
		if (skipped > 0)
		{
			log.debug("LritReader {} characters skipped.", skipped);
		}

		// There has to be at least 39 bytes: 2 flag bytes+37 byte DOMSAT header.
		if (currentOffset >= image.length-39)
			return null;

		// 16-bit flags word.
		int flags = ((image[currentOffset++]&0xff) << 8)
			+ (image[currentOffset++]&0xff);

		int msgDataLength = -1;
		try
		{
			msgDataLength =
			   ByteUtil.parseInt(image, currentOffset+DcpMsg.IDX_DATALENGTH, 5);
		}
		catch(NumberFormatException ex)
		{
			log.atInfo()
			   .setCause(ex)
			   .log("Invalid message length field '{}' -- skipping to next message start.",
			   		(new String(image, currentOffset+DcpMsg.IDX_DATALENGTH, 5)));
			return getNextMsg();
		}
		DcpMsg ret = new DcpMsg(image, 37 + msgDataLength, currentOffset);
		ret.flagbits = flags;
		currentOffset += (37 + msgDataLength);
		if ((flags & DcpMsgFlag.HAS_CARRIER_TIMES) != 0)
		{
			String start = new String(image, currentOffset, 14);
			currentOffset += 15; // skip space between start & end
			String end = new String(image, currentOffset, 14);
			currentOffset += 14;
			try
			{
				ret.setCarrierStart(ctimeFmt.parse(start));
				ret.setCarrierStop(ctimeFmt.parse(end));
			}
			catch(Exception ex)
			{
				log.atWarn()
				   .setCause(ex)
				   .log("Bad carrier times at offset {} {}",
						(currentOffset-29), new String(image, currentOffset-29, 29));
			}
		}
		return ret;
	}

	/**
	 * Get next messge in HRIT-Format file, as defined by UCOM HRIT DCS File Format doc.
	 *
	 * @return null if EOF, dummyMsg to ignore, good message if success
	 */
	public DcpMsg getNextMsgHrit()
	{
		// Smallest possible block is length 8 with sequence num.
		if (currentOffset >= image.length - 8)
			return null;

		DcpMsg ret = dummyMsg;
		int blkId = image[currentOffset];
		int blkLen = ByteUtil.getInt2_LittleEndian(image, currentOffset+1);

		CRC16 crc16 = new CRC16();
		crc16.reset();
		crc16.update(image, currentOffset, blkLen-2);

		int crcInFile = ByteUtil.getInt2_LittleEndian(image, currentOffset + blkLen-2);
		int crcComputed = crc16.getCRC();

		int seqNum =
				 ((int)image[currentOffset+3] & 0xff)
			  + (((int)image[currentOffset+4] & 0xff) << 8)
			  + (((int)image[currentOffset+5] & 0xff) << 16);

		int blkStart = currentOffset;
		currentOffset += blkLen;

		if (blkId == 1) // DCP Message Block
		{
			// 33 byte header starts at offset 6 after seqnum
			int msgFlags = image[blkStart+6] & 0xff;
			int armFlags = image[blkStart+7] & 0xff;
			long dcpAddr = ByteUtil.getInt4_LittleEndian(image, blkStart+8);
			// 7 byte BCD carrier start - little endian
			Date carrierStart = bcd7littleEndian(image, blkStart + 12);

			// 7 byte BCD msg end - little endian
			Date msgEnd = bcd7littleEndian(image, blkStart + 19);

			// 2 byte binary sig strength x 10, mask low 10 bits
			int sigx10 = ByteUtil.getInt2_LittleEndian(image, blkStart + 26) & 0x3ff;

			// 2 byte binary freq offset x 10, high 2 bits not used, do sign extension
			int freqOffx10 = ByteUtil.getInt2_LittleEndian(image, blkStart + 28);
			freqOffx10 &= 0x3FFF;
			if ((freqOffx10 & 0x2000) != 0) // sign bit set?
				freqOffx10 |= 0xFFFFC000;   // sign extend


			// 2 byte binary phase noise x 100, and mod index
			int noisex100 = ByteUtil.getInt2_LittleEndian(image, blkStart + 30);
			char modIndex = 'N';
			int modIndexCode = (noisex100 & 0xC000) >> 14;
			switch(modIndexCode)
			{
			case 0: modIndex = 'U'; break;
			case 1: modIndex = 'N'; break;
			case 2: modIndex = 'H'; break;
			case 3: modIndex = 'L'; break;
			}
			noisex100 &= 0xFFF;

			// 1 byte binary good phase x 2
			double goodPhasePct = (image[blkStart + 32] & 0xff) / 2.0;

			// 2 byte binary chan/sc
			int chan = ByteUtil.getInt2_LittleEndian(image, blkStart + 33);
			char sc = 'U';
			switch((chan >> 12) & 0xf)
			{
			case 0: sc = 'U'; break;
			case 1: sc = 'E'; break;
			case 2: sc = 'W'; break;
			case 3: sc = 'C'; break;
			}
			chan &= 0x3ff;

			// 2 char source code
			byte sourceCode[] = new byte[2];
			sourceCode[0] = image[blkStart + 35];
			sourceCode[1] = image[blkStart + 36];

			// Ignore bytes 37 & 38 = secondary source code
			// 2 char secondary source (TBD)

			// Now build a standard 38-byte GOES header
			SimpleDateFormat sdf = new SimpleDateFormat("yyDDDHHmmss");
			sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
			NumberFormat numfmt = NumberFormat.getIntegerInstance();
			numfmt.setGroupingUsed(false);
			numfmt.setMinimumIntegerDigits(2);
			numfmt.setMaximumIntegerDigits(2);

			try
			{
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				DcpAddress addr = new DcpAddress(dcpAddr);
				baos.write(addr.toString().getBytes());


				baos.write(sdf.format(carrierStart).getBytes());
				baos.write((msgFlags & 0x10) != 0 ? (byte)'?' : (byte)'G');
				baos.write(numfmt.format(sigx10/10).getBytes());

				char sign = freqOffx10 < 0 ? '-' : '+';
				baos.write((byte)sign);
				int n = freqOffx10 / 500;
				if (n < 0) n = -n;
				byte c = n >= 10 ? (byte)'A' : (byte)('0' + n);
				baos.write(c);

				baos.write((byte)modIndex);
				byte q = goodPhasePct >= 85. ? (byte)'N'
					   : goodPhasePct >= 75. ? (byte)'F' : (byte)'P';
				baos.write((byte)q);

				numfmt.setMinimumIntegerDigits(3);
				numfmt.setMaximumIntegerDigits(3);
				baos.write(numfmt.format(chan).getBytes());
				baos.write((byte)sc);
				baos.write(sourceCode);
				numfmt.setMinimumIntegerDigits(5);
				numfmt.setMaximumIntegerDigits(5);
				int msglen = blkLen - 41;
				baos.write(numfmt.format(msglen).getBytes());

				// Copy message data and strip parity.

				for(int idx = 0; idx < msglen; idx++)
				{
					int b = image[blkStart+39+idx] & 0x7f;
					baos.write(b);
				}

				byte msgdata[] = baos.toByteArray();

				ret = new DcpMsg(msgdata, msglen+37, 0);
				ret.setCarrierStart(carrierStart);
				ret.setXmitTime(carrierStart);
				ret.setCarrierStop(msgEnd);
				int flags = DcpMsgFlag.MSG_PRESENT | DcpMsgFlag.SRC_LRIT
					| DcpMsgFlag.MSG_TYPE_GOES | DcpMsgFlag.HAS_CARRIER_TIMES;

				int x = msgFlags & 0x7;
				flags |= (x == 1 ? DcpMsgFlag.BAUD_100
						: x == 2 ? DcpMsgFlag.BAUD_300
						: x == 3 ? DcpMsgFlag.BAUD_1200 : DcpMsgFlag.BAUD_UNKNOWN);
				if ((msgFlags & 8) != 0)
					flags |= DcpMsgFlag.PLATFORM_TYPE_CS2;
				if ((msgFlags & 0x20) != 0)
					flags |= DcpMsgFlag.NO_EOT;
				if ((armFlags & 0x01) != 0)
					flags |= DcpMsgFlag.ADDR_CORRECTED;
				if ((armFlags & 0x02) != 0)
					flags |= DcpMsgFlag.ARM_UNCORRECTABLE_ADDR;
				if ((armFlags & 0x04) != 0)
					flags |= DcpMsgFlag.ARM_ADDR_NOT_IN_PDT;
				if ((armFlags & 0x08) != 0)
					flags |= DcpMsgFlag.ARM_PDT_INCOMPLETE;
				if ((armFlags & 0x10) != 0)
					flags |= DcpMsgFlag.ARM_TIMING_ERROR;
				if ((armFlags & 0x20) != 0)
					flags |= DcpMsgFlag.ARM_UNEXPECTED_MSG;
				if ((armFlags & 0x40) != 0)
					flags |= DcpMsgFlag.ARM_WRONG_CHANNEL;

				ret.setFlagbits(flags);
				ret.setSequenceNum(seqNum);
				ret.setGoesFreqOffset(freqOffx10 / 10.);
				ret.setGoesSignalStrength(sigx10 / 10.);
				ret.setGoesGoodPhasePct(goodPhasePct);
				baos.close();
			}
			catch (IOException ex)
			{
				log.atWarn().setCause(ex).log("Error writing to byte array.");
			}

		}
		else if (blkId == 2) // Missing Message Block
		{
			log.trace("Skipping MISSING MESSAGE block.");
			//TODO Future: Handle missing message blocks.
		}
		else
		{
			log.warn("Invalid block type {}, should be 1 for msg or 2 for MM.", blkId);
		}

		return ret;
	}

	private Date bcd7littleEndian(byte img[], int off)
	{
		int yy = ((img[off+6] & 0xf0)>>4) * 10
			   +  (img[off+6] & 0x0f);
		int ddd = ((img[off+5] & 0xf0)>>4) * 100
				+  (img[off+5] & 0x0f)     *  10
				+ ((img[off+4] & 0xf0)>>4);
		int hh  = ((img[off+4] & 0x0f))    * 10
				+ ((img[off+3] & 0xf0)>>4);
		int mm  = ((img[off+3] & 0x0f))    * 10
				+ ((img[off+2] & 0xf0)>>4);
		int ss  = ((img[off+2] & 0x0f))    * 10
				+ ((img[off+1] & 0xf0)>>4);
		int ms  = ((img[off+1] & 0x0f)     * 100)
				+ ((img[off  ] & 0xf0)>>4) *  10
				+  (img[off  ] & 0x0f);
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		cal.set(Calendar.YEAR, 2000 + yy);
		cal.set(Calendar.DAY_OF_YEAR, ddd);
		cal.set(Calendar.HOUR_OF_DAY, hh);
		cal.set(Calendar.MINUTE, mm);
		cal.set(Calendar.SECOND, ss);
		cal.set(Calendar.MILLISECOND, ms);
		return cal.getTime();


	}



	private static ApplicationSettings cmdLineArgs = new ApplicationSettings();
	static BooleanToken lritHeaderArg = new BooleanToken(
		"h", "Check LRIT Headers", "", TokenOptions.optSwitch, false);
	static StringToken filesArg = new StringToken("", "Input Files", "",
		TokenOptions.optArgument|TokenOptions.optMultiple
			|TokenOptions.optRequired, "");
	static
	{
		cmdLineArgs.addToken(lritHeaderArg);
		cmdLineArgs.addToken(filesArg);
	}

	public static void main(String args[])
	{
		cmdLineArgs.parseArgs(args);

		if (args.length == 0)
		{
			System.out.println("Usage: <progname> [files...]");
			System.exit(1);
		}

		for(int i=0; i<filesArg.NumberOfValues(); i++)
		{
			String fname = filesArg.getValue(i);

			LritDcsFileReader ldfr =
				new LritDcsFileReader(fname, lritHeaderArg.getValue());
			try { ldfr.load(); }
			catch(IOException ex)
			{
				System.err.println("Cannot load '" + args[i] + "': " + ex);
				continue;
			}
			catch(BadMessageException ex)
			{
				System.err.println("Bad LRIT Header in '"
					+ args[i] + "': " + ex);
				continue;
			}

			if (ldfr.checkHeader() == false)
				System.out.println("Header check failed.\n");
			else if (ldfr.checkLength() == false)
				System.out.println("Length check failed.\n");
			else if (ldfr.checkCRC() == false)
				System.out.println("CRC check failed.\n");
			System.out.println("Original File Name: '" + ldfr.origFileName() + "'");
			System.out.println("\tSource = '" + ldfr.getSource() + "'");
			System.out.println("\tType = '" + ldfr.getType() + "'");
			ldfr.listMessages(System.out);
		}
	}

	@Override
	public void close()
	{
	}
}