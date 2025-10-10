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
  This command sends a search criteria file back to the client.
*/
public class CmdSendSearchCrit extends LddsCommand
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	/** return "CmdSendSearchCrit"; */
	public String cmdType()
	{
		return "CmdSendSearchCrit";
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
				"Login required before send searchcrit");
		byte scdata[];
		File scfile = new File(ldds.user.directory.getCanonicalPath()
			+ File.separator + "searchcrit");
		try (FileInputStream fis = new FileInputStream(scfile))
		{
			scdata = new byte[50+(int)scfile.length()];
			byte fnbytes[] = SearchCriteria.defaultName.getBytes();
			for(int i=0; i<fnbytes.length; i++)
				scdata[i] = fnbytes[i];

			fis.read(scdata, 50, (int)scfile.length());

			ldds.crit = new SearchCriteria(scfile);
			ldds.msgretriever.setSearchCriteria(ldds.crit);

		}
		catch(IOException ioe)
		{
			throw new NoSuchFileException("Cannot send searchcrit.", ioe);
		}

		LddsMessage msg = new LddsMessage(LddsMessage.IdCriteria, "");

		// Legacy client expects me to return the number of messages
		// that pass the new criteria. I can't do this because of the
		// new architecture. Spoof the client by always returning "1".
		scdata[0] = (byte)'1';
		scdata[0] = (byte)0;
		//msg.MsgLength = 50;    // Don't bother returning the file data.
		msg.MsgLength = scdata.length;
		msg.MsgData = scdata;
		ldds.send(msg);

		log.trace("Successfully retrieved and sent search criteria file {}", scfile.getPath());
		return 0;
	}

	/** @return the code associated with this command. */
	public char getCommandCode() { return LddsMessage.IdCriteria; }
}