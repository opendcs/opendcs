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
import java.util.Properties;

public class ApiDataSource
{
	private Long dataSourceId = null;
	private String name = null;
	private String type = null;
	private int usedBy = 0;
	private Properties props = null;
	private ArrayList<ApiDataSourceGroupMember> groupMembers = new ArrayList<ApiDataSourceGroupMember>();
	
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
	public int getUsedBy()
	{
		return usedBy;
	}
	public void setUsedBy(int usedBy)
	{
		this.usedBy = usedBy;
	}
	public Properties getProps()
	{
		return props;
	}
	public void setProps(Properties props)
	{
		this.props = props;
	}
	public ArrayList<ApiDataSourceGroupMember> getGroupMembers()
	{
		return groupMembers;
	}
	public void setGroupMembers(ArrayList<ApiDataSourceGroupMember> groupMembers)
	{
		this.groupMembers = groupMembers;
	}

}
