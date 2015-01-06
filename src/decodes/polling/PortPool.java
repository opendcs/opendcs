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

import java.util.Properties;

import decodes.db.TransportMedium;

public abstract class PortPool
{
	private String name;
	
	public PortPool(String name)
	{
		this.name = name;
	}
	
	public abstract void configPool(Properties dataSourceProps)
		throws ConfigException;
	
	/**
	 * @return free IOPort or null if none is currently available
	 */
	public abstract IOPort allocatePort();
	
	/**
	 * Releases a previously allocated port and returns it to the pool.
	 * @param finalState TODO
	 * @param port the port previously returned from allocatePort.
	 */
	public abstract void releasePort(IOPort ioPort, PollingThreadState finalState);
	
	/**
	 * @return total number of ports in this pool.
	 */
	public abstract int getNumPorts();
	
	/**
	 * @return number of ports in this pool that are currently free.
	 */
	public abstract int getNumFreePorts();
	

	public String getName()
	{
		return name;
	}

	/**
	 * Called from IOPort.connect() to do any port configuration required.
	 * @param ioPort the previously allocated IOPort
	 * @param tm the TransportMedium to connect to
	 * @throws DialException on configuration error
	 */
	public abstract void configPort(IOPort ioPort, TransportMedium tm)
		throws DialException;
	
	/** Free any allocated resources */
	public abstract void close();
}
