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
import java.util.Properties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Represents a computation application, including ID, name, type, and configuration details.")
public final class ApiLoadingApp
{
	@Schema(description = "The unique numeric identifier of the application.", example = "56")
	private Long appId = null;

	@Schema(description = "The name of the application.", example = "compproc")
	private String appName = null;

	@Schema(description = "The type of the application.", example = "computationprocess")
	private String appType = null;

	@Schema(description = "A descriptive comment about the application.", example = "Main Computation Process")
	private String comment = null;

	@Schema(description = "The date and time the application was last modified.",
			example = "2025-01-01T00:00:00.000[UTC]")
	private Date lastModified = null;

	@Schema(description = "Specifies if this is a manual editing application.", example = "false")
	private boolean manualEditingApp = false;

	@Schema(description = "Additional properties associated with the application.")
	private Properties properties = new Properties();

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
	public boolean isManualEditingApp()
	{
		return manualEditingApp;
	}
	public void setManualEditingApp(boolean manualEditingApp)
	{
		this.manualEditingApp = manualEditingApp;
	}
	public Properties getProperties()
	{
		return properties;
	}
	public void setProperties(Properties properties)
	{
		this.properties = properties;
	}
}
