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
package lqm;

import java.util.LinkedList;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.io.*;
import java.util.*;

import ilex.net.BasicClient;

/**
This class sends queued notifications to the LRIT process via a socket.
 */
public class SenderThread extends Thread
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
		log.info("SenderThread starting...");
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
					}
					catch (Exception ex)
					{
						log.atError().setCause(ex).log("Error on LRIT socket.");
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
					}
					catch (Exception ex)
					{
						log.atError().setCause(ex).log("Error on Backup LRIT socket.");
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
					log.info("Message '{}' sent to LRIT.", s);
				}

					if(bcAlt.isConnected())
					{
						pwAlt.write(s);
						pwAlt.newLine();
						pwAlt.flush();
						log.info("Message '{}' sent to Backup LRIT .", s);
					}


			}
			catch(NoSuchElementException ex)
			{
				try{ Thread.sleep(1000L); }
				catch(InterruptedException iZex){ };

			}
			catch(IOException ex)
			{
				log.atWarn().setCause(ex).log("IO Error on LRIT socket.");
				bc.disconnect();
				bcAlt.disconnect();
			}




		}
	}

	public void shutdown() { _shutdown = true; }

	public synchronized void sendResult(String filename, boolean success)
	{
		String s = "FILE " + filename + (success ? " G" : " B");
		log.info("Queueing message '{}'", s);
		sendQueue.add(s);
	}

	public synchronized void sendStatus(String msg)
	{
		sendQueue.add("STATUS " + msg);
	}

	public void configure()
	{
		log.info("Getting configuration");
		if(bc != null)
			bc.disconnect();

		if(bcAlt != null)
			bc.disconnect();

		LqmConfiguration cfg = LqmConfiguration.instance();
		bc = new BasicClient(cfg.lritHostName, cfg.lritPortNum);
		bcAlt = new BasicClient(cfg.lritHostNameAlt, cfg.lritPortNumAlt);
		log.info("Configuration updated");
		lastConfigGet = System.currentTimeMillis();
	}

	public void connectToLrit()
	{
		try
		{
			log.info("Connecting to {} at {}", bc.getHost(), bc.getPort());
			bc.connect();
			pw = new BufferedWriter(new OutputStreamWriter(bc.getOutputStream()));
			log.info("Connected to {} at {}", bc.getHost(), bc.getPort());
		}
		catch(IOException ex)
		{
			log.atError().setCause(ex).log("Can't connect to server {}", bc.getName());
			try{ Thread.sleep(10000); }
			catch(InterruptedException iZex2){ };
		}
	}


	public void connectToLritAlt()
	{
		try
		{
			log.info("Connecting to {} at {}", bcAlt.getHost(), bcAlt.getPort());
			bcAlt.connect();
			pwAlt = new BufferedWriter(new OutputStreamWriter(bcAlt.getOutputStream()));
			log.info("Connected to {} at {}", bcAlt.getHost(), bcAlt.getPort());
		}
		catch(IOException ex)
		{
			log.atError().setCause(ex).log("Can't connect to server {}", bcAlt.getName());
			try{ Thread.sleep(10000); }
			catch(InterruptedException iZex2){ };
		}
	}
}
