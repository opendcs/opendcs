/*
 *  Copyright 2024 OpenDCS Consortium and its Contributors
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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ApiAlgorithm
{
	/** Surrogate key for this algorithm in the time series database.  */
	private Long algorithmId = null;

	/** Name of this algorithm */
	private String name = null;

	/** Fully qualified Java class name to execut this algorithm. */
	private String execClass = null;

	/** Free form multi-line comment */
	private String description = null;

	/** Properties associated with this algorithm. */
	private Properties props = new Properties();
	
	/** parameters to this algorithm */
	private List<ApiAlgoParm> parms = new ArrayList<>();

	/** For use in the editor -- the number of computations using this algo. */
	private int numCompsUsing = 0;
	
	private List<ApiAlgorithmScript> algoScripts = new ArrayList<>();

	public Long getAlgorithmId()
	{
		return algorithmId;
	}

	public void setAlgorithmId(Long algorithmId)
	{
		this.algorithmId = algorithmId;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getExecClass()
	{
		return execClass;
	}

	public void setExecClass(String execClass)
	{
		this.execClass = execClass;
	}

	public Properties getProps()
	{
		return props;
	}

	public void setProps(Properties props)
	{
		this.props = props;
	}

	public List<ApiAlgoParm> getParms()
	{
		return parms;
	}

	public void setParms(List<ApiAlgoParm> parms)
	{
		this.parms = parms;
	}

	public int getNumCompsUsing()
	{
		return numCompsUsing;
	}

	public void setNumCompsUsing(int numCompsUsing)
	{
		this.numCompsUsing = numCompsUsing;
	}

	public List<ApiAlgorithmScript> getAlgoScripts()
	{
		return algoScripts;
	}

	public void setAlgoScripts(List<ApiAlgorithmScript> algoScripts)
	{
		this.algoScripts = algoScripts;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}
	
	
}
