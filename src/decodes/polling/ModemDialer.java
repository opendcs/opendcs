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
	public static String EOL = "\r\n";
	private String AT = "AT" + EOL;
	private double AtWaitSec = 3.0;
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
			streamReader.flushBacklog();
			String msg = module + " sending " + AT + " to modem.";
			pollingThread.debug2(msg);
			pollingThread.annotate(msg);
			ioPort.getOut().write(AT.getBytes());
			ioPort.getOut().flush();
			if (!streamReader.wait(AtWaitSec, OK))
			{
				pollingThread.warning(module + " response to AT failed, session buf: "
					+ AsciiUtil.bin2ascii(streamReader.getSessionBuf()));
			}
			else // success OK response to AT
			{
				String dialstr = "ATDT " + tm.getMediumId() + EOL;
				msg = module + " sending " + dialstr + " to modem.";
				Logger.instance().debug1(msg);
				pollingThread.annotate(msg);
				ioPort.getOut().write(dialstr.getBytes());
				if (!streamReader.wait(ConnectWaitSec, CONNECT))
				{
					msg = module + " response to ATDT failed.";
					pollingThread.warning(msg);
					pollingThread.annotate(msg);
				}
				else
				{
					pollingThread.annotate(module + " dialing success!");
					return; // Success!
				}
			}
			pollingThread.annotate("Dialing failed.");
			disconnect(ioPort);
			throw new DialException("Could not dial modem.");
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
			String msg = module + " sending " + Break + " to modem.";
			pollingThread.debug2(msg);
			pollingThread.annotate(msg);
			ioPort.getOut().write(Break.getBytes());
			try { Thread.sleep(2000L); } catch(InterruptedException ex) {}
			msg = module + " sending " + Hangup + " to modem.";
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
