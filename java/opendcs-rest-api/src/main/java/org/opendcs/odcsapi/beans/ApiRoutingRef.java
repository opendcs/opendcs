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

@Schema(description = "Represents a reference to a routing configuration, including metadata such as name, "
		+ "data source, and modification time.")
public final class ApiRoutingRef
{
	@Schema(description = "Unique numeric identifier of the routing configuration.", example = "3")
	private Long routingId = null;

	@Schema(description = "Name of the routing configuration.", example = "OKVI4-input")
	private String name = null;

	@Schema(description = "The name of the data source associated with the routing configuration.", example = "OKVI4")
	private String dataSourceName = null;

	@Schema(description = "The destination of the routing configuration.", example = "pipe()")
	private String destination = null;

	@Schema(description = "Timestamp when the routing configuration was last modified.",
			example = "2020-05-11T20:24:53.052Z[UTC]")
	private Date lastModified = null;

	public Long getRoutingId()
	{
		return routingId;
	}

	public void setRoutingId(Long routingId)
	{
		this.routingId = routingId;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getDataSourceName()
	{
		return dataSourceName;
	}

	public void setDataSourceName(String dataSourceName)
	{
		this.dataSourceName = dataSourceName;
	}

	public String getDestination()
	{
		return destination;
	}

	public void setDestination(String consumer)
	{
		this.destination = consumer;
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
