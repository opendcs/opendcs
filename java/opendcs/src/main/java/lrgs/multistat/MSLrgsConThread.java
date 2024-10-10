package lrgs.multistat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;

import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.xml.sax.SAXException;

import hec.util.TextUtil;
import ilex.util.AuthException;
import ilex.util.ByteUtil;
import ilex.util.DesEncrypter;
import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.util.PasswordFile;
import ilex.util.PasswordFileEntry;
import ilex.gui.EventsPanel;
import ilex.xml.XmlOutputStream;
import lrgs.ldds.LddsClient;
import lrgs.ldds.LddsMessage;
import lrgs.ldds.ServerError;
import lrgs.ldds.ProtocolError;
import lrgs.statusxml.LrgsStatusSnapshotExt;
import lrgs.statusxml.TopLevelXio;
import lrgs.lrgsmon.DetailReportGenerator;
import lrgs.rtstat.DdsClientIf;
import lrgs.ldds.LddsClient;
import lrgs.ldds.DdsUser;
import lrgs.rtstat.RtStatPanel;
import lrgs.lrgsmain.LrgsConfig;
import lrgs.apiadmin.AuthenticatorString;
import lrgs.ddsrecv.DdsRecvSettings;
import lrgs.drgs.DrgsInputSettings;
import lrgs.db.Outage;

public class MSLrgsConThread
	extends Thread
	implements DdsClientIf
{
	private int conNum;
	private LrgsSummaryStatPanel summaryPanel;
	private LddsClient lddsClient;
	private long lastConfigCheck = 0L;
	private long lastConfigLoad = 0L;
	private boolean isShutdown;
	private String lrgsDisplayName;
	private String lrgsHostName;
	private int lrgsPort;
	private String lrgsUserName;
	private String lrgsPassword;
	private char lrgsSource;
	private JTabbedPane tabbedPane;
	private long lastConnectAttempt;
	private TopLevelXio statusParser;
	private RtStatPanel detailPanel;
	private ByteArrayOutputStream baos;
	private XmlOutputStream xos;
	private DetailReportGenerator repgen;
	private MultiStatFrame msFrame;
	public LrgsStatusSnapshotExt currentStatus;

	/**
	 * @param conNum 1, 2, or 3.
	 */
	public MSLrgsConThread(int conNum, LrgsSummaryStatPanel summaryPanel,
		JTabbedPane tabbedPane, RtStatPanel detailPanel, MultiStatFrame msFrame)
	{
		super();
		setName("MSCon-" + conNum);
		this.conNum = conNum;
		this.summaryPanel = summaryPanel;
		this.tabbedPane = tabbedPane;
		this.detailPanel = detailPanel;
		this.msFrame = msFrame;
		lddsClient = null;
		isShutdown = false;
		lrgsDisplayName = "";
		lrgsHostName = "";
		lrgsPort = -1;
		lrgsUserName = "";
		lrgsSource = 'u';
		lastConnectAttempt = 0L;
		lrgsPassword = "";
		try { statusParser = new TopLevelXio(); }
		catch(Exception ex)
		{
			Logger.instance().warning("Cannot construct XML parser: " + ex);
		}
		baos = new ByteArrayOutputStream(16000);
		xos = new XmlOutputStream(baos, "html");
		repgen = new DetailReportGenerator("satdish.jpg");
		currentStatus = new LrgsStatusSnapshotExt();
	}

	public void run()
	{
		try { sleep(5000L); } catch(Exception ex) {}
		
		while(!isShutdown)
		{
			long now = System.currentTimeMillis();
			if (now - lastConfigCheck > 10000L)
			{
				lastConfigCheck = now;
				checkConfig();
			}
			if (lddsClient == null && now - lastConnectAttempt > 30000L)
				attemptConnect();
			if (lddsClient != null)
				poll();

			try { sleep(1000L); }
			catch(InterruptedException ex) {}
		}
	}

	public void shutdown()
	{
		isShutdown = true;
	}


	private void checkConfig()
	{
		MultiStatConfig cfg = MultiStatConfig.instance();
		if (cfg.getLastLoadTime() <= lastConfigLoad)
			return;
		Logger.instance().debug1("Reloading configuration.");
		lastConfigLoad = System.currentTimeMillis();

		if (conNum == 1)
		{
			if (!TextUtil.equalsIgnoreCase(lrgsHostName, cfg.Lrgs1HostName)
			 || lrgsPort != cfg.Lrgs1Port
			 || !lrgsUserName.equals(cfg.Lrgs1UserName)
			 || !lrgsPassword.equals(cfg.Lrgs1Password))
				setConnection(cfg.Lrgs1HostName, cfg.Lrgs1Port,
					cfg.Lrgs1UserName, cfg.Lrgs1Password);
			if (cfg.Lrgs1HostName != null)
				summaryPanel.systemNameField.setText(lrgsDisplayName = cfg.Lrgs1DisplayName);
		}
		else if (conNum == 2)
		{
			if (!TextUtil.equalsIgnoreCase(lrgsHostName, cfg.Lrgs2HostName)
			 || lrgsPort != cfg.Lrgs2Port
			 || !lrgsUserName.equals(cfg.Lrgs2UserName)
			 || !lrgsPassword.equals(cfg.Lrgs2Password))
				setConnection(cfg.Lrgs2HostName, cfg.Lrgs2Port,
					cfg.Lrgs2UserName, cfg.Lrgs2Password);
			if (cfg.Lrgs2HostName != null)
				summaryPanel.systemNameField.setText(lrgsDisplayName = cfg.Lrgs2DisplayName);
		}
		else if (conNum == 3)
		{
			if (!TextUtil.equalsIgnoreCase(lrgsHostName, cfg.Lrgs3HostName)
			 || lrgsPort != cfg.Lrgs3Port
			 || !lrgsUserName.equals(cfg.Lrgs3UserName)
			 || !lrgsPassword.equals(cfg.Lrgs3Password))
				setConnection(cfg.Lrgs3HostName, cfg.Lrgs3Port,
					cfg.Lrgs3UserName, cfg.Lrgs3Password);
			if (cfg.Lrgs3HostName != null)
				summaryPanel.systemNameField.setText(lrgsDisplayName = cfg.Lrgs3DisplayName);
		}
		else if (conNum == 4)
		{
			if (!TextUtil.equalsIgnoreCase(lrgsHostName, cfg.Lrgs4HostName)
			 || lrgsPort != cfg.Lrgs4Port
			 || !lrgsUserName.equals(cfg.Lrgs4UserName)
			 || !lrgsPassword.equals(cfg.Lrgs4Password))
				setConnection(cfg.Lrgs4HostName, cfg.Lrgs4Port,
					cfg.Lrgs4UserName, cfg.Lrgs4Password);
			if (cfg.Lrgs4HostName != null)
				summaryPanel.systemNameField.setText(lrgsDisplayName = cfg.Lrgs4DisplayName);
		}

		Logger.instance().debug1("After config, displayName="
			+ lrgsDisplayName + ", host=" + lrgsHostName + ", port=" + lrgsPort
			+ ", user=" + lrgsUserName + ", pw=" + lrgsPassword
			+ ", source=" + lrgsSource);
	}


	public String getLrgsDisplayName()
	{
		return lrgsDisplayName;
	}

	private void setConnection(String hn, int port, String un, String pw)
	{
		Logger.instance().debug1("Setting connection to host=" + hn
			+ ", port=" + port + ", user=" + un + ", pw=" + pw);
		lrgsHostName = hn;
		lrgsUserName = un;
		lrgsPort = port;
		lrgsPassword = pw;
		SwingUtilities.invokeLater(
			new Runnable()
			{
				public void run()
				{
					currentStatus.systemStatus = "Y:Not Connected";
					updateStatus(null);
					summaryPanel.numClientsField.setText("Unknown");
					summaryPanel.numClientsField.setWarning();
				}
			});
		if (lddsClient != null)
		{
			disconnect("Config Change");
		}

	}

	private void setStatus(final String status, final boolean isError,
		final boolean isWarning)
	{
		SwingUtilities.invokeLater(
			new Runnable()
			{
				public void run()
				{
					summaryPanel.systemStatusField.setText(status);
					if (isError)
						summaryPanel.systemStatusField.setError();
					else if (isWarning)
						summaryPanel.systemStatusField.setWarning();
					else
						summaryPanel.systemStatusField.setOk();
				}
			});
	}

	private void setLrgsSource(char sr)
	{
		lrgsSource = sr;
		SwingUtilities.invokeLater(
			new Runnable()
			{
				public void run()
				{
					summaryPanel.dataSourceField.setText(
						lrgsSource == 'd' ? "Downlink" : "Uplink");
				}
			});
	}

	private synchronized void attemptConnect()
	{
		lastConnectAttempt = System.currentTimeMillis();
		if (lrgsHostName == null)
			return;
		
		lddsClient = new LddsClient(lrgsHostName, lrgsPort);
		try
		{
			currentStatus.systemStatus = "Y:Connecting";
			updateStatus(null);
			Logger.instance().info(
				"LRGS " + conNum + " Connecting to " + lddsClient.getName());
			lddsClient.connect();
		}
		catch(UnknownHostException ex)
		{
			currentStatus.systemStatus = "R:Bad Hostname";
			updateStatus(null);
			String msg = "Can't connect to "
				+ lddsClient.getName() + ": " + ex;
			Logger.instance().failure(
				MultiStat.module + ":" 
				+ MultiStat.EVT_CANT_CONNECT_LRGS1 + (conNum-1)
				+ " " + msg);
			lddsClient = null;
			return;
		}
		catch(IOException ex)
		{
			currentStatus.systemStatus = "R:IO Error";
			updateStatus(null);
			String msg = "IO Error on connection to "
				+ lddsClient.getName() + ": " + ex;
			Logger.instance().warning(
				MultiStat.module + ":" 
					+ MultiStat.EVT_CANT_CONNECT_LRGS1 + (conNum-1)
					+ " " + msg);
			lddsClient = null;
			return;
		}
		if (lrgsPassword != null && lrgsPassword.length() > 0
		 && lrgsPassword.charAt(0) != 'N' && lrgsPassword.charAt(0) != 'n')
		{
			PasswordFile pf;
			String pwfn = EnvExpander.expand("$LRGSHOME/.lrgs.passwd");
			try
			{
 				pf = new PasswordFile(new File(pwfn));
				pf.read();
			}
			catch(IOException ex)
			{
				Logger.instance().warning(
					MultiStat.module + " Can't open '" + pwfn + "': " + ex);
				pwfn = EnvExpander.expand("$DCSTOOL_HOME/.lrgs.passwd");
				try
				{
 					pf = new PasswordFile(new File(pwfn));
					pf.read();
				}
				catch(IOException ex2)
				{
					Logger.instance().warning(
						MultiStat.module + " Can't open '" + pwfn + "': " +ex2);
	
					Logger.instance().warning(
						MultiStat.module + ":" 
						+ MultiStat.EVT_CANT_CONNECT_LRGS1 + (conNum-1)
						+ " " + "Connection to " + lddsClient.getName()
						+ " calls for authenticated connection "
						+ "but can't read local password file.");
					disconnect("Auth Error");
					return;
				}
			}
			PasswordFileEntry pfe = pf.getEntryByName(lrgsUserName);
			if (pfe == null)
			{
				Logger.instance().warning(
					MultiStat.module + ":" 
					+ MultiStat.EVT_CANT_CONNECT_LRGS1 + (conNum-1)
					+ " Connection to " + lddsClient.getName()
					+ " calls for authenticated connection "
					+ "but no local entry for username '" + lrgsUserName
					+ "'");
				disconnect("No Auth");
				return;
			}
			try
			{
				lddsClient.sendAuthHello(pfe, AuthenticatorString.ALGO_SHA);
			}
			catch(IOException ex)
			{
				Logger.instance().warning(
					MultiStat.module + ":" 
					+ MultiStat.EVT_CANT_CONNECT_LRGS1 + (conNum-1)
					+ " IO Error on connection to "
					+ lddsClient.getName() + ": " + ex);
				disconnect("IO Error");
				return;
			}
			catch(ServerError ex)
			{
				Logger.instance().warning(
					MultiStat.module + ":" 
					+ MultiStat.EVT_CANT_CONNECT_LRGS1 + (conNum-1)
					+ " Server at "
					+ lddsClient.getName() + " rejected connection: " + ex);
				disconnect("Rejected by Svr");
				return;
			}
			catch(ProtocolError ex)
			{
				Logger.instance().warning(
					MultiStat.module + ":" 
					+ MultiStat.EVT_CANT_CONNECT_LRGS1 + (conNum-1)
					+ " Protocol Error on connection to "
					+ lddsClient.getName() + ": " + ex);
				disconnect("Proto Error");
				return;
			}
		}
		else
		{
			try
			{
				lddsClient.sendHello(lrgsUserName);
			}
			catch(IOException ex)
			{
				Logger.instance().warning(
					MultiStat.module + ":" 
					+ MultiStat.EVT_CANT_CONNECT_LRGS1 + (conNum-1)
					+ " IO Error on connection to "
					+ lddsClient.getName() + ": " + ex);
				disconnect("IO Error");
				return;
			}
			catch(ServerError ex)
			{
				Logger.instance().warning(
					MultiStat.module + ":" 
					+ MultiStat.EVT_CANT_CONNECT_LRGS1 + (conNum-1)
					+ " Server at "
					+ lddsClient.getName() + " rejected connection: " + ex);
				disconnect("Rejected by Svr");
				return;
			}
			catch(ProtocolError ex)
			{
				Logger.instance().warning(
					MultiStat.module + ":" 
					+ MultiStat.EVT_CANT_CONNECT_LRGS1 + (conNum-1)
					+ " Protocol Error on connection to "
					+ lddsClient.getName() + ": " + ex);
				disconnect("Proto Error");
				return;
			}
		}
		currentStatus.systemStatus = "Connected";
		updateStatus(null);
		Logger.instance().info(
			MultiStat.module + ":" 
			+ (-(MultiStat.EVT_CANT_CONNECT_LRGS1 + (conNum-1)))	
			+ " Connected to " + lddsClient.getName());
	}

	private synchronized void poll()
	{
		try
		{
			byte[] statmsg = lddsClient.getStatus();
			currentStatus 
				= statusParser.parse(statmsg, 0, statmsg.length, lrgsHostName);
			baos.reset();
			updateStatus(lddsClient.getEvents());
			return;
		}
		catch(ServerError ex)
		{
			Logger.instance().warning(
				"ServerError on getStatus from " + lddsClient.getName()
				+ ": " + ex);
			disconnect("Server Error");
			currentStatus.systemStatus = "R:Server Error";
		}
		catch(ProtocolError ex)
		{
			Logger.instance().warning(
				"Unexpected response to getStatus " + lddsClient.getName()
				+ ": " + ex);
			disconnect("Proto Error");
			currentStatus.systemStatus = "R:Proto Error";
		}
		catch(IOException ex)
		{
			Logger.instance().warning(
				"IOException on getStatus from " + lddsClient.getName()
				+ ": " + ex);
			disconnect("IO Error");
			currentStatus.systemStatus = "R:IO Error";
		}
		catch(SAXException ex)
		{
			Logger.instance().warning(
				"Error parsing status from " + lddsClient.getName()
				+ ": " + ex);
			disconnect("StatFmtErr");
			currentStatus.systemStatus = "R:Stat Fmt Err";
		}
		updateStatus(null);
	}

	private void disconnect(String why)
	{
		lddsClient.disconnect();
		lddsClient = null;
	}

	private void updateStatus(final String[] events)
	{
		if (lrgsHostName != null)
			SwingUtilities.invokeLater(
				new Runnable()
				{
					public void run()
					{
						if (events != null)
							for(int i=0; i<events.length; i++)
								msFrame.addEvent(events[i], lrgsDisplayName);
						int n = msFrame.alarmList.countAlarmsForSource(
							lrgsDisplayName);
						summaryPanel.setStatus(currentStatus, n, lrgsSource);
						try 
						{
							repgen.writeReport(xos, lrgsHostName, currentStatus, 0); 
							detailPanel.updateStatus(baos.toString());
						}
						catch(IOException ex)
						{
							Logger.instance().warning(
							  "Unexpected IO exception writing detail report: "
							 + ex);
						}
					}
				});
	}

	// Not used
	public byte[] getStatus()
	{
		return null;
	}

	// Not used
	public String[] getEvents()
	{
		return null;
	}

	public synchronized ArrayList<DdsUser> getUsers()
		throws AuthException
	{
		if (lddsClient == null)
			throw new AuthException("Cannot get user list -- not connected.");
		return lddsClient.getUsers();
	}

	public synchronized void modUser(DdsUser ddsUser, String pw)
		throws AuthException
	{
		if (lddsClient == null)
			throw new AuthException("Cannot set user info -- not connected.");
		lddsClient.modUser(ddsUser, pw);
	}

	public synchronized void rmUser(String userName)
		throws AuthException
	{
		if (lddsClient == null)
			throw new AuthException("Cannot remove user -- not connected.");
		lddsClient.rmUser(userName);
	}

	/**
	 * Sends the new LrgsConfiguration to the server.
	 */
	public void applyLrgsConfig(LrgsConfig lrgsConfig)
		throws AuthException
	{
	}

	/**
	 * Sends the new DdsRecvSettings to the server.
	 */
	public void applyDdsRecvSettings(DdsRecvSettings settings)
		throws AuthException
	{
	}

	/**
	 * Sends the new DrgsInputSettings to the server.
	 */
	public void applyDrgsInputSettings(DrgsInputSettings settings)
		throws AuthException
	{
	}

	public void applyNetworkDcpSettings(DrgsInputSettings settings)
		throws AuthException
	{
	}

	/**
	 * @return a list of network lists that exist on the server.
	 */
	public String[] getNetlistList()
		throws AuthException
	{
		return null;
	}

	/**
	 * @return the data in a particular network list.
	 */
	public byte[] getNetlist(String listname)
	{
		return null;
	}

	/**
	 * Install a network list on the server.
	 */
	public void installNetlist(String listname, byte[] data)
	{
	}

	/**
	 * Delte a network list from the server.
	 */
	public void deleteNetlist(String listname)
	{
	}

	public ArrayList<Outage> getOutages()
	{
		return null;
	}

	public void assertOutages(ArrayList<Outage> outages)
	{
	}

	/* (non-Javadoc)
     * @see lrgs.rtstat.DdsClientIf#getServerHost()
     */
    //@Override
    public String getServerHost()
    {
	    // TODO Auto-generated method stub
	    return null;
    }
}
