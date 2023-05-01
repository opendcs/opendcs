package ilex.net;

import ilex.util.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Opens listening socket for control and another for forwarding.
 * Control port accepts lines formatted as hostname_or_ip:port
 * Subsequent connects to forwarding port are forwarded to the specified hostname & ip.
 * @author mmaloney
 *
 */
public class PortForwardServer
	extends BasicServer
	
{
	private int fwdPort = 6785;
	String fwdToHost = null;
	int fwdToPort = 6785;
	private ForwardingServer forwardingServer = null;
	
	public PortForwardServer(int ctrlPort, int fwdPort)
		throws IOException
	{
		super(ctrlPort);
		this.fwdPort = fwdPort;
	}

	/**
	* Returns new TestFileSvrThread object
	* @param sock the socket to the remote client
	* @return new TestFileSvrThread object
	*/
	protected BasicSvrThread newSvrThread( Socket sock ) throws IOException
	{
		System.out.println("New Client");
		return new PortForwardCtlThread(this, sock);
	}
	
	private void start() 
		throws IOException
	{
		forwardingServer = new ForwardingServer(this, fwdPort);
		new Thread()
		{
			public void run()
			{
				try
				{
					forwardingServer.listen();
				}
				catch (IOException e)
				{
					System.err.println("ForwardingServerError: " + e);
					e.printStackTrace();
					shutdown();
				}
			}
		}.start();
		listen();
	}

	/**
	* Usage java ilex.net.PortForwardServer ctlPort fwdPort
	* @param args args[0]==control port  args[1]==forwarding port
	* @throws IOException
	*/
	public static void main( String[] args ) throws IOException
	{
		Logger.instance().setMinLogPriority(Logger.E_DEBUG3);
		int ctlPort = Integer.parseInt(args[0]);
		int fwdPort = Integer.parseInt(args[1]);
		
		PortForwardServer tfs = new PortForwardServer(ctlPort, fwdPort);
		tfs.start();
	}
	
	public void setForwardTo(String host, int port)
	{
		this.fwdToHost = host;
		this.fwdToPort = port;
		System.out.println("Set forward to to host='" + host + "', port=" + port);
	}
}

class PortForwardCtlThread extends BasicSvrThread
{
	PortForwardCtlThread(PortForwardServer parent, Socket socket)
	{
		super(parent, socket);
	}
	
	@Override
	protected void serviceClient()
	{
		try
		{
			BufferedInputStream bis = new BufferedInputStream(socket.getInputStream());
			BufferedReader br = new BufferedReader(new InputStreamReader(bis));
			String line = br.readLine();
			int colon = line.indexOf(':');
			if (colon <= 0)
			{
				System.err.println("Invalid line '" + line + "' -- no colon separator.");
				disconnect();
			}
			int port = Integer.parseInt(line.substring(colon+1));
			((PortForwardServer)parent).setForwardTo(line.substring(0, colon), port);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			disconnect();
		}
	}
	
}

class ForwardingServer extends BasicServer
{
	PortForwardServer mainServer = null;
	
	public ForwardingServer(PortForwardServer mainServer, int port)
		throws IllegalArgumentException, IOException
	{
		super(port);
		this.mainServer = mainServer;
	}
	
	@Override
	protected BasicSvrThread newSvrThread(Socket localSock) throws IOException
	{
		return new ForwardingThread(this, localSock);
	}
	
	public void shutdown()
	{
		System.err.println("Forwarding Server shutdown");
		mainServer.shutdown();
	}

}

class ForwardingThread extends BasicSvrThread
{
	String fwdToHost = null;
	int fwdToPort = 6785;
	private boolean started = false;
	BasicClient remoteClient = null;
	int n = 0;
	
	protected ForwardingThread(ForwardingServer parent, Socket localSock)
	{
		super(parent, localSock);
		fwdToHost = parent.mainServer.fwdToHost;
		fwdToPort = parent.mainServer.fwdToPort;
	}

	@Override
	protected void serviceClient()
	{
		if (!started)
		{
			startup();
			started = true;
		}
		
		// Use this thread to forward input from remote to local.
		InputStream inFromRemote = remoteClient.input;
		try
		{
			OutputStream outToLocal = this.socket.getOutputStream();
			int c;
			while((c = inFromRemote.read()) != -1)
			{
				outToLocal.write(c);
				System.out.println("Write '" + (char)c + "' to Local");
				n++;
			}
			System.out.println("inFromRemote.read() returned -1, n=" +n);
		}
		catch(Exception ex)
		{
			System.err.println("ForwardingThread remoteIn-->localOut exception: " + ex
				+ " -- will disconnect.");
			ex.printStackTrace();
			disconnect();
		}
	
	}
	
	private void startup()
	{
		// Local is the socket associated with THIS
		// Remote is the connection I make to the fwdHost:fwdPort
		
		System.out.println("Opening connection to host='" + fwdToHost + "', port=" + fwdToPort);
		remoteClient = new BasicClient(fwdToHost, fwdToPort);
		try
		{
			remoteClient.connect();
			
			// Separate thread to forward input from local to the remote
			final InputStream inpFromLocal = this.socket.getInputStream();
			final OutputStream outToRemote = remoteClient.output;
			new Thread()
			{
				int n=0;
				public void run()
				{
					while (socket.isConnected() && remoteClient.isConnected())
					{
						try
						{
							int c;
							while((c = inpFromLocal.read()) != -1)
							{
								outToRemote.write(c);
								System.out.println("Write '" + (char)c + "' to remote");
								n++;
							}
							System.out.println("inpFromLocal.read() returned -1, n=" + n);
						}
						catch(Exception ex)
						{
							System.err.println("error on inpLocal --> outRemote: " + ex);
							disconnect();
							break;
						}
					}
					System.out.println("inpLocal-->outRemote exiting -- socket.isConnected=" 
						+ socket.isConnected()
						+ ", remoteClient.isConnected=" + remoteClient.isConnected());
				}
			}.start();
			
		}
		catch (Exception e)
		{
			System.err.println("startup exception: " + e);
			e.printStackTrace();
			System.out.println("serviceClient() disconnecting.");
			disconnect();
		}
	}
	
}

