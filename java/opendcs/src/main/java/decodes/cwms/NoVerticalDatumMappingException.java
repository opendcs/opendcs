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
package decodes.cwms;

import decodes.tsdb.DbIoException;

/**
 * Indicates that CWMS could not find a vertical datum mapping
 * between two requested datums at a given location/time.
 *
 * This is used to distinguish "no mapping" from generic DB errors
 * when calling cwms_loc.get_vertical_datum_offset.
 */
public class NoVerticalDatumMappingException extends DbIoException
{
	public NoVerticalDatumMappingException(String msg)
	{
		super(msg);
	}

	public NoVerticalDatumMappingException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
}

