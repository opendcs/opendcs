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
*  Revision 1.12  2005/07/20 20:18:57  mjmaloney
*  LRGS 5.0 Release preparation
*
*  Revision 1.11  2005/03/07 21:33:51  mjmaloney
*  dev
*
*  Revision 1.10  2005/01/05 19:21:07  mjmaloney
*  Bug fixes & updates.
*
*  Revision 1.9  2004/08/30 14:51:46  mjmaloney
*  Javadocs
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
*
*/
package lrgs.ldds;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import ilex.util.Logger;

import lrgs.common.*;

/**
  This command receives a network list file from the client.
  This is an extension of the Legacy server. It is not supported by
  the ISI DRS tcpip server.
  The message contains a 64 character filename followed by the network
  list file data. The filename is relative to the user's directory on
  the LRGS.
*/
public class CmdRecvNetList extends LddsCommand
{
	String filename;
	byte nldata[];

	/**
	  Constructor.
	  @param filename the name of the network list
	  @param nldata the data to place into the file
	*/
	public CmdRecvNetList(String filename, byte nldata[])
	{
		int idx = filename.lastIndexOf('/');
		if (idx == -1)
			idx = filename.lastIndexOf('\\');
		if (idx != -1)
			filename = filename.substring(idx+1);
		this.filename = filename;
		this.nldata = nldata;
	}

	/** return "CmdRecvNetList"; */
	public String cmdType()
	{
		return "CmdRecvNetList";
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
				"HELLO required before retrieving GetNetlist.");

		try
		{
			File nlfile = new File(ldds.user.directory.getCanonicalPath()
				+ File.separator + filename);
			writeFile(nlfile);
			LddsMessage msg = new LddsMessage(LddsMessage.IdPutNetlist,
				filename);
			ldds.send(msg);
			Logger.instance().log(Logger.E_DEBUG2,
				"Successfully saved network list file in " + nlfile.getPath());
		}
		catch (IOException ex)
		{
			throw new DdsInternalException("Cannot create netlist: "
				+ex.toString());
		}
		return 0;
	}

	/**
	  Internal method to do the actual write.
	  @param f the file
	*/
	public void writeFile(File f) throws IOException
	{
		FileOutputStream fos = new FileOutputStream(f);
		fos.write(nldata);
		fos.close();
	}

	/** @return the code associated with this command. */
	public char getCommandCode() { return LddsMessage.IdPutNetlist; }
}
