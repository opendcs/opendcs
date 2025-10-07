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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.util.EnvExpander;
import ilex.util.TextUtil;

import lrgs.common.ArchiveException;
import lrgs.common.LrgsErrorCode;
import lrgs.lrgsmain.LrgsConfig;
import lrgs.networkdcp.NetworkDcpRecv;

/**
This command returns a configuration file back to the client.
The request message contains a 64-byte field denoting the configuration
file to be returned:
<ul>
  <li>lrgs - return main configuration file (usually lrgs.conf)</li>
  <li>ddsrecv - return DDS Recv Config file (usually ddsrecv.conf)</li>
  <li>drgs - return DRGS Recv Config file (usually drgsconf.xml)</li>
  <li>networkDcp - return Network DCP Config file (network-dcp.conf)</li>
  <li>netlist-list - return a list of files from the netlist dir</li>
  <li>netlist:name - return the named network list from the netlist dir</li>
</ul>
The file-names may vary from system to system because the main config file
can be specified on the command line argument, and the others can be
specified in the main config file.
<p>
The response message (sent back to the client) contains the same 64-byte
field followed by the variable length file contents.
<p>
If an error occurs, the 64-byte field will contain a question mark
followed by two comma-separated integer fields corresponding to
'derrno' and 'errno'.
*/
public class CmdReturnConfig extends CmdAdminCmd
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private String cfgType;

	/** @return "CmdReturnConfig"; */
	public String cmdType()
	{
		return "CmdReturnConfig";
	}

	/**
	  Constructor.
	  @param ctb (config type bytes) which configuration file to return.
	*/
	public CmdReturnConfig(byte[] ctb)
	{
		int len = 0;
		for(; len<ctb.length; len++)
			if (ctb[len] == 0)
				break;
		this.cfgType = new String(ctb, 0, len);
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
		else if (cfgType.equalsIgnoreCase("netlist-list"))
		{
			getNetlistList(ldds);
			return 0;
		}
		else if (TextUtil.startsWithIgnoreCase(cfgType, "netlist:"))
		{
			String nlname = cfg.ddsNetlistDir + "/" + cfgType.substring(8);
			f = new File(EnvExpander.expand(nlname));
		}
		else
			throw new LddsRequestException("Invalid config name '"
				+ cfgType + "'", LrgsErrorCode.DBADKEYWORD, false);

		if (!f.canRead())
			throw new LddsRequestException("Connot read config type '"
				+ cfgType + "' file='" + f.getPath() + "'",
				LrgsErrorCode.DDDSINTERNAL, false);

		try (FileInputStream fis = new FileInputStream(f))
		{
			byte filedata[] = new byte[(int)f.length()];
			fis.read(filedata);

			byte msgdata[] = new byte[64+filedata.length];
			int i;
			for(i = 0; i < 64 && i < cfgType.length(); i++)
				msgdata[i] = (byte)cfgType.charAt(i);
			msgdata[i] = (byte)0;

			for(i=0; i<filedata.length; i++)
				msgdata[i+64] = filedata[i];

			LddsMessage msg = new LddsMessage(LddsMessage.IdRetConfig, "");
			msg.MsgLength = msgdata.length;
			msg.MsgData = msgdata;
			ldds.send(msg);

			log.trace("Successfully retrieved and sent config {}, file={}", cfgType, f.getPath());
		}
		catch(IOException ioe)
		{
			throw new NoSuchFileException("Cannot send config '" + cfgType + "', file=" + f.getPath(), ioe);
		}
		return 0;
	}

	String[] knownFiles = { "cleanup.sh", "cronfile", "last_copy",
		"nl_sync.sh", "readme.txt", "remote" };

	private void getNetlistList(LddsThread ldds)
		throws ArchiveException, UnknownUserException
	{
		LrgsConfig cfg = LrgsConfig.instance();
		File nldir = new File(EnvExpander.expand(cfg.ddsNetlistDir));
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		String files[] = nldir.list();
		int nret = 0;
		try
		{
			String fn = "netlist-list";
			baos.write(fn.getBytes());
			for(int i=fn.length(); i<64; i++)
				baos.write((byte)0);

		  nextFile:
			for(int i=0; files != null && i < files.length; i++)
			{
				for(int ki = 0; ki<knownFiles.length; ki++)
					if (knownFiles[ki].equals(files[i]))
						continue nextFile;
				baos.write(files[i].getBytes());
				baos.write((byte)'\n');
				nret++;
			}
			LddsMessage msg = new LddsMessage(LddsMessage.IdRetConfig, "");
			msg.MsgData = baos.toByteArray();
			msg.MsgLength = msg.MsgData.length;
			ldds.send(msg);

		}
		catch(IOException ex)
		{
			throw new NoSuchFileException("Error listing directory '" + nldir.getPath(), ex);
		}
	}

	/** @return the code associated with this command. */
	public char getCommandCode() { return LddsMessage.IdRetConfig; }
}
