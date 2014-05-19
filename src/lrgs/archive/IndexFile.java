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

import java.io.RandomAccessFile;
import java.io.IOException;
import java.io.EOFException;
import java.io.File;
import java.util.Date;

import lrgs.common.DcpAddress;
import lrgs.common.DcpMsgIndex;
import ilex.util.ArrayUtil;
import ilex.util.Logger;
import ilex.util.TextUtil;

/**
This class does IO operations on the binary index file.
*/
public class IndexFile
{
	/** Used for random access IO on the file. */
	private RandomAccessFile raf;

	/** Size of an index entry on disk */
	private int INDEX_SIZE = 32;

	private String filename;

	/** Number of entries in this file. */
	private int numEntries;

	/** True if this is a Version 6 (or later) Index File */
	private int fileVersion = 5;

	/**
	 * Constructor.
	 * @param fn the file name
	 * @param update if true, open for read/write, else read-only.
	 */
	public IndexFile(String fn, boolean update)
		throws IOException
	{
		filename = fn;
		File tf = new File(filename);
		String tfn = tf.getName();
		if (TextUtil.startsWithIgnoreCase(tfn, "archv"))
		{
			fileVersion = 7;
			INDEX_SIZE = 60;
		}
		else if (TextUtil.startsWithIgnoreCase(tfn, "arch"))
		{
			fileVersion = 6;
			INDEX_SIZE = 44;
		}
		else
		{
			fileVersion = 5;
			INDEX_SIZE = 32;
		}

		raf = new RandomAccessFile(fn, update ? "rw" : "r");
		try { numEntries = (int)(raf.length() / INDEX_SIZE); }
		catch(IOException ioex) { numEntries = 0; }

		Logger.instance().debug1("Opened index file '" + fn + "' update="
			+ update + ", len=" + raf.length() + ", num indexes="
			+ numEntries + ", fileVersion=" + fileVersion);
	}

	/**
	 * Writes an index entry to the file at the specified location.
	 * Note: This may be overwriting a previous entry because we combine 
	 * DAPS status messages into a single flag word.
	 */
	public synchronized void writeIndex( DcpMsgIndex ie, int idxNum )
		throws IOException
	{
		raf.seek(idxNum * INDEX_SIZE);
		raf.writeInt((int)(ie.getLocalRecvTime().getTime()/1000L));
		raf.writeInt((int)(ie.getXmitTime().getTime()/1000L));
		
		if (fileVersion < 7)
		{
			raf.writeInt((int)ie.getOffset());
			raf.writeInt((int)ie.getDcpAddress().getAddr());
		}
		else
		{
			raf.writeLong(ie.getOffset());
			raf.write(ArrayUtil.resize(
				ie.getDcpAddress().toString().getBytes(), 16));
		}

		raf.writeShort((short)ie.getSequenceNum());
		raf.writeShort((short)ie.getChannel());

		if (fileVersion < 7)
			raf.writeShort((short)ie.getFlagbits());
		else
			raf.writeShort((short)0);
		
		raf.writeByte((byte)ie.getFailureCode());
		raf.writeByte(ie.getMergeFilterCode());
		raf.writeInt(ie.getPrevFileThisDcp());
		raf.writeInt(ie.getPrevIdxNumThisDcp());
		if (fileVersion >= 6)
		{
			raf.writeInt(ie.getDataSourceId());
			raf.writeInt(ie.getMsgLength());
			if (fileVersion >= 7)
				raf.writeInt(ie.getFlagbits());
			else
				raf.writeInt(0);				// Reserved Space
		}
		if (idxNum >= numEntries)
			numEntries = idxNum + 1;
	}
	
	/** Called once when this object will no longer be used. */
	public void close( )
	{
		try { raf.close(); }
		catch(Exception ex) {}
	}
	
	/**
	 * Read a bunch of 'n' index entries into the passed
	 * array, starting at specified index number.
	 * For efficiency, I might want to transfer data directly into the
	 * MsgIndexEntry objects already in the array (require this); Rather
	 * than allocating new MsgIndexEntry objects and putting them in the
	 * array.
	 * @return number of entries read.
	 */
	public synchronized int readIndexes(int startNum, int n, DcpMsgIndex[] ies)
		throws IOException
	{
		raf.seek(startNum * INDEX_SIZE);
		long startPtr = raf.getFilePointer();
		int nr = 0;
		try
		{
			for(int i=0; i<n; i++)
			{
				ies[i] = new DcpMsgIndex();
				ies[i].setLocalRecvTime(
					new Date((((long)raf.readInt())&0xffffffffL)*1000L));
				ies[i].setXmitTime(
					new Date((((long)raf.readInt())&0x7fffffffL)*1000L));
				if (fileVersion >= 7)
				{
					ies[i].setOffset(raf.readLong());
					byte b[] = new byte[16];
					raf.read(b);
					int p=0;
					for(; p < 16 && b[p] != 0; p++);
					ies[i].setDcpAddress(new DcpAddress(new String(b, 0, p)));
				}
				else
				{
					ies[i].setOffset(((long)raf.readInt()) & 0xffffffff);
					ies[i].setDcpAddress(new DcpAddress(raf.readInt()));
				}
				ies[i].setSequenceNum((short)raf.readShort());
				ies[i].setChannel((short)raf.readShort());
				if (fileVersion >= 7)
					raf.readShort(); // unused
				else
					ies[i].setFlagbits((int)raf.readShort());
				ies[i].setFailureCode((char)raf.readByte());
				ies[i].setMergeFilterCode(raf.readByte());
				ies[i].setPrevFileThisDcp(raf.readInt());
				ies[i].setPrevIdxNumThisDcp(raf.readInt());
				if (fileVersion >= 6)
				{
					ies[i].setDataSourceId(raf.readInt());
					ies[i].setMsgLength(raf.readInt());
					if (fileVersion >= 7)
						ies[i].setFlagbits(raf.readInt());
					else
						raf.readInt();   // throw away reserved space
				}
				else // before v6 length was not saved in file
					ies[i].setMsgLength(0);

				nr++;
			}
		}
		catch(EOFException eofex)
		{
			String msg = "EOF reading index number "
				+ (startNum + nr) + " from '" + filename 
				+ "' startNum=" + startNum 
				+ ", startPtr=" + startPtr
				+ ", numRequested=" + n
				+ ", numRead=" + nr 
				+ ", curPtr=" + raf.getFilePointer() 
				+ ", numEntries=" + numEntries
				+ ", fileLen=" + raf.length();
			Logger.instance().debug1(msg);
		}
		// Allow other IO exceptions to propegate.
		return nr;
	}

	/** @return number of index entries in this file. */
	public int getNumEntries()
	{
		return numEntries;
	}
}
