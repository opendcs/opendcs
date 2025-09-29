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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.net.BasicSvrThread;
import ilex.util.IndexRangeException;
import ilex.util.QueueLogger;

public class CompEventSvrThread extends BasicSvrThread
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	BufferedWriter output;
	int eventIndex;
	QueueLogger eventQueue;

	CompEventSvrThread(CompEventSvr parent, Socket socket, QueueLogger eventQueue)
		throws IOException
	{
		super(parent, socket);
		this.eventQueue = eventQueue;
		output = new BufferedWriter(
				new OutputStreamWriter(socket.getOutputStream()));
		eventIndex = eventQueue.getNextIdx() - 10;
		if (eventIndex < 0)
			eventIndex = 0;
	}

	@Override
	protected void serviceClient()
	{
		try { sendQueuedEvents(); }
		catch(IndexRangeException ex)
		{
			eventIndex = eventQueue.getStartIdx();
		}
		catch(IOException ex)
		{
			log.atError()
			   .setCause(ex)
			   .log("IO Error on User Interface Socket from {} -- disconnecting", getClientName());
			disconnect();
			return;
		}
		try { sleep(500L); }
		catch(InterruptedException ex) {}
	}

	private void sendQueuedEvents()
		throws IndexRangeException, IOException
	{
		String msg;
		while( (msg = eventQueue.getMsg(eventIndex)) != null)
		{
			output.write(msg);
			output.newLine();
			eventIndex++;
		}
		output.flush();
	}
}