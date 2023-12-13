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

public class ApiConfigScriptSensor
{
	private int sensorNumber = 0;
	private ApiUnitConverter unitConverter = null;
	
	public int getSensorNumber()
	{
		return sensorNumber;
	}
	public void setSensorNumber(int sensorNumber)
	{
		this.sensorNumber = sensorNumber;
	}
	public ApiUnitConverter getUnitConverter()
	{
		return unitConverter;
	}
	public void setUnitConverter(ApiUnitConverter unitConverter)
	{
		this.unitConverter = unitConverter;
	}
	
	public String prettyPrint()
	{
		return "sensor[" + sensorNumber + "] unitConv=" + unitConverter.getFromAbbr()
			+ "->" + unitConverter.getToAbbr() + " algo=" + unitConverter.getAlgorithm()
			+ " A=" + unitConverter.getA()
			+ " B=" + unitConverter.getB()
			+ " C=" + unitConverter.getC()
			+ " D=" + unitConverter.getD()
			+ " E=" + unitConverter.getE()
			+ " F=" + unitConverter.getF();
	}

}
