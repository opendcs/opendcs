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

public class ApiLrgsHourlyQuality
{
	private int hour = 0;
	private int numGood = 0;
	private int numBad = 0;
	private int numRecovered = 0;
	public int getHour()
	{
		return hour;
	}
	public void setHour(int hour)
	{
		this.hour = hour;
	}
	public int getNumGood()
	{
		return numGood;
	}
	public void setNumGood(int numGood)
	{
		this.numGood = numGood;
	}
	public int getNumBad()
	{
		return numBad;
	}
	public void setNumBad(int numBad)
	{
		this.numBad = numBad;
	}
	public int getNumRecovered()
	{
		return numRecovered;
	}
	public void setNumRecovered(int numRecovered)
	{
		this.numRecovered = numRecovered;
	}
}
