/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.8  2013/02/28 16:14:42  mmaloney
*  bugfix in fromString.
*
*  Revision 1.7  2008/12/14 01:14:59  mjmaloney
*  case insensitive compare fix.
*
*  Revision 1.6  2008/10/14 12:04:42  mjmaloney
*  dev
*
*  Revision 1.5  2008/09/09 18:40:01  mjmaloney
*  dev
*
*  Revision 1.4  2008/09/08 19:14:03  mjmaloney
*  LRGS 7 dev
*
*  Revision 1.3  2008/08/19 16:38:15  mjmaloney
*  DcpAddress stores internal value as String.
*
*  Revision 1.2  2008/08/06 19:40:58  mjmaloney
*  dev
*
*  Revision 1.1  2008/04/04 18:21:11  cvs
*  Added legacy code to repository
*
*  Revision 1.11  2005/03/21 14:14:21  mjmaloney
*  Added unassigned address to distinguish an un-initialized message struct.
*
*  Revision 1.10  2005/03/21 13:15:58  mjmaloney
*  dev
*
*  Revision 1.9  2002/11/24 13:32:32  mjmaloney
*  Allow spaces before and after colon delimiter.
*
*  Revision 1.8  2000/04/26 16:24:48  mike
*  Need to trucate to 32 bits in constructor. It was doing a signed expansion.
*
*  Revision 1.7  2000/03/02 15:53:46  mike
*  Added correct implementation of equals and compareTo
*
*  Revision 1.6  1999/09/29 11:49:32  mike
*  First working version
*
*  Revision 1.5  1999/09/27 20:17:39  mike
*  9/27/1999
*
*  Revision 1.4  1999/09/23 12:34:08  mike
*  Initial implementation
*
*  Revision 1.3  1999/09/16 16:23:49  mike
*  9/16/1999
*
*  Revision 1.2  1999/09/14 17:05:34  mike
*  9/14/1999
*
*  Revision 1.1  1999/09/03 15:34:52  mike
*  Initial checkin.
*
*
*/
package lrgs.common;

import ilex.util.TextUtil;

/**
* Class DCP Address encapsulates a GOES-DCS DCP Address
*/
public class DcpAddress
	implements Comparable<DcpAddress>
{
	// Special DCP address values:
	public static final long GlobalBulletinAddr = 0x11111111;
	public static final long DcpBulletinAddr =    0x22222222;
	public static final long ElecMailAddr =       0x33333333;
	public static final long DapsSwitchoverAddr = 0x55555555;
	public static final long DapsAliveAddr =      0xdadadada;
	public static final long UnassignedAddr =     0x0a0a0a0a;
	public static final long InvalidAddr =        0xffffffff;

    private String mediumId;

    /**
     * Default constructor for null DcpAddress
     */
    DcpAddress() 
    {
    	mediumId = null;
    }

    /**
     * Copy Constructor
     * @param rhs element being copied
     */
    public DcpAddress(DcpAddress rhs) 
	{
		this();
		this.mediumId = rhs.mediumId;
	}

    public DcpAddress(long addr) 
    {
    	this();
    	setAddr(addr);
    }
    
    public DcpAddress(String str)
    {
    	this();
    	fromString(str);
    }

    /**
     * Returns long-integer representation of address. This is
     * only guaranteed to work with GOES DCP Addresses.
     * @return long-integer representation of address or InvalidAddr if the
     * medium ID is not numeric.
     */
	public long getAddr()
	{
		try { return Long.parseLong(mediumId, 16); }
		catch(NumberFormatException ex)
		{
			return InvalidAddr;
		}
	}

	public void setAddr(long addr) 
	{
		this.mediumId = toGoesDcpAddr(addr);
	}
    
    /**
       Compare two DCP Addresses by comparing the 21 significant bits. 
       Differences in the 11 error-checking bits are ignored.
       Return true if the two addresses are equal. False if not.
     */
    public boolean equals(DcpAddress addr2) 
    {
    	boolean ret = TextUtil.strEqualIgnoreCase(mediumId, addr2.mediumId);
//System.out.println("DcpAddress.equals: this='" + mediumId + "' rhs='" + addr2.mediumId + "' ret=" + ret);
    	return ret;
    }
    
    public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof DcpAddress))
			return false;

		return equals((DcpAddress)obj);
	}

	public int compareTo(DcpAddress rhs)
	{
		if (this == rhs)
			return 0;
		if (rhs == null)
			return 1;     // A null object is always greater than a non-null.
		int ret = TextUtil.strCompareIgnoreCase(mediumId, rhs.mediumId);
//System.out.println("DcpAddress.compare: this='" + mediumId + "' rhs='" + rhs.mediumId + "' ret=" + ret);
		return ret;
	}

    /**
       Convert to a string containing 8 hex digits.
     */
    public String toString() 
    {
    	return mediumId;
    }
    
    public static String toGoesDcpAddr(long addr)
    {
		StringBuilder ret = new StringBuilder(
			Long.toHexString(addr&0xffffffffL).toUpperCase());
		while(ret.length() < 8)
			ret.insert(0, '0');
		return new String(ret);
    }
    
    /**
       Convert from a string containing eight hex digits optionally
       starting with "0x". The resulting DCP address is stored
       internally.
     */
    public void fromString(String str) 
    {
    	if (str.startsWith("0x") || str.startsWith("0X"))
    		str = str.substring(2);
    	int e = str.indexOf(':');
    	if (e != -1)
    		mediumId = str.substring(0, e).trim();
    	else
    		mediumId = str.trim();
    }

	public int hashCode()
	{
		if (mediumId == null)
			return 0;
		return mediumId.toUpperCase().hashCode();
	}
}
