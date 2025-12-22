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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.Vector;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.util.Iterator;
import java.util.Enumeration;
import java.util.Date;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import ilex.util.FileUtil;
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
import decodes.db.DataSource;
import decodes.db.TransportMedium;
import decodes.db.NetworkList;
import decodes.db.NetworkListEntry;
import decodes.db.InvalidDatabaseException;
import decodes.db.DatabaseException;
import decodes.util.PropertySpec;

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
   <li>oldChannelRanges - (default=false) If true, then chan &lt; 100 assumed to
       be self-timed, &gt; 100 assumed to be random.</li>
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
    private static Logger log = OpenDcsLoggerFactory.getLogger();
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

    /** Asserts GOES channels &lt; 100 are Self-Timed for backward compatibility. */
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

    /**
     * @return true if re-open should be attempted on IO error
     *
     */
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

    static final PropertySpec[] SDSprops =
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
            PropertySpec.DECODES_ENUM + "TransportMediumType",
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
            PropertySpec.JAVA_ENUM + "decodes.datasource.Parity",
            "(default none) If odd or even is specified, any characters that fail" +
            " the parity check will be replaced with '$'"),
        new PropertySpec("OneMessageFile", PropertySpec.BOOLEAN,
            "(default false) If true then the entire stream (e.g. file) is "
            + "assumed to contain a single message. This is frequently used "
            + "with MediumID and header=NoHeader."),
        new PropertySpec("fileNameTimeStamp", PropertySpec.STRING,
            "(default null) Optional file-name time stamp in format 'MMddyyyyHHmmss'."),

    };

    /**
     * @see decodes.datasource.DataSourceExec#DataSourceExec(DataSource, Database) DataSourceExec Constructor
     *
     * @param ds data source
     * @param db database
     */
    public StreamDataSource(DataSource ds, Database db)
    {
        super(ds,db);

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
        log.trace("processDataSource '{}', args='{}'", getName(), dbDataSource.getDataSourceArg());
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
    public void init(Properties routingSpecProps, String since, String until,
        Vector<NetworkList> networkLists)
        throws DataSourceException
    {
        log.debug("init() for '{}'", getName());

        // Build a complete property set. Routing Spec props override DS props.
        Properties allProps = routingSpecProps;
        if (dbDataSource != null)
        {
            allProps = new Properties(dbDataSource.arguments);
            for(Enumeration<?> it = routingSpecProps.propertyNames();
                it.hasMoreElements();)
            {
                String name = (String)it.nextElement();
                String value = routingSpecProps.getProperty(name);
                allProps.setProperty(name, value);
            }
        }

        for(Enumeration<?> it = allProps.propertyNames(); it.hasMoreElements();)
        {
            String name = (String)it.nextElement();
            String value = allProps.getProperty(name);

            name = name.trim().toLowerCase();

            boolean processed = true;
            if (name.equals("lengthadj"))
            {
                try { lengthAdj = Integer.parseInt(value.trim()); }
                catch(NumberFormatException ex)
                {
                    throw new DataSourceException("StreamDataSource '"
                        + getName()
                        + "': invalid length adjustment '" +  value
                        + "' - must be a number", ex);
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
                catch(HeaderParseException ex)
                {
                    throw new DataSourceException("StreamDataSource '"
                        + getName()
                        + "': invalid header type '" + mediumType
                        + "' - not defined in your database",ex);
                }
            }
            else if (name.equals("oldchannelranges"))
            {
                char c = value.length() > 0 ? value.charAt(0) : 'f';
                oldChannelRanges =
                    c == 'y' || c == 'Y' || c == 't' || c == 'T';
                log.debug("Stream Data Source '{}' oldChannelRanges={}", getName(), oldChannelRanges);
            }
            else if (name.equals("onemessagefile"))
                oneMessageFile = TextUtil.str2boolean(value.trim());
            else if (name.equals("mediumid"))
            {
                mediumId = EnvExpander.expand(value.trim());

            }
            else if (name.equals("filenametimestamp"))
                fileNameTimeStamp = value.trim();
            else if (name.equals("filenamedelimiter") && value != null)
                fileNameDelimiter = true;
            else if (name.equalsIgnoreCase("shefmode"))
                shefMode = TextUtil.str2boolean(value.trim());
            else if (name.equalsIgnoreCase("filename"))
            {
                savedFileName = value.trim();
            }
            else if (name.equalsIgnoreCase("paritycheck")
                  || name.equalsIgnoreCase("parity"))
                parityCheck = value.trim().toLowerCase();
            else if (name.equalsIgnoreCase("ratelimit"))
            {
                this.requestRateLimit = Integer.parseInt(value.trim());
            }
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
                    log.trace("'{}' Unknown data source property '{}' ignored.", dbDataSource.getName(), name);            
                }
            }
        }

        if( log.isTraceEnabled())
        {
            log.trace("Stream Data Source '" + getName() + "' "
            + "lengthAdj=" + lengthAdj + ", "
            + "before = '" +
                (startDelimiter != null ? AsciiUtil.bin2ascii(startDelimiter)
                : "null") + "', "
            + "after ='" +
                (endDelimiter != null ? AsciiUtil.bin2ascii(endDelimiter)
                : "null")
            + "', shefMode=" + shefMode
            + ", parityCheck=" + parityCheck
            + ", header type=" + (pmp==null?"none":pmp.getHeaderType()));
        }
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
                    "Stream Data Source '" + getName() + "' "
                    + " Header type '" + pmp.getHeaderType()
                    + "' Header Length Unknown -- endDelimiter required!");
        }

        pmp.setProperties(allProps);

        // Call subclass method to open the stream
        if (inputStream != null)
        {
            try
            {
                inputStream.close();
            }
            catch (IOException ex)
            {
                log.atError().setCause(ex).log("Unable to close input stream before reopening.");
            }
        }
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
    @Override
    protected RawMessage getSourceRawMessage()
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
                log.atWarn().setCause(ex).log("Message skipped.");
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
            log.trace("parityCheck: {}", parityCheck);
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
                            "Bad fileName format (timestamp in the file name could not be parsed)", ex);
                    }
                }
                else
                    ret.setTimeStamp(new Date());
            }

            v = ret.getPM(GoesPMParser.CHANNEL);
            if (v != null)
                chan = v.getIntValue();

        }
        catch(NoConversionException ex)
        {
            throw new DataSourceException(
                "Bad header format (this should never happen)",ex);
        }

        Platform p = null;
        try
        {

            p = db.platformList.getPlatform(
                pmp.getMediumType(), ret.getMediumId(), ret.getTimeStamp());
        }
        catch(DatabaseException ex)
        {
            String dataMsg = new String(ret.getData(), 0, (ret.getData().length > 19 ? 19 : ret.getData().length));
            log.atError()
               .setCause(ex)
               .log("Stream Data Source '{}' Cannot read complete platform record for message '{}'",
                    getName(), dataMsg);
            p = null;
        }
        if (p != null)
        {
            ret.setPlatform(p);
            // Use the base class resolver to find exact matching TM.
            if ((mediumType == null || mediumType.equalsIgnoreCase("noheader") || mediumType.equalsIgnoreCase("idstart"))
                && pmp.getMediumType() != null)
                mediumType = pmp.getMediumType();
            TransportMedium tm = resolveTransportMedium(p, ret.getMediumId(),
                chan, oldChannelRanges);
            ret.setTransportMedium(tm);
        }
        else // Couldn't find platform using TM
        {
            String msg =
                "Stream Data Source '" + getName() + "' "
                + "No platform matching '" + ret.getMediumId()+"'"
                + " with medium type '" + pmp.getMediumType() + "'";
            if (chan != Constants.undefinedIntKey)
                msg += " and channel " + chan;
            if (!getAllowNullPlatform())
            {
                throw new UnknownPlatformException(msg);
            }
            else
            {
                log.warn(msg);
            }
        }

    }

    //=================================================================
    // Internal methods
    //=================================================================

    private RawMessage scanForMessage()
        throws DataSourceException
    {
        log.trace("scanForMessage oneMessageFile={}, mediumId={}", oneMessageFile, mediumId);
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
                    catch(HeaderParseException ex)
                    {
                        log.atWarn()
                           .setCause(ex)
                           .log("StreamDataSource (shefMode) Failed to parse header '{}' " +
                                " -- skipping to next delimiter.",
                                new String(msgbuf, 0, 10));
                        huntMode = true;
                        return null;
                    }
                    return ret;
                }
                else // we just got 1st delimiter in the file, need to scan for the next one.
                {
                    log.trace("scanFM - got start of 1st shef message. Will scan for next.");
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

            log.trace("scanFM - Have start, containsExplicitLength={}", pmp.containsExplicitLength());
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
                    log.trace("StreamDataSource Complete header not ready (only {} bytes read, need {}" +
                              "), will try again later.",
                              n, headerLength);
                    if (!tryAgainOnEOF())
                        throw new DataSourceEndException("Stream EOF.");

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
                {
                    ret.setMediumId(mediumId);
                }
                if (savedFileName != null)
                    ret.setPM(GoesPMParser.FILE_NAME, new Variable(savedFileName));
                try { pmp.parsePerformanceMeasurements(ret); }
                catch(HeaderParseException ex)
                {
                    log.atWarn()
                       .setCause(ex)
                       .log("Failed to parse header '{}' -- skipping to next delimiter.",
                            new String(header));
                    huntMode = true;

                    inputStream.reset();
                    return null;
                }

                // Note: PMParser guarantees that length has been parsed.
                int len = -1;
                try {len=ret.getPM(GoesPMParser.MESSAGE_LENGTH).getIntValue();}
                catch(NoConversionException e) {}
                if (len < 0 || len > MAX_MESSAGE_LENGTH)
                {
                    log.warn("'{}', scan found invalid message length, skipping.", getName());
                    huntMode = true;

                    inputStream.reset();
                    return null;
                }

                len += lengthAdj;
                if (len < 0)
                {
                    log.warn("len={}, lengthAdj={}, headerlen={}, file skipped.", len, lengthAdj, header.length);
                    throw new DataSourceEndException("Negative msg length, file skipped.");
                }
                ret.data = ArrayUtil.resize(ret.data, header.length + len);

                // Read all message data (allow 5 sec for it all to arrive.)
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
                            log.warn("'{}', Failed to read all msg data (expected {} bytes, got {}) -- skipped.",
                                     getName(), len, n);
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
                    log.trace("'{}' read {} bytes & then got endDelimiter.", getName(), len);
                }
                else // No end delim. Scan to next start delim, then push it back.
                {
                    len = readToDelimiter(startDelimiter, msgbuf, true);
                    justGotStartDelim = true;

                    log.trace("'{}' read {} bytes & then got startDelimiter.", getName(), len);
                }

                // Always return to hunt mode. We need to find next start Delim.
                huntMode = true;

                if (len <= 0)
                {
                    log.error("Failed frame message after {} bytes -- skipping to next delimiter.", msgbuf.length);
                    return null;
                }
                ret = new RawMessage(msgbuf, len);

                // If mediumId supplied as property, set before parsing header.
                if (mediumId != null)
                    ret.setMediumId(mediumId);
                if (savedFileName != null)
                {
                    ret.setPM(GoesPMParser.FILE_NAME, new Variable(savedFileName));
                    log.trace("added PM '{}' with value '{}'",
                              GoesPMParser.FILE_NAME, ret.getPM(GoesPMParser.FILE_NAME) + "'");
                }

                try { pmp.parsePerformanceMeasurements(ret); }
                catch(HeaderParseException ex)
                {
                    log.atError().setCause(ex).log("Failed to parse header -- skipping to next delimiter.");
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
                    log.trace("StreamDataSource '{}', skipping message from '{}' - not in network lists.",
                              getName(), addr);
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
            throw new DataSourceException("IO Error: "+getName(),ex);
        }
    }


    protected void checkForMessageStart()
        throws IOException, DataSourceException
    {
        log.trace("checkForMessageStart() shefMode={}, startDelim={}, startDelimLength={}",
                  shefMode,
                  (startDelimiter==null?"null":AsciiUtil.bin2ascii(startDelimiter)),
                  (startDelimiter==null?0:startDelimiter.length));
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
                log.trace("No start delim, assuming sync.");
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
                log.trace("No delimiters, assuming sync");
                huntMode = false;
                return;
            }
        }

        if (justGotStartDelim)
        {
            justGotStartDelim = false;
            huntMode = false;
            return;
        }

        log.trace("Hunting for start delimiter...");

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
                    inputStream.reset();
                    int c = inputStream.read();  // throw away 1 byte

                    skipped++;
                    if (skipped % 100 == 0)
                        log.warn("'{}' skipping lots of data, ({}) bytes -- perhaps delimiter is not correct?",
                                 getName(), skipped);
                    break;
                }

            if (i == startDelimiter.length)
            {
                // Fell through, found complete delimiter.
                huntMode = false;
                log.trace("'{}' found delimiter.", getName());
                inputStream.mark(100);
            }
        }
        if (skipped > 4)
        {
            log.warn("'{}' {} bytes skipped. huntMode={}", getName(), skipped, huntMode);
        }
        else if (skipped > 0)
        {
            log.debug("'{}' {} bytes skipped. huntMode={}", getName(), skipped, huntMode);
        }

    }

    protected void checkForShefStart()
        throws IOException, DataSourceException
    {
        // Delimiter can be one of: \n.E<sp>  \n.ER<sp>  \n.A<sp>  \n.AR<sp>

        log.trace("Hunting for start of SHEF message...");

        inputStream.mark(10);
        byte[] delimTest = new byte[5];   // Max length of delimiter is 5 bytes.

        int n = inputStream.read(delimTest, 0, 5);
        if (n < 0)
            throw new DataSourceEndException("Stream EOF.");
        else if (n < 5)
        {
            log.trace("Stream only returned {} bytes.", n);
            for(int i=0; i<n; i++)
                msgbuf[msgbufLen++] = delimTest[i];
            huntMode = false;
            return;
        }
        else // got 5 bytes. See if it matches a delimiter.
        {
            log.trace("1st byte read '{}'", (char)delimTest[0]);
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
                log.trace("found delim number {}", delimNum);
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
     * Reads to next occurrence of delimiter and returns number of chars read.
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
        try
        {
            while(!delimFound)
            {
                inputStream.mark(80);
                int n = inputStream.read(delimTest, 0, delim.length);

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
                        log.trace("Stream '{}' found delimiter '{}'", getName(), AsciiUtil.bin2ascii(delim));
                    }
                }
            }
        }
        catch(IOException ex)
        {
            throw new DataSourceException("Unable to read stream for data source '" + getName() + "'", ex);
        }
        log.trace("StreamDS.readToDelimiter, returning buflen={}", buflen);
        if (buf != null)
            log.trace("StreamDS buf='{}'", new String(buf, 0, buflen));
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
            throw new DataSourceException("StreamDataSource '" + getName()+"'", ex);
        }

        RawMessage ret = new RawMessage(msgbuf, msgbuf.length);

        // If mediumId supplied as property, set before parsing header.
        if (mediumId != null && mediumType != null)
        {
            log.debug("oneMessageFile=true, mediumId define as '{}'", mediumId);
            ret.setMediumId(mediumId);
            ret.setPM(GoesPMParser.DCP_ADDRESS, new Variable(mediumId));
        }

        try
        {
            if (!fileNameDelimiter)
            {
                if (savedFileName != null)
                {
                    ret.setPM(GoesPMParser.FILE_NAME, new Variable(savedFileName));
                }
                pmp.parsePerformanceMeasurements(ret);
            }
        }
        catch(HeaderParseException ex)
        {
            log.atError().setCause(ex).log("Failed to parse header: -- message file is invalid.");
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

    @Override
    public String getMediumType()
    {
        return mediumType;
    }
}
