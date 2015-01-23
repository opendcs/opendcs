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

import ilex.util.Logger;

import java.io.InputStream;
import java.io.OutputStream;

import decodes.db.TransportMedium;

public class IOPort
{
	/** The PortPool object that owns this IOPort */
	private PortPool portPool;
	
	/** Uniquely identifies this port within the pool */
	private int portNum;
	
	/** The input stream associated with this IOPort */
	private InputStream in = null;
	
	/** The output stream associated with this IOPort */
	private OutputStream out = null;
	
	/** The dialer (if one is needed) associated with this IOPort */
	private Dialer dialer = null;
	
	private PollingThreadState configureState = PollingThreadState.Waiting;
	private boolean dialerConnected = false;
	private PollingThread pollingThread = null;
	
	public IOPort(PortPool portPool, int portNum, Dialer dialer)
	{
		this.portPool = portPool;
		this.portNum = portNum;
		this.dialer = dialer;
	}
	
	/**
	 * Delegate to the dialer to establish an end-to-end connection with the DCP
	 * using this IOPort and the passed TransportMedium
	 * @param tm
	 * @throws DialException
	 */
	public void connect(TransportMedium tm, PollingThread pollingThread)
		throws DialException
	{
		this.pollingThread = pollingThread;
		pollingThread.debug2("IOPort.connect() -- calling portPool.configPort");
		portPool.configPort(this, tm);
		pollingThread.debug2("IOPort.connect() -- calling dialer.connect");
		dialer.connect(this, tm, this.pollingThread);
		dialerConnected = true;
	}
	
	/**
	 * Delegate to the dialer to discard and close any resources associated with
	 * this connection (open files, sockets, streams, etc.)
	 */
	public void disconnect()
	{
		if (dialerConnected) // don't use dialer to disconnect if we never connected.
			dialer.disconnect(this);
		pollingThread.debug2("IOPort.disconnect() complete.");		
	}
	
	public InputStream getIn()
	{
		return in;
	}

	public void setIn(InputStream in)
	{
		this.in = in;
	}

	public OutputStream getOut()
	{
		return out;
	}

	public void setOut(OutputStream out)
	{
		this.out = out;
	}

	public int getPortNum()
	{
		return portNum;
	}

	public PollingThreadState getConfigureState()
	{
		return configureState;
	}

	public void setConfigureState(PollingThreadState configureState)
	{
		this.configureState = configureState;
	}

}
