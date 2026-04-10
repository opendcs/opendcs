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
package ilex.net;

import java.net.*;
import java.util.Date;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;

/**
* Class BasicSvrThread works with BasicServer to provide the network
* functionality necessary in a simple multi-threaded server.
* You should subclass this class and implement the specific functionality
* for talking to your clients.
*/
public abstract class BasicSvrThread extends Thread
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	/** The client connection socket */
	protected Socket socket;

	/** The owning BasicServer object */
	protected BasicServer parent;

	/** True if currently connected */
	protected boolean connected;

	private Date connectTime = null;

	protected boolean isReadOnly = false;

	/**
	* Constructor called from your concrete subclass.
	* @param parent the owning BasicServer
	* @param socket the client connection socket
	*/
	protected BasicSvrThread( BasicServer parent, Socket socket )
	{
		this.parent = parent;
		this.socket = socket;
		connected = true;
		connectTime = new Date();
	}

	public Date getConnectTime() { return connectTime; }

	/** Guarantees disconnect when object destroyed. */
	public void finalize( )
	{
		if (connected)
		{
			log.warn("Disconnect called from finalize. Server thread left socket open.");
			disconnect();
		}
	}

	/**
	* You can overload this if you need to do special clean-up in your
	* subclass. But if you do, you should call super.disconnect() so
	* that the base-class function gets called.
	* The base-class disconnect method closes the socket and removes
	* this thread from the server's list. Finally, it sets a boolean
	* flag causing the thread's run() method to exit.
	*/
	public void disconnect( )
	{
		try { socket.close(); } catch (IOException ioe) {}

		parent.rmSvrThread(this);  // Remove me from parent's list.

		connected = false;         // Cause run() to exit.
		log.debug("BasicSvrThread disconnect complete for {}", getClientName());
	}

	/**
	  Thread run method.
	  You do not need to override this unless you want different behavior.
	  This method will continually call the "serviceClient" method as long
	  as the socket is open.
	  @see BasicSvrThread#serviceClient
	*/
	public void run( )
	{
		try
		{
			while(connected)
			{
				if (isConnected())
					serviceClient();
				else
					disconnect();
			}
		}
		catch (Exception ex)
		{
			disconnect();
			log.atError().setCause(ex).log("Unexpected exception servicing client.");
		}
		log.debug("client-thread exiting {}", getClientName());
	}

	public boolean isConnected()
	{
		if (socket.isClosed())
		{
			log.info("socket.isClosed()");
			return false;
		}
		if (!socket.isConnected())
		{
			log.info("!socket.isConnected()");
			return false;
		}
		if (socket.isInputShutdown())
		{
			log.info("socket.isInputShutdown()");
			return false;
		}
		// Don't shut down in the write-half of a read-only connection times out.
		if (!isReadOnly && socket.isOutputShutdown())
		{
			log.info("socket.isOutputShutdown()");
			return false;
		}
		return true;
	}

	/**
	* You must override the serviceClient() method. In this method you
	* should read and respond to data coming from the client. Your method
	* should block while reading client data. No blocking is done in the
	* thread outside of this method.
	*/
	protected abstract void serviceClient( );

	/**
	* @return hostname of other end of this connection.
	*/
	public String getClientName( )
	{
		if (!connected)
			return "none";
		InetAddress ipaddr = socket.getInetAddress();
		if (ipaddr == null)
			return "unknown-host";
		else
			return ipaddr.getHostName();
	}

	/**
	* @return owning BasicServer object
	*/
	public BasicServer getParent( )
	{
		return parent;
	}

	/**
	* @return this client's socket connection
	*/
	public Socket getSocket( )
	{
		return socket;
	}
}
