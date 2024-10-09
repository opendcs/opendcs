package decodes.routmon;

import java.io.*;
import java.util.*;

/**
This class holds status info for a single routing spec.
*/
public class RoutingSpecStatus implements Comparable 
{
	/** Name of the routing spec */
	public String name;

	/** Brief description of current status */
	public String status;

	/** Time this routing spec was last started */
	public Date startTime;

	/** Time this routing spec last retrieved a message */
	public Date lastMsgRecieveTime;

	/** Number of messages retrieved in this run of the routing spec */
	public String msgsThisRun;

	/** Number of messages retrieved today, possibly spanning multiple runs. */
	public String msgsToday;

	/** Current (or last) server used by this routing spec */
	public String currentServer;

	/** Name of current output file or pipe */
	public String outputName;

	/** Output format being used */
	public String outputFormat;

	/** Last time status was changed by this routing spec */
	public long lastStatusChangeMsec;

	/** Used for undefined dates: */
	private static Date nullDate = new Date(0L);

	/**
	 * Constructor.
	 * @param name the name of the routing spec
	 */
	public RoutingSpecStatus(String name)
	{
		this.name = name;
		status = "unknown";
		startTime = nullDate;
		lastMsgRecieveTime = nullDate;
		msgsThisRun = "0";
		msgsToday = "0";
		currentServer = "";
		outputName = "";
		outputFormat = "";
		lastStatusChangeMsec = System.currentTimeMillis();
	}

	/**
	 * Return true if the routing spec names match.
	 * @param spec name of routing spec.
	 * @return true if the routing spec names match
	*/
	public int compareTo(Object spec)
	{
		return name.compareTo(((RoutingSpecStatus)spec).name);
	}
}
