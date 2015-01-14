package decodes.polling;

import java.io.IOException;
import java.rmi.UnknownHostException;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import decodes.db.TransportMedium;
import ilex.net.BasicClient;
import ilex.util.Logger;

public class DigiConnectPortManager 
	extends Thread
	implements StreamReaderOwner
{
	public static String module = "DigiConnectPortManager";
	private boolean _shutdown = false;
	
	/** Number of ms to pause if config request queue is empty. */
	private long PAUSE_MSEC = 100L;
	
	/** Number of ms for a calling thread to wait to successfully enqueue a config request. */
	private long ENQUEUE_WAIT_MS = 5000L;
	public static final long WAIT_FOR_CONFIG_MS = 30000L;
	
	/** If telnet connection fails, wait this many ms before re-attempting */
	public static final long WAIT_RECONNECT_MS = 60000L;
	private BasicClient telnetCon = null;
	private DigiConnectPortPool parent = null;
	StreamReader streamReader = null;
	private byte[] digiPrompt = "#>".getBytes(); 
	private String captured = null;
	private int consecutiveFails = 0;
	private boolean telnetInputStreamClosed = false;
	public static String EOL = "\r\0";
	
	/** The queue holding configuration tasks to be done. */
	private ArrayBlockingQueue<AllocatedSerialPort> configQueue = new ArrayBlockingQueue<AllocatedSerialPort>(500);


	public DigiConnectPortManager(DigiConnectPortPool parent)
	{
		this.parent = parent;
		setName(module);
		telnetCon = new BasicClient(parent.getDigiIpAddr(), 23);
	}
	
	
	@Override
	public void run()
	{
		while (!_shutdown)
		{
			if (!telnetCon.isConnected())
			{
				try
				{
					tryConnectTelnet();
				}
				catch (IOException ex)
				{
					Logger.instance().failure(module + " Cannot connect to Digi: " + ex);
					disconnect();
					try { sleep(WAIT_RECONNECT_MS); } catch(InterruptedException ie) {}
					continue;
				}
			}
			
			if (telnetCon.isConnected())
			{
				AllocatedSerialPort asp = configQueue.poll();
				if (asp != null)
					processPort(asp);
			}
			if (configQueue.size() == 0)
			{
				try { sleep(PAUSE_MSEC); } catch(InterruptedException ex) {}
			}
		}
		disconnect();
	}
	
	private void tryConnectTelnet()
		throws IOException, UnknownHostException
	{
		// Connect to the parent's digiIpAddr on port 23
		telnetCon.setHost(parent.getDigiIpAddr());
		telnetCon.setPort(23);
		telnetCon.connect();
		
		// Spawn a separate thread to read and buffer responses from the device
		consecutiveFails = 0;
		telnetInputStreamClosed = false;

Logger.instance().info(module + " spawning StreamReader to read telnet 23 port.");
		streamReader = new StreamReader(telnetCon.getInputStream(), this);
		streamReader.start();
		
		if (!sendAndAwaitResponse(null, 5, "login:".getBytes()))
			throw new IOException("Never got login prompt from digiconnect device " + parent.getDigiIpAddr());
		if (!sendAndAwaitResponse((parent.getDigiUserName()+EOL).getBytes(), 5, "Password:".getBytes()))
			throw new IOException("Never got password prompt from digiconnect device " + parent.getDigiIpAddr());
		if (!sendAndAwaitResponse((parent.getDigiPassword()+EOL).getBytes(), 5, "#>".getBytes()))
			throw new IOException("Never got initial prompt after login from digiconnect device " + parent.getDigiIpAddr());
	}
	
	public void disconnect()
	{
		if (streamReader != null)
			streamReader.shutdown();
		streamReader = null;

		// disconnect the basicClient
		telnetCon.disconnect();
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
		if (!tryEnqueue(asp))
			return false;
		long timeout = System.currentTimeMillis() + WAIT_FOR_CONFIG_MS;
		while(System.currentTimeMillis() < timeout 
			&& asp.ioPort.getConfigureState() == PollingThreadState.Waiting)
		{
			try { Thread.sleep(PAUSE_MSEC); }
			catch(InterruptedException ex) {}
		}
		return asp.ioPort.getConfigureState() == PollingThreadState.Success;
	}
	
	private boolean tryEnqueue(AllocatedSerialPort asp)
	{
		// There can be only one config request for a given port. This one supersedes
		// any outstanding config requests.
//		for(Iterator<AllocatedSerialPort> aspit = configQueue.iterator(); aspit.hasNext(); )
//		{
//			AllocatedSerialPort qasp = aspit.next();
//			if (qasp.ioPort.getPortNum() == asp.ioPort.getPortNum())
//			{
//				aspit.remove();
//				break;
//			}
//		}
		try
		{
			if (configQueue.offer(asp, ENQUEUE_WAIT_MS, TimeUnit.MILLISECONDS) == false)
			{
				Logger.instance().warning(module + " Timeout trying to enqueue config request.");
				return false;
			}
		}
		catch (InterruptedException e)
		{
			Logger.instance().warning(module + " Interrupted while enqueing config request.");
			return false;
		}
		return true;
		
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
		// Build command to send to telnet with settings in transport medium.
		StringBuilder cmdb = new StringBuilder("set serial port=" + asp.ioPort.getPortNum());
		int nparams = 0;
		if (asp.transportMedium.getBaud() > 0)
		{
			cmdb.append(" baud=" + asp.transportMedium.getBaud());
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
Logger.instance().info(module + " will attempt " + cmd);		
		
		if (nparams == 0) // nothing to do
			asp.ioPort.setConfigureState(PollingThreadState.Success);
		else
		{
			try
			{
				if (!sendAndAwaitResponse((cmd + EOL).getBytes(), 5, digiPrompt))
				{
					// Failed to get prompt after sending set serial command. 
					// Also, more than one consecutive failure means we close telnet.
					if (consecutiveFails >= 2)
						disconnect();
					asp.ioPort.setConfigureState(PollingThreadState.Failed);
					return;
				}
				
				String showCmd = "show serial port=" + asp.ioPort.getPortNum() + EOL;
				if (!sendAndAwaitResponse(showCmd.getBytes(), 5, digiPrompt))
				{
					// Failed to get prompt after sending show serial command.
					// Also, more than one consecutive failure means we close telnet.
					if (consecutiveFails >= 2)
						disconnect();
					asp.ioPort.setConfigureState(PollingThreadState.Failed);
					return;
				}
				
				if (!verifySettings(asp.ioPort.getPortNum(), asp.transportMedium))
				{
					// This means that the set command failed to make the required settings
					asp.ioPort.setConfigureState(PollingThreadState.Failed);
					return;
				}
				
				asp.ioPort.setConfigureState(PollingThreadState.Success);
			}
			catch(IOException ex)
			{
				Logger.instance().failure(module + " Error sending config cmd '" + cmd
					+ "' to digi device " + telnetCon.getName() + ": " + ex);
				asp.ioPort.setConfigureState(PollingThreadState.Failed);
				disconnect();
			}
		}
	}


	/**
	 * Optionally send a string, wait a number of seconds for an expected response.
	 * All data received after sending command up to and including the response will
	 * be saved in instance variable 'captured'.
	 * @param toSend data to send or null if don't send
	 * @param seconds number of seconds to wait for expected response
	 * @param expect the expected response
	 * @param capture true if data should be captured up to and including response
	 * @return true if expected response was received, false if timeout
	 * @throws IOException
	 */
	private boolean sendAndAwaitResponse(byte[] toSend, int seconds, byte[] expect)
		throws IOException
	{
		// Flush any received data up til now.
		streamReader.flushBacklog();
		streamReader.setCapture(true);
		if (toSend != null)
			telnetCon.sendData(toSend);
		
		PatternMatcher expectPat[] = new PatternMatcher[] { new PatternMatcher(expect) };
		boolean found = streamReader.wait(seconds, expectPat);
		if (found)
			consecutiveFails = 0;
		else
			consecutiveFails++;
		
		streamReader.setCapture(false);
		
		captured = new String(streamReader.getCapturedData());
		Logger.instance().log(
			found ? Logger.E_DEBUG2 : Logger.E_WARNING,
			" Sent " 
			+ (toSend == null ? "nothing" : ("'"+new String(toSend)+"'"))
			+ " to digi device " + telnetCon.getName()
			+ " and received response '" + captured + "'."
			+ (found ? "" : " Expected '" + new String(expect) + "'"));

		return found;
	}


	@Override
	public void inputError(IOException ex)
	{
		Logger.instance().failure(module + " Error reading from Digi telnet port: " + ex);
		disconnect();
	}
	
	/**
	 * This is called after sending the 'show serial' command. Look at the captured data
	 * returned by the command and compare to settings in TransportMedium.
	 * @param tm
	 * @return true if settings match, false if they do not.
	 */
	private boolean verifySettings(int portNum, TransportMedium tm)
	{
		// Scan the captured string for settings that match TM.
		// There should be two lines of the format:
		//       port    baudrate     databits    stopbits    parity    flowcontrol^M^M
	    //       1       19200            8           1      none           none^M^M	
		if (captured == null)
		{
			Logger.instance().warning(module + " verifySettings failed. No response"
				+ " to show serial.");
			return false;
		}
		String cap = new String(captured);
		// Split the string with whitespace as delimiter
		String tokens[] = cap.split("\\s+");
		
Logger.instance().info(module + " Tokanized response is: ");
		int idx=0;
		for(; idx < tokens.length && !tokens[idx].equals("flowcontrol"); idx++);
		if (idx >= tokens.length-5)
		{
			Logger.instance().warning(module + " verifySettings failed. Not enough"
				+ " tokens in response to show serial: " + cap);
			return false;
		}
		
//for(int ti = 0; ti<tokens.length; ti++)
//	Logger.instance().info("tok[" + ti + "] = '" + tokens[ti] + "'");
//
		
		String parsing = "port number";
		String tok = tokens[idx+1];
		try
		{
			tok = tokens[idx+1];
			if (portNum != Integer.parseInt(tok))
			{
				Logger.instance().warning(module + " verifySettings failed. "
					+ "Expected port " + portNum + " but incorrect"
					+ " port in response: " + cap);
				return false;
			}
			parsing = "baud";
			tok = tokens[idx+2];
			if (tm.getBaud() > 0 && tm.getBaud() != Integer.parseInt(tok))
			{
				Logger.instance().warning(module + " verifySettings failed. "
					+ "Expected baud=" + tm.getBaud() + ". Incorrect"
					+ " baud in response: " + cap);
				return false;
			}
			parsing = "databits";
			tok = tokens[idx+3];
			if (tm.getDataBits() > 0 && tm.getDataBits() != Integer.parseInt(tok))
			{
				Logger.instance().warning(module + " verifySettings failed. "
					+ "Expected databits=" + tm.getDataBits() + ". Incorrect"
					+ " databits in response: " + cap);
				return false;
			}
			parsing = "stopbits";
			tok = tokens[idx+4];
			if (tm.getStopBits() > 0 && tm.getStopBits() != Integer.parseInt(tok))
			{
				Logger.instance().warning(module + " verifySettings failed. "
					+ "Expected stopbits=" + tm.getStopBits() + ". Incorrect"
					+ " stopbits in response: " + cap);
				return false;
			}
			Parity tmParity = Parity.fromCode(tm.getParity());
			tok = tokens[idx+5];
			if (tmParity != Parity.Unknown && tmParity != Parity.fromString(tok))
			{
				Logger.instance().warning(module + " verifySettings failed. "
					+ "Expected parity=" + tm.getParity() + ". Incorrect"
					+ " parity in response: " + cap);
				return false;
			}
		}
		catch(NumberFormatException ex)
		{
			Logger.instance().warning(module + " verifySettings failed. "
				+ "Expected integer while parsing " + parsing + "tok='" + tok + "' Full Response was: " + cap);
			return false;
		}
		
		return true;
	}


	@Override
	public void inputClosed()
	{
		telnetInputStreamClosed = true;
	}
	
	@Override
	public String getModule() { return module; }
}
