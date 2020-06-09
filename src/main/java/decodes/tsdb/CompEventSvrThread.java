package decodes.tsdb;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;

import ilex.net.BasicServer;
import ilex.net.BasicSvrThread;
import ilex.util.IndexRangeException;
import ilex.util.Logger;
import ilex.util.QueueLogger;

public class CompEventSvrThread extends BasicSvrThread
{
	BufferedWriter output;
	int eventIndex;
	QueueLogger eventQueue;

	CompEventSvrThread(CompEventSvr parent, Socket socket, QueueLogger eventQueue)
		throws IOException
	{
		super(parent, socket);
		this.eventQueue = eventQueue;
		output = new BufferedWriter(
				new OutputStreamWriter(socket.getOutputStream()));
		eventIndex = eventQueue.getNextIdx() - 10;
		if (eventIndex < 0)
			eventIndex = 0;
	}

	@Override
	protected void serviceClient()
	{
		try { sendQueuedEvents(); }
		catch(IndexRangeException ex)
		{
			eventIndex = eventQueue.getStartIdx();
		}
		catch(IOException ex)
		{
			Logger.instance().log(Logger.E_INFORMATION,
				"IO Error on User Interface Socket from " + getClientName() 
				+ " -- disconnecting");
			disconnect();
			return;
		}
		try { sleep(500L); }
		catch(InterruptedException ex) {}
	}
	
	private void sendQueuedEvents()
		throws IndexRangeException, IOException
	{
		String msg;
		while( (msg = eventQueue.getMsg(eventIndex)) != null)
		{
			output.write(msg);
			output.newLine();
			eventIndex++;
		}
		output.flush();
	}
}
