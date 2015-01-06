package decodes.polling;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.Date;

import ilex.net.BasicServer;
import ilex.net.BasicSvrThread;

public class TestPollScriptServer extends BasicServer
{
	String scriptFileName = null;

	public TestPollScriptServer(int port, String scriptFileName)
		throws IllegalArgumentException, IOException
	{
		super(port);
		this.scriptFileName = scriptFileName;
	}

	@Override
	protected BasicSvrThread newSvrThread(Socket sock) throws IOException
	{
		System.out.println("New Client");
		return new TestPollScriptServerThread(this, sock);
	}
	
	/**
	 * Usage: TestPollScriptServer listeningPort scriptname
	 * Open listening socket on specified port. When a client connects spawn a
	 * TestPollScriptServerThread to execute the specified script with the client.
	 * 
	 * @param args Two arguments expected: listeningPort scriptname
	 */
	public static void main(String args[])
		throws Exception
	{
		int port = Integer.parseInt(args[0]);
		
		TestPollScriptServer tts = new TestPollScriptServer(port, args[1]);
		tts.listen();
	}
}

class TestPollScriptServerThread
	extends BasicSvrThread
{
	protected TestPollScriptServerThread(BasicServer parent, Socket socket)
	{
		super(parent, socket);
	}

	@Override
	protected void serviceClient()
	{
		PollScriptProtocol prot = new PollScriptProtocol();
		try
		{
			prot.readScript(new File(((TestPollScriptServer)parent).scriptFileName));
			
			// mock up an IOPort to execute the script
			IOPort ioPort = new IOPort(null, 0, null);
			ioPort.setIn(getSocket().getInputStream());
			ioPort.setOut(getSocket().getOutputStream());
			
			prot.executeScript(ioPort, new Date(System.currentTimeMillis() - 3600000L));
		}
		catch (Exception ex)
		{
			System.err.println(ex);
			ex.printStackTrace(System.err);
		}
		disconnect();
	}
}