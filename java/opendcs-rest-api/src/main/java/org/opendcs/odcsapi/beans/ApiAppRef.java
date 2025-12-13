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

import java.util.Date;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Represents a reference to a computation process (application) with its details, "
		+ "including type, name, and last modified date.")
public final class ApiAppRef
{
	@Schema(description = "The unique identifier of the application.", example = "35")
	private Long appId = null;

	@Schema(description = "The name of the application.", example = "compproc")
	private String appName = null;

	@Schema(description = "The type of the application.", example = "computationprocess")
	private String appType = null;

	@Schema(description = "Additional comments about the application.", example = "Main Computation Process")
	private String comment = null;

	@Schema(description = "The last modified timestamp of the application record.",
			example = "2022-03-30T20:49:44Z[UTC]")
	private Date lastModified = null;

	public Long getAppId()
	{
		return appId;
	}
	public void setAppId(Long appId)
	{
		this.appId = appId;
	}
	public String getAppName()
	{
		return appName;
	}
	public void setAppName(String appName)
	{
		this.appName = appName;
	}
	public String getAppType()
	{
		return appType;
	}
	public void setAppType(String appType)
	{
		this.appType = appType;
	}
	public String getComment()
	{
		return comment;
	}
	public void setComment(String comment)
	{
		this.comment = comment;
	}
	public Date getLastModified()
	{
		return lastModified;
	}
	public void setLastModified(Date lastModified)
	{
		this.lastModified = lastModified;
	}
}
