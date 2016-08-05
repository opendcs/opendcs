package decodes.polling;

import ilex.util.Logger;

import java.util.Properties;

import decodes.db.TransportMedium;

public class ListeningPortPool extends PortPool
{
	public static final String module = "ListeningPortPool";


	public ListeningPortPool()
	{
		super(module);
		Logger.instance().debug1("Constructing " + module);
	}

	@Override
	public void configPool(Properties dataSourceProps) throws ConfigException
	{
		// TODO Auto-generated method stub

	}

	@Override
	public IOPort allocatePort()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void releasePort(IOPort ioPort, PollingThreadState finalState, boolean wasConnectException)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public int getNumPorts()
	{
		// TODO Auto-generated method stub
		return 0;
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
		// TODO Auto-generated method stub

	}

	@Override
	public void close()
	{
		// TODO Auto-generated method stub

	}

}
