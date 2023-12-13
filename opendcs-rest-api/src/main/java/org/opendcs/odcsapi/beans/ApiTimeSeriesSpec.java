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

public class ApiTimeSeriesSpec
{
	private ApiTimeSeriesIdentifier tsid = null;
	
	private String location = null;
	private String param = null;
	private String statCode = null;
	private String interval = null;
	private String duration = null;
	private String version = null;
	
	private Long siteId = null;
	private Long datatypeId = null;
	private Long intervalId = null;
	private Long durationId = null;
	
	private Date lastModified = null;
	
	private boolean active = true;
	private boolean allowDSTVariation = false;
	private int utcOffset = 0;
	private String offsetErrorAction = "IGNORE"; // IGNORE, REJECT, or ROUND
	private char storageType = 'N'; // N=Numeric, S=String
	private int storageTable = 0;
	
	// Not really part of the spec, but returned with it:
	private int numValues = 0;
	private ApiTimeSeriesValue min = null;
	private ApiTimeSeriesValue max = null;
	private ApiTimeSeriesValue oldest = null;
	private ApiTimeSeriesValue newest = null;
	
	
	public ApiTimeSeriesIdentifier getTsid()
	{
		return tsid;
	}
	public void setTsid(ApiTimeSeriesIdentifier tsid)
	{
		this.tsid = tsid;
	}
	public String getLocation()
	{
		return location;
	}
	public void setLocation(String location)
	{
		this.location = location;
	}
	public String getParam()
	{
		return param;
	}
	public void setParam(String param)
	{
		this.param = param;
	}
	public String getStatCode()
	{
		return statCode;
	}
	public void setStatCode(String statCode)
	{
		this.statCode = statCode;
	}
	public String getInterval()
	{
		return interval;
	}
	public void setInterval(String interval)
	{
		this.interval = interval;
	}
	public String getDuration()
	{
		return duration;
	}
	public void setDuration(String duration)
	{
		this.duration = duration;
	}
	public String getVersion()
	{
		return version;
	}
	public void setVersion(String version)
	{
		this.version = version;
	}
	public Date getLastModified()
	{
		return lastModified;
	}
	public void setLastModified(Date lastModified)
	{
		this.lastModified = lastModified;
	}
	public boolean isActive()
	{
		return active;
	}
	public void setActive(boolean active)
	{
		this.active = active;
	}
	public boolean isAllowDSTVariation()
	{
		return allowDSTVariation;
	}
	public void setAllowDSTVariation(boolean allowDSTVariation)
	{
		this.allowDSTVariation = allowDSTVariation;
	}
	public int getUtcOffset()
	{
		return utcOffset;
	}
	public void setUtcOffset(int utcOffset)
	{
		this.utcOffset = utcOffset;
	}
	public String getOffsetErrorAction()
	{
		return offsetErrorAction;
	}
	public void setOffsetErrorAction(String offsetErrorAction)
	{
		this.offsetErrorAction = offsetErrorAction;
	}
	public char getStorageType()
	{
		return storageType;
	}
	public void setStorageType(char storageType)
	{
		this.storageType = storageType;
	}
	public int getStorageTable()
	{
		return storageTable;
	}
	public void setStorageTable(int storageTable)
	{
		this.storageTable = storageTable;
	}
	public int getNumValues()
	{
		return numValues;
	}
	public void setNumValues(int numValues)
	{
		this.numValues = numValues;
	}
	public ApiTimeSeriesValue getMin()
	{
		return min;
	}
	public void setMin(ApiTimeSeriesValue min)
	{
		this.min = min;
	}
	public ApiTimeSeriesValue getMax()
	{
		return max;
	}
	public void setMax(ApiTimeSeriesValue max)
	{
		this.max = max;
	}
	public ApiTimeSeriesValue getOldest()
	{
		return oldest;
	}
	public void setOldest(ApiTimeSeriesValue oldest)
	{
		this.oldest = oldest;
	}
	public ApiTimeSeriesValue getNewest()
	{
		return newest;
	}
	public void setNewest(ApiTimeSeriesValue newest)
	{
		this.newest = newest;
	}
	public Long getSiteId()
	{
		return siteId;
	}
	public void setSiteId(Long siteId)
	{
		this.siteId = siteId;
	}
	public Long getDatatypeId()
	{
		return datatypeId;
	}
	public void setDatatypeId(Long datatypeId)
	{
		this.datatypeId = datatypeId;
	}
	public Long getIntervalId()
	{
		return intervalId;
	}
	public void setIntervalId(Long intervalId)
	{
		this.intervalId = intervalId;
	}
	public Long getDurationId()
	{
		return durationId;
	}
	public void setDurationId(Long durationId)
	{
		this.durationId = durationId;
	}
}
