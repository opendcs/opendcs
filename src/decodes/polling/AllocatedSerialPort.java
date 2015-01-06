package decodes.polling;

import ilex.net.BasicClient;
import decodes.db.TransportMedium;

public class AllocatedSerialPort
{
	IOPort ioPort;
	DeviceStatus deviceStatus;
	BasicClient basicClient;
	TransportMedium transportMedium = null;
	
	public AllocatedSerialPort(IOPort ioPort, DeviceStatus deviceStatus, BasicClient basicClient)
	{
		super();
		this.ioPort = ioPort;
		this.deviceStatus = deviceStatus;
		this.basicClient = basicClient;
	}
}