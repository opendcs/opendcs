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

import decodes.db.TransportMedium;

public abstract class Dialer
{
	/**
	 * Dials the station using the passed IOPort.
	 * Responsible for making an end-to end connection.
	 * @param port the IOPort to use
	 * @param tm the TransportMedium containing dailing information
	 * @throws DialException if failed to connect to remote station
	 */
	public abstract void connect(IOPort ioPort, TransportMedium tm)
		throws DialException;
	
	/**
	 * Close any resources opened by this connection.
	 * Must not do any harm if connection is already closed.
	 * @param ioPort
	 */
	public abstract void disconnect(IOPort ioPort);
}
