package decodes.polling;

import ilex.util.AsciiUtil;
import ilex.util.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

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
			Logger.instance().failure("Mis-configuration: Listening Thread Controller"
				+ " requires a ListeningPortPool.");
			return;
		}
		
		dataSource.log(Logger.E_DEBUG1, module + " starting. " + portPool.getNumPorts() 
			+ " DCPs are allowed to use the listening socket. authenticateClient="
			+ dataSource.getAuthenticateClient());
		
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
					dataSource.log(Logger.E_FAILURE, "Badly formed login params in data source config.");
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
Logger.instance().debug1(module + " have new ioPort: " + ioPort.getPortName()
+ " maxPorts=" + portPool.getNumFreePorts() + " #threads=" + threads.size());

				if (threads.size() >= portPool.getNumPorts())
				{
					Logger.instance().warning(module + " Hanging up on new client "
						+ ioPort.getPortName() + " because max clients of "
							+ portPool.getNumPorts() + " already reached.");
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
Logger.instance().debug1(module + " have client with tm=" + tm.toString());
					
					// For listen, the threads are constructed dynamically
					// Or consider -- could they be reused from a single list of polling threads?
					PollingThread pt = new PollingThread(this, dataSource, tm);
					threads.add(pt);
					
					pt.setState(PollingThreadState.Running);
					pt.setSaveSessionFile(saveSessionFile);
					pt.setIoPort(ioPort);
					pt.setThreadStart(new Date());
					dataSource.log(Logger.E_DEBUG1, module + " starting " 
						+ pt.getModule()
						+ ", TM " + pt.getTransportMedium()
						+ " on port number " + ioPort.getPortNum()
						+ ", pollPriority=" + pt.getPollPriority());
					(new Thread(pt)).start();

				}
				catch (LoginException ex)
				{
					dataSource.log(Logger.E_WARNING, "Incorrect password response from client " 
						+ ioPort.getPortName() + ": " + ex);
					portPool.releasePort(ioPort, PollingThreadState.Failed, true);
				}
				catch (IOException ex)
				{
					dataSource.log(Logger.E_WARNING, "IO Error on client " 
						+ ioPort.getPortName() + ": " + ex);
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
				dataSource.log(Logger.E_INFORMATION, 
					module + " Threads: total=" + threads.size()
					+ ", waiting=" + countThreads(PollingThreadState.Waiting) 
					+ ", running=" + countThreads(PollingThreadState.Running)
					+ ", success=" + countThreads(PollingThreadState.Success)
					+ ", failed=" + countThreads(PollingThreadState.Failed)
					);
				debugmsec = System.currentTimeMillis();
			}

			try { sleep(PAUSE_MSEC); } catch(InterruptedException ex) {}
		}

		// Kill any PollingThreads that are still alive.
		dataSource.log(Logger.E_INFORMATION, module + " shutting down. "
			+ threads.size() + " still active.");
		
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
			dataSource.log(Logger.E_INFORMATION, module 
				+ " Will wait up to 30 sec for " + nk + " polling threads to terminate.");
		else
			dataSource.log(Logger.E_INFORMATION, module 
				+ " All threads terminated, proceeding with shutdown.");
	
		long x = System.currentTimeMillis();
		while(countThreads(PollingThreadState.Running) > 0
			&& System.currentTimeMillis() - x < 30000L)
		{	
			try { sleep(PAUSE_MSEC); }
			catch(InterruptedException ex) {}
		}

		dataSource.log(Logger.E_INFORMATION, module + " finished. "
			+ successfullPolls + " stations called in, " + failedPolls + " failed.");
		dataSource.pollingComplete();
	}

	private TransportMedium identifyClient(IOPort ioPort)
		throws IOException, LoginException
	{
Logger.instance().debug1("identifyClient, waiting for client to send TM ID...");

		// In this initial prototype, A client identifies itself with a single
		// line containing the transport medium ID. Other methods can be implemented
		// in the future, probably driven by data source properties.
		BufferedReader br = new BufferedReader(new InputStreamReader(ioPort.getIn()));
		String id = br.readLine();
		if (id == null || id.length() == 0)
		{
			String msg = module + " client at " + ioPort.getPortName()
				+ " did not send ID.";
			dataSource.log(Logger.E_FAILURE, msg);
			throw new LoginException(msg);
		}
Logger.instance().debug1("... client sent '" + id + "'");
		
		for(TransportMedium tm : aggTMList)
			if (tm.getMediumId().equalsIgnoreCase(id))
				return tm;

		String msg = module + " client at " + ioPort.getPortName()
			+ " sent invalid ID '" + id + "' -- Does not match any transport medium "
			+ "in this routing spec.";
		dataSource.log(Logger.E_FAILURE, msg);
		throw new LoginException(msg);
	}

	/**
	 * Optional: If clients are required to login to this server, the 'authenticateClient'
	 * property says how this is done. Default is no authentication is required.
	 */
	private void checkPassword(IOPort ioPort, byte prompt[], String reqResponse)
		throws LoginException, IOException
	{
Logger.instance().debug1("checkPassword prompt='" + prompt + "'");

		if (prompt != null && prompt.length > 0)
		{
			ioPort.getOut().write(prompt);
			ioPort.getOut().flush();
			if (reqResponse != null && reqResponse.length() > 0)
			{
Logger.instance().debug1("wrote prompt, awaiting response.");
				BufferedReader br = new BufferedReader(new InputStreamReader(ioPort.getIn()));
				String clientResponse = br.readLine();
Logger.instance().debug1("response received '" + clientResponse + "'");
				if (clientResponse == null || clientResponse.length() == 0
				 || !clientResponse.equals(reqResponse))
				{
					Logger.instance().warning(module + " invalid client login response '" + clientResponse + "'");
					throw new LoginException("Client provided invalid password to login to server.");
				}
			}
			// Else either no response required or client provided correct response.
		}
	}
}
