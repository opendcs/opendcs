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

package org.opendcs.odcsapi.hydrojson;

import java.util.Properties;

/**
 * This class is constructed for each request and is used to access the TSDB.
 * @author mmaloney
 *
 * @deprecated implementations will be replaced by those in OpenDCS itself to reduce
 * redundant query maintenance and allow support for multiple database implementations.
 */
@Deprecated
public final class DbInterface
{
	//Will remove with issue: https://github.com/opendcs/rest_api/issues/191
	@Deprecated
	public static final Properties decodesProperties = new Properties();

	private DbInterface()
	{
		//placeholder class that will get removed
	}
}
