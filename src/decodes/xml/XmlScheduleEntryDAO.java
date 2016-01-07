package decodes.xml;

import ilex.util.ByteUtil;
import ilex.util.EnvExpander;
import ilex.util.Logger;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;

import org.xml.sax.SAXException;

import decodes.db.DatabaseObject;
import decodes.db.ScheduleEntry;
import decodes.db.ScheduleEntryStatus;
import decodes.sql.DbKey;
import decodes.tsdb.CompAppInfo;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import opendcs.dai.ScheduleEntryDAI;

/**
 * This implements the ScheduleEntry Data Access Interface for XML DECODES Databases.
 * Schedule Entries are stored in individual files in the "schedule" subdirectory.
 *
 * ScheduleStatus is stored in a binary file of fixed length entries with the
 * following structure:
 * <ul>
 *   <li>sched entry name: 32 char null-terminated string</li>
 *   <li>run start time: 8-byte long int number of msec since epoch</li>
 *   <li>last message time: 8-byte long int number of msec since epoch</li>
 *   <li>run stop time: 8-byte long int number of msec since epoch</li>
 *   <li>hostname: 32 char null-terminated string</li>
 *   <li>run status: 32 char null-terminated string</li>
 *   <li>num messages: 4-byte int</li>
 *   <li>num decode errors: 4-byte int</li>
 *   <li>num platforms: 4-byte int<li>
 *   <li>last source: 32 char null-terminated string</li>
 *   <li>last consumer: 32 char null-terminated string</li>
 *   <li>last modify time: 8-byte long int number of msec since epoch</li>
 * </ul>
 * Thus an entry takes 204 bytes.
 * When sched entry name starts with a null char, it means this slot is empty.
 * @author mmaloney Mike Maloney, Cove Software LLC
 */
public class XmlScheduleEntryDAO implements ScheduleEntryDAI
{
	private XmlDatabaseIO parent = null;
	private RandomAccessFile statusFile = null;
	private static final int entryLength = 204;
	// Size the file to hold 34560 entries. This is based on running a schedule
	// entry every 5 minutes for 120 days = 12 (per hour) * 24 hours * 120 days.
	private static final int maxEntries = 12 * 24 * 120;
	private static HashMap<String, ScheduleEntryStatus> mostRecentStatus
		= new HashMap<String, ScheduleEntryStatus>();
	
	public XmlScheduleEntryDAO(XmlDatabaseIO parent)
	{
		this.parent = parent;
	}
	
	
	@Override
	public ArrayList<ScheduleEntry> listScheduleEntries(CompAppInfo app)
		throws DbIoException
	{
		try
		{
			ArrayList<ScheduleEntry> ret = new ArrayList<ScheduleEntry>();
			File dbdir = new File(parent.xmldir, XmlDatabaseIO.ScheduleEntryDir);
			File files[] = dbdir.listFiles();
			for(File f : files)
			{
				if (!f.isFile())
					continue;
				try
				{
					DatabaseObject dbo = parent.getParser().parse(f);
					if (dbo instanceof ScheduleEntry)
					{
						ScheduleEntry se = (ScheduleEntry)dbo;
						ret.add(se);
					}
					else
						Logger.instance().warning("Ignoring non-ScheduleEntry "
							+ "in file '" + f.getPath() + "'");
				}
				catch (SAXException ex)
				{
					Logger.instance().warning("Error parsing '" + f.getPath() + "': " + ex);
				}
			}
			return ret;
		}
		catch (IOException ex)
		{
			throw new DbIoException("Cannot list 'schedule' xml directory: " + ex);
		}
	}
	
	@Override
	public ScheduleEntry readScheduleEntry(String name) throws DbIoException
	{
		ArrayList<ScheduleEntry> ses = listScheduleEntries(null);
		for(ScheduleEntry se : ses)
			if (se.getName().equalsIgnoreCase(name))
				return se;
		return null;
	}


	@Override
	public boolean checkScheduleEntry(ScheduleEntry scheduleEntry)
		throws DbIoException, NoSuchObjectException
	{
		File schedDir = new File(parent.xmldir, XmlDatabaseIO.ScheduleEntryDir);
		if (!schedDir.isDirectory())
			schedDir.mkdir();
		File f = new File(schedDir, scheduleEntry.getName() + ".xml");
		if (!f.exists())
			throw new NoSuchObjectException(f.getPath() + " does not exist.");
		if (f.lastModified() > scheduleEntry.getLastModified().getTime())
		{
			try
			{
				DatabaseObject dbo = parent.getParser().parse(f);
				if (dbo instanceof ScheduleEntry)
				{
					scheduleEntry.copyFrom((ScheduleEntry)dbo);
					return true;
				}
				else
					throw new NoSuchObjectException(f.getPath() 
						+ " does not contain a schedule entry.");
			}
			catch (Exception ex)
			{
				throw new DbIoException("Cannot read '" + f.getPath() + "': " + ex);
			}
		}
		else
			return false;
	}

	@Override
	public void writeScheduleEntry(ScheduleEntry scheduleEntry)
		throws DbIoException
	{
		File schedDir = new File(parent.xmldir, XmlDatabaseIO.ScheduleEntryDir);
		if (!schedDir.isDirectory())
			schedDir.mkdir();
		
		File f = new File(schedDir, scheduleEntry.getName() + ".xml");
		try
		{
			TopLevelParser.write(f, scheduleEntry);
		}
		catch (IOException ex)
		{
			throw new DbIoException("Cannot write '" + f + "': " + ex);
		}
	}

	@Override
	public void deleteScheduleEntry(ScheduleEntry scheduleEntry)
		throws DbIoException
	{
		File schedDir = new File(parent.xmldir, XmlDatabaseIO.ScheduleEntryDir);
		if (!schedDir.isDirectory())
			schedDir.mkdir();
		
		this.deleteScheduleStatusFor(scheduleEntry);
		File f = new File(schedDir, scheduleEntry.getName() + ".xml");
		f.delete();
	}

	@Override
	public synchronized ArrayList<ScheduleEntryStatus> 
		readScheduleStatus(ScheduleEntry scheduleEntry) 
		throws DbIoException
	{
		if (statusFile == null)
			openStatusFile();
		try
		{
			statusFile.seek(0L);
			ArrayList<ScheduleEntryStatus> ret = new ArrayList<ScheduleEntryStatus>();
			ScheduleEntryStatus ses = null;

			// Read the entries from beginning to end of file.
			// Fill the cache of most recent statuses as we go.
			while((ses = readNextEntry()) != null)
			{
				String seName = ses.getScheduleEntryName();
				if (seName.length() != 0) // not empty slot
				{
					if (scheduleEntry == null                   // we're getting everything
					 || seName.equals(scheduleEntry.getName())) // or matches desired SE
						ret.add(ses);
					long thisLMT = ses.getLastModified().getTime();
					ScheduleEntryStatus cachedSES = mostRecentStatus.get(seName);
					if (cachedSES == null || thisLMT > cachedSES.getLastModified().getTime())
						mostRecentStatus.put(seName, ses);
				}
			}

			// Sort into ascending order by last modify time.
			if (ret.size() > 0)
			{
				Collections.sort(ret, 
					new Comparator<ScheduleEntryStatus>()
					{
						@Override
						public int compare(ScheduleEntryStatus arg0,
							ScheduleEntryStatus arg1)
						{
							long x = arg0.getLastModified().getTime() 
								- arg1.getLastModified().getTime();
							return x < 0 ? -1 : x > 0 ? 1 : 0;
						}
					});
			}
			return ret;
		}
		catch (IOException ex)
		{
			throw new DbIoException("Error reading status file: " + ex);
		}
	}
	
	/**
	 * Reads the next ScheduleEntryStatus from the file, starting at the current
	 * position.
	 * @return null if EOF reached.
	 * @throws IOException on other IO errors.
	 */
	private ScheduleEntryStatus readNextEntry()
		throws IOException
	{
		long startPtr = statusFile.getFilePointer();
		long entryNum = (int)(startPtr/entryLength);
		ScheduleEntryStatus ses = new ScheduleEntryStatus(DbKey.createDbKey(entryNum));
		byte buffer[] = new byte[entryLength];
		
		try
		{
			statusFile.readFully(buffer);
			
			// A null byte at the start of the record indicates an empty record.
			if (buffer[0] == 0)
				return ses;
			
			ses.setScheduleEntryName(ByteUtil.getCString(buffer, 0));

			long msec = ByteUtil.getInt8_BigEndian(buffer, 32);
			ses.setRunStart(msec == 0 ? null : new Date(msec));
			
			msec = ByteUtil.getInt8_BigEndian(buffer, 40);
			ses.setLastMessageTime(msec == 0 ? null : new Date(msec));

			msec = ByteUtil.getInt8_BigEndian(buffer, 48);
			ses.setRunStop(msec == 0 ? null : new Date(msec));
				
			ses.setHostname(ByteUtil.getCString(buffer, 56));
			ses.setRunStatus(ByteUtil.getCString(buffer, 88));
			
			ses.setNumMessages(ByteUtil.getInt4_BigEndian(buffer, 120));
			ses.setNumDecodesErrors(ByteUtil.getInt4_BigEndian(buffer, 124));
			ses.setNumPlatforms(ByteUtil.getInt4_BigEndian(buffer, 128));
			
			ses.setLastSource(ByteUtil.getCString(buffer, 132));
			ses.setLastConsumer(ByteUtil.getCString(buffer, 164));
			
//StringBuilder x = new StringBuilder("last 8 bytes of buffer: ");
//for(int i=0; i<8; i++)
//	x.append(" " + Integer.toHexString((int)buffer[196+i] & 0xff));
//Logger.instance().debug3(x.toString());
			msec = ByteUtil.getInt8_BigEndian(buffer, 196);
			ses.setLastModified(new Date(msec));
//Logger.instance().debug3("Read lastmod msec=" + msec + " (0x" + Long.toHexString(msec)
//	+ ") date=" + ses.getLastModified());
		
			return ses;
		}
		catch(EOFException ex)
		{
			Logger.instance().debug1("EOF reading schedule entry, startPtr=" + startPtr);
			return null;
		}
	}

	private void openStatusFile()
		throws DbIoException
	{
		String fn = EnvExpander.expand("$DCSTOOL_USERDIR/schedstat.dat");
		try
		{
			statusFile = new RandomAccessFile(fn, "rw");
		}
		catch (FileNotFoundException ex)
		{
			throw new DbIoException("Cannot open '" + fn + "': " + ex);
		}
	}
	
	@Override
	public synchronized void writeScheduleStatus(ScheduleEntryStatus ses)
		throws DbIoException
	{
		if (statusFile == null)
			openStatusFile();

		ses.setLastModified(new Date());
		try
		{
			DbKey key = ses.getKey();
			long recnum = 0L;
			if (key.isNull())
			{
				recnum = findEmptyRec();
				ses.forceSetId(DbKey.createDbKey(recnum));
			}
			else
				recnum = key.getValue();
			
			statusFile.seek(recnum * entryLength);
//Logger.instance().debug3("Writing schedule status entry at position "
//+ (recnum*entryLength) + ", entryLength=" + entryLength);

			byte strbuf[] = new byte[32];
			ByteUtil.putCString(ses.getScheduleEntryName(), strbuf, 0, 32);
			statusFile.write(strbuf);
//Logger.instance().debug3("   SE-Name='" + ses.getScheduleEntryName() + "', "
//	+ "LastMod=" + ses.getLastModified()
//	+ ", host='" + ses.getHostname() + "'");
//Logger.instance().debug3("   Last Src='" + ses.getLastSource() + "', "
//	+ "Last Cons='" + ses.getLastConsumer() + "'");
//Logger.instance().debug3("   msgs=" + ses.getNumMessages() + ", "
//	+ "Errs=" + ses.getNumDecodesErrors() + ", plats=" + ses.getNumPlatforms());

			statusFile.writeLong(ses.getRunStart().getTime());
			statusFile.writeLong(
				ses.getLastMessageTime() == null ? 0L : ses.getLastMessageTime().getTime());
			statusFile.writeLong(
				ses.getRunStop() == null ? 0L : ses.getRunStop().getTime());
			ByteUtil.putCString(ses.getHostname(), strbuf, 0, 32);
			statusFile.write(strbuf);
			ByteUtil.putCString(ses.getRunStatus(), strbuf, 0, 32);
			statusFile.write(strbuf);
			statusFile.writeInt(ses.getNumMessages());
			statusFile.writeInt(ses.getNumDecodesErrors());
			statusFile.writeInt(ses.getNumPlatforms());
			ByteUtil.putCString(ses.getLastSource(), strbuf, 0, 32);
			statusFile.write(strbuf);
			ByteUtil.putCString(ses.getLastConsumer(), strbuf, 0, 32);
			statusFile.write(strbuf);
//Logger.instance().debug3("   lastmod msec=0x" 
//	+ Long.toHexString(ses.getLastModified().getTime())
//	+ " writing at position " + statusFile.getFilePointer());
			statusFile.writeLong(ses.getLastModified().getTime());
		}
		catch (IOException ex)
		{
			throw new DbIoException("Cannot write Schedule Entry Status: " + ex);
		}
		mostRecentStatus.put(ses.getScheduleEntryName(), ses);
	}
	
	/**
	 * @return record number to write to. This is either the end of the file or
	 * an empty record previously used.
	 * @throws IOException
	 */
	private long findEmptyRec()
		throws IOException
	{
		long numEntries = statusFile.length() / entryLength;
		if (numEntries < maxEntries)
			return numEntries;
		Date oldestLMT = null;
		long oldestRecnum = -1;
		for(long n = 0; n < maxEntries; n++)
		{
			statusFile.seek(n * entryLength);
			ScheduleEntryStatus ses = readNextEntry();
			if (ses.getScheduleEntryName().length() == 0)
				return n;
			if (oldestLMT == null || ses.getLastMessageTime().before(oldestLMT))
			{
				oldestRecnum = n;
				oldestLMT = ses.getLastMessageTime();
			}
		}
		return oldestRecnum;
	}

	@Override
	public synchronized void deleteScheduleStatusBefore(CompAppInfo appInfo, Date cutoff) 
		throws DbIoException
	{
		if (statusFile == null)
			openStatusFile();

		ArrayList<ScheduleEntry> appSchedEntries = listScheduleEntries(appInfo);
		try
		{
			long numEntries = statusFile.length() / entryLength;
Logger.instance().debug3("deleteScheduleStatusBefore filelen=" + statusFile.length()
+ ", numEntries=" + numEntries);
			for(long n = 0; n < numEntries; n++)
			{
				statusFile.seek(n * entryLength);
Logger.instance().debug3("Reading entry[" + n + "] at position " + statusFile.getFilePointer());
				ScheduleEntryStatus ses = readNextEntry();
				if (ses == null)
					continue;
				if (ses.getScheduleEntryName() != null
				 && ses.getScheduleEntryName().length() != 0   // not already deleted
				 && ses.getLastModified().before(cutoff))   // LMT before cutoff
				{
					for(ScheduleEntry se : appSchedEntries)
						if (se.getName().equalsIgnoreCase(ses.getScheduleEntryName()))
						{
							statusFile.seek(n * entryLength);
							statusFile.writeByte(0); // Nulling the name will mark record as deleted.
							break;
						}
				}
			}
		}
		catch (IOException ex)
		{
			throw new DbIoException("Error in deleteScheduleStatusBefore(" 
				+ cutoff + "): " + ex);
		}
	}

	@Override
	public synchronized void deleteScheduleStatusFor(ScheduleEntry scheduleEntry)
		throws DbIoException
	{
		if (statusFile == null)
			openStatusFile();

		String seName = scheduleEntry.getName();
		if (seName.length() == 0)
			return;
		try
		{
			long numEntries = statusFile.length() / entryLength;
			for(long n = 0; n < numEntries; n++)
			{
				statusFile.seek(n * entryLength);
				ScheduleEntryStatus ses = readNextEntry();
				if (seName.equals(ses.getScheduleEntryName()))
				{
					statusFile.seek(n * entryLength);
					statusFile.writeByte(0); // Nulling the name will mark record as deleted.
				}
			}
		}
		catch (IOException ex)
		{
			throw new DbIoException("Error in deleteScheduleStatusFor(" 
				+ seName + "): " + ex);
		}
	}

	@Override
	public ScheduleEntryStatus getLastScheduleStatusFor(
		ScheduleEntry scheduleEntry) throws DbIoException
	{
		if (statusFile == null)
			openStatusFile();

		ScheduleEntryStatus ret = mostRecentStatus.get(scheduleEntry.getName());
		if (ret != null)
			return ret;
		readScheduleStatus(scheduleEntry);
		return mostRecentStatus.get(scheduleEntry.getName());
	}

	@Override
	public void close()
	{
		if (statusFile != null)
		{
			try { statusFile.close(); } catch(Exception ex) {}
			statusFile = null;
		}
		mostRecentStatus.clear();
	}




}
