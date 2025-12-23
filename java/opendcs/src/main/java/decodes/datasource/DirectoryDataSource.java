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
package decodes.datasource;

import java.util.Properties;
import java.util.Vector;
import java.util.Enumeration;
import java.io.*;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.util.TextUtil;
import ilex.util.EnvExpander;
import ilex.util.FileUtil;
import ilex.util.PropertiesUtil;
import decodes.db.DataSource;
import decodes.db.Database;
import decodes.db.InvalidDatabaseException;
import decodes.db.NetworkList;
import decodes.db.Platform;
import decodes.util.DecodesSettings;
import decodes.util.PropertySpec;


/**
  Implements DataSourceExec to monitor a directory (or group of directories)
  for incoming data files.

  Properties include:
  <ul>
	<li>DirectoryName - Path name to directory to monitor. May contain 
		environment variables (default = current directory).</li>
	<li>FileExt - Only process files with the given extension (e.g. 
		".dat")</li>
	<li>Recursive - true or false. If true, monitor all subdirectories.</li>
	<li>NameIsMediumId (true or false) - If true, then the name of 
		the file (not counting the FileExt, if supplied) is
		used as the mediumID for looking up the platform.</li>
	<li>SubdirIsMediumId (true or false) - Used with Recursive flag. If true, 
		then the name of the immediate directory containing the data files is 
		used as the mediumID for looking up the platform.</li>
	<li>DoneDir - Path name of directory to put files after they have been
		processed. Either this or DoneExt must be supplied. May contain
		environment variables.</li>
	<li>DoneExt - After processing, files will be renamed with the addition
		of this extension. Either this or DoneDir must be supplied.</li>
  </ul>
  <p>
	This data source scans the specified directories periodically for new
	files. When it finds one, it uses an internal FileDataSource to open
	the file, read the header, and read the message body. The following
	properties are also supported and are simply passed-through to the
	FileDataSource prior to parsing each file.
  </p>
  <ul>
	<li>MediumType - The type of transport medium (default, "EDL")</li>
	<li>header - Determines the header parser to use.</li>
	<li>MediumId - If all data in this directory is for the same platform,
		you can specify the ID as a property.</li>
   <li>lengthAdj - (default=-1) adjustment to header length for reading 
	   socket. Will read 'adjusted length' bytes following header.</li>
   <li>delimiter - (default " \r\n") used for finding sync'ing stream</li>
   <li>before - synonym for 'delimiter'</li>
   <li>endDelimiter - (default null) marks end of message</li>
   <li>after - synonym for 'endDelimiter'</li>
   <li>oldChannelRanges - (default=false) If true, then chan &lt; 100 assumed to
       be self-timed, &gt; 100 assumed to be random.</li>
   <li>OneMessageFile - (default=false) If true, assume entire file is a
	   single message, meaning no delimiters needed and file length is the
	   message length.</li>
   <li>OneScanOnly - (default=false) If true, scan the directory once then exit.
       </li>
  </ul>
*/
public class DirectoryDataSource extends DataSourceExec
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	//========================================================================
	// The following variables are set from properties.
	//========================================================================

	/** Path name to directory to monitor. May contain environment variables.*/
	private String directoryName;

 	/** If present, only process files with this extension.*/
	private String fileExt;
	
 	/** If present, extract DCP Address from the filename itself.*/
	private String fileNameDelimiter;
	
	/** List of files to be processed */
	private File[] files;

 	/** If true, monitor all subdirectories in addition to the specified dir. */
	private boolean recursive;

	/** If true, use name of file as the mediumID. */
	private boolean nameIsMediumId;

	/** If true, use name of immediate directory as the mediumID. */
	private boolean subdirIsMediumId;
	
	/** Set property to true if filename contains timestamp */
	private boolean fileNameTimeStamp = false;
	
	private boolean oneScanOnly = false;
	
 	/**
	  If supplied, place files in in this dir after processing. Either this or
	  doneExt must be supplied.
	*/
	private String doneDir = null;

 	/**
	  If supplied, files will be renamed with addition of this extension after
	  processing. Either this or doneDir must be supplied.
	*/
	private String doneExt;
	private String siteNo;
	private String dbNo;
	private boolean onemessagefile;
	private boolean doneProcessingEnabled;
	

	//========================================================================
	// Internal working variables.
	//========================================================================

	/// The FileDataSource used to read files when we find one.
	private FileDataSource fileDataSource;

	/// Storage for properties to be passed to each FileDataSource init.
	private Properties allProps;

	/// Expanded list of directories to search.
	private Vector<File> dirs;

	/// Current index in the list of directories.
	private int dirsIdx;

	/// Used to list data files that we want.
	private FilenameFilter fileFilter;
	
	private int fileRestSeconds = 0;
	
	private static final PropertySpec[] DDSprops =
	{
		new PropertySpec("directoryName", PropertySpec.DIRECTORY, 
			"(required) name of directory to read."),
		new PropertySpec("fileExt", PropertySpec.STRING, 
			"If set, only process files with a matching extension. Other files are ignored."),
		new PropertySpec("nameIsMediumId", PropertySpec.BOOLEAN,
			"The file name contains the medium ID. Used with oneMessageFile. The medium ID"
			+ " is the file name minus the fileExt, if one is specified. Also see"
			+ " fileNameDelimiter"),
		new PropertySpec("fileNameDelimiter", PropertySpec.STRING, 
			"Used with nameIsMediumId if only the first part of the name, up to some "
			+ "delimiter, is to be considered the medium ID. Specify the delimiter here."),
		new PropertySpec("recursive", PropertySpec.BOOLEAN,
			"(default=false) This means to process the tree of subdirectories under "
			+ "the specified directoryName"),
		new PropertySpec("subDirIsMediumId", PropertySpec.BOOLEAN,
			"(default=false) Used with recursive. If true, then the subdirectory containing "
			+ "the file being processed is taken as the medium ID."),
		new PropertySpec("doneProcessing", PropertySpec.BOOLEAN,
			"(default=true) If false, then simply delete a file after processing. If "
			+ "true, then either rename the file with doneExt or move the file to doneDir"),
		new PropertySpec("doneExt", PropertySpec.STRING,
			"If set, then after processing a file, rename it by adding this extension. "
			+ "This should be something different than fileExt to prevent the same file from "
			+ "being processed repeatedly."),
		new PropertySpec("doneDir", PropertySpec.DIRECTORY,
			"If set, then after processing a file, move it to this directory."),
		new PropertySpec("oneMessageFile", PropertySpec.BOOLEAN,
			"(default=false) If set to true, then the entire file contents is taken to be"
			+ " a single message for the purposes of decoding."),
		new PropertySpec("fileNameTimeStamp", PropertySpec.BOOLEAN,
			"(default=false) Used with oneMessageFile, fileNameDelimiter, and fileExt. "
			+ " If true, then the characters between the dilimiter and the extension are "
			+ "taken to contain the message time stamp, which must be in the format "
			+ "MMDDYYYYHHMMSS"),
		new PropertySpec("oneScanOnly", PropertySpec.BOOLEAN,
			"(default=false) If true, then only scan the directory once. The normal "
			+ "behavior is to repeatedly scan the directory with a brief pause between "
			+ "each scan."),
		new PropertySpec("gzip", PropertySpec.BOOLEAN, 
			"(default=false) set to true to un-gzip each file before processing."),
		new PropertySpec("fileRestSeconds", PropertySpec.INT,
			"(default=0) Don't process files until at least this many seconds have elapsed "
			+ "since the file was last modified. This is a way of preventing DECODES from "
			+ "processing a file as it is being created.")
	};

	
	/**
	 * @see decodes.datasource.DataSourceExec#DataSourceExec(DataSource, Database) DataSourceExec Constructor
	 *
	 * @param ds data source
	 * @param db database
	 */
	public DirectoryDataSource(DataSource ds, Database db)
	{
		super(ds,db);
	}

	/** Clears internal variables to default states. */
	public void clear()
	{
		// Fill in defaults
		directoryName = ".";
		fileExt= null;
		recursive = false;
		subdirIsMediumId = false;
		doneDir = null;
		doneExt = null;
		fileDataSource = null;
		allProps = null;
		dirs = new Vector<File>();
		dirsIdx = 0;
		siteNo ="";
		onemessagefile = false;
		doneProcessingEnabled = true;
		fileNameDelimiter = ".";
	}

	/**
	  Called when object is instantiated and associated with a database
	  record. Do nothing. Wait for init method.
	*/
	public void processDataSource()
		throws InvalidDatabaseException
	{
	}

	/**
	  Initializes the data source.

	  @param routingSpecProps the routing spec properties.
	  @param since the since time from the routing spec.
	  @param until the until time from the routing spec.
	  @param networkLists contains NetworkList objects.
	  @throws DataSourceException if the source could not be initialized.
	*/
	public void init(Properties routingSpecProps, String since, 
		String until, Vector<NetworkList> networkLists)
		throws DataSourceException
	{
		log.debug("DirectoryDataSource.init() for '{}'",getName());

		clear();

		// Build a complete property set. Routing Spec props override DS props.
		allProps = new Properties(dbDataSource.arguments);
		for(Enumeration it = routingSpecProps.propertyNames();
			it.hasMoreElements();)
		{
			String name = (String)it.nextElement();
			String value = routingSpecProps.getProperty(name);
			allProps.setProperty(name, value);
		}

		for(Enumeration it = allProps.propertyNames(); it.hasMoreElements();)
		{
			String name = (String)it.nextElement();
			String value = allProps.getProperty(name);

			name = name.trim().toLowerCase();
			if (name.equals("directoryname"))
				directoryName = EnvExpander.expand(value);
			else if (name.equals("fileext"))
				fileExt = value;
			else if (name.equals("filenamedelimiter"))
			{
				fileNameDelimiter = value;
				nameIsMediumId = true; // implied
			}
			else if (name.equals("recursive"))
				recursive = TextUtil.str2boolean(value);
			else if (name.equals("nameismediumid"))
				nameIsMediumId = TextUtil.str2boolean(value);
			else if (name.equals("subdirismediumid"))
				subdirIsMediumId = TextUtil.str2boolean(value);
			else if (name.equals("doneprocessing")) 
				doneProcessingEnabled = TextUtil.str2boolean(value);
			else if (name.equals("doneext"))
				doneExt = value;
			else if (name.equalsIgnoreCase("donedir"))
			{
				if (value != null && value.trim().length() > 0
				 && !value.equalsIgnoreCase("none"))
					doneDir = value;
				
				else
					doneDir=null;
			}
			else if (name.equalsIgnoreCase("onemessagefile") ) {
				if	( value.equalsIgnoreCase("true" ) )
				{
					onemessagefile = true;
					allProps.setProperty("OneMessageFile", "true");
				} else {
					onemessagefile = false;
				}
			}
			else if (name.equals("filenametimestamp"))
				fileNameTimeStamp = TextUtil.str2boolean(value);
			else if (name.equals("onescanonly"))
				oneScanOnly = TextUtil.str2boolean(value);
			else if (name.equals("filerestseconds"))
			{
				try { fileRestSeconds = Integer.parseInt(value); }
				catch(NumberFormatException ex)
				{
					log.atWarn().setCause(ex).log("DirectoryDataSource invalid property '{}' should be integer. Ignored.", name);
					fileRestSeconds = 0;
				}
			}
		}
		
		String archiveDirName = DecodesSettings.instance().archiveDataDir;
		if ( doneDir == null && archiveDirName != null && archiveDirName.trim().length() > 0)
			doneDir = archiveDirName;
		
		if (doneProcessingEnabled && doneDir == null && doneExt == null)
			throw new DataSourceException(
				"Either DoneDir or DoneExt must be specified!");
		
		// Construct the file reader, but don't init. This is done on each file.
		DataSource fds = new DataSource("DirFileReader", "file");
		try 
		{
			fileDataSource = (FileDataSource)fds.makeDelegate(); 
			fileDataSource.processDataSource();
		}
		catch(InvalidDatabaseException ex)
		{
			throw new DataSourceException("Unexpected Error", ex);
		}

		// Build the filename filter.
		final String dext = doneExt;
		final String fext = fileExt;
		fileFilter = 
			new FilenameFilter()
			{
				public boolean accept(File dir, String name)
				{
					if (dext != null && name.endsWith(dext))
						return false;
					if (name.endsWith(".err"))
						return false;
					File f = new File(dir, name);
					if (!f.isFile())
						return false;
					if (fileRestSeconds > 0)
					{
						int sec = (int)((System.currentTimeMillis() - f.lastModified())/1000L);
						if (sec < fileRestSeconds)
							return false;
					}
					return fext == null || name.endsWith(fext);
				}
			};
		expandDirs();
	}

	/**
	  Closes the data source.
	  This method is called by the routing specification when the data
	  source is no longer needed.
	*/
	public void close()
	{
		clear();
	}

	/**
	  Scans the directories for a new file, and if one is found, reads
	  it and returns it.
	  If all directories are scanned and no file is found, 
	  return null, causing routing spec to pause.

	  <p>No timeout is set for directories.

	  @return the next RawMessage from the data source.

	  @throws DataSourceException if some other problem arises.
	*/
	@Override
	protected RawMessage getSourceRawMessage()
		throws DataSourceException
	{
		if (dirs.size() == 0)
			throw new DataSourceException("Directory '" + directoryName 
				+ " unreadable.");

		// Start where I left off in the directory list.
		int startIdx = dirsIdx;
		while(true)
		{
			boolean fileWasOpen = fileDataSource.isOpen();
			RawMessage ret = scanDirectory(dirs.elementAt(dirsIdx));
			
			// MJM if we get null because the current file hit EOF,
			// then try again, which will force the directory to move to the next file.
			if (fileWasOpen && ret == null)
				continue;
			
			if (ret != null) 
			{
				if ( onemessagefile ) 
				{
					Platform p = ret.getPlatform();
					if ( p != null && p.getSite() != null ) 
						siteNo = p.getSite().getDisplayName();
				}
				
				return ret;
			}
			else if (oneScanOnly)
				throw new DataSourceEndException("Directory Scan Complete");
			
			dirsIdx = (dirsIdx + 1) % dirs.size();

			if (dirsIdx == 0 && recursive)
				expandDirs(); // Every time through loop, re-expand.

			if (dirsIdx == startIdx)
				break;
		}

		// Fell through means searched entire list with no success,
		// returning null will cause 5-sec pause in RS.
		return null;
	}

	private RawMessage scanDirectory(File dir)
		throws DataSourceException
	{
		/*
		  File may contain multiple messages. If still open from last time,
		  try to read the next message from it.
		*/
		boolean messageExists = false;
		if ( dir == null )
			return null;
		if ( !fileDataSource.isOpen())
		{		
			files = dir.listFiles(fileFilter);
			
			if (files != null && files.length > 0)
			{
				allProps.setProperty("filename", files[0].getPath());
log.trace("DirectoryDataSource, added 'filename' property={}",
						  allProps.getProperty("filename"));
				if (nameIsMediumId)
				{
					String name = files[0].getName();
					// lop off the extension if one was given.
					if (fileExt != null)
						name = name.substring(0,name.length()-fileExt.length());
					
					String mediumId = name;
					
					// trim everything after delimiter, if one was given.
					int idx = mediumId.indexOf(fileNameDelimiter);
					if (idx > 0)
						mediumId = mediumId.substring(0, idx);
					allProps.setProperty("mediumid", mediumId);
					if (fileNameTimeStamp && idx > 0 && name.length()>idx+1)
						allProps.setProperty("filenametimestamp",
							name.substring(idx+1));
				}
				else if (subdirIsMediumId)
					allProps.setProperty("mediumid",
						files[0].getParentFile().getName());

				try
				{
					// opens file.
					log.trace("DirectoryDataSource 2, added 'filename' property={}",
							  allProps.getProperty("filename"));
					fileDataSource.init(allProps, null, null, null); 
					messageExists=true;
				}
				catch(DataSourceException ex)
				{
					log.atError().setCause(ex).log("Unable to initialize file data source.");
					files[0].renameTo(new File(files[0].getPath() + ".err"));
					return null;
				}
			}
		}
		else
			messageExists = true;
		if ( messageExists ) 
		{
			try 
			{
				RawMessage ret = fileDataSource.getSourceRawMessage(); // reads it.
				return ret;
			}
			catch(DataSourceEndException ex)
			{
				fileDataSource.close();
				if ( !doneProcessingEnabled ) 
				{
					files[0].delete();
				}
				else 
				{
					// We evaluate doneDir here at the closing of every file.
					// This allows the name to have embedded strings in it.
					if (doneDir != null)
					{
						File targetDir = null;
						Properties p = new Properties(System.getProperties());
						String agency = DecodesSettings.instance().agency;
						if (agency != null && agency.length() > 0)
							p.setProperty("AGENCY", agency);
						String location = DecodesSettings.instance().location;
						if (location != null && location.length() > 0)
							p.setProperty("LOCATION",DecodesSettings.instance().location);
						if (onemessagefile && siteNo != "" && siteNo != null) 
							p.setProperty("SITENAME",siteNo);
						if (onemessagefile && dbNo != "" && dbNo != null) 
							p.setProperty("DBNO",dbNo);
						targetDir = new File(EnvExpander.expand(doneDir,p));
						log.info("File '{}' will be moved to '{}'.",
						         files[0].getPath(), targetDir.getPath());
						if (!targetDir.isDirectory() && !targetDir.mkdirs())
							log.warn("Could not create '{}'.", targetDir.getPath());
						
						moveFile(files[0], targetDir);
					}
					else if (doneExt != null)
						files[0].renameTo(new File(files[0].getPath() + doneExt));
				}
			}
			catch(DataSourceException ex)
	   		{
				log.atError().setCause(ex).log("Unable to retrieve raw message.");
				fileDataSource.close();
				files[0].renameTo(new File(files[0].getPath() + ".err"));
			}
		}
		return null;
	}

	private void moveFile(File orig, File dir)
	{
		File target = new File(dir.getPath(), orig.getName());
		try { FileUtil.moveFile(orig, target); }
		catch(Exception ex)
		{
			log.atError().setCause(ex).log("Error moving '{}' to '{}'", orig.getPath(), dir.getPath());
		}
	}

	
	private void expandDirs()
		throws DataSourceException
	{
		dirs.clear();
		File topdir = new File(directoryName);
		if (!topdir.isDirectory())
		{
			try{ topdir.mkdirs(); }
			catch(Exception ex)
			{
				throw new DataSourceException("Directory '" + directoryName
					+ "' does not exist and cannot be created.", ex);
			}

		}
		dirs.add(topdir);
		if (recursive)
			expand(topdir);
		log.trace("Expanded '{}' to {} directories.", topdir.getPath(), dirs.size());
	}

	private void expand(File dir)
	{
		File[] files = dir.listFiles();
		for(int i=0; i<files.length; i++)
			if (files[i].isDirectory())
			{
				dirs.add(files[i]);
				expand(files[i]);
			}
	}
	
	@Override
	public PropertySpec[] getSupportedProps()
	{
		return PropertiesUtil.combineSpecs(super.getSupportedProps(), DDSprops);
	}

}
