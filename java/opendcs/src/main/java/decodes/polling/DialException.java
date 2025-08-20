/*
 * $Id$
 * 
 * This software was written by Cove Software, LLC ("COVE") under contract
 * to Alberta Environment and Sustainable Resource Development (Alberta ESRD).
 * No warranty is provided or implied other than specific contractual terms 
 * between COVE and Alberta ESRD.
 *
 * Copyright 2014 Alberta Environment and Sustainable Resource Development.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package decodes.polling;

/**
 * Thrown on a failed attempt to connect to a remote station by a Dialer.
 */
@SuppressWarnings("serial")
public class DialException extends PollException
{
	/** True if the error was due to inability to communicate over the port.
	 * If so, this is also considered a device error.
	 */
	private boolean portError = false;
	
	public DialException(String msg, boolean portError)
	{
		super(msg);
		this.portError = portError;
	}

	public boolean isPortError()
	{
		return portError;
	}
}
