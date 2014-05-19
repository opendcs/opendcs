/*
* $Id$
*/
package lrgs.ldds;

import java.util.StringTokenizer;
import java.util.Properties;

import ilex.util.PropertiesUtil;
import ilex.util.TextUtil;
import ilex.util.AsciiUtil;
import lrgs.common.BadConfigException;

/**
This class holds info about a single DDS user.
*/
public class DdsUser
{
	/** The user name */
	public String userName;

	/** True if this user has a password on the server. */
	public boolean hasPassword;

	/** List of permissions (a.k.a. roles). */
	public String perms[];

	/** IP Restriction, null if unrestricted. */
	private String ipAddr;

	/** DCP Limit, -1 if unlimited. */
	public int dcpLimit;

	/** True to force ascending-only data reception, default=false. */
	public boolean forceAscending;

	/** Description of this user. */
	public String desc;
	
	public boolean isLocal = false;

	/** Constructor */
	public DdsUser()
	{
		userName = "";
		hasPassword = false;
		perms = null;
		ipAddr = null;
		dcpLimit = -1;
		forceAscending = false;
		desc = "";
	}

	/** 
	 * Construct with a string specification in the form:
	 * <p>name +|- perm[,perm]...
	 * @param spec the string specification
	 */
	public DdsUser(String spec)
		throws BadConfigException
	{
		this();
		fromString(spec);
	}

	/** Copy constructor */
	public DdsUser(DdsUser rhs)
	{
		this();
		this.userName = rhs.userName;
		this.hasPassword = rhs.hasPassword;
		this.perms = rhs.perms;
		this.ipAddr = rhs.ipAddr;
		this.dcpLimit = rhs.dcpLimit;
		this.forceAscending = rhs.forceAscending;
		this.desc = rhs.desc;
		this.isLocal = rhs.isLocal;
	}

	/**
	 Returns a user spec string, as would be returned from a DDS server.
	 This string is in the form: "name +|- perm[,perm]..."
	 @return a user spec string, as would be returned from a DDS server.
	*/
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append(userName);
		if (hasPassword)
			sb.append(" + ");
		else
			sb.append(" - ");

		sb.append(permsString());
		sb.append(" ");
		sb.append(propsString());

		return sb.toString();
	}

	/**
	 * @return the set of permissions as a comma-separated string, or "-"
	 * if no permissions are set.
	 */
	public String permsString()
	{
		StringBuffer sb = new StringBuffer();
		if (perms == null || perms.length == 0)
			return "-";
		for(int i=0; i<perms.length; i++)
		{
			if (i>0)
				sb.append(',');
			sb.append(perms[i]);
		}
		return sb.toString();
	}

	/**
	 * @return properties as a comma-separated string or "" if no props set.
	 */
	public String propsString()
	{
		StringBuilder sb = new StringBuilder();

		if (ipAddr != null && ipAddr.trim().length() > 0)
		{
			sb.append("ipaddr=");
			sb.append(ipAddr);
		}

		if (dcpLimit != -1)
		{
			if (sb.length() > 0)
				sb.append(",");
			sb.append("maxdcps=" + dcpLimit);
		}

		if (forceAscending)
		{
			if (sb.length() > 0)
				sb.append(",");
			sb.append("forceAscending=true");
		}

		if (desc != null && desc.length() > 0)
		{
			if (sb.length() > 0)
				sb.append(",");
			sb.append("desc=" + AsciiUtil.bin2ascii(desc.getBytes(), ",= '\""));
		}
		
		if (isLocal)
		{
			if (sb.length() > 0)
				sb.append(",");
			sb.append("local=true");
		}

		String ret = sb.toString();

		return ret;
	}

	/**
	 * Parses the spec string and assign the user properties internally.
	 * @param userSpec the user spec string.
	 * @throws BadConfigException if userSpec is empty.
	 */
	public void fromString(String userSpec)
		throws BadConfigException
	{
		hasPassword = false;
		perms = null;

		StringTokenizer st = new StringTokenizer(userSpec);
		if (!st.hasMoreTokens())
			throw new BadConfigException("Invalid user spec '" + userSpec
				+ "' missing user name.");
		userName = st.nextToken();
		if (!st.hasMoreTokens())
			return;

		String x = st.nextToken();
		if (x.charAt(0) == '+')
			hasPassword = true;

		if (!st.hasMoreTokens())
			return;
		x = st.nextToken();
		perms = x.split(",");

		if (!st.hasMoreTokens())
			return;
		x = st.nextToken();
		Properties props = PropertiesUtil.string2props(x);
		ipAddr = PropertiesUtil.getIgnoreCase(props, "ipaddr");
		x = PropertiesUtil.getIgnoreCase(props, "maxdcps");
		if (x == null)
			dcpLimit = -1;
		else
		{
			try { dcpLimit = Integer.parseInt(x); }
			catch(NumberFormatException ex) { dcpLimit = -1; }
		}
		x = PropertiesUtil.getIgnoreCase(props, "forceAscending");
		forceAscending = TextUtil.str2boolean(x);
		x = PropertiesUtil.getIgnoreCase(props, "desc");
		if (x != null && x.length() > 0)
			desc = new String(AsciiUtil.ascii2bin(x));
		x = PropertiesUtil.getIgnoreCase(props, "local");
		isLocal = TextUtil.str2boolean(x);
	}

	public boolean hasPerm(String perm)
	{
		if (perms == null)
			return false;
		for(int i=0; i<perms.length; i++)
			if (perms[i].equalsIgnoreCase(perm))
				return true;
		return false;
	}

	public void addPerm(String perm)
	{
		if (perms == null)
			perms = new String[1];
		else
		{
			String newPerms[] = new String[perms.length + 1];
			for(int i=0; i<perms.length; i++)
				newPerms[i] = perms[i];
			perms = newPerms;
		}
		perms[perms.length-1] = perm;
	}

	public String getIpAddr()
	{
		return ipAddr;
	}

	public void setIpAddr(String ipa)
	{
		if (ipa == null)
		{
			ipAddr = null;
			return;
		}
		StringBuilder sb = new StringBuilder();
		StringTokenizer st = new StringTokenizer(ipa, " ,;");
		int n = 0;
		while(st.hasMoreTokens())
		{
			String t = st.nextToken();
			if (n++ > 0)
				sb.append(';');
			sb.append(t);
		}
		
		ipAddr = sb.toString();
	}
}
