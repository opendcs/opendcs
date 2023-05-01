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

import ilex.net.BasicSvrThread;
import ilex.net.BasicServer;
import java.net.*;
import java.io.*;

/**
Works with TestServer
*/
public class TestServerThread extends BasicSvrThread
{
	BufferedReader reader;

	/**
	* @param parent
	* @param socket
	* @throws IOException
	*/
	public TestServerThread( BasicServer parent, Socket socket ) throws IOException
	{
		super(parent, socket);
		reader = new BufferedReader(new InputStreamReader(
			socket.getInputStream()));
	}

	public void serviceClient( )
	{
		try
		{
			String line = reader.readLine();
			if (line == null)
			{
				System.out.println("hangup");
				disconnect();
			}
			else
				System.out.println(line);
		}
		catch(IOException ex) 
		{
			System.out.println("hangup: " + ex);
			disconnect();
		}
	}
}

