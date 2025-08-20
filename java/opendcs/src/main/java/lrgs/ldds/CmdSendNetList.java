/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:14  cvs
*  Added legacy code to repository
*
*  Revision 1.13  2005/07/20 20:18:57  mjmaloney
*  LRGS 5.0 Release preparation
*
*  Revision 1.12  2005/03/07 21:33:51  mjmaloney
*  dev
*
*  Revision 1.11  2005/01/05 19:21:07  mjmaloney
*  Bug fixes & updates.
*
*  Revision 1.10  2004/08/30 14:51:47  mjmaloney
*  Javadocs
*
*  Revision 1.9  2004/04/03 17:11:37  mjmaloney
*  Ref List Editor Development
*
*  Revision 1.8  2003/05/29 14:27:07  mjmaloney
*  Enhancements for 3.4
*
*  Revision 1.7  2003/05/03 12:33:32  mjmaloney
*  Updates
*
*  Revision 1.6  2002/06/19 19:25:17  mjmaloney
*  Release preparation.
*
*  Revision 1.5  2002/06/18 19:19:21  mjmaloney
*  Use Logger class rather than LrgsEventQueue directly.
*
*  Revision 1.4  2000/03/31 16:09:57  mike
*  Error codes are now in lrgs.common.LrgsErrorCode
*
*  Revision 1.3  2000/01/08 21:53:28  mike
*  generic LddsClient interface
*
*  Revision 1.2  1999/11/19 15:56:00  mike
*  Modifications made for compatibility with legacy DRS Server
*
*  Revision 1.1  1999/11/11 16:23:35  mike
*  Initial implementation
*
*/
package lrgs.ldds;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import ilex.util.Logger;

import lrgs.common.*;

/**
  This command sends a network list file back to the client.
  This is an extension to the legacy server. It is not supported by
  the ISI legacy server.

  The request message contains a 64-byte network list file name. The file
  name is relative to this user's home directory.

  The response message (sent back to the client) contains the 64-byte 
  network list name followed by the variable length file contents.

  If an error occurs, the filename field will contain a question mark
  followed by two comma-separated integer fields corresponding to 
  'derrno' and 'errno'.
*/
public class CmdSendNetList extends LddsCommand
{
	private String filename;

	/** @return "CmdSendNetList"; */
	public String cmdType()
	{
		return "CmdSendNetList";
	}

	/**
	  Constructor.
	  @param filename the name of the network list that the client wants.
	*/
	public CmdSendNetList(String filename)
	{
		int idx = filename.lastIndexOf('/');
		if (idx == -1)
			idx = filename.lastIndexOf('\\');
		if (idx != -1)
			filename = filename.substring(idx+1);
		this.filename = filename;
	}

	/**
	  Executes the command.
	  @param ldds the server thread object holding connection to client.
	*/
	public int execute(LddsThread ldds) throws IOException,
			ArchiveException, UnknownUserException
	{
		if (ldds.user == null)
			throw new UnknownUserException(
				"Login required prior to get netlist.");
		try
		{
			File nlfile = new File(ldds.user.directory.getCanonicalPath()
				+ File.separator + filename);
			FileInputStream fis = new FileInputStream(nlfile);
			byte filedata[] = new byte[(int)nlfile.length()];
			fis.read(filedata);
			fis.close();

			byte msgdata[] = new byte[64+(int)nlfile.length()];
			int i;
			for(i = 0; i < 64 && i < filename.length(); i++)
				msgdata[i] = (byte)filename.charAt(i);
			msgdata[i] = (byte)0;

			for(i=0; i<filedata.length; i++)
				msgdata[i+64] = filedata[i];

			LddsMessage msg = new LddsMessage(LddsMessage.IdGetNetlist, "");
			msg.MsgLength = msgdata.length;
			msg.MsgData = msgdata;
			ldds.send(msg);

			Logger.instance().log(Logger.E_DEBUG2,
				"Successfully retrieved and sent network list file " +
				nlfile.getPath());
		}
		catch(IOException ioe)
		{
			throw new NoSuchFileException("Cannot send netlist '"
				+ filename + "': " + ioe.toString());
		}
		return 0;
	}

	/** @return the code associated with this command. */
	public char getCommandCode() { return LddsMessage.IdGetNetlist; }
}
