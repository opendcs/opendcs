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
package lritdcs.lrit2damsnt;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import ilex.net.BasicSvrThread;

public class DamsNtMsgSvrThread extends BasicSvrThread
{
	/** The socket output stream. */
	private OutputStream outs;

	protected DamsNtMsgSvrThread(DamsNtMsgSvr parent, Socket socket)
		throws IOException
	{
		super(parent, socket);
		outs = socket.getOutputStream();
	}

	@Override
	protected void serviceClient()
	{
		try { sleep(1000L); }
		catch (InterruptedException e) {}
	}

	public void sendToClient(byte[] msg)
		throws IOException
	{
		// Send the data to the client
		try
		{
			outs.write(msg);
			outs.flush();
		}
		catch(Exception ex)
		{
			throw new IOException("Error sending data.", ex);
		}
	}

}
