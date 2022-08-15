package opendcs.opentsdb.hydrojson.beans;

import decodes.db.EnumValue;

/**
 * Adapter for EnumValue
 * @author mmaloney
 *
 */
public class RefListItem
{
	private String value;
	private String description;
	private String execClassName;
	private String editClassName;
	private Integer sortNumber;
	
	public RefListItem(EnumValue ev)
	{
		value = ev.getValue();
		description = ev.getDescription();
		execClassName = ev.getExecClassName();
		editClassName = ev.getEditClassName();
		sortNumber = ev.getSortNumber() == EnumValue.UNDEFINED_SORT_NUMBER ? null :
			ev.getSortNumber();
	}

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
