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

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import lrgs.common.DcpMsg;
import lrgs.common.SearchCriteria;
import lrgs.common.DcpMsgRetriever;
import lrgs.common.DcpMsgIndex;
import lrgs.common.ArchiveException;

/**
This class manages the search-criteria checking and file-saving for a
single priority (one of H, M, or L).
*/
public class LritDcsFileWriter
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	File dir;
	File scFile;
	SearchCriteria searchcrit;
	boolean searchCritFileExisted;
	DcpMsgRetriever searchCritTester;
	long lastScLoad;
	LritDcsFile output;
	char myPriority;
	int maxMsgs;
	int maxFileBytes;
	int maxFileSeconds;
	FileQueue myQueue;
	LritDcsFileStats myStats = new LritDcsFileStats();

	public LritDcsFileWriter(char priority, String dirname)
		throws InitFailedException
	{
		this(priority, dirname, true);
	}

	public LritDcsFileWriter(char priority, String dirname,
		boolean useSearchCrit)
		throws InitFailedException
	{
		myPriority = priority;
		dir = new File(dirname);
		try
		{
			if (!dir.isDirectory())
				dir.mkdirs();
			output = new LritDcsFile(myPriority, dir, Constants.SC_Both);
		}
		catch(Exception ex)
		{
			throw new InitFailedException("Cannot init LritDcsFileWriter " + priority, ex);
		}

		if (useSearchCrit)
		{
			scFile = new File(LritDcsConfig.instance().getLritDcsHome(),
				"searchcrit." + priority);
			log.info("{} LritDcsFileWriter for {} priority will read searchcrit '{}'",
					 0 , myPriority, scFile.getPath());

			lastScLoad = 0L;
			searchcrit = new SearchCriteria();
			try
			{
				searchCritTester = new DcpMsgRetriever();
				searchCritTester.setSearchCriteria(searchcrit);
			}
			catch(IOException ioex)
			{
				throw new InitFailedException("Cannot initialize searchcrit tester.", ioex);
			}
			catch(ArchiveException aex)
			{
				throw new InitFailedException("Cannot initialize searchcrit tester.", aex);
			}

			searchCritFileExisted = true; // Assume it does exist at first.
			checkSearchCriteria();
		}
		else // don't use search crit.
		{
			scFile = null;
			searchcrit = null;
			lastScLoad = 0L;
			searchCritTester = null;
		}

		maxMsgs = 1000;
		maxFileBytes = 1000000;
		maxFileSeconds = 60;

		if (!useSearchCrit)
			myQueue = LritDcsMain.instance().getFileQueueManualRetrans();
		else if (myPriority == Constants.HighPri)
			myQueue = LritDcsMain.instance().getFileQueueHigh();
		else if (myPriority == Constants.MediumPri)
			myQueue = LritDcsMain.instance().getFileQueueMedium();
		else
			myQueue = LritDcsMain.instance().getFileQueueLow();

		myStats.setPriority(priority);
		myStats.clear();
	}

	public void checkSearchCriteria()
	{
		try
		{
			if (!scFile.exists())
			{
				// Issue warning when I detect that searchcrit file was deleted.
				if (searchCritFileExisted)
				{
					if (myPriority == Constants.HighPri)
					{
						log.warn("{}- No High Priority search criteria file '{}' -- " +
								 "All messages will be considered high!",
								 Constants.EVT_SEARCHCRIT, scFile.getPath());
					}
					else if (myPriority == Constants.MediumPri)
					{
						log.warn("{}- No Medium Priority search criteria file '{}' -- " +
								 "All messages that are not high prority will be considered medium!",
								 Constants.EVT_SEARCHCRIT, scFile.getPath());
					}
					searchCritFileExisted = false;
					searchcrit.clear();
					searchCritTester.init();
				}
			}
			else
			{
				searchCritFileExisted = true;

				// ELSE scFile DOES exist, check to see if it has been modified.
				if (scFile.lastModified() > lastScLoad)
				{
					log.info("Reloading modified searchcrit file '{}'", scFile.getPath());
					lastScLoad = System.currentTimeMillis();
					searchcrit.parseFile(scFile);

					// Make sure that no time ranges are used.
					if (searchcrit.getDapsSince() != null
					 || searchcrit.getDapsUntil() != null
					 || searchcrit.getLrgsSince() != null
					 || searchcrit.getLrgsUntil() != null)
					{
						log.warn("Time ranges in search criteria '{}' will be ignored.", scFile.getPath());
						searchcrit.setDapsSince(null);
						searchcrit.setDapsUntil(null);
						searchcrit.setLrgsSince(null);
						searchcrit.setLrgsUntil(null);
					}

					searchCritTester.init();
				}
			}
		}
		catch(Exception ex)
		{
			log.atWarn()
			   .setCause(ex)
			   .log("{}- Bad Search Criteria File '{}' -- NO messages will pass priority-{}",
			   		Constants.EVT_SEARCHCRIT, scFile.getPath(), myPriority);
			searchCritFileExisted = false;
			searchcrit.clear();
			try { searchCritTester.init(); }
			// Exceptions will never happen with cleared SC
			catch(IOException ioex) {}
			catch(ArchiveException ssex) {}
		}
	}

	public boolean passesCriteria(DcpMsgIndex msgidx)
	{
		/*
		  Empty searchcrit? High does not pass, med & low do.
		*/
		if (searchcrit.isEmpty() )
		{
			if (myPriority == Constants.HighPri)
				return false;
			else
				return true;
		}

		// Test against search criteria
		return searchCritTester.testCriteria(msgidx);
	}

	public void saveMessage(DcpMsg dcpMsg)
	{
		// Add message to my LritDcsFile object.
		try { output.addMessage(dcpMsg); }
		catch(BadMessageException ex)
		{
			byte [] rawmsg = dcpMsg.getData();
			String s = new String(rawmsg, 0,
				(rawmsg.length < 19 ? rawmsg.length : 19));
			log.atWarn().setCause(ex).log( "Badly formatted DCP message cannot be archived '{}'", s);
			return;
		}

		myStats.messageAdded(dcpMsg);

		// check limits. If reached, save the file & clear it.
		if (output.getNumMessages() >= maxMsgs)
		{
			log.debug("Priority {} file message limit of {} reached.", myPriority, maxMsgs);
			saveFile();
		}
		else if (output.getImageSize() >= maxFileBytes)
		{
			log.debug("Priority {} file size limit of {} bytes reached.", myPriority, maxFileBytes);
			saveFile();
		}
	}

	public void checkFileTimeRange()
	{
		if (System.currentTimeMillis() - output.getFileStartTime()
			>= (maxFileSeconds*1000L)
		 && output.getNumMessages() > 0)
		{
			log.debug("Priority {} file time range of {} seconds expired.", myPriority, maxFileSeconds);
			saveFile();
		}
	}

	/// Saves the file & adds the File object to the queue.
	public void saveFile()
	{
		try
		{
			myStats.setFile(output.saveFile());  // save & clear the file.
			myStats.setFileSaveTime(new Date());
			myQueue.enqueue(myStats);
			log.debug("Created & queued priority {} file '{}'", myPriority, myStats.getFile().getPath());
			myStats = new LritDcsFileStats();
			myStats.setPriority(myPriority);
		}
		catch(LritDcsFileException ex)
		{
			log.atWarn().setCause(ex).log("{} - unable to save file.", Constants.EVT_FILE_SAVE_ERR);
		}
	}

	public void setMaxMsgs(int max) { maxMsgs = max; }

	public void setMaxFileBytes(int max) { maxFileBytes = max; }

	public void setMaxFileSeconds(int max) { maxFileSeconds = max; }

	public int getNumMessages() { return output.getNumMessages(); }
}
