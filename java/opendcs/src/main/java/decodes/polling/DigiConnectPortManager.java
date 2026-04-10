/*
 * 
 * Copyright 2014 Alberta Environment and Sustainable Resource Development.
 *
 * Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package decodes.polling;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.spi.LoggingEventBuilder;

import decodes.db.TransportMedium;
import ilex.net.BasicClient;
import ilex.util.AsciiUtil;

public class DigiConnectPortManager extends Thread implements StreamReaderOwner
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	public static String module = "DigiPortManager";
	private boolean _shutdown = false;
	
	/** Number of ms to pause if config request queue is empty. */
	private long PAUSE_MSEC = 100L;
	
	/** Number of ms for a calling thread to wait to successfully enqueue a config request. */
	private long ENQUEUE_WAIT_MS = 5000L;
	
	/** Calling threads wait this long for a configuration to succeed. */
	public static final long WAIT_FOR_CONFIG_MS = 60000L;
	
	public static final long MaxConsecutiveConfigFails = 10;
	
	private DigiConnectPortPool parent = null;
	private byte[] digiPrompt = "#>".getBytes(); 
	private String captured = null;
	public static String EOL = "\r"; // "\r\0";
	
	/** The queue holding configuration tasks to be done. */
	private ArrayBlockingQueue<AllocatedSerialPort> configQueue = 
		new ArrayBlockingQueue<AllocatedSerialPort>(500);

	private int consecutiveConfigFails = 0;

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
			AllocatedSerialPort asp = configQueue.poll();
			
			if (asp != null)
				processPort(asp);
			
			try { sleep(PAUSE_MSEC); } catch(InterruptedException ex) {}
		}
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
		PollingThreadState result = asp.ioPort.getConfigureState();
		if (result == PollingThreadState.Success)
			return true;
		else if (result == PollingThreadState.Failed)
			return false;
		else // must be still waiting.
		{
			configQueue.remove(asp); // Make sure it's not still on the queue.
			return false;
		}
	}
	
	private boolean tryEnqueue(AllocatedSerialPort asp)
	{
		try
		{
			if (configQueue.offer(asp, ENQUEUE_WAIT_MS, TimeUnit.MILLISECONDS) == false)
			{
				log.warn("Timeout trying to enqueue config request.");
				return false;
			}
		}
		catch (InterruptedException e)
		{
			log.warn("Interrupted while enqueing config request.");
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
		// Connect to telnet port 23 on digi device.
		BasicClient telnetCon = new BasicClient(parent.getDigiIpAddr(), 23);
		StreamReader streamReader = null;
		String cmd = null;
		try
		{
			log.trace("connecting to {}" + telnetCon.getName());
			telnetCon.connect();
			
			log.trace("spawning StreamReader to read telnet 23 port.");
			
			streamReader = new StreamReader(telnetCon.getInputStream(), this);
			streamReader.setCapture(true);
			streamReader.start();
			
			// Login with supplied username & password
			cmd = "(initial connect)";
			if (!sendAndAwaitResponse(null, 5, "login:".getBytes(), telnetCon, streamReader))
			{
				byte[] sessionBuf = streamReader.getSessionBuf();
				throw new IOException("Never got login prompt from digiconnect device " 
					+ parent.getDigiIpAddr() + ", received " + sessionBuf.length + " bytes: "
					+ AsciiUtil.bin2ascii(streamReader.getSessionBuf()));
			}
			cmd = parent.getDigiUserName()+EOL;
			if (!sendAndAwaitResponse(cmd.getBytes(), 5, "Password:".getBytes(), telnetCon, streamReader))
				throw new IOException("Never got password prompt from digiconnect device " 
					+ parent.getDigiIpAddr());
			cmd = parent.getDigiPassword()+EOL;
			if (!sendAndAwaitResponse(cmd.getBytes(), 5, digiPrompt, telnetCon, streamReader))
			{
				if (!sendAndAwaitResponse(EOL.getBytes(), 3, digiPrompt, telnetCon, streamReader))
					throw new IOException("Never got initial prompt after login from digiconnect device " 
						+ parent.getDigiIpAddr());
			}
			
			// Build command to send to telnet with settings in transport medium.
			StringBuilder cmdb = new StringBuilder("set serial port=" + asp.ioPort.getPortNum()
				+ " flowcontrol=none");
			
			// End of Kludge, build actual params for this session
			if (asp.transportMedium.getBaud() > 0)
				cmdb.append(" baud=" + asp.transportMedium.getBaud());
			if (asp.transportMedium.getDataBits() > 0)
				cmdb.append(" databits=" + asp.transportMedium.getDataBits());
			if (asp.transportMedium.getStopBits() > 0)
				cmdb.append(" stopbits=" + asp.transportMedium.getStopBits());
			Parity parity = Parity.fromCode(asp.transportMedium.getParity());
			if (parity != Parity.Unknown)
				cmdb.append(" parity=" + parity.toString().toLowerCase());
			cmd = cmdb.toString() + EOL;
			
			if (!sendAndAwaitResponse(cmd.getBytes(), 5, digiPrompt, telnetCon, streamReader))
			{
				throw new IOException("No response or invalid response to set serial");
			}
			
			cmd = "show serial port=" + asp.ioPort.getPortNum() + EOL;
			if (!sendAndAwaitResponse(cmd.getBytes(), 5, digiPrompt, telnetCon, streamReader))
				throw new IOException("No response or invalid response to show serial");
			
			if (!verifySettings(asp.ioPort.getPortNum(), asp.transportMedium))
				throw new IOException("verify settings failed. Response was '" + captured + "'");
			
			asp.ioPort.setConfigureState(PollingThreadState.Success);
			consecutiveConfigFails = 0;
		}
		catch (Exception ex)
		{
			consecutiveConfigFails++;
			log.atError()
			   .setCause(ex)
			   .log("Error sending '{}' to digi device {}, consecutive failure={}",
			   		cmd, telnetCon.getName(), consecutiveConfigFails);
			asp.ioPort.setConfigureState(PollingThreadState.Failed);
		}
		finally
		{
			log.trace("Closing telnet 23 to digi.");
			if (streamReader != null)
			{
				streamReader.shutdown();
				try { sleep(100L); } catch(InterruptedException ex) {}
			}
			telnetCon.disconnect();
		}
		if (consecutiveConfigFails > MaxConsecutiveConfigFails)
			tryRebootDigi();
	}


	private void tryRebootDigi()
	{
		log.warn("There have been {} consecutive configuration failures. Rebooting digi device.",
				 consecutiveConfigFails);
		consecutiveConfigFails = 0;
		
		// Connect to telnet port 23 on digi device.
		BasicClient telnetCon = new BasicClient(parent.getDigiIpAddr(), 23);
		StreamReader streamReader = null;
		String cmd = null;
		boolean success = false;
		try
		{
			log.info("tryRebootDigi() connecting to {}", telnetCon.getName());
			telnetCon.connect();
			
			log.info("tryRebootDigi() spawning StreamReader to read telnet 23 port.");
			
			streamReader = new StreamReader(telnetCon.getInputStream(), this);
			streamReader.setCapture(true);
			streamReader.start();
			
			// Login with supplied username & password
			cmd = "(initial connect)";
			if (!sendAndAwaitResponse(null, 5, "login:".getBytes(), telnetCon, streamReader))
			{
				byte[] sessionBuf = streamReader.getSessionBuf();
				throw new IOException("Never got login prompt from digiconnect device " 
					+ parent.getDigiIpAddr() + ", received " + sessionBuf.length + " bytes: "
					+ AsciiUtil.bin2ascii(streamReader.getSessionBuf()));
			}
			cmd = parent.getDigiUserName()+EOL;
			if (!sendAndAwaitResponse(cmd.getBytes(), 5, "Password:".getBytes(), telnetCon, streamReader))
				throw new IOException("Never got password prompt from digiconnect device " 
					+ parent.getDigiIpAddr());
			cmd = parent.getDigiPassword()+EOL;
			if (!sendAndAwaitResponse(cmd.getBytes(), 5, digiPrompt, telnetCon, streamReader))
			{
				if (!sendAndAwaitResponse(EOL.getBytes(), 3, digiPrompt, telnetCon, streamReader))
					throw new IOException("Never got initial prompt after login from digiconnect device " 
						+ parent.getDigiIpAddr());
			}
			
			cmd = "boot action=reset" + EOL;
			telnetCon.sendData(cmd.getBytes());

			// There won't be any further prompts.
			consecutiveConfigFails = 0;
			success = true;
		}
		catch (Exception ex)
		{
			log.atError()
			   .setCause(ex)
			   .log("tryRebootDigi() Error sending '{}' to digi device {}", cmd, telnetCon.getName());
		}
		finally
		{
			log.info("tryRebootDigi() Closing telnet 23 to digi.");
			if (streamReader != null)
			{
				streamReader.shutdown();
				try { sleep(100L); } catch(InterruptedException ex) {}
			}
			telnetCon.disconnect();
		}
		if (success)
		{
			log.info("tryRebootDigi() Successfully rebooted {} -- will wait 30 sec before continuing.",
					 parent.getDigiIpAddr());
			// Digi takes about 30 sec to reboot.
			try { sleep(30000L); } catch(InterruptedException ex) {}
			log.info("tryRebootDigi() Continuing after digi reboot.");
		}
		else
			log.info("tryRebootDigi() -- reboot failed.");
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
	private boolean sendAndAwaitResponse(byte[] toSend, int seconds, byte[] expect,
		BasicClient telnetCon, StreamReader streamReader)
		throws IOException
	{
		// If I'm sending a prompt, flush any received data up til now.
		if (toSend != null)
			streamReader.flushBacklog();
		if (!streamReader.isCapture())
			streamReader.setCapture(true);
		if (toSend != null)
			telnetCon.sendData(toSend);
		
		PatternMatcher expectPat[] = new PatternMatcher[] { new PatternMatcher(expect) };
		boolean found = streamReader.wait(seconds, expectPat);
		
		streamReader.setCapture(false);
		
		captured = new String(streamReader.getCapturedData());
		LoggingEventBuilder le = found ? log.atTrace() : log.atWarn();
		le.log(" Sent {} to digi device {} and recieved response '{}'. {}",
			  (toSend == null ? "nothing" : ("'"+new String(toSend)+"'")),
			  telnetCon.getName(),
			  AsciiUtil.bin2ascii(streamReader.getCapturedData()),
			  (found ? "" : " Expected '" + AsciiUtil.bin2ascii(expect) + "'"));

		return found;
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
			log.warn(module + " verifySettings failed. No response to show serial.");
			return false;
		}
		String cap = new String(captured);
		// Split the string with whitespace as delimiter
		String tokens[] = cap.split("\\s+");
		
		int idx=0;
		for(; idx < tokens.length && !tokens[idx].equals("flowcontrol"); idx++);
		if (idx >= tokens.length-5)
		{
			log.warn("verifySettings failed. Not enough tokens in response to show serial, #tokens={} " +
					 "flowcontrol found at token {}, data returned: {}",
					 tokens.length, idx, cap);
			return false;
		}
		
		String parsing = "port number";
		String tok = tokens[idx+1];
		try
		{
			tok = tokens[idx+1];
			if (portNum != Integer.parseInt(tok))
			{
				log.warn("verifySettings failed. Expected port {} but incorrect port in response: {}",
						 portNum, cap);
				return false;
			}
			parsing = "baud";
			tok = tokens[idx+2];
			if (tm.getBaud() > 0 && tm.getBaud() != Integer.parseInt(tok))
			{
				log.warn("verifySettings failed. Expected baud={}. Incorrect" +
						 " baud in response: {}",
						 tm.getBaud(), cap);
				return false;
			}
			parsing = "databits";
			tok = tokens[idx+3];
			if (tm.getDataBits() > 0 && tm.getDataBits() != Integer.parseInt(tok))
			{
				log.warn("verifySettings failed. Expected databits={}. Incorrect" +
						 " databits in response: {}",
						 tm.getDataBits(), cap);
				return false;
			}
			parsing = "stopbits";
			tok = tokens[idx+4];
			if (tm.getStopBits() > 0 && tm.getStopBits() != Integer.parseInt(tok))
			{
				log.warn("verifySettings failed. Expected stopbits={}. Incorrect" +
						 " stopbits in response: {}",
						 tm.getStopBits(), cap);
				return false;
			}
			Parity tmParity = Parity.fromCode(tm.getParity());
			tok = tokens[idx+5];
			if (tmParity != Parity.Unknown && tmParity != Parity.fromString(tok))
			{
				log.warn("verifySettings failed. Expected parity={}. Incorrect" +
						 " parity in response: {}",
						 tm.getParity(), cap);
				return false;
			}
		}
		catch(NumberFormatException ex)
		{
			log.atWarn()
			   .setCause(ex)
			   .log("verifySettings failed. Expected integer while parsing {} tok='{}' Full Response was: {}",
			   		parsing, tok, cap);
			return false;
		}
		
		return true;
	}


	@Override
	public void inputError(Exception ex)
	{
		/* do nothing */
		
	}


	@Override
	public void inputClosed()
	{
		// TODO Auto-generated method stub
		
	}


	@Override
	public String getModule()
	{
		return module;
	}
}
