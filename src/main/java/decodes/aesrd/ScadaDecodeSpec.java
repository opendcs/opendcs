/*
 * This software was written by Cove Software, LLC ("COVE") under contract
 * to Alberta Environment and Sustainable Resource Development (Alberta ESRD).
 * No warranty is provided or implied other than specific contractual terms 
 * between COVE and Alberta ESRD.
 *
 * Copyright 2014 Alberta Environment and Sustainable Resource Development.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package decodes.aesrd;

import java.util.StringTokenizer;

import decodes.tsdb.NoSuchObjectException;

public class ScadaDecodeSpec
{
	/** Label in the scdalst.in file */
	public String label = null;
	
	public String newleafSite = null;
	
	public String sensorCodes[] = null;
	
	/**
	 * Construct a ScadaDecodeSpec from a line read from the scdalst.in file.
	 * Throw NoSuchObjectException if parse error.
	 * @param scdalst_line
	 */
	public ScadaDecodeSpec(String scdalst_line)
		throws NoSuchObjectException
	{
		if (scdalst_line.length() < 38)
			throw new NoSuchObjectException("line too short");
		if (!Character.isLetter(scdalst_line.charAt(0)))
			throw new NoSuchObjectException("First char must be a letter.");
		this.label = scdalst_line.substring(0, 21).trim();
		this.newleafSite = scdalst_line.substring(26, 36).trim();
		int numSensors = (int)scdalst_line.charAt(36) - (int)'0';
		if (numSensors <=0 || numSensors > 9)
			throw new NoSuchObjectException("Invalid numSensors '"
				+ scdalst_line.charAt(36) + "'");
		sensorCodes = new String[numSensors];
		StringTokenizer st = new StringTokenizer(scdalst_line.substring(37));
		for(int idx = 0; idx < numSensors; idx++)
		{
			String code = st.hasMoreTokens() ? st.nextToken() : null;
			if (code != null && code.equals("XX"))
				code = null;
			sensorCodes[idx] = code;
		}
	}

	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(label + "," + newleafSite + "," + sensorCodes.length);
		for(int idx = 0; idx < sensorCodes.length; idx++)
			sb.append("," + sensorCodes[idx]);
		return sb.toString();
	}
	
	public String getLabel()
	{
		return label;
	}

	public void setLabel(String label)
	{
		this.label = label;
	}

	public String getNewleafSite()
	{
		return newleafSite;
	}

	public void setNewleafSite(String newleafSite)
	{
		this.newleafSite = newleafSite;
	}

	public String[] getSensorCodes()
	{
		return sensorCodes;
	}

	public void setSensorCodes(String[] sensorCodes)
	{
		this.sensorCodes = sensorCodes;
	}

}
