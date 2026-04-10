/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package decodes.util.hads;

import ilex.util.WarningException;

/**
 * Exception Class used when an error occurs while parsing
 * a USGS Hads flat file.
 *
 */
public class BadHadsEntryException extends WarningException
{
	/** Constructor **/
	public BadHadsEntryException(String msg)
	{
		super(msg);
	}

	public BadHadsEntryException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
}