/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package ilex.net;

import ilex.util.Pair;

import java.net.*;
import java.util.LinkedList;
import java.util.Objects;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	public static final Logger log = OpenDcsLoggerFactory.getLogger();
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

	/** The server socket factory so SSL can be injected */
	protected ServerSocketFactory serverSocketFactory;
	/** So that the server can be upgraded to TLS after connection. */
	protected SSLSocketFactory socketFactory;

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
		this(port,bindaddr,Pair.of(ServerSocketFactory.getDefault(),null));
	}

	/**
	* This version of the constructor allows you to specify the inet
	* address for the listening socket. This should be used if your
	* host has more than one network connection and you need to specify
	* which to use.
	* @param port port to listen on
	* @param bindaddr used if you have multiple NICs and only want to listen
	* on one.
	* @param socketFactory used to allow setup of SSL for those servers that need it.
	*/
	public BasicServer( int port, InetAddress bindaddr, Pair<ServerSocketFactory,SSLSocketFactory> socketFactories)
		throws IOException
	{
		Objects.requireNonNull(socketFactories, "Socket Factories MUST non-null with this constructor.");
		if (port < 0)
			throw new IOException(
				"BasicServer: port number must be a positive integer, or zero to get a random port assigned.");
		this.serverSocketFactory = socketFactories.first;
		this.socketFactory = socketFactories.second;
		portNum = port;
		this.bindaddr = bindaddr;
		makeServerSocket();
		mySvrThreads = new LinkedList<>();
		listeningThread = null;
	}

	/**
	* Makes the server (listening) socket.
	* @throws IllegalArgumentException if bad bindaddr or port number.
	* @throws IOException if can't open.
	*/
	protected void makeServerSocket( ) throws IllegalArgumentException, IOException
	{
		listeningSocket = this.serverSocketFactory.createServerSocket(portNum, portNum, bindaddr);
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
				log.info("Socket connection of type: {}", client.getClass().getName());
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

	public int getPort()
	{
		return this.listeningSocket.getLocalPort();
	}

	protected void setModuleName(String nm) { module = nm; }

}
