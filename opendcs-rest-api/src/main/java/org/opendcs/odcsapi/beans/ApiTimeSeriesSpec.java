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

import decodes.sql.DbKey;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Represents the specification of a time series, including metadata and configuration details.")
public final class ApiTimeSeriesSpec
{
	@Schema(description = "The time series identifier (TSID) containing unique metadata for the time series.")
	private ApiTimeSeriesIdentifier tsid = null;

	@Schema(description = "The location associated with the time series.", example = "MROI4")
	private String location = null;
	@Schema(description = "The parameter measured by the time series (e.g., temperature, flow rate).", example = "Stage")
	private String param = null;
	@Schema(description = "Statistical code representing the type of calculation applied to the time series.", example = "Inst")
	private String statCode = null;
	@Schema(description = "The interval of time between data points in the time series.", example = "30Minutes")
	private String interval = null;
	@Schema(description = "The duration of time that each time series value represents.", example = "0")
	private String duration = null;
	@Schema(description = "Version identifier for the time series.", example = "raw")
	private String version = null;

	@Schema(description = "The unique numeric identifier for the site associated with the time series.", example = "6")
	private DbKey siteId = null;
	@Schema(description = "The unique numeric identifier for the datatype used in the time series.", example = "48")
	private DbKey datatypeId = null;
	@Schema(description = "The unique numeric identifier for the interval used in the time series.", example = "12")
	private DbKey intervalId = null;
	@Schema(description = "The unique numeric identifier for the duration used in the time series.", example = "36")
	private DbKey durationId = null;

	@Schema(description = "The date and time when the time series configuration was last modified.",
			example = "2020-05-11T20:50:55.795Z[UTC]")
	private Date lastModified = null;

	@Schema(description = "Indicates whether the time series is active.", example = "true")
	private boolean active = true;
	@Schema(description = "Indicates whether Daylight Saving Time (DST) variations are allowed in the time series.",
			example = "false")
	private boolean allowDSTVariation = false;
	@Schema(description = "The UTC offset (in hours) for the time series.", example = "0")
	private int utcOffset = 0;
	@Schema(description = "The action taken when offset errors occur. Valid values: IGNORE, REJECT, or ROUND.",
			example = "IGNORE")
	private String offsetErrorAction = "IGNORE"; // IGNORE, REJECT, or ROUND
	@Schema(description = "The type of storage for the time series data. Valid values: N (Numeric), S (String).",
			example = "N")
	private char storageType = 'N'; // N=Numeric, S=String
	@Schema(description = "The index of the storage table where time series data is stored.", example = "2")
	private int storageTable = 0;

	// Not really part of the spec, but returned with it:
	@Schema(description = "The number of values currently stored in the time series.", example = "144")
	private int numValues = 0;
	@Schema(description = "The minimum value across the time series.")
	private ApiTimeSeriesValue min = null;
	@Schema(description = "The maximum value across the time series.")
	private ApiTimeSeriesValue max = null;
	@Schema(description = "The oldest value in the time series.")
	private ApiTimeSeriesValue oldest = null;
	@Schema(description = "The most recent value in the time series.")
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
	public DbKey getSiteId()
	{
		return siteId;
	}
	public void setSiteId(DbKey siteId)
	{
		this.siteId = siteId;
	}
	public DbKey getDatatypeId()
	{
		return datatypeId;
	}
	public void setDatatypeId(DbKey datatypeId)
	{
		this.datatypeId = datatypeId;
	}
	public DbKey getIntervalId()
	{
		return intervalId;
	}
	public void setIntervalId(DbKey intervalId)
	{
		this.intervalId = intervalId;
	}
	public DbKey getDurationId()
	{
		return durationId;
	}
	public void setDurationId(DbKey durationId)
	{
		this.durationId = durationId;
	}
}
