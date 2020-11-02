/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:09  cvs
*  Added legacy code to repository
*
*  Revision 1.3  2007/06/06 16:26:58  mmaloney
*  dev
*
*  Revision 1.2  2004/08/30 14:50:23  mjmaloney
*  Javadocs
*
*  Revision 1.1  2003/09/18 14:24:53  mjmaloney
*  Created Usbr test server to test the USBR client in the lrgs.lddc package.
*
*/

package ilex.net;

import ilex.net.BasicServer;
import ilex.net.BasicSvrThread;
import java.net.*;
import java.util.LinkedList;
import java.io.*;

/** Tester for the USBR Server */
public class TestUsbrServer extends BasicServer
{
	/**
	* @param port
	* @throws IllegalArgumentException
	* @throws IOException
	*/
	public TestUsbrServer( int port ) throws IllegalArgumentException, IOException
	{
		super(port);
	}

	/**
	* @param sock
	* @return @throws IOException
	*/
	protected BasicSvrThread newSvrThread( Socket sock ) throws IOException
	{
//		System.out.println("New Client");
		return new TestUsbrSvrThread(this, sock);
	}

	/// Usage java ilex.net.TestUsbrServer port filename
	/**
	* @param args
	* @throws IOException
	*/
	public static void main( String[] args ) throws IOException
	{
		int p = Integer.parseInt(args[0]);
		TestUsbrServer tfs = new TestUsbrServer(p);
		tfs.listen();
	}
}

