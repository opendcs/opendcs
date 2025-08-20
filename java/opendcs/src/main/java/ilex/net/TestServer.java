/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:09  cvs
*  Added legacy code to repository
*
*  Revision 1.2  2004/08/30 14:50:23  mjmaloney
*  Javadocs
*
*  Revision 1.1  2004/03/05 14:17:11  mjmaloney
*  Added TestServer
*
*/

package ilex.net;

import ilex.net.BasicServer;
import ilex.net.BasicSvrThread;
import java.net.*;
import java.util.LinkedList;
import java.io.*;

/**
Stand along test class for BasicServer.
*/
public class TestServer extends BasicServer
{
	/**
	* @param port
	* @throws IllegalArgumentException
	* @throws IOException
	*/
	public TestServer( int port ) throws IllegalArgumentException, IOException
	{
		super(port);
	}

	/**
	* @param sock
	* @return @throws IOException
	*/
	protected BasicSvrThread newSvrThread( Socket sock ) throws IOException
	{
		System.out.println("New Client");
		return new TestServerThread(this, sock);
	}

	/**
	* Usage java ilex.net.TestFileServer port filename
	* @param args
	* @throws IOException
	*/
	public static void main( String[] args ) throws IOException
	{
		int p = Integer.parseInt(args[0]);
		TestServer tfs = new TestServer(p);
		tfs.listen();
	}
}

