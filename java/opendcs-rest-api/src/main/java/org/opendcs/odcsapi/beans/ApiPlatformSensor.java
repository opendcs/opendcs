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

import java.util.Properties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Represents a platform sensor, including configuration details like limits, site association, and custom properties.")
public final class ApiPlatformSensor
{
	@Schema(description = "The unique sensor number for this platform sensor.", example = "5")
	private int sensorNum = 0;

	/**
	 * Null means this sensor is at same site as Platform
	 */
	@Schema(description = "The unique numeric identifier of the site where this sensor is located. "
			+ "Null indicates it's at the same site as the platform.", example = "12345")
	private Long actualSiteId = null;

	/**
	 * Null means no limit is set
	 */
	@Schema(description = "The minimum allowed value for this sensor. Null means no minimum limit is set.",
			example = "0.0")
	private Double min = null;

	@Schema(description = "The maximum allowed value for this sensor. Null means no maximum limit is set.",
			example = "100.0")
	private Double max = null;

	@Schema(description = "The USGS Data Descriptor number associated with this sensor. "
			+ "Null or 0 indicates it's not defined.", example = "10")
	private Integer usgsDdno = null;

	@Schema(description = "Any additional properties associated with this sensor, stored as key-value pairs.")
	private Properties sensorProps = new Properties();

	public int getSensorNum()
	{
		return sensorNum;
	}

	public void setSensorNum(int sensorNum)
	{
		this.sensorNum = sensorNum;
	}

	public Long getActualSiteId()
	{
		return actualSiteId;
	}

	public void setActualSiteId(Long actualSiteId)
	{
		this.actualSiteId = actualSiteId;
	}

	public Double getMin()
	{
		return min;
	}

	public void setMin(Double min)
	{
		this.min = min;
	}

	public Double getMax()
	{
		return max;
	}

	public void setMax(Double max)
	{
		this.max = max;
	}

	public Integer getUsgsDdno()
	{
		return usgsDdno;
	}

	public void setUsgsDdno(Integer usgsDdno)
	{
		this.usgsDdno = usgsDdno;
	}

	public Properties getSensorProps()
	{
		return sensorProps;
	}

	public void setSensorProps(Properties sensorProps)
	{
		this.sensorProps = sensorProps;
	}
}
