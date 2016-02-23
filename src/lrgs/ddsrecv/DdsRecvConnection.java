/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*/
package lrgs.ddsrecv;

import java.io.IOException;
import java.net.UnknownHostException;

import ilex.util.Logger;
import ilex.util.PasswordFileEntry;
import ilex.util.TextUtil;
import lrgs.apiadmin.AuthenticatorString;
import lrgs.common.DcpMsg;
import lrgs.common.LrgsErrorCode;
import lrgs.common.SearchCriteria;
import lrgs.ldds.CmdAuthHello;
import lrgs.ldds.LddsClient;
import lrgs.ldds.ProtocolError;
import lrgs.ldds.ServerError;
import lrgs.ldds.UnknownUserException;
import lrgs.lrgsmain.LrgsMain;
import lrgs.lrgsmain.LrgsInputInterface;
import lrgs.lrgsmain.LrgsInputException;

/**
This class encapsulates a connection to a remote DDS server for the purpose
of retrieving DCP data for my own local archive.
*/
public class DdsRecvConnection
	implements LrgsInputInterface
{
	/** My status */
	String status;

	/** My slot number */
	private int slot;

	/** My configuration */
	private DdsRecvConnectCfg config;

	private long lastActivityTime;
	private long lastMessageTime;

	private LddsClient lddsClient;

	private int dataSourceId;

	private LrgsMain lrgsMain;
	
	/** Dds Receive group: primary/secondary. Used to display group  on rtstat screen */
	private String group;

	/**
	 * Constructor.
	 * @param config the configuration for this connection.
	 */
	public DdsRecvConnection(DdsRecvConnectCfg config, LrgsMain lrgsMain)
	{
		lddsClient = new LddsClient(config.host, config.port);
		lddsClient.enableMultiMessageMode(true);
		lddsClient.enableExtMessageMode(true);
		lddsClient.setModule(DdsRecv.module);

		this.config = config;
		slot = -1;
		status = "Unused";
		lastActivityTime = 0L;
		lastMessageTime = 0L;
		dataSourceId = lrgs.db.LrgsConstants.undefinedId;
		this.lrgsMain = lrgsMain;
		group = config.group;
	}

	/**
	 * @return the configuration for this connection.
	 */
	public DdsRecvConnectCfg getConfig()
	{
		return config;
	}

	/**
	 * From LrgsInputInterface.
	 * @return the type of this input interface.
	 */
	public int getType()
	{
		return DL_DDSCON;
	}

	/**
	 * From LrgsInputInterface.
	 * All inputs must keep track of their 'slot', which is a unique index
	 * into the LrgsMain's vector of all input interfaces.
	 * @param slot the slot number.
	 */
	public void setSlot(int slot)
	{
		this.slot = slot;
	}

	/**
	 * From LrgsInputInterface.
	 * @return the name of this interface.
	 */
	public String getInputName()
	{
		return "DDS:" + config.name;
	}

	/**
	 * From LrgsInputInterface.
	 * Initializes the interface by connecting to the server and sending
	 * the (authenticated or anonymous) hello message.
	 * @throws LrgsInputException when an unrecoverable error occurs.
	 */
	public void initLrgsInput()
		throws LrgsInputException
	{
		status = "Initializing";
		Logger.instance().info(DdsRecv.module 
			+ " Initializing DDS recv connection to " + lddsClient.getName());
		lastActivityTime = System.currentTimeMillis();
		lastMessageTime = System.currentTimeMillis();
		try
		{
			lddsClient.connect();
			status = "Ready";
			Logger.instance().info(DdsRecv.module
				+ " Connection established to " + lddsClient.getName());
		}
		catch(UnknownHostException ex)
		{
			status = "Unknown Host";
			String msg = "Can't connect to "
				+ lddsClient.getName() + ": " + ex;
			Logger.instance().warning(
				DdsRecv.module + ":" + DdsRecv.EVT_CONNECTION_FAILED + "- " 
				+ msg);
			throw new LrgsInputException(msg);
		}
		catch(IOException ex)
		{
			status = "IO Error";
			String msg = "IO Error on connection to "
				+ lddsClient.getName() + ": " + ex;
			Logger.instance().warning(
				DdsRecv.module + ":" + DdsRecv.EVT_CONNECTION_FAILED + "- " 
				+ msg);
			throw new LrgsInputException(msg);
		}

		if (config.authenticate)
		{
			PasswordFileEntry pfe;
			try
			{
				pfe = CmdAuthHello.getPasswordFileEntry(config.username);
			}
			catch (UnknownUserException e)
			{
				pfe = null;
			}
//			PasswordFile pf;
//			try
//			{
// 				pf = new PasswordFile(new File(
//					EnvExpander.expand("$LRGSHOME/.lrgs.passwd")));
//				pf.read();
//			}
//			catch(IOException ex)
//			{
//				status = "Auth Error";
//				throw new LrgsInputException("Connection to "
//					+ lddsClient.getName() 
//					+ " calls for authenticated connection "
//					+ "but can't read local password file: " + ex);
//			}
//			PasswordFileEntry pfe = pf.getEntryByName(config.username);
			if (pfe == null)
				throw new LrgsInputException("Connection to "
					+ lddsClient.getName() 
					+ " calls for authenticated connection "
					+ "but no entry for username '" + config.username
					+ "'");
			try
			{
				lddsClient.sendAuthHello(pfe, AuthenticatorString.ALGO_SHA);
			}
			catch(IOException ex)
			{
				status = "IO Error";
				throw new LrgsInputException("IO Error on connection to "
					+ lddsClient.getName() + ": " + ex);
			}
			catch(ServerError ex)
			{
				status = "Rejected by Svr";
				throw new LrgsInputException("Server at "
					+ lddsClient.getName() + " rejected connection: " + ex);
			}
			catch(ProtocolError ex)
			{
				status = "Proto Error";
				throw new LrgsInputException("Protocol Error on connection to "
					+ lddsClient.getName() + ": " + ex);
			}
		}
		else
		{
			try
			{
				lddsClient.sendHello(config.username);
			}
			catch(IOException ex)
			{
				status = "IO Error";
				throw new LrgsInputException("IO Error on connection to "
					+ lddsClient.getName() + ": " + ex);
			}
			catch(ServerError ex)
			{
				status = "Rejected by Svr";
				throw new LrgsInputException("Server at "
					+ lddsClient.getName() + " rejected connection: " + ex);
			}
			catch(ProtocolError ex)
			{
				status = "Proto Error";
				throw new LrgsInputException("Protocol Error on connection to "
					+ lddsClient.getName() + ": " + ex);
			}
		}
		dataSourceId =
            lrgsMain.getDbThread().getDataSourceId(DL_DDS_TYPESTR, config.host);
	}

	/**
	 * Sends a noop command to keep the connection alive.
	 */
	public void noop()
		throws LrgsInputException
	{
		try 
		{
			lddsClient.sendNoop();
			status = "Ready";
			lastActivityTime = System.currentTimeMillis();
		}
		catch(Exception ex)
		{
			status = "Error";
			throw new LrgsInputException("Error sending NOOP: " + ex);
		}
	}

	/**
	 * @return true if this connection is enabled in the configuration.
	 */
	public boolean isEnabled()
	{
		return config.enabled;
	}

	/**
	 * From LrgsInputInterface.
	 * Shuts down the interface.
	 * Any errors encountered should be handled within this method.
	 */
	public void shutdownLrgsInput()
	{
		lddsClient.disconnect();
		status = "Disconnected";
	}

	/**
	 * @return true if this connection is currently connected.
	 */
	public boolean isConnected()
	{
		return lddsClient.isConnected();
	}

	/**
	 * Enable or Disable the interface. 
	 * The interface should only attempt to archive messages when enabled.
	 * @param enabled true if the interface is to be enabled, false if disabled.
	 */
	public void enableLrgsInput(boolean enabled)
	{
		if (enabled && !config.enabled)
		{
			Logger.instance().info("Enabling DDS-Recv connection to "
				+ config.host);
			config.enabled = true;
		}
		else if (!enabled && config.enabled)
		{
			Logger.instance().info("Disabling DDS-Recv connection to "
				+ config.host);
			config.enabled = false;
			shutdownLrgsInput();
			status = "Disabled";
		}
	}

	/**
	 * @return true if this downlink can report a Bit Error Rate.
	 */
	public boolean hasBER()
	{
		return false;
	}

	/**
	 * @return the Bit Error Rate as a string.
	 */
	public String getBER()
	{
		return "";
	}

	/**
	 * @return true if this downlink assigns a sequence number to each msg.
	 */
	public boolean hasSequenceNums()
	{
		return false;
	}

	/**
	 * @return the numeric code representing the current status.
	 */
	public int getStatusCode()
	{
		return DL_STRSTAT;
	}

	/**
	 * @return a short string description of the current status.
	 */
	public String getStatus()
	{
		return status;
	}

	/**
	 * @return slot number assigned to this interface.
	 */
	public int getSlot()
	{
		return slot;
	}

	/**
	 * Sends a search criteria to the connection.
	 * @param searchCrit the search criteria.
	 * @return true on success, false on failure.
	 */
	public boolean sendSearchCriteria(SearchCriteria searchCrit)
	{
		DdsRecvSettings settings = DdsRecvSettings.instance();
		for(NetlistGroupAssoc nga : settings.getNetlistGroupAssociations())
//		for(NetworkList netlist : settings.networkLists)
		{
			if (!TextUtil.strEqualIgnoreCase(nga.getGroupName(), group))
				continue;
			if (nga.getNetworkList() == null)
			{
				Logger.instance().warning(DdsRecv.module +
					" No network list found for group=" + nga.getGroupName()
					+ " list='" + nga.getNetlistName() + "' -- ignored.");
				continue;
			}
			try
			{
				lddsClient.sendNetList(nga.getNetworkList(), null);
			}
			catch(ServerError ex)
			{
				Logger.instance().warning(DdsRecv.module +
					"Server at " + lddsClient.getName() 
					+ " rejected network list '" + nga.getNetworkList().makeFileName() 
					+ "': " + ex + " -- ignored.");
			}
			catch(Exception ex)
			{
				String msg = DdsRecv.module + ":" + DdsRecv.EVT_CONNECTION_FAILED + "- " 
					+ " Error sending network list to "
					+ lddsClient.getName() + ": " + ex;
				Logger.instance().warning(msg);
				System.err.println(msg);
				ex.printStackTrace(System.err);
				shutdownLrgsInput();
				status = "Error";
				return false;
			}
		}
		searchCrit.DapsStatus = 
			config.acceptARMs ? SearchCriteria.ACCEPT : SearchCriteria.REJECT;
		try 
		{
//			Logger.instance().info(DdsRecv.module +
//				" sending search crit '" + searchCrit.toString() + "'"
//				+ " to " + lddsClient.getName());
			lddsClient.sendSearchCrit(searchCrit); 
			status = "Catch-up";
			return true;
		}
		catch(Exception ex)
		{
			Logger.instance().warning(
				DdsRecv.module + ":" + DdsRecv.EVT_CONNECTION_FAILED + "- " 
				+ " Error sending search criteria to "
				+ lddsClient.getName() + ": " + ex);
			shutdownLrgsInput();
			status = "Error";
			return false;
		}
	}

	/**
	 * @return a DCP message from the connection, or null if caught-up.
	 * @throws LrgsInputException if the connection has failed.
	 */
	public DcpMsg getDcpMsg()
		throws LrgsInputException
	{
		try 
		{
			DcpMsg msg = lddsClient.getDcpMsg(60); 
			long now = System.currentTimeMillis();
			lastActivityTime = now;
			if (msg != null)
			{
				if (System.currentTimeMillis() - msg.getDapsTime().getTime()
			 		< 30000L)
				status = "Real-Time";
				lastMessageTime = now;
				msg.setDataSourceId(dataSourceId);
			}
			else if (now - lastMessageTime > 
				DdsRecvSettings.instance().timeout * 1000L)
			{
				String errmsg = DdsRecv.module + 
					" timeout on connection to " + lddsClient.getName();
				Logger.instance().warning(
					DdsRecv.module + ":" + DdsRecv.EVT_CONNECTION_FAILED + "- " 
					+ errmsg);
				lddsClient.disconnect();
				status = "Timeout";
				throw new LrgsInputException(errmsg);
			}
			return msg;
		}
		catch(Exception ex)
		{
			if (ex instanceof ServerError)
			{
				ServerError se = (ServerError)ex;
				if (se.Derrno == LrgsErrorCode.DMSGTIMEOUT
				 || se.Derrno == LrgsErrorCode.DUNTIL)
				{
					long now = System.currentTimeMillis();
					if (now - lastMessageTime > 
						(DdsRecvSettings.instance().timeout * 1000L))
					{
						String errmsg = DdsRecv.module + 
							" timeout on connection to " + lddsClient.getName();
						Logger.instance().warning(
							DdsRecv.module + ":" 
							+ DdsRecv.EVT_CONNECTION_FAILED + "- " 
							+ errmsg);
						lddsClient.disconnect();
						status = "Timeout";
						throw new LrgsInputException(errmsg);
					}
					else
					{
						Logger.instance().debug3(DdsRecv.module 
							+ " caught up on connection " + getName());
						status = "Real-Time";
						return null;
					}
				}
			}
			String msg = DdsRecv.module + 
				" Error getting message from " + lddsClient.getName()
				+ ": " + ex;
			Logger.instance().warning(
				DdsRecv.module + ":" + DdsRecv.EVT_CONNECTION_FAILED + "- " 
				+ msg);
			lddsClient.disconnect();
			if (!(ex instanceof IOException)
			 && !(ex instanceof ServerError
			       && ((ServerError)ex).Derrno == LrgsErrorCode.DDDSINTERNAL
				   && ex.getMessage().contains("not currently usable")))
			{
				System.err.println(msg);
				ex.printStackTrace(System.err);
			}
			status = "Error";
			throw new LrgsInputException(msg);
		}
	}

	/**
	 * @return name of this connection.
	 */
	public String getName()
	{
		return lddsClient.getName();
	}

	public long getLastActivityTime()
	{
		return lastActivityTime;
	}

	public void flush()
	{
		lddsClient.flushBufferedMessages();
	}

	public int getDataSourceId() { return dataSourceId; }
	
	/** @return true if this interface receives APR messages */
	public boolean getsAPRMessages() { return true; }
// TODO: Make the APR setting configurable for each connection.

	/**
	 * @return the group
	 */
	public String getGroup() {
		return group;
	}

	/**
	 * @param group the group to set
	 */
	public void setGroup(String group) {
		this.group = group;
	}

}
