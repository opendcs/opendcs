package decodes.polling;

import java.io.IOException;
import java.rmi.UnknownHostException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import decodes.datasource.RawMessage;
import ilex.net.BasicClient;
import ilex.util.Logger;

public class DigiConnectPortManager 
	extends Thread
{
	public static String module = "DigiConnectPortManager";
	private boolean _shutdown = false;
	private long PAUSE_MSEC = 100L;
	private long ENQUEUE_WAIT_MS = 5000L;
	public static final long WAIT_FOR_CONFIG_MS = 30000L;
	private BasicClient telnetCon = null;
	private DigiConnectPortPool parent = null;
	
	/** The queue holding configuration tasks to be done. */
	private ArrayBlockingQueue<AllocatedSerialPort> configQueue = new ArrayBlockingQueue<AllocatedSerialPort>(500);


	public DigiConnectPortManager(DigiConnectPortPool parent)
	{
		this.parent = parent;
		setName(module);
	}
	
	
	@Override
	public void run()
	{
		while (!_shutdown)
		{
			if (telnetCon == null)
			{
				try
				{
					tryConnectTelnet();
				}
				catch (IOException ex)
				{
					// Issue FAILURE, sleep 60 seconds, and try again.
					Logger.instance().failure(module + " Cannot connect to Digi: " + ex);
					try { sleep(60000L); } catch(InterruptedException ie) {}
					continue;
				}
			}
			AllocatedSerialPort asp;
			while(telnetCon.isConnected() && (asp = configQueue.poll()) != null)
				processPort(asp);
			
			try { sleep(PAUSE_MSEC); } catch(InterruptedException ex) {}
		}
	}
	
	private void tryConnectTelnet()
		throws IOException, UnknownHostException
	{
		//TODO connect to the parent's digiIpAddr on port 23
		
		//TODO spawn a separate thread to read and buffer responses from the device
	}
	
	private void disconnect()
	{
		//TODO stop the read-thread
		
		// disconnect the basicClient
		if (telnetCon != null)
			telnetCon.disconnect();
		telnetCon = null;
	}
	
	public void shutdown() { _shutdown = true; }
	
	/**
	 * Called from a polling thread to configure the port. This will be executed inside
	 * a different thread from 'this'. Enqueue a message and wait for 'this' thread to
	 * process it.
	 * @param asp
	 * @return
	 */
	public boolean configPort(AllocatedSerialPort asp)
	{
		asp.ioPort.setConfigureState(PollingThreadState.Waiting);
		try
		{
			if (configQueue.offer(asp, ENQUEUE_WAIT_MS, TimeUnit.MILLISECONDS) == false)
			{
				Logger.instance().info(module + " Timeout trying to enqueue config request.");
				return false;
			}
		}
		catch (InterruptedException e)
		{
			Logger.instance().info(module + " Interrupted while enqueing config request.");
			return false;
		}
		long timeout = System.currentTimeMillis() + WAIT_FOR_CONFIG_MS;
		while(System.currentTimeMillis() < timeout 
			&& asp.ioPort.getConfigureState() == PollingThreadState.Waiting)
		{
			try { Thread.sleep(PAUSE_MSEC); }
			catch(InterruptedException ex) {}
		}
		return asp.ioPort.getConfigureState() == PollingThreadState.Success;
	}
	
	/**
	 * Called from the run loop after an entry is dequeued.
	 * Attempt to use Telnet interface to configure the passed port with the
	 * parameters specified in the TransportMedium.
	 * Set the port's ConfigureState.
	 * @param asp
	 */
	private void processPort(AllocatedSerialPort asp)
	{
		StringBuilder cmdb = new StringBuilder("set serial port=" + asp.ioPort.getPortNum());
		int nparams = 0;
		if (asp.transportMedium.getBaud() > 0)
		{
			cmdb.append(" baudrate=" + asp.transportMedium.getBaud());
			nparams++;
		}
		if (asp.transportMedium.getDataBits() > 0)
		{
			cmdb.append(" databits=" + asp.transportMedium.getDataBits());
			nparams++;
		}
		if (asp.transportMedium.getStopBits() > 0)
		{
			cmdb.append(" stopbits=" + asp.transportMedium.getStopBits());
			nparams++;
		}
		Parity parity = Parity.fromCode(asp.transportMedium.getParity());
		if (parity != Parity.Unknown)
		{
			cmdb.append(" parity=" + parity.toString().toLowerCase());
			nparams++;
		}
		String cmd = cmdb.toString();
		
		if (nparams == 0) // nothing to do
			asp.ioPort.setConfigureState(PollingThreadState.Success);
		else
		{
			// Use telnet interface to configure port
			try
			{
				Logger.instance().debug3(module + " Sending '" + cmdb + "' to digi device "
					+ telnetCon.getName());
				telnetCon.sendData((cmd + "\n").getBytes());
				awaitTelnetResponse();
				
				// TODO - experiment with digi device. Is there a command now that I can
				// issue to verify that the port settings are now changed?
				// TODO Set the success/failed state accordingly
				
				asp.ioPort.setConfigureState(PollingThreadState.Success);
			}
			catch(IOException ex)
			{
				Logger.instance().failure(module + " Error sending config cmd '" + cmd
					+ "' to digi device " + telnetCon.getName() + ": " + ex);
				asp.ioPort.setConfigureState(PollingThreadState.Failed);
			}

		}
			
		
	}


	private void awaitTelnetResponse()
	{
		// TODO Flush any received data up til now.
		// I assume there is some generic response to a telnet command, if only even cr.
	
		// TODO this method may need to be more complicated, e.g. with arguments telling
		// it what to wait for.
	}
}
