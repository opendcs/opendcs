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
*  Revision 1.1  2003/09/18 14:24:53  mjmaloney
*  Created Usbr test server to test the USBR client in the lrgs.lddc package.
*
*/
package ilex.net;

import ilex.net.BasicSvrThread;
import ilex.net.BasicServer;
import java.net.*;
import java.io.*;

public class TestUsbrSvrThread extends BasicSvrThread
{
	/**
	* @param parent
	* @param socket
	*/
	public TestUsbrSvrThread( BasicServer parent, Socket socket )
	{
		super(parent, socket);
	}

	public void run( )
	{
		try
		{
			final OutputStream os = socket.getOutputStream();
			Thread readyThread = new Thread()
			{
				public void run()
				{
					byte ready = 0;
					try
					{
						while(true)
						{
							os.write((int)ready);
							try { sleep(100L); }
							catch(InterruptedException ex) {}
						}
					}
					catch(IOException ex)
					{}
				}
			};
			readyThread.start();

			byte buf[] = new byte[256];
			BufferedInputStream is = new BufferedInputStream(
				socket.getInputStream());

			while(true)
			{
				int n = is.read(buf, 0, 256);
				if (n > 0)
				{
					System.out.print(new String(buf, 0, n));
					System.out.flush();
				}
				else
				{
					try { Thread.sleep(1000L); }
					catch(InterruptedException ex) {}
				}
			}
		}
		catch (Exception ex)
		{
			disconnect();
			System.err.println(ex.toString());
			ex.printStackTrace(System.err);
		}
	}

	public void serviceClient( )
	{
		try{ sleep(1000L); }
		catch(InterruptedException e) {}
	}
}

