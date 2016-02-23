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
	
	private String fname = null;
	private String lname = null;
	private String org = null;
	private String email = null;
	private String  tel = null;
	private boolean suspended = false;

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
		this.fname = rhs.fname;
		this.lname = rhs.lname;
		this.org = rhs.org;
		this.email = rhs.email;
		this.tel = rhs.tel;
		this.suspended = rhs.suspended;
	}
	
	public boolean isAdmin()
	{
		if (perms == null)
			return false;
		for(String p : perms)
			if (p.equalsIgnoreCase("admin"))
				return true;
		return false;
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
		Properties props = new Properties();

		if (ipAddr != null && ipAddr.trim().length() > 0)
			props.setProperty("ipaddr", ipAddr);

		if (dcpLimit != -1)
			props.setProperty("maxdcps", "" + dcpLimit);

		if (forceAscending)
			props.setProperty("forceAscending", "true");

		if (desc != null && desc.trim().length() > 0)
			props.setProperty("desc", desc);
		
		if (isLocal)
			props.setProperty("local", "true");
		
		if (fname != null && fname.trim().length() > 0)
			props.setProperty("fname", fname);
		if (lname != null && lname.trim().length() > 0)
			props.setProperty("lname", lname);
		if (org != null && org.trim().length() > 0)
			props.setProperty("org", org);
		if (email != null && email.trim().length() > 0)
			props.setProperty("email", email);
		if (tel != null && tel.trim().length() > 0)
			props.setProperty("tel", tel);
		if (suspended)
			props.setProperty("suspended", "true");
		
		return PropertiesUtil.props2string(props);
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
//System.out.println("permsString='" + x + "'");
		perms=x.split(",");
		
		// Must have at least 3 space delimited tokens. Find the beginning of the 3rd token.
//System.out.println("entire userSpec='" + userSpec + "'");
		int idx = userSpec.indexOf(' ');
		while(idx < userSpec.length() && Character.isWhitespace(userSpec.charAt(idx)))
			idx++;
		while(idx < userSpec.length() && !Character.isWhitespace(userSpec.charAt(idx)))
			idx++;
		while(idx < userSpec.length() && Character.isWhitespace(userSpec.charAt(idx)))
			idx++;
		while(idx < userSpec.length() && !Character.isWhitespace(userSpec.charAt(idx)))
			idx++;
		while(idx < userSpec.length() && Character.isWhitespace(userSpec.charAt(idx)))
			idx++;
	
		String pstr = userSpec.substring(idx);
//System.out.println("user spec propstr = '" + pstr + "'");
		Properties props = PropertiesUtil.string2props(pstr);
		ipAddr = PropertiesUtil.getIgnoreCase(props, "ipaddr");
		x = PropertiesUtil.getIgnoreCase(props, "maxdcps");
		if (x == null)
			dcpLimit = -1;
		else
		{
			try { dcpLimit = Integer.parseInt(x); }
			catch(NumberFormatException ex) { dcpLimit = -1; }
		}
		forceAscending = TextUtil.str2boolean(PropertiesUtil.getIgnoreCase(props, "forceAscending"));
		desc = PropertiesUtil.getIgnoreCase(props, "desc");
		if (desc != null && desc.contains("\\"))
			desc = new String(AsciiUtil.ascii2bin(desc));
		isLocal = TextUtil.str2boolean(PropertiesUtil.getIgnoreCase(props, "local"));
		fname = PropertiesUtil.getIgnoreCase(props, "fname");
		lname = PropertiesUtil.getIgnoreCase(props, "lname");
		org = PropertiesUtil.getIgnoreCase(props, "org");
		email = PropertiesUtil.getIgnoreCase(props, "email");
		tel = PropertiesUtil.getIgnoreCase(props, "tel");
		suspended = TextUtil.str2boolean(PropertiesUtil.getIgnoreCase(props, "suspended"));
//System.out.println("fromString, after parse, username=" + userName + ", suspended=" + suspended);
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

	public String getFname()
	{
		return fname;
	}

	public void setFname(String fname)
	{
		this.fname = fname;
	}

	public String getLname()
	{
		return lname;
	}

	public void setLname(String lname)
	{
		this.lname = lname;
	}

	public String getOrg()
	{
		return org;
	}

	public void setOrg(String org)
	{
		this.org = org;
	}

	public String getEmail()
	{
		return email;
	}

	public void setEmail(String email)
	{
		this.email = email;
	}

	public String getTel()
	{
		return tel;
	}

	public void setTel(String tel)
	{
		this.tel = tel;
	}

	public boolean isSuspended()
	{
		return suspended;
	}

	public void setSuspended(boolean suspended)
	{
		this.suspended = suspended;
	}
}
