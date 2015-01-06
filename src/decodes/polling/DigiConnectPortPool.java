package decodes.polling;

import ilex.net.BasicClient;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.StringTokenizer;

import opendcs.dai.DeviceStatusDAI;
import decodes.db.Database;
import decodes.db.DatabaseIO;
import decodes.db.TransportMedium;
import decodes.sql.SqlDatabaseIO;
import decodes.tsdb.DbIoException;

public class DigiConnectPortPool 
	extends PortPool
{
	public static final String module = "DigiConnectPortPool";
	private String digiIpAddr = null;
	private int digiPortBase = 2101;
	private String digiUserName = null;
	private String digiPassword = null;
	private String processName = null;
	private String hostname = null;
	
	/** 
	 * the names match PORT_NAME column in the SERIAL_PORT_STATUS table.
	 * Format is <digiIpAddr>:portNum.
	 */
	private ArrayList<String> portNames = new ArrayList<String>();
	
	ArrayList<AllocatedSerialPort> allocatedPorts = new ArrayList<AllocatedSerialPort>();
	
	/** The index of the next port to try, so we can use them round-robin. */
	private int nextPortIdx = 0;
	
	private SqlDatabaseIO sqldbio = null;
	
	public DigiConnectPortPool()
	{
		super(module);
		Logger.instance().debug1("Constructing " + module);
	}


	@Override
	public void configPool(Properties dataSourceProps) 
		throws ConfigException
	{
		// Parse property digiIpAddr
		digiIpAddr = PropertiesUtil.getIgnoreCase(dataSourceProps, "digiIpAddr");
		if (digiIpAddr == null || digiIpAddr.trim().length() == 0)
			throw new ConfigException("Missing required property 'digiIpAddr'. Should be "
				+ "set to hostname or IP Address of the Digi ConnectPort device.");
		
		// Parse property digiPortBase
		String s = PropertiesUtil.getIgnoreCase(dataSourceProps, "digiPortBase");
		if (s != null && s.trim().length() > 0)
		{
			try { digiPortBase = Integer.parseInt(s); }
			catch(NumberFormatException ex)
			{
				throw new ConfigException("Invalid digiPortBase property '" + s + "' -- expected integer.");
			}
		}
		
		// Parse the comma-separated list of port numbers and ranges
		// e.g. "1-4,7,9-13". Fill in the portNames array.
		s = PropertiesUtil.getIgnoreCase(dataSourceProps, "availablePorts");
		portNames.clear();
		if (s != null && s.trim().length() > 0)
		{
			try
			{
				StringTokenizer st = new StringTokenizer(s, ", ");
				while(st.hasMoreTokens())
				{
					String t = st.nextToken();
					int hyphen = t.indexOf('-');
					if (hyphen == -1)
					{
						portNames.add(digiIpAddr + ":" + Integer.parseInt(t));
					}
					else
					{
						int start = Integer.parseInt(t.substring(0, hyphen));
						int end = Integer.parseInt(t.substring(hyphen+1));
						if (start > end)
							throw new ConfigException("Invalid range '" + t + "' in availablePorts property '"
								+ s + "'");
						while(start <= end)
							portNames.add(digiIpAddr + ":" + start++);
					}
				}
			}
			catch(ConfigException ex)
			{
				throw ex;
			}
			catch(Exception ex)
			{
				throw new ConfigException("Invalid availablePorts property '" + s 
					+ "'. Must be comma separated list of ports, or port ranges. Example: "
					+ "1-5,7,9-12");
			}
		}
		if (portNames.size() == 0)
			throw new ConfigException("No availablePorts specified");
		Logger.instance().info(module + " initialized with " + portNames.size() + " ports.");
		
		// Parse properties for digiUserName and digiPassword
		digiUserName = PropertiesUtil.getIgnoreCase(dataSourceProps, "digiUserName");
		if (digiUserName == null || digiUserName.trim().length() == 0)
			throw new ConfigException("Missing required property 'digiUserName'. Should be "
				+ "the username for telnetting into the control port 23 of the Digi ConnectPort device.");
		digiPassword = PropertiesUtil.getIgnoreCase(dataSourceProps, "digiPassword");
		if (digiPassword == null || digiPassword.trim().length() == 0)
			throw new ConfigException("Missing required property 'digiPassword'. Should be "
				+ "the password for telnetting into the control port 23 of the Digi ConnectPort device.");

		processName = PropertiesUtil.getIgnoreCase(dataSourceProps, "RoutingSpecName");
		if (processName == null)
		{
			Logger.instance().warning(module + " no 'RoutingSpecName' property available, will use process name '"
				+ module + "'");
			processName = module;
		}
		try { hostname = InetAddress.getLocalHost().getHostName(); }
		catch(Exception ex)
		{
			Logger.instance().warning(module + " Cannot resolve hostname. Will use 'localhost'");
			hostname = "localhost";
		}
		
		// Get access to the database for DeviceStatus records.
		DatabaseIO dbio = Database.getDb().getDbIo();
		if (!(dbio instanceof SqlDatabaseIO))
			throw new ConfigException("The DECODES Database is not a SQL Database. Cannot use Digi!");
		sqldbio = (SqlDatabaseIO)dbio;
	}

	@Override
	public synchronized IOPort allocatePort()
	{
		// Use SERIAL_PORT_STATUS table to find a free port
		DeviceStatusDAI deviceStatusDAO = sqldbio.makeDeviceStatusDAO();
		
		IOPort ret = null;
		try
		{
			if (nextPortIdx >= portNames.size())
				nextPortIdx = 0;
			int startPortIdx = nextPortIdx;
			while(ret == null)
			{
				String portName = portNames.get(nextPortIdx);
				DeviceStatus devStat = deviceStatusDAO.getDeviceStatus(portName);
				if (!devStat.isInUse())
				{
					// Set its IN_USE, LAST_USED_BY_PROC, and LAST_USED_BY_HOST
					devStat.setInUse(true);
					devStat.setLastUsedByProc(processName);
					devStat.setLastUsedByHost(hostname);
					devStat.setLastActivityTime(new Date());
					devStat.setPortStatus("Allocated");
					deviceStatusDAO.writeDeviceStatus(devStat);

					// Wait 2 seconds and then check to see that last used proc/host are still me.
					try { Thread.sleep(2000L); } catch(InterruptedException ex) {}
					devStat = deviceStatusDAO.getDeviceStatus(portName);
					
					if (!devStat.getLastUsedByHost().equals(hostname)
					 || !devStat.getLastUsedByProc().equals(processName))
					{
						Logger.instance().info(module + " Port '" + portName + " stolen by process '"
							+ devStat.getLastUsedByProc() + "' host '" + devStat.getLastUsedByHost()
							+ "' -- will keep trying.");
					}
					else // If so, I have the port! Open socket, create IOPort and set ret.
					{
						int colon = portName.indexOf(':');
						int portnum = Integer.parseInt(portName.substring(colon+1));
						
						try
						{
							BasicClient bc = new BasicClient(digiIpAddr, digiPortBase + portnum);
							bc.connect();
							ret = new IOPort(this, portnum, new ModemDialer());
							ret.setIn(bc.getInputStream());
							ret.setOut(bc.getOutputStream());
							allocatedPorts.add(new AllocatedSerialPort(ret, devStat, bc));
						}
						catch(Exception ex)
						{
							Logger.instance().warning(module + " cannot open " + portName + ": " + ex);
						}
					}
				}
				
				// Else either this is already in use, or it was stolen as I tried to allocate it.
				if (++nextPortIdx >= portNames.size())
					nextPortIdx = 0;
				if (nextPortIdx == startPortIdx)
				{
					// Already tried all the ports
					Logger.instance().debug1(module + " No ports currently available.");
					break; // stop trying, will return null.
				}
			}
		}
		catch (DbIoException ex)
		{
			Logger.instance().failure(module + " Error reading/writing device statuses: " + ex);
		}
		finally
		{
			deviceStatusDAO.close();
		}
		return ret;

	}

	@Override
	public synchronized void releasePort(IOPort ioPort, PollingThreadState finalState)
	{
		// Close the socket and remove it from my allocatedPorts
		AllocatedSerialPort allocatedPort = null;
		for (AllocatedSerialPort ap : allocatedPorts)
			if (ap.ioPort == ioPort)
			{
				allocatedPort = ap;
				break;
			}
		allocatedPorts.remove(ioPort);
		if (allocatedPort == null)
			return;
		
		allocatedPort.basicClient.disconnect();
		ioPort.setIn(null);
		ioPort.setOut(null);
		
		// Free the port in SERIAL_PORT_STATUS
		DeviceStatusDAI deviceStatusDAO = sqldbio.makeDeviceStatusDAO();
		
		// Finalize and set the deviceStatus to no In Use
		try
		{
			Date now = new Date();
			allocatedPort.deviceStatus.setLastActivityTime(now);
			if (finalState == PollingThreadState.Success)
			{
				allocatedPort.deviceStatus.setLastReceiveTime(now);
				allocatedPort.deviceStatus.setLastMediumId(allocatedPort.transportMedium.getMediumId());
			}
			else
				allocatedPort.deviceStatus.setLastErrorTime(now);
			allocatedPort.deviceStatus.setInUse(false);
			allocatedPort.deviceStatus.setPortStatus("");
			deviceStatusDAO.writeDeviceStatus(allocatedPort.deviceStatus);
		}
		catch (DbIoException ex)
		{
			Logger.instance().failure(module + " Cannot write deviceStatus: " + ex);
		}
		finally
		{
			deviceStatusDAO.close();
		}
	}

	@Override
	public int getNumPorts()
	{
		return portNames.size();
	}

	@Override
	public int getNumFreePorts()
	{
		DeviceStatusDAI deviceStatusDAO = sqldbio.makeDeviceStatusDAO();
		try
		{
			int n = 0;
			for(DeviceStatus devstat : deviceStatusDAO.listDeviceStatuses())
			{
				if (portNames.contains(devstat.getPortName()))
					n++;
			}
			return n;
		}
		catch (DbIoException ex)
		{
			Logger.instance().failure(module + " Cannot list deviceStatus: " + ex);
		}
		finally
		{
			deviceStatusDAO.close();
		}
		return 0;
	}


	@Override
	public void configPort(IOPort ioPort, TransportMedium tm) throws DialException
	{
		// Close the socket and remove it from my allocatedPorts
		AllocatedSerialPort allocatedPort = null;
		for (AllocatedSerialPort ap : allocatedPorts)
			if (ap.ioPort == ioPort)
			{
				allocatedPort = ap;
				break;
			}
		if (allocatedPort == null)
		{
			Logger.instance().warning(module + " asked to config port for " + tm.toString()
				+ ", but port was not allocated by this object!");
			return;
		}
		allocatedPort.transportMedium = tm;
		
		// TODO Use info in tm to set serial port parameters
		
		// TODO Connect to device at AESRD with telnet and type either
		// "set serial ?" or "help set serial"
	}


	@Override
	public void close()
	{
		// It's up to each PollingThread to close it's own port.
		// So if anything is left, it means the controller is shutting down prematurely.
		// So close anything still open.
		for(AllocatedSerialPort ap : allocatedPorts)
			releasePort(ap.ioPort, PollingThreadState.Failed);
	}

}
