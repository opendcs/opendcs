/*
 *  Copyright 2023 OpenDCS Consortium
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opendcs.odcsapi.beans;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;

import org.opendcs.odcsapi.util.ApiPropertiesUtil;

public class ApiDataSourceRef
{
	private Long dataSourceId = null;
	private String name = null;
	private String type = null;
	private String arguments = null;
	private int usedBy = 0;
	
	public Long getDataSourceId()
	{
		return dataSourceId;
	}
	public void setDataSourceId(Long dataSourceId)
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
			Properties props = ApiPropertiesUtil.string2props(arguments);
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
