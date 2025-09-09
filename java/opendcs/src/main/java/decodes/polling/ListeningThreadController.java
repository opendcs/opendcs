/*
* This software was written by Cove Software, LLC
*
*  Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
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
package decodes.polling;

import ilex.util.AsciiUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.db.TransportMedium;

/**
 * The dynamics of a listening interface are different then for call-out:
 * 1. The array of transport media indicate platforms that are allowed to call in
 *    on this interface.
 * 2. Threads are created and destroyed dynamically as clients call in and the
 *    sessions are terminated.
 * 3. The link stays up indefinitely, or until the data source is shut down.
 *    
 * @author mmaloney
 *
 */
public class ListeningThreadController extends PollingThreadController
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	public ListeningThreadController(PollingDataSource dataSource, 
		ArrayList<TransportMedium> aggTMList, ListeningPortPool lpp)
	{
		super(dataSource, aggTMList, lpp);
		module = "ListeningThreadController";
	}
	
	@Override
	public void run()
	{
		_shutdown = false;
		
		if (!(portPool instanceof ListeningPortPool))
		{
			log.error("Mis-configuration: Listening Thread Controller requires a ListeningPortPool.");
			return;
		}
		
		log.debug("starting. {} DCPs are allowed to use the listening socket. authenticateClient={}",
				  portPool.getNumPorts(), dataSource.getAuthenticateClient());
		
		String authType = null;
		if (dataSource.getAuthenticateClient() != null
		 && dataSource.getAuthenticateClient().trim().length() > 0
		 && !dataSource.getAuthenticateClient().equalsIgnoreCase("none"))
			authType = dataSource.getAuthenticateClient();
		byte authPrompt[] = null;
		String reqResponse = null;
		
		if (authType != null)
		{
			// Format is 'method=<separated by equal sign'
			// Example: password=\r\npw? =ToPsEcReT
			String []t = dataSource.getAuthenticateClient().split("=");
			if (t[0].equalsIgnoreCase("password"))
			{
				// Arguments are prompt and required response
				if (t.length != 3)
				{
					log.error("Badly formed login params in data source config.");
					dataSource.close();
				}
				if (t[1].length() > 0)
					authPrompt = AsciiUtil.ascii2bin(t[1]);
				reqResponse = t[2];
			}
			// Future: parse args for other auth methods here.
		}
		
		long debugmsec = 0L;
		while(!_shutdown)
		{
			// ListingPortPool will return non-null when a client connects. Null otherwise.
			IOPort ioPort = portPool.allocatePort();
			if (ioPort != null)
			{
				log.debug("have new ioPort: {} maxPorts={} #threads={}",
						  ioPort.getPortName(), portPool.getNumFreePorts(), threads.size());

				if (threads.size() >= portPool.getNumPorts())
				{
					log.warn("Hanging up on new client {} because max clients of {} already reached.",
							 ioPort.getPortName(), portPool.getNumPorts());
					portPool.releasePort(ioPort, PollingThreadState.Failed, true);
					continue;
				}
				
				try
				{
					//TODO Improvement needed: Consider having the checkPassword and identify
					//client done in the PollingThread. If I do it here, I am not listening
					// for new clients which may come in rapid fire.
					checkPassword(ioPort, authPrompt, reqResponse);
					TransportMedium tm = identifyClient(ioPort);
					log.debug("have client with tm={}", tm.toString());
					
					// For listen, the threads are constructed dynamically
					// Or consider -- could they be reused from a single list of polling threads?
					PollingThread pt = new PollingThread(this, dataSource, tm);
					threads.add(pt);
					
					pt.setState(PollingThreadState.Running);
					pt.setSaveSessionFile(saveSessionFile);
					pt.setIoPort(ioPort);
					pt.setThreadStart(new Date());
					log.debug("starting {}, TM {} on port number {}, pollPriority={}",
							  pt.getModule(), pt.getTransportMedium(), ioPort.getPortNum(), pt.getPollPriority());
					(new Thread(pt)).start();

				}
				catch (LoginException ex)
				{
					log.atWarn().setCause(ex).log("Incorrect password response from client {}", ioPort.getPortName());
					portPool.releasePort(ioPort, PollingThreadState.Failed, true);
				}
				catch (IOException ex)
				{
					log.atWarn().setCause(ex).log("IO Error on client {}", ioPort.getPortName());
					portPool.releasePort(ioPort, PollingThreadState.Failed, true);
				}
			}
			if (System.currentTimeMillis() - debugmsec > 10000L)
			{
				checkDeadThreads();
				for(Iterator<PollingThread> ptit = threads.iterator(); ptit.hasNext(); )
				{
					PollingThread pt = ptit.next();
					if (pt.getState() == PollingThreadState.Failed
					 || pt.getState() == PollingThreadState.Success)
						ptit.remove();
				}
				log.info("Threads: total={}, waiting={}, running={}, success={}, failed={}",
						 threads.size(), countThreads(PollingThreadState.Waiting),
						 countThreads(PollingThreadState.Running), countThreads(PollingThreadState.Success),
						 countThreads(PollingThreadState.Failed)
					);
				debugmsec = System.currentTimeMillis();
			}

			try { sleep(PAUSE_MSEC); } catch(InterruptedException ex) {}
		}

		// Kill any PollingThreads that are still alive.
		log.info(" shutting down. {} still active.", threads.size());
		
		portPool.close();

		
		int nk = 0;
		for(PollingThread pt : threads)
			if (pt.getState() == PollingThreadState.Running)
			{
				pt.shutdown();
				nk++;
			}
			
		// Wait up to 30 sec until all the kids have called pollComplete().
		if (nk > 0)
		{
			log.info("Will wait up to 30 sec for {} polling threads to terminate.", nk);
		}
		else
		{
			log.info("All threads terminated, proceeding with shutdown.");
		}
	
		long x = System.currentTimeMillis();
		while(countThreads(PollingThreadState.Running) > 0
			&& System.currentTimeMillis() - x < 30000L)
		{	
			try { sleep(PAUSE_MSEC); }
			catch(InterruptedException ex) {}
		}

		log.info("finished. {} stations called in, {} failed.", successfullPolls, failedPolls);
		dataSource.pollingComplete();
	}

	private TransportMedium identifyClient(IOPort ioPort)
		throws IOException, LoginException
	{
		log.debug("identifyClient, waiting for client to send TM ID...");

		// In this initial prototype, A client identifies itself with a single
		// line containing the transport medium ID. Other methods can be implemented
		// in the future, probably driven by data source properties.
		BufferedReader br = new BufferedReader(new InputStreamReader(ioPort.getIn()));
		String id = br.readLine();
		if (id == null || id.length() == 0)
		{
			String msg = module + " client at " + ioPort.getPortName()
				+ " did not send ID.";
			throw new LoginException(msg);
		}
		log.debug("... client sent '{}'", id);
		
		for(TransportMedium tm : aggTMList)
			if (tm.getMediumId().equalsIgnoreCase(id))
				return tm;

		String msg = module + " client at " + ioPort.getPortName()
			+ " sent invalid ID '" + id + "' -- Does not match any transport medium "
			+ "in this routing spec.";
		throw new LoginException(msg);
	}

	/**
	 * Optional: If clients are required to login to this server, the 'authenticateClient'
	 * property says how this is done. Default is no authentication is required.
	 */
	private void checkPassword(IOPort ioPort, byte prompt[], String reqResponse)
		throws LoginException, IOException
	{
		log.debug("checkPassword prompt='{}'", prompt);

		if (prompt != null && prompt.length > 0)
		{
			ioPort.getOut().write(prompt);
			ioPort.getOut().flush();
			if (reqResponse != null && reqResponse.length() > 0)
			{
				log.debug("wrote prompt, awaiting response.");
				BufferedReader br = new BufferedReader(new InputStreamReader(ioPort.getIn()));
				String clientResponse = br.readLine();
				log.debug("response received '{}'", clientResponse);
				if (clientResponse == null || clientResponse.length() == 0
				 || !clientResponse.equals(reqResponse))
				{
					log.warn("invalid client login response '{}'", clientResponse);
					throw new LoginException("Client provided invalid password to login to server.");
				}
			}
			// Else either no response required or client provided correct response.
		}
	}
}
