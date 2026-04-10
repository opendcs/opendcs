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
  Thrown on request for netlist or searchcrit file that does not exist.
*/
public class NoSuchFileException extends LddsRequestException
{
	/**
	  Constructor takes a String message.
	  @param msg the message
	*/
	public NoSuchFileException(String msg)
	{
		this(msg, null);
	}

	public NoSuchFileException(String msg, Throwable cause)
	{
		super(msg, LrgsErrorCode.DNOSUCHFILE, false, cause);
	}
}