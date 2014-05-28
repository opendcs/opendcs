/*
*  $Id$
*
*	StreamDataSource is an abstract class for implementing file & socket
*	data sources that must read delimited messages from a one-way stream 
*	of data.
*
*  $Log$
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.14  2013/03/21 18:27:40  mmaloney
*  DbKey Implementation
*
*  Revision 1.13  2012/09/30 15:17:17  mmaloney
*  Improved debug messages.
*
*  Revision 1.12  2012/04/09 15:25:57  mmaloney
*  Added "ParityCheck" property.
*
*  Revision 1.11  2011/11/29 16:06:22  mmaloney
*  Add "filename" to msg before calling PMP
*
*  Revision 1.10  2011/09/27 01:23:08  mmaloney
*  Enhancements to StreamDataSource for SHEF and NOS Decoding.
*
*  Revision 1.9  2010/08/26 17:44:46  mmaloney
*  Only warn about skipped characters if there are 5 or more.
*
*  Revision 1.8  2010/06/15 16:03:42  mjmaloney
*  Removed debugs
*
*  Revision 1.7  2009/05/06 14:01:58  mjmaloney
*  dev
*
*  Revision 1.6  2009/04/17 17:08:26  sjagga
*  Filename timestamp is assumed to be in mmddyyyyhhmiss format and changes were made for AutoPoll directory data source
*
*  Revision 1.5  2008/11/20 18:49:18  mjmaloney
*  merge from usgs mods
*
*  Revision 1.1  2008/11/15 01:03:13  mmaloney
*  Moved from separate trees to common parent
*
*  Revision 1.15  2008/11/10 17:18:33  satin
*  *** empty log message ***
*
*  Revision 1.14  2008/11/10 15:46:19  satin
*  *** empty log message ***
*
*  Revision 1.13  2008/10/14 19:53:03  satin
*  Eliminated some logging insignificant warnings from the log.
*
*  Revision 1.12  2008/10/05 12:24:27  satin
*  Eliminated messages for unknown properties in which the property was valid
*  but not known in this particular source.
*
*  Revision 1.11  2008/08/06 12:35:39  satin
*  Pass the value of the medium id through the Environment variable
*  expander to expand any passed enviroment variables.
*
*  Revision 1.10  2008/03/25 18:16:03  mmaloney
*  Don't throw UnknownPlatformException, just skip message and get next one in
*  the stream.
*
*  Revision 1.9  2007/05/01 00:57:05  mmaloney
*  dev
*
*  Revision 1.8  2004/08/24 23:52:46  mjmaloney
*  Added javadocs.
*
*  Revision 1.7  2004/04/08 19:48:39  satin
*  Optimized the buffer allocation handling for incoming data.
*
*  Revision 1.6  2003/12/12 17:55:33  mjmaloney
*  Working implementation of DirectoryDataSource.
*
*  Revision 1.5  2003/12/07 20:36:48  mjmaloney
*  First working implementation of EDL time stamping.
*
*  Revision 1.4  2003/11/15 20:16:33  mjmaloney
*  Use accessor methods for TransportMedium type.
*  For GOES, don't need to explicitely look for GOES, RD, and ST. The tmKey
*  in the Platform set will be the same for all three.
*
*  Revision 1.3  2003/09/12 19:48:08  mjmaloney
*  Added method StreamDataSource.tryAgainOnEOF(), which defaults to true,
*  as appropriate for sockets & serial ports. FileDataSource overrides it
*  with false, so that stream will terminate on EOF.
*
*  Revision 1.2  2003/06/19 17:32:17  mjmaloney
*  Fixed parsing issue for no start pattern and end pattern that can occur in data.
*
*  Revision 1.1  2003/06/17 00:34:00  mjmaloney
*  StreamDataSource implemented.
*  FileDataSource re-implemented as a subclass of StreamDataSource.
*
*/
package decodes.datasource;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.Vector;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.Date;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import ilex.util.FileUtil;
import ilex.util.Logger;
import ilex.util.ParityCheck;
import ilex.util.PropertiesUtil;
import ilex.util.AsciiUtil;
import ilex.util.ArrayUtil;
import ilex.util.TextUtil;
import ilex.util.EnvExpander;
import ilex.var.NoConversionException;
import ilex.var.Variable;

import decodes.db.Database;
import decodes.db.Platform;
import decodes.db.Constants;
import decodes.db.TransportMedium;
import decodes.db.DataSource;
import decodes.db.NetworkList;
import decodes.db.NetworkListEntry;
import decodes.db.InvalidDatabaseException;
import decodes.db.DatabaseException;
import decodes.util.PropertySpec;

import lrgs.common.DcpMsg;

/**
  Abstract class for implementing data sources that read from a one-way
  Stream of data. The stream can be a socket, file, serial port, etc.

  <p>
  Properties processed by this abstract Data Source are as follows:
  <ul>
   <li>lengthAdj - (default=-1) adjustment to header length for reading 
	   socket. Will read 'adjusted length' bytes following header.</li>
   <li>delimiter - (default " \r\n") used for finding sync'ing stream</li>
   <li>before - synonym for 'delimiter'</li>
   <li>endDelimiter - (default null) marks end of message</li>
   <li>after - synonym for 'endDelimiter'</li>
   <li>oldChannelRanges - (default=false) If true, then chan<100 assumed to
       be self-timed, >100 assumed to be random.</li>
   <li>header - (default "GOES"), either GOES or VITEL. The Vitel DRGS is
	   slightly different. It does not include the 'failure code', causing
	   subsequent fields to be shifted by one byte.
   <li>mediumType - synonym for 'header'</li>
   <li>MediumId - If stream is all for the same platform, you can specify
	   the ID in a property. The ID in the header (if one is present) will
	   be ignored.</li>
   <li>shefMode - boolean, if true this sets the begin/end delimiters
       and enables special processing for SHEF format data</li>
   <li>ParityCheck - null (default) or "none" means do nothing, "odd" means
       do an odd-parity check, strip the parity bit, and replace any chars
       that fail the check with '$'. "even" means likewise but even parity,
       "strip" means strip the parity bits but do no checking.</li>
  </ul>
  <p>
*/
public abstract class StreamDataSource extends DataSourceExec
{
	/** Some file data sources can have quite long messages */
	public static final int MAX_MESSAGE_LENGTH = 200000;
	
	/** Parses the header found in messages in this file. */
	protected PMParser pmp;

	/** An adjustment added to the length found in the header. */
	protected int lengthAdj = -1;

	/** Byte sequence before messages in the file. */
	protected byte[] startDelimiter = null;

	/** Byte sequence after messages in the file. */
	protected byte[] endDelimiter = null;

	/** Asserts GOES channels < 100 are Self-Timed for backward compatibility. */
	protected boolean oldChannelRanges;

	/** The input stream provided by the concrete subclass. */
	protected BufferedInputStream inputStream;

	/** Network list of GOES DCP addresses to accept. */
	protected NetworkList myNetworkList;

	/** Flag meaning that the file contains a single message. */
	protected boolean oneMessageFile;
	
	/** FileName Delimiter carries the mediumID for PMParsing */
	protected boolean fileNameDelimiter;
	
	/** Optional Medium Type supplied by property. */
	String mediumType = null;

	/** Optional Medium ID supplied by property. */
	String mediumId = null;
	
    /** Optional fileNameTimeStamp supplied by property */
	String fileNameTimeStamp;

	private boolean huntMode;
	private byte msgbuf[];
	private int msgbufLen = 0;
	
	private String savedFileName = null;
	private String parityCheck = null;


	//=====================================================================
	// Abstract methods that must be provided by subclass:
	//=====================================================================

	/// Opens the input stream:
	public abstract BufferedInputStream open()
		throws DataSourceException;

	/// Close input stream, free any resources allocated.
	public abstract void close(BufferedInputStream inputStream);

	/// Return true if re-open should be attempted on IO error
	public abstract boolean doReOpen();

	private boolean startOfStream;
	private boolean justGotEndDelimiter;
	private boolean shefMode = false;
	private static final String[] shefDelims = { "\n.E ", "\n.A ", "\n.ER ", "\n.AR " };
	private char shefMsgType = 'n';
	private char prevShefMsgType = 'n';

	/**
		Sets a property from either data source or routing spec properties.
		All names are trimmed and converted to lower case before calling this
		method.  Subclass method should ignore unknown properties.
		@return true if property was process, false if it was ignored.
	*/
	public abstract boolean setProperty(String name, String value);

	private boolean firstMsg;
	private boolean justGotStartDelim = false;
	
	public static final PropertySpec[] SDSprops =
	{
		new PropertySpec("lengthAdj", PropertySpec.INT, 
			"(default=-1) adjustment to header length for reading "
			+ "socket. Will read 'adjusted length' bytes following header."),
		new PropertySpec("before", PropertySpec.STRING, 
			"(default \" \r\n\") used for finding sync'ing stream." +
			" In legacy systems this is called 'delimiter'."),
		new PropertySpec("after", PropertySpec.STRING, 
			"(default null) marks end of message. In legacy" +
			" systems this is called 'endDelimiter'"),
		new PropertySpec("header", 
			PropertySpec.JAVA_ENUM + ":decodes.datasource.PmParsers", 
			"(default GOES) Determines the format of the message header." +
			" Legacy systems called this 'mediumType'"),
		new PropertySpec("MediumId", PropertySpec.STRING, 
			"If stream is all for the same platform, you can specify "
			+ " the ID in a property. The ID in the header (if one is present) will " 
			+ "be ignored."),
		new PropertySpec("shefMode", PropertySpec.BOOLEAN, 
			"(default false) if true this sets the begin/end delimiters "
			+ "and enables special processing for SHEF format data"),
		new PropertySpec("ParityCheck", 
			PropertySpec.JAVA_ENUM + ":decodes.datasource.Parity", 
			"(default none) If odd or even is specified, any characters that fail" +
			" the parity check will be replaced with '$'"),
		new PropertySpec("OneMessageFile", PropertySpec.BOOLEAN, 
			"(default false) If true then the entire stream (e.g. file) is "
			+ "assumed to contain a single message. This is frequently used "
			+ "with MediumID and header=NoHeader."),
		new PropertySpec("fileNameTimeStamp", PropertySpec.STRING, 
			"(default null) Optional file-name time stamp in format 'MMddyyyyHHmmss'."),
			
	};

	//=====================================================================
	/**
	  No-args constructor is necessary because this is instantiated from
	  a Class object that was loaded dynamically.
	*/
	public StreamDataSource()
	{
		super();

		oldChannelRanges = false;
		msgbuf = new byte[MAX_MESSAGE_LENGTH];
		try { pmp = PMParser.getPMParser(Constants.medium_Goes); }
		catch(HeaderParseException e) {} // shouldn't happen.
		firstMsg = true;
		fileNameDelimiter = false;
		fileNameTimeStamp = null;
	}

	/**
	  No actions taken by base class for processDataSource
	*/
	public void processDataSource()
		throws InvalidDatabaseException
	{
		Logger.instance().log(Logger.E_DEBUG3, 
			"StreamDataSource.processDataSource '" + dbDataSource.getName() 
			+ "', args='" +dbDataSource.dataSourceArg+"'");
	}


	/**
	  Initialize the this data source because it is about to be used.
	  You do not need to implement the init() method in a subclass.
	  <p>This base-class method will: <p>
	  <ul>
		<li>Construct a composite property set from routing spec and data source
			properties. Routing spec will override data source properties.</li>
		<li>Call the template method setProperty with each property.</li>
		<li>Process internally managed properties like delimiters and
		    header type</li>
		<li>Construct an aggregate network list stored internally as 
			protected NetworkList myNetworkList (available to subclass).</li>
		<li>Call the template method open() to create the input stream.</li>
	  </ul>
	  <p>
	  @param routingSpecProps the routing spec properties.
	  @param since the since time from the routing spec.
	  @param until the until time from the routing spec.
	  @param networkLists contains NetworkList objects.
	  @throws DataSourceException if the source could not be initialized.
	*/
	public void initDataSource(Properties routingSpecProps, String since, String until,
		Vector<NetworkList> networkLists)
		throws DataSourceException
	{
		Logger.instance().log(Logger.E_DEBUG1, 
			"StreamDataSource.init() for '" + dbDataSource.getName() + "'");

		// Build a complete property set. Routing Spec props override DS props.
		Properties allProps = new Properties(dbDataSource.arguments);
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

//			Logger.instance().log(Logger.E_DEBUG1, 
//				"Processing property name='" + name + "', value='"+value + "'");

			boolean processed = true;
			if (name.equals("lengthadj"))
			{
				try { lengthAdj = Integer.parseInt(value.trim()); }
				catch(NumberFormatException e)
				{
					throw new DataSourceException("StreamDataSource '"
						+ dbDataSource.getName() 
						+ "': invalid length adjustment '" +  value
						+ "' - must be a number");
				}
			}
			else if (name.equals("delimiter") || name.equals("before"))
				startDelimiter = AsciiUtil.ascii2bin(value);
			else if (name.equals("enddelimiter") || name.equals("after"))
				endDelimiter = AsciiUtil.ascii2bin(value);
			else if (name.equals("header") || name.equals("mediumtype"))
			{
				mediumType = value.trim();
				try { pmp = PMParser.getPMParser(mediumType); }
				catch(HeaderParseException e)
				{
					throw new DataSourceException("StreamDataSource '"
						+ dbDataSource.getName() 
						+ "': invalid header type '" + mediumType
						+ "' - not defined in your database: " + e);
				}
			}
			else if (name.equals("oldchannelranges"))
			{
				char c = value.length() > 0 ? value.charAt(0) : 'f';
				oldChannelRanges = 
					c == 'y' || c == 'Y' || c == 't' || c == 'T';
				Logger.instance().log(Logger.E_DEBUG1, 
					"Stream Data Source '" + dbDataSource.getName() + "' "
					+ "oldChannelRanges=" + oldChannelRanges);
			}
			else if (name.equals("onemessagefile"))
				oneMessageFile = TextUtil.str2boolean(value.trim());
			else if (name.equals("mediumid"))
				mediumId = EnvExpander.expand(value.trim());
			else if (name.equals("filenametimestamp"))
				fileNameTimeStamp = value.trim();
			else if (name.equals("filenamedelimiter") && value != null)
				fileNameDelimiter = true;
			else if (name.equalsIgnoreCase("shefmode"))
				shefMode = TextUtil.str2boolean(value.trim());
			else if (name.equalsIgnoreCase("filename"))
			{
				savedFileName = value.trim();
Logger.instance().debug3("StreamDataSource savedFileName=" + savedFileName);
			}
			else if (name.equalsIgnoreCase("paritycheck")
				  || name.equalsIgnoreCase("parity"))
				parityCheck = value.trim().toLowerCase();
			else
				processed = false;

			if (!setProperty(name, value) && !processed)
			{
				if (!(  name.equals("maxfilesize") ||
                name.equals("directoryname") ||
                name.equals("transportid") ||
                name.equals("sitename") ||
                name.equals("nwishome") ||
                name.equals("outputfilenameprefix") ||
                name.equals("outputfilename") ||
                name.equals("donedir") ||
                name.equals("dbno"))  ) 
				{ 
//					Logger.instance().debug2(
//					"Stream Data Source '" + dbDataSource.name + "' "
//					+ "Unknown data source property '" + name + "' ignored.");
				}
			}
		}

		Logger.instance().log(Logger.E_DEBUG3,
			"Stream Data Source '" + dbDataSource.getName() + "' "
			+ "lengthAdj=" + lengthAdj + ", "
			+ "before = '" + 
				(startDelimiter != null ? AsciiUtil.bin2ascii(startDelimiter)
				: "null") + "', "
			+ "after ='" +
				(endDelimiter != null ? AsciiUtil.bin2ascii(endDelimiter)
				: "null") 
			+ "', shefMode=" + shefMode
			+ ", parityCheck=" + parityCheck);

		// Construct aggregate network list.
		myNetworkList = new NetworkList();
		if (networkLists != null)
			for(Iterator<NetworkList> it = networkLists.iterator(); it.hasNext(); )
			{
				NetworkList nl = it.next();
				for(Iterator<NetworkListEntry> it2 = nl.iterator(); it2.hasNext(); )
				{
					NetworkListEntry nle = it2.next();
					myNetworkList.addEntry(nle);
				}
			}

		if (!oneMessageFile && !shefMode)
		{
			if (pmp.getHeaderLength() == -1 && endDelimiter == null)
				throw new DataSourceException(
					"Stream Data Source '" + dbDataSource.getName() + "' "
					+ " Header type '" + pmp.getHeaderType()
					+ "' Header Length Unknown -- endDelimiter required!");
		}
		
		pmp.setProperties(routingSpecProps);

		// Call subclass method to open the stream
		inputStream = open();
		startOfStream = true;
		huntMode = true;
		justGotEndDelimiter = false;
		firstMsg = true;
		justGotStartDelim = false;
	}

	/**
	  Reads the next raw message from the data source and returns it.
	  This DataSource will fill in the message data and attempt to 
	  associate it with a TransportMedium object.

	  @throws DataSourceTimeoutException if the data source is still
	  waiting for a message and the timeout (as defined in the properties
	  when init was called) has expired.
	  @throws DataSourceEndException if the server reports that we have
	  reached the 'until' time specified in the search criteria, or if this
	  is a file data source and we reach the end-of-file.

	  @return the next RawMessage from the data source.
	
	  @throws DataSourceException if some other problem arises.
	*/
	public RawMessage getRawMessage()
		throws DataSourceException
	{
		RawMessage ret;

		do
		{
			while((ret = scanForMessage()) == null)
				;

			try
			{
				makePlatformAssociation(ret);
			}
			catch(UnknownPlatformException ex)
			{
				Logger.instance().warning("Message skipped: " + ex);
				ret = null;
			}
		} while(ret == null);

		ret.dataSourceName = getName();
		char pc = 
			parityCheck == null || parityCheck.equals("none") ? 'n' :
			parityCheck.equals("odd") ? 'o' :
			parityCheck.equals("even") ? 'e' :
			parityCheck.equals("strip") ? 's' : 'n';
		if (pc != 'n')
		{
			Logger.instance().debug3("StreamDataSource parityCheck: " + parityCheck);
			for(int idx = ret.getHeaderLength(); idx < ret.data.length; idx++)
			{
				int c = ret.data[idx] & 0xff;
				boolean isOdd = ParityCheck.isOddParity(c);
				if ((pc == 'o' && !isOdd)
				 || (pc == 'e' && isOdd))
					c = '$';
				else
					c &= 0x7F;
				ret.data[idx] = (byte)c;
			}
		}

		return ret;
	}

	/**
	  Using info parse from the header, associate this raw message with a
	  platform and transport medium in the database.
	  Subclass may (but probably doesn't need to) override this method.
	*/
	protected void makePlatformAssociation(RawMessage ret)
		throws DataSourceException
	{
		// PMParser guarantees setting of medium ID and MESSAGE_TIME. 
		// Use this to associate to a platform.
		int chan = Constants.undefinedIntKey; // If GOES message, also get this.
		try
		{
			Variable v = ret.getPM(GoesPMParser.MESSAGE_TIME);
 
			if (v != null)
				ret.setTimeStamp(v.getDateValue());
			else
			{
				if (fileNameTimeStamp != null)
				{
					try 
					{
						SimpleDateFormat timeFormat = new SimpleDateFormat("MMddyyyyHHmmss");
						ret.setTimeStamp(timeFormat.parse(fileNameTimeStamp));
					}
					catch(ParseException ex)
					{
						throw new DataSourceException(
							"Bad fileName format (timestamp in the file name could not be parsed)");
					}
				}
				else
					ret.setTimeStamp(new Date());
			}
			
			v = ret.getPM(GoesPMParser.CHANNEL);
			if (v != null)
				chan = v.getIntValue();
 
		}
		catch(NoConversionException e) 
		{
			throw new DataSourceException(
				"Bad header format (this should never happen)");
		}

		Platform p = null;
		try
		{
			p = Database.getDb().platformList.getPlatform(
				pmp.getMediumType(), ret.getMediumId(), ret.getTimeStamp());
		}
		catch(DatabaseException e)
		{ 
			String msg = 
				"Stream Data Source '" + dbDataSource.getName() + "' "
				+ "Cannot read complete platform record for message '"
				+ new String(ret.getData(), 0, 
				(ret.getData().length > 19 ? 19 : ret.getData().length))
				+ "': " + e;
			Logger.instance().log(Logger.E_WARNING, msg);
			p = null;
		}
		if (p != null)
		{
			ret.setPlatform(p);

			// Use the base class resolver to find exact matching TM.
			TransportMedium tm = resolveTransportMedium(p, ret.getMediumId(), 
				chan, oldChannelRanges);
			ret.setTransportMedium(tm);

		}
		else // Couldn't find platform using TM
		{
			String msg = 
				"Stream Data Source '" + dbDataSource.getName() + "' "
				+ "No platform matching '" + ret.getMediumId()+"'"
				+ " with medium type '" + pmp.getMediumType() + "'";
			if (chan != Constants.undefinedIntKey)
				msg += " and channel " + chan;
			Logger.instance().log(Logger.E_WARNING, msg);
			if (!getAllowNullPlatform())
				throw new UnknownPlatformException(msg);
		}

	}

	//=================================================================
	// Internal methods
	//=================================================================

	private RawMessage scanForMessage()
		throws DataSourceException
	{
		if (oneMessageFile)
			return getEntireFileAsMessage();
		try
		{
			RawMessage ret = null;

			msgbufLen = 0;
			while(huntMode)
				checkForMessageStart();

			if (shefMode)
			{
				huntMode = true;
				if (!startOfStream)
				{
					// then there is a shef msg in the buffer.
					ret = new RawMessage(msgbuf, msgbufLen);
					
					// the scanner found the start of the _next_ msg. the prev msg type
					// gets associated with this message.
					ret.setPM(ShefPMParser.PM_MESSAGE_TYPE, new Variable(prevShefMsgType));
	
					try { pmp.parsePerformanceMeasurements(ret); }
					catch(HeaderParseException e)
					{
						Logger.instance().log(Logger.E_WARNING,
							"StreamDataSource (shefMode) Failed to parse header '"
							+ new String(msgbuf, 0, 10) + "': " + e.toString() 
							+ " -- skipping to next delimiter.");
						huntMode = true;
						return null;
					}
					return ret;
				}
				else // we just got 1st delimiter in the file, need to scan for the next one.
				{
					startOfStream = false;
					return null;
				}
			}
			
			startOfStream = false;
			justGotEndDelimiter = false;

			// At this point we have read the delimiter

			// If headerLength contains an explicit length, then read
			// that many characters.
			// Else, read ahead to end-delimiter.

			int headerLength = pmp.getHeaderLength();

			if (pmp.containsExplicitLength())
			{
				inputStream.mark(headerLength + 64);
				byte header[] = new byte[headerLength];
				int n = inputStream.read(header, 0, headerLength);
				if (n == -1)
					throw new DataSourceEndException("Stream EOF.");
				else if (n != headerLength)
				{
					if (!tryAgainOnEOF())
						throw new DataSourceEndException("Stream EOF.");
					Logger.instance().log(Logger.E_DEBUG2,
					  "StreamDataSource Complete header not ready (only " 
					  + n + " bytes read, need " + headerLength
					  + "), will try again later.");
					if (!tryAgainOnEOF())
						throw new DataSourceEndException("Stream EOF.");
Logger.instance().debug3("StreamDS reset 3");

					inputStream.reset();
					try { Thread.sleep(50L); }
					catch(InterruptedException e) {}
					return null;
				}

				// Note: if PMParser can parse the crucial header fields
				// (e.g. DCP address, time, chan and length), then we are 
				// reasonably certain we have a valid header.
				ret = new RawMessage(header, header.length);

				// If mediumId supplied as property, set before parsing header.
				if (mediumId != null)
					ret.setMediumId(mediumId);
				if (savedFileName != null)
					ret.setPM(GoesPMParser.FILE_NAME, new Variable(savedFileName));
				try { pmp.parsePerformanceMeasurements(ret); }
				catch(HeaderParseException e)
				{
					Logger.instance().log(Logger.E_WARNING,
						"StreamDataSource Failed to parse header '"
						+ new String(header) + "': " + e.getMessage() 
						+ " -- skipping to next delimiter.");
					huntMode = true;
Logger.instance().debug3("StreamDS reset 4");

					inputStream.reset();
					return null;
				}

				// Note: PMParser guarantees that length has been parsed.
				int len = -1;
				try {len=ret.getPM(GoesPMParser.MESSAGE_LENGTH).getIntValue();}
				catch(NoConversionException e) {}
				if (len < 0 || len > MAX_MESSAGE_LENGTH)
				{
					Logger.instance().log(Logger.E_WARNING, 
						"StreamDataSource '" + dbDataSource.getName() 
						+ "', scan found invalid message length, skipping.");
					huntMode = true;
Logger.instance().debug3("StreamDS reset 5");

					inputStream.reset();
					return null;
				}
	
				len += lengthAdj;
				if (len < 0)
				{
					Logger.instance().warning("len=" + len + ", lengthAdj=" + lengthAdj
						+ ", headerlen=" + header.length + ", file skipped.");
					throw new DataSourceEndException("Negative msg length, file skipped.");
				}
				ret.data = ArrayUtil.resize(ret.data, header.length + len);

				// Read all message data (allow 5 sec for it all to arrive.)
Logger.instance().debug3("Reading " + len + " bytes of msg data after header.");
				long start = System.currentTimeMillis();
				n = 0;
				while(n < len)
				{
					int rr = inputStream.read(
						ret.data, header.length + n, len - n);
					if (rr == -1)
						throw new DataSourceEndException("Stream EOF.");
					n += rr;
					if (n < len)
					{
						if (!tryAgainOnEOF())
							throw new DataSourceEndException("Stream EOF.");
						if (System.currentTimeMillis() - start > 5000L)
						{
							Logger.instance().log(Logger.E_WARNING, 
								"StreamDataSource '" + dbDataSource.getName() 
								+ "', Failed to read all msg data (expected "
								+ len + " bytes, got " + n + ") -- skipped.");
							huntMode = true;
							return null;
						}
						else
						{
							try { Thread.sleep(100L); }
							catch(InterruptedException ex) {}
						}
					}
				}

				// If there is an end-delimiter, gobble it.
				if (endDelimiter != null)
				{
					try 
					{
						readToDelimiter(endDelimiter, null, false); 
						justGotEndDelimiter = true;
					}
					catch(Exception ex) {}
				}
			}
			else // No explicit length in header: we have to use endDelimiter.
			{
				int len = 0;
				if (endDelimiter != null && endDelimiter.length > 0)
				{
					len = readToDelimiter(endDelimiter, msgbuf, false);
					justGotEndDelimiter = true;
					Logger.instance().log(Logger.E_DEBUG3,
						"StreamDataSource '" + dbDataSource.getName() 
						+ "' read " + len + " bytes & then got endDelimiter.");
				}
				else // No end delim. Scan to next start delim, then push it back.
				{
Logger.instance().debug3("No end delim, looking for next start delim '"
+ new String(startDelimiter) + "'");
					len = readToDelimiter(startDelimiter, msgbuf, true);
					justGotStartDelim = true;
					Logger.instance().log(Logger.E_DEBUG3,
						"StreamDataSource '" + dbDataSource.getName() 
						+ "' read " + len + " bytes & then got startDelimiter.");
				}
				
				// Always return to hunt mode. We need to find next start Delim.
				huntMode = true;

				if (len <= 0)
				{
					Logger.instance().log(Logger.E_FAILURE,
						"StreamDataSource Failed frame message after "
						+ msgbuf.length 
						+ " bytes -- skipping to next delimiter.");
Logger.instance().debug3("StreamDS reset 6");
//					inputStream.reset();
					return null;
				}
				ret = new RawMessage(msgbuf, len);

				// If mediumId supplied as property, set before parsing header.
				if (mediumId != null)
					ret.setMediumId(mediumId);
				if (savedFileName != null)
				{
					ret.setPM(GoesPMParser.FILE_NAME, new Variable(savedFileName));
Logger.instance().debug3("StreamDataSource added PM '" + GoesPMParser.FILE_NAME
+ "' with value '" + ret.getPM(GoesPMParser.FILE_NAME) + "'");
				}

				try { pmp.parsePerformanceMeasurements(ret); }
				catch(HeaderParseException e)
				{
					Logger.instance().log(Logger.E_FAILURE,
						"StreamDataSource Failed to parse header: "
						+ e.toString() + " -- skipping to next delimiter.");
Logger.instance().debug3("StreamDS reset 7");
//					inputStream.reset();
					return null;
				}
			}

			Variable addrVar = ret.getPM(GoesPMParser.DCP_ADDRESS);
			if (addrVar != null)
			{
				String addr= addrVar.getStringValue();
				if (myNetworkList.size() > 0
				 && myNetworkList.getEntry(addr) == null)
				{
					Logger.instance().log(Logger.E_DEBUG2, 
						"StreamDataSource '" + dbDataSource.getName() 
						+ "', skipping message from '" + addr
						+ "' - not in network lists.");
					return null;
				}
			}

			// Go into hunt mode to grab the next delimiter
			huntMode = true;

			return ret;
		}
		catch(IOException ex)
		{
			if (doReOpen())
			{
				if (inputStream != null)
					close();
				inputStream = open();	
			}
			String msg = "StreamDataSource '" + dbDataSource.getName() 
				+ "', IO Error: " + ex;
			Logger.instance().log(Logger.E_WARNING, msg);
			throw new DataSourceException(msg);
		}
	}


	protected void checkForMessageStart()
		throws IOException, DataSourceException
	{
		if (shefMode)
		{
			checkForShefStart();
			return;
		}
		int skipped = 0;

		// No start delimiter specified?
		if (startDelimiter == null || startDelimiter.length == 0)
		{
			// Assume we're in sync if I'm at the start or just read end delim.
			if (startOfStream || justGotEndDelimiter)
			{
				Logger.instance().debug3("StreamDS - No start delim, assuming sync.");
				huntMode = false;
				return;
			}
			else if (endDelimiter != null && endDelimiter.length > 0)
			{
				// Skip ahead to next end delimiter.
				int n = readToDelimiter(endDelimiter, null, false);
				inputStream.mark(64);
				justGotEndDelimiter = true;
				huntMode = false;
				return;
			}
			else
			{
				Logger.instance().log(Logger.E_DEBUG3, 
					"No delimiters, assuming sync");
				huntMode = false;
				return;
			}
		}

		if (justGotStartDelim)
		{
			justGotStartDelim = false;
			return;
		}

		Logger.instance().log(Logger.E_DEBUG3, "Hunting for start delimiter...");

		inputStream.mark(64);
		byte[] delimTest = new byte[startDelimiter.length];
		int n = inputStream.read(delimTest, 0, startDelimiter.length);
		if (n == -1)
			throw new DataSourceEndException("Stream closed.");
		else if (n != startDelimiter.length)
		{
			if (!tryAgainOnEOF())
			{
				throw new DataSourceEndException("Stream EOF.");
			}
			// Reset stream, pause & try again later.
Logger.instance().debug3("StreamDS reset 8");
			inputStream.reset();
			try { Thread.sleep(50L); }
			catch(InterruptedException e) {}
		}
		else // read correct # of bytes
		{
			int i;
			for(i=0; i<startDelimiter.length; i++)
				if (startDelimiter[i] != delimTest[i])
				{
Logger.instance().debug3("StreamDS reset 9");
					inputStream.reset();
					int c = inputStream.read();  // throw away 1 byte
Logger.instance().debug3(" threw away '" + (char)c + "' val=" + c);
					
					skipped++;
					if (skipped % 100 == 0)
						Logger.instance().log(Logger.E_WARNING, 
							"Stream '" + dbDataSource.getName() 
							+ "' skipping lots of data, ("
							+ skipped + ") bytes -- perhaps "
							+ "delimiter is not correct?");
					break;
				}

			if (i == startDelimiter.length)
			{
				// Fell through, found complete delimiter.
				huntMode = false;
				Logger.instance().log(Logger.E_DEBUG3, 
					"Stream '" + dbDataSource.getName() + "' found delimiter.");
				inputStream.mark(100);
			}
		}
		if (skipped>0)
			Logger.instance().log(
				skipped > 4 ? Logger.E_WARNING : Logger.E_DEBUG1, 
				"Stream '" + dbDataSource.getName() + "' " 
				+ skipped + " bytes skipped before delim was found.");

	}

	protected void checkForShefStart()
		throws IOException, DataSourceException
	{
		// Delimiter can be one of: \n.E<sp>  \n.ER<sp>  \n.A<sp>  \n.AR<sp>
		
		Logger.instance().log(Logger.E_DEBUG3, "Hunting for start of SHEF message...");
		
		inputStream.mark(10);
		byte[] delimTest = new byte[5];   // Max length of delimiter is 5 bytes.

		int n = inputStream.read(delimTest, 0, 5);
		if (n < 0)
			throw new DataSourceEndException("Stream EOF.");
		else if (n < 5)
		{
			Logger.instance().debug3("Stream only returned " + n + " bytes.");
			for(int i=0; i<n; i++)
				msgbuf[msgbufLen++] = delimTest[i];
			huntMode = false;
			return;
		}
		else // got 5 bytes. See if it matches a delimiter.
		{
			Logger.instance().debug3("1st byte read '" + (char)delimTest[0] + "'");
			int delimNum = 0;
			for(; delimNum < shefDelims.length; delimNum++)
			{
				byte[] delimBytes = shefDelims[delimNum].getBytes();
				int idx = 0;
				for(; idx < delimBytes.length; idx++)
					if (delimTest[idx] != delimBytes[idx])
						break;
				if (idx == delimBytes.length) // match? 
					break;
			}
			if (delimNum < shefDelims.length)
			{
				// Found a valid delimiter
				prevShefMsgType = shefMsgType;
				shefMsgType = shefDelims[delimNum].charAt(2); // E or A
				if (shefDelims[delimNum].length() == 4)
				{
					// We read 5 but delim is only 4, put one back.
					inputStream.reset(); // reset back 5 then re-read 4
					inputStream.read();
					inputStream.read();
					inputStream.read();
					inputStream.read();
				}
				huntMode = false;
				return;
			}
			else // no delimiter found yet
			{
				inputStream.reset();
				msgbuf[msgbufLen++] = (byte)inputStream.read();  // put 1 byte in buf
			}
		}
	}

	
	
	/**
	 * Reads to next occurence of delimiter and returns number of chars read.
	 * If buf == null then data is discarded.
	 * @param delim the delimiter to look for
	 * @param buf the buffer to place data in
	 * @param allowEOF if true, return nonempty buffer when EOF seen.
	*/
	private int readToDelimiter(byte[] delim, byte[] buf, boolean allowEOF)
		throws DataSourceException
	{
		boolean delimFound = false;
		byte[] delimTest = new byte[delim.length];
		
		int buflen = 0;
//Logger.instance().debug3("StreamDS.readToDelimiter delim='"
//+ new String(delim) + "', delim.length=" + delim.length + ", buf.length=" + buf.length);
		try
		{
			while(!delimFound)
			{
				//inputStream.mark(delim.length+1);
				inputStream.mark(80);
				int n = inputStream.read(delimTest, 0, delim.length);
				
//Logger.instance().debug3("StreamDS.readToDelimiter read " + n + " chars, delimTest='"
//+ new String(delimTest, 0, n) + "'");

				if (n < 0) // End of Stream reached
				{
					throw new DataSourceEndException("Input Stream Closed.");
				}
				else if (n < delim.length) // We read fewer than length of delimiter.
				{
					if (tryAgainOnEOF())
						// something like a socket. Wait and try again later
					{
						// Reset stream, pause & try again later.
//Logger.instance().debug3("StreamDS reset 1");
						inputStream.reset();
						try { Thread.sleep(50L); }
						catch(InterruptedException e) {}
					}
					else if (allowEOF && buflen > 0)
					{
						// We only have start delim and reading from a file.
						// Return data between last start delim and EOF.
						for(int i=0; i<n; i++)
							buf[buflen++] = delimTest[i];
						return buflen;
					}
					else
						throw new DataSourceEndException("Stream EOF.");
				}
				else // We did read correct # of bytes
				{
					int i;
					for(i=0; i<delim.length; i++)
						if (delim[i] != delimTest[i])
						{
//Logger.instance().debug3("StreamDS reset 2");
							inputStream.reset();
							int x = inputStream.read();  // throw away 1 byte
							if (buf != null)
							{
								if (buflen >= buf.length)
									return -1;
								buf[buflen] = (byte)x;
							}
							buflen++;
//Logger.instance().debug3("StreamDS added char '" + (char)x + "' to buf, buflen=" + buflen);
							break;
						}
	
					if (i == delim.length)
					{
						// Fell through, found complete delimiter.
						delimFound = true;
						Logger.instance().log(Logger.E_DEBUG3, 
							"Stream '" + dbDataSource.getName() 
							+ "' found end delimiter.");
					}
				}
			}
		}
		catch(IOException e)
		{
			String msg = "StreamDataSource.readToDelimiter '" + dbDataSource.getName() + "': " + e;
			Logger.instance().warning(msg);
			throw new DataSourceException(msg);
		}
		Logger.instance().debug3("StreamDS.readToDelimiter, returning buflen=" + buflen);
		if (buf != null)
			Logger.instance().debug3("StreamDS buf='" + new String(buf, 0, buflen) + "'");
//try { Thread.sleep(1000L); } catch(InterruptedException ex) {}
		return buflen;
	}

	public void close()
	{
		if (inputStream != null)
			close(inputStream);
		inputStream = null;
	}

	/**
	  Base class should override this method to return true or false 
	  depending on whether the stream should pause and keep trying when
	  EOF is detected.
	  Socket and serial port streams will probably return true.
	  File streams should probably return false, unless the file can
	  grow dynamically as it is being read.
	*/
	public boolean tryAgainOnEOF()
	{
		return true;
	}


	/**
	  Returns the entire stream contents as a single raw message. Only succeeds
	  on the first call. Subsequent calls will throw DataSourceEndException.
	*/
	protected RawMessage getEntireFileAsMessage()
		throws DataSourceException
	{		
		if (!firstMsg)
			throw new DataSourceEndException("Stream EOF.");
		firstMsg = false;

		try
		{
			// MJM Continue reading until end of stream or 10 seconds go by with no data.
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			FileUtil.copyStream(inputStream, baos, 10000L);
			msgbuf = baos.toByteArray();
		}
		catch(IOException ex)
		{
			throw new DataSourceException(
				"StreamDataSource '" + dbDataSource.getName() + "': " + ex);
		}

		RawMessage ret = new RawMessage(msgbuf, msgbuf.length);

		// If mediumId supplied as property, set before parsing header.
		if (mediumId != null)
			ret.setMediumId(mediumId);

		try 
		{
			if (!fileNameDelimiter)
			{
				if (savedFileName != null)
				{
					ret.setPM(GoesPMParser.FILE_NAME, new Variable(savedFileName));
Logger.instance().debug3("StreamDataSource added PM '" + GoesPMParser.FILE_NAME
+ "' with value '" + ret.getPM(GoesPMParser.FILE_NAME) + "'");
				}
			    pmp.parsePerformanceMeasurements(ret);
			}
		}
		catch(HeaderParseException e)
		{
			Logger.instance().log(Logger.E_FAILURE,
				"StreamDataSource Failed to parse header: "
				+ e.toString() + " -- message file is invalid.");
			return null;
		}
		return ret;
	}

	/// Used by DirectoryDataSource
	public boolean isOpen() 
	{
		return inputStream != null; 
	}
	
	@Override
	public PropertySpec[] getSupportedProps()
	{
		return PropertiesUtil.combineSpecs(super.getSupportedProps(), SDSprops);
	}


}
