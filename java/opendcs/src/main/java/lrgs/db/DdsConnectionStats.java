package lrgs.db;

import java.util.Date;

/**
 * 
 * This encapsulates information about LRGS Dds Connection.
 *  
 *
 */
public class DdsConnectionStats 
	implements LrgsDatabaseObject
{	
	private int connectionId;
	private String lrgsHost;
	private Date startTime;
	private Date endTime;
	private String fromIpAddr;
	private char successCode;
	private String userName;
	/** Total # of messages delivered during connection. */
	private int msgsReceived;
	private boolean admin_done;
	private int protocolVersion = 0;
	private Date lastActivity = null;

	/** True after this connection's stats have been saved to database. */
	private boolean _inDb;

	/** A transient msg tally used to aggregate hourly stats (not saved). */
	private int msgTallyForAgg;

	/** Connected but nothing else done */
	public static final char SC_CONNECTED       = 'C';

	/** Hangup before login */
	public static final char SC_HANGUP          = 'H';

	/** Successfully authenticated */
	public static final char SC_AUTHENTICATED   = 'A';

	/** Successfully connected but unauthenticated */
	public static final char SC_UNAUTHENTICATED = 'U';

	/** Connection terminated due to bad password */
	public static final char SC_BAD_PASSWORD    = 'P';

	/** Connection terminated due to no DDS permission */
	public static final char SC_NO_DDS_PERM     = 'D';

	/** Connection terminated due to bad username */
	public static final char SC_BAD_USERNAME    = 'N';
	
	/** Connection terminated because disallowed IP address */
	public static final char SC_BAD_IP_ADDR     = 'I';
	
	public static final char SC_ACCOUNT_SUSPENDED = 'S';
	
	/**
	 * Constructor. Initialize all private variables.
	 */
	public DdsConnectionStats()
	{
		connectionId = 0;
		lrgsHost = null;
		startTime = null;
		endTime = null;
		fromIpAddr = null;
		successCode = '\0';
		userName = null;
		msgsReceived = 0;
		admin_done = false;
		_inDb = false;
		msgTallyForAgg = 0;
	}

	/**
	 * Construct DdsConnectionStats Object from a connectionId, startTime, endTime, 
	 * fromIpAddr, successCode, userName, msgsReceived and admin_done.
	 * 
	 * @param connectionId the unique id for the dds connection
	 * @param startTime begin time for connection
	 * @param endTime end time for connection
	 * @param fromIpAddr where the dds connection came from
	 * @param successCode indicates the disposition of the connection
	 * @param userName indicates user of the connection
	 * @param msgsReceived number of messages received in that connection 
	 * @param admin_done a true or false value indicating if any administration was done
	*/
	public DdsConnectionStats(int connectionId, String lrgsHost, Date startTime, Date endTime, 
		String fromIpAddr, char successCode, String userName, int msgsReceived,
		boolean admin_done, int protocolVersion, Date lastActivity)
	{
		this();
		this.connectionId = connectionId;
		this.lrgsHost = lrgsHost;
		this.startTime = startTime;
		this.endTime = endTime;
		this.fromIpAddr = fromIpAddr;
		this.successCode = successCode;
		this.userName = userName;
		this.msgsReceived = msgsReceived;
		this.admin_done = admin_done;
	}

	/**
	 *  This method returns the Dds Connection Id.
	 *   
	 *  @return connectionId the unique id for the dds connection
	 */
	public int getConnectionId() 
	{
		return connectionId;
	}

	/**
	 * This method sets the Dds Connection Id.
	 * 
	 * @param connectionId the unique id for the dds connection
	 */
	public void setConnectionId(int connectionId) 
	{
		this.connectionId = connectionId;
	}

	/**
	 *  This method returns the end time of the Dds Connection.
	 *   
	 *  @return endTime end time for connection
	 */
	public Date getEndTime() 
	{
		return endTime;
	}

	/**
	 * This method sets the Dds Connection end time.
	 * 
	 * @param endTime end time for connection
	 */
	public void setEndTime(Date endTime) 
	{
		this.endTime = endTime;
	}

	/**
	 *  This method returns the from ip address of the Dds Connection.
	 *   
	 *  @return fromIpAddr where the dds connection came from
	 */
	public String getFromIpAddr() 
	{
		return fromIpAddr;
	}

	/**
	 * This method sets the Dds Connection from ip address.
	 * 
	 * @param fromIpAddr where the dds connection came from
	 */
	public void setFromIpAddr(String fromIpAddr) 
	{
		this.fromIpAddr = fromIpAddr;
	}

	/**
	 *  This method returns the number of messages received in the Dds Connection.
	 *   
	 *  @return msgsReceived number of messages received in that connection
	 */
	public int getMsgsReceived() 
	{
		return msgsReceived;
	}

	/**
	 * This method sets the Dds Connection number of msgs received.
	 * 
	 * @param msgsReceived number of messages received in that connection
	 */
	public void setMsgsReceived(int msgsReceived) 
	{
		this.msgsReceived = msgsReceived;
	}

	public synchronized void addMsgsReceived(int n)
	{
		this.msgsReceived += n;
		msgTallyForAgg += n;
	}


	/**
	 *  This method returns the start time of the Dds Connection.
	 *   
	 *  @return startTime begin time for connection
	 */
	public Date getStartTime() 
	{
		return startTime;
	}

	/**
	 * This method sets the Dds Connection start time.
	 * 
	 * @param startTime begin time for connection
	 */
	public void setStartTime(Date startTime) 
	{
		this.startTime = startTime;
	}

	/**
	 *  This method returns the success code of the Dds Connection.
	 *   
	 *  @return successCode indicates the disposition of the connection
	 */
	public char getSuccessCode() 
	{
		return successCode;
	}

	/**
	 * This method sets the Dds Connection success code.
	 * 
	 * @param successCode indicates the disposition of the connection
	 */
	public void setSuccessCode(char successCode) 
	{
		this.successCode = successCode;
	}

	/**
	 *  This method returns the user name of the Dds Connection.
	 *   
	 *  @return userName indicates user of the connection
	 */
	public String getUserName() 
	{
		return userName;
	}

	/**
	 * This method sets the Dds Connection user name.
	 * 
	 * @param userName indicates user of the connection
	 */
	public void setUserName(String userName) 
	{
		this.userName = userName;
	}

	/**
	 *  This method returns admin done value (true or false) of the Dds Connection.
	 *   
	 *  @return admin_done a true or false value indicating if any administration was done
	 */
	public boolean isAdmin_done()
	{
		return admin_done;
	}

	/**
	 * This method sets the Dds Connection admin done value.
	 * 
	 * @param admin_done a true or false value indicating if any administration was done
	 */
	public void setAdmin_done(boolean admin_done)
	{
		this.admin_done = admin_done;
	}

	public boolean getInDb() { return _inDb; }
	public void setInDb(boolean inDb) { _inDb = inDb; }

	/**
	 * Gets and resets the msgTallyForAgg.
	 */
	public synchronized int getMsgTally()
	{
		int r = msgTallyForAgg;
		msgTallyForAgg = 0;
		return r;
	}

	public String getLrgsHost()
	{
		return lrgsHost;
	}

	public void setLrgsHost(String lrgsHost)
	{
		this.lrgsHost = lrgsHost;
	}

	public int getProtocolVersion()
	{
		return protocolVersion;
	}

	public void setProtocolVersion(int protocolVersion)
	{
		this.protocolVersion = protocolVersion;
	}

	public Date getLastActivity()
	{
		return lastActivity;
	}

	public void setLastActivity(Date lastActivity)
	{
		this.lastActivity = lastActivity;
	}
}
