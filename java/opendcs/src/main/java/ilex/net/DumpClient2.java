/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:09  cvs
*  Added legacy code to repository
*
*  Revision 1.1  2007/11/20 16:32:47  mmaloney
*  dev
*
*  Revision 1.3  2004/08/30 14:50:22  mjmaloney
*  Javadocs
*
*  Revision 1.2  2003/06/19 18:57:48  mjmaloney
*  Fixed bugs in DumpClient & added debug msgs to TestFileServer
*  ServerLock can now be used by clients to monitor status of running server.
*
*  Revision 1.1  2002/12/08 20:21:30  mjmaloney
*  Created.
*
*/
package ilex.net;

import java.net.*;
import java.io.*;

/**
This is a utility main client. It connects to a given host:port and
reads data, then writes it to standard output.
*/
public class DumpClient2
{
	/**
	* Main method.
	* @param args args[0]==host, args[1]==port
	* @throws IOException
	*/
	public static void main( String[] args ) throws IOException
	{
		String host = args[0];
		int port = Integer.parseInt(args[1]);
		
		Socket sock = new Socket(host, port);
		InputStream input = sock.getInputStream();
		int total = 0;
		int c;
		while(true)
		{
			if (input.available() > 0)
			{
				c = input.read();
				total++;
				System.out.write((byte)c);
				System.out.flush();
			}
			else
			{
				try { Thread.sleep(100L); } catch(InterruptedException ex) {}
			}
		}
	}
}

