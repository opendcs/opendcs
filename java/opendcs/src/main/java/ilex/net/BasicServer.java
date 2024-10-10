/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.3  2009/08/26 11:04:06  mjmaloney
*  Collections
*
*  Revision 1.2  2008/04/23 11:23:57  cvs
*  dev
*
*  Revision 1.1  2008/04/04 18:21:09  cvs
*  Added legacy code to repository
*
*  Revision 1.22  2007/06/07 11:50:20  mmaloney
*  Added getPort() method.
*
*  Revision 1.21  2005/11/21 19:14:59  mmaloney
*  LRGS 5.4 release prep
*
*  Revision 1.20  2005/07/10 14:21:48  mjmaloney
*  Don't use IllegalArgumentException -- it's not checked.
*
*  Revision 1.19  2005/01/07 18:45:21  mjmaloney
*  Added getSvrThreads() method to iterate through all clients.
*
*  Revision 1.18  2004/08/30 14:50:22  mjmaloney
*  Javadocs
*
*  Revision 1.17  2004/05/25 13:03:59  mjmaloney
*  LRIT Release Prep
*
*  Revision 1.16  2004/05/17 22:59:09  mjmaloney
*  Default for -h arg set to "localhost"
*
*  Revision 1.15  2004/03/01 20:18:16  mjmaloney
*  Upgraded socket apps to flush data after each interaction.
*
*  Revision 1.14  2003/12/18 19:38:55  mjmaloney
*  Added BasicSvrThread.getParent() and BasicServer.getNumClients() methods.
*
*  Revision 1.13  2003/05/01 13:58:56  mjmaloney
*  Rethrow IOExecption if listen throws for reason other than shutdown.
*
*  Revision 1.12  2003/05/01 13:57:05  mjmaloney
*  No err msg if IOException on listen() because of shutdown.
*
*  Revision 1.11  2001/03/13 03:05:29  mike
*  Fixed bug - was instantiating server thread twice.
*
*  Revision 1.10  2000/09/08 13:11:03  mike
*  Release prep
*
*  Revision 1.9  2000/06/07 15:09:00  mike
*  Removed diagnostic.
*
*  Revision 1.8  2000/06/07 12:13:25  mike
*  Removed stdout message "Added thread"
*
*  Revision 1.7  2000/05/31 21:14:27  mike
*  dev
*
*  Revision 1.6  2000/01/19 14:34:37  mike
*  Debug messages to detect garbage collection.
*
*  Revision 1.5  1999/12/06 15:07:08  mike
*  Removed println diagnostics
*
*  Revision 1.4  1999/10/22 17:28:22  mike
*  10/22/1999
*
*  Revision 1.3  1999/10/09 17:03:40  mike
*  10/9/1999
*
*  Revision 1.2  1999/09/30 18:16:44  mike
*  9/30/1999
*
*  Revision 1.1  1999/09/20 17:20:01  mike
*  Initial
*
*
*/

package ilex.net;

import ilex.net.BasicSvrThread;
import java.net.*;
import java.util.LinkedList;
import java.util.Iterator;
import java.io.*;

/**
The BasicServer class is intended to be sub-classed by more complete
servers. It provides the basic functionality for:
<ul>
  <li>Listening on a well-known port</li>
  <li>Accepting new clients</li>
  <li>Handling client disconnects</li>
</ul>
*/
public abstract class BasicServer
{
	/** The port number to listen on */
	protected int portNum;

	/** The listening socket */
	protected ServerSocket listeningSocket;

	/** List of BasicSvrThread objects, one for each client connection. */
	protected LinkedList<BasicSvrThread> mySvrThreads;

	/** The listening thread */
	private Thread listeningThread;

	/** The bind address if one is specified. */
	private InetAddress bindaddr;

	/** Should be set by concrete subclass calling setModuleName() */
	String module = "BasicServer";


	/**
	* The constructor will throw IllegalArgumentException if port is
	* <= 0. It will throw an IOException if it cannot create a listening
	* socket on the specified port.
	* @param port the port to listen on
	*/
	public BasicServer( int port ) throws IllegalArgumentException, IOException
	{
		this(port, null);
	}

	/**
	* This version of the constructor allows you to specify the inet
	* address for the listening socket. This should be used if your
	* host has more than one network connection and you need to specify
	* which to use.
	* @param port port to listen on
	* @param bindaddr used if you have multiple NICs and only want to listen 
	* on one.
	*/
	public BasicServer( int port, InetAddress bindaddr ) 
		throws IOException
	{
		if (port <= 0)
			throw new IOException(
				"BasicServer: port number must be a positive integer.");

		portNum = port;
		this.bindaddr = bindaddr;
		makeServerSocket();
		mySvrThreads = new LinkedList();
		listeningThread = null;
	}

	/**
	* Makes the server (listening) socket.
	* @throws IllegalArgumentException if bad bindaddr or port number.
	* @throws IOException if can't open.
	*/
	protected void makeServerSocket( ) throws IllegalArgumentException, IOException
	{
		listeningSocket = new ServerSocket(portNum, 50, bindaddr);
	}

	/**
	* The listen method continually waits for new clients and accepts
	* them. When a new client is accepted, the serviceNewClient method
	* is called.
	* <p>
	* If you have set a timeout on the listening socket, this method
	* will call the listenTimeout method when a timeout occurs. Your
	* sub-class should override listenTimeout to perform periodic
	* functions while listening.
	* Other IOExceptions indicate error conditions on the listening
	* socket and should be handled by the caller.
	* @throws IOException if IO error on listening socket.
	*/
	public void listen( ) throws IOException
	{
		while(listeningSocket != null)   // shutdown() will null the socket
		{
			try
			{
				listeningThread = Thread.currentThread();
				Socket client = listeningSocket.accept();
				listeningThread = null;
				serviceNewClient(client);
			}
			catch(InterruptedIOException intr)
			{
				listenTimeout();
			}
			catch(IOException ioe)
			{
				// only rethrow if IOE is NOT due to shutdown.
				if (listeningSocket != null)
					throw ioe;
			}
		}
	}

	/**
	  Guarantees that listening socket is shut down before object destroyed.
	*/
	public void finalize( )
	{
		if (listeningSocket != null)
			shutdown();
	}

	/**
	* The shutdown method will disconnect all client threads and close
	* the listening socket.
	*/
	public void shutdown( )
	{
		killAllSvrThreads();

		ServerSocket tsock = listeningSocket;
		listeningSocket = null;
		if (listeningThread != null)
			listeningThread.interrupt();
		try { Thread.sleep(100L); }
		catch(InterruptedException ie) {}

		try 
		{
			if (tsock != null)
				tsock.close(); 
		}
		catch(IOException ioe) {}
	}

	/**
	* This is the low-level method called when a new client has
	* connected to the server. The default action taken here is to
	* call your over-ridden 'newSvrThread()' method, which should
	* create a new thread object to handle the client.
	* Override this method only if you need more low-level control.
	* @param client the new client connection
	*/
	protected void serviceNewClient( Socket client ) throws IOException
	{
		BasicSvrThread bst = newSvrThread(client);
		if (bst != null)
		{
			addSvrThread( bst );
			bst.start();
		}
	}

	/**
	* rmSvrThread is typically called from the child thread object
	* when it wants to die. It must be synchronized to protect the
	* vector.
	* @param bst the BasicSvrThread to remove.
	*/
	public synchronized void rmSvrThread( BasicSvrThread bst )
	{
		boolean wasthere = mySvrThreads.remove(bst);
	}

	/**
	 * This method replaces the old 'getSvrThreads' method. The old method
	 * was unsafe because iterating the list could cause a 
	 * ConcurrentModificationException when a client asynchronously disconnects.
	 *
	 * @return a clone of the LinkedList containing the server threads.
	 */
	public synchronized LinkedList getAllSvrThreads()
	{
		return (LinkedList)mySvrThreads.clone();
	}

	/**
	* addSvrThreads is called internally when a new client is created.
	* MJM 20020226 - must be synchronized. Other threads may be traversing
	* the list via the iterators. Threads that do so should synchronize
	* on the server object.
	* @param bst the BasicSvrThread to remove.
	*/
	private synchronized void addSvrThread( BasicSvrThread bst )
	{
		mySvrThreads.add(bst);
	}

	/**
	  Shuts down and removes all server threads.
	*/
	public void killAllSvrThreads( )
	{
		while(mySvrThreads.size() > 0)
		{
			BasicSvrThread bst = (BasicSvrThread)mySvrThreads.getFirst();
			bst.disconnect();
			// This should have called rmSvrThread() above. ...But make sure:
			rmSvrThread(bst);
		}
	}

	/**
	* @return number of clients currently connected.
	*/
	public int getNumSvrThreads( )
	{
		return mySvrThreads.size();
	}

	/**
	* @return iterator into list of threads
	*/
	public Iterator getSvrThreads()
	{
		return mySvrThreads.iterator();
	}

	//===============================================================
	// The following methods should be over-ridden by your sub-class:
	//===============================================================


	/**
	* This method is called if you have set a timeout on the
	* ServerSocket object and a timeout occurs. The default
	* implementation does nothing.
	*/
	protected void listenTimeout( )
	{
	}

	
	/**
	* Override this method to create a new thread object to handle
	* interaction with a client. You should sub-class BasicSvrThread
	* and create an instance of your sub-class here.
	* If there is a problem in creating the object and you want
	* to reject the connection return null.
	* If a fatal error occurs and you want to abort the server listen()
	* method, throw IOException.
	* @param sock the socket for the new client connection.
	* @return BasicSvrThread (or concrete subclass) object
	* @throws IOException if error opening IO streams for this socket.
	*/
	protected abstract BasicSvrThread newSvrThread( Socket sock ) throws IOException;

	public int getPort() { return portNum; }

	protected void setModuleName(String nm) { module = nm; }

}

