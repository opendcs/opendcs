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

import java.util.ArrayList;

public class ApiDecodesTimeSeries
{
	private int sensorNum = 0;
	private String sensorName = null;
	private String units = null;
	private ArrayList<ApiDecodesTSValue> values = new ArrayList<ApiDecodesTSValue>();
	public int getSensorNum()
	{
		return sensorNum;
	}
	public void setSensorNum(int sensorNum)
	{
		this.sensorNum = sensorNum;
	}
	public ArrayList<ApiDecodesTSValue> getValues()
	{
		return values;
	}
	public void setValues(ArrayList<ApiDecodesTSValue> values)
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
