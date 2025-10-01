/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package lrgs.common;

import ilex.util.TextUtil;

/**
* Class DCP Address encapsulates a GOES-DCS DCP Address
*/
public class DcpAddress implements Comparable<DcpAddress>
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
