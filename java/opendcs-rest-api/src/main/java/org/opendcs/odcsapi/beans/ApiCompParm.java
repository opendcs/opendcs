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

/**
 * This needs to be updated to be more "I've expanded everything to so it can be rendered in the UI or saved to the database
 * " as appropriate to a given implementation... *without* any "if this impl" checks.
 * ApiCompParm
 */
@Schema(description = "Represents a computation parameter used in algorithms, including details about time series, "
		+ "sites, data types, intervals, and HDB/CWMS specifics.")
public final class ApiCompParm
{
	@Schema(description = "The type of the algorithm parameter. Can be input (i) or output (o)", example = "i")
	private String algoParmType;

	@Schema(description = "The role name of the algorithm parameter.", example = "output3")
	private String algoRoleName = null;

	// Non-group comp parms will be completely defined with a time series key.
	@Schema(description = "The time series key associated with this computation parameter.", example = "1")
	private Long tsKey = null;

	@Schema(description = "The identifier of the data type associated with this computation parameter.", example = "48")
	private Long dataTypeId = null;

	@Schema(description = "The name of the data type associated with this computation parameter.", example = "Stage")
	private String dataType = null;

	@Schema(description = "The string interval code. See IntervalCodes for more information.", example = "15Minutes")
	private String interval = null;

	@Schema(description = "Signed number of time intervals (seconds, minutes, etc.). "
			+ "Offset for retrieving this parameter relative to zero-based time.", example = "0")
	private int deltaT = 0;

	@Schema(description = "The time units for deltaT (seconds, minutes, etc.), default is 'Seconds'.", example = "Hours")
	private String deltaTUnits = "Seconds";

	@Schema(description = "The abbreviation for the units of the parameter.", example = "ft")
	private String unitsAbbr = null;

	@Schema(description = "The identifier of the site associated with this computation parameter.", example = "1")
	private Long siteId = null;

	@Schema(description = "The name of the site associated with this computation parameter.", example = "OKVI4")
	private String siteName = null;

	// HDB-specific fields:
	// NOTE: This field is *not* HDB specific. The simple case in the example of "R_" is hdb specific.
	// However, due to the history of how the computation processor evolved for CWMS the parameter type, duration,
	// and version are encoded into this field as the database table does not contain columns for them.
	// For anyone how wants a good project... this entire class needs to be restructured to be
	// an interface with the absolute basics, with derived classes for specific behavior.
	// Additionally the UI itself needs to adapt. See the 7.0 Swing UI for some reference on the differences
	@Schema(description = "HDB-specific: The table selector for this computation parameter.", example = "R_")
	private String tableSelector = null;

	// NOTE this is the only actually "HDB specific" field in this class. Other implementations
	// Make sure of various other fields in different ways.
	@Schema(description = "HDB-specific: The model ID associated with this computation parameter.")
	private Integer modelId = null;

	// CWMS-specific fields:
	@Schema(description = "CWMS-specific: The type of this parameter.", example = "Inst")
	private String paramType = null;

	@Schema(description = "CWMS-specific: The duration for this computation parameter.", example = "0")
	private String duration = null;

	@Schema(description = "CWMS-specific: The version of this computation parameter.", example = "raw")
	private String version = null;

	@Schema(description = "CWMS-specific: Action to take if the parameter is missing.", example = "IGNORE")
	private String ifMissing = null;

	public String getAlgoParmType()
	{
		return algoParmType;
	}
	public void setAlgoParmType(String algoParmType)
	{
		this.algoParmType = algoParmType;
	}
	public String getAlgoRoleName()
	{
		return algoRoleName;
	}
	public void setAlgoRoleName(String algoRoleName)
	{
		this.algoRoleName = algoRoleName;
	}
	public String getInterval()
	{
		return interval;
	}
	public void setInterval(String interval)
	{
		this.interval = interval;
	}
	public int getDeltaT()
	{
		return deltaT;
	}
	public void setDeltaT(int deltaT)
	{
		this.deltaT = deltaT;
	}
	public String getDeltaTUnits()
	{
		return deltaTUnits;
	}
	public void setDeltaTUnits(String deltaTUnits)
	{
		this.deltaTUnits = deltaTUnits;
	}
	public String getUnitsAbbr()
	{
		return unitsAbbr;
	}
	public void setUnitsAbbr(String unitsAbbr)
	{
		this.unitsAbbr = unitsAbbr;
	}
	public Long getSiteId()
	{
		return siteId;
	}
	public void setSiteId(Long siteId)
	{
		this.siteId = siteId;
	}
	public String getSiteName()
	{
		return siteName;
	}
	public void setSiteName(String siteName)
	{
		this.siteName = siteName;
	}
	public Long getDataTypeId()
	{
		return dataTypeId;
	}
	public void setDataTypeId(Long dataTypeId)
	{
		this.dataTypeId = dataTypeId;
	}
	public String getDataType()
	{
		return dataType;
	}
	public void setDataType(String dataType)
	{
		this.dataType = dataType;
	}


	public String getTableSelector()
	{
		return tableSelector;
	}
	public void setTableSelector(String tableSelector)
	{
		this.tableSelector = tableSelector;
	}
	public Integer getModelId()
	{
		return modelId;
	}
	public void setModelId(Integer modelId)
	{
		this.modelId = modelId;
	}

	public String getParamType()
	{
		return paramType;
	}
	public void setParamType(String paramType)
	{
		this.paramType = paramType;
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
	public String getIfMissing()
	{
		return ifMissing;
	}
	public void setIfMissing(String ifMissing)
	{
		this.ifMissing = ifMissing;
	}

	public Long getTsKey()
	{
		return tsKey;
	}

	public void setTsKey(Long tsKey)
	{
		this.tsKey = tsKey;
	}


}
