package lrgs.ldds;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import ilex.util.Logger;

import lrgs.common.*;

/**
  This command sends a search criteria file back to the client.
*/
public class CmdSendSearchCrit extends LddsCommand
{
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
		try
		{
			FileInputStream fis = new FileInputStream(scfile);

			scdata = new byte[50+(int)scfile.length()];
			byte fnbytes[] = SearchCriteria.defaultName.getBytes();
			for(int i=0; i<fnbytes.length; i++)
				scdata[i] = fnbytes[i];

			fis.read(scdata, 50, (int)scfile.length());
			fis.close();

			// Activate this search crit file. Ignore exceptions (perhaps
			// from network lists not being found.
// MJM 20050306 -- why do we want to ignore exceptions -- let them propegate.
			//try
			//{
			ldds.crit = new SearchCriteria(scfile);
			ldds.msgretriever.setSearchCriteria(ldds.crit);
			//}
			//catch(Exception e) {}
		}
		catch(IOException ioe)
		{
			throw new NoSuchFileException("Cannot send searchcrit: "
				+ ioe.toString());
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

		Logger.instance().log(Logger.E_DEBUG2,
			"Successfully retrieved and sent search criteria file " +
			scfile.getPath());
		return 0;
	}

	/** @return the code associated with this command. */
	public char getCommandCode() { return LddsMessage.IdCriteria; }
}
