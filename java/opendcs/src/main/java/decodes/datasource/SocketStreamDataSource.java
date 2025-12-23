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
import java.util.Iterator;
import java.util.Enumeration;
import java.net.Socket;
import java.io.BufferedInputStream;
import java.io.IOException;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.util.PropertiesUtil;
import ilex.util.AsciiUtil;
import ilex.util.ArrayUtil;
import ilex.var.NoConversionException;

import decodes.db.Database;
import decodes.db.Platform;
import decodes.db.Constants;
import decodes.db.DataSource;
import decodes.db.TransportMedium;
import decodes.db.NetworkList;
import decodes.db.NetworkListEntry;
import decodes.db.InvalidDatabaseException;
import decodes.db.DatabaseException;
import decodes.util.PropertySpec;


/**
  This is the implementation of the DataSourceInterface for receiving data
  over a one-way socket stream. Some DOMSAT and GOES receiver manufacturers
  implement such a real-time stream.
  <p>
  Properties for the Data Source are as follows:
  <ul>
   <li>host - Either a host name or IP address for the server. If this 
       property is not present, attempt to use the data source name as the
       host</li>
   <li>port - (default=5001) Port # on server</li>
   <li>lengthAdj - (default=-1) adjustment to header length for reading 
	   socket. Will read 'adjusted length' bytes following header.</li>
   <li>delimiter - (default " \r\n") used for finding sync'ing stream</li>
   <li>endDelimiter - (default null) marks end of message</li>
   <li>oldChannelRanges - (default=false) If true, then chan &lt; 100 assumed to
       be self-timed, &gt; 100 assumed to be random.</li>
   <li>header - (default "GOES"), either GOES or VITEL. The Vitel DRGS is
	   slightly different. It does not include the 'failure code', causing
	   subsequent fields to be shifted by one byte.
   <li>reconnect - (default=false) tells the data source to attempt a 
	   reconnect if the remote server closes the socket. This behavior has
	   been seen in the datawise domsat systems.</li>
  </ul>
  <p>
  The defaults are set up for a Vitel DRGS socket stream.
*/
public class SocketStreamDataSource extends DataSourceExec
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	String host;
	int port;
	PMParser pmp;
	int lengthAdj;
	byte[] delimiter;
	byte[] endDelimiter;
	boolean oldChannelRanges;
	Socket socket;
	BufferedInputStream inputStream;
	private boolean huntMode;
	private NetworkList myNetworkList;
	private boolean reconnect;
	private byte msgbuf[];

	static final PropertySpec[] propSpecs =
	{
		new PropertySpec("port", PropertySpec.INT, 
			"(default=5001) TCP Port to connect to"),
		new PropertySpec("host", PropertySpec.HOSTNAME, 
			"(required) Host name or IP Address to connect to"),
		new PropertySpec("header", 
			PropertySpec.DECODES_ENUM + "TransportMediumType", 
			"(default GOES) Determines the format of the message header." +
			" Legacy systems called this 'mediumType'"),
		new PropertySpec("lengthAdj", PropertySpec.INT, 
			"(default=-1) adjustment to header length for reading "
			+ "socket. Will read 'adjusted length' bytes following header."),
		new PropertySpec("delimiter", PropertySpec.STRING, 
				"(default \" \r\n\") used for finding sync'ing stream."),
		new PropertySpec("endDelimiter", PropertySpec.STRING, 
			"(default null) marks end of message. In legacy"),
		new PropertySpec("reconnect", PropertySpec.BOOLEAN,
			"(default=false) if remote server closes socket, attempt to reconnect.")
	};

	/**
	 * @see decodes.datasource.DataSourceExec#DataSourceExec(DataSource, Database) DataSourceExec Constructor
	 *
	 * @param ds data source
	 * @param db database
	 */
	public SocketStreamDataSource(DataSource ds, Database db)
	{
		super(ds,db);
		host = null;
		port = 5001;
		reconnect = false;

		lengthAdj = -1;
		delimiter = new byte[3];
		delimiter[0] = (byte)' ';
		delimiter[1] = (byte)'\r';
		delimiter[2] = (byte)'\n';
		endDelimiter = null;
		oldChannelRanges = false;
		socket = null;
		msgbuf = new byte[16000];
		try { pmp = PMParser.getPMParser(Constants.medium_Goes); }
		catch(HeaderParseException e) {} // shouldn't happen.
	}

	/**
	  Extract host, port, and username.
	  Do NOT make connection to LRGS here. Wait until first call to getMessage.
	*/
	public void processDataSource()
		throws InvalidDatabaseException
	{
		log.debug("SocketStreamDataSource.processDataSource '{}', args='{}'",
		          dbDataSource.getName(), dbDataSource.getDataSourceArg());
	}


	/**
	  This data source is about to be used.
	  Open the connection, construct composit network list.
	  <p>
	  @param routingSpecProps the routing spec properties.
	  @param since the since time from the routing spec.
	  @param until the until time from the routing spec.
	  @param networkLists contains NetworkList objects.
	  @throws DataSourceException if the source could not be initialized.
	*/
	public void init(Properties routingSpecProps, String since, String until,
		Vector<NetworkList> networkLists)
		throws DataSourceException
	{
		log.debug("SocketStreamDataSource.init() for '{}'", dbDataSource.getName());

		// Build a complete property set. Routing Spec props override DS props.
		Properties allProps = new Properties(dbDataSource.arguments);
		for(Enumeration it = routingSpecProps.propertyNames();
			it.hasMoreElements();)
		{
			String name = (String)it.nextElement();
			String value = routingSpecProps.getProperty(name);
			allProps.setProperty(name, value);
		}

		host = dbDataSource.getName();

		for(Enumeration it = allProps.propertyNames(); it.hasMoreElements();)
		{
			String name = (String)it.nextElement();
			String value = allProps.getProperty(name);

			name = name.trim().toLowerCase();

			log.debug("Processing property name='{}', value='{}'", value);

			if (name.equals("host"))
				host = value;
			else if (name.equals("port"))
			{
				try { port = Integer.parseInt(value.trim()); }
				catch(NumberFormatException ex)
				{
					throw new DataSourceException("SocketStreamDataSource '"
						+ host + "': invalid port '" + value
						+ "' - must be a number",ex);
				}
			}
			else if (name.equals("lengthadj"))
			{
				try { lengthAdj = Integer.parseInt(value.trim()); }
				catch(NumberFormatException ex)
				{
					throw new DataSourceException("SocketStreamDataSource '"
						+ host + "': invalid length adjustment '" +  value
						+ "' - must be a number",ex);
				}
			}
			else if (name.equals("delimiter"))
				delimiter = AsciiUtil.ascii2bin(value);
			else if (name.equals("enddelimiter"))
				endDelimiter = AsciiUtil.ascii2bin(value);
			else if (name.equals("header")
			      || name.equals("mediumtype"))
			{
				try { pmp = PMParser.getPMParser(value.trim()); }
				catch(HeaderParseException ex)
				{
					throw new DataSourceException("SocketStreamDataSource '"
						+ host + "': invalid header type '" +  value
						+ "' - not defined in your database: ", ex);
				}
			}
			else if (name.equals("reconnect"))
			{
				char c = value.charAt(0);
				// True or False, set reconnection option. Default is false,
				// meaning that if server hangs up, throw an exception.
				reconnect = c == 'y' || c == 'Y' || c == 't' || c == 'T';
			}
			else if ( name.equals("maxfilesize") ||
				name.equals("directoryname") ||
				name.equals("transportid") ||
				name.equals("sitename") ||
				name.equals("nwishome") ||
				name.equals("outputfilenameprefix") ||
				name.equals("outputfilename") ||
				name.equals("donedir") ||
				name.equals("dbno")	)
			{
					//	Valid but unused by this source
					continue;
			}
			else if (!name.startsWith("java.") && !name.startsWith("sun."))
			{
				log.warn("Unknown data source property '{}' ignored", name);
			}
		}

		log.trace("'{}' host='{}'", dbDataSource.getName(), host);

		log.trace("'{}' lengthAdj={}, delimiter = '{}'",
				  dbDataSource.getName(), lengthAdj, AsciiUtil.bin2ascii(delimiter));

		if (endDelimiter != null)
		{
			log.trace("'{}' endDelimiter = '{}'", dbDataSource.getName(), AsciiUtil.bin2ascii(endDelimiter));
		}

		String s = PropertiesUtil.getIgnoreCase(allProps, "OldChannelRanges");
		if (s != null)
		{
			oldChannelRanges = s.equalsIgnoreCase("true");
			log.debug("Will use old channel ranges to determine Transport Medium");
		}

		openConnection();

		myNetworkList = new NetworkList();
		for(Iterator it = networkLists.iterator(); it.hasNext(); )
		{
			NetworkList nl = (NetworkList)it.next();
			for(Iterator it2 = nl.iterator(); it2.hasNext(); )
			{
				NetworkListEntry nle = (NetworkListEntry)it2.next();
				myNetworkList.addEntry(nle);
			}
		}

		if (pmp.getHeaderLength() == -1 && endDelimiter == null)
			throw new DataSourceException("SocketStreamDataSource '"
				+ host + "': header type '" + pmp.getHeaderType()
				+ "' requires that an endDelimiter be used in the stream!");

	}

	/**
	  Closes the data source.
	  This method is called by the routing specification when the data
	  source is no longer needed.
	*/
	public void close()
	{
		if (socket != null)
		{
			try { socket.close(); }
			catch(Exception e) {}
			socket = null;
		}
		if (inputStream != null)
		{
			try { inputStream.close(); }
			catch(Exception e) {}
			inputStream = null;
		}
	}

	private void openConnection()
		throws DataSourceException
	{
		log.trace("openConnection to host '{}', port={}", host, port);

		try
		{
			close();
			socket = new Socket(host, port);
			inputStream = new BufferedInputStream(socket.getInputStream(),
				16000);
		}
		catch(Exception ex)
		{
			close();
			throw new DataSourceException("Cannot connect to Socket Stream at '"
				+ host + '/' + port, ex);
		}

		huntMode = true;
	}


	/**
	  Reads the next raw message from the data source and returns it.
	  This DataSource will fill in the message data and attempt to 
	  associate it with a TransportMedium object.

	  @return the next RawMessage from the data source.
	  @throws DataSourceTimeoutException if the data source is still
	  waiting for a message and the timeout (as defined in the properties
	  when init was called) has expired.
	  @throws DataSourceEndException if the server reports that we have
	  reached the 'until' time specified in the search criteria.
	  @throws DataSourceException if some other problem arises.
	*/
	@Override
	protected RawMessage getSourceRawMessage()
		throws DataSourceException
	{
		RawMessage ret;
		while((ret = scanForMessage()) == null)
			;

		// Now we have a message, GoesPMParser guarantees that address,
		// time, channel, & length have been successfully parsed.
		String addrField;
		int chan;
		try
		{
			addrField = ret.getPM(GoesPMParser.DCP_ADDRESS).getStringValue();
			addrField = addrField.toUpperCase();
			ret.setTimeStamp(
				ret.getPM(GoesPMParser.MESSAGE_TIME).getDateValue());
			chan = ret.getPM(GoesPMParser.CHANNEL).getIntValue();
		}
		catch(NoConversionException ex) 
		{
			throw new DataSourceException(
				"Bad header format (this should never happen)",ex);
		}

		// Assume GOES Transport Medium type, retrieve platform
		Platform p = null;
		try
		{
			p = db.platformList.getPlatform(
				Constants.medium_Goes, addrField, ret.getTimeStamp());
		}
		catch(DatabaseException ex)
		{
			byte[] data = ret.getData();
			log.atWarn().setCause(ex).log(
				"Cannot read complete platform record for message '"
				+ (new String(data, 0, data.length > 19 ? 19 : data.length)));
			p = null;
		}
		if (p != null)
		{
			ret.setPlatform(p);

			// Use the base class resolver to find exact matching TM.
			TransportMedium tm = resolveTransportMedium(p, addrField, chan,
				oldChannelRanges);
			ret.setTransportMedium(tm);
		}
		else // Couldn't find platform using TM
		{
			String msg = 
				"SocketStream-getRawMessage: No platform matching '" 
				+ addrField + "' and channel " + chan;
			if (!getAllowNullPlatform())
			{
				throw new UnknownPlatformException(msg);
			}
			else
			{
				log.warn(msg);
			}
		}

		return ret;
	}

	//=================================================================
	// Internal methods
	//=================================================================

	private RawMessage scanForMessage()
		throws DataSourceException
	{
		try
		{
			int skipped = 0;
			byte[] delimTest = new byte[delimiter.length];

			while(huntMode)
			{
				inputStream.mark(64);
				int n = inputStream.read(delimTest, 0, delimiter.length);
				if (n == -1)
					throw new DataSourceException("Socket closed by remote.");
				else if (n != delimiter.length)
				{
					// Reset stream, pause & try again later.
					inputStream.reset();
					try { Thread.sleep(50L); }
					catch(InterruptedException e) {}
				}
				else // read correct # of bytes
				{
					int i;
					for(i=0; i<delimiter.length; i++)
						if (delimiter[i] != delimTest[i])
						{
							inputStream.reset();
							inputStream.read();  // throw away 1 byte
							skipped++;
							if (skipped % 100 == 0)
								log.warn(
									"'{}' skipping lots of data, ({}) bytes -- perhaps " +
									"delimiter is not correct?",
									dbDataSource.getName(), skipped);
							break;
						}

					if (i == delimiter.length)
					{
						// Fell through, found complete delimiter.
						huntMode = false;
						log.trace("'{}' found delimiter.", dbDataSource.getName());
						inputStream.mark(100);
					}
				}
			}
			if (skipped>0)
				log.warn("'{}' skipped {} bytes before delim was found.",
						 dbDataSource.getName(), skipped);

			//
			// At this point we have read the delimiter
			//
			// >If headerLength is >0 then read header, which will then
			// >contain the message length.
			// >Else, read ahead to next occurance of delimiter & read
			// >whole message
/* MJM, not right, implemented algo is:
If endDelimiter == null, get headerlen & read that many bytes.
Else scan for end delim.
Note: This will not work if endDelim can occur in data, like (SP CR LF).
SHOULD do, what the comment says: use header len if possible, only THEN resort
to end-delim.
*/

			int headerLength = pmp.getHeaderLength();
			RawMessage ret = null;

			if (endDelimiter == null)
			{
				inputStream.mark(headerLength + 64);
				byte header[] = new byte[headerLength];
				int n = inputStream.read(header, 0, headerLength);
				if (n == -1)
					throw new DataSourceException("Socket closed by remote.");
				else if (n != headerLength)
				{
					log.debug("Complete header not ready (only {} bytes read, need {}" +
					  		  "), will try again later.",
							  n ,headerLength);
					inputStream.reset();
					try { Thread.sleep(50L); }
					catch(InterruptedException e) {}
					return null;
				}

				// Note: if GoesPMParser can parse the DCP address, time, chan
				// and length fields, we are reasonably certain we have a valid
				// header.
				ret = new RawMessage(header, header.length);
				try { pmp.parsePerformanceMeasurements(ret); }
				catch(HeaderParseException ex)
				{
					log.atError()
					   .setCause(ex)
					   .log("Failed to parse header '{}' -- skipping to next delimiter.",
					        new String(header));
					huntMode = true;
					inputStream.reset();
					return null;
				}


				// Note: GoesPMParser guarantees that length has been parsed.
				int len = -1;
				try {len=ret.getPM(GoesPMParser.MESSAGE_LENGTH).getIntValue();}
				catch(NoConversionException e) {}
				if (len < 0 || len > 16000)
				{
					log.warn("'{}', scan found invalid message length, skipping.",
							 dbDataSource.getName());
					huntMode = true;
					inputStream.reset();
					return null;
				}
	
				len += lengthAdj;
				ret.data = ArrayUtil.resize(ret.data, header.length + len);
				n = inputStream.read(ret.data, header.length, len);
			}
			else // headerlength <=0, means we have to use endDelimiter
			{
				int len = readToDelimiter(endDelimiter, msgbuf);
				log.trace("'{}' read {} bytes & then got endDelimiter.",
						  dbDataSource.getName(), len);

				// Always return to hunt mode. We need to find next start Delim.
				huntMode = true;

				if (len <= 0)
				{
					log.error("Failed frame message after {} bytes" +
					          " -- skipping to next delimiter.",
							  msgbuf.length);
					inputStream.reset();
					return null;
				}
				ret = new RawMessage(msgbuf, len);
				try { pmp.parsePerformanceMeasurements(ret); }
				catch(HeaderParseException ex)
				{
					log.atError().setCause(ex).log("Failed to parse header: -- skipping to next delimiter.");
					inputStream.reset();
					return null;
				}
			}

			String addrField = 
				ret.getPM(GoesPMParser.DCP_ADDRESS).getStringValue();
			if (myNetworkList.size() > 0
			 && myNetworkList.getEntry(addrField) == null)
			{
				log.trace("'{}'', skipping message from '{}' - not in network lists.",
						  dbDataSource.getName(), addrField);
				return null;
			}

			// Go into hunt mode to grab the next delimiter
			huntMode = true;

			return ret;
		}
		catch(IOException ex)
		{
			if (reconnect)
				openConnection();	
			throw new DataSourceException("Error reading socket: ",ex);
		}

	}


	/**
	  Reads to next occurence of delimiter, returns # chars read.
	  If buf == null then data is discarded.
	  @return number of characters read
	*/
	int readToDelimiter(byte[] delim, byte[] buf)
		throws DataSourceException
	{
		boolean delimFound = false;
		byte[] delimTest = new byte[delim.length];
		
		int buflen = 0;
		int delimIdx = 0;
		try
		{
			while(!delimFound)
			{
				//inputStream.mark(delim.length+1);
				inputStream.mark(80);
				int n = inputStream.read(delimTest, 0, delim.length);
				if (n < 0)
					throw new DataSourceException("Socket closed by remote.");
				else if (n < delim.length)
				{
					// Reset stream, pause & try again later.
					inputStream.reset();
					try { Thread.sleep(50L); }
					catch(InterruptedException e) {}
				}
				else // We did read correct # of bytes
				{
					int i;
					for(i=0; i<delim.length; i++)
						if (delim[i] != delimTest[i])
						{
							inputStream.reset();
							int x = inputStream.read();  // throw away 1 byte
							if (buf != null)
							{
								if (buflen >= buf.length)
									return -1;
								buf[buflen] = (byte)x;
							}
							buflen++;
							break;
						}
	
					if (i == delim.length)
					{
						// Fell through, found complete delimiter.
						delimFound = true;
						log.trace("'{}' found delimiter.", dbDataSource.getName());
					}
				}
			}
		}
		catch(IOException ex)
		{
			throw new DataSourceException(
				"SocketStreamDataSource '" + dbDataSource.getName() + "'", ex);
		}
		return buflen;
	}
	
	@Override
	public PropertySpec[] getSupportedProps()
	{
		return propSpecs;
	}

}
