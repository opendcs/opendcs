package lritdcs.lrit2damsnt;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;

import ilex.net.BasicServer;
import ilex.net.BasicSvrThread;

public class DamsNtMsgSvr
	extends BasicServer
{
	static final byte[] none = "NONE\r\n".getBytes();
	private Lrit2DamsNt parent = null;

	public DamsNtMsgSvr(int port, InetAddress bindaddr, Lrit2DamsNt parent)
		throws IOException
	{
		super(port, bindaddr);
		this.parent = parent;
	}
	
	@Override
	protected BasicSvrThread newSvrThread(Socket sock)
		throws IOException
	{
		return new DamsNtMsgSvrThread(this, sock);
	}

	/**
	 * Send the NONE message to all currently connect clients.
	 */
	public void sendNone()
	{
		parent.debug("Sending NONE to clients.");
		sendToClients(none);
	}

	public void distribute(byte[] msgData)
	{
		parent.debug("Distributing DCP Message '" + new String(msgData) + "'");
		sendToClients(msgData);
	}
	
	private void sendToClients(byte [] data)
	{
		ArrayList<DamsNtMsgSvrThread> badClients = new ArrayList<DamsNtMsgSvrThread>();
		for(BasicSvrThread bst : mySvrThreads)
		{
			DamsNtMsgSvrThread damsNtMsgSvrThread = (DamsNtMsgSvrThread)bst;
			try
			{
				damsNtMsgSvrThread.sendToClient(data);
			}
			catch (IOException ex)
			{
				parent.warning("Error sending data to client '"
					+ damsNtMsgSvrThread.getClientName() + "': " + ex 
					+ " -- will disconnect.");
				badClients.add(damsNtMsgSvrThread);
			}
		}
		
		// Can't do this in above loop because it would modify the collection that
		// I'm iterating.
		for(DamsNtMsgSvrThread client : badClients)
			client.disconnect();
	}
}
