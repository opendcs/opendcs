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

package org.opendcs.odcsapi.fixtures;

import org.opendcs.fixtures.configurations.cwms.CwmsOracleConfiguration;
import org.opendcs.fixtures.configurations.opendcs.pg.OpenDCSPGConfiguration;

public enum DbType
{
	CWMS_ORACLE(CwmsOracleConfiguration.NAME, decodes.sql.OracleSequenceKeyGenerator.class.getName(), "CWMS"),
	OPENDCS_POSTGRES(OpenDCSPGConfiguration.NAME, decodes.sql.SequenceKeyGenerator.class.getName(), "OPENTSDB");

	private final String provider;
	private final String keyGenerator;
	private final String oldType;

	DbType(String provider, String keyGenerator, String oldType)
	{
		this.provider = provider;
		this.keyGenerator = keyGenerator;
		this.oldType = oldType;
	}

	@Override
	public String toString()
	{
		return provider;
	}

	public String getKeyGenerator()
	{
		return keyGenerator;
	}

	public String getOldType()
	{
		return oldType;
	}

	public String getProvider()
	{
		return provider;
	}


	public static DbType from(String value)
	{
		for (var tmp: values())
		{
			if (tmp.provider.equals(value))
			{
				return tmp;
			}
		}
		throw new IllegalArgumentException("Enum for " + value + "does not exist");
	}
}
