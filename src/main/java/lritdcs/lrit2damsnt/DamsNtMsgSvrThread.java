package lritdcs.lrit2damsnt;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import ilex.net.BasicSvrThread;

public class DamsNtMsgSvrThread extends BasicSvrThread
{
	/** The socket output stream. */
	private OutputStream outs;

	protected DamsNtMsgSvrThread(DamsNtMsgSvr parent, Socket socket)
		throws IOException
	{
		super(parent, socket);
		outs = socket.getOutputStream();
	}

	@Override
	protected void serviceClient()
	{
		try { sleep(1000L); }
		catch (InterruptedException e) {}
	}
	
	public void sendToClient(byte[] msg)
		throws IOException
	{
		// Send the data to the client
		try
		{
			outs.write(msg);
			outs.flush();
		}
		catch(Exception ex)
		{
			throw new IOException("Error sending data: " + ex);
		}
	}

}
