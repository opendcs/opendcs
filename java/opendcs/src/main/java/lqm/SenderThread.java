/*
 *  $Id$
 *
 *  $Log$
 *  Revision 1.2  2009/08/24 13:34:02  shweta
 *  Code added to send messages to  backup LRIT.
 *
 *  Revision 1.1  2008/04/04 18:21:10  cvs
 *  Added legacy code to repository
 *
 *  Revision 1.3  2004/05/24 17:11:08  mjmaloney
 *  release prep
 *
 *  Revision 1.2  2004/05/19 14:03:45  mjmaloney
 *  dev.
 *
 *  Revision 1.1  2004/05/18 01:01:58  mjmaloney
 *  Created.
 *
 */
package lqm;

import java.util.LinkedList;
import java.io.*;
import java.util.*;

import lritdcs.Constants;
import ilex.net.BasicClient;
import ilex.util.Logger;

/**
This class sends queued notifications to the LRIT process via a socket.
 */
public class SenderThread extends Thread
{
	/// Last time this object retrieved its configuration.
	long lastConfigGet;

	/// Queue of notifications to send to LRIT
	LinkedList sendQueue;

	/// Internal shutdown flag
	private boolean _shutdown;
	BasicClient bc;
	BasicClient bcAlt;
	BufferedWriter pw;
	BufferedWriter pwAlt;
	String filenamePublic;

	SenderThread()
	{
		super();
		sendQueue = new LinkedList();

		lastConfigGet = 0;
		_shutdown = false;
		bc = null;
		bcAlt=null;
		pw = null;
	}

	public void run()
	{
		Logger.instance().log(Logger.E_INFORMATION, "SenderThread starting...");
		_shutdown = false;
		configure();
		while(!_shutdown)
		{
			if (lastConfigGet < LqmConfiguration.instance().getLastLoadTime())
			{
				configure();
			}

			Thread lritThread = new Thread() {
				public void run() {
					try {
						while (!bc.isConnected())
						{
							connectToLrit();
							if (!bc.isConnected()) 
								continue;
						}
					} catch (Exception ex) {
						//ex.printStackTrace();
						Logger.instance().failure(" Error on LRIT socket: " + ex);
					}
				}
			};
			lritThread.start();

			Thread lritBackupThread = new Thread() {
				public void run() {
					try {
						while (!bcAlt.isConnected())
						{
							connectToLritAlt();
							if (!bcAlt.isConnected()) 
								continue;
						}
					} catch (Exception ex) {						
						Logger.instance().failure(" Error on Backup LRIT socket: " + ex);
					}					
				}
			};
			lritBackupThread.start();

			String s= "";
			try 
			{
				synchronized(this) {
					s = (String)sendQueue.removeFirst(); 
				}

				if(bc.isConnected())
				{					
					pw.write(s);
					pw.newLine();
					pw.flush();				
					Logger.instance().info("Message '" + s + "' sent to LRIT.");
				}

					if(bcAlt.isConnected())
					{						
						pwAlt.write(s);
						pwAlt.newLine();
						pwAlt.flush();				
						Logger.instance().info("Message '" + s + "' sent to Backup LRIT .");
					}
				

			}
			catch(NoSuchElementException ex)
			{
				try{ Thread.sleep(1000L); }
				catch(InterruptedException iZex){ };

			}	
			catch(IOException ex)
			{
				Logger.instance().warning("IO Error on LRIT socket: " + ex );
				bc.disconnect();
				bcAlt.disconnect();
			}




		}
	}

	public void shutdown() { _shutdown = true; }

	public synchronized void sendResult(String filename, boolean success)
	{
		String s = "FILE " + filename + (success ? " G" : " B");
		sendQueue.add(s);
		Logger.instance().log(Logger.E_INFORMATION,
				"Queueing message '" + s + "'");
	}

	public synchronized void sendStatus(String msg)
	{
		sendQueue.add("STATUS " + msg);
	}

	public void configure()
	{	
		Logger.instance().log(Logger.E_INFORMATION, "Getting configuration");
		if(bc != null)
			bc.disconnect();

		if(bcAlt != null)
			bc.disconnect();

		LqmConfiguration cfg = LqmConfiguration.instance();
		bc = new BasicClient(cfg.lritHostName, cfg.lritPortNum);
		bcAlt = new BasicClient(cfg.lritHostNameAlt, cfg.lritPortNumAlt);
		Logger.instance().log(Logger.E_INFORMATION, "Configuration updated");
		lastConfigGet = System.currentTimeMillis();
	}

	public void connectToLrit()
	{
		try
		{ 
			Logger.instance().log(Logger.E_INFORMATION, "Connecting to " 
					+ bc.getHost() + " at " + bc.getPort());
			bc.connect(); 
			pw = new BufferedWriter(
					new OutputStreamWriter(bc.getOutputStream()));
			Logger.instance().log(Logger.E_INFORMATION, "Connected to " 
					+ bc.getHost() + " at " + bc.getPort());
		}
		catch(IOException ex)
		{
			//ex.printStackTrace();
			Logger.instance().log(Logger.E_FAILURE,
					"Can't connect to server " + bc.getName() + ": " + ex);
			try{ Thread.sleep(10000); }
			catch(InterruptedException iZex2){ };
		}
	}


	public void connectToLritAlt()
	{
		try
		{ 
			Logger.instance().log(Logger.E_INFORMATION, "Connecting to " 
					+ bcAlt.getHost() + " at " + bcAlt.getPort());
			bcAlt.connect(); 
			pwAlt = new BufferedWriter(
					new OutputStreamWriter(bcAlt.getOutputStream()));
			Logger.instance().log(Logger.E_INFORMATION, "Connected to " 
					+ bcAlt.getHost() + " at " + bcAlt.getPort());
		}
		catch(IOException ex)
		{
			Logger.instance().log(Logger.E_FAILURE,
					"Can't connect to server " + bcAlt.getName() + ": " + ex);
			try{ Thread.sleep(10000); }
			catch(InterruptedException iZex2){ };
		}
	}
}
