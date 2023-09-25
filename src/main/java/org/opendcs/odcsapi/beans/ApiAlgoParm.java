package org.opendcs.odcsapi.beans;

public class ApiAlgoParm
{
	/** The role name -- must be unique within an algorithm. */
	private String roleName;
	
	/** The parameter type -- one of the constant codes defined herein. */
	private String parmType;

	public String getRoleName()
	{
		return roleName;
	}

	public void setRoleName(String roleName)
	{
		this.roleName = roleName;
	}

	public String getParmType()
	{
		return parmType;
	}

	public void setParmType(String parmType)
	{
		this.parmType = parmType;
	}


}
