/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.4  2009/05/08 14:30:13  mjmaloney
*  remove debugs
*
*  Revision 1.3  2008/11/20 18:49:18  mjmaloney
*  merge from usgs mods
*
*  Revision 1.1  2008/11/15 01:03:12  mmaloney
*  Moved from separate trees to common parent
*
*  Revision 1.18  2008/11/10 17:18:32  satin
*  *** empty log message ***
*
*  Revision 1.17  2008/11/10 15:46:18  satin
*  *** empty log message ***
*
*  Revision 1.16  2008/10/05 12:23:46  satin
*  Eliminated messages for unknown properties in which the property was valid
*  but not known in this particular source.
*
*  Revision 1.15  2004/08/31 16:31:18  mjmaloney
*  javadoc
*
*  Revision 1.14  2004/08/24 23:52:45  mjmaloney
*  Added javadocs.
*
*  Revision 1.13  2003/11/15 20:16:33  mjmaloney
*  Use accessor methods for TransportMedium type.
*  For GOES, don't need to explicitely look for GOES, RD, and ST. The tmKey
*  in the Platform set will be the same for all three.
*
*  Revision 1.12  2003/06/17 00:34:00  mjmaloney
*  StreamDataSource implemented.
*  FileDataSource re-implemented as a subclass of StreamDataSource.
*
*  Revision 1.11  2003/06/06 01:39:20  mjmaloney
*  Datasources to handle either datasource or routingspec properties.
*  Consumers to handle delimiters consistently.
*  FileConsumer and DirectoryConsumer to handle File Name Templates.
*
*  Revision 1.10  2003/03/05 18:13:34  mjmaloney
*  Fix DR 122 - Base class method in DataSourceExec now makes association to TM.
*
*  Revision 1.9  2002/10/25 19:49:04  mjmaloney
*  Fixed problems in NOAAPORT PM Parser & Socket Stream
*
*  Revision 1.8  2002/10/11 01:27:01  mjmaloney
*  Added SocketStreamDataSource and NoaaportPMParser stuff.
*
*  Revision 1.7  2002/08/29 01:33:17  mjmaloney
*  Bug fixes for Omaha
*
*  Revision 1.6  2002/06/03 15:38:59  mjmaloney
*  DR fixes.
*
*  Revision 1.5  2002/06/03 00:54:43  mjmaloney
*  dev
*
*  Revision 1.4  2002/05/21 20:50:10  mjmaloney
*  dev
*
*  Revision 1.3  2002/01/28 02:57:59  mike
*  dev
*
*  Revision 1.2  2002/01/08 22:45:35  mike
*  1st compilable version of SocketStreamDataSource.
*  Added validating code to GoesPMParser
*
*  Revision 1.1  2002/01/07 12:53:05  mike
*  interim
*
*/
package decodes.datasource;

import java.util.Properties;
import java.util.Vector;
import java.util.Iterator;
import java.util.Enumeration;
import java.net.Socket;
import java.io.BufferedInputStream;
import java.io.IOException;

import ilex.util.Logger;
import ilex.util.PropertiesUtil;
import ilex.util.AsciiUtil;
import ilex.util.ArrayUtil;
import ilex.var.NoConversionException;

import decodes.db.Database;
import decodes.db.Platform;
import decodes.db.Constants;
import decodes.db.TransportMedium;
import decodes.db.DataSource;
import decodes.db.NetworkList;
import decodes.db.NetworkListEntry;
import decodes.db.InvalidDatabaseException;
import decodes.db.DatabaseException;

import lrgs.common.DcpMsg;

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
   <li>oldChannelRanges - (default=false) If true, then chan<100 assumed to
       be self-timed, >100 assumed to be random.</li>
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
	String host;
	int port;
	PMParser pmp;
	int lengthAdj;
	byte[] delimiter;
	byte[] endDelimiter;
	boolean oldChannelRanges;
	Socket socket;
	BufferedInputStream inputStream;
	private static final String validFailureCodes = "G?WDABTUMINQ";
	private boolean huntMode;
	private NetworkList myNetworkList;
	private boolean reconnect;
	private byte msgbuf[];

	/**
	  No-args constructor is necessary because this is instantiated from
	  a Class object that was loaded dynamically.
	*/
	public SocketStreamDataSource()
	{
		super();
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
	  Extract host, port, & username.
	  Do NOT make connection to LRGS here. Wait until first call to getMessage.
	*/
	public void processDataSource()
		throws InvalidDatabaseException
	{
		Logger.instance().log(Logger.E_DEBUG1, 
			"SocketStreamDataSource.processDataSource '" + dbDataSource.getName() 
			+ "', args='" +dbDataSource.dataSourceArg+"'");
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
		Logger.instance().log(Logger.E_DEBUG1, 
			"SocketStreamDataSource.init() for '" + dbDataSource.getName() + "'");

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

			Logger.instance().log(Logger.E_DEBUG1, 
				"Processing property name='" + name + "', value='" 
				+ value + "'");

			if (name.equals("host"))
				host = value;
			else if (name.equals("port"))
			{
				try { port = Integer.parseInt(value.trim()); }
				catch(NumberFormatException e)
				{
					throw new DataSourceException("SocketStreamDataSource '"
						+ host + "': invalid port '" + value
						+ "' - must be a number");
				}
			}
			else if (name.equals("lengthadj"))
			{
				try { lengthAdj = Integer.parseInt(value.trim()); }
				catch(NumberFormatException e)
				{
					throw new DataSourceException("SocketStreamDataSource '"
						+ host + "': invalid length adjustment '" +  value
						+ "' - must be a number");
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
				catch(HeaderParseException e)
				{
					throw new DataSourceException("SocketStreamDataSource '"
						+ host + "': invalid header type '" +  value
						+ "' - not defined in your database: " + e);
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
				Logger.instance().warning(
					"Unknown data source property '" + name + "' ignored.");
			}
		}

		Logger.instance().log(Logger.E_DEBUG3,
			"SocketStreamDataSource '" + dbDataSource.getName() + "' host=" + host);

		Logger.instance().log(Logger.E_DEBUG3,
			"SocketStreamDataSource '" + dbDataSource.getName() + "' lengthAdj="
			+ lengthAdj + ", delimiter = '" + AsciiUtil.bin2ascii(delimiter)
			+ "'");

		if (endDelimiter != null)
			Logger.instance().log(Logger.E_DEBUG3,
				"SocketStreamDataSource '" + dbDataSource.getName() 
				+ "' endDelimiter = '" + AsciiUtil.bin2ascii(endDelimiter)
				+ "'");

		String s = PropertiesUtil.getIgnoreCase(allProps, "OldChannelRanges");
		if (s != null)
		{
			oldChannelRanges = s.equalsIgnoreCase("true");
			Logger.instance().log(Logger.E_DEBUG1, 
				"Will use old channel ranges to determine Transport Medium");
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
		Logger.instance().log(Logger.E_DEBUG2, 
			"SocketStreamDataSource.openConnection to host '" + host + 
			"', port=" + port);

		try
		{
			close();
			socket = new Socket(host, port);
			inputStream = new BufferedInputStream(socket.getInputStream(),
				16000);
		}
		catch(Exception e)
		{
			close();
			throw new DataSourceException("Cannot connect to Socket Stream at '"
				+ host + '/' + port + ": " + e.toString());
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
	public RawMessage getRawMessage()
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
		catch(NoConversionException e) 
		{
			throw new DataSourceException(
				"Bad header format (this should never happen)");
		}

		// Assume GOES Transport Medium type, retrieve platform
		Platform p = null;
		try
		{
			p = Database.getDb().platformList.getPlatform(
				Constants.medium_Goes, addrField, ret.getTimeStamp());
		}
		catch(DatabaseException e)
		{
			byte[] data = ret.getData();
			Logger.instance().log(Logger.E_WARNING,
				"Cannot read complete platform record for message '"
				+ (new String(data, 0, data.length > 19 ? 19 : data.length))
				+ ": " + e);
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
			Logger.instance().log(Logger.E_WARNING, msg);
			if (!getAllowNullPlatform())
				throw new UnknownPlatformException(msg);
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
								Logger.instance().log(Logger.E_WARNING, 
									"SocketStream '" + dbDataSource.getName() 
									+ "' skipping lots of data, ("
									+ skipped + ") bytes -- perhaps "
									+ "delimiter is not correct?");
							break;
						}

					if (i == delimiter.length)
					{
						// Fell through, found complete delimiter.
						huntMode = false;
						Logger.instance().log(Logger.E_DEBUG3, 
							"SocketStream '" + dbDataSource.getName() 
							+ "' found delimiter.");
						inputStream.mark(100);
					}
				}
			}
			if (skipped>0)
				Logger.instance().log(Logger.E_WARNING, 
					"SocketStream '" + dbDataSource.getName() + "' skipped " 
					+ skipped + " bytes before delim was found.");

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
					Logger.instance().log(Logger.E_DEBUG2,
					  "SocketStreamDataSource Complete header not ready (only " 
					  + n + " bytes read, need " + headerLength
					  + "), will try again later.");
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
				catch(HeaderParseException e)
				{
					Logger.instance().log(Logger.E_FAILURE,
						"SocketStreamDataSource Failed to parse header '"
						+ new String(header) + "': " + e.toString() 
						+ " -- skipping to next delimiter.");
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
					Logger.instance().log(Logger.E_WARNING, 
						"SocketStreamDataSource '" + dbDataSource.getName() 
						+ "', scan found invalid message length, skipping.");
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
				Logger.instance().log(Logger.E_DEBUG3,
					"SocketStreamDataSource '" + dbDataSource.getName() 
					+ "' read " + len + " bytes & then got endDelimiter.");

				// Always return to hunt mode. We need to find next start Delim.
				huntMode = true;

				if (len <= 0)
				{
					Logger.instance().log(Logger.E_FAILURE,
						"SocketStreamDataSource Failed frame message after "
						+ msgbuf.length 
						+ " bytes -- skipping to next delimiter.");
					inputStream.reset();
					return null;
				}
				ret = new RawMessage(msgbuf, len);
				try { pmp.parsePerformanceMeasurements(ret); }
				catch(HeaderParseException e)
				{
					Logger.instance().log(Logger.E_FAILURE,
						"SocketStreamDataSource Failed to parse header: "
						+ e.toString() + " -- skipping to next delimiter.");
					inputStream.reset();
					return null;
				}
			}

			String addrField = 
				ret.getPM(GoesPMParser.DCP_ADDRESS).getStringValue();
			if (myNetworkList.size() > 0
			 && myNetworkList.getEntry(addrField) == null)
			{
				Logger.instance().log(Logger.E_DEBUG2, 
					"SocketStreamDataSource '" + dbDataSource.getName() 
					+ "', skipping message from '" + addrField
					+ "' - not in network lists.");
				return null;
			}

			// Go into hunt mode to grab the next delimiter
			huntMode = true;

			return ret;
		}
		catch(IOException e)
		{
			if (reconnect)
				openConnection();	
			throw new DataSourceException("Error reading socket: " + e);
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
						Logger.instance().log(Logger.E_DEBUG3, 
							"SocketStream '" + dbDataSource.getName() 
							+ "' found delimiter.");
					}
				}
			}
		}
		catch(IOException e)
		{
			throw new DataSourceException(
				"SocketStreamDataSource '" + dbDataSource.getName() 
				+ "': " + e);
		}
		return buflen;
	}

}
