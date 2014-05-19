/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*/
package lrgs.noaaportrecv;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import lrgs.lrgsmain.LrgsConfig;
import ilex.util.Logger;
import ilex.net.BasicSvrThread;
import ilex.net.BasicServer;

/**
Handles the parsing of messages from the NOAAPORT socket.
*/
public class NoaaportSvrThread
	extends BasicSvrThread
	implements NoaaportConnection
{
	private NoaaportProtocol protocolHandler = null;

	NoaaportSvrThread(BasicServer parent, Socket socket, 
		NoaaportRecv noaaportRecv)
		throws IOException
	{
		super(parent, socket);
		try { socket.setSoTimeout(0); }
		catch(Exception ex)
		{
			Logger.instance().warning("BasicSvrThread Cannot set read timeout to 0.");
		}


		// PDI NOAAPORT interface requires special protocol handler
		String rcvType = LrgsConfig.instance().noaaportReceiverType;
		if (rcvType.toLowerCase().contains("pdi"))
			protocolHandler = new PdiNoaaportProtocol(socket.getInputStream(),
				noaaportRecv, this, getClientName());
		else
			protocolHandler = new NoaaportProtocol(socket.getInputStream(),
				noaaportRecv, this, getClientName());
		
		protocolHandler.info("New connection from "
			+ getClientName() + ", receiver type=" + rcvType);
	}

	/**
	 * Repeatedly called from base-class until connection is broken.
	 */
	protected void serviceClient()
	{
		protocolHandler.read();
	}
	
	public void disconnect( )
	{
		protocolHandler.info("Disconnecting from " + getClientName());
		super.disconnect();
	}
}
