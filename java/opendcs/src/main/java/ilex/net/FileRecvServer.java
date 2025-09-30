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

import ilex.util.FileUtil;

import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.io.*;

/**
Test implementation of BasicServer that allows clients to connect.
Data sent by the client is saved in a file with name ipaddr-datetime.
*/
public class FileRecvServer extends BasicServer
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	/**
	* Constructor
	* @param port the port number
	* @param filename the file to send
	*/
	public FileRecvServer( int port)
			throws IllegalArgumentException, IOException
	{
		super(port);
	}

	/**
	* Returns new TestFileSvrThread object
	* @param sock the socket to the remote client
	* @return new TestFileSvrThread object
	*/
	protected BasicSvrThread newSvrThread( Socket sock ) throws IOException
	{
		log.info("New Client");
		return new FileRecvSvrThread(this, sock);
	}

	/**
	* Usage java ilex.net.FileRecvServer port
	* @param args port number on first argument
	* @throws IOException
	*/
	public static void main( String[] args )
			throws IOException
	{
		int p = Integer.parseInt(args[0]);
		FileRecvServer tfs = new FileRecvServer(p);
		tfs.listen();
	}


	//========================================================================
	// The server thread class
	//========================================================================
	class FileRecvSvrThread extends BasicSvrThread
	{
		/** the file to send */
		File theFile;

		/**
		* Constructor
		* @param parent the parent Server
		* @param socket the socket connection to the client
		*/
		public FileRecvSvrThread( BasicServer parent, Socket socket)
		{
			super(parent, socket);
			InetAddress ia = socket.getInetAddress();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
			sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
			String filename = ia.getHostAddress() + "-" + sdf.format(new Date());
			theFile = new File(filename);
		}

		/**
		  We DO override the run method here.
		  This method simply spits the file out over the socket in 256 byte
		  chunks, then hangs up.
		*/
		public void run( )
		{
			try (FileOutputStream fout = new FileOutputStream(theFile);
				 InputStream is = socket.getInputStream();)
			{
				FileUtil.copyStream(is, fout, 600000L);
				disconnect();
			}
			catch (Exception ex)
			{
				log.atError().setCause(ex).log("Unable to copy stream.");
				disconnect();
			}
		}

		/** Not used. */
		public void serviceClient( )
		{
			try{ sleep(1000L); }
			catch(InterruptedException e) {}
		}
	}

}
