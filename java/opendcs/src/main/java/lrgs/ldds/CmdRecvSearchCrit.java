/*
*  $Id$
*/
package lrgs.ldds;

import java.io.*;

import ilex.util.Logger;

import lrgs.common.*;

/**
  This command receives a search criteria file from the client.
*/
public class CmdRecvSearchCrit extends LddsCommand
{
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
//Logger.instance().info("CmdRecvSearchCrit: num sources = " + 
//ldds.crit.numSources + ", src[0]=" 
//+ (ldds.crit.numSources == 0 ? -1 : ldds.crit.sources[0]));

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
			throw new NoSuchFileException("Cannot save searchcrit: "
				+ex.toString());
		}

		LddsMessage msg = new LddsMessage(LddsMessage.IdCriteria,"Success\0");
		ldds.send(msg);

		Logger.instance().log(Logger.E_DEBUG2,
			"Successfully saved search criteria in " +
			scfile.getPath());
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
			Logger.instance().log(Logger.E_DEBUG3,
				"Saving " + scdata.length + " bytes of data to " + f.getName());
			FileOutputStream fos = new FileOutputStream(f);
			fos.write(scdata);
			fos.close();
		}
	}

	/** @return the code associated with this command. */
	public char getCommandCode() { return LddsMessage.IdCriteria; }
}
