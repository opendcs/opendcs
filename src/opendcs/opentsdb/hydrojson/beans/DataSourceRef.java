package opendcs.opentsdb.hydrojson.beans;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;

import decodes.sql.DbKey;
import ilex.util.PropertiesUtil;

public class DataSourceRef
{
	private long dataSourceId = DbKey.NullKey.getValue();
	private String name = null;
	private String type = null;
	private String arguments = null;
	private int usedBy = 0;
	
	public long getDataSourceId()
	{
		return dataSourceId;
	}
	public void setDataSourceId(long dataSourceId)
	{
		this.dataSourceId = dataSourceId;
	}
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public String getType()
	{
		return type;
	}
	public void setType(String type)
	{
		this.type = type;
	}
	public String getArguments()
	{
		return arguments;
	}
	public void setArguments(String arguments)
	{
		this.arguments = null;
		if (arguments != null && arguments.trim().length() > 0)
		{
			Properties props = PropertiesUtil.string2props(arguments);
			if (props == null)
				return;
			ArrayList<String> names = new ArrayList<String>();
			for(Object k : props.keySet())
				names.add((String)k);
			if (names.size() == 0)
				return;
			Collections.sort(names);
			StringBuilder sb = new StringBuilder();
			for(String n : names)
			{
				if (sb.length() > 0)
					sb.append(", ");
				if (n.equalsIgnoreCase("password"))
					sb.append("password=****");
				else
					sb.append(n + "=" + props.getProperty(n));
			}
			this.arguments = sb.toString();
		}
	}
	public int getUsedBy()
	{
		return usedBy;
	}
	public void setUsedBy(int usedBy)
	{
		this.usedBy = usedBy;
	}
	


}
