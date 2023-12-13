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

import java.util.Date;
import java.util.Properties;

public class ApiLoadingApp
{
	private Long appId = null;
	private String appName = null;
	private String appType = null;
	private String comment = null;
	private Date lastModified = null;
	private boolean manualEditingApp = false;
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
