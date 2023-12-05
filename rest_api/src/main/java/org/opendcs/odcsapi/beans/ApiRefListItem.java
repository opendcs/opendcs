package org.opendcs.odcsapi.beans;

/**
 * Adapter for EnumValue
 * @author mmaloney
 *
 */
public class ApiRefListItem
{
	private String value;
	private String description;
	private String execClassName;
	private String editClassName;
	private Integer sortNumber;
	
	public String getValue()
	{
		return value;
	}

	public void setValue(String value)
	{
		this.value = value;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public String getExecClassName()
	{
		return execClassName;
	}

	public void setExecClassName(String execClassName)
	{
		this.execClassName = execClassName;
	}

	public String getEditClassName()
	{
		return editClassName;
	}

	public void setEditClassName(String editClassName)
	{
		this.editClassName = editClassName;
	}

	public Integer getSortNumber()
	{
		return sortNumber;
	}

	public void setSortNumber(Integer sortNumber)
	{
		this.sortNumber = sortNumber;
	}

}
