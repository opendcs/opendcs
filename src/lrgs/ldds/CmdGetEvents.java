/*
*  $Id$
*
*  $Log$
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.2  2009/04/21 13:26:17  mjmaloney
*  dev
*
*  Revision 1.1  2008/04/04 18:21:14  cvs
*  Added legacy code to repository
*
*  Revision 1.6  2005/07/20 20:18:56  mjmaloney
*  LRGS 5.0 Release preparation
*
*  Revision 1.5  2005/03/07 21:33:50  mjmaloney
*  dev
*
*  Revision 1.4  2005/01/05 19:21:06  mjmaloney
*  Bug fixes & updates.
*
*  Revision 1.3  2004/08/31 16:34:55  mjmaloney
*  javadoc
*
*  Revision 1.2  2004/08/30 14:51:45  mjmaloney
*  Javadocs
*
*  Revision 1.1  2004/07/02 18:56:38  mjmaloney
*  Added events distribution capability.
*
*/
package lrgs.ldds;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.net.InetAddress;

import ilex.util.IndexRangeException;
import lrgs.common.*;
import lrgs.lrgsmain.LrgsConfig;

/**
This command returns events that have been generated on the LRGS.
*/
public class CmdGetEvents extends LddsCommand
{
	/**
	  Create a new 'GetEvents' request.
	*/
	public CmdGetEvents()
	{
	}

	/** Returns string 'CmdGetEvents'. */
	public String cmdType()
	{
		return "CmdGetEvents";
	}

	/**
	  Executes the command.
	  @param ldds the server thread object holding connection to client.
	  @return buffer containing recent events.
	*/
	public int execute(LddsThread ldds)
		throws ArchiveException, IOException
	{
		if (ldds.user == null)
			throw new UnknownUserException(
				"HELLO required before GetEvents.");

		StringBuffer sb = new StringBuffer();
		
		if (!LrgsConfig.instance().getMiscBooleanProperty("restrictEventsToAdmin", false)
		 || ldds.user.isAdmin)
		{
			try
			{
				String evt;
				while((evt = ldds.qlog.getMsg(ldds.logIndex)) != null)
				{
					sb.append(evt);
					sb.append('\n');
					ldds.logIndex++;
				}
			}
			catch(IndexRangeException ex)
			{
				int newIdx = ldds.qlog.getNextIdx();
				sb.append("Events Resynchronized: " + ex.getMessage()
					+ " tried=" + ldds.logIndex + ", reset to "
					+ newIdx + "\n");
				ldds.logIndex = newIdx;
			}
		}

		LddsMessage response = 
			new LddsMessage(LddsMessage.IdEvents, sb.toString());
		ldds.send(response);
		return 0;
	}

	/** @return "GetEvents"; */
	public String toString()
	{
		return "GetEvents";
	}

	/** @return the code associated with this command. */
	public char getCommandCode() { return LddsMessage.IdEvents; }
}
