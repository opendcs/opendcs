package lrgs.db;

import java.util.Date;

/**
 * 
 * This encapsulates information about LRGS dds period stats.
 * 
 */
public class DdsPeriodStats
	implements LrgsDatabaseObject
{

	private Date startTime;
	private String lrgsHost;
	private char periodDuration;
	private int numAuth;
	private int numUnAuth;
	private int badPasswords;
	private int badUsernames;
	private int maxClients;
	private int minClients;
	private double aveClients;
	private int msgsDelivered;

	/**
	 * Constructor.
	 * Initialize all private variables.
	 */
	public DdsPeriodStats()
	{
		startTime = null;
		lrgsHost = null;
		periodDuration = 'H'; // store hourly
		numAuth = 0;
		numUnAuth = 0;
		badPasswords = 0;
		badUsernames = 0;
		maxClients = 0;
		minClients = 0;
		aveClients = 0.0;
		msgsDelivered = 0;
	}

	/**
	 * Construct DdsPeriodStats Object from a startTime, periodDuration,
	 * numAuth, numUnAuth, badPasswords, badUsernames, maxClients, minClients,
	 * aveClients and msgsDelivered.
	 * 
	 * @param startTime begin time for dds period stats
	 * @param periodDuration indicates the duration in H (hour) D (day) W (week) M (month) Y (year)
	 * @param numAuth number of authentications
	 * @param numUnAuth number of unauths
	 * @param badPasswords number of bad passwords
	 * @param badUsernames number of bad user names
	 * @param maxClients max concurrent connections number
	 * @param minClients min concurrent connections number
	 * @param aveClients average concurrent connections number
	 * @param msgsDelivered number of messages delivered
	 */
	public DdsPeriodStats(Date startTime, String lrgsHost, char periodDuration, int numAuth, 
		int numUnAuth, int badPasswords, int badUsernames, int maxClients, 
		int minClients, double aveClients, int msgsDelivered)
	{
		this();
		this.startTime = startTime;
		this.lrgsHost = lrgsHost;
		this.periodDuration = periodDuration;
		this.numAuth = numAuth;
		this.numUnAuth = numUnAuth;
		this.badPasswords = badPasswords;
		this.badUsernames = badUsernames;
		this.maxClients = maxClients;
		this.minClients = minClients;
		this.aveClients = aveClients;
		this.msgsDelivered = msgsDelivered;
	}

	/**
	 * This method returns the average concurrent connections field.
	 *   
	 * @return aveClients average concurrent connections number
	 */
	public double getAveClients()
	{
		return aveClients;
	}

	/**
	 * This method sets the average concurrent connections field.
	 * 
	 * @param aveClients average concurrent connections number
	 */
	public void setAveClients(double aveClients)
	{
		this.aveClients = aveClients;
	}

	/**
	 * This method returns the number of bad passwords.
	 *   
	 * @return badPasswords number of bad passwords
	 */
	public int getBadPasswords()
	{
		return badPasswords;
	}

	/**
	 * This method sets the number of bad passwords.
	 * 
	 * @param badPasswords number of bad passwords
	 */
	public void setBadPasswords(int badPasswords)
	{
		this.badPasswords = badPasswords;
	}

	/**
	 * This method returns the number of bad user names.
	 *   
	 * @return badUsernames number of bad user names
	 */
	public int getBadUsernames()
	{
		return badUsernames;
	}

	/**
	 * This method sets the number of bad user names.
	 * 
	 * @param badUsernames number of bad user names
	 */
	public void setBadUsernames(int badUsernames)
	{
		this.badUsernames = badUsernames;
	}

	/**
	 * This method returns the number of max concurrent connections.
	 *   
	 * @return maxClients max concurrent connections number
	 */
	public int getMaxClients()
	{
		return maxClients;
	}

	/**
	 * This method sets the number of max concurrent connections.
	 * 
	 * @param maxClients max concurrent connections number
	 */
	public void setMaxClients(int maxClients)
	{
		this.maxClients = maxClients;
	}

	/**
	 * This method returns the number of min concurrent connections.
	 *   
	 * @return minClients min concurrent connections number
	 */
	public int getMinClients()
	{
		return minClients;
	}

	/**
	 * This method sets the number of min concurrent connections.
	 * 
	 * @param minClients min concurrent connections number
	 */
	public void setMinClients(int minClients)
	{
		this.minClients = minClients;
	}

	/**
	 * This method returns the number of messages delivered.
	 *   
	 * @return msgsDelivered number of messages delivered
	 */
	public int getMsgsDelivered()
	{
		return msgsDelivered;
	}

	/**
	 * This method sets the number of messages delivered.
	 * 
	 * @param msgsDelivered number of messages delivered
	 */
	public void setMsgsDelivered(int msgsDelivered)
	{
		this.msgsDelivered = msgsDelivered;
	}

	/**
	 * Adds the number of messages delivered.
	 * 
	 * @param msgsDelivered number of messages delivered
	 */
	public void addMsgsDelivered(int msgsDelivered)
	{
		this.msgsDelivered += msgsDelivered;
	}

	/**
	 * This method returns the number of authentications.
	 *   
	 * @return number of authentications
	 */
	public int getNumAuth()
	{
		return numAuth;
	}

	/**
	 * This method sets the number of authentications.
	 * 
	 * @param numAuth of authentications
	 */
	public void setNumAuth(int numAuth)
	{
		this.numAuth = numAuth;
	}

	/**
	 * This method returns the number of unauths.
	 *   
	 * @return number of unauths
	 */
	public int getNumUnAuth()
	{
		return numUnAuth;
	}

	/**
	 * This method sets the number of unauths.
	 * 
	 * @param numUnAuth number of unauths
	 */
	public void setNumUnAuth(int numUnAuth)
	{
		this.numUnAuth = numUnAuth;
	}

	/**
	 * This method returns the begin time for dds period stats.
	 *   
	 * @return startTime begin time for dds period stats
	 */
	public Date getStartTime()
	{
		return startTime;
	}

	/**
	 * This method sets the begin time for dds period stats.
	 * 
	 * @param startTime begin time for dds period stats
	 */
	public void setStartTime(Date startTime)
	{
		this.startTime = startTime;
	}

	/**
	 * This method returns the periodDuration for dds period stats.
	 *   
	 * @return periodDuration indicates the duration in H (hour) D (day) W (week) M (month) Y (year)
	 */
	public char getPeriodDuration()
	{
		return periodDuration;
	}

	/**
	 * This method sets the periodDuration for dds period stats.
	 * 
	 * @param periodDuration indicates the duration in H (hour) D (day) W (week) M (month) Y (year)
	 */
	public void setPeriodDuration(char periodDuration)
	{
		this.periodDuration = periodDuration;
	}

	public void incrNumAuth() { numAuth++; }
	public void incrNumUnAuth() { numUnAuth++; }
	public void incrBadPasswords() { badPasswords++; }
	public void incrBadUsernames() { badUsernames++; }

	public String getLrgsHost()
	{
		return lrgsHost;
	}

	public void setLrgsHost(String lrgsHost)
	{
		this.lrgsHost = lrgsHost;
	}
}
