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
package lrgs.ldds;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import lrgs.common.*;

/**
  This command sends a network list file back to the client.
  This is an extension to the legacy server. It is not supported by
  the ISI legacy server.

  The request message contains a 64-byte network list file name. The file
  name is relative to this user's home directory.

  The response message (sent back to the client) contains the 64-byte
  network list name followed by the variable length file contents.

  If an error occurs, the filename field will contain a question mark
  followed by two comma-separated integer fields corresponding to
  'derrno' and 'errno'.
*/
public class CmdSendNetList extends LddsCommand
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private String filename;

	/** @return "CmdSendNetList"; */
	public String cmdType()
	{
		return "CmdSendNetList";
	}

	/**
	  Constructor.
	  @param filename the name of the network list that the client wants.
	*/
	public CmdSendNetList(String filename)
	{
		int idx = filename.lastIndexOf('/');
		if (idx == -1)
			idx = filename.lastIndexOf('\\');
		if (idx != -1)
			filename = filename.substring(idx+1);
		this.filename = filename;
	}

	/**
	  Executes the command.
	  @param ldds the server thread object holding connection to client.
	*/
	public int execute(LddsThread ldds) throws IOException,
			ArchiveException, UnknownUserException
	{
		if (ldds.user == null)
			throw new UnknownUserException(
				"Login required prior to get netlist.");
		File nlfile = new File(ldds.user.directory.getCanonicalPath() + File.separator + filename);
		try (FileInputStream fis = new FileInputStream(nlfile))
		{
			byte filedata[] = new byte[(int)nlfile.length()];
			fis.read(filedata);

			byte msgdata[] = new byte[64+(int)nlfile.length()];
			int i;
			for(i = 0; i < 64 && i < filename.length(); i++)
				msgdata[i] = (byte)filename.charAt(i);
			msgdata[i] = (byte)0;

			for(i=0; i<filedata.length; i++)
				msgdata[i+64] = filedata[i];

			LddsMessage msg = new LddsMessage(LddsMessage.IdGetNetlist, "");
			msg.MsgLength = msgdata.length;
			msg.MsgData = msgdata;
			ldds.send(msg);

			log.trace("Successfully retrieved and sent network list file {}", nlfile.getPath());
		}
		catch(IOException ioe)
		{
			throw new NoSuchFileException("Cannot send netlist '" + filename, ioe);
		}
		return 0;
	}

	/** @return the code associated with this command. */
	public char getCommandCode() { return LddsMessage.IdGetNetlist; }
}