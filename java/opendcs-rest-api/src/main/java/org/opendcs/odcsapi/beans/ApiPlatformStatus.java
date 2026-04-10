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

@Schema(description = "Represents the status of a platform, including its last contact time, message information, and annotations.")
public final class ApiPlatformStatus
{
	@Schema(description = "The unique numeric identifier of the platform.", example = "1234")
	private Long platformId = null;

	@Schema(description = "The name of the platform.", example = "Platform A")
	private String platformName = null;

	@Schema(description = "The unique numeric identifier of the site where the platform is located.", example = "5678")
	private Long siteId = null;

	@Schema(description = "The date and time of the platform's last successful contact.",
			example = "2025-01-01T12:00:00.000[UTC]")
	private Date lastContact = null;

	@Schema(description = "The date and time of the platform's last received message.",
			example = "2025-01-01T15:30:00.000[UTC]")
	private Date lastMessage = null;

	@Schema(description = "The date and time of the platform's last error.", example = "2025-01-01T14:00:00.000[UTC]")
	private Date lastError = null;

	@Schema(description = "The quality of the last received message.", example = "GOOD")
	private String lastMsgQuality = null;

	@Schema(description = "Additional annotations or notes about the platform.",
			example = "This platform requires maintenance.")
	private String annotation = null;

	@Schema(description = "The unique numeric identifier of the last routing execution.", example = "8910")
	private Long lastRoutingExecId = null;

	@Schema(description = "The name of the routing specification used during the last routing.", example = "Route 42")
	private String routingSpecName = null;

	public Long getPlatformId()
	{
		return platformId;
	}

	public void setPlatformId(Long platformId)
	{
		this.platformId = platformId;
	}

	public String getPlatformName()
	{
		return platformName;
	}

	public void setPlatformName(String platformName)
	{
		this.platformName = platformName;
	}

	public Long getSiteId()
	{
		return siteId;
	}

	public void setSiteId(Long siteId)
	{
		this.siteId = siteId;
	}

	public Date getLastContact()
	{
		return lastContact;
	}

	public void setLastContact(Date lastContact)
	{
		this.lastContact = lastContact;
	}

	public Date getLastMessage()
	{
		return lastMessage;
	}

	public void setLastMessage(Date lastMessage)
	{
		this.lastMessage = lastMessage;
	}

	public Date getLastError()
	{
		return lastError;
	}

	public void setLastError(Date lastError)
	{
		this.lastError = lastError;
	}

	public String getLastMsgQuality()
	{
		return lastMsgQuality;
	}

	public void setLastMsgQuality(String lastMsgQuality)
	{
		this.lastMsgQuality = lastMsgQuality;
	}

	public String getAnnotation()
	{
		return annotation;
	}

	public void setAnnotation(String annotation)
	{
		this.annotation = annotation;
	}

	public Long getLastRoutingExecId()
	{
		return lastRoutingExecId;
	}

	public void setLastRoutingExecId(Long lastRoutingExecId)
	{
		this.lastRoutingExecId = lastRoutingExecId;
	}

	public String getRoutingSpecName()
	{
		return routingSpecName;
	}

	public void setRoutingSpecName(String routingSpecName)
	{
		this.routingSpecName = routingSpecName;
	}

}
