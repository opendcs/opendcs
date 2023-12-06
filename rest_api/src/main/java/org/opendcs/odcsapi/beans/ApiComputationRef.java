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

public class ApiComputationRef
{
	private Long computationId = null;
	private String name = null;
	private Long algorithmId = null;
	private String algorithmName = null;
	private Long processId = null;
	private String processName = null;
	private boolean enabled = false;
	private String description = null;
	private Long groupId = null;
	private String groupName = null;
	
	public ApiComputationRef() {}
	
	public Long getComputationId()
	{
		return computationId;
	}
	public void setComputationId(Long computationId)
	{
		this.computationId = computationId;
	}
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public Long getAlgorithmId()
	{
		return algorithmId;
	}
	public void setAlgorithmId(Long algorithmId)
	{
		this.algorithmId = algorithmId;
	}
	public String getAlgorithmName()
	{
		return algorithmName;
	}
	public void setAlgorithmName(String algorithmName)
	{
		this.algorithmName = algorithmName;
	}
	public Long getProcessId()
	{
		return processId;
	}
	public void setProcessId(Long processId)
	{
		this.processId = processId;
	}
	public String getProcessName()
	{
		return processName;
	}
	public void setProcessName(String processName)
	{
		this.processName = processName;
	}
	public boolean isEnabled()
	{
		return enabled;
	}
	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	}
	public String getDescription()
	{
		return description;
	}
	public void setDescription(String description)
	{
		this.description = description;
	}

	public Long getGroupId()
	{
		return groupId;
	}

	public void setGroupId(Long groupId)
	{
		this.groupId = groupId;
	}

	public String getGroupName()
	{
		return groupName;
	}

	public void setGroupName(String groupName)
	{
		this.groupName = groupName;
	}
}
