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
package lrgs.ldds;

import lrgs.common.LrgsErrorCode;

/**
Thrown when a search timed-out, meaning the designated wait time has
elapsed and no messages were retrieved.
*/
public class DdsInternalException extends LddsRequestException
{
	/**
	 * Constructor.
	 * @param msg the message
	 */
	public DdsInternalException(String msg)
	{
		this(msg, null);
	}

	public DdsInternalException(String msg, Throwable cause)
	{
		super(msg, LrgsErrorCode.DDDSINTERNAL, true, cause);
	}
}