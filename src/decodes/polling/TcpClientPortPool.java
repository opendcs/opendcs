package decodes.polling;

import ilex.util.Logger;
import ilex.util.PropertiesUtil;

import java.util.ArrayList;
import java.util.Properties;

import decodes.db.TransportMedium;

public class TcpClientPortPool extends PortPool
{
	private int maxSockets = 32;
	public static final String module = "TcpClientPortPool";
	private ArrayList<IOPort> ioPorts = new ArrayList<IOPort>();
	private int portNum = 0;
	
	public TcpClientPortPool()
	{
		super(module);
		Logger.instance().debug1("Constructing " + module);
	}

	@Override
	public void configPool(Properties dataSourceProps)
		throws ConfigException
	{
		String s = PropertiesUtil.getIgnoreCase(dataSourceProps, "availablePorts");
		if (s != null && s.trim().length() > 0)
		{
			try { maxSockets = Integer.parseInt(s); }
			catch(NumberFormatException ex)
			{	
				throw new ConfigException(module + " invalid availablePorts value '" + s 
					+ "'. Expected integer number of simultaneous sockets.");
			}
		}
		Logger.instance().debug1(module + " will allow " + maxSockets + " simultaneous polling sessions.");
	}

	@Override
	public IOPort allocatePort()
	{
		if (ioPorts.size() >= maxSockets)
			return null;
		IOPort iop = new IOPort(this, portNum++, new TcpDialer());
		ioPorts.add(iop);
		Logger.instance().debug3(module + " allocating IOPort");
		return iop;
	}

	@Override
	public void releasePort(IOPort port, PollingThreadState finalState)
	{
		ioPorts.remove(port);
	}

	@Override
	public int getNumPorts()
	{
		return maxSockets;
	}

	@Override
	public int getNumFreePorts()
	{
		return maxSockets - ioPorts.size();
	}

	@Override
	public void configPort(IOPort ioPort, TransportMedium tm) throws DialException
	{
		// Nothing to do here.
		
	}

	@Override
	public void close()
	{
		// Nothing to do
	}

}
