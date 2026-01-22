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

@Schema(description = "Represents the configuration for a script sensor, including its sensor number and unit conversion details.")
public final class ApiConfigScriptSensor
{
	@Schema(description = "The unique numeric identifier of the sensor.", example = "1")
	private int sensorNumber = 0;

	@Schema(description = "The unit conversion details for the sensor.")
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
