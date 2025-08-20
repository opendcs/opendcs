/*
*  This software was written by Cove Software, LLC, under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between COVE and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@covesw.com
*  
*  Copyright 2016 U.S. Government.
*/
package decodes.polling;

import ilex.util.Logger;
import ilex.util.PropertiesUtil;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Properties;

import decodes.db.TransportMedium;

public class ListeningPortPool extends PortPool
{
	public static final String module = "ListeningPortPool";
	
	/** Set by the 'availablePorts' property, default=20, this is the maximum number of
	 * simultaneous clients that will be accepted.
	 */
	private int maxSockets = 50;
	
	/** Default listening socket port, set by 'listeningPort' property */
	private int listeningPort = 16050;
	
	int clientNum = 0;
	LinkedList<IOPort> portQueue = new LinkedList<IOPort>();
	
	private Listener listener = null;

	public ListeningPortPool()
	{
		super(module);
		Logger.instance().debug1("Constructing " + module);
		Logger.instance().debug1("portQueue.hashCode=" + portQueue.hashCode());
	}

	@Override
	public void configPool(Properties dataSourceProps) throws ConfigException
	{
		Logger.instance().debug1(module + " configPool");
		String s = PropertiesUtil.getIgnoreCase(dataSourceProps, "availablePorts");
		if (s != null && s.trim().length() > 0)
		{
			try { maxSockets = Integer.parseInt(s); }
			catch(NumberFormatException ex)
			{	
				throw new ConfigException(module + " invalid availablePorts value '" + s 
					+ "'. Expected integer number of simultaneous clients.");
			}
		}
		
		s = PropertiesUtil.getIgnoreCase(dataSourceProps, "listeningPort");
		if (s != null && s.trim().length() > 0)
		{
			try { listeningPort = Integer.parseInt(s); }
			catch(NumberFormatException ex)
			{	
				throw new ConfigException(module + " invalid listeningPort value '" + s 
					+ "'. Expected integer listening socket port number.");
			}
		}

		Logger.instance().debug1(module + " will listen on port " + listeningPort
			+ " and will allow " + maxSockets + " simultaneous polling sessions.");
		
		// start listening
		try
		{
			listener = new Listener(this, listeningPort);
			new Thread(listener).start();
		}
		catch (Exception ex)
		{
			String msg = module + " cannot listen on port " + listeningPort
				+ ": " + ex;
			Logger.instance().failure(msg);
			System.err.println(msg);
			ex.printStackTrace();
		}
	}
	
	public synchronized void enqueueIoPort(IOPort ioPort)
	{
		portQueue.add(ioPort);
Logger.instance().debug1(module + " after add, there are " + portQueue.size()
+ " socket connections in the queue.");
	}

	@Override
	public synchronized IOPort allocatePort()
	{
		return portQueue.isEmpty() ? null : portQueue.remove();
	}

	@Override
	public void releasePort(IOPort ioPort, PollingThreadState finalState, 
		boolean wasConnectException)
	{
		try { ioPort.getIn().close(); }
		catch(IOException ex) {}
		
		try { ioPort.getOut().close(); }
		catch(IOException ex) {}
		
		try { ioPort.getSocket().close(); }
		catch(IOException ex) {}
	}

	@Override
	public int getNumPorts()
	{
		return maxSockets;
	}

	@Override
	public int getNumFreePorts()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void configPort(IOPort ioPort, TransportMedium tm) throws DialException
	{
		Logger.instance().info("Configuring ioPort " + ioPort.getPortNum()
			+ " with tm=" + tm.toString());
		// TODO Auto-generated method stub

	}

	@Override
	public void close()
	{
		Logger.instance().info(module + ".close()");
		if (listener != null)
		{
			listener.shutdown();
			listener._shutdown = true;
			listener = null;
			portQueue.clear();
		}

	}

}
