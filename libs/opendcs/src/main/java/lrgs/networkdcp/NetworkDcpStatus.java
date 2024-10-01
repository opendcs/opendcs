/**
 * 
 */
package lrgs.networkdcp;

import java.util.Date;

/**
 * @author mjmaloney
 *
 */
public class NetworkDcpStatus
{
	/** Host name or IP Address (along with port# this forms a key) */
	private String host;
	
	/** Port number */
	private int port;
	
	/** The display name for this DCP */
	private String displayName = "";
	
	/** Polling period in minutes, 0=continuous */
	private int pollingMinutes = 0;
	
	/** Time of last poll-attempt */
	private Date lastPollAttempt = null;
	
	/** Time of last successful contact */
	private Date lastContact = null;
	
	/** Total # of polls */
	private long numGoodPolls = 0;

	/** Total # of failed polls */
	private long numFailedPolls = 0;
	
	/** Total # messages returned */
	private long numMessages = 0;
	
	
	/** Constructs a new status object with the key host/port values */
	public NetworkDcpStatus(String host, int port)
	{
		this.setHost(host);
		this.setPort(port);
	}

	/**
	 * Called from NetworkDcpStatusList after a poll attempt has been made.
	 * @param success true if the poll succeeded. False if it failed.
	 * @param numMessages If success, this is the number of messages received.
	 */
	public void pollAttempt(boolean success, int numMessages)
	{
		lastPollAttempt = new Date();
		if (success)
		{
			lastContact = lastPollAttempt;
			// don't bump poll count for continuous
			if (this.pollingMinutes > 0 || numMessages == 0) 
				numGoodPolls++;
			this.numMessages += numMessages;
		}
		else
		{
			numFailedPolls++;
		}
	}

	/**
     * @param displayName the displayName to set
     */
    public void setDisplayName(String displayName)
    {
	    this.displayName = displayName;
    }

	/**
     * @return the displayName
     */
    public String getDisplayName()
    {
	    return displayName;
    }

	/**
     * @param pollingMinutes the pollingMinutes to set
     */
    public void setPollingMinutes(int pollingMinutes)
    {
	    this.pollingMinutes = pollingMinutes;
    }

	/**
     * @return the pollingMinutes
     */
    public int getPollingMinutes()
    {
	    return pollingMinutes;
    }

	/**
     * @param lastPollAttempt the lastPollAttempt to set
     */
    public void setLastPollAttempt(Date lastPollAttempt)
    {
	    this.lastPollAttempt = lastPollAttempt;
    }

	/**
     * @return the lastPollAttempt
     */
    public Date getLastPollAttempt()
    {
	    return lastPollAttempt;
    }

	/**
     * @param lastContact the lastContact to set
     */
    public void setLastContact(Date lastContact)
    {
	    this.lastContact = lastContact;
    }

	/**
     * @return the lastContact
     */
    public Date getLastContact()
    {
	    return lastContact;
    }

	/**
     * @param numPolls the numPolls to set
     */
    public void setNumGoodPolls(long numGoodPolls)
    {
	    this.numGoodPolls = numGoodPolls;
    }

	/**
     * @return the numPolls
     */
    public long getNumGoodPolls()
    {
	    return numGoodPolls;
    }

	/**
     * @param numMessages the numMessages to set
     */
    public void setNumMessages(long numMessages)
    {
	    this.numMessages = numMessages;
    }

	/**
     * @return the numMessages
     */
    public long getNumMessages()
    {
	    return numMessages;
    }

	/**
     * @param numFailedPolls the numFailedPolls to set
     */
    public void setNumFailedPolls(long numFailedPolls)
    {
	    this.numFailedPolls = numFailedPolls;
    }

	/**
     * @return the numFailedPolls
     */
    public long getNumFailedPolls()
    {
	    return numFailedPolls;
    }

	/**
     * @param host the host to set
     */
    public void setHost(String host)
    {
	    this.host = host;
    }

	/**
     * @return the host
     */
    public String getHost()
    {
	    return host;
    }

	/**
     * @param port the port to set
     */
    public void setPort(int port)
    {
	    this.port = port;
    }

	/**
     * @return the port
     */
    public int getPort()
    {
	    return port;
    }


}
