package decodes.polling;

import java.io.IOException;

import ilex.net.BasicClient;
import decodes.db.TransportMedium;

/**
 * Implements Dialer for TCP Socket Connections.
 * In a TCP Connection, 'Dialing' involves opening the socket. It is passed
 * an IOPort with no connection established. The Transport Medium ID must be of
 * the form host[:port], where host can be a resolvable hostname or an IP address.
 * If port is omitted, 23 is assumed (the standard Telnet Port used by many
 * network DCPs.)
 */
public class TcpDialer extends Dialer
{
	private BasicClient basicClient = null;

	@Override
	public void connect(IOPort ioPort, TransportMedium tm)
		throws DialException
	{
		// Medium ID should be of the form host[:port], where host can be
		// a resolvable hostname or an IP address.
		// Port defaults to 23, which is the standard telnet port.
		String host = tm.getMediumId();
		int port = 23;
		int colon = host.indexOf(':');
		if (colon > 0)
		{
			try
			{
				port = Integer.parseInt(host.substring(colon+1).trim());
				host = host.substring(0,colon);
			}
			catch(NumberFormatException ex)
			{
				throw new DialException("Invalid host string '" + host + "' -- expected port number after colon.");
			}
		}
		basicClient = new BasicClient(host, port);
		try
		{
			basicClient.connect();
		}
		catch (IOException ex)
		{
			throw new DialException("Cannot connect to '" + tm.getMediumId() + "': " + ex);
		}
		ioPort.setIn(basicClient.getInputStream());
		ioPort.setOut(basicClient.getOutputStream());
	}

	@Override
	public void disconnect(IOPort ioPort)
	{
		if (basicClient != null)
			basicClient.disconnect();
		basicClient = null;
		ioPort.setIn(null);
		ioPort.setOut(null);
	}

}
