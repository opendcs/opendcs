/*
 *  Copyright 2023 OpenDCS Consortium
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opendcs.odcsapi.util;

import java.net.*;
import java.io.*;
import java.rmi.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
This class encapsulates common functions for a TCP/IP client.
You can implement a client by subclassing this class or by wrapping an
object of this type.
*/
public class ApiBasicClient
{
	private static final Logger LOGGER = LoggerFactory.getLogger(ApiBasicClient.class);
	/** port to connect to. */
	protected int port;
	/** host to connect to. */
	protected String host;

	/** The socket is opened and I/O streams are created. */
	protected Socket socket;
	/** The input stream */
	protected InputStream input;
	/** The output stream */
	protected OutputStream output;

	/** Allows sub-class to control how often connect attempts are made: */
	protected long lastConnectAttempt;

	/**
	* Construct client for specified port and host. Note, this creates
	* the client object but the connection is not made until the
	* 'connect()' method is called.
	* @param host the host name
	* @param port the port number
	*/
	public ApiBasicClient( String host, int port )
	{
		this.port = port;
		this.host = host;
		socket = null;
		input = null;
		output = null;
		lastConnectAttempt = 0L;
	}

	/**
	* Finalizer makes sure socket and stream resources are freed. If
	* disconnect() was already called, no harm done.
	*/
	protected void finalize( ) throws IOException
	{
		disconnect();
	}

	/**
	* Connect to the previously specified host and port.
	* @throws IOException if can't open socket.
	* @throws UnknownHostException if host cannot be resolved.
	*/
	public void connect( ) throws IOException, UnknownHostException
	{
		if (isConnected())
			disconnect();

		lastConnectAttempt = System.currentTimeMillis();
		LOGGER.debug("Connecting to host '{}', port {}", (host != null ? host : "(unknown)"), port);
		socket = doConnect(host, port);
		input = socket.getInputStream();
		output = socket.getOutputStream();
	}

	/**
	* When several client objects are created in different threads, the
	* connect call can get an interrupted system call (inside the native
	* implementation) and subsequently generate a SocketException. The
	* use of this method prevents this by synchronizing at the class
	* level. Only one socket connection can be made at a time.
	* @param host the host name
	* @param port the port number
	* @return the new Socket
	* @throws IOException if can't open socket.
	* @throws UnknownHostException if host cannot be resolved.
	*/
	private static synchronized Socket doConnect( String host, int port ) throws IOException, UnknownHostException
	{
		Socket ret = new Socket();
		InetSocketAddress iaddr = new InetSocketAddress(host, port);
		if (iaddr.isUnresolved())
			throw new UnknownHostException(host);
		ret.connect(iaddr, 20000);
		return ret;
	}

	/**
	* Disconnect from the server. Close input and output streams and release
	* all socket resources.
	* This function should be called when any of the other methods throws
	* an exception. You can then call connect() again to reconnect to the
	* server.
	*/
	public void disconnect( )
	{
		try
		{
			if (socket != null)
				socket.close();
			if (input != null)
				input.close();
			if (output != null)
				output.close();
			LOGGER.debug("Disconnected form host '{}', port {}", (host != null ? host : "(unknown)"), port);
		}
		catch (IOException ex)
		{
			LOGGER.error("There was an error closing the socket connections.", ex);
		}
		finally
		{
			input = null;
			output = null;
			socket = null;
		}
	}

	/**
	* Sends a byte array of data to the socket and flushes it.
	* @param data the byte array.
	* @throws IOException on IO error
	*/
	public void sendData( byte[] data ) throws IOException
	{
		if (output == null)
			throw new IOException("BasicClient socket closed.");
		output.write(data);
		output.flush();
	}

	/**
	* @return String in the form host:port
	*/
	public String getName( )
	{
		return host + ":" + port;
	}

	/**
	* @return true if currently connected
	*/
	public boolean isConnected( )
	{

		return socket != null;
	}

	/**
	* @return port number
	*/
	public int getPort( ) { return port; }

	/**
	* Set the port number.
	* If currently connected this does nothing until you dis and re connect.
	* @param port the number
	*/
	public void setPort( int port ) { this.port = port; }

	/**
	* @return the host name
	*/
	public String getHost( ) { return host; }

	/**
	* Sets the host name.
	* If currently connected this does nothing until you dis and re connect.
	* @param host
	*/
	public void setHost( String host ) { this.host = host; }

	/**
	* @return Socket for this connection, or null if not connected.
	*/
	public Socket getSocket( ) { return socket; }

	/**
	* @return InputStream for this connection, or null if not connected.
	*/
	public InputStream getInputStream( ) { return input; }

	/**
	* @return OutputStream for this connection, or null if not connected.
	*/
	public OutputStream getOutputStream( ) { return output; }

	/**
	* @return Java msec time when last connect attempt was made.
	*/
	public long getLastConnectAttempt( ) { return lastConnectAttempt; }
}
