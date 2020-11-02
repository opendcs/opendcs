/*
*  $Id$
*
*  $Log$
*  Revision 1.3  2008/09/21 16:08:51  mjmaloney
*  network DCPs
*
*  Revision 1.2  2008/09/19 11:53:23  mjmaloney
*  dev
*
*  Revision 1.1  2008/04/04 18:21:13  cvs
*  Added legacy code to repository
*
*  Revision 1.5  2007/02/16 22:24:14  mmaloney
*  LRGS Configuration GUI Implementation
*
*  Revision 1.4  2005/12/14 21:20:24  mmaloney
*  Fix the 'synchronizing' in MsgArchive, particularly at GMT midnight.
*
*  Revision 1.3  2005/10/10 19:44:24  mmaloney
*  dev
*
*  Revision 1.2  2004/09/02 13:09:03  mjmaloney
*  javadoc
*
*  Revision 1.1  2003/03/27 16:58:50  mjmaloney
*  drgs development.
*
*/
package lrgs.drgs;

import ilex.util.ByteUtil;
import ilex.util.TextUtil;
import lrgs.networkdcp.NetworkDcpState;

/**
Encapsulates settings necessary for a DRGS connection.
*/
public class DrgsConnectCfg
	implements Comparable<DrgsConnectCfg>
{
	/** The connection number */
	public int connectNum;
	/** the DRGS host name */
	public String host;
	/** the DRGS name for log messages */
	public String name;
	/** Message socket listening port */
	public int msgPort;
	/** Enable the message port */
	public boolean msgEnabled;
	/** Enable the events port */
	public int evtPort;
	/** Events socket listening port */
	public boolean evtEnabled;
	/** Start pattern on DCP messages */
	public byte[] startPattern;
	/** The name of the file containing the configuration */
	public String cfgFile;
	/** 2-char source-code to be inserted into message. */
	public byte drgsSourceCode[];
	/** Polling period, # minutes, 0=continuous */
	public int pollingPeriod = 0;
	
	
	
	/** Used for Network DCPs only to track the run-time state. */
	private NetworkDcpState state = NetworkDcpState.Waiting;
	/** Used for Network DCPs only - time this DCP was last polled */
	private long lastPollTime = 0L;
	
	/**
	  Constructs a config and fills it with defaults.
	  @param num connect number
	  @param host DRGS host name
	*/
	public DrgsConnectCfg(int num, String host)
	{
		connectNum = num;
		this.host = host;
		name = host;
		msgPort = 17010;
		msgEnabled = true;
		evtPort = 17011;
		evtEnabled = false;
		startPattern = new byte[4];
		startPattern[0] = (byte)'S';
		startPattern[1] = (byte)'M';
		startPattern[2] = (byte)'\r';
		startPattern[3] = (byte)'\n';
		cfgFile = null;
		drgsSourceCode = new byte[2];
		drgsSourceCode[0] = (byte)'D';
		drgsSourceCode[1] = (byte)'R';
		pollingPeriod = 0;
	}

	/** Copy Constructor */
	public DrgsConnectCfg(DrgsConnectCfg rhs)
	{
		connectNum = rhs.connectNum;
		host = rhs.host;
		name = rhs.name;
		msgPort = rhs.msgPort;
		msgEnabled = rhs.msgEnabled;
		evtPort = rhs.evtPort;
		evtEnabled = rhs.evtEnabled;
		startPattern = new byte[4];
		startPattern[0] = rhs.startPattern[0];
		startPattern[1] = rhs.startPattern[1];
		startPattern[2] = rhs.startPattern[2];
		startPattern[3] = rhs.startPattern[3];
		cfgFile = rhs.cfgFile;
		drgsSourceCode = new byte[2];
		drgsSourceCode[0] = rhs.drgsSourceCode[0];
		drgsSourceCode[1] = rhs.drgsSourceCode[1];
		pollingPeriod = rhs.pollingPeriod;
	}

	/** 
	@return String containing comma separated name=value pairs for all settings.
	*/
	public String toString()
	{
		return "drgs[" + connectNum + "] host=" + host
			+ ", name=" + name
			+ ", msgPort=" + msgPort
			+ ", msgEnabled=" + msgEnabled
			+ ", evtPort=" + evtPort
			+ ", evtEnabled=" + evtEnabled
			+ ", startPattern=" + ByteUtil.toHexString(startPattern)
			+ ", cfgfile=" + cfgFile
			+ ", src=" + (char)drgsSourceCode[0] + (char)drgsSourceCode[1]
			+ ", pollingPeriod=" + pollingPeriod;
	}

	public int compareTo(DrgsConnectCfg rhs)
	{
		int r = connectNum - rhs.connectNum;
		if (r != 0) return r;

		r = TextUtil.strCompareIgnoreCase(host, rhs.host);
		if (r != 0) return r;

		r = TextUtil.strCompareIgnoreCase(name, rhs.name);
		if (r != 0) return r;

		r = msgPort - rhs.msgPort;
		if (r != 0) return r;

		return 0;
	}

	public NetworkDcpState getState() { return state; }

	public void setState(NetworkDcpState state) { this.state = state; }

	/** @return time last polled, or null if never polled */
	public long getLastPollTime() { return lastPollTime; }
	
	public void setLastPollTime(long lastPollTime) 
	{
		this.lastPollTime = lastPollTime;
	}
	
	public boolean equalToNetDcp(DrgsConnectCfg rhs)
	{
		if (this.connectNum != rhs.connectNum
		 || !TextUtil.strEqual(this.host, rhs.host) 
		 || !TextUtil.strEqual(this.name, rhs.name)
		 || this.msgPort != rhs.msgPort
		 || this.msgEnabled != rhs.msgEnabled
		 || this.pollingPeriod != rhs.pollingPeriod
		 || this.startPattern.length != rhs.startPattern.length
		 || this.drgsSourceCode.length != rhs.drgsSourceCode.length)
			return false;

		for(int i=0; i<startPattern.length; i++)
			if (this.startPattern[i] != rhs.startPattern[i])
				return false;
		for(int i=0; i<drgsSourceCode.length; i++)
			if (this.drgsSourceCode[i] != rhs.drgsSourceCode[i])
				return false;
		return true;
	}

}
