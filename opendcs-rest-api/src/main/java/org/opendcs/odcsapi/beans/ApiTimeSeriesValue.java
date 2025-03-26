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

@Schema(description = "Represents a time series value, including timestamp, value, and flags.")
public final class ApiTimeSeriesValue
{
	@Schema(description = "The time the value was sampled.", example = "2023-10-15T12:34:56Z")
	private Date sampleTime = null;

	@Schema(description = "The recorded value.", example = "123.45")
	private double value = 0.0;

	@Schema(description = "Flags associated with the value, encoded as a long integer.", example = "1")
	private long flags = 0;

	public ApiTimeSeriesValue()
	{
	}
	
	public ApiTimeSeriesValue(Date sampleTime, double value, long flags)
	{
		super();
		this.sampleTime = sampleTime;
		this.value = value;
		this.flags = flags;
	}

	public Date getSampleTime()
	{
		return sampleTime;
	}

	public void setSampleTime(Date sampleTime)
	{
		this.sampleTime = sampleTime;
	}

	public double getValue()
	{
		return value;
	}

	public void setValue(double value)
	{
		this.value = value;
	}

	public long getFlags()
	{
		return flags;
	}

	public void setFlags(long flags)
	{
		this.flags = flags;
	}
}
