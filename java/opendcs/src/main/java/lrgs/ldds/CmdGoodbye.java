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

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

/**
This command signals the end of the session.
*/
public class CmdGoodbye extends LddsCommand
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	/**
	  Executes the command.
	  The server will hangup.
	  @param ldds the server thread object holding connection to client.
	*/
	public int execute(LddsThread ldds)
	{
		log.debug("Executing GOODBYE for {}", ldds.getClientName());

		try
		{
			LddsMessage msg = new LddsMessage(LddsMessage.IdGoodbye, "");
			ldds.send(msg);
			Thread.sleep(100);
		}
		catch (Exception ex)
		{
		}
		ldds.disconnect();
		return 0;
	}

	public String cmdType()
	{
		return "CmdGoodbye";
	}

	/** @return the code associated with this command. */
	public char getCommandCode() { return LddsMessage.IdGoodbye; }
}
