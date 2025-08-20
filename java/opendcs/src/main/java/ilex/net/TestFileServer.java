/*
*  $Id$
*
*  $Log$
*  Revision 1.2  2010/02/01 20:02:28  mjmaloney
*  added debugs
*
*  Revision 1.1  2008/04/04 18:21:09  cvs
*  Added legacy code to repository
*
*  Revision 1.3  2004/08/30 14:50:22  mjmaloney
*  Javadocs
*
*  Revision 1.2  2003/06/19 18:57:48  mjmaloney
*  Fixed bugs in DumpClient & added debug msgs to TestFileServer
*  ServerLock can now be used by clients to monitor status of running server.
*
*  Revision 1.1  2002/10/11 01:27:31  mjmaloney
*  Created TestFileServer
*
*/

package ilex.net;

import ilex.net.BasicServer;
import ilex.net.BasicSvrThread;
import ilex.util.Logger;

import java.net.*;
import java.util.LinkedList;
import java.io.*;

/**
Test implementation of BasicServer that allows clients to connect. Each
client gets sent a copy of the specified file.
*/
public class TestFileServer extends BasicServer
{
	/** The file to send to each client. */
	String filename;

	/**
	* Constructor
	* @param port the port number
	* @param filename the file to send
	*/
	public TestFileServer( int port, String filename ) throws IllegalArgumentException, IOException
	{
		super(port);
		this.filename = filename;
	}

	/**
	* Returns new TestFileSvrThread object
	* @param sock the socket to the remote client
	* @return new TestFileSvrThread object
	*/
	protected BasicSvrThread newSvrThread( Socket sock ) throws IOException
	{
		System.out.println("New Client");
		return new TestFileSvrThread(this, sock, filename);
	}

	/**
	* Usage java ilex.net.TestFileServer port filename
	* @param args args[0]==portnum, args[1]=filename
	* @throws IOException
	*/
	public static void main( String[] args ) throws IOException
	{
		Logger.instance().setMinLogPriority(Logger.E_DEBUG3);
		int p = Integer.parseInt(args[0]);
		TestFileServer tfs = new TestFileServer(p, args[1]);
		tfs.listen();
	}
}

