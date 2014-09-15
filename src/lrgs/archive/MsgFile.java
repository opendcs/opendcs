/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of this source
*  code may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*/
package lrgs.archive;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.EOFException;
import java.util.Date;

import lrgs.common.DcpAddress;
import lrgs.common.DcpMsg;
import lrgs.common.DcpMsgFlag;
import ilex.util.ArrayUtil;
import ilex.util.ByteUtil;
import ilex.util.Logger;
import ilex.util.TextUtil;
import ilex.xml.XmlOutputStream;
import lrgs.ldds.ExtBlockXmlParser;
import lrgs.ldds.ProtocolError;

/**
IO Methods for reading & writing periodic files contining DCP messages.
*/
public class MsgFile
{
	/** Used for the I/O */
	private RandomAccessFile raf;
	private byte buf[];
	private File file;
	/** True if this is a Version 6 (or later) Index File */
	private int fileVersion = 5;

	/** Size of header on each message */
	private int headerSize;

	private static final byte MSG_STARTPATTERN[] = 
		{ (byte)0x1f, (byte)0x2e, (byte)0x3d, (byte)0x4c} ;
	
	/** For v7 files, used to parse & write XML blocks */
	private ExtBlockXmlParser xmlParser = null;
	private ByteArrayOutputStream baos = null;
	private XmlOutputStream xos = null;
	public static final byte[] msgStartTag = 
		("<" + ExtBlockXmlParser.DcpMsgElem).getBytes();
	public static final byte[] msgEndTag = 
		("</" + ExtBlockXmlParser.DcpMsgElem + ">").getBytes();
	public static final byte[] flagStart = 
		(" " + ExtBlockXmlParser.flagsAttr + "=\"").getBytes();
	public static final byte[] domSeqStart = 
		("<" + ExtBlockXmlParser.DomsatSeqElem + ">").getBytes();
	public static final byte[] domTimeStart = 
		("<" + ExtBlockXmlParser.DomsatTimeElem + ">").getBytes();

	private byte[] xmlMsgBuf = null;
	
	public MsgFile(File file, boolean writable)
		throws FileNotFoundException
	{
		this.file = file;
		String fn = file.getName();
		
		if (TextUtil.startsWithIgnoreCase(fn, "archv"))
		{
			fileVersion = 7;
			xmlParser = new ExtBlockXmlParser(0);
			baos = new ByteArrayOutputStream(DcpMsg.MAX_DATA_LENGTH);
			xos = new XmlOutputStream(baos, ExtBlockXmlParser.DcpMsgElem);
			xmlMsgBuf = new byte[DcpMsg.MAX_DATA_LENGTH];
		}
		else if (TextUtil.startsWithIgnoreCase(fn, "arch"))
			fileVersion = 6;
		else
			fileVersion = 5;

		headerSize = (fileVersion == 6) ? 84 : (fileVersion == 5) ? 20 : 0;
		Logger.instance().debug1("Opening '" + file.getPath() 
			+ "' fileVersion="
			+ fileVersion + ", headerSize = " + headerSize);

// Note "rw" is 5 to 10 times faster than "rws". Since we intend
// All of the file io to be done in a single JVM process, "rw" should work
// just fine.
//		raf = new RandomAccessFile(file, writable ? "rws" : "r");
		raf = new RandomAccessFile(file, writable ? "rw" : "r");
		buf = new byte[4];
		try
		{
			long fileLen = raf.length();
			Logger.instance().debug1("Opened '" + file.getPath() + "' len="
				+ fileLen + ", writable=" + writable
				+ ", fileVersion=" + fileVersion);
		}
		catch(IOException ex)
		{
			warning("Opened file but couldn't determine size: " + ex);
		}
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
	 * @param recvTime the receive time as a unix time_t
	 * @return byte location that message was saved at.
	 */
	public synchronized long writeMsg( DcpMsg msg)
		throws IOException
	{
		long offset = raf.length();
		int len = msg.getMsgLength();
		raf.seek(offset);
		if (fileVersion >= 7)
		{
			writeMsgXml(msg);
			return offset;
		}
		
		raf.write(MSG_STARTPATTERN);
		raf.writeInt(msg.flagbits);
		Date d = msg.getLocalReceiveTime();
		int time_t = d == null ? 0 : (int)(d.getTime()/1000);
		raf.writeInt(time_t);
		raf.writeInt(msg.getSequenceNum());

		if (fileVersion == 6)
		{
			d = msg.getCarrierStart();
			long msec = d == null ? 0L : d.getTime();
			raf.writeLong(msec);
			d = msg.getCarrierStop();
			msec = d == null ? 0L : d.getTime();
			raf.writeLong(msec);
			d = msg.getDomsatTime();
			msec = d == null ? 0L : d.getTime();
			raf.writeLong(msec);
			raf.writeShort((short)msg.getBaud());
			raf.writeByte(msg.mergeFilterCode);
			raf.writeByte((byte)0);   // reserved
			raf.writeInt(msg.getDataSourceId());
			raf.write(msg.reserved, 0, 32);
		}

		raf.writeInt(len);
		if (len > 0)
			raf.write(msg.getData());

		return offset;
	}
	
	/**
	 * Marks the flag bits in a message indicating that it has been
	 * deleted.
	 * @param loc the location of the message start.
	 */
	public synchronized void markMsgDeleted(long loc)
		throws EOFException, IOException
	{
		if (fileVersion >= 7)
		{
			markMsgDeletedXml(loc);
			return;
		}
		raf.seek(loc);
		raf.read(buf, 0, 4);
		if (!matchStartPattern(buf))
		{
			warning("Attempt to delete message at location "
				+ loc + " but start pattern not found.");
			return;
		}
		int flagbits = raf.readInt() | DcpMsgFlag.MSG_DELETED;
		raf.seek(loc+4);
		raf.writeInt(flagbits);
	}

	/**
	 * Adds a domsat sequence num to a message.
	 * @param loc the location of the message start.
	 * @param seq the domsat sequence #
	 * @param domtim the domsat time
	 */
	public synchronized void addDomsatSequence(long loc, int seq, long domtim)
		throws EOFException, IOException
	{
		raf.seek(loc);
		
		if (fileVersion >= 7)
		{
			addDomsatSequenceXml(loc, seq, domtim);
			return;
		}
		
		raf.read(buf, 0, 4);
		if (!matchStartPattern(buf))
		{
			warning(
				"MsgFile: Attempt to add seq# at location "
				+ loc + " but start pattern not found.");
			return;
		}
		int flagbits = raf.readInt() & (~DcpMsgFlag.MSG_NO_SEQNUM);
		raf.seek(loc+4);
		raf.writeInt(flagbits);
		raf.seek(loc+12);
		raf.writeInt(seq);
		if (fileVersion == 6)
		{
			raf.seek(loc+32);
			raf.writeLong(domtim);
		}
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
		if (fileVersion == 7)
			return readMsgXml(loc);
		
		long origLoc = loc;
		long fileLen = raf.length();
		int n = 0;
		boolean startPatFound = false;
		raf.seek(loc);
		int slide = 0;
		while(! startPatFound )
		{
			if ((fileLen-loc) < (headerSize-n))
				throw new EOFException("EOF-Header loc=" + loc + ", len="
					+ fileLen);
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
			warning("Skipped " + slide + " bytes, origloc=" + origLoc);
		DcpMsg msg = new DcpMsg();
		msg.flagbits = raf.readInt();
		msg.setLocalReceiveTime(new Date(raf.readInt()*1000L));
		msg.setSequenceNum(raf.readInt());

		if (fileVersion == 6)
		{
			long ms = raf.readLong();
			if (ms != 0)
				msg.setCarrierStart(new Date(ms));
			ms = raf.readLong();
			if (ms != 0)
				msg.setCarrierStop(new Date(ms));
			msg.setDomsatTime(new Date(raf.readLong()));
			msg.setBaud((int)raf.readShort());
			msg.mergeFilterCode = raf.readByte();
			raf.readByte();                         // reserved
			msg.setDataSourceId(raf.readInt());
			raf.read(msg.reserved, 0, 32);
		}

		int len = raf.readInt();
		loc += (headerSize - 4);
		if (fileLen - loc < len)
			throw new EOFException(
				"EOF-Data: origloc=" + origLoc 
				+ ", loc=" + loc + ", len=" + len + ", filelen="+fileLen);
		byte startPatternBuf[] = new byte[len];
		raf.read(startPatternBuf);
		msg.setData(startPatternBuf);
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
	
	private void writeMsgXml(DcpMsg msg)
		throws IOException
	{
		baos.reset();
		if (!xmlParser.addMsg(xos, msg, file.getName()))
			return;
//		if (!msg.isGoesMessage())
//			Logger.instance().info("MsgFile Writing NON-GOES message:\n" 
//				+ new String(baos.toByteArray()));
		raf.write(baos.toByteArray());
	}
	
	private void markMsgDeletedXml(long loc)
		throws IOException
	{
		raf.seek(loc);
		int numRead = raf.read(xmlMsgBuf, 0, 1024);
		int idx = ByteUtil.indexOf(xmlMsgBuf, numRead, flagStart);
		if (idx < 0 || idx+flagStart.length+10 >= numRead)
		{
			warning("markMsgDeletedXml - no flags found at location " + loc
				+ ", numRead=" + numRead + ", 1st 32 bytes '"
				+ new String(xmlMsgBuf, 0, (numRead < 32 ? numRead : 32)));
			return;
		}
		
		// Get old flag value & convert to hex number. Add 2 to skip "0x".
		try
		{
			String fstr = new String(
				ArrayUtil.getField(xmlMsgBuf, idx+flagStart.length + 2, 8));
			int flagbits = Integer.parseInt(fstr, 16);
			flagbits |= DcpMsgFlag.MSG_DELETED;
			raf.seek(loc + idx + flagStart.length);
			raf.write(ExtBlockXmlParser.formatFlagsValue(flagbits).getBytes());
		}
		catch(NumberFormatException ex)
		{
			warning("markMsgDeletedXml failed at location " + loc + ": " + ex);
		}
	}
	
	private void addDomsatSequenceXml(long loc, int seq, long domtim)
		throws IOException
	{
		raf.seek(loc);
		int numRead = raf.read(xmlMsgBuf, 0, 1024);
		int idx = ByteUtil.indexOf(xmlMsgBuf, numRead, domSeqStart);
		String seqstr = ExtBlockXmlParser.formatDomsatSeq(seq);
		if (idx < 0 || idx+domSeqStart.length+5 >= numRead)
		{
			warning("addDomsatSequenceXml - no domsat seq found at location "
				+ loc + "idx=" + idx + ", numRead=" + numRead 
				+ ", 1st 120 bytes:" 
				+ new String(xmlMsgBuf, 0, numRead < 120 ? numRead : 120));
			return;
		}
		raf.seek(loc+idx+domSeqStart.length);
		raf.write(seqstr.getBytes());

		idx = ByteUtil.indexOf(xmlMsgBuf, numRead, domTimeStart);
		String timstr = xmlParser.formatDate(new Date(domtim));
		if (idx < 0 || idx+domTimeStart.length+timstr.length() >= numRead)
		{
			warning("addDomsatSequenceXml - no domsat time found at location "
				+ loc);
			return;
		}
		raf.seek(loc+idx+domTimeStart.length);
		raf.write(timstr.getBytes());
	}
	
	private void warning(String s)
	{
		Logger.instance().warning("MsgFile(" + file.getName() + "): " + s);
	}

	private DcpMsg readMsgXml(long loc)
		throws IOException, EOFException
	{
		raf.seek(loc);
//		int off = 0;
		int totalLen = 0;
		int endTagIdx = -1;
		int numRead = 0;
		do
		{
			int numRequest = DcpMsg.MAX_DATA_LENGTH - totalLen;
			if (numRequest > 1024) numRequest = 1024;
			numRead = raf.read(xmlMsgBuf, totalLen, numRequest);
			totalLen += numRead;
			endTagIdx = ByteUtil.indexOf(xmlMsgBuf, totalLen, msgEndTag);
		} while(endTagIdx == -1 && numRead == 1024);
		
		if (endTagIdx == -1)
			throw new EOFException("No end tag found from location " + loc);
		
		int startIdx = ByteUtil.indexOf(xmlMsgBuf, totalLen, msgStartTag);
		if (startIdx != 0)
			warning("Msg at location " + loc
				+ " didn't start with correct tag, wasted " + startIdx
				+ " bytes.");
		if (startIdx >= endTagIdx)
		{
			warning("No valid message at loc=" + loc + ", startIdx="
				+ startIdx + ", endTagIdx=" + endTagIdx
				+ ", 1st 32 bytes '" + new String(xmlMsgBuf, 0, 
					totalLen>32 ? 32 : totalLen) + "'");
			return null;
		}
		
		// endTagIdx is the position where the string </DcpMsg> starts.
		// So we must adjust the seek position to just after the string
		// for programs like MsgFileDump that read without an index.
		int eom = endTagIdx + msgEndTag.length;
		while(eom < totalLen && Character.isWhitespace((char)xmlMsgBuf[eom]))
			eom++;
		raf.seek(loc + eom);
		
		byte[] msgBuf = ArrayUtil.getField(xmlMsgBuf, startIdx, 
			endTagIdx + msgEndTag.length - startIdx);
//warning("readMsgXml loc=" + loc + ", startIdx=" + startIdx + ", endTagIdx=" 
//+ endTagIdx + ", totalLen=" + totalLen + ", 1st 32 bytes '"
//+ new String(xmlMsgBuf, 0, 32) + "'");
		try { return xmlParser.parseDcpMsg(msgBuf); }
		catch(ProtocolError ex)
		{
			String errmsg = "Can't parse DcpMsg from data: " + ex;
			warning(errmsg);
			throw new IOException(errmsg);
		}
	}
}
