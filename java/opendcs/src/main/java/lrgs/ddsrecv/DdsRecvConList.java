/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*/
package lrgs.ddsrecv;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import ilex.util.Logger;
import lrgs.lrgsmain.LrgsMain;
import lrgs.lrgsmain.LrgsConfig;
import lrgs.lrgsmain.LrgsInputException;

public class DdsRecvConList
	extends Thread
{
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
		Logger.instance().debug1(DdsRecv.module 
			+ " DdsRecvConList thread starting.");
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

		Logger.instance().debug3(DdsRecv.module 
			+ " DdsRecvConList checking connections, " + 
			numConnected + " currently connected.");

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
Logger.instance().debug3(DdsRecv.module 
+ " skipping current connection to " + con.getName());
				continue;
			}
			if (!con.isEnabled())
			{
Logger.instance().debug3(DdsRecv.module 
+ " skipping disabled connection to " + con.getName());
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
						Logger.instance().warning(
							DdsRecv.module + ":" + DdsRecv.EVT_CONNECTION_FAILED
							+ " Error making connection to "
							+ con.getName() + ": " + ex);
						con.shutdownLrgsInput();
					}
				}
				else
					Logger.instance().debug3(DdsRecv.module 
						+ " skipping timed-out connection to " + con.getName());
			}
			else // Is connected!
			{
				Logger.instance().debug3(DdsRecv.module 
					+ " sending noop to " + con.getName());
				try { con.noop(); }
				catch(LrgsInputException ex)
				{
					Logger.instance().warning(DdsRecv.module +
						" Error on connection to "
						+ con.getName() + ": " + ex);
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
