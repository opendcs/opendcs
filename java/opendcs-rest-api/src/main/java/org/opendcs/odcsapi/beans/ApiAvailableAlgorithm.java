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

/**
 * Represents an algorithm discovered in the classpath or filesystem
 * that may or may not have been imported into the database yet.
 */
@Schema(description = "An algorithm found in the classpath or filesystem, with a flag indicating whether it is already imported into the database.")
public final class ApiAvailableAlgorithm
{
	@Schema(description = "Name of the algorithm.", example = "AverageAlgorithm")
	private String name;

	@Schema(description = "Fully qualified Java execution class for the algorithm.",
			example = "decodes.tsdb.algo.AverageAlgorithm")
	private String execClass;

	@Schema(description = "A brief description of the algorithm.",
			example = "Computes a simple average of the input values.")
	private String description;

	@Schema(description = "True if this algorithm is already present in the database.", example = "false")
	private boolean alreadyImported;

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

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public boolean isAlreadyImported()
	{
		return alreadyImported;
	}

	public void setAlreadyImported(boolean alreadyImported)
	{
		this.alreadyImported = alreadyImported;
	}
}
