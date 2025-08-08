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
package decodes.consumer;

import ilex.util.EnvExpander;
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

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.datasource.RawMessage;
import decodes.datasource.UnknownPlatformException;
import decodes.db.Platform;
import decodes.db.TransportMedium;
import decodes.decoder.DecodedMessage;
import decodes.util.PropertySpec;

/**
  FileAppendConsumer appends data to a file in a named directory.
  It creates a new file 1) if the target file cannot be locked or
  2) the file reaches a specified maximum size ( defined by property
  'maxfilesize' ).
*/
public class FileAppendConsumer extends DataConsumer
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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

	private PropertySpec[] myspecs = new PropertySpec[]
	{
		new PropertySpec("outputfilenameprefix", PropertySpec.STRING,
			"(default=stdmsg) prefix for file names"),
		new PropertySpec("maxfilesize", PropertySpec.INT,
			"Maximum allowable file size (default=no limit).")
	};

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
	public void open(String consumerArg, Properties props) throws DataConsumerException
	{
		directoryNameTemplate = consumerArg;
		this.props = props;
		filenamePrefix = PropertiesUtil.getIgnoreCase(props, "outputfilenameprefix");
		if (filenamePrefix == null)
			filenamePrefix = "stdmsg";
		filenameTemplate =  filenamePrefix + ".$DATE(yyMMdd.hhmmss)";
		maxFileSize = PropertiesUtil.getIgnoreCase(props, "maxfilesize");
		if (maxFileSize == null)
		{
			maxSize = -1;
		}
		else
		{
			maxSize = Long.valueOf(maxFileSize);
		}
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
			log.trace("Opening temp file '{}'", tempFile.getPath());


			currentFile = new FileConsumer();
			currentFile.open(tempFile.getPath(), props);
			currentFile.startMessage(msg);
		}
		catch(NullPointerException ex)
		{
			log.atError()
			   .setCause(ex)
			   .log("Cannot create output file: Cannot resolve site name.");
			currentFile = null;
		}
		catch(UnknownPlatformException | DataConsumerException ex)
		{
			log.atError()
			   .setCause(ex)
			   .log("Cannot create output file.");
			currentFile = null;
		}
	}

	public void println(String line)
	{
		if (currentFile != null)
			currentFile.println(line);
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
						catch(IOException ex)
						{
					  		log.atError().setCause(ex).log("IO Exception retriving channel lock.");
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
						int s = Integer.valueOf(fcomps[fcomps.length-1].substring(4));
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
			catch(Exception ex)
			{
				log.atError()
				   .setCause(ex)
				   .log("Cannot move '{}' to '{}'", tempFile.getPath(), path);
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

	@Override
	public PropertySpec[] getSupportedProps()
	{
		FileConsumer fc = new FileConsumer();
		PropertySpec[] fcprops = fc.getSupportedProps();
		PropertySpec[] ret = new PropertySpec[myspecs.length + fcprops.length];
		int i = 0;
		for (PropertySpec ps : fcprops)
			ret[i++] = ps;
		for (PropertySpec ps : myspecs)
			ret[i++] = ps;
		return ret;
	}

}
