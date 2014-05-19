/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:09  cvs
*  Added legacy code to repository
*
*  Revision 1.5  2005/07/08 20:03:08  mjmaloney
*  dev
*
*  Revision 1.4  2005/03/02 22:22:03  mjmaloney
*  update
*
*  Revision 1.3  2004/08/30 14:50:26  mjmaloney
*  Javadocs
*
*  Revision 1.2  2004/05/06 20:57:10  mjmaloney
*  Implemented QueueLogger to be used by servers that export events.
*
*  Revision 1.1  2004/04/02 18:58:16  mjmaloney
*  Created.
*
*/
package ilex.util;

import java.util.*;
import java.io.*;
 
/**
Used to implement daemons that watch one or more directories for files
to appear, and then processes them.
*/
public abstract class DirectoryMonitorThread extends Thread
{
	/** Optional filter to specify which files to process */
	private FilenameFilter myFilter;

	/** If true, sleep every cycle regardless of whether files were found. */
	private boolean sleepEveryCycle;
	
	/** Sleep this amount of time after each cycle through the directories. */
	private long sleepMsec;

	/** Directories to be scanned */
	protected ArrayList<File> myDirs = new ArrayList<File>();

	/** Internal flag to kill this thread */
	protected boolean isShutdown;

	/** Constructor called from sub-class only. */
	protected DirectoryMonitorThread( )
	{
		myFilter=null;
		sleepEveryCycle = true;
		sleepMsec = 1000L;
		isShutdown = false;
	}
	
	/**
	* By default, all files are processed, but call this method to install
	* a filter. Call with null to un-install the filter.
	* @param filter the filter
	*/
	public void setFilenameFilter( FilenameFilter filter )
	{
		myFilter=filter;
	}

	/**
	* Sets the amount of time to pause after each cycle through directories.
	* @param msec amount of time to pause in msec.
	*/
	public void setSleepInterval( long msec )
	{
		sleepMsec=msec;
	}
	
	/**
	* Sets flag to pause after each cycle through directories, regardless of
	* whether files are found.
	* The default is false, meaning that the thread will only pause if it
	* cycles through all directories without finding a file.
	* @param inp true if you want monitor to pause after each pass of the
	* directories. False if you're doing the pause in some other way.
	*/
	public void setSleepEveryCycle( boolean inp )
	{
		sleepEveryCycle=inp;
	}

	/**
	  Adds a directory to the list to be scanned.
	* @param dir the directory
	*/
	public void addDirectory( File dir )
	{
		myDirs.add(dir);
	}

	/**
	* Abstract method to be supplied by subclass, should
	* open the passed File and process the data therein. If you want to
	* process a file only once, this method should move the file to another
	* location or delete it.
	* @param file the file
	*/
	protected abstract void processFile( File file );
	
	/**
	* Abstract method to be supplied by subclass,
	* called after each cycle through the directories is completed, but
	* before the pause, if one is set.
	*/
	protected abstract void finishedScan( );
	
	/** Clears the list of directories to be scanned. */
	public void emptyDirectories( )
	{
		myDirs.clear();
	}
	
	/** Sets internal flag causing this thread to die. */
	public void shutdown( )
	{
		isShutdown=true;
	}
	
	/** Thread run method. */
	public void run( )
	{
		while(!isShutdown)
		{
			int numFilesProcessed = 0;
			for(int pos=0; pos<myDirs.size(); pos++)
			{
				File dir = (File)myDirs.get(pos);

				// List the files in this directory.
				File[] myfiles;
				if (myFilter != null)
					myfiles = dir.listFiles(myFilter);
				else
					myfiles = dir.listFiles();

				for(int pos1=0; myfiles != null && pos1<myfiles.length; pos1++)
				{
					if (!myfiles[pos1].isDirectory())
					{
						processFile(myfiles[pos1]);
						numFilesProcessed++;
					}
				}
			}
			finishedScan();

			if(sleepEveryCycle || numFilesProcessed == 0)
			{
				try{Thread.sleep(sleepMsec);}
				catch(InterruptedException e) {}
			}
		}
		cleanup();
	}
	
	/**
	 * Called once before run() method exits.
	 */
	protected abstract void cleanup();
}
