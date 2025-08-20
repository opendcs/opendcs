/*
* $Id$
*
* $Log$
* Revision 1.3  2012/12/12 16:01:31  mmaloney
* Several updates for 5.2
*
* Revision 1.2  2009/08/14 14:11:42  shweta
* Introduced 6 new variables for all 3 domain 2 hosts and their LRIT status
* These variables are  wriiten in lritdcs.stat file
*
* Revision 1.1  2008/04/04 18:21:16  cvs
* Added legacy code to repository
*
* Revision 1.7  2004/05/18 18:02:10  mjmaloney
* dev
*
* Revision 1.6  2004/05/11 20:46:26  mjmaloney
* LQM Impl
*
* Revision 1.5  2004/05/11 19:00:31  mjmaloney
* Working UI
*
* Revision 1.4  2003/08/15 20:13:07  mjmaloney
* dev
*
* Revision 1.3  2003/08/11 23:38:11  mjmaloney
* dev
*
* Revision 1.2  2003/08/11 15:59:19  mjmaloney
* dev
*
* Revision 1.1  2003/08/06 23:29:24  mjmaloney
* dev
*
*/
package lritdcs;

import java.io.*;
import java.util.*;
import java.lang.reflect.*;

import ilex.util.Logger;
import ilex.util.PropertiesUtil;

/**
 This class encapsulates current status for the LRIT DCS task.
*/
public class LritDcsStatus
{
	// Constants:
	public static final String STAT_FILE_NAME = "lritdcs.stat";

	/// Status was successfully read from file
	private boolean isValid;

	/// A status file existed for this object
	private boolean hasStatusFile;

	/// Overall process status.
	public String status;

	/// Last time message was received from any DDS connection.
	public long lastRetrieval;

	/// Current data source being used to retrieve data.
	public String lastDataSource;

	public long lastFileSendA;
	public long lastFileSendB;
	public long lastFileSendC;

	// stats for each priority.
	public int msgsTodayHigh;
	public int msgsYesterdayHigh;
	public int msgsThisHourHigh;
	public int msgsLastHourHigh;
	public int filesSentTodayHigh;
	public int filesSentYesterdayHigh;
	public int filesSentThisHourHigh;
	public int filesSentLastHourHigh;

	public int msgsTodayMedium;
	public int msgsYesterdayMedium;
	public int msgsThisHourMedium;
	public int msgsLastHourMedium;
	public int filesSentTodayMedium;
	public int filesSentYesterdayMedium;
	public int filesSentThisHourMedium;
	public int filesSentLastHourMedium;

	public int msgsTodayLow;
	public int msgsYesterdayLow;
	public int msgsThisHourLow;
	public int msgsLastHourLow;
	public int filesSentTodayLow;
	public int filesSentYesterdayLow;
	public int filesSentThisHourLow;
	public int filesSentLastHourLow;

	public int filesQueuedLow;
	public int filesQueuedMedium;
	public int filesQueuedHigh;
	public int filesQueuedAutoRetrans;
	public int filesQueuedManualRetrans;
	public int filesPending;

	public long lastLqmContact;
	public String lqmStatus;

	public long serverGMT;

	/// Last time status was modified.
	private long lastModified;
	private long lastRotate;

	/// Status is stored here.
	private File myFile;

	/// Use as line separator in file.
	private String lineSep;

	static final String startTimeTagLabel = "startTimeTag";
	static final String endTimeTagLabel = "endTimeTag";
	
	public String domain2Ahost;
	public String domain2Bhost;
	public String domain2Chost;
	
	public String domain2AStatus;
	public String domain2BStatus;
	public String domain2CStatus;
	
//	public String ptpDir;
//	public boolean ptpEnabled;
//	public long ptpLastSave;
	public String lastFileName;
	
	public LritDcsStatus(String filepath)
	{
		clear();
		myFile = new File(filepath);
		Logger.instance().log(Logger.E_DEBUG3,
			"Status file will have path '" + myFile.getPath() + "'");
		lineSep = System.getProperty("line.separator");
		if (lineSep == null)
			lineSep = "\n";
		lastModified = 0L;
	}

	public File getFile() { return myFile; }

	public void clear()
	{
		isValid = false;
		status = "init";
		lastRetrieval = 0L;
		lastDataSource = null;
		msgsTodayHigh = 0;
		msgsYesterdayHigh = 0;
		msgsThisHourHigh = 0;
		msgsLastHourHigh = 0;
		filesSentTodayHigh = 0;
		filesSentYesterdayHigh = 0;
		filesSentThisHourHigh = 0;
		filesSentLastHourHigh = 0;
		msgsTodayMedium = 0;
		msgsYesterdayMedium = 0;
		msgsThisHourMedium = 0;
		msgsLastHourMedium = 0;
		filesSentTodayMedium = 0;
		filesSentYesterdayMedium = 0;
		filesSentThisHourMedium = 0;
		filesSentLastHourMedium = 0;
		msgsTodayLow = 0;
		msgsYesterdayLow = 0;
		msgsThisHourLow = 0;
		msgsLastHourLow = 0;
		filesSentTodayLow = 0;
		filesSentYesterdayLow = 0;
		filesSentThisHourLow = 0;
		filesSentLastHourLow = 0;
		filesQueuedLow = 0;
		filesQueuedMedium = 0;
		filesQueuedHigh = 0;
		lastLqmContact = 0L;
		lqmStatus="Not Connected";
		serverGMT = 0L;
		domain2Ahost="";
		domain2Bhost="";
		domain2Chost="";
		domain2AStatus="";
		domain2BStatus="";
		domain2CStatus="";
		lastFileSendA = 0L;
		lastFileSendB = 0L;
		lastFileSendC = 0L;
//		ptpDir = "";
//		ptpEnabled = false;
//		ptpLastSave = 0L;
		lastFileName = "";
	}

	public void loadFromFile()
		throws StatusInvalidException
	{
		if (myFile.exists() && myFile.lastModified() <= lastModified)
		{
			Logger.instance().log(Logger.E_DEBUG3,
				"Skipping reload of '" + myFile.getPath() + "' -- no changes.");
			return;
		}

		Logger.instance().log(Logger.E_DEBUG2,
			"Opening '" + myFile.getPath() + "' for reading.");

		Properties props = new Properties();
		try
		{
			FileInputStream fis = new FileInputStream(myFile);
			props.load(fis);
			fis.close();
			loadFromProps(props);
		}
		catch(IOException ex)
		{
			throw new StatusInvalidException(ex.toString());
		}
	}

	public void loadFromProps(Properties props)
		throws StatusInvalidException
	{
		try
		{
			String s = props.getProperty(startTimeTagLabel);
			if (s == null)
				return;
			long startTimeTag = Long.parseLong(s);
			s = props.getProperty(endTimeTagLabel);
			if (s == null);
			long endTimeTag = Long.parseLong(s);
			if (startTimeTag != endTimeTag)
			{
				isValid = false;
				Logger.instance().log(Logger.E_DEBUG1, 
					"time tags don't match, skipping");
				throw new StatusInvalidException(
					"Start/End time tags don't match.");
			}
		}
		catch(NumberFormatException nfe)
		{
			isValid = false;
			return; // leave as invalid
		}
		clear();
		PropertiesUtil.loadFromProps(this, props);

		isValid = true;
		lastModified = myFile.lastModified();
		lastRotate = lastModified;
	}

	public void writeToFile()
	{
		Logger.instance().log(Logger.E_DEBUG3,"Writing '" + myFile.getPath()
			+ "'");
		String fileContents = toString();
		
		try
		{
			FileOutputStream fos = new FileOutputStream(myFile);
			fos.write(fileContents.getBytes());
			fos.close();
		}
		catch(IOException ex)
		{
			Logger.instance().log(Logger.E_FAILURE, "Cannot write '" 
				+ myFile.getPath() + "': " + ex);
		}
	}

	public String toString()
	{
		Properties props = new Properties();
		PropertiesUtil.storeInProps(this, props, "");
		
		StringWriter sw = new StringWriter();
		try
		{
			PrintWriter pw = new PrintWriter(sw);
			long timeTag = System.currentTimeMillis();
			pw.println(startTimeTagLabel + "=" + timeTag);
			props.store(pw, null);
			pw.println(endTimeTagLabel + "=" + timeTag);
			pw.close();
		}
		catch(IOException ex)
		{
			String msg = "LritDcsStatus cannot convert to string: " + ex;
			Logger.instance().warning(msg);
			System.err.println(msg);
			ex.printStackTrace(System.err);
		}
		
		return sw.toString();
	}

	public long getLastModified() { return lastModified; }

	public synchronized void incrementMsgHigh()
	{
		msgsTodayHigh++;
		msgsThisHourHigh++;
	}

	public synchronized void incrementMsgMedium()
	{
		msgsTodayMedium++;
		msgsThisHourMedium++;
	}

	public synchronized void incrementMsgLow()
	{
		msgsTodayLow++;
		msgsThisHourLow++;
	}

	public synchronized void incrementFileHigh()
	{
		filesSentTodayHigh++;
		filesSentThisHourHigh++;
	}

	public synchronized void incrementFileMedium()
	{
		filesSentTodayMedium++;
		filesSentThisHourMedium++;
	}

	public synchronized void incrementFileLow()
	{
		filesSentTodayLow++;
		filesSentThisHourLow++;
	}

	// Called periodically from main.
	public synchronized void rotateStatus()
	{
		long now = System.currentTimeMillis();

		long thisHour = now / 3600000L;
		long lastHour = lastRotate / 3600000L;

		if (thisHour == lastHour)
			; // Do nothing
		else if (thisHour != lastHour)
		{
			if (thisHour == lastHour + 1)
			{
				msgsLastHourHigh = msgsThisHourHigh;
				msgsThisHourHigh = 0;
				filesSentLastHourHigh = filesSentThisHourHigh;
				filesSentThisHourHigh = 0;
				msgsLastHourMedium = msgsThisHourMedium;
				msgsThisHourMedium = 0;
				filesSentLastHourMedium = filesSentThisHourMedium;
				filesSentThisHourMedium = 0;
				msgsLastHourLow = msgsThisHourLow;
				msgsThisHourLow = 0;
				filesSentLastHourLow = filesSentThisHourLow;
				filesSentThisHourLow = 0;
			}
			else // more than 1 hour elapsed.
			{
				msgsLastHourHigh = 0;
				msgsThisHourHigh = 0;
				filesSentLastHourHigh = 0;
				filesSentThisHourHigh = 0;
				msgsLastHourMedium = 0;
				msgsThisHourMedium = 0;
				filesSentLastHourMedium = 0;
				filesSentThisHourMedium = 0;
				msgsLastHourLow = 0;
				msgsThisHourLow = 0;
				filesSentLastHourLow = 0;
				filesSentThisHourLow = 0;
			}

			long thisDay = thisHour / 24;
			long lastDay = lastHour / 24;

			if (thisDay != lastDay)
			{
				if (thisDay == lastDay + 1)
				{
					msgsYesterdayHigh = msgsTodayHigh;
					msgsTodayHigh = 0;
					filesSentYesterdayHigh = filesSentTodayHigh;
					filesSentTodayHigh = 0;
					msgsYesterdayMedium = msgsTodayMedium;
					msgsTodayMedium = 0;
					filesSentYesterdayMedium = filesSentTodayMedium;
					filesSentTodayMedium = 0;
					msgsYesterdayLow = msgsTodayLow;
					msgsTodayLow = 0;
					filesSentYesterdayLow = filesSentTodayLow;
					filesSentTodayLow = 0;
				}
				else // More than 1 day elapsed.
				{
					msgsYesterdayHigh = 0;
					msgsTodayHigh = 0;
					filesSentYesterdayHigh = 0;
					filesSentTodayHigh = 0;
					msgsYesterdayMedium = 0;
					msgsTodayMedium = 0;
					filesSentYesterdayMedium = 0;
					filesSentTodayMedium = 0;
					msgsYesterdayLow = 0;
					msgsTodayLow = 0;
					filesSentYesterdayLow = 0;
					filesSentTodayLow = 0;
				}
			}
		}

		lastRotate = now;
	}

	public static void main(String args[])
		throws Exception
	{
		Logger.instance().setMinLogPriority(Logger.E_DEBUG3);
		LritDcsConfig cfg = LritDcsConfig.instance();
		LritDcsStatus stat = new LritDcsStatus(
			cfg.getLritDcsHome() + File.separator + STAT_FILE_NAME);
		//stat.loadFromFile();
		stat.writeToFile();
		//System.out.println(stat.toString());
	}
}
