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

import java.io.*;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import lrgs.common.*;

/**
  This command receives a search criteria file from the client.
*/
public class CmdRecvSearchCrit extends LddsCommand
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	byte scdata[];

	/** No arguments constructor - empties search criteria - all msgs pass */
	public CmdRecvSearchCrit()
	{
		scdata = null;
	}

	/**
	  constructor.
	  @param scdata the search-criteria data received from the client.
	*/
	public CmdRecvSearchCrit(byte scdata[])
	{
		this.scdata = scdata;
	}

	/** @return "CmdRecvSearchCrit"; */
	public String cmdType()
	{
		return "CmdRecvSearchCrit";
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
				"HELLO required before this operation.");

		ldds.seqNumMsgBuf = null;

		File scfile = new File(ldds.user.directory.getCanonicalPath()
			+ File.separator + "searchcrit");
		try
		{
			writeFile(scfile);

			ldds.crit = new SearchCriteria();
			if (scdata != null)
			{
				Reader reader =
					new InputStreamReader(new ByteArrayInputStream(scdata));
				ldds.crit.parseFile(reader);
			}
			ldds.msgretriever.setSearchCriteria(ldds.crit);

			if (ldds.user.dcpLimit > 0)
			{
				String lus = ldds.crit.getLrgsUntil();
				String dus = ldds.crit.getDapsUntil();
				if ((lus == null || lus.trim().length() == 0)
				 && (dus == null || dus.trim().length() == 0))
				{
					int all = ldds.msgretriever.getAggregateListLength();
					if (all == 0 || all > ldds.user.dcpLimit)
					{
						throw new LddsRequestException(
							"User '" + ldds.getUserName()
							+ "' is limited to " + ldds.user.dcpLimit
							+ " DCPs for a real-time stream. You have " + all,
							LrgsErrorCode.DTOOMANYDCPS, true);
					}
				}
			}
		}
		catch (IOException ex)
		{
			throw new NoSuchFileException("Cannot save searchcrit: ", ex);
		}

		LddsMessage msg = new LddsMessage(LddsMessage.IdCriteria,"Success\0");
		ldds.send(msg);

		log.trace("Successfully saved search criteria in {}", scfile.getPath());
		return 0;
	}

	/**
	  Internal utility method to write the file.
	  @param f the file
	*/
	public void writeFile(File f) throws IOException
	{
		if (scdata == null)
		{
			f.delete();
		}
		else
		{
			log.trace("Saving {} bytes of data to '{}'", scdata.length,  f.getName());
			try (FileOutputStream fos = new FileOutputStream(f))
			{
				fos.write(scdata);
			}
		}
	}

	/** @return the code associated with this command. */
	public char getCommandCode() { return LddsMessage.IdCriteria; }
}