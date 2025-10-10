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
import java.io.FileOutputStream;
import java.io.IOException;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import lrgs.common.*;

/**
  This command receives a network list file from the client.
  This is an extension of the Legacy server. It is not supported by
  the ISI DRS tcpip server.
  The message contains a 64 character filename followed by the network
  list file data. The filename is relative to the user's directory on
  the LRGS.
*/
public class CmdRecvNetList extends LddsCommand
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	String filename;
	byte nldata[];

	/**
	  Constructor.
	  @param filename the name of the network list
	  @param nldata the data to place into the file
	*/
	public CmdRecvNetList(String filename, byte nldata[])
	{
		int idx = filename.lastIndexOf('/');
		if (idx != -1)
			filename = filename.substring(idx+1);
		idx = filename.lastIndexOf('\\');
		if (idx != -1)
			filename = filename.substring(idx+1);

		this.filename = filename;
		this.nldata = nldata;
	}

	/** return "CmdRecvNetList"; */
	public String cmdType()
	{
		return "CmdRecvNetList";
	}

	/**
	  Executes the command.
	  @param ldds the server thread object holding connection to client.
	*/
	public int execute(LddsThread ldds)
		throws ArchiveException, IOException
	{
		if (ldds.user == null)
			throw new UnknownUserException(
				"HELLO required before retrieving GetNetlist.");

		try
		{
			File nlfile = new File(ldds.user.directory.getCanonicalPath()
				+ File.separator + filename);
			writeFile(nlfile);
			LddsMessage msg = new LddsMessage(LddsMessage.IdPutNetlist,
				filename);
			ldds.send(msg);
			log.trace("Successfully saved network list file in {}", nlfile.getPath());
		}
		catch (IOException ex)
		{
			throw new DdsInternalException("Cannot create netlist.", ex);
		}
		return 0;
	}

	/**
	  Internal method to do the actual write.
	  @param f the file
	*/
	public void writeFile(File f) throws IOException
	{
		try (FileOutputStream fos = new FileOutputStream(f))
		{
			fos.write(nldata);
		}
	}

	/** @return the code associated with this command. */
	public char getCommandCode() { return LddsMessage.IdPutNetlist; }
}
