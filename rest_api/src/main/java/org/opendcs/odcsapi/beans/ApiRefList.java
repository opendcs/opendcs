package org.opendcs.odcsapi.beans;

import java.util.HashMap;

/**
 * @author mmaloney
 *
 */
public class ApiRefList
{
	private Long reflistId = null;
	private String enumName = null;
	private HashMap<String, ApiRefListItem> items = new HashMap<String, ApiRefListItem>();
	private String defaultValue = null;
	private String description = null;
	
	public String getEnumName()
	{
		return enumName;
	}

	public void setEnumName(String enumName)
	{
		this.enumName = enumName;
	}

	public HashMap<String, ApiRefListItem> getItems()
	{
		return items;
	}

	public String getDefaultValue()
	{
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue)
	{
		this.defaultValue = defaultValue;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public Long getReflistId()
	{
		return reflistId;
	}

	public void setReflistId(Long reflistId)
	{
		this.reflistId = reflistId;
	}

	public void setItems(HashMap<String, ApiRefListItem> items)
	{
		this.items = items;
	}
}
