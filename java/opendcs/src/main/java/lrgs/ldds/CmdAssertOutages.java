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
import java.util.ArrayList;

import lrgs.common.ArchiveException;
import lrgs.common.LrgsErrorCode;
import lrgs.db.LrgsDatabaseThread;
import lrgs.db.LrgsConstants;
import lrgs.db.Outage;

/**
This class asserts new Outages on the LRGS server.
@deprecated outage mechanism no longer supported
*/
@Deprecated
public class CmdAssertOutages extends CmdAdminCmd
{
	byte data[];

	/**
	  Create a new 'Assert Outages' request.
	  The request body contains an XML block of new outages.
	  @param data body of the request from client
	*/
	public CmdAssertOutages(byte data[])
	{
		this.data = data;
	}

	/** @return "CmdAssertOutages"; */
	public String cmdType()
	{
		return "CmdAssertOutages";
	}

	/**
	  Executes the Outage command and returns the result.
	  Called from base class execute() method after checking admin priviledge.
	  @param ldds the server thread object
	*/
	public int executeAdmin(LddsThread ldds)
		throws ArchiveException, IOException
	{
		ArrayList<Outage> outages;
		try { outages = ldds.getOutageXmlParser().parse(data); }
		catch(ProtocolError ex)
		{
			String msg = "Cannot parse outages: " + ex;
			throw new LddsRequestException(msg, LrgsErrorCode.DPARSEERROR, false, ex);
		}

		LrgsDatabaseThread ldt = LrgsDatabaseThread.instance();
		int n = 0;
		for(Outage outage : outages)
		{
			int id = outage.getOutageId();
			Outage oldOut = ldt.getOutageById(id);
			if (oldOut != null)
				ldt.deleteOutage(oldOut);
			if (outage.getStatusCode() != LrgsConstants.outageStatusDeleted)
			{
				outage.setInDb(true);
				ldt.assertOutage(outage);
			}
			n++;
		}
		LddsMessage response = new LddsMessage(LddsMessage.IdAssertOutages, 
			"" + n + " outages processed.");
		ldds.send(response);
		return 0;
	}

	/** @return a string representation for log messages. */
	public String toString()
	{
		return "Assert Outages";
	}

	/** @return the code associated with this command. */
	public char getCommandCode() { return LddsMessage.IdAssertOutages; }
}