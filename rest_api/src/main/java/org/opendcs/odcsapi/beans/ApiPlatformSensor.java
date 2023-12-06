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

import java.util.Properties;

public class ApiPlatformSensor
{
	private int sensorNum = 0;
	
	/** Null means this sensor is at same site as Platform */
	private Long actualSiteId = null;
	
	/** Null means no limit is set */
	private Double min = null;
	private Double max = null;

	private Integer usgsDdno = null;
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
