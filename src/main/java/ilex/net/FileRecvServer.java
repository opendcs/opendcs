/*
*  $Id$
*
*  $Log$
*  Revision 1.2  2013/02/28 16:38:11  mmaloney
*  Adjust timeout.
*
*  Revision 1.1  2012/02/21 14:14:17  mmaloney
*  created
*
*/

package ilex.net;

import ilex.net.BasicServer;
import ilex.net.BasicSvrThread;
import ilex.util.FileUtil;
import ilex.util.Logger;

import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.io.*;

/**
Test implementation of BasicServer that allows clients to connect.
Data sent by the client is saved in a file with name ipaddr-datetime.
*/
public class FileRecvServer extends BasicServer
{
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
		System.out.println("New Client");
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
		Logger.instance().setMinLogPriority(Logger.E_DEBUG3);
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
			try
			{
				FileOutputStream fout = new FileOutputStream(theFile);
				InputStream is = socket.getInputStream();
				FileUtil.copyStream(is, fout, 600000L);
				fout.close();
				disconnect();
			}
			catch (Exception ex)
			{
				disconnect();
				System.err.println(ex.toString());
				ex.printStackTrace(System.err);
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

