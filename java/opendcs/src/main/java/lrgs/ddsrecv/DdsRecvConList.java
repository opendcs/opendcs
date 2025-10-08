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
package lrgs.ddsrecv;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import lrgs.lrgsmain.LrgsMain;
import lrgs.lrgsmain.LrgsConfig;
import lrgs.lrgsmain.LrgsInputException;

public class DdsRecvConList extends Thread
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	/** Vector of my connections */
	private Vector conlist;

	/** The main object provides links to other modules. */
	private LrgsMain lrgsMain;

	/** Shutdown flag */
	private boolean isShutdown;

	/** The connection currently being used to retrieve data. */
	DdsRecvConnection currentConnection;

	/** Recheck all connections at this time interval. */
	private static final long conCheckTime = 30000L; // 30 sec.

	/** When a connection is in error, it stays timed-out for this long. */
	private static final long conTimeOut = 300000L; // 5 min.

	/** Constructor */
	public DdsRecvConList(LrgsMain lrgsMain)
	{
		conlist = new Vector();
		this.lrgsMain = lrgsMain;
		isShutdown = false;
		currentConnection = null;
	}

	/** Causes this thread to shutdown, disconnecting all connections. */
	public void shutdown()
	{
		isShutdown = true;
	}

	/** Thread run method */
	public void run()
	{
		log.info("{} DdsRecvConList thread starting.", DdsRecv.module);
		while(!isShutdown)
		{
			if (LrgsConfig.instance().enableDdsRecv)
				checkConnections();
			try { sleep(conCheckTime); }
			catch(InterruptedException ex) {}
		}
	}

	/**
	 * Removes all connections from the list. This is done when the code
	 * detects a configuration change.
	 * Note: The call to remove all and add is synchronized on this object
	 * externally, so the methods are NOT declared to be syncrhonized.
	 */
	public void removeAll()
	{
		while(conlist.size() > 0)
		{
			DdsRecvConnection con = (DdsRecvConnection)conlist.get(0);
			con.shutdownLrgsInput();
			lrgsMain.freeInput(con.getSlot());
			conlist.remove(0);
		}
		currentConnection = null;
	}

	/**
	 * Adds a connection.
	 * @param cfg the connection configuration.
	 */
	public void addConnection(DdsRecvConnectCfg cfg)
	{
		DdsRecvConnection con = new DdsRecvConnection(cfg, lrgsMain);
		conlist.add(con);
		lrgsMain.addInput(con);
	}

	/**
	 * Attempts to keep all configured connections alive.
	 * Any that are not connected are connected.
	 * Any that are idle are sent an echo periodically.
	 */
	public synchronized void checkConnections()
	{
		// First count the number of connections & number connected.
		int numConnected = 0;
		for(Iterator it = conlist.iterator(); it.hasNext(); )
		{
			DdsRecvConnection con = (DdsRecvConnection)it.next();
			if (con.isConnected())
				numConnected++;
		}

		log.trace("{} DdsRecvConList checking connections, {} currently connected.",
				  DdsRecv.module, numConnected);

		DdsRecvConnection lowestNumberConnected = null;

		for(Iterator it = conlist.iterator(); it.hasNext(); )
		{
			DdsRecvConnection con = (DdsRecvConnection)it.next();
			if (con.isConnected()
			 && (lowestNumberConnected == null
				 || con.getSlot() < lowestNumberConnected.getSlot()))
				lowestNumberConnected = con;

			if (con == currentConnection)
			{
				log.trace("{} skipping current connection to {}", DdsRecv.module, con.getName());
				continue;
			}
			if (!con.isEnabled())
			{
				log.trace("{} skipping disabled connection to {}", DdsRecv.module, con.getName());
				continue;
			}
			if (!con.isConnected())
			{
				// Skip timed-out connections, but only if I have at least one
				// good one.
				if (System.currentTimeMillis() - con.getLastActivityTime()
					>= conTimeOut
				 || numConnected == 0)
				{
					try
					{
						con.initLrgsInput();
			 			if (lowestNumberConnected == null
				 		 || con.getSlot() < lowestNumberConnected.getSlot())
							lowestNumberConnected = con;
					}
					catch(LrgsInputException ex)
					{
						log.atWarn()
						   .setCause(ex)
						   .log("{}:{} Error making connection to {}",
						   		DdsRecv.module, DdsRecv.EVT_CONNECTION_FAILED, con.getName());
						con.shutdownLrgsInput();
					}
				}
				else
				{
					log.trace("{} skipping timed-out connection to {}", DdsRecv.module, con.getName());
				}
			}
			else // Is connected!
			{
				log.trace("{} sending noop to {}", DdsRecv.module, con.getName());
				try { con.noop(); }
				catch(LrgsInputException ex)
				{
					log.atWarn()
					   .setCause(ex)
					   .log("{} Error on connection to {}", DdsRecv.module, con.getName());
					con.shutdownLrgsInput();
				}
			}
		}

		// If I'm not currently using the lowest numbered connection,
		// set current connection to null, causing DdsRecv to call
		// getCurrentConnection. This will switch.
		if (currentConnection != lowestNumberConnected)
		{
			if (currentConnection != null)
				currentConnection.status = "Ready";
			currentConnection = null;
		}
	}

	/**
	 * Finds a new connection to be the current connection for retrieving
	 * data. Called from the DdsRecv thread when it needs to start a session.
	 * @return DdsRecvConnection ready for retrieval
	 */
	public synchronized DdsRecvConnection getCurrentConnection()
	{
		currentConnection = null;
		DdsRecvConnection lowestNumberConnected = null;
		for(Iterator it = conlist.iterator(); it.hasNext(); )
		{
			DdsRecvConnection con = (DdsRecvConnection)it.next();
			if (!con.isConnected())
				continue;
			if (lowestNumberConnected == null
			 || con.getSlot() < lowestNumberConnected.getSlot())
				lowestNumberConnected = con;
		}

		if (lowestNumberConnected != null)
		{
			lowestNumberConnected.status = "Active";
			lowestNumberConnected.flush();
			return currentConnection = lowestNumberConnected;
		}
		return null;
	}

	/**
	 * Same behavior as getCurrentConnection() except that it also rejects
	 * connections that have already been tried.
	 * @param tried a list of already-tried connections.
	 * @return DdsRecvConnection ready for retrieval
	 */
	public synchronized DdsRecvConnection getUntriedConnection(
		ArrayList<DdsRecvConnection> tried)
	{
		if (tried == null)
			return getCurrentConnection();

		currentConnection = null;
		DdsRecvConnection lowestNumberConnected = null;
	  nextcon:
		for(Iterator it = conlist.iterator(); it.hasNext(); )
		{
			DdsRecvConnection con = (DdsRecvConnection)it.next();
			if (!con.isConnected())
				continue;
			for(DdsRecvConnection tcon : tried)
				if (tcon == con)
					continue nextcon;
			if (lowestNumberConnected == null
			 || con.getSlot() < lowestNumberConnected.getSlot())
				lowestNumberConnected = con;
		}

		if (lowestNumberConnected != null)
		{
			lowestNumberConnected.status = "Active";
			lowestNumberConnected.flush();
			return currentConnection = lowestNumberConnected;
		}
		return null;
	}

	public void clearCurrentConnection()
	{
		currentConnection = null;
	}
}