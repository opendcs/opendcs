package opendcs.opentsdb.hydrojson.beans;


import java.util.HashMap;

import decodes.db.DbEnum;
import decodes.db.EnumValue;

/**
 * Adapter for DbEnum
 * @author mmaloney
 *
 */
public class RefList
{
	private String enumName = null;
	private HashMap<String, RefListItem> items = new HashMap<String, RefListItem>();
	private String defaultValue = null;
	private String description = null;
	
	public RefList(DbEnum dbEnum)
	{
		enumName = dbEnum.enumName;
		defaultValue = dbEnum.getDefault();
		description = dbEnum.getDescription();
		for(EnumValue ev : dbEnum.values())
			items.put(ev.getValue(), new RefListItem(ev));
	}

	public String getEnumName()
	{
		return enumName;
	}

	public void setEnumName(String enumName)
	{
		this.enumName = enumName;
	}

	public HashMap<String, RefListItem> getItems()
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
}
