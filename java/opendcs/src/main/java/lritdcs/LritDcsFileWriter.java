/*
*  $Id$
*
*  $Log$
*  Revision 1.5  2012/12/12 16:01:31  mmaloney
*  Several updates for 5.2
*
*  Revision 1.4  2009/11/11 19:31:50  shweta
*  LRIT update
*
*  Revision 1.3  2009/10/16 12:39:00  mjmaloney
*  LRIT updates
*
*  Revision 1.2  2009/10/09 14:52:26  mjmaloney
*  Added flag bytes and carrier times to LRIT File.
*
*  Revision 1.1  2008/04/04 18:21:16  cvs
*  Added legacy code to repository
*
*  Revision 1.10  2005/12/30 19:40:59  mmaloney
*  dev
*
*  Revision 1.9  2005/07/28 20:33:06  mjmaloney
*  dev
*
*  Revision 1.8  2005/04/25 21:39:45  mjmaloney
*  dev
*
*  Revision 1.7  2005/03/21 14:44:58  mjmaloney
*  dev
*
*  Revision 1.6  2004/05/27 13:15:01  mjmaloney
*  DR fixes.
*
*  Revision 1.5  2004/05/18 22:52:40  mjmaloney
*  dev
*
*  Revision 1.4  2004/05/15 22:02:34  mjmaloney
*  Implemented retransmit and queue flush features.
*
*  Revision 1.3  2003/08/15 20:13:07  mjmaloney
*  dev
*
*  Revision 1.2  2003/08/10 02:22:47  mjmaloney
*  dev.
*
*  Revision 1.1  2003/08/06 23:29:24  mjmaloney
*  dev
*
*/
package lritdcs;

import java.io.*;
import java.util.Date;

import ilex.util.Logger;
import lrgs.common.DcpMsg;
import lrgs.common.SearchCriteria;
import lrgs.common.DcpMsgRetriever;
import lrgs.common.DcpMsgIndex;
import lrgs.common.DcpMsgSource;
import lrgs.common.DcpNameMapper;
import lrgs.common.ArchiveException;

/**
This class manages the search-criteria checking and file-saving for a
single priority (one of H, M, or L).
*/
public class LritDcsFileWriter
{
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
			throw new InitFailedException(
				"Cannot init LritDcsFileWriter " + priority + ": " + ex);
		}

		if (useSearchCrit)
		{
			scFile = new File(LritDcsConfig.instance().getLritDcsHome(), 
				"searchcrit." + priority);
			info(0, "LritDcsFileWriter for "
				+ myPriority + " priority will read searchcrit '"
				+ scFile.getPath() + "'");

			lastScLoad = 0L;
			searchcrit = new SearchCriteria();
			try
			{
				searchCritTester = new DcpMsgRetriever();
				searchCritTester.setSearchCriteria(searchcrit);
			}
			catch(IOException ioex)
			{
				throw new InitFailedException(
					"Cannot initialize searchcrit tester: " + ioex);
			}
			catch(ArchiveException aex)
			{
				throw new InitFailedException(
					"Cannot initialize searchcrit tester: " + aex);
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
						warning(Constants.EVT_SEARCHCRIT,
							"- No High Priority search criteria file '" 
							+ scFile.getPath() 
							+ " -- All messages will be considered high!");
					else if (myPriority == Constants.MediumPri)
						warning(Constants.EVT_SEARCHCRIT,
							"- No Medium Priority search criteria file '" 
							+ scFile.getPath() 
							+ "All messages that are not high prority will "
							+ " be considered medium!");
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
					info(0, "Reloading modified searchcrit file '"
						+ scFile.getPath() + "'");
					lastScLoad = System.currentTimeMillis();
					searchcrit.parseFile(scFile);
	
					// Make sure that no time ranges are used.
					if (searchcrit.getDapsSince() != null
					 || searchcrit.getDapsUntil() != null
					 || searchcrit.getLrgsSince() != null
					 || searchcrit.getLrgsUntil() != null)
					{
						warning(0, "Time ranges in search criteria '" 
							+ scFile.getPath() + "' will be ignored.");
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
			warning(Constants.EVT_SEARCHCRIT,
				"- Bad Search Criteria File '" + scFile.getPath()
				+ "': " + ex + 
				" -- NO messages will pass priority-" + myPriority);
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

//	public void saveMessage(byte rawmsg[])
	public void saveMessage(DcpMsg dcpMsg)
	{
		// Add message to my LritDcsFile object.
		try { output.addMessage(dcpMsg); }
		catch(BadMessageException ex)
		{
			byte [] rawmsg = dcpMsg.getData();
			String s = new String(rawmsg, 0, 
				(rawmsg.length < 19 ? rawmsg.length : 19));
			warning(0, "Badly formatted DCP message cannot be archived '" 
				+ s + "'");
			return;
		}

		myStats.messageAdded(dcpMsg);
	
		// check limits. If reached, save the file & clear it.
		if (output.getNumMessages() >= maxMsgs)
		{
			Logger.instance().log(Logger.E_DEBUG1,
				"Priority " + myPriority + " file message limit of " 
				+ maxMsgs + " reached.");
			saveFile();
		}
		else if (output.getImageSize() >= maxFileBytes)
		{
			Logger.instance().log(Logger.E_DEBUG1,
				"Priority " + myPriority + " file size limit of " 
				+ maxFileBytes + " bytes reached.");
			saveFile();
		}
	}

	public void checkFileTimeRange()
	{
		if (System.currentTimeMillis() - output.getFileStartTime()
			>= (maxFileSeconds*1000L)
		 && output.getNumMessages() > 0)
		{
			Logger.instance().log(Logger.E_DEBUG1,
				"Priority " + myPriority + " file time range of " 
				+ maxFileSeconds + " seconds expired.");
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
			Logger.instance().debug1( 
				"Created & queued priority " 
				+ myPriority + " file '" 
				+ myStats.getFile().getPath() + "'");
			myStats = new LritDcsFileStats();
			myStats.setPriority(myPriority);
		}
		catch(LritDcsFileException ex)
		{
			warning(Constants.EVT_FILE_SAVE_ERR, ex.toString());
		}
	}

	private String format(int evtnum, String msg)
	{
		String s = "LRIT";
		if (evtnum != 0)
			s = s + ":" + evtnum;
		if (msg.charAt(0) != '-')
			s = s + ' ';
		return s + msg + " (FileWriter-" + myPriority + ")";
	}

	private void warning(int evtnum, String s)
	{
		Logger.instance().warning(format(evtnum, s));
	}

	private void info(int evtnum, String s)
	{
		Logger.instance().info(format(evtnum, s));
	}

	public void setMaxMsgs(int max) { maxMsgs = max; }

	public void setMaxFileBytes(int max) { maxFileBytes = max; }

	public void setMaxFileSeconds(int max) { maxFileSeconds = max; }

	public int getNumMessages() { return output.getNumMessages(); }
}

