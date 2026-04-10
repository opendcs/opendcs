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
import java.util.*;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.util.PropertiesUtil;

/**
 This class encapsulates current status for the LRIT DCS task.
*/
public class LritDcsStatus
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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

	public String lastFileName;

	public LritDcsStatus(String filepath)
	{
		clear();
		myFile = new File(filepath);
		log.trace("Status file will have path '{}'", myFile.getPath());
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
		lastFileName = "";
	}

	public void loadFromFile()
		throws StatusInvalidException
	{
		if (myFile.exists() && myFile.lastModified() <= lastModified)
		{
			log.trace("Skipping reload of '{}' -- no changes.", myFile.getPath());
			return;
		}

		log.trace("Opening '{}' for reading.", myFile.getPath());

		Properties props = new Properties();
		try (FileInputStream fis = new FileInputStream(myFile))
		{
			props.load(fis);
			loadFromProps(props);
		}
		catch(IOException ex)
		{
			throw new StatusInvalidException(ex.toString(), ex);
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
				log.debug("time tags don't match, skipping");
				throw new StatusInvalidException("Start/End time tags don't match.");
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
		log.trace("Writing '{}'", myFile.getPath());
		String fileContents = toString();

		try (FileOutputStream fos = new FileOutputStream(myFile))
		{
			fos.write(fileContents.getBytes());
		}
		catch(IOException ex)
		{
			log.atError().setCause(ex).log("Cannot write '{}'", myFile.getPath());
		}
	}

	public String toString()
	{
		Properties props = new Properties();
		PropertiesUtil.storeInProps(this, props, "");

		StringWriter sw = new StringWriter();
		try (PrintWriter pw = new PrintWriter(sw))
		{

			long timeTag = System.currentTimeMillis();
			pw.println(startTimeTagLabel + "=" + timeTag);
			props.store(pw, null);
			pw.println(endTimeTagLabel + "=" + timeTag);
		}
		catch(IOException ex)
		{
			log.atWarn().setCause(ex).log("LritDcsStatus cannot convert to string");
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
		LritDcsConfig cfg = LritDcsConfig.instance();
		LritDcsStatus stat = new LritDcsStatus(
			cfg.getLritDcsHome() + File.separator + STAT_FILE_NAME);

		stat.writeToFile();
	}
}