/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:04  cvs
*  Added legacy code to repository
*
*  Revision 1.4  2005/01/03 18:51:33  mjmaloney
*  Added javadocs.
*
*  Revision 1.3  2004/05/06 15:29:40  mjmaloney
*  Bug fixes in beta 6.1
*
*  Revision 1.2  2004/04/29 19:14:49  mjmaloney
*  6.1 release prep
*
*  Revision 1.1  2004/04/29 01:10:21  mjmaloney
*  Created.
*
*/
package decodes.routmon;

import java.io.*;
import java.util.*;

import ilex.util.Logger;
import ilex.util.DirectoryMonitorThread;

/**
This class monitors the routing-spec status directory for status files.
Each time the files are scanned, a new HTML report is produced showing
the current status of all routing specs on this machine.
*/
public class RoutingDirMonitor extends DirectoryMonitorThread
{	
	/** Link back to application main */
	private RoutingMonitor parent;

	/**
	 * Constructor configures the DirectoryMonitorThread to watch for files
	 * in the specified directory with names ending in ".stat".
	 * @param mymonitor parent application main
	 */
	RoutingDirMonitor(RoutingMonitor mymonitor)
	{
		parent=mymonitor;
		FilenameFilter ff = 
			new FilenameFilter()
			{
				public boolean accept(File dir, String name)
				{
					return name.endsWith(".stat");
				}
			};
		setFilenameFilter(ff);
	}
	
	
	/** 
	  Passed in a file and returns the property class contained
	  inside of the file.
	  @param myfile the properties file.
	  @return the Properties set
	 */ 
	private Properties load(File myfile)
	{
		FileInputStream mystream = null;
		try
		{
			mystream = new FileInputStream(myfile);
			Properties myproperties = new Properties();
			myproperties.load(mystream);
			return myproperties;
		}
		catch(Exception ex)
		{
			Logger.instance().log(Logger.E_WARNING,
				"Cannot read properties file '" + myfile.getPath()
				+ "': " + ex + ", -- ignoring");
			return null;
		}
	}
	
	/** 
	 * Called by the DirectoryMonitorThread class when it finds a routing
	 * spec status file. Parse the properties set and pass it to the parent
	 * application.
	 * @param myfile the routing spec status file
	 */
	public void processFile(File myfile)
	{
		try { parent.setStatus(load(myfile), myfile.lastModified()); }
		catch(BadStatusFile ex)
		{
			Logger.instance().log(Logger.E_WARNING,
				"Error in routing status file '" + myfile.getPath()
				+ "': " + ex);
		}
	}
	
	/** 
	 * Called by the DirectoryMonitorThread class when it completes a scan
	 * of the intput directory. Tell the parent to generate the HTML report.
	*/
	public void finishedScan()
	{
		parent.generateReport();
	}


	@Override
	protected void cleanup()
	{
	}
}
