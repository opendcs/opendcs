/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:14  cvs
*  Added legacy code to repository
*
*  Revision 1.6  2005/07/20 20:18:56  mjmaloney
*  LRGS 5.0 Release preparation
*
*  Revision 1.5  2005/06/28 17:37:01  mjmaloney
*  Java-Only-Archive implementation.
*
*  Revision 1.4  2005/03/07 21:33:50  mjmaloney
*  dev
*
*  Revision 1.3  2005/01/05 19:21:06  mjmaloney
*  Bug fixes & updates.
*
*  Revision 1.2  2004/08/30 14:51:46  mjmaloney
*  Javadocs
*
*  Revision 1.1  2004/05/04 16:41:26  mjmaloney
*  Added.
*
*/
package lrgs.ldds;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.net.InetAddress;

import ilex.util.Logger;
import ilex.xml.XmlOutputStream;

import lrgs.common.*;
import lrgs.apistatus.AttachedProcess;
import lrgs.apistatus.DownLink;
import lrgs.apistatus.ArchiveStatistics;
import lrgs.apistatus.QualityMeasurement;
import lrgs.statusxml.StatusXmlTags;
import lrgs.statusxml.LrgsStatusSnapshotExt;
import lrgs.statusxml.LrgsStatusSnapshotXio;

/**
This command returns an XML block containing the current LRGS status.
*/
public class CmdGetStatus extends LddsCommand
{
	/**
	  Create a new 'GetStatus' request.
	*/
	public CmdGetStatus()
	{
	}

	/** @return "CmdGetStatus"; */
	public String cmdType()
	{
		return "CmdGetStatus";
	}

	/**
	  Executes the command.
	  Construct status response & send to user.
	  @param ldds the server thread object holding connection to client.
	*/
	public int execute(LddsThread ldds)
		throws ArchiveException, IOException
	{
		if (ldds.user == null)
			throw new UnknownUserException("HELLO required before GetStatus.");

		LrgsStatusProvider sp = ldds.getStatusProvider();
		LrgsStatusSnapshotExt lsse = sp.getStatusSnapshot();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		XmlOutputStream xos = 
			new XmlOutputStream(baos, StatusXmlTags.LrgsStatusSnapshot);

		LrgsStatusSnapshotXio lssxio = new LrgsStatusSnapshotXio(lsse);
		lssxio.writeXml(xos);

		LddsMessage response = new LddsMessage(LddsMessage.IdStatus,
			baos.toString());
		ldds.send(response);
		return 0;
	}

	public String toString()
	{
		return "GetStatus";
	}

	/** @return the code associated with this command. */
	public char getCommandCode() { return LddsMessage.IdStatus; }
}
