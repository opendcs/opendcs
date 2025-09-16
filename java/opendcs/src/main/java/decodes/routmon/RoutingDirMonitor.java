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
package decodes.routmon;

import java.io.*;
import java.util.*;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.util.DirectoryMonitorThread;

/**
This class monitors the routing-spec status directory for status files.
Each time the files are scanned, a new HTML report is produced showing
the current status of all routing specs on this machine.
*/
public class RoutingDirMonitor extends DirectoryMonitorThread
{	
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
		try(FileInputStream mystream = new FileInputStream(myfile))
		{
			Properties myproperties = new Properties();
			myproperties.load(mystream);
			return myproperties;
		}
		catch(Exception ex)
		{
			log.atWarn().setCause(ex).log("Cannot read properties file '{}' -- ignoring", myfile.getPath());
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
		log.trace("Processing '{}'", myfile.getPath());
		try { parent.setStatus(load(myfile), myfile.lastModified()); }
		catch(BadStatusFile ex)
		{
			log.atWarn().setCause(ex).log("Error in routing status file '{}'", myfile.getPath());
		}
	}
	
	/** 
	 * Called by the DirectoryMonitorThread class when it completes a scan
	 * of the input directory. Tell the parent to generate the HTML report.
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
