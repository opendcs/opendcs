/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package decodes.tsdb;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.net.BasicServer;
import ilex.net.BasicSvrThread;
import ilex.util.QueueLogger;

/**
 * @deprecated Components are implementing JMX Beans or relying on logger backend configuration
 *             to affect this behavior.
 */
@Deprecated
public class CompEventSvr extends BasicServer
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	QueueLogger eventQueue = null;

	public CompEventSvr(int port)
		throws IOException
	{
		super(port);
	}

	/**
	 * Tees the current logger into a queue. Starts a thread with a listening
	 * socket on the specified port. Accepts connections, and sends events
	 * to clients.
	 */
	public void startup()
	{
		// Current logger is typically a FileLogger


		log.warn("Event system no longer support. Configure logger backend to push to appropriate " +
				 "remote system. Additinoally Some components have had JMX support added.");

		/* do nothing now. behavior is getting replaced with standard systems. */
	}

	@Override
	protected BasicSvrThread newSvrThread(Socket sock) throws IOException
	{
		InetAddress sockaddr = sock.getInetAddress();
		try { return new CompEventSvrThread(this, sock, eventQueue); }
		catch(IOException ex)
		{
			log.atError()
			   .setCause(ex)
			   .log("Error accepting connection from {} (disconnecting). Additionally, you shouldn't see " +
			   		"this message as the component has been disabled.",
			    	sockaddr.toString());
			try { sock.close(); }
			catch(IOException ex2) {}
			return null;
		}
	}
}