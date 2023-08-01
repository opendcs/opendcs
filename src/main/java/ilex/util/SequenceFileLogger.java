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

	/**
	* Construct with a process name and a filename.
	* @param procName Name of this process, to appear in each log message.
	* @param filename Name of log file.
	* @param maxLength how many bytes each log can be
	* @param count how many total files we will keep
	* @throws FileNotFoundException if can't open file
	*/
	public SequenceFileLogger( String procName, String filename, int logLevel, int maxLength, int count)
		throws IOException
	{
		super(procName, filename,logLevel, maxLength, count);
	}	

	/**
	 * Renames the current log with a suffix indicating it is now old.
	 */
	protected void renameCurrentLog()
	{
		// controlled by JULHandler
	}
}
