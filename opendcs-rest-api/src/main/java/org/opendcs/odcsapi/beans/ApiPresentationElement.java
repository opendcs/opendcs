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

public class ApiPresentationElement
{
	private String dataTypeStd = null;
	private String dataTypeCode = null;
	private String units = null;
	private int fractionalDigits = 2;
	private Double min = null;
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
