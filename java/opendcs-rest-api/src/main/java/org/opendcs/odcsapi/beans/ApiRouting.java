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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Represents the routing configuration for data transmission, including metadata and processing settings.")
public final class ApiRouting
{
	@Schema(description = "Unique numeric identifier for the routing configuration.", example = "20")
	private Long routingId = null;
	@Schema(description = "Name of the routing configuration.", example = "Test Routing")
	private String name = null;
	@Schema(description = "Unique numeric identifier of the associated data source.", example = "10")
	private Long dataSourceId = null;
	@Schema(description = "The name of the associated data source.", example = "USGS-LRGS")
	private String dataSourceName = null;
	@Schema(description = "Type of the data destination.", example = "directory")
	private String destinationType = null;
	@Schema(description = "Argument or configuration for the data destination.", example = "some-directory-path")
	private String destinationArg = null;
	@Schema(description = "Whether equations are enabled for processing.", example = "true")
	private boolean enableEquations = false;
	@Schema(description = "Format for output data.", example = "emit-ascii")
	private String outputFormat = null;
	@Schema(description = "Time zone for the output data.", example = "EST5EDT")
	private String outputTZ = null;
	@Schema(description = "Presentation group name associated with the routing.", example = "CWMS-English")
	private String presGroupName = null;
	@Schema(description = "Timestamp for when this routing was last modified.",
			example = "2022-03-22T17:44:15.941Z[UTC]")
	private Date lastModified = null;
	@Schema(description = "Indicates if this is a production routing configuration.", example = "false")
	private boolean isProduction = false;
	@Schema(description = "Start time for routing applicability in ISO 8601 format.", example = "2022-06-05 00:00:00.000")
	private String since = null;
	@Schema(description = "End time for routing applicability in ISO 8601 format.", example = "2022-06-06 00:00:00.000")
	private String until = null;
	@Schema(description = "Indicates if settling time delay is used. Default is false.", example = "true")
	private boolean settlingTimeDelay = false;
	@Schema(description = "Specifies the reference time for applying configurations. Default is 'Local Receive Time'.",
			example = "Both")
	private String applyTimeTo = "Local Receive Time";
	@Schema(description = "Specifies if time is in ascending order. Default is false.", example = "true")
	private boolean ascendingTime = false;

	@Schema(description = "List of platform IDs associated with the routing.")
	private List<String> platformIds = new ArrayList<>();
	@Schema(description = "List of platform names associated with the routing.")
	private List<String> platformNames = new ArrayList<>();
	@Schema(description = "List of netlist names associated with the routing.")
	private List<String> netlistNames = new ArrayList<>();
	@Schema(description = "List of GOES channels for the routing.")
	private List<Integer> goesChannels = new ArrayList<>();

	@Schema(description = "Additional configuration properties for the routing.")
	private Properties properties = new Properties();

	@Schema(description = "Indicates if the GOES DCP is self-timed.", example = "true")
	private boolean goesSelfTimed = false;
	@Schema(description = "Indicates if the GOES DCP is random format.", example = "false")
	private boolean goesRandom = false;
	@Schema(description = "Indicates if the data is sourced from a network DCP.", example = "false")
	private boolean networkDCP = false;
	@Schema(description = "Indicates if the data is sourced via the Iridium satellite network.", example = "true")
	private boolean iridium = false;
	@Schema(description = "Indicates if quality notifications are enabled.", example = "false")
	private boolean qualityNotifications = false;
	@Schema(description = "Indicates if GOES spacecraft health checks are enabled.", example = "true")
	private boolean goesSpacecraftCheck = false;
	@Schema(description = "Selection for the GOES spacecraft (e.g., East or West). Default is 'East'.", example = "East")
	private String goesSpacecraftSelection = "East";
	@Schema(description = "Indicates if parity checks are enabled. Default is false.", example = "true")
	private boolean parityCheck = false;
	@Schema(description = "Selection for parity status (e.g., Good or Bad). Default is 'Good'.", example = "Good")
	private String paritySelection = "Good";


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
	public Long getDataSourceId()
	{
		return dataSourceId;
	}
	public void setDataSourceId(Long dataSourceId)
	{
		this.dataSourceId = dataSourceId;
	}
	public String getDataSourceName()
	{
		return dataSourceName;
	}
	public void setDataSourceName(String dataSourceName)
	{
		this.dataSourceName = dataSourceName;
	}
	public String getDestinationType()
	{
		return destinationType;
	}
	public void setDestinationType(String destinationType)
	{
		this.destinationType = destinationType;
	}
	public String getDestinationArg()
	{
		return destinationArg;
	}
	public void setDestinationArg(String destinationArg)
	{
		this.destinationArg = destinationArg;
	}
	public boolean isEnableEquations()
	{
		return enableEquations;
	}
	public void setEnableEquations(boolean enableEquations)
	{
		this.enableEquations = enableEquations;
	}
	public String getOutputFormat()
	{
		return outputFormat;
	}
	public void setOutputFormat(String outputFormat)
	{
		this.outputFormat = outputFormat;
	}
	public String getOutputTZ()
	{
		return outputTZ;
	}
	public void setOutputTZ(String outputTZ)
	{
		this.outputTZ = outputTZ;
	}
	public String getPresGroupName()
	{
		return presGroupName;
	}
	public void setPresGroupName(String presGroupName)
	{
		this.presGroupName = presGroupName;
	}
	public Date getLastModified()
	{
		return lastModified;
	}
	public void setLastModified(Date lastModified)
	{
		this.lastModified = lastModified;
	}
	public boolean isProduction()
	{
		return isProduction;
	}
	public void setProduction(boolean isProduction)
	{
		this.isProduction = isProduction;
	}
	public String getSince()
	{
		return since;
	}
	public void setSince(String since)
	{
		this.since = since;
	}
	public String getUntil()
	{
		return until;
	}
	public void setUntil(String until)
	{
		this.until = until;
	}
	public boolean isSettlingTimeDelay()
	{
		return settlingTimeDelay;
	}
	public void setSettlingTimeDelay(boolean settlingTimeDelay)
	{
		this.settlingTimeDelay = settlingTimeDelay;
	}
	public String getApplyTimeTo()
	{
		return applyTimeTo;
	}
	public void setApplyTimeTo(String applyTimeTo)
	{
		this.applyTimeTo = applyTimeTo;
	}
	public List<String> getPlatformIds()
	{
		return platformIds;
	}
	public void setPlatformIds(List<String> platformIds)
	{
		this.platformIds = platformIds;
	}
	public List<String> getPlatformNames()
	{
		return platformNames;
	}
	public void setPlatformNames(List<String> platformNames)
	{
		this.platformNames = platformNames;
	}
	public List<String> getNetlistNames()
	{
		return netlistNames;
	}
	public void setNetlistNames(List<String> netlistNames)
	{
		this.netlistNames = netlistNames;
	}
	public List<Integer> getGoesChannels()
	{
		return goesChannels;
	}
	public void setGoesChannels(List<Integer> goesChannels)
	{
		this.goesChannels = goesChannels;
	}
	public Properties getProperties()
	{
		return properties;
	}
	public void setProperties(Properties properties)
	{
		this.properties = properties;
	}
	public boolean isGoesSelfTimed()
	{
		return goesSelfTimed;
	}
	public void setGoesSelfTimed(boolean goesSelfTimed)
	{
		this.goesSelfTimed = goesSelfTimed;
	}
	public boolean isGoesRandom()
	{
		return goesRandom;
	}
	public void setGoesRandom(boolean goesRandom)
	{
		this.goesRandom = goesRandom;
	}
	public boolean isNetworkDCP()
	{
		return networkDCP;
	}
	public void setNetworkDCP(boolean networkDCP)
	{
		this.networkDCP = networkDCP;
	}
	public boolean isIridium()
	{
		return iridium;
	}
	public void setIridium(boolean iridium)
	{
		this.iridium = iridium;
	}
	public boolean isQualityNotifications()
	{
		return qualityNotifications;
	}
	public void setQualityNotifications(boolean qualityNotifications)
	{
		this.qualityNotifications = qualityNotifications;
	}
	public boolean isGoesSpacecraftCheck()
	{
		return goesSpacecraftCheck;
	}
	public void setGoesSpacecraftCheck(boolean goesSpacecraftCheck)
	{
		this.goesSpacecraftCheck = goesSpacecraftCheck;
	}
	public String getGoesSpacecraftSelection()
	{
		return goesSpacecraftSelection;
	}
	public void setGoesSpacecraftSelection(String goesSpacecraftSelection)
	{
		this.goesSpacecraftSelection = goesSpacecraftSelection;
	}
	public boolean isParityCheck()
	{
		return parityCheck;
	}
	public void setParityCheck(boolean parityCheck)
	{
		this.parityCheck = parityCheck;
	}
	public String getParitySelection()
	{
		return paritySelection;
	}
	public void setParitySelection(String paritySelection)
	{
		this.paritySelection = paritySelection;
	}
	public boolean isAscendingTime()
	{
		return ascendingTime;
	}
	public void setAscendingTime(boolean ascendingTime)
	{
		this.ascendingTime = ascendingTime;
	}
	

}
