/*
*  $Id$
*/
package decodes.dcpmon1;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.net.*;
import ilex.net.*;
import ilex.util.StringPair;
import decodes.consumer.HtmlFormatter;

/**
This class is the socket server portion of the DCP Monitor application.
The server object listens for connections on the port specified
in the configuration.
*/
public class DcpMonitorServer extends BasicServer
{
	/**
	  Constructor.
	  @throw IOException if can't bind to the configured socket.
	*/
	public DcpMonitorServer()
		throws IOException
	{
		super(DcpMonitorConfig.instance().serverPort);
		HtmlFormatter.comesFromDcpMon = true;
	}

	/**
	  Overloaded from base class, this method constructs a new server
	  thread and returns it. The base server class holds the thread in its
	  collection and delegates IO operations to it.
	  @return DcpMonitorServerThread object to service a new client.
	*/
	public BasicSvrThread newSvrThread(Socket sock)
		throws IOException
	{
		return new DcpMonitorServerThread(this, sock);
	}

	/**
	  Places the server's status properties into the passed object.
	  @param status Vector of StringPair objects represeting server status.
	*/
	public void setStatus(ArrayList<StringPair> status)
	{
		status.add(new StringPair("numClients", "" + getNumSvrThreads()));
		status.add(new StringPair("numXmitsWritten",
				 "" + XRWriteThread.numQueued));
		status.add(new StringPair("lastXmitTime",
			 (new Date(XRWriteThread.lastMsgMsec)).toString()));
	}
}

