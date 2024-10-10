/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*/
package lrgs.ddsrecv;

import ilex.util.ByteUtil;
import ilex.util.TextUtil;

/**
Encapsulates settings necessary for a DDS Recv connection.
*/
public class DdsRecvConnectCfg
	implements Comparable<DdsRecvConnectCfg>
{
	/** The connection number */
	public int connectNum;

	/** the DDS Server's host name */
	public String host;

	/** name for log messages */
	public String name;

	/** socket listening port */
	public int port;

	/** Enable the port for receiving data */
	public boolean enabled;

	/** User name to give to the server. */
	public String username;

	/** True if we should authenticate when connecting. */
	public boolean authenticate;

	/** True if this server has DOMSAT sequence #s */
	public boolean hasDomsatSeqNums;
	
	/** True if we are to accept Abnormal Response Messages */
	public boolean acceptARMs;
	
	public String group="Primary";

	/**
	  Constructs a config and fills it with defaults.
	  @param num connect number
	  @param host DRGS host name
	*/
	public DdsRecvConnectCfg(int num, String host)
	{
		connectNum = num;
		this.host = host;
		name = host;
		port = 16003;
		enabled = true;
		username = "ddsrecv";
		authenticate = false;
		hasDomsatSeqNums = false;
		acceptARMs = false;
		group = "primary";
	}

	/** Copy constructor */
	public DdsRecvConnectCfg(DdsRecvConnectCfg rhs)
	{
		connectNum = rhs.connectNum;
		host = rhs.host;
		name = rhs.name;
		port = rhs.port;
		enabled = rhs.enabled;
		username = rhs.username;
		authenticate = rhs.authenticate;
		hasDomsatSeqNums = rhs.hasDomsatSeqNums;
		acceptARMs = rhs.acceptARMs;
		group = rhs.group;
	}

	/** 
	@return String containing comma separated name=value pairs for all settings.
	*/
	public String toString()
	{
		return "ddsrecv[" + connectNum + "] host=" + host
			+ ", name=" + name
			+ ", port=" + port
			+ ", enabled=" + enabled
			+ ", username=" + username
			+ ", authenticate=" + authenticate
			+ ", hasDomsatSeqNums=" + hasDomsatSeqNums
			+ ", acceptARMs=" + acceptARMs
			+ ", group=" + group;
	}

	public int compareTo(DdsRecvConnectCfg rhs)
	{
		int r = connectNum - rhs.connectNum;
		if (r != 0) return r;

		r = TextUtil.strCompareIgnoreCase(host, rhs.host);
		if (r != 0) return r;

		r = TextUtil.strCompareIgnoreCase(name, rhs.name);
		if (r != 0) return r;

		r = port - rhs.port;
		if (r != 0) return r;

		r = TextUtil.strCompareIgnoreCase(username, rhs.username);
		if (r != 0) return r;
		
		return 0;
	}
}
