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

import java.io.Serializable;
import java.util.ArrayList;

public class ApiSearchCrit implements Serializable
{
	public static final String ATTRIBUTE = "api-search-crit";
	private static final long serialVersionUID = -1770468107162716859L;

	private String since = null;
	private String until = null;
	private boolean settlingTimeDelay = false;
	private ArrayList<String> platformIds = new ArrayList<String>();
	private ArrayList<String> platformNames = new ArrayList<String>();
	private ArrayList<String> netlistNames = new ArrayList<String>();
	private ArrayList<Integer> goesChannels = new ArrayList<Integer>();
	private boolean goesSelfTimed = false;
	private boolean goesRandom = false;
	private boolean networkDCP = false;
	private boolean iridium = false;
	private boolean qualityNotifications = false;
	private boolean goesSpacecraftCheck = false;
	private String goesSpacecraftSelection = "East";
	private boolean parityCheck = false;
	private String paritySelection = "Good";
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
	public ArrayList<String> getPlatformIds()
	{
		return platformIds;
	}
	public void setPlatformIds(ArrayList<String> platformIds)
	{
		this.platformIds = platformIds;
	}
	public ArrayList<String> getPlatformNames()
	{
		return platformNames;
	}
	public void setPlatformNames(ArrayList<String> platformNames)
	{
		this.platformNames = platformNames;
	}
	public ArrayList<String> getNetlistNames()
	{
		return netlistNames;
	}
	public void setNetlistNames(ArrayList<String> netlistNames)
	{
		this.netlistNames = netlistNames;
	}
	public ArrayList<Integer> getGoesChannels()
	{
		return goesChannels;
	}
	public void setGoesChannels(ArrayList<Integer> goesChannels)
	{
		this.goesChannels = goesChannels;
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

}
