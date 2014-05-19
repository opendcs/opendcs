/*
*  $Id$
*/
package ilex.util;

import java.util.Date;
import java.io.*;


/**
* This extends the normal FileLogger by adding the capability to
* store multiple logs with a sequence number extension.
* The logs are give an increasing number with age: the current log is
* logname, the previous one is logname.1, the one before that is logname.2,
* etc.
*/
public class SequenceFileLogger extends FileLogger
{
	private int numOldLogs;

	/**
	* Construct with a process name and a filename.
	* @param procName Name of this process, to appear in each log message.
	* @param filename Name of log file.
	* @throws FileNotFoundException if can't open file
	*/
	public SequenceFileLogger( String procName, String filename)
		throws FileNotFoundException
	{
		super(procName, filename);
		this.numOldLogs = 5;
	}

	/** 
	* Sets the number of old logs to use.
	* @param numOldLogs the number of sequenced old logs to keep.
	*/
	public void setNumOldLogs(int numOldLogs)
	{
		this.numOldLogs = numOldLogs;
	}

	/**
	 * Renames the current log with a suffix indicating it is now old.
	 */
	protected void renameCurrentLog()
	{
//System.out.println("Renaming logs.");
		// Delete the oldest log if it exists.
		File oldFile = new File(filename + "." + numOldLogs);
		if (oldFile.exists())
//{System.out.println("Deleting '" + oldFile.getName() + "'");
			oldFile.delete();
//}

		// Increase the number on each log.
		for(int i=numOldLogs-1; i>0; i--)
		{
			oldFile = new File(filename + "." + i);
			if (oldFile.exists())
				oldFile.renameTo(new File(filename + "." + (i+1)));
		}

		// Finally change current file with a ".1" extension.
		oldFile = new File(filename + ".1");
		outputFile.renameTo(oldFile);
//try { Thread.sleep(3000L); } catch(InterruptedException ex) {}
	}
}

