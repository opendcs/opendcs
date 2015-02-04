/*
 * $Id$
 * 
 * This software was written by Cove Software, LLC ("COVE") under contract
 * to Alberta Environment and Sustainable Resource Development (Alberta ESRD).
 * No warranty is provided or implied other than specific contractual terms 
 * between COVE and Alberta ESRD.
 *
 * Copyright 2014 Alberta Environment and Sustainable Resource Development.
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

import ilex.util.AsciiUtil;
import ilex.util.Logger;

import java.io.IOException;

import decodes.db.TransportMedium;

/**
 * When connecting:
 * Send "AT\r" & Await "OK\r".
 * SEND "ATDT " + telnum. Then await "CONNECT .*\r"
 * Try up to 3 times.
 *
 * When disconnecting:
 * Send "+++".
 * Wait 2 sec.
 * Send "ATZ".
 */
public class ModemDialer 
	extends Dialer
	implements StreamReaderOwner
{
	public String module = "ModemDialer";
	public static String EOL = "\r";
	private String AT = "AT" + EOL;
	private double AtWaitSec = 5.0;
	private double ConnectWaitSec = 90.0;
	private IOException readError = null;
	private String Break = "+++";
	private String Hangup = "ATZ" + EOL;
	private boolean _inputClosed = false;
	private PatternMatcher CONNECT[] = new PatternMatcher[]
		{ new PatternMatcher("CONNECT".getBytes()) };
	private PatternMatcher  OK[] = new PatternMatcher[]
		{ new PatternMatcher("OK".getBytes()) };
	private PollingThread pollingThread = null;
	
	/**
	 * S0=0 = turn off auto answer
	 * &D2 = normal DTR operation (hangup when digi drops DTR)
	 * &C1 = normal DCD operation
	 * &R2 = hardware flow control
	 * &I0 = disable software xon/xoff
	 */
	private String modemInitString = "ATS0=0&D2&C1&R2&I0";

	@Override
	public void connect(IOPort ioPort, TransportMedium tm, PollingThread pollingThread)
		throws DialException
	{
		this.pollingThread = pollingThread;
		pollingThread.debug2(module + ".connect() - Spawning StreamReader to read responses from modem.");
		// Spawn a separate thread to read and buffer responses from the device
		readError = null;
		_inputClosed = false;
		StreamReader streamReader = new StreamReader(ioPort.getIn(), this);
		streamReader.start();
		try
		{
			ioPort.getOut().write(EOL.getBytes()); ioPort.getOut().flush();
			try { Thread.sleep(500L); } catch(InterruptedException ex) {}
			ioPort.getOut().write(EOL.getBytes()); ioPort.getOut().flush();
			try { Thread.sleep(500L); } catch(InterruptedException ex) {}
			
			streamReader.flushBacklog();
			String init = modemInitString+EOL;
			String msg = module + " sending '" + AsciiUtil.bin2ascii(init.getBytes()) + "' to modem on port "
				+ ioPort.getPortNum();
			pollingThread.debug2(msg);
			pollingThread.annotate(msg);
			ioPort.getOut().write(init.getBytes());
			ioPort.getOut().flush();
			String what = "";
			if (!streamReader.wait(AtWaitSec, OK))
			{
				what = "No response to 'AT' from modem";
				pollingThread.warning(module + " response to AT failed, session buf: "
					+ AsciiUtil.bin2ascii(streamReader.getSessionBuf()));
			}
			else // success OK response to AT
			{
				String dialstr = "ATDT " + tm.getMediumId() + EOL;
				msg = module + " sending '" + AsciiUtil.bin2ascii(dialstr.getBytes()) + "' to modem on port "
					+ ioPort.getPortNum() + ", will wait " + ConnectWaitSec + " sec for CONNECT.";
				Logger.instance().debug1(msg);
				pollingThread.annotate(msg);
				ioPort.getOut().write(dialstr.getBytes());
				if (!streamReader.wait(ConnectWaitSec, CONNECT))
				{
					what = "No answer from station at " + tm.getMediumId();
					msg = module + " " + what;
					pollingThread.warning(msg);
					pollingThread.annotate(msg);
				}
				else
				{
					pollingThread.annotate(module + " dialing success on port" + ioPort.getPortNum() + "!");
					return; // Success!
				}
			}
			pollingThread.annotate("Dialing failed -- " + what);
			disconnect(ioPort);
			throw new DialException("Could not dial modem on port" + ioPort.getPortNum());
		}
		catch (IOException ex)
		{
			throw new DialException("IOException talking to modem: " + ex);
		}
		finally
		{
			streamReader.shutdown();
		}
			
	}

	@Override
	public void disconnect(IOPort ioPort)
	{
		if (ioPort == null || ioPort.getOut() == null)
			return; // must already be in the process of shutting down.
		try
		{
			String msg = module + " sending '" + Break + "' to modem on port " + ioPort.getPortNum();
			pollingThread.debug2(msg);
			pollingThread.annotate(msg);
			ioPort.getOut().write(Break.getBytes());
			try { Thread.sleep(2000L); } catch(InterruptedException ex) {}
			msg = module + " sending '" + AsciiUtil.bin2ascii(Hangup.getBytes()) + "' to modem on port "
				+ ioPort.getPortNum();
			pollingThread.debug1(msg);
			pollingThread.annotate(msg);
			if (ioPort.getOut() != null)
				try { ioPort.getOut().write(Hangup.getBytes()); }
				catch(Exception ex) {}
			try { Thread.sleep(2000L); } catch(InterruptedException ex) {}
		}
		catch(IOException ex)
		{
			Logger.instance().warning(module + " Error while disconnecting: " + ex);
		}
		Logger.instance().debug2(module + " disconnect complete.");	
	}

	@Override
	public void inputError(IOException ex)
	{
		readError = ex;
		pollingThread.annotate(module + " Input Error: " + ex);
	}

	@Override
	public void inputClosed()
	{
		pollingThread.annotate(module + " Input closed.");
		_inputClosed = true;
	}
	
	@Override
	public String getModule() { return pollingThread.getModule() + "" + module; }


}
