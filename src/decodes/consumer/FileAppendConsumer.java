/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.6  2013/03/28 19:19:32  mmaloney
*  User temp files are now placed under DCSTOOL_USERDIR which may be different
*  from DCSTOOL_HOME on linux/unix multi-user installations.
*
*  Revision 1.5  2012/05/15 15:10:07  mmaloney
*  Use DECODES_INSTALL_DIR, not DCSTOOL_HOME because this is in the legacy branch.
*
*  Revision 1.4  2011/01/14 21:02:06  sparab
*  Changed temporary directory from windows temp to $DCSTOOL_HOME/tmp
*
*  Revision 1.3  2008/11/20 18:49:17  mjmaloney
*  merge from usgs mods
*
*  Revision 1.1  2008/11/15 01:03:05  mmaloney
*  Moved from separate trees to common parent
*
*  Revision 1.11  2008/10/22 00:03:29  satin
*  *** empty log message ***
*
*  Revision 1.10  2008/09/02 13:20:36  satin
*  *** empty log message ***
*
*  Revision 1.9  2008/09/02 01:44:00  sedreyer
*  Changed algorithm for handling file contention.  This consumer will
*  now only append to the most recently created file when there are more
*  than 2 files in the directory; otherwise it will create a new file.
*
*  Revision 1.8  2008/08/14 22:37:37  satin
*  Added method endmessage(dbno) to allow a USGS dbno to be expanded in
*  a directory template that has the variable ${DBNO} embedded in it.
*
*  Revision 1.7  2005/04/25 21:38:08  mjmaloney
*  dev
*
*  Revision 1.6  2004/08/24 21:01:36  mjmaloney
*  added javadocs
*
*  Revision 1.5  2004/04/27 19:27:48  mjmaloney
*  compile bug fix.
*
*  Revision 1.4  2004/04/15 19:47:48  mjmaloney
*  Added status methods to support the routng status monitor web app.
*
*  Revision 1.3  2004/04/08 19:54:21  satin
*  Corrected a null pointer exception that occurs when an array of
*  files is fetched.  If there are no files to be fetched, a null pointer
*  is returned.  The old code assumed an empty array was returned and
*  tried to use the pointer causing the null pointer exception.
*
*  Revision 1.2  2003/12/04 21:17:19  satin
*  Corrected the format of a comment.
*
*  Revision 1.1  2003/12/04 21:05:48  satin
*  Initial import.
*
*  
*/
package decodes.consumer;

import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;

import decodes.datasource.RawMessage;
import decodes.datasource.UnknownPlatformException;
import decodes.db.Platform;
import decodes.db.TransportMedium;
import decodes.decoder.DecodedMessage;

/**
  FileAppendConsumer appends data to a file in a named directory.
  It creates a new file 1) if the target file cannot be locked or
  2) the file reaches a specified maximum size ( defined by property
  'maxfilesize' ).  
*/
public class FileAppendConsumer extends DataConsumer
{
	private String directoryNameTemplate;
	private String directoryName;
	private Properties props;
	private FileConsumer currentFile;
	private File tempFile;
	private File consumerPath;
	private String currentFileName;
	private String filenameTemplate;
	private String filenamePrefix;
	private Date timeStamp;
	private String maxFileSize;
	private long maxSize;
	private String curDbNo;
	private Properties systemProperties = null;
	DecimalFormat decimalFormat = new DecimalFormat("00");

	private int fileSeqNo = 0;
	/** default constructor */


	public FileAppendConsumer()
	{
		super();
		currentFile = null;
		systemProperties = System.getProperties();

	}

	/**
	  Opens and initializes the consumer.
	  @param consumerArg argument passed from routing spec.
	  @param props routing spec properties.
	  @throws DataConsumerException if the consumer could not be initialized.
	*/
	public void open(String consumerArg, Properties props)
		throws DataConsumerException
	{
		//org.apache.log4j.rolling.RollingFileAppender c = new RollingFileAppender();
		directoryNameTemplate = consumerArg;
		this.props = props;
		filenamePrefix = PropertiesUtil.getIgnoreCase(props, "outputfilenameprefix");
		if (filenamePrefix == null)
			filenamePrefix = "stdmsg";
		filenameTemplate =  filenamePrefix +
//				".$DATE(" + Constants.suffixDateFormat_fmt + ")";
				".$DATE(yyMMdd.hhmmss)";
		maxFileSize = PropertiesUtil.getIgnoreCase(props, "maxfilesize");
		if ( maxFileSize == null )
			maxSize = -1;
		else
			maxSize = new Integer(maxFileSize).longValue();
	}

	/**
	  Closes the data consumer.
	  This method is called by the routing specification when the data
	  consumer is no longer needed.
	*/
	public void close()
	{
		endMessage();
	}

	/**
	  Opens new file for output.
	  @param msg The message about to be written.
	  @throws DataConsumerException if an error occurs.
	*/
	public void startMessage(DecodedMessage msg)
		throws DataConsumerException
	{
		if (currentFile != null)
			endMessage(); // shouldn't happen!

		try
		{
			RawMessage rm = msg.getRawMessage();

			Platform p = rm.getPlatform();
			String n = p.getSiteName(false);
//			timeStamp = rm.getTimeStamp();
			timeStamp = new Date();
			TransportMedium tm = rm.getTransportMedium();
			if (tm != null)
				props.setProperty("TRANSPORTID", tm.getMediumId());

			if(n != null) props.setProperty("SITENAME", n);
			String nwisHome = systemProperties.getProperty("NWISHOME");
			if(nwisHome != null) props.setProperty("NWISHOME", nwisHome);
			currentFileName = EnvExpander.expand(directoryNameTemplate, props, timeStamp);

			String dcstool_tempdir = EnvExpander.expand("$DCSTOOL_USERDIR") + "/tmp";			
			tempFile = new File(dcstool_tempdir + currentFileName + Math.random());
			Logger.instance().log(Logger.E_DEBUG3,
				"Opening temp file '" + tempFile.getPath() + "'");

			
			currentFile = new FileConsumer();
			currentFile.open(tempFile.getPath(), props);
			currentFile.startMessage(msg);
		}
		catch(UnknownPlatformException e) 
		{
			Logger.instance().log(Logger.E_FAILURE,
				"Cannot create output file: " + e);
			currentFile = null;
		}
		catch(NullPointerException e)
		{
			Logger.instance().log(Logger.E_FAILURE,
				"Cannot create output file: Cannot resolve site name");
			currentFile = null;
		}
		catch(DataConsumerException e)
		{
			Logger.instance().log(Logger.E_FAILURE,
				"Cannot create output file: " + e);
			currentFile = null;
		}
	}

	public void printLine(String line)
	{
		if (currentFile != null)
			currentFile.printLine(line);
	}
	public void endMessage(String dbNo)
	{
		
			props.setProperty("DBNO", dbNo);
			curDbNo=dbNo;
			endMessage();
	}
	public void endMessage()
	{
		FileLock lock;
	 	RandomAccessFile raf  = null;
		FileChannel channel = null;
		String path = "";
		if (currentFile != null)
		{
			currentFile.endMessage();
			currentFile = null;
			try 
				{
				directoryName= EnvExpander.expand(directoryNameTemplate, props );
				consumerPath = new File ( directoryName );
				File[] fl = consumerPath.listFiles(new ConsumerFilenameFilter(filenamePrefix));
				if ( fl != null && fl.length > 2 ) {
					Arrays.sort(fl);
					currentFileName = fl[fl.length - 1].getName();
					path = directoryName + "/" + currentFileName;
					raf = new RandomAccessFile( path, "rw");
					if ( maxSize > 0 && raf.length() > maxSize )
					{
						lock = null;
					}
					else
					{
						channel = raf.getChannel();
						try {
							lock = channel.tryLock();  /* Try to get a lock; if can't, create new file */
						}

						catch(IOException e)  {
					  		System.out.println("IO Exception: " + e.getMessage());
						        lock = null;
						}
					}
					if ( lock == null )
						raf.close();
				}
				else
					lock = null;
				if ( lock == null )
				{
					currentFileName = EnvExpander.expand(filenameTemplate, props, timeStamp);
					path = directoryName + "/" + currentFileName;
					int k = 0;
					while  ( new File(path).exists() && k++ < 99 ) {
						String[] fcomps = path.split("\\.");
						int s = new Integer(fcomps[fcomps.length-1].substring(4));
						if ( ++s > 99 )
							s = 0;
						String sec = decimalFormat.format(s);
						path=fcomps[0];
						for (int j=1; j < fcomps.length-1; j++ ) {
							path = path + "." + fcomps[j];
						}
						path=path+"."+ fcomps[fcomps.length-1].substring(0,4)+sec;
					}
					raf = new RandomAccessFile( path, "rw");
					channel = raf.getChannel();
					lock = channel.lock();		/* Wait on lock */
				}
				raf.seek(raf.length());
				FileInputStream fis = new FileInputStream(tempFile);
				byte buf[] = new byte[4096];
				int len;
				while((len = fis.read(buf)) > 0)
					raf.write(buf, 0, len);
				if ( lock != null )
				  lock.release();
				if ( channel != null )
				  channel.close();
				raf.close();
				fis.close();
				tempFile.delete();
			}
			catch(Exception e)
			{
				Logger.instance().log(Logger.E_FAILURE,
					"Cannot move '" + tempFile.getPath() +  "' to '" + path +  "': " + e);
			}
			tempFile = null;
		}
	}

	public OutputStream getOutputStream()
		throws DataConsumerException
	{
		if (currentFile == null)
			throw new DataConsumerException("No current file in directory.");
		return currentFile.getOutputStream();
	}


	/** @return current file name. */
	public String getActiveOutput()
	{
		return currentFileName != null ? currentFileName : "(none)";
	}
	
	@Override
	public String getArgLabel()
	{
		return "File Name";
	}

}

