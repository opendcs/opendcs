/*
*  $Id$
*/
package ilex.net;

import ilex.net.BasicServer;
import java.net.*;
import java.util.Date;
import java.io.IOException;
import ilex.util.Logger;

/**
* Class BasicSvrThread works with BasicServer to provide the network
* functionality necessary in a simple multi-threaded server.
* You should subclass this class and implement the specific functionality
* for talking to your clients.
*/
public abstract class BasicSvrThread extends Thread
{
	/** The client connection socket */
	protected Socket socket;

	/** The owning BasicServer object */
	protected BasicServer parent;

	/** True if currently connected */
	protected boolean connected;
	
	private Date connectTime = null;
	
	protected boolean isReadOnly = false;

	/**
	* Constructor called from your concrete subclass.
	* @param parent the owning BasicServer
	* @param socket the client connection socket
	*/
	protected BasicSvrThread( BasicServer parent, Socket socket )
	{
		this.parent = parent;
		this.socket = socket;
		connected = true;
		connectTime = new Date();
	}
	
	public Date getConnectTime() { return connectTime; }

	/** Guarantees disconnect when object destroyed. */
	public void finalize( )
	{
		if (connected)
		{
			Logger.instance().warning("Disconnect called from finalize. "
				+ " Server thread left socket open.");
			disconnect();
		}
	}

	/**
	* You can overload this if you need to do special clean-up in your
	* subclass. But if you do, you should call super.disconnect() so
	* that the base-class function gets called.
	* The base-class disconnect method closes the socket and removes
	* this thread from the server's list. Finally, it sets a boolean
	* flag causing the thread's run() method to exit.
	*/
	public void disconnect( )
	{
		try { socket.close(); } catch (IOException ioe) {}

		parent.rmSvrThread(this);  // Remove me from parent's list.

		connected = false;         // Cause run() to exit.
		Logger.instance().debug1("BasicSvrThread disconnect complete for " 
			+ getClientName());
//		try { throw new Exception(); }
//		catch(Exception ex)
//		{
//			System.err.println("Disconnect stack trace:");
//			ex.printStackTrace(System.err);
//		}
	}

	/**
	  Thread run method.
	  You do not need to override this unless you want different behavior.
	  This method will continually call the "serviceClient" method as long
	  as the socket is open.
	  @see BasicSvrThread#serviceClient
	*/
	public void run( )
	{
		try
		{
			while(connected)
			{
				if (isConnected())
					serviceClient();
				else
					disconnect();
			}
		}
		catch (Exception ex)
		{
			disconnect();
			String msg = "Unexpected exception servicing client: " + ex;
			Logger.instance().failure(msg);
			System.err.println(msg);
			ex.printStackTrace(System.err);
		}
		Logger.instance().debug1(parent.module + " client-thread exiting "
			+ getClientName());
	}
	
	public boolean isConnected()
	{
		if (socket.isClosed())
		{
			Logger.instance().info(parent.module + " socket.isClosed()");
			return false;
		}
		if (!socket.isConnected())
		{
			Logger.instance().info(parent.module + " !socket.isConnected()");
			return false;
		}
		if (socket.isInputShutdown())
		{
			Logger.instance().info(parent.module + " socket.isInputShutdown()");
			return false;
		}
		// Don't shut down in the write-half of a read-only connection times out.
		if (!isReadOnly && socket.isOutputShutdown())
		{
			Logger.instance().info(parent.module + " socket.isOutputShutdown()");
			return false;
		}
		return true;
	}

	/**
	* You must override the serviceClient() method. In this method you
	* should read and respond to data coming from the client. Your method
	* should block while reading client data. No blocking is done in the
	* thread outside of this method.
	*/
	protected abstract void serviceClient( );

	/**
	* @return hostname of other end of this connection.
	*/
	public String getClientName( )
	{
		if (!connected)
			return "none";
		InetAddress ipaddr = socket.getInetAddress();
		if (ipaddr == null)
			return "unknown-host";
		else
			return ipaddr.getHostName();
	}

	/**
	* @return owning BasicServer object
	*/
	public BasicServer getParent( )
	{
		return parent;
	}

	/**
	* @return this client's socket connection
	*/
	public Socket getSocket( )
	{
		return socket;
	}
}

