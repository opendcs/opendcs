package org.opendcs.odcsapi.beans;

import java.util.ArrayList;
import java.util.Date;

public class ApiPresentationGroup
{
	private Long groupId = null;
	
	private String name = null;
	
	private String inheritsFrom = null;
	
	private Long inheritsFromId = null;
	
	private Date lastModified = null;
	
	private boolean isProduction = false;

	private ArrayList<ApiPresentationElement> elements = new ArrayList<ApiPresentationElement>();

	public Long getGroupId()
	{
		return groupId;
	}

	public void setGroupId(Long groupId)
	{
		this.groupId = groupId;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getInheritsFrom()
	{
		return inheritsFrom;
	}

	public void setInheritsFrom(String inheritsFrom)
	{
		this.inheritsFrom = inheritsFrom;
	}

	public Long getInheritsFromId()
	{
		return inheritsFromId;
	}

	public void setInheritsFromId(Long inheritsFromId)
	{
		this.inheritsFromId = inheritsFromId;
	}

	public Date getLastModified()
	{
		return lastModified;
	}

	public void setLastModified(Date lastModified)
	{
		this.lastModified = lastModified;
	}

	public boolean isProduction()
	{
		return isProduction;
	}

	public void setProduction(boolean isProduction)
	{
		this.isProduction = isProduction;
	}

	public ArrayList<ApiPresentationElement> getElements()
	{
		return elements;
	}

	public void setElements(ArrayList<ApiPresentationElement> elements)
	{
		this.elements = elements;
	}
}
