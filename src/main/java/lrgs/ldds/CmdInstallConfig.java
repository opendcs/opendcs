/*
*  $Id$
*/
package lrgs.ldds;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import ilex.util.ArrayUtil;
import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.util.TextUtil;

import lrgs.common.ArchiveException;
import lrgs.common.LrgsErrorCode;
import lrgs.lrgsmain.LrgsConfig;
import lrgs.networkdcp.NetworkDcpRecv;;

/**
This command installs a configuration file on the server
The request message contains a 64-byte field denoting the configuration
file to be returned:
<ul>
  <li>lrgs - install main configuration file (usually lrgs.conf)</li>
  <li>ddsrecv - install DDS Recv Config file (usually ddsrecv.conf)</li>
  <li>drgs - install DRGS Recv Config file (usually drgsconf.xml)</li>
  <li>networkDcp - return Network DCP Config file (network-dcp.conf)</li>
  <li>netlist:name - install network list.</li>
  <li>netlist-delete:name - delete network list.</li>
</ul>
After the 64-byte field, the remainder of the message is the file contents.
This file just saves the file data. The LRGS modules know to check for
config changes and load them synchronously with their other operations.
<p>
The response message (sent back to the client) contains the same 64-byte 
field without the file-contents body.
<p>
If an error occurs, the 64-byte field will contain a question mark
followed by two comma-separated integer fields corresponding to 
'derrno' and 'errno'.
*/
public class CmdInstallConfig extends CmdAdminCmd
{
	private String cfgType;
	private byte[] filedata;

	/** @return "CmdInstallConfig"; */
	public String cmdType()
	{
		return "CmdInstallConfig";
	}

	/**
	  Constructor.
	  @param data configuration name and file contents
	*/
	public CmdInstallConfig(byte[] data)
	{
		int len = 0;
		for(; len<data.length && len < 64; len++)
			if (data[len] == 0)
				break;
		cfgType = new String(data, 0, len);
		if (data.length <= 64)
			filedata = null;
		else
			filedata = ArrayUtil.getField(data, 64, data.length - 64);
	}

	/**
	  Executes the command, administrative priviledge has already been
	  checked.
	  @param ldds the server thread object holding connection to client.
	*/
	public int executeAdmin(LddsThread ldds) throws IOException,
		ArchiveException, UnknownUserException
	{
		File f = null;
		LrgsConfig cfg = LrgsConfig.instance();

		if (cfgType.equalsIgnoreCase("lrgs"))
		{
			f = cfg.getCfgFile();
		}
		else if (cfgType.equalsIgnoreCase("ddsrecv"))
		{
			f = new File(EnvExpander.expand(cfg.ddsRecvConfig));
		}
		else if (cfgType.equalsIgnoreCase("drgs"))
		{
			f = new File(EnvExpander.expand(cfg.drgsRecvConfig));
		}
		else if (cfgType.equalsIgnoreCase("networkDcp"))
		{
			f = new File(EnvExpander.expand(NetworkDcpRecv.cfgFileName));
		}
		else if (TextUtil.startsWithIgnoreCase(cfgType, "netlist-delete:"))
		{
			String listname = cfgType.substring(15);
			f = new File(
				EnvExpander.expand(cfg.ddsNetlistDir + "/" + listname));
			if (f.exists())
				f.delete();
			LddsMessage msg = 
				new LddsMessage(LddsMessage.IdInstConfig, cfgType);
			ldds.send(msg);
			Logger.instance().info("Client " + ldds.getName() 
				+ " deleted network list '" + listname + "'");
			return 0;
		}
		else if (TextUtil.startsWithIgnoreCase(cfgType, "netlist:"))
		{
			String listname = cfgType.substring(8);
			f = new File(
				EnvExpander.expand(cfg.ddsNetlistDir + "/" + listname));
		}
		else
			throw new LddsRequestException("Invalid config name '"
				+ cfgType + "'", LrgsErrorCode.DBADKEYWORD, false);

		try
		{
			FileOutputStream fos = new FileOutputStream(f);
			fos.write(filedata);
			fos.close();

			LddsMessage msg = 
				new LddsMessage(LddsMessage.IdInstConfig, cfgType);
			ldds.send(msg);

			Logger.instance().info("Client " + ldds.getName() 
				+ " installed config " + cfgType + ", file=" + f.getPath());
		}
		catch(IOException ioe)
		{
			throw new NoSuchFileException("Cannot save config '"
				+ cfgType + "', file=" + f.getPath() + ": " + ioe.toString());
		}
		return 0;
	}

	/** @return the code associated with this command. */
	public char getCommandCode() { return LddsMessage.IdInstConfig; }
}
