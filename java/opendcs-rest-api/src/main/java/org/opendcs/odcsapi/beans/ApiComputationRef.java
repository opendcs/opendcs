/*
 *  Copyright 2025 OpenDCS Consortium and its Contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License")
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

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Represents a reference to a computation, including details about its algorithm, process, and group.")
public final class ApiComputationRef
{
	@Schema(description = "The unique numeric identifier of the computation.", example = "2")
	private Long computationId = null;
	@Schema(description = "The name of the computation.", example = "Daily Ave")
	private String name = null;
	@Schema(description = "The unique numeric identifier of the algorithm associated with the computation.", example = "25")
	private Long algorithmId = null;
	@Schema(description = "The name of the algorithm associated with the computation.", example = "AverageAlgorithm")
	private String algorithmName = null;
	@Schema(description = "The unique numeric identifier (PID) of the process instance associated with the computation.", example = "2158")
	private Long processId = null;
	@Schema(description = "The name of the process associated with the computation.", example = "compproc")
	private String processName = null;
	@Schema(description = "Specifies whether the computation is enabled.", example = "true")
	private boolean enabled = false;
	@Schema(description = "A detailed description of the computation.",
			example = "This computation calculates the daily average of the input data.")
	private String description = null;
	@Schema(description = "The unique numeric identifier of the group associated with the computation.", example = "16")
	private Long groupId = null;
	@Schema(description = "The name of the group associated with the computation.", example = "DailyComputations")
	private String groupName = null;

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
