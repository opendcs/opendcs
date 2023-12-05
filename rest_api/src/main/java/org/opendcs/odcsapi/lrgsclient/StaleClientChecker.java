package org.opendcs.odcsapi.lrgsclient;

import org.opendcs.odcsapi.hydrojson.DbInterface;

/**
 * This runs a continuous thread to check periodically for stale LDDS client connections
 * and close them.
 */
public class StaleClientChecker
	extends Thread
{

	@Override
	public void run()
	{
		while(true)
		{
			try
			{
				sleep(30000L);
				checkClients();
			}
			catch (InterruptedException e)
			{
			}
		}

	}
	
	private void checkClients()
	{
		DbInterface.getTokenManager().checkStaleConnections();
	}

}
