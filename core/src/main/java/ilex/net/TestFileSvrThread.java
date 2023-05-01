/*
*  $Id$
*
*  $Log$
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

import ilex.net.BasicSvrThread;
import ilex.net.BasicServer;
import java.net.*;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;

/**
Works with TestFileServer.
Each connected client will be an object of this type.
*/
public class TestFileSvrThread extends BasicSvrThread
{
	/** the file to send */
	File theFile;

	/**
	* Constructor
	* @param parent the parent Server
	* @param socket the socket connection to the client
	* @param filename the file to send
	*/
	public TestFileSvrThread( BasicServer parent, Socket socket, String filename )
	{
		super(parent, socket);
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
			FileInputStream fis = new FileInputStream(theFile);
			byte buf[] = new byte[256];
			OutputStream os = socket.getOutputStream();

			int total = 0;
			int n;
			while((n = fis.read(buf)) > 0)
			{
				os.write(buf, 0, n);
				System.out.println("...wrote " + n + " bytes.");
				total += n;
			}
			System.out.println("...TOTAL " + total + " bytes.");

			fis.close();

			try { sleep(2000L); } catch(InterruptedException ex) {}

			os.flush();
			os.close();

			for(int i=0; i<10; i++)
				serviceClient();
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

