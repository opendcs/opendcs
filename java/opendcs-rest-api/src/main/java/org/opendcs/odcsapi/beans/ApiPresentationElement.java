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

@Schema(description = "Represents a presentation element, including data type, units, and constraints.")
public final class ApiPresentationElement
{
	@Schema(description = "The standard name of the data type.", example = "CWMS")
	private String dataTypeStd = null;

	@Schema(description = "The code representing the data type.", example = "Elev-Pool")
	private String dataTypeCode = null;

	@Schema(description = "The units of the data.", example = "ft")
	private String units = null;

	@Schema(description = "The number of fractional digits to use, default is 2.", example = "2")
	private int fractionalDigits = 2;

	@Schema(description = "The minimum allowable value for the data.", example = "0.0")
	private Double min = null;

	@Schema(description = "The maximum allowable value for the data.", example = "1000.0")
	private Double max = null;

	public String getDataTypeStd()
	{
		return dataTypeStd;
	}
	public void setDataTypeStd(String dataTypeStd)
	{
		this.dataTypeStd = dataTypeStd;
	}
	public String getDataTypeCode()
	{
		return dataTypeCode;
	}
	public void setDataTypeCode(String dataTypeCode)
	{
		this.dataTypeCode = dataTypeCode;
	}
	public String getUnits()
	{
		return units;
	}
	public void setUnits(String units)
	{
		this.units = units;
	}
	public int getFractionalDigits()
	{
		return fractionalDigits;
	}
	public void setFractionalDigits(int fractionalDigits)
	{
		this.fractionalDigits = fractionalDigits;
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
	
	

}
