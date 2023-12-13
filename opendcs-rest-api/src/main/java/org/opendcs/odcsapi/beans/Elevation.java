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

import java.io.Serializable;

public class Elevation
	implements Serializable
{
	private static final long serialVersionUID = -5057938869365960814L;
	private double accuracy = 0.0;
	private String datum = "NGVD29";
	private String method = "";
	private double value = 0.0;
	
	public Elevation()
	{
	}

	public double getAccuracy()
	{
		return accuracy;
	}

	public void setAccuracy(double accuracy)
	{
		this.accuracy = accuracy;
	}

	public String getDatum()
	{
		return datum;
	}

	public void setDatum(String datum)
	{
		this.datum = datum;
	}

	public String getMethod()
	{
		return method;
	}

	public void setMethod(String method)
	{
		this.method = method;
	}

	public double getValue()
	{
		return value;
	}

	public void setValue(double value)
	{
		this.value = value;
	}

}
