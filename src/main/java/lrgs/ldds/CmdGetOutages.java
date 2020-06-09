/*
*  $Id$
*/
package lrgs.ldds;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.zip.GZIPOutputStream;
import java.text.SimpleDateFormat;

import ilex.util.Logger;
import lrgs.common.ArchiveException;
import lrgs.common.LrgsErrorCode;
import lrgs.db.DataSource;
import lrgs.db.LrgsDatabaseThread;
import lrgs.db.Outage;

/**
This class handles the 'Outage' command on the LRGS server.
*/
public class CmdGetOutages extends CmdAdminCmd
{
	String args = "";
	public static final String dateSpec = "yyyy/DDD-HH:mm:ss";
	private SimpleDateFormat sdf;

	/**
	  Create a new 'Get Outages' request with the specified user name.
	  Supported arguments: [start-date/time [end-date/time]]
	  - If both times missing, return all outages
	  - If only start provided, return all outages since that time.
	  - If both provided, return the range.
	  @param data body of the request from client, containing arguments as
	  described above.
	*/
	public CmdGetOutages(byte data[])
	{
		sdf = new SimpleDateFormat(dateSpec);
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		args = (data == null || data.length == 0) ? ""
			: (new String(data, 0, 
			(data[data.length-1] == (byte)0) ? data.length-1 : data.length));
	}

	/** @return "CmdGetOutages"; */
	public String cmdType()
	{
		return "CmdGetOutages";
	}

	/**
	  Executes the Outage command and returns the result.
	  Called from base class execute() method after checking admin priviledge.
	  @param ldds the server thread object
	*/
	public int executeAdmin(LddsThread ldds)
		throws ArchiveException, IOException
	{
Logger.instance().info("executing GetOutages " + args);
		StringTokenizer st = new StringTokenizer(args);

		Date start = null;
		if (st.hasMoreTokens())
		{
			String t = st.nextToken();
			try { start = sdf.parse(t); }
			catch(Exception ex)
			{
				String msg = "Bad outage request start time '" + t 
					+ "': Required format is '" + dateSpec + "'" + ex;
System.err.println(msg);
ex.printStackTrace();
				ldds.warning(msg);
				throw new 
					LddsRequestException(msg, LrgsErrorCode.DBADSINCE, false);
			}
		}

		Date end = null;
		if (st.hasMoreTokens())
		{
			String t = st.nextToken();
			try { end = sdf.parse(t); }
			catch(Exception ex)
			{
				String msg = "Bad outage request end time '" + t 
					+ "': Required format is '" + dateSpec + "'" + ex;
System.err.println(msg);
ex.printStackTrace();
				ldds.warning(msg);
				throw new 
					LddsRequestException(msg, LrgsErrorCode.DBADUNTIL, false);
			}
		}

		// Retrieve the outages
		ArrayList<Outage> outages = new ArrayList<Outage>();
		LrgsDatabaseThread ldt = LrgsDatabaseThread.instance();
		ldt.getOutages(outages, start, end);
		Collections.sort(outages);
		for(Outage otg : outages)
		{
			int id = otg.getSourceId();
			DataSource ds = ldt.getDataSource(id);
			if (ds != null)
				otg.setDataSourceName(ds.getDataSourceName());
		}
Logger.instance().info("Retrieved " + outages.size() + " outages.");

		// Use OutageXmlParser to format them in a response & return
		byte xmldata[] = ldds.getOutageXmlParser().outages2xml(outages);

		// GZip the data before returning to client.
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		GZIPOutputStream gzos = new GZIPOutputStream(baos);
		gzos.write(xmldata);
		gzos.finish();
		try { gzos.close(); } catch(Exception ex) {}
		LddsMessage response = new LddsMessage(LddsMessage.IdGetOutages, "");
		response.MsgData = baos.toByteArray();
		response.MsgLength = response.MsgData.length;
		ldds.send(response);
		return 0;
	}

	/** @return a string representation for log messages. */
	public String toString()
	{
		return "Get Outages";
	}

	/** @return the code associated with this command. */
	public char getCommandCode() { return LddsMessage.IdGetOutages; }
}
