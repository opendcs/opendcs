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

import java.io.IOException;


/**
  This command simply echos back to the client
*/
public class CmdEcho extends LddsCommand
{
	LddsMessage msg;
	char cmdCode;

	/**
	  Constructor.
	  @param msg the complete message from the client.
	*/
	public CmdEcho(LddsMessage msg, char cmdCode)
	{
		this.msg = msg;
	}

	/**
	  Executes the command.
	  @param ldds the server thread object holding connection to client.
	*/
	public int execute(LddsThread ldds) throws IOException
	{
		ldds.send(msg);
		return 0;
	}

	/** @return "CmdEcho"; */
	public String cmdType()
	{
		return "CmdEcho";
	}

	/** @return the code associated with this command. */
	public char getCommandCode() { return cmdCode; }
}
