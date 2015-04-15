/*
*  $Id$
*
*  $Log$
*  Revision 1.5  2015/04/02 18:13:38  mmaloney
*  Added 'fileRestSeconds' property.
*  Added PropertySpecs.
*
*  Revision 1.4  2014/10/02 18:21:42  mmaloney
*  FTP Data Source to handle multiple file names.
*
*  Revision 1.3  2014/05/30 13:15:35  mmaloney
*  dev
*
*  Revision 1.2  2014/05/28 13:09:30  mmaloney
*  dev
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.15  2011/11/29 16:06:22  mmaloney
*  Add "filename" to msg before calling PMP
*
*  Revision 1.14  2011/09/27 01:23:08  mmaloney
*  Enhancements to StreamDataSource for SHEF and NOS Decoding.
*
*  Revision 1.13  2011/07/29 14:44:22  mmaloney
*  Re-added processing for the DoneDir property. Now the done-dir can be set by routing-spec property,
*  or by the archiveDirName setting in "decodes.properties". The DoneDir property will take precedence
*  if both are set.
*
*  Revision 1.12  2010/12/21 19:21:06  sparab
*  USGS Dan's changes incorporated to simplify the doneDir property
*
*  Revision 1.11  2009/09/12 13:31:27  mjmaloney
*  USGS Merge
*
*  Revision 1.10  2009/05/08 14:30:13  mjmaloney
*  remove debugs
*
*  Revision 1.9  2009/05/06 14:43:41  mjmaloney
*  fixed USGS archive file processing for done dir.
*
*  Revision 1.8  2009/05/06 14:34:52  mjmaloney
*  dev
*
*  Revision 1.7  2009/05/06 14:26:03  mjmaloney
*  dev
*
*  Revision 1.6  2009/05/06 14:01:40  mjmaloney
*  dev
*
*  Revision 1.5  2009/04/17 17:05:18  sjagga
*  Changes for Decoding AutoPoll related directory data source. Based on new AutoPoll requirements, we will now add two new properties to read the MediumId and Message time stamp from the file name itself using another new property called delimiter.
*
*  Revision 1.4  2008/11/20 18:49:18  mjmaloney
*  merge from usgs mods
*
*
*  satin Fixed to handle files whose file name has the mediumId as part of
*  its name. This is to handle directories that have the property
*  NameIsMediumId and handles files whose name has this format:
*
*  				<mediumid>
*  				<mediumid>.<anything>
*
*  Revision 1.3  2008/09/26 14:56:53  mjmaloney
*  Added <all> and <production> network lists
*
*  Revision 1.2  2008/06/06 15:10:05  cvs
*  updates from USGS & fixes to update-check.
*
*  Revision 1.9  2008/05/29 01:09:34  satin
*  Added property to enable/disable done processing.
*
*  Revision 1.8  2008/05/28 20:22:29  satin
*  *** empty log message ***
*
*  Revision 1.7  2008/05/27 11:54:44  satin
*  If DoneDir = none, do not create directory.
*
*  Revision 1.6  2008/05/21 18:52:04  satin
*  Corrected problem in determining whether the files were "onemessagefiles".
*
*  Revision 1.5  2008/05/19 13:59:00  satin
*  Modified so that the "DoneDir" has variables expanded and if 1) it is
*  processing "OneMessageFile"s and 2) the property "archiveDataFileName"
*  is set, will use an expanded "DoneDir/archiveDataFileName" as the name
*  of the archived raw file.  Since it is a one message file, the "SITENAME"
*  variable is also made available in the expansion.  This allows the user
*  flexibility in defining where the archived raw data file should be placed.
*
*  Revision 1.4  2005/06/21 14:00:51  mjmaloney
*  Better responsiveness on DDS links for timeout & hangup conditions.
*
*  Revision 1.3  2004/08/24 23:52:43  mjmaloney
*  Added javadocs.
*
*  Revision 1.2  2004/04/08 19:47:18  satin
*  For files with multiple messages, modified so that end-of-file
*  exceptions were properly handled.
*
*  Revision 1.1  2003/12/12 17:55:32  mjmaloney
*  Working implementation of DirectoryDataSource.
*
*/
package decodes.datasource;

import java.util.Properties;
import java.util.Vector;
import java.util.Enumeration;
import java.io.*;

import ilex.util.Logger;
import ilex.util.TextUtil;
import ilex.util.EnvExpander;
import ilex.util.FileUtil;
import ilex.util.PropertiesUtil;
import decodes.db.DataSource;
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
   <li>oldChannelRanges - (default=false) If true, then chan<100 assumed to
       be self-timed, >100 assumed to be random.</li>
   <li>OneMessageFile - (default=false) If true, assume entire file is a
	   single message, meaning no delimiters needed and file length is the
	   message length.</li>
   <li>OneScanOnly - (default=false) If true, scan the directory once then exit.
       </li>
  </ul>
*/
public class DirectoryDataSource extends DataSourceExec
{
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

	
	/** default constructor */
	public DirectoryDataSource()
	{
		super();
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
		Logger.instance().log(Logger.E_DEBUG1, 
			"DirectoryDataSource.init() for '" + getName() + "'");

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
				
//				{
//					doneDir = new File(EnvExpander.expand(value));
//					if (!doneDir.isDirectory())
//					{
//						try { if (!doneDir.mkdirs()) throw new Exception(); }
//						catch(Exception ex)
//						{
//							throw new DataSourceException("Done Directory '"
//								+ value + "' doesn't exist and cannot be created: "
//								+ ex);
//						}
//					}
//					Logger.instance().info(
//						"After processing, files will be moved to '"
//						+ doneDir.getPath() + "'");
//				}
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
					Logger.instance().warning("DirectoryDataSource invalid property '"
						+ name + "' should be integer. Ignored.");
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
			throw new DataSourceException("Unexpected: " + ex);
		}

		// Build the filename filter.
//		final File ddir = doneDir;
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
	public RawMessage getRawMessage()
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
Logger.instance().debug3("DirectoryDataSource, added 'filename' property=" 
+ allProps.getProperty("filename"));
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
Logger.instance().debug3("DirectoryDataSource 2, added 'filename' property=" 
+ allProps.getProperty("filename"));
					fileDataSource.init(allProps, null, null, null); 
					messageExists=true;
				}
				catch(DataSourceException ex)
				{
					Logger.instance().log(Logger.E_FAILURE,
						"DirectoryDataSource: " + ex);
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
				RawMessage ret = fileDataSource.getRawMessage(); // reads it.
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
						Logger.instance().info("File '" + files[0].getPath() 
							+ "' will be moved to '" + targetDir.getPath() + "'");
						if (!targetDir.isDirectory() && !targetDir.mkdirs())
							Logger.instance().warning("Could not create '" 
								+ targetDir.getPath() + "'");
						
						moveFile(files[0], targetDir);
					}
					else if (doneExt != null)
						files[0].renameTo(new File(files[0].getPath() + doneExt));
				}
			}
			catch(DataSourceException ex)
	   		{
				Logger.instance().log(Logger.E_FAILURE,
					"DirectoryDataSource: " + ex);
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
			Logger.instance().failure(
				"Error moving '" + orig.getPath() + "' to '"
					+ dir.getPath() + "': " + ex);
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
					+ "' does not exist and cannot be created: " + ex);
			}

		}
		dirs.add(topdir);
		if (recursive)
			expand(topdir);
Logger.instance().log(Logger.E_DEBUG3,"Expanded '" + topdir.getPath() 
+ "' to " + dirs.size() + " directories.");
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

