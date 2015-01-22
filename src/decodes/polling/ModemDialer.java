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
	private double ConnectWaitSec = 60.0;
	private IOException readError = null;
	private String Break = "+++";
	private String Hangup = "ATZ" + EOL;
	private boolean _inputClosed = false;
	private PatternMatcher CONNECT[] = new PatternMatcher[]
		{ new PatternMatcher("CONNECT".getBytes()) };
	private PatternMatcher  OK[] = new PatternMatcher[]
		{ new PatternMatcher("OK".getBytes()) };

	@Override
	public void connect(IOPort ioPort, TransportMedium tm)
		throws DialException
	{
		module = "ModemDialer(" + tm.platform.getSiteName(false) + ":" + ioPort.getPortNum() + ")";
		
		Logger.instance().debug2(module + ".connect() - Spawning StreamReader to read responses from modem.");
		// Spawn a separate thread to read and buffer responses from the device
		readError = null;
		_inputClosed = false;
		StreamReader streamReader = new StreamReader(ioPort.getIn(), this);
		streamReader.start();
		try
		{
			for(int ntries = 0; ntries < 3 && readError == null; ntries++)
			{
				streamReader.flushBacklog();
				Logger.instance().debug2(module + " sending " + AT + " to modem. Try #" + (ntries+1));
				ioPort.getOut().write(AT.getBytes());
				ioPort.getOut().flush();
				if (!streamReader.wait(AtWaitSec, OK))
				{
					Logger.instance().warning(module + " response to AT failed, session buf: "
						+ AsciiUtil.bin2ascii(streamReader.getSessionBuf()));
					
					disconnect(ioPort);
					continue;
				}
				
				String dialstr = "ATDT " + tm.getMediumId() + EOL;
				Logger.instance().debug1(module + " sending " + dialstr + " to modem. Try #" + (ntries+1));
				ioPort.getOut().write(dialstr.getBytes());
				if (!streamReader.wait(ConnectWaitSec, CONNECT))
				{
					Logger.instance().warning(module + " response to ATDT failed.");
					disconnect(ioPort);
					continue;
				}
				else
					return; // Success!
			}
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
			Logger.instance().debug2(module + " sending " + Break + " to modem.");
			ioPort.getOut().write(Break.getBytes());
			try { Thread.sleep(2000L); } catch(InterruptedException ex) {}
			Logger.instance().debug1(module + " sending " + Hangup + " to modem.");
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
	}

	@Override
	public void inputClosed()
	{
		_inputClosed = true;
	}
	
	@Override
	public String getModule() { return module; }


}
