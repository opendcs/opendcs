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

import lrgs.common.ArchiveException;

/**
  This is thrown internally to indicate that a request failed.
  The hangup flag will be set indicating whether or not the server
  should hangup on the client or not as a result of this error.
*/
public class LddsRequestException extends ArchiveException
{
	/**
	  Constructor.
	  @param msg the explanation.
	  @param errorCode application-specific error code
	  @param hangup true if this exception warrants a hangup on the client
	*/
	public LddsRequestException(String msg, int errorCode, boolean hangup)
	{
		super(msg, errorCode, hangup);
	}

	public LddsRequestException(String msg, int errorCode, boolean hangup, Throwable cause)
	{
		super(msg, errorCode, hangup, cause);
	}

	/**
	 * Sets the hangup flag.
	 * @param tf the new flag value
	 */
	public void setHangup(boolean tf) { hangup = tf; }
}