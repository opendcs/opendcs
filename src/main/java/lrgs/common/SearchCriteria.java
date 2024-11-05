package lrgs.common;

import java.io.File;
import java.io.Serializable;
import java.io.IOException;
import java.io.Reader;
import java.io.FileWriter;
import java.io.LineNumberReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.StringTokenizer;

import lrgs.ldds.DdsVersion;
import lrgs.ldds.SearchCritLocalFilter;

import ilex.util.IDateFormat;
import ilex.util.TextUtil;



public class SearchCriteria implements Serializable
{
    public static final String defaultName = "searchcrit";

    // ================================================
    // Attributes
    // ================================================
    private String LrgsSince;
    private String LrgsUntil;
    private String DapsSince;
    private String DapsUntil;

    private boolean ascendingTimeOnly = false;
    private boolean realtimeSettlingDelay = false;

    /**
      The constants ACCEPT, REJECT, EXCLUSIVE are used for several of
      the criteria.
    */
    public static final char UNSPECIFIED = '\0';
    public static final char ACCEPT = 'A';
    public static final char REJECT = 'R';
    public static final char EXCLUSIVE = 'O';
    public static final char YES = 'Y';
    public static final char NO = 'N';

    /**
      Set DomsatEmail to 'Y' to accept DOMSAT e-mail messages,
      'N' to filter them out, or 'O' if you only want e-mail
      messages.
    */
    public char DomsatEmail;

    /**
      Set DomsatRetrans to 'Y' to accept DOMSAT Retranmitted messages,
      'N' to filter them out, or 'O' if you only want Retransmitted
      messages.
    */
    public char Retrans;

    /**
      Set DapsStatus to 'Y' to accept DAPS Status messages,
      'N' to filter them out, or 'O' if you only want DAPS status
      messages.
    */
    public char DapsStatus;

    /**
      Set GlobalBul to 'Y' to accept DOMSAT Global Bulletins,
      'N' to filter them out, or 'O' if you only want bulletins.
      messages.
    */
    public char GlobalBul;

    /**
      Set DcpBul to 'Y' to accept DOMSAT DCP Bulletins,
      'N' to filter them out, or 'O' if you only want DCP bulletins.
      messages.
    */
    public char DcpBul;

    /**
     * NetlistNames is a vector containing String objects. Each string is
     * the name of a network list to be loaded.
     * Use the addNetworkList() method to add String objects to the vector.
     * For display and editing, access the vector methods directly.
     * Clients can manage this vector directly.
    */
    public ArrayList<String> NetlistFiles;

    /**
      The DcpNames vector holds a list of explicit DCP names
      that were entered. No processing is done in the SearchCriteria
      class on the validity of the names.

      You can manipulate the vector directly. Each object in the vector
      should implement the toString() method.
    */
    public ArrayList<String> DcpNames;

    /**
      The user can explicitely specify DCP addresses as part of the criteria.
      This vector must only hold DcpAddress objects. You can add, remove,
      and edit objects by manipulating the vector directly.
    */
    public ArrayList<DcpAddress> ExplicitDcpAddrs;

    public int channels[];
    public static final int CHANNEL_AND=0x200;
        /* This bit determines how channel specs */
        /* are combined with DCP address specs. */

    /// list of data sources to accept data from
    public static final int MAX_SOURCES = 12;
    public int sources[] = new int[MAX_SOURCES];
    public int numSources = 0;

    public char spacecraft;
    public static final char SC_EAST = 'E';
    public static final char SC_WEST = 'W';
    public static final char SC_ANY = 'A';
    public int seqStart, seqEnd;

    public static String lineSep = System.getProperty("line.separator");

    public String baudRates = null;

    public boolean single = false;

    public char parityErrors = ACCEPT;

    private SearchCritLocalFilter localFilter = null;

    /**
        Initialize empty search criteria object.
    */
    public SearchCriteria()
    {
        clear();
    }


    /**
        Initialize search criteria object by parsing the named
        file.
    */
    public SearchCriteria(File f)
        throws IOException, SearchSyntaxException
    {
        this();
        parseFile(f);
    }

    /**
     * Copy constructor
     */
    public SearchCriteria(SearchCriteria rhs)
    {
        this();
        LrgsSince = rhs.LrgsSince;
        LrgsUntil = rhs.LrgsUntil;
        DapsSince = rhs.DapsSince;
        DapsUntil = rhs.DapsUntil;
        DomsatEmail = rhs.DomsatEmail;
        Retrans = rhs.Retrans;
        DapsStatus = rhs.DapsStatus;
        GlobalBul = rhs.GlobalBul;
        DcpBul = rhs.DcpBul;

        NetlistFiles = new ArrayList<String>();
        NetlistFiles.addAll(rhs.NetlistFiles);

        DcpNames = (rhs.DcpNames == null) ? null :
            (ArrayList<String>)rhs.DcpNames.clone();

        ExplicitDcpAddrs = (rhs.ExplicitDcpAddrs == null) ? null :
            (ArrayList<DcpAddress>)rhs.ExplicitDcpAddrs.clone();

        channels = rhs.channels == null ? null : (int[])rhs.channels.clone();

        numSources = rhs.numSources;
        for(int i=0; i<numSources; i++)
            sources[i] = rhs.sources[i];

        spacecraft = rhs.spacecraft;
        seqStart = rhs.seqStart;
        seqEnd = rhs.seqEnd;
        baudRates = rhs.baudRates;
        ascendingTimeOnly = rhs.ascendingTimeOnly;
        realtimeSettlingDelay = rhs.realtimeSettlingDelay;
        single = rhs.single;
        parityErrors = rhs.parityErrors;
    }


    // ================================================
    // Search Criteria Data and Access Functions
    // ================================================
    /**
      Set the LRGS 'since' time. This is the lower limit for LRGS time.
      LRGS time is the time that the message was received and time-tagged
      by the LRGS.

      Call this method with a null argument to cancel the since criterion.
    */
    public void setLrgsSince(String since)
    {
        if (since == null || since.length() == 0)
        {
            LrgsSince = null;
        }
        else
        {
            LrgsSince = since;
        }
    }

    /**
      Return string containing 'since' limit for LRGS time. Return null
      if no limit has been set.
    */
    public String getLrgsSince()
    {
        return LrgsSince;
    }

    /**
     * Set the ascendingTimeOnly variable. This is a boolean that specifies
     * if the data should come in in Ascending order. The default is false as
     * it is more efficient to retreive messages from newest to oldest.
     */
    public void setAscendingTimeOnly(boolean ascending)
    {
        ascendingTimeOnly=ascending;
    }

    /**
     * Returns the value of ascendingTimeOnly which represents weither the
     * data is retreived in ascending order from Oldest to Newest. Default
     * is false.
     */
    public boolean getAscendingTimeOnly()
    {
        return ascendingTimeOnly;
    }

    /**
     * Sets value for realtimeSettlingDelay which if true will allow a
     * short delay on retreiving incoming data to minimise duplicate
     * duplicate messages received by the system
     */
    public void setRealtimeSettlingDelay(boolean realtime)
    {
        realtimeSettlingDelay=realtime;
    }

    /**
     * Returns value for realtimeSettlingDelay. if True there is a
     * momentary delay which prevents the retreival of multiple
     * messages.
     */
    public boolean getRealtimeSettlingDelay()
    {
        return realtimeSettlingDelay;
    }

    /**
      Evaluate the string containing the since time (which may be
      expressed as a relative time to 'now'.
    */
    public Date evaluateLrgsSinceTime() throws SearchSyntaxException
    {
        if (LrgsSince == null)
        {
            return null;
        }
        try
        {
            return IDateFormat.parse(LrgsSince);
        }
        catch(Exception ex)
        {
            throw new SearchSyntaxException("Unable to evaluate Lrgs Since Time.",
                LrgsErrorCode.DBADSINCE, ex);
        }
    }

    /**
      LrgsUntil is the upper limit for LRGS time. LRGS time is the
      time that the message was received and time-tagged by the LRGS.
    */
    public void setLrgsUntil(String until)
    {
        if (until == null || until.length() == 0)
        {
            LrgsUntil = null;
        }
        else
        {
            LrgsUntil = until;
        }
    }

    /**
      Return string containing 'until' limit for LRGS time. Return null
      if no limit has been set.
    */
    public String getLrgsUntil()
    {
        return LrgsUntil;
    }

    /**
      Evaluate the string containing the until time (which may be
      expressed as a relative time to 'now'.
    */
    public Date evaluateLrgsUntilTime() throws SearchSyntaxException
    {
        if (LrgsUntil == null)
        {
            return null;
        }
        try
        {
            return IDateFormat.parse(LrgsUntil);
        }
        catch(Exception ex)
        {
            throw new SearchSyntaxException("Unable to evaluate Lrgs Until Time.",
                LrgsErrorCode.DBADUNTIL, ex);
        }
    }


    /**
      DapsSince is the lower limit for DAPS time. DAPS time is the
      time that the message was received and time-tagged by DAPS
      in Wallops, VA.
    */
    public void setDapsSince(String since)
    {
        if (since == null || since.length() == 0)
        {
            DapsSince = null;
        }
        else
        {
            DapsSince = since;
        }
    }

    /**
      Return string containing 'until' limit for LRGS time. Return null
      if no limit has been set.
    */
    public String getDapsSince()
    {
        return DapsSince;
    }

    /**
      Evaluate the string containing the until time (which may be
      expressed as a relative time to 'now'.
    */
    public Date evaluateDapsSinceTime() throws SearchSyntaxException
    {
        if (DapsSince == null)
        {
            return null;
        }
        try
        {
            return IDateFormat.parse(DapsSince);
        }
        catch(Exception ex)
        {
            throw new SearchSyntaxException("Unable to evaluate Daps Since Time.",
                LrgsErrorCode.DBADSINCE, ex);
        }
    }

    /**
      DapsUntil is the upper limit for DAPS time. DAPS time is the
      time that the message was received and time-tagged by DAPS in Wallops, VA.
    */
    public void setDapsUntil(String until)
    {
        if (until == null || until.length() == 0)
        {
            DapsUntil = null;
        }
        else
        {
            DapsUntil = until;
        }
    }

    /**
      Return string containing 'until' limit for LRGS time. Return null
      if no limit has been set.
    */
    public String getDapsUntil()
    {
        return DapsUntil;
    }

    /**
      Evaluate the string containing the until time (which may be
      expressed as a relative time to 'now'.
    */
    public Date evaluateDapsUntilTime() throws SearchSyntaxException
    {
        if (DapsUntil == null)
        {
            return null;
        }
        try
        {
            return IDateFormat.parse(DapsUntil);
        }
        catch(Exception ex)
        {
            throw new SearchSyntaxException("Unable to evaluate Daps Until Time.",
                LrgsErrorCode.DBADUNTIL, ex);
        }
    }

    /**
      Add a network list to the criteria. This method simply
      adds the passed File object to the end of the vector. It
      does not test to see if the file exists.

      It does not load DCP addresses from the file. This is
      done by the testCriteria() method.
    */
    public void addNetworkList(String f)
    {
        NetlistFiles.add(f);
    }

    /**
      Safe method to add a string DCP name to the criteria.
    */
    public void addDcpName(String name)
    {
        DcpNames.add(name);
    }

    /**
      Safe method to add an explicit DCP address.
    */
    public void addDcpAddress(DcpAddress addr)
    {
        ExplicitDcpAddrs.add(addr);
    }


    // ================================================
    // Search Criteria Execution & Initialization Functions
    // ================================================
    public void clear()
    {
        LrgsSince = null;
        LrgsUntil = null;
        DapsSince = null;
        DapsUntil = null;
        DomsatEmail = UNSPECIFIED;
        Retrans = UNSPECIFIED;
        DapsStatus = UNSPECIFIED;
        GlobalBul = UNSPECIFIED;
        DcpBul = UNSPECIFIED;
        NetlistFiles = new ArrayList<String>();
        DcpNames = new ArrayList<String>();
        ExplicitDcpAddrs = new ArrayList<DcpAddress>();
        channels = null;
        for(int i=0; i<MAX_SOURCES; i++)
            sources[i] = 0;
        numSources = 0;
        spacecraft =SC_ANY;
        seqStart = -1;
        seqEnd = -1;
        baudRates = null;
        ascendingTimeOnly = false;
        realtimeSettlingDelay = false;
        single = false;
        parityErrors = ACCEPT;
    }

    /// Returns true if no values have been set since the last clear.
    public boolean isEmpty()
    {
        if (LrgsSince != null
         || LrgsUntil != null
         || DapsSince != null
         || DapsUntil != null
         || DomsatEmail != UNSPECIFIED
         || Retrans != UNSPECIFIED
         || DapsStatus != UNSPECIFIED
         || GlobalBul != UNSPECIFIED
         || DcpBul != UNSPECIFIED)
        {
            return false;
        }

        if (NetlistFiles.size() > 0
         || DcpNames.size() > 0
         || ExplicitDcpAddrs.size() > 0
         || channels != null)
        {
            return false;
        }

        for(int i=0; i<MAX_SOURCES; i++)
        {
            if (sources[i] != 0)
            {
                return false;
            }
        }

        if (numSources != 0)
        {
            return false;
        }

        if (spacecraft != 'a')
        {
            return false;
        }
        if (seqStart != -1 || seqEnd != -1)
        {
            return false;
        }
        if (baudRates != null)
        {
            return false;
        }

        if (ascendingTimeOnly || realtimeSettlingDelay)
        {
            return false;
        }

        return true;
    }

    /**
    * Save current search criteria in specified file. If fname is NULL
    * file will go to "~/searchcrit".
    *
    * Return true if successful, false if error.
    */
    public void saveFile(File name) throws IOException
    {
        try (FileWriter fw = new FileWriter(name))
        {
            fw.write(toString());
            fw.flush();
        }
    }

    /**
    * Read search criteria from specified file. If fname is NULL, the
    * default "~/searchcrit" will be used.
    */
    public boolean parseFile(File input) throws IOException, SearchSyntaxException
    {
        clear();
        try (FileReader reader = new FileReader(input))
        {
            return parseFile(reader);
        }
    }

    public boolean parseFile(Reader reader)
        throws IOException, SearchSyntaxException
    {
        LineNumberReader rdr = new LineNumberReader(reader);
        String ln;
        try
        {
               while( (ln = rdr.readLine()) != null)
               {
                // skip empty & comment lines.
                if (ln.length() == 0 || ln.charAt(0) == '#')
                {
                    continue;
                }

                // No space after colon? Insert one so nextToken will work.
                int idx = ln.indexOf(':');
                String allArgs = "";
                if (idx != -1)
                {
                    allArgs = ln.substring(idx+1);
                    if (ln.length() > idx && ln.charAt(idx+1) != ' ')
                    {
                        ln = ln.substring(0,idx+1) + ' ' + ln.substring(idx+1);
                    }
                }

                StringTokenizer st = new StringTokenizer(ln);
                if (!st.hasMoreTokens())
                {
                    continue;
                }

                String kw = st.nextToken();  // Keyword followed by colon
                if (kw.charAt(0) == '#') // skip comment lines.
                {
                    continue;
                }

                // Else try to parse the line.
                if (kw.equalsIgnoreCase("DRS_SINCE:")
                 || kw.equalsIgnoreCase("LRGS_SINCE:")
                 || kw.equalsIgnoreCase("DRSSINCE:")
                 || kw.equalsIgnoreCase("LRGSSINCE:"))
                {
                    setLrgsSince(ln.substring(kw.length()).trim());
                }
                else if (kw.equalsIgnoreCase("DRS_UNTIL:")
                      || kw.equalsIgnoreCase("LRGS_UNTIL:")
                      || kw.equalsIgnoreCase("DRSUNTIL:")
                      || kw.equalsIgnoreCase("LRGSUNTIL:"))
                {
                    setLrgsUntil(ln.substring(kw.length()).trim());
                }
                else if (kw.equalsIgnoreCase("DAPS_SINCE:")
                      || kw.equalsIgnoreCase("DAPSSINCE:"))
                {
                    setDapsSince(ln.substring(kw.length()).trim());
                }
                else if (kw.equalsIgnoreCase("DAPS_UNTIL:")
                      || kw.equalsIgnoreCase("DAPSUNTIL:"))
                {
                    setDapsUntil(ln.substring(kw.length()).trim());
                }
                else if (kw.equalsIgnoreCase("NETWORKLIST:")
                      || kw.equalsIgnoreCase("NETWORK_LIST:"))
                {
                    if (!st.hasMoreTokens())
                    {
                        throw new SearchSyntaxException(
                            "Expected network list name.",
                            LrgsErrorCode.DBADNLIST);
                    }
                    addNetworkList(st.nextToken());
                }
                else if (kw.equalsIgnoreCase("DCP_NAME:"))
                {
                    if (!st.hasMoreTokens())
                    {
                        throw new SearchSyntaxException("Expected DCP name.",
                            LrgsErrorCode.DBADDCPNAME);
                    }
                    addDcpName(st.nextToken());
                }
                else if (kw.equalsIgnoreCase("DCP_ADDRESS:")
                      || kw.equalsIgnoreCase("DCPADDRESS:"))
                {
                    if (!st.hasMoreTokens())
                    {
                        throw new SearchSyntaxException(
                            "Expected DCP address.", LrgsErrorCode.DBADADDR);
                    }
                    String tok = st.nextToken();
                    addDcpAddress(new DcpAddress(tok));
                }
                else if (kw.equalsIgnoreCase("ELECTRONIC_MAIL:"))
                {
                    char c = '-';
                    if (st.hasMoreTokens())
                    {
                        c = st.nextToken().charAt(0);
                    }
                    if (c != ACCEPT && c != REJECT && c != EXCLUSIVE
                     && c != YES && c != NO)
                    {
                        throw new SearchSyntaxException(
                            "Expected one of "+ACCEPT+", "+REJECT+", or "
                            +EXCLUSIVE, LrgsErrorCode.DBADEMAIL);
                    }

                    DomsatEmail = (c == YES) ? ACCEPT :
                                  (c == NO ) ? REJECT : c;
                }
                else if (kw.equalsIgnoreCase("DAPS_STATUS:"))
                {
                    char c = '-';
                    if (st.hasMoreTokens())
                    {
                        c = st.nextToken().charAt(0);
                    }
                    if (c != ACCEPT && c != REJECT && c != EXCLUSIVE
                     && c != YES && c != NO)
                    {
                        throw new SearchSyntaxException(
                            "Expected "+ACCEPT+", "+REJECT+", or "+EXCLUSIVE,
                            LrgsErrorCode.DBADDAPSSTAT);
                    }
                    DapsStatus = c;
                }
                else if (kw.equalsIgnoreCase("RETRANSMITTED:"))
                {
                    char c = '-';
                    if (st.hasMoreTokens())
                    {
                        c = st.nextToken().charAt(0);
                    }

                    if (c != ACCEPT && c != REJECT && c != EXCLUSIVE
                     && c != YES && c != NO)
                        throw new SearchSyntaxException(
                            "Expected "+ACCEPT+", "+REJECT+", or "+EXCLUSIVE,
                            LrgsErrorCode.DBADRTRAN);
                    Retrans = (c == YES) ? ACCEPT :
                                  (c == NO ) ? REJECT : c;
                }
                else if (kw.equalsIgnoreCase("GLOB_BUL:"))
                {
                    char c = '-';
                    if (st.hasMoreTokens())
                        c = st.nextToken().charAt(0);

                    if (c != ACCEPT && c != REJECT && c != EXCLUSIVE
                     && c != YES && c != NO)
                        throw new SearchSyntaxException(
                            "Expected "+ACCEPT+", "+REJECT+", or "+EXCLUSIVE,
                            LrgsErrorCode.DBADDAPSSTAT);
                    GlobalBul = (c == YES) ? ACCEPT :
                                  (c == NO ) ? REJECT : c;
                }
                else if (kw.equalsIgnoreCase("DCP_BUL:"))
                {
                    char c = '-';
                    if (st.hasMoreTokens())
                        c = st.nextToken().charAt(0);

                    if (c != ACCEPT && c != REJECT && c != EXCLUSIVE
                     && c != YES && c != NO)
                    {
                        throw new SearchSyntaxException(
                            "Expected "+ACCEPT+", "+REJECT+", or "+EXCLUSIVE,
                            LrgsErrorCode.DBADDAPSSTAT);
                    }
                    DcpBul = (c == YES) ? ACCEPT :
                             (c == NO ) ? REJECT : c;
                }
                else if (kw.equalsIgnoreCase("CHANNEL:"))
                {
                    if (!st.hasMoreTokens())
                    {
                        throw new SearchSyntaxException(
                            "Expected channel number",
                            LrgsErrorCode.DBADCHANNEL);
                    }
                    String t = st.nextToken();
                    if (t.length() == 1
                     && (t.charAt(0) == '&' || t.charAt(0) == '|')
                     && st.hasMoreTokens())
                    {
                        t = t + st.nextToken();
                    }

                    int hidx = t.indexOf("-");
                    if (hidx != -1)
                    {
                        try
                        {
                            int start = Integer.parseInt(t.substring(0, hidx));
                            int end = Integer.parseInt(t.substring(hidx+1));
                            for(int i=start; i<end; i++)
                            {
                                addChannelToken("&" + i);
                            }
                        }
                        catch(Exception ex)
                        {
                            throw new SearchSyntaxException("Bad channel range '"
                                + t + "'", LrgsErrorCode.DBADSEARCHCRIT, ex);
                        }
                    }
                    else
                    {
                        addChannelToken(t);
                    }
                }
                else if (kw.equalsIgnoreCase("SOURCE:"))
                {
                    if (!st.hasMoreTokens())
                    {
                        throw new SearchSyntaxException("Expected source name",
                            LrgsErrorCode.DNOSUCHSOURCE);
                    }
                    String t = st.nextToken();
                    int v = DcpMsgFlag.sourceName2Value(t);
                    addSource(v);
                }
                else if (kw.equalsIgnoreCase("SPACECRAFT:"))
                {
                    if (!st.hasMoreTokens())
                    {
                        throw new SearchSyntaxException("Expected E or W.",
                            LrgsErrorCode.DBADSEARCHCRIT);
                    }
                    String t = st.nextToken();
                    char c = t.charAt(0);
                    if (c == 'e' || c == 'E')
                    {
                        spacecraft = SC_EAST;
                    }
                    else if (c == 'w' || c == 'W')
                    {
                        spacecraft = SC_WEST;
                    }
                    else
                    {
                        throw new SearchSyntaxException("Bad SPACECRAFT '"
                            + c + "'", LrgsErrorCode.DBADSEARCHCRIT);
                    }
                }
                else if (kw.equalsIgnoreCase("SEQUENCE:"))
                {
                    if (!st.hasMoreTokens())
                    {
                        throw new SearchSyntaxException("Expected sequence start.",
                            LrgsErrorCode.DBADSEARCHCRIT);
                    }
                    String t = st.nextToken();
                    try
                    {
                        seqStart = Integer.parseInt(t);
                    }
                    catch(NumberFormatException ex)
                    {
                        throw new SearchSyntaxException("Non-numeric sequence start '"
                            + t + "'", LrgsErrorCode.DBADSEARCHCRIT, ex);
                    }

                    if (!st.hasMoreTokens())
                    {
                        throw new SearchSyntaxException("Expected sequence end.",
                            LrgsErrorCode.DBADSEARCHCRIT);
                    }
                    t = st.nextToken();
                    try
                    {
                        seqEnd = Integer.parseInt(t);
                    }
                    catch(NumberFormatException ex)
                    {
                        throw new SearchSyntaxException("Non-numeric sequence end '"
                            + t + "'", LrgsErrorCode.DBADSEARCHCRIT, ex);
                    }
                }
                else if (kw.equalsIgnoreCase("BAUD:"))
                {
                    if (!st.hasMoreTokens())
                    {
                        throw new SearchSyntaxException("Expected Baud Rate.",
                            LrgsErrorCode.DBADDCPNAME);
                    }
                    StringBuilder sb = new StringBuilder();
                    while(st.hasMoreTokens())
                    {
                        sb.append(st.nextToken() + " ");
                    }
                    baudRates = sb.toString().trim();
                }
                else if (kw.equalsIgnoreCase("ASCENDING_TIME:"))
                {
                    if (!st.hasMoreTokens())
                    {
                        throw new SearchSyntaxException(
                            "ASCENDING_TIME without true/false argument.",
                            LrgsErrorCode.DBADSEARCHCRIT);
                    }
                    ascendingTimeOnly = TextUtil.str2boolean(st.nextToken());
                }
                else if (kw.equalsIgnoreCase("RT_SETTLE_DELAY:"))
                {
                    if (!st.hasMoreTokens())
                    {
                        throw new SearchSyntaxException(
                            "RT_SETTLE_DELAY without true/false argument.",
                            LrgsErrorCode.DBADSEARCHCRIT);
                    }
                    realtimeSettlingDelay = TextUtil.str2boolean(st.nextToken());
                }
                else if (kw.equalsIgnoreCase("SINGLE:"))
                {
                    if (!st.hasMoreTokens())
                    {
                        throw new SearchSyntaxException(
                            "SINGLE without true/false argument.",
                            LrgsErrorCode.DBADSEARCHCRIT);
                    }
                    single = TextUtil.str2boolean(st.nextToken());
                }
                else if (kw.equalsIgnoreCase("PARITY_ERROR:"))
                {
                    char c = '-';
                    if (st.hasMoreTokens())
                    {
                        c = st.nextToken().charAt(0);
                    }
                    if (c != ACCEPT && c != REJECT && c != EXCLUSIVE)
                    {
                        throw new SearchSyntaxException(
                            "Expected "+ACCEPT+", "+REJECT+", or "+EXCLUSIVE,
                            LrgsErrorCode.DBADDAPSSTAT);
                    }
                    parityErrors = c;
                }
                else
                {
                    throw new SearchSyntaxException(
                        "Unrecognized criteria name '" + ln + "'",
                        LrgsErrorCode.DBADKEYWORD);
                }
               }
        }
        catch(IOException |SearchSyntaxException ex)
        {
            throw ex;
        }
        catch(Exception ex)
        {
            // numberformat, etc.
            throw new SearchSyntaxException(
                "Unexpected exception parsing search-criteria: " + ex,
                LrgsErrorCode.DBADSEARCHCRIT, ex);
        }
        finally
        {
            rdr.close();
        }

        return true;
    }

    public void addChannelToken(String t)
    {
        boolean and = false;
        if (t.charAt(0) == '&')
        {
            and = true;
            t = t.substring(1);
        }
        else if (t.charAt(0) == '|')
        {
            t = t.substring(1);
        }
        int chan = Integer.parseInt(t);
        if (and)
        {
            chan |= CHANNEL_AND;
        }
        if (channels == null)
        {
            channels = new int[1];
        }
        else
        {
            int tmpchan[] = new int[channels.length+1];
            for(int i = 0; i < channels.length; i++)
            {
                tmpchan[i] = channels[i];
            }
            channels = tmpchan;
        }
        channels[channels.length-1] = chan;
    }

    /**
      Return a string containing this search criteria object suitable
      for saving in a search criterie text-file.
    */
    public String toString()
    {
        // Use max value to enable all fields in output.
        return toString(Integer.MAX_VALUE);
    }

    /**
     * Return string containing this search criteria object suitable
     * for sending to a server with the specified protocol version.
     * Older servers cannot tolerate new search-crit fields.
     */
    public String toString(int protoVersion)
    {
        boolean needLocalFilter = false;

        StringBuffer ret = new StringBuffer("#\n# LRGS Search Criteria\n#\n");

        if (LrgsSince != null)
        {
            ret.append("DRS_SINCE: " + LrgsSince + lineSep);
        }
        if (LrgsUntil != null)
        {
            ret.append("DRS_UNTIL: " + LrgsUntil + lineSep);
        }
        if (DapsSince != null)
        {
            ret.append("DAPS_SINCE: " + DapsSince + lineSep);
        }
        if (DapsUntil != null)
        {
            ret.append("DAPS_UNTIL: " + DapsUntil + lineSep);
        }

        for(String nlf : NetlistFiles)
        {
            ret.append("NETWORKLIST: " + nlf + lineSep);
        }
        if (DcpNames != null)
        {
            for(int i = 0; i < DcpNames.size(); ++i)
            {
                ret.append("DCP_NAME: " + DcpNames.get(i) + lineSep);
            }
        }
        if (ExplicitDcpAddrs != null)
        {
            for(int i = 0; i < ExplicitDcpAddrs.size(); ++i)
            {
                ret.append("DCP_ADDRESS: " +
                    (DcpAddress)ExplicitDcpAddrs.get(i) + lineSep);
            }
        }
        if (DomsatEmail != UNSPECIFIED)
        {
            ret.append("ELECTRONIC_MAIL: " + DomsatEmail + lineSep);
        }
        if (DapsStatus != UNSPECIFIED)
        {
            ret.append("DAPS_STATUS: " + DapsStatus + lineSep);
        }
        if (Retrans != UNSPECIFIED)
        {
            ret.append("RETRANSMITTED: " + Retrans + lineSep);
        }
        if (GlobalBul != UNSPECIFIED)
        {
            ret.append("GLOB_BUL: " + GlobalBul + lineSep);
        }
        if (DcpBul != UNSPECIFIED)
        {
            ret.append("DCP_BUL: " + DcpBul + lineSep);
        }

        if (channels != null && channels.length > 0)
        {
            boolean individual = true;
            int start = channels[0];
            if (channels.length > 1 && (start & CHANNEL_AND) != 0)
            {
                individual = false;
                start &= (~CHANNEL_AND);
                int last = start;
                for(int i = 1; i < channels.length; ++i)
                {
                    int c = channels[i];
                    if ((c & CHANNEL_AND) == 0
                     || (c & (~CHANNEL_AND)) != last + 1)
                    {
                        individual = true;
                        break;
                    }
                    last = c & (~CHANNEL_AND);
                }
                ret.append("CHANNEL: " + start + "-" + last + lineSep);
            }
            if (individual)
            {
                for(int i = 0; i < channels.length; ++i)
                {
                    ret.append("CHANNEL: "
                        + ((channels[i] & CHANNEL_AND) != 0 ? '&' : '|')
                        + (channels[i] & 0x1ff) + lineSep);
                }
            }
        }

        boolean atLeastOneGoesTypeSpecified = false;
        boolean atLeastOneNonGoesTypeSpecified = false;
        for(int i=0; i<numSources; i++)
        {
            String sourceName = DcpMsgFlag.sourceValue2Name(sources[i]);
            if (sourceName == null)
            {
                continue;
            }

            // Don't send the new GOES source types to a pre-version 12 server.
            if (protoVersion < 12
             && (sourceName.equalsIgnoreCase("GOES_SELFTIMED")
                 || sourceName.equalsIgnoreCase("GOES_RANDOM")))
            {
                atLeastOneGoesTypeSpecified = true;
                continue;
            }
            if (sourceName.equalsIgnoreCase("NETDCP")
             || sourceName.equalsIgnoreCase("IRIDIUM")
             || sourceName.equalsIgnoreCase("OTHER"))
            {
                atLeastOneNonGoesTypeSpecified = true;
            }
            ret.append("SOURCE: " + sourceName + lineSep);
        }

        // Special case for legacy server where a goes type, e.g. GOES_SELFTIMED
        // and a non-goes type, e.g. IRIDIUM, are both specified. A legacy server
        // will ignore GOES_SELFTIME and only return IRIDIUM. Therefore we spoof
        // the server by sending all possible sources that can have GOES.
        if (protoVersion < DdsVersion.version_12
         && atLeastOneGoesTypeSpecified && atLeastOneNonGoesTypeSpecified)
        {
            ret.append("SOURCE: DOMSAT" + lineSep);
            ret.append("SOURCE: DRGS" + lineSep);
            ret.append("SOURCE: NOAAPORT" + lineSep);
            ret.append("SOURCE: LRIT" + lineSep);
            ret.append("SOURCE: DDS" + lineSep);
        }
        if (protoVersion < DdsVersion.version_12 && atLeastOneGoesTypeSpecified)
        {
            needLocalFilter = true;
        }

        if (spacecraft == SC_EAST || spacecraft == SC_WEST)
        {
            ret.append("SPACECRAFT: " + spacecraft + lineSep);
        }

        if (seqStart != -1 && seqEnd != -1)
        {
            ret.append("SEQUENCE: " + seqStart + " " + seqEnd + lineSep);
        }

        if (baudRates != null && baudRates.trim().length() > 0)
        {
            ret.append("BAUD: " + baudRates + lineSep);
        }

        if (protoVersion >= DdsVersion.version_9 && ascendingTimeOnly)
        {
            ret.append("ASCENDING_TIME: true" + lineSep);
        }

        if (protoVersion >= DdsVersion.version_9 && realtimeSettlingDelay)
        {
            ret.append("RT_SETTLE_DELAY: true" + lineSep);
        }
        if (protoVersion >= 11 && single)
        {
            ret.append("SINGLE: true" + lineSep);
        }

        if (protoVersion >= DdsVersion.version_12 && parityErrors != ACCEPT)
        {
            ret.append("PARITY_ERROR: " + parityErrors + lineSep);
            needLocalFilter = true;
        }

        if (needLocalFilter)
        {
            localFilter = new SearchCritLocalFilter(this, protoVersion);
        }

        return ret.toString(); // string containing complete searchcrit file.
    }

    public SearchCritLocalFilter getSearchCritLocalFilter()
    {
        SearchCritLocalFilter ret = localFilter;
        localFilter = null;
        return ret;
    }

    /** Add a source by its numeric code */
    public void addSource(int srcCode)
    {
        if (numSources >= MAX_SOURCES)
        {
            return;
        }
        for(int i=0; i<numSources; i++)
        {
            if (sources[i] == srcCode)
            {
                return;
            }
        }
        sources[numSources++] = srcCode;
    }

    public boolean equals(SearchCriteria rhs)
    {
        if (!TextUtil.strEqual(LrgsSince, rhs.LrgsSince))
        {
            return false;
        }
        if (!TextUtil.strEqual(LrgsUntil, rhs.LrgsUntil))
        {
            return false;
        }
        if (!TextUtil.strEqual(DapsSince, rhs.DapsSince))
        {
            return false;
        }
        if (!TextUtil.strEqual(DapsUntil, rhs.DapsUntil))
        {
            return false;
        }
        if (!TextUtil.strEqual(baudRates, rhs.baudRates))
        {
            return false;
        }

        if (ascendingTimeOnly != rhs.ascendingTimeOnly
         || realtimeSettlingDelay != rhs.realtimeSettlingDelay
         || single != rhs.single
         || seqStart != rhs.seqStart
         || seqEnd != rhs.seqEnd)
        {
            return false;
        }

        if (DapsStatus != rhs.DapsStatus
         || parityErrors != rhs.parityErrors
         || spacecraft != rhs.spacecraft)
        {
            return false;
        }

        if (NetlistFiles.size() != rhs.NetlistFiles.size())
        {
            return false;
        }
        for(String nl : NetlistFiles)
        {
            if (!rhs.NetlistFiles.contains(nl))
            {
                return false;
            }
        }

        if (DcpNames.size() != rhs.DcpNames.size())
        {
            return false;
        }
        for(String nl : DcpNames)
        {
            if (!rhs.DcpNames.contains(nl))
            {
                return false;
            }
        }

        if (ExplicitDcpAddrs.size() != rhs.ExplicitDcpAddrs.size())
        {
            return false;
        }
        for(DcpAddress nl : ExplicitDcpAddrs)
        {
            if (!rhs.ExplicitDcpAddrs.contains(nl))
            {
                return false;
            }
        }

        if (channels == null ^ rhs.channels == null)
        {
            return false;
        }
        if (channels != null)
        {
            if (channels.length != rhs.channels.length)
            {
                return false;
            }
            for(int chan : channels)
            {
                boolean found = false;
                for(int rhschan : rhs.channels)
                {
                    if (chan == rhschan)
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    return false;
                }
            }
        }

        if (numSources != rhs.numSources)
        {
            return false;
        }
        for(int source : sources)
        {
            boolean found = false;
            for(int rhssource : rhs.sources)
            {
                if (source == rhssource)
                {
                    found = true;
                    break;
                }
            }
            if (!found)
            {
                return false;
            }
        }

        return true;
    }
}
