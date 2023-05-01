/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:16  cvs
*  Added legacy code to repository
*
*  Revision 1.4  2005/12/30 19:41:00  mmaloney
*  dev
*
*  Revision 1.3  2004/05/18 22:52:41  mjmaloney
*  dev
*
*  Revision 1.2  2004/05/05 19:52:46  mjmaloney
*  Integrated UIServer & UISvrThread to LritDcsMain
*
*  Revision 1.1  2004/05/05 18:48:18  mjmaloney
*  Added UIServer & UISvrThread
*
*/
package lritdcs;

import java.util.StringTokenizer;
import java.net.*;
import java.io.IOException;

import ilex.net.*;
import ilex.util.Logger;

public class UIServer extends BasicServer
{
	public UIServer(int port)
		throws IOException
	{
		super(port);
	}

	protected BasicSvrThread newSvrThread(Socket sock)
	{
		// verify that inet address of this client is authorized.
		InetAddress sockaddr = sock.getInetAddress();
		if (isOK(sockaddr))
		{
			try { return new UISvrThread(this, sock); }
			catch(IOException ex)
			{
				Logger.instance().warning("LRIT:" + Constants.EVT_UI_LISTEN_ERR
					+ "- Error accepting connection from " + sockaddr.toString()
					+ " (disconnecting): " + ex);
			}
		}
		Logger.instance().warning("LRIT:" + Constants.EVT_UI_INVALID_HOST
			+ "- Rejecting UI connection from " + sockaddr.toString());
		try { sock.close(); }
		catch(IOException ex) {}
		return null;
	}

	private boolean isOK(InetAddress sockaddr)
	{
		Logger.instance().log(Logger.E_INFORMATION,
			"User Interface Socket connection from " + sockaddr.toString());
		// Localhost is always OK.
		InetAddress testaddr=null;
		try
		{
			testaddr = InetAddress.getByName("127.0.0.1");
			if (sockaddr.equals(testaddr))
				return true;
		}
		catch(UnknownHostException ex)
		{
			Logger.instance().log(Logger.E_WARNING,
				"Cannot resolve localhost: " + ex);
		}

		// Check each addr/name in the configuration.
		String ailist = LritDcsConfig.instance().getUIIPAddresses();
		if (ailist == null)
			return false;
		StringTokenizer st = new StringTokenizer(ailist);
		while(st.hasMoreTokens())
		{
			String s = st.nextToken();
			try { testaddr = InetAddress.getByName(s); }
			catch(UnknownHostException ex) { continue; }

			if (sockaddr.equals(testaddr))
				return true;
		}
		return false;
	}
}
