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

public class ApiUnit
{
	private String abbr = null;
	private String name = null;
	private String family = null;
	private String measures = null;
	
	public String getAbbr()
	{
		return abbr;
	}
	public void setAbbr(String abbr)
	{
		this.abbr = abbr;
	}
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public String getFamily()
	{
		return family;
	}
	public void setFamily(String family)
	{
		this.family = family;
	}
	public String getMeasures()
	{
		return measures;
	}
	public void setMeasures(String measures)
	{
		this.measures = measures;
	}
	
	

}
