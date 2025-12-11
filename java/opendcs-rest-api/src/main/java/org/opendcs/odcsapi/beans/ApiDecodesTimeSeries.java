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

import java.util.ArrayList;
import java.util.List;

@Schema(description = "Represents a decoded time series, including sensor details, units, and associated time series values.")
public final class ApiDecodesTimeSeries
{
	@Schema(description = "The unique sensor number associated with this time series.", example = "20")
	private int sensorNum = 0;

	@Schema(description = "The name of the sensor associated with this time series.", example = "Stage")
	private String sensorName = null;

	@Schema(description = "The unit of measurement for this time series.", example = "m")
	private String units = null;

	@Schema(description = "List of time series values associated with this sensor.")
	private List<ApiDecodesTSValue> values = new ArrayList<>();

	public int getSensorNum()
	{
		return sensorNum;
	}

	public void setSensorNum(int sensorNum)
	{
		this.sensorNum = sensorNum;
	}

	public List<ApiDecodesTSValue> getValues()
	{
		return values;
	}

	public void setValues(List<ApiDecodesTSValue> values)
	{
		this.values = values;
	}

	public String getSensorName()
	{
		return sensorName;
	}

	public void setSensorName(String sensorName)
	{
		this.sensorName = sensorName;
	}

	public String getUnits()
	{
		return units;
	}

	public void setUnits(String units)
	{
		this.units = units;
	}


}
