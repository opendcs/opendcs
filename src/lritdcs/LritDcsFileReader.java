/*
*  $Id$
*
*  $Log$
*  Revision 1.5  2011/09/10 10:24:00  mmaloney
*  Implement EUMETSAT LRIT File Capability.
*
*  Revision 1.4  2011/02/23 20:35:37  mmaloney
*  got rid of output to stdout.
*
*  Revision 1.3  2009/10/12 15:04:51  mjmaloney
*  Added flag bytes and carrier times to LRIT File.
*
*  Revision 1.2  2009/10/09 14:52:26  mjmaloney
*  Added flag bytes and carrier times to LRIT File.
*
*  Revision 1.1  2008/04/04 18:21:16  cvs
*  Added legacy code to repository
*
*  Revision 1.5  2005/03/02 22:21:54  mjmaloney
*  update
*
*  Revision 1.4  2004/05/19 14:03:45  mjmaloney
*  dev.
*
*  Revision 1.3  2004/04/29 16:11:07  mjmaloney
*  Implemented new header fields.
*
*  Revision 1.2  2003/08/15 13:48:12  mjmaloney
*  Handle LRIT File header.
*
*  Revision 1.1  2003/08/11 23:38:11  mjmaloney
*  dev
*
*/
package lritdcs;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.zip.CRC32;


import ilex.util.*;
import ilex.cmdline.*;
import lrgs.common.DcpMsg;
import lrgs.common.DcpMsgFlag;

public class LritDcsFileReader
	implements LritFileReaderIF
{
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
		FileInputStream fis = new FileInputStream(file);
		int len = (int)file.length();
		if (len < 64)
			throw new IOException("Invalid LRIT DCS file length=" + len);
		image = new byte[len];
		fis.read(image);
		fis.close();
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

			//System.out.println("Valid LRIT Header, total header length = "
			//	+ fileStartOffset);
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
			Logger.instance().log(Logger.E_WARNING,
				"LRIT File '" + origFileName() + "' invalid length field '"
				+ s + "'");
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
		return fileCRC == (int)crc32.getValue();
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
			while((msg = getNextMsg()) != null)
			{
				System.out.println("\n------\n" + msg.toString());
				System.out.println("FLAGS=0x" + Integer.toHexString(msg.flagbits));
				if ((msg.flagbits & DcpMsgFlag.HAS_CARRIER_TIMES) != 0)
					System.out.println("Carrier: "
						+ ctimeFmt.format(msg.getCarrierStart()) + " "
						+ ctimeFmt.format(msg.getCarrierStop()));
			}
		}
		catch(IOException ex)
		{
			System.err.println(ex);
			ex.printStackTrace(System.err);
		}
	}

	@Override
	public DcpMsg getNextMsg()
		throws IOException
	{
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
			Logger.instance().debug1("LritReader " + skipped + " characters skipped.");
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
			Logger.instance().info("Invalid message length field '"
				+ (new String(image, currentOffset+DcpMsg.IDX_DATALENGTH, 5))
				+ "' -- skipping to next message start.");
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
				Logger.instance().warning("Bad carrier times at offset "
					+ (currentOffset-29) + " "
					+ new String(image, currentOffset-29, 29));
			}
		}
		return ret;
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
			System.out.println("Original File Name: '" + ldfr.origFileName() 
				+ "'");
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
