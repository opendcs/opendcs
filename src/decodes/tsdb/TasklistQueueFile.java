package decodes.tsdb;

import ilex.util.Logger;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Date;
import decodes.sql.DbKey;

/**
 * Manages a binary file containing a queue of task list entries.
 * This is used by computation processor if enabled by the tasklistQueueDir property.
 * 
 * @author mmaloney - Michael Maloney, Cove Software LLC
 */
public class TasklistQueueFile
{
	public static final String module = "TasklistQueue";
	
	/** Used for random access IO on the file. */
	private RandomAccessFile raf;

	/** Size of an index entry on disk */
	private static final int RECORD_SIZE = 80;
	
	/** Max number of records in a file */
	private static final int MAX_RECORDS = 2000000;

	/**
	 * Constructor.
	 * @param fn the file name
	 * @param update if true, open for read/write, else read-only.
	 */
	public TasklistQueueFile(String dir, String appName)
		throws IOException
	{
		makeFile(dir, appName);
	}

	private void makeFile(String dir, String appName)
		throws IOException
	{
		// build file name
		StringBuilder fileName = new StringBuilder(appName);
		for(int idx = 0; idx < fileName.length(); idx++)
		{
			char c = fileName.charAt(idx);
			if (Character.isWhitespace(c))
				fileName.setCharAt(idx, '_');
			else if (!Character.isLetter(c) && !Character.isDigit(c) && c != '-' && c != '_' && c != '.')
				fileName.deleteCharAt(idx--);
		}
		
		fileName.append("-tasklist.queue");
		File file = new File(dir, fileName.toString());
		raf = new RandomAccessFile(file, "rw");

		// If doesn't exist, initialize it with in and out ptr == 0
		if (raf.length() == 0)
		{
			raf.writeInt(0); // Initialize inPtr = 0
			raf.writeInt(0); // Initialize outPtr = 0
		}

		Logger.instance().debug1("Opened tasklist queue file '" + file.getPath() + "'"
			+ ", inPtr=" + getInPtr() + ", outPtr=" + getOutPtr());
	}

	/**
	 * Writes an index entry to the file at the specified location.
	 * Note: This may be overwriting a previous entry because we combine 
	 * DAPS status messages into a single flag word.
	 */
	public synchronized void writeRec(TasklistRec rec)
		throws IOException
	{
		int inPtr = getInPtr();
		positionToRecord(inPtr);
		
//Logger.instance().debug3(module + " writing record at inPtr=" + inPtr + ", position="
//+ raf.getFilePointer());
//Logger.instance().debug3(module + " Record writing to queue: " + rec);
		
		raf.writeInt(rec.getRecordNum());
		raf.writeLong(rec.getSdi().getValue());
		raf.writeByte(rec.isValueWasNull() ? (byte)'T' : (byte)'F');
		raf.writeByte(rec.isDeleted() ? (byte)'T' : (byte)'F');
		raf.writeDouble(rec.isValueWasNull() ? 0.0 : rec.getValue());
		raf.writeLong(rec.getTimeStamp().getTime());
		String eu = rec.getUnitsAbbr();
		if (eu == null)
			eu = "";
		for(int i=0; i<24; i++)
			raf.writeByte(i >= eu.length() ? (byte)0 : (byte)eu.charAt(i));
		
		raf.writeLong(rec.getVersionDate() != null ? rec.getVersionDate().getTime() : 0L);
		raf.writeLong(rec.getQualityCode());
		raf.writeInt(rec.getSourceId());
		
		String s = rec.getInterval();
		char code = 'u';
		if (s != null && s.length() > 0)
			code = Character.toLowerCase(s.charAt(0));
		raf.writeByte((byte)code);
		
		s = rec.getTableSelector();
		code = 'u';
		if (s != null && s.length() > 0)
			code = Character.toLowerCase(s.charAt(0));
		raf.writeByte((byte)code);

		raf.writeInt(rec.getModelRunId());

		// Update pointers
		if (++inPtr >= MAX_RECORDS)
			inPtr = 0;
		setInPtr(inPtr);
		int outPtr = getOutPtr();
		if (inPtr == outPtr)
		{
			if (++outPtr >= MAX_RECORDS)
				outPtr = 0;
			setOutPtr(outPtr);
		}
	}
	
	/** Called once when this object will no longer be used. */
	public void close( )
	{
		try { raf.close(); }
		catch(Exception ex) {}
	}
	
	/**
	 * Read the next tasklist record from the queue file.
	 * @return tasklist record or null if queue is currently empty.
	 */
	public synchronized TasklistRec readRec()
		throws IOException
	{
		int inPtr = getInPtr();
		int outPtr = getOutPtr();
		if (inPtr == outPtr)
			return null;
		
		positionToRecord(outPtr);

//Logger.instance().debug3(module + " reading record at outPtr=" + outPtr + ", position="
//+ raf.getFilePointer());

		int recnum = raf.readInt();
		DbKey tsKey = DbKey.createDbKey(raf.readLong());
		boolean wasNull = raf.readByte() == (byte)'T';
		boolean wasDeleted = raf.readByte() == (byte)'T';
		double value = raf.readDouble();
		Date timeStamp = new Date(raf.readLong());
		StringBuilder eu = new StringBuilder();
		for(int i=0; i<24; i++)
		{
			int c = raf.readByte();
			if (c != 0)
				eu.append((char)c);
		}
		
		long versionDate = raf.readLong();
		long qualityCode = raf.readLong();
		
		int sourceId = raf.readInt();
		
		// Interval only used for HDB, so only need to handle the HDB intervals.
		char c = (char)raf.readByte();
		String interval = null;
		switch(c)
		{
		case 'u': break;
		case 'i': interval = IntervalCodes.int_instant; break;
		case 'h': interval = IntervalCodes.int_hour; break;
		case 'd': interval = IntervalCodes.int_day; break;
		case 'm': interval = IntervalCodes.int_month; break;
		case 'y': interval = IntervalCodes.int_year; break;
		case 'w': interval = IntervalCodes.int_wy; break;
		}
		
		// Table Selector only used for HDB.
		c = (char)raf.readByte();
		String tabSel = null;
		if (c == 'r')
			tabSel = "R_";
		else if (c == 'm')
			tabSel = "M_";

		int modelRunId = raf.readInt();

		TasklistRec rec = new TasklistRec(recnum, tsKey, value,
			wasNull, timeStamp, wasDeleted,
			eu.toString(), versionDate != 0L ? new Date(versionDate) : null, qualityCode);
		rec.setSourceId(sourceId);
		if (interval != null)
			rec.setInterval(interval);
		if (tabSel != null)
			rec.setTableSelector(tabSel);
		rec.setModelRunId(modelRunId);
		
		// Update pointer
		if (++outPtr >= MAX_RECORDS)
			outPtr = 0;
		setOutPtr(outPtr);

//Logger.instance().debug3(module + " Record read from queue: " + rec);

		return rec;
	}

	private int getInPtr()
		throws IOException
	{
		raf.seek(0L);
		return raf.readInt();
	}
	
	private void setInPtr(int inPtr) 
		throws IOException
	{
		raf.seek(0L);
		raf.writeInt(inPtr);
	}

	private int getOutPtr()
		throws IOException
	{
		raf.seek(4L);
		return raf.readInt();
	}
	
	private void setOutPtr(int outPtr) 
		throws IOException
	{
		raf.seek(4L);
		raf.writeInt(outPtr);
	}
	
	private void positionToRecord(int recNum)
		throws IOException
	{
		raf.seek(8 + recNum * RECORD_SIZE);
	}

	/**
	 * test main to dump a queue.
	 * Call with two args, dir and filename.
	 * @param args
	 */
	public static void main(String args[])
		throws Exception
	{
		if (args.length != 2)
			System.out.println("Usage: java ... directory filename");
		TasklistQueueFile tqf = new TasklistQueueFile(args[0], args[1]);
		System.out.println("inPtr = " + tqf.getInPtr() + ", outPtr = " + tqf.getOutPtr());
		TasklistRec rec = null;
		while((rec = tqf.readRec()) != null)
			System.out.println(rec);
	}
}
