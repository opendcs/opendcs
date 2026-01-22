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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Represents the configuration details of a sensor including its recording mode, interval, and other properties.")
public final class ApiConfigSensor
{
	@Schema(description = "The unique number identifying the sensor.", example = "13")
	private int sensorNumber = 0;

	@Schema(description = "The name assigned to the sensor.", example = "WL")
	private String sensorName = null;

	/**
	 * V=Variable, F=Fixed, U=undefined
	 */
	@JsonProperty("recordingMode")
	@Schema(description = "The sensor's recording mode. Possible values from the RecordingMode enum are V (Variable), " +
			"F (Fixed), or U (Undefined).",
			example = "F")
	private RecordingMode recordingMode = RecordingMode.UNDEFINED; // undefined

	@Schema(description = "The duration between successive samples in seconds. Default is 3600.", example = "3600")
	private int recordingInterval = 3600;

	/**
	 * The time-of-day of the first sample, in seconds since midnight.
	 */
	@Schema(description = "The time in seconds past midnight of the sensor's first sample.", example = "0")
	private int timeOfFirstSample = 0;

	@Schema(description = "The absolute minimum value for a data sample from this sensor.", example = "0.0")
	private Double absoluteMin = null;

	@Schema(description = "The absolute maximum value for a data sample from this sensor.", example = "100.0")
	private Double absoluteMax = null;

	@Schema(description = "A collection of sensor-specific properties defined as key-value pairs.")
	private Properties properties = new Properties();

	@Schema(description = "A map of data type identifiers and their corresponding descriptions.")
	private Map<String, String> dataTypes = new HashMap<>();

	@Schema(description = "The USGS statistical code associated with the sensor. Null means undefined.")
	private String usgsStatCode = null;

	public int getSensorNumber()
	{
		return sensorNumber;
	}

	public void setSensorNumber(int sensorNumber)
	{
		this.sensorNumber = sensorNumber;
	}

	public String getSensorName()
	{
		return sensorName;
	}

	public void setSensorName(String sensorName)
	{
		this.sensorName = sensorName;
	}

	public RecordingMode getRecordingMode()
	{
		return recordingMode;
	}

	public void setRecordingMode(RecordingMode recordingMode)
	{
		this.recordingMode = recordingMode;
	}

	public int getRecordingInterval()
	{
		return recordingInterval;
	}

	public void setRecordingInterval(int recordingInterval)
	{
		this.recordingInterval = recordingInterval;
	}

	public int getTimeOfFirstSample()
	{
		return timeOfFirstSample;
	}

	public void setTimeOfFirstSample(int timeOfFirstSample)
	{
		this.timeOfFirstSample = timeOfFirstSample;
	}

	public Double getAbsoluteMin()
	{
		return absoluteMin;
	}

	public void setAbsoluteMin(Double absoluteMin)
	{
		this.absoluteMin = absoluteMin;
	}

	public Double getAbsoluteMax()
	{
		return absoluteMax;
	}

	public void setAbsoluteMax(Double absoluteMax)
	{
		this.absoluteMax = absoluteMax;
	}

	public Properties getProperties()
	{
		return properties;
	}

	public void setProperties(Properties properties)
	{
		this.properties = properties;
	}

	public Map<String, String> getDataTypes()
	{
		return dataTypes;
	}

	public void setDataTypes(Map<String, String> dataTypes)
	{
		this.dataTypes = dataTypes;
	}

	public String getUsgsStatCode()
	{
		return usgsStatCode;
	}

	public void setUsgsStatCode(String usgsStatCode)
	{
		this.usgsStatCode = usgsStatCode;
	}

	public enum RecordingMode
	{
		VARIABLE('V'),
		FIXED('F'),
		UNDEFINED('U');

		private final char code;

		RecordingMode(char code)
		{
			this.code = code;
		}

		@JsonValue
		public char getCode()
		{
			return code;
		}

		public static RecordingMode fromChar(char code)
		{
			for (RecordingMode mode : values())
			{
				if (mode.code == code)
				{
					return mode;
				}
			}
			return UNDEFINED;
		}
	}
}
