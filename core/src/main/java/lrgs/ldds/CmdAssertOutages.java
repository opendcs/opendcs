/*
*  $Id$
*/
package lrgs.ldds;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

import lrgs.common.ArchiveException;
import lrgs.common.LrgsErrorCode;
import lrgs.db.LrgsDatabaseThread;
import lrgs.db.LrgsConstants;
import lrgs.db.Outage;

/**
This class asserts new Outages on the LRGS server.
*/
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
			ldds.warning(msg);
			throw new 
				LddsRequestException(msg, LrgsErrorCode.DPARSEERROR, false);
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
