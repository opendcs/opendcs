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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Represents an algorithm configuration with properties, parameters, and associated scripts.")
public final class ApiAlgorithm
{
	/**
	 * Surrogate key for this algorithm in the time series database.
	 */
	@Schema(description = "Unique numeric identifier for the algorithm.", example = "4")
	private Long algorithmId = null;

	/**
	 * Name of this algorithm
	 */
	@Schema(description = "Name of the algorithm.", example = "ChooseOne")
	private String name = null;

	/**
	 * Fully qualified Java class name to execute this algorithm.
	 */
	@Schema(description = "Fully qualified Java class name used to execute the algorithm.",
			example = "decodes.tsdb.algo.ChooseOne")
	private String execClass = null;

	/**
	 * Free form multi-line comment
	 */
	@Schema(description = "Description or comments about the algorithm.", example = "Given two inputs, "
			+ "output the best one: If only one is present at the time-slice, output it. "
			+ "If one is outside the specified upper or lower limit (see properties) output the other. "
			+ "If both are acceptable, output the first one. Useful in situations where you have redundant sensors.")
	private String description = null;

	/**
	 * Properties associated with this algorithm.
	 */
	@Schema(description = "Key-value pairs containing properties associated with the algorithm.")
	private Properties props = new Properties();

	/**
	 * parameters to this algorithm
	 */
	@Schema(description = "List of parameters used by the algorithm.", implementation = ApiAlgoParm.class)
	private List<ApiAlgoParm> parms = new ArrayList<>();

	/**
	 * For use in the editor -- the number of computations using this algo.
	 */
	@Schema(description = "Number of computations currently using this algorithm.", example = "1")
	private int numCompsUsing = 0;

	@Schema(description = "List of scripts associated with the algorithm.", implementation = ApiAlgorithmScript.class)
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
