/**
 * $Id$
 * 
 *  This is open-source software written by ILEX Engineering, Inc., under
 *  contract to the federal government. You are free to copy and use this
 *  source code for your own purposes, except that no part of the information
 *  contained in this file may be claimed to be proprietary.
 *
 * $Log$
 * Revision 1.1  2010/11/28 21:05:24  mmaloney
 * Refactoring for CCP Time-Series Groups
 *
 */
package decodes.tsdb;


public class TsGroupMember
{
	private String memberType = null;
	private String memberValue = null;
	
	public TsGroupMember(String memberType, String memberValue)
	{
		this.memberType = memberType;
		this.memberValue = memberValue;
	}

	public String getMemberType()
	{
		return memberType;
	}

	public void setMemberType(String memberType)
	{
		this.memberType = memberType;
	}

	public String getMemberValue()
	{
		return memberValue;
	}

	public void setMemberValue(String memberValue)
	{
		this.memberValue = memberValue;
	}

}
