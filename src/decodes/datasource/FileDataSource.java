/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.3  2012/04/09 15:24:22  mmaloney
*  Added gzip property.
*
*  Revision 1.2  2012/04/07 16:50:36  mmaloney
*  Added "gzip" boolean property. Set to true to have FileDataSource read a gzipped file.
*
*  Revision 1.1  2008/04/04 18:21:00  cvs
*  Added legacy code to repository
*
*  Revision 1.12  2004/08/24 23:52:43  mjmaloney
*  Added javadocs.
*
*  Revision 1.11  2003/12/12 17:55:32  mjmaloney
*  Working implementation of DirectoryDataSource.
*
*  Revision 1.10  2003/12/07 20:36:47  mjmaloney
*  First working implementation of EDL time stamping.
*
*  Revision 1.9  2003/11/15 20:16:32  mjmaloney
*  Use accessor methods for TransportMedium type.
*  For GOES, don't need to explicitely look for GOES, RD, and ST. The tmKey
*  in the Platform set will be the same for all three.
*
*  Revision 1.8  2003/09/12 19:48:07  mjmaloney
*  Added method StreamDataSource.tryAgainOnEOF(), which defaults to true,
*  as appropriate for sockets & serial ports. FileDataSource overrides it
*  with false, so that stream will terminate on EOF.
*
*  Revision 1.7  2003/06/17 00:34:00  mjmaloney
*  StreamDataSource implemented.
*  FileDataSource re-implemented as a subclass of StreamDataSource.
*
*  Revision 1.6  2003/06/06 01:39:20  mjmaloney
*  Datasources to handle either datasource or routingspec properties.
*  Consumers to handle delimiters consistently.
*  FileConsumer and DirectoryConsumer to handle File Name Templates.
*
*  Revision 1.5  2003/03/05 18:13:34  mjmaloney
*  Fix DR 122 - Base class method in DataSourceExec now makes association to TM.
*
*  Revision 1.4  2002/11/18 19:26:59  mjmaloney
*  Fixed FileDataSource bug.
*
*  Revision 1.3  2002/10/30 15:56:40  mjmaloney
*  Property retrieval vie PropertiesUtil.getIgnoreCase.
*
*  Revision 1.2  2001/09/14 21:17:37  mike
*  dev
*
*  Revision 1.1  2001/08/24 19:31:41  mike
*  Moved PMParser stuff to datasource package.
*  Added reference in RawMessage to performance measurements.
*  Created FileDataSource.
*
*/
package decodes.datasource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;
import java.util.Vector;
import java.util.Enumeration;
import java.util.zip.GZIPInputStream;

import ilex.util.AsciiUtil;
import ilex.util.Logger;
import ilex.util.TextUtil;
import ilex.var.Variable;
import ilex.util.PropertiesUtil;
import ilex.util.EnvExpander;

import decodes.db.Database;
import decodes.db.Constants;
import decodes.db.Platform;
import decodes.db.PlatformList;
import decodes.db.TransportMedium;
import decodes.db.InvalidDatabaseException;
import decodes.db.DatabaseException;

/**
  This is the implementation of the DataSourceInterface for reading data
  out of a file. 
  <p>
  Properties for the Data Source are as follows:
  <ul>
   <li>lengthAdj - (default=-1) adjustment to header length for reading 
	   socket. Will read 'adjusted length' bytes following header.</li>
   <li>before - delimiter expected before each message</li>
   <li>after - delimiter expected after each message</li>
   <li>oldChannelRanges - (default=false) If true, then chan<100 assumed to
       be self-timed, >100 assumed to be random.</li>
   <li>header - (default "goes"), Specifies type of header for messages in
	   this file.
   <li>OneMessageFile - (default=false) If true, assume entire file is a
	   single message, meaning no delimiters needed and file length is the
	   message length.</li>
   <li>gzip - (default=false) If true, then assume file is gzipped.
       It is uncompressed as it is read.</li>
  </ul>
  <p>
*/
public class FileDataSource
    extends StreamDataSource
{
	public static final int LARGEST_MSG_LENGTH = 18000;

    File file;
	String filename;
	//String mediumType;
	//String mediumId;
	//Platform platform;   // Only used if mediumId is specified
	//TransportMedium tm;
	boolean firstCall; // Used dynamically in parsing
	boolean gzip = false;

	/** default constructor */
	public FileDataSource()
	{
		super();
        file = null;
		filename = null;
		//mediumType = null;
		//mediumId = null;
		//platform = null;
		//tm = null;
	}

	/**
	 * Pulls arguments necessary for FileDataSource.
	 * Does no validation. The file does not need to exist at the time this
	 * method is called.
	 */
	public void processDataSource()
		throws InvalidDatabaseException
	{
		super.processDataSource();
		Logger.instance().log(Logger.E_DEBUG1,
			"FileDataSource.processDataSource for '" + dbDataSource.getName()
			+ "', args='" +dbDataSource.dataSourceArg+"'");
		filename = null;
		//mediumId = null;
		//mediumType = null;
	}

	/**
	  Sets a property by name & value.
	  @param name the property name
	  @param value the property value
	  @return true if property is understood and accepted, false otherwise.
	*/
	public boolean setProperty(String name, String value)
	{
		name = name.toLowerCase();
		if (name.equalsIgnoreCase("filename")
		 || name.equalsIgnoreCase("file"))
        	filename = value;
		else if (name.equalsIgnoreCase("gzip"))
			gzip = TextUtil.str2boolean(value);
		else
			return false;
		return true;
	}

	/** Does nothing: Reopen is for sockets & serial ports, not files. */
	public boolean doReOpen() { return false; }

	/**
	  Opens the file and returns input stream.
	  @return input stream.
	*/
	public BufferedInputStream open()
		throws DataSourceException
	{
		if (filename == null)
			filename = dbDataSource.getName();
		String expFilename = EnvExpander.expand(filename);

		Logger.instance().log(Logger.E_DEBUG1,
			"FileDataSource.open for '" + dbDataSource.getName()
			+ "', args='" +dbDataSource.dataSourceArg+"', actual filename='"
			+ expFilename + "', gzip="+gzip);

		file = new File(expFilename);

		// Open the input file.
		BufferedInputStream istr = null;
		try
		{
			InputStream fis = new FileInputStream(file);
			if (gzip)
				fis = new GZIPInputStream(fis);
			istr = new BufferedInputStream(fis);
		}
		catch(FileNotFoundException e)
		{
			throw new DataSourceException("Cannot open file '" + file.getName()
				+ "': " + e);
		}
		catch(IOException e)
		{
			throw new DataSourceException("Error opening file '"+file.getName()
				+ "': " + e);
		}

		firstCall = true;
		return istr;
	}

	/**
	  Closes the input file.
	  @param istr the input stream.
	*/
	public void close(BufferedInputStream istr)
	{
		if (istr != null)
		{
			try { istr.close(); }
			catch(Exception e) {}
		}
	}

	/**
	  Assume that when EOF is reached that the stream should exit.
	  @return false.
	*/
	public boolean tryAgainOnEOF()
	{
		return false;
	}
}
