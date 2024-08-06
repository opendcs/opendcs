package lrgs.rtstat;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.Properties;

import org.opendcs.gui.GuiConstants;
import org.opendcs.gui.PasswordWithShow;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import decodes.gui.AboutBox;
import decodes.gui.TopFrame;
import decodes.util.DecodesVersion;
import ilex.gui.EventsPanel;
import ilex.gui.JobDialog;
import ilex.gui.LoginDialog;
import ilex.util.AsciiUtil;
import ilex.util.AuthException;
import ilex.util.DesEncrypter;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;
import ilex.util.TextUtil;
import ilex.xml.DomHelper;
import lrgs.ldds.ServerError;
import lrgs.ldds.ProtocolError;
import lrgs.ldds.DdsUser;
import lrgs.ldds.LddsClient;
import lrgs.ldds.LddsMessage;
import lrgs.lrgsmain.LrgsConfig;
import lrgs.rtstat.hosts.LrgsConnection;
import lrgs.rtstat.hosts.LrgsConnectionComboBoxModel;
import lrgs.rtstat.hosts.LrgsConnectionPanel;
import lrgs.ddsrecv.DdsRecvSettings;
import lrgs.drgs.DrgsInputSettings;
import lrgs.db.Outage;
import lrgs.gui.MessageBrowser;
import ilex.util.LoadResourceBundle;


/**
Main frame for the LRGS Real-Time Status Application.
*/
public class RtStatFrame
	extends TopFrame
	implements DdsClientIf, HyperlinkListener
{
	private final static org.slf4j.Logger log = LoggerFactory.getLogger(RtStatFrame.class);
	private static ResourceBundle labels =
		RtStat.getLabels();
	private static ResourceBundle genericLabels =
		RtStat.getGenericLabels();
	private JPanel contentPane;
	private JMenuBar jMenuBar1 = new JMenuBar();
	private JMenu jMenuFile = new JMenu();
	private JMenuItem fileSetPasswordMenuItem = new JMenuItem();
	private JMenuItem fileUserAdmin = new JMenuItem();
	private JMenuItem fileLrgsConfig = new JMenuItem();
	private JMenuItem jMenuFileExit = new JMenuItem();
	private JMenuItem fileNetworkLists = new JMenuItem();
	private JMenu jMenuHelp = new JMenu();
	private JMenuItem jMenuHelpAbout = new JMenuItem();
	private BorderLayout borderLayout1 = new BorderLayout();
	private JPanel topPanel = new JPanel();
	private JLabel hostLabel = new JLabel();
	private JComboBox<LrgsConnection> hostCombo = new JComboBox<>();
	private PasswordWithShow passwordField = new PasswordWithShow(GuiConstants.DEFAULT_PASSWORD_WITH);
	private JButton pauseButton = new JButton();
	private JSplitPane jSplitPane1 = new JSplitPane();
	private RtStatPanel rtStatPanel = new RtStatPanel();
	private EventsPanel eventsPanel = new EventsPanel(false);
	private JCheckBox passwordCheck = new JCheckBox();
	private LrgsConnectionPanel connectionPanel = new LrgsConnectionPanel();

	/** True if the display is currently paused. */
	boolean isPaused = false;

	/** The object used to communicate with the server. */
	private LddsClient client;

	/** The background polling thread. */
	RtStatFrameThread myThread;

	/** The currently selected host name. */
	private String connectedHostName = "";

	private int dividerLoc;
	private int splitPaneHeight;

	/** the currently connected host -- null if not connected. */
	String host;
	int port;
	String user;
	String passwd;

	/** Dialog for editing users. */
	private UserListDialog userListDialog = null;

	private LrgsConfigDialog lrgsConfigDialog = null;
	private LrgsConfig lrgsConfig;  //  @jve:decl-index=0:
	private DdsRecvSettings ddsSettings;  //  @jve:decl-index=0:
	private DrgsInputSettings drgsSettings;
	private NetlistMaintenanceDialog netlistDlg;
	private DrgsInputSettings networkDcpSettings;
	private NetworkDcpStatusFrame networkDcpStatusFrame = null;

	private final JobDialog connectionJobDialog = new JobDialog(
									this,
									labels.getString("RtStatFrame.connectingToInfo") + host,
									true);

	/** Constructor. */
	public RtStatFrame(int scanPeriod, String iconFile, String headerFile)
	{
		exitOnClose = true;
		try
		{
			jbInit();
		}
		catch (Exception e)
		{
			log.error("RtStatFrame: ",e);
		}
		isPaused = false;
		client = null;
		passwordCheck.setSelected(false);
		passwordField.setEnabled(false);
		myThread = new RtStatFrameThread(this, scanPeriod,iconFile,headerFile);
		myThread.start();
		Dimension d = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
		d.width = 800;
		d.height -= 60;
		setSize(d);
		dividerLoc = d.height - 80 - 160;
		splitPaneHeight = 0;
		jSplitPane1.setDividerLocation(dividerLoc);
		jSplitPane1.addComponentListener(
			new ComponentAdapter()
			{
				public void componentResized(ComponentEvent e)
				{
					if (splitPaneHeight == 0)
					{
						splitPaneHeight = jSplitPane1.getHeight();
						return;
					}
					int newHeight = jSplitPane1.getHeight();
					int newLoc = newHeight - (splitPaneHeight - dividerLoc);
					jSplitPane1.setDividerLocation(newLoc);
					splitPaneHeight = newHeight;
					dividerLoc = newLoc;
				}
			});
		eventsPanel.addAncestorListener(
			new AncestorListener()
			{
				public void ancestorAdded(AncestorEvent ev) {}
				public void ancestorRemoved(AncestorEvent ev) {}
				public void ancestorMoved(AncestorEvent ev)
				{
					dividerLoc = jSplitPane1.getDividerLocation();
				}
			});

		//loadConnectionsField(hostCombo, connectionList, connectedHostName);
		netlistDlg = null;
		rtStatPanel.htmlPanel.addHyperlinkListener(this);

		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		final RtStatFrame myframe = this;
		addWindowListener(
			new WindowAdapter()
			{
				public void windowClosed(WindowEvent e)
				{
					if (myframe.getExitOnClose())
						System.exit(0);
				}
			});
		trackChanges("RtStat");
	}

	/** Initializes GUI components. */
	private void jbInit()
		throws Exception
	{
		boolean canConfig = true;
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		try { cl.loadClass("lrgs.ddsrecv.DdsRecvConnectCfg"); }
		catch(ClassNotFoundException ex) { canConfig = false; }



		contentPane = (JPanel)this.getContentPane();
		contentPane.setLayout(borderLayout1);
		this.setSize(new Dimension(793, 716));
		this.setTitle(
				labels.getString("RtStatFrame.frameTitle"));
		jMenuFile.setText(genericLabels.getString("file"));
		jMenuFileExit.setText(genericLabels.getString("exit"));
		jMenuFileExit.addActionListener(
			new java.awt.event.ActionListener()
			{
	    		public void actionPerformed(ActionEvent e)
	    		{
					jMenuFileExit_actionPerformed(e);
				}
			});
		fileSetPasswordMenuItem.setText(
				labels.getString("RtStatFrame.setPassword"));
		fileSetPasswordMenuItem.addActionListener(
			new java.awt.event.ActionListener()
			{
	    		public void actionPerformed(ActionEvent e)
	    		{
					fileSetPasswordMenuItem_actionPerformed();
				}
			});
		fileUserAdmin.setText(
			labels.getString("RtStatFrame.userAdministration"));
		fileUserAdmin.addActionListener(
			new java.awt.event.ActionListener()
			{
	    		public void actionPerformed(ActionEvent e)
	    		{
					fileUserAdmin_actionPerformed();
				}
			});


		if (canConfig)
		{
			fileLrgsConfig.setText(
					labels.getString("RtStatFrame.LRGSConfiguration"));
			fileLrgsConfig.addActionListener(
				new java.awt.event.ActionListener()
				{
		    		public void actionPerformed(ActionEvent e)
		    		{
						fileLrgsConfig_actionPerformed();
					}
				});

			fileNetworkLists.setText(
					labels.getString("RtStatFrame.networkLists"));
			fileNetworkLists.addActionListener(
				new java.awt.event.ActionListener()
				{
		    		public void actionPerformed(ActionEvent e)
		    		{
						fileNetworkLists_actionPerformed();
					}
				});
		}

		jMenuHelp.setText(
				genericLabels.getString("help"));
		jMenuHelpAbout.setText(
				genericLabels.getString("about"));
		jMenuHelpAbout.addActionListener(
			new java.awt.event.ActionListener()
			{
	    		public void actionPerformed(ActionEvent e)
	    		{
					jMenuHelpAbout_actionPerformed(e);
				}
			});

		hostLabel.setText(
				labels.getString("RtStatFrame.host"));
		topPanel.setLayout(new BorderLayout());

		pauseButton.setText(
				labels.getString("RtStatFrame.pause"));
		pauseButton.addActionListener(
			new java.awt.event.ActionListener()
			{
	    		public void actionPerformed(ActionEvent e)
	    		{
					pauseButton_actionPerformed(e);
	    		}
			});
		jSplitPane1.setOrientation(JSplitPane.VERTICAL_SPLIT);
		eventsPanel.setMaximumSize(new Dimension(32767, 150));
		eventsPanel.setMinimumSize(new Dimension(10, 120));
		eventsPanel.setPreferredSize(new Dimension(10, 120));
		jMenuFile.add(fileSetPasswordMenuItem);
		jMenuFile.add(fileUserAdmin);
		if (canConfig)
		{
			jMenuFile.add(fileLrgsConfig);
			jMenuFile.add(fileNetworkLists);
		}
		jMenuFile.addSeparator();
		jMenuFile.add(jMenuFileExit);
		jMenuHelp.add(jMenuHelpAbout);
		jMenuBar1.add(jMenuFile);
		jMenuBar1.add(jMenuHelp);
		contentPane.add(topPanel, BorderLayout.NORTH);
		topPanel.add(connectionPanel);
		connectionPanel.onConnect(c -> connectButton_actionPerformed(c));
		contentPane.add(jSplitPane1, BorderLayout.CENTER);
		jSplitPane1.add(rtStatPanel, JSplitPane.TOP);
		jSplitPane1.add(eventsPanel, JSplitPane.BOTTOM);
		this.setJMenuBar(jMenuBar1);
	}

	//File | Exit action performed
	private void jMenuFileExit_actionPerformed(ActionEvent e)
	{
		dispose();
		if (exitOnClose)
		{
			System.exit(0);
		}
	}

	//Help | About action performed
	private void jMenuHelpAbout_actionPerformed(ActionEvent e)
	{
		AboutBox dlg = new AboutBox(this, "LRGS", "Local Readout Ground Station");
		Dimension dlgSize = dlg.getPreferredSize();
		Dimension frmSize = getSize();
		Point loc = getLocation();
		dlg.setLocation((frmSize.width - dlgSize.width) / 2 + loc.x,
			(frmSize.height - dlgSize.height) / 2 + loc.y);
		dlg.setModal(true);
		dlg.setVisible(true);
	}

	public void connectButton_actionPerformed(LrgsConnection c)
	{
		final String thost = c.getHostName();
		if (thost.length() == 0)
		{
			showError(labels.getString("RtStatFrame.hostOrIpEmptyErr"));
			return;
		}

		final int p = c.getPort();
		if (p <= 0)
		{
			showError(labels.getString(	"RtStatFrame.portNumErr"));

		}
		port = p;

		user = c.getUsername();
		if (user.length() == 0)
		{
			showError(labels.getString("RtStatFrame.userEmptyErr"));
			return;
		}

		passwd = LrgsConnection.decryptPassword(c, LrgsConnectionPanel.pwk);

		if (passwd == null || passwd.length() == 0)
		{
			showError(labels.getString("RtStatFrame.passAuthErr"));
			return;
		}
		SwingUtilities.invokeLater(() -> setTitle(labels.getString("RtStatFrame.frameTitle")));

		host = thost;
		doConnect(c);
	}

	private void doConnect(LrgsConnection c)
	{
		closeConnection();
		client = null;	
		final LddsClient tclient = new LddsClient(host, port);

		connectionJobDialog.setCanCancel(true);		
		Thread backgroundJob =
			new Thread()
			{
				public void run()
				{
					pause(1000L);

					while(host != null && !connectionJobDialog.wasCancelled() && connectionJobDialog.isVisible())
					{
						connectionJobDialog.addToProgress(LoadResourceBundle.sprintf(
								labels.getString("RtStatFrame.connectingInfo"),
								host, port)
						);
						try
						{
							tclient.connect();
						}
						catch(Exception ex)
						{
							connectionJobDialog.addToProgress(
							labels.getString("RtStatFrame.cannotConnectErr")
							+ ex);
							tclient.disconnect();
							pause(5000L);
							continue;
						}
						try
						{
							connectionJobDialog.addToProgress(
								LoadResourceBundle.sprintf(
									labels.getString("RtStatFrame.authenticatingUserInfo"),
									user)
							);
							tclient.sendAuthHello(user, passwd);
							connectionJobDialog.addToProgress(labels.getString(
									"RtStatFrame.connectionSuccessInfo"));
							pause(2000L);
							connectionJobDialog.allDone();
							return;
						}
						catch(ServerError ex)
						{
							connectionJobDialog.addToProgress(labels.getString(
									"RtStatFrame.connectionRejectedErr")
									+ ex);
							tclient.disconnect();
						}
						catch(ProtocolError ex)
						{
							connectionJobDialog.addToProgress(
							labels.getString("RtStatFrame.protocolErr") + ex);
							tclient.disconnect();
						}
						catch(Exception ex)
						{
							connectionJobDialog.addToProgress(labels.getString(
							  "RtStatFrame.authenticationErr") + ex);
							tclient.disconnect();
						}
						// Pause 5 seconds and try again.
						pause(5000L);
					}
				}


			};
		backgroundJob.setName("LRGS Connection background thread.");
		backgroundJob.start();
		launch(connectionJobDialog);
		if (tclient.isConnected())
		{
			setTitle(labels.getString("RtStatFrame.frameTitle")+": " + host);
			client = tclient;
			displayEvent("Connected to " + host + ":" + port + " as user '" + user + "'");
			LrgsConnection newConnection = new LrgsConnection(c, new Date());
			connectionPanel.addOrUpdateConnection(newConnection);
		}
	}
	private static void pause(long millis)
	{
		try
		{
			Thread.sleep(millis);
		} catch(InterruptedException ex)
		{
			log.error("Error during pause({})",millis,ex);
		}
	}
	private void closeConnection()
	{
		if (client != null)
		{
			try
			{
			  client.sendGoodbye();
			} catch(Exception ex)
			{
				log.error("Error closing connection",ex);
			}
			client.disconnect();
		}
	}

	/**
	 * Called from the RtStatFrameThread, this polls the client for status
	 * and returns the byte buffer.
	 * It is in this class so it can be synchronized against connect calls.
	 * @return the server's status, null if not currently connected or paused.
	 */
	public synchronized byte[] getStatus()
	{
		if (client == null || isPaused)
		{
			return null;
		}
		try
		{
			return client.getStatus();
		}
		catch(final Exception ex)
		{
			log.error("Error getting status",ex);
			client.disconnect();
			client = null;
			String msg = "An error occurred in client.getStatus(). ";
			log.error(msg);
			rtStatPanel.updateStatus("<h1>"+msg+" "+ex.getMessage()+"</h1>");
			rtStatPanel.invalidate();
			return null;
		}
	}

	public synchronized ArrayList<DdsUser> getUsers()
		throws AuthException
	{
		if (client == null || isPaused)
		{
			throw new AuthException(labels.getString("RtStatFrame.listUsersErr"));
		}
		return client.getUsers();
	}

	public synchronized void modUser(DdsUser ddsUser, String pw)
		throws AuthException
	{
		if (client == null || isPaused)
		{
			throw new AuthException(labels.getString("RtStatFrame.listUsersErr"));
		}
		client.modUser(ddsUser, pw);
	}

	public void rmUser(String userName)
		throws AuthException
	{
		if (client == null || isPaused)
		{
			throw new AuthException(labels.getString("RtStatFrame.listUsersErr"));
		}
		client.rmUser(userName);
	}

	/**
	 * Called from the RtStatFrameThread, this polls the client for events
	 * and returns the array.
	 * It is in this class so it can be synchronized against connect calls.
	 * @return the server's events, null if not currently connected or paused.
	 */
	public synchronized String[] getEvents()
	{
		if (client == null || isPaused)
		{
			return null;
		}
		try
		{
			return client.getEvents();
		}
		catch(final Exception ex)
		{
			client.disconnect();
			client = null;
			SwingUtilities.invokeLater(
				new Runnable()
				{
					public void run()
					{
						doConnect(null);
					}
				});
			return null;
		}
	}

	/**
	 * Sends the new LrgsConfiguration to the server.
	 */
	public synchronized void applyLrgsConfig(LrgsConfig lrgsConfig)
		throws AuthException
	{
		// Convert lrgsConfig to properties-set byte array.
		Properties props = new Properties();
		PropertiesUtil.storeInProps(lrgsConfig, props, null);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try{
			props.store(baos, "LRGS Configuration Modified on "+new Date());
		}
		catch(IOException ex)
		{
			throw new AuthException(labels.getString("RtStatFrame.constructConfigArrayErr" +ex));
		}
		byte cfgData[] = baos.toByteArray();
		client.installConfig("lrgs", cfgData);
	}

	/**
	 * Sends the new DdsRecvSettings to the server.
	 */
	public synchronized void applyDdsRecvSettings(DdsRecvSettings settings)
		throws AuthException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try
		{
			settings.storeToXml(baos);
		}
		catch(IOException ex)
		{
			throw new AuthException(
				labels.getString("RtStatFrame.constructddsConfigArrayErr") + ex.getLocalizedMessage(),
				ex
			);
		}
		byte cfgData[] = baos.toByteArray();
		client.installConfig("ddsrecv", cfgData);
	}

	/**
	 * Sends the new DrgsInputSettings to the server.
	 */
	public synchronized void applyDrgsInputSettings(DrgsInputSettings settings)
		throws AuthException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try
		{
			settings.storeToXml(baos);
		}
		catch(IOException ex)
		{
			throw new AuthException(
				labels.getString("RtStatFrame.constructdrgsConfigArrayErr")	+ ex.getLocalizedMessage(),
				ex);
		}
		byte cfgData[] = baos.toByteArray();
		client.installConfig("drgs", cfgData);
	}

	public void applyNetworkDcpSettings(DrgsInputSettings settings)
		throws AuthException
	{
		// There was no networkDCP config prior to version 10.
		if (client.getServerProtoVersion() < 10)
		{
			return;
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try
		{
			settings.storeToXml(baos);
		}
		catch(IOException ex)
		{
			throw new AuthException(
				labels.getString("RtStatFrame.constructdrgsConfigArrayErr")	+ ex.getLocalizedMessage(),
				ex);
		}
		byte cfgData[] = baos.toByteArray();
		client.installConfig("networkDcp", cfgData);
	}

	/**
	 * Retrieve the 3 configurations from the server and store them locally.
	 */
	public synchronized void getConfigurations()
		throws AuthException
	{
		lrgsConfig = new LrgsConfig();
		ddsSettings =  DdsRecvSettings.instance();
		drgsSettings = new DrgsInputSettings();
		networkDcpSettings = new DrgsInputSettings();
		networkDcpSettings.savePollingPeriod = true;

		try
		{
			byte[] cfgData = client.getConfig("lrgs");
			ByteArrayInputStream bais = new ByteArrayInputStream(cfgData);
			Properties props = new Properties();
			props.load(bais);
			PropertiesUtil.loadFromProps(lrgsConfig, props);
		}
		catch(Exception ex)
		{
			String msg = LoadResourceBundle.sprintf(
				labels.getString("RtStatFrame.cannotReadLrgsConfErr"),
				host) + ex;
			showError(msg);
			throw new AuthException(msg);
		}

		try
		{
			byte[] cfgData = client.getConfig("ddsrecv");
			ByteArrayInputStream bais = new ByteArrayInputStream(cfgData);
			Document doc = DomHelper.readStream("rtstat", bais);
			ddsSettings.readNetworkLists = false;
			ddsSettings.setFromDoc(doc, "remote");
		}
		catch(Exception ex)
		{
			String msg = LoadResourceBundle.sprintf(
					labels.getString("RtStatFrame.cannotReadDdsConfErr"),
					host, ex);
			showError(msg);
		}

		try
		{
			byte[] cfgData = client.getConfig("drgs");
			ByteArrayInputStream bais = new ByteArrayInputStream(cfgData);
			Document doc = DomHelper.readStream("rtstat", bais);
			drgsSettings.setFromDoc(doc, "remote");
		}
		catch(Exception ex)
		{
			String msg = LoadResourceBundle.sprintf(
					labels.getString("RtStatFrame.cannotReadDrgsConfErr"),
					host, ex);
			showError(msg);
		}

		try
		{
			byte[] cfgData = client.getConfig("networkDcp");
			ByteArrayInputStream bais = new ByteArrayInputStream(cfgData);
			Document doc = DomHelper.readStream("rtstat", bais);
			networkDcpSettings.setFromDoc(doc, "remote");
		}
		catch(Exception ex)
		{
			String msg = LoadResourceBundle.sprintf(
					labels.getString("RtStatFrame.cannotReadNetworkDcpConfErr"),
					host, ex);
			Logger.instance().warning(msg);
		}
	}



	/**
	 * @return a list of network lists that exist on the server.
	 */
	public synchronized String[] getNetlistList()
		throws AuthException
	{
		try
		{
			byte data[] = client.getConfig("netlist-list");
			StringTokenizer st = new StringTokenizer(new String(data));
			String ret[] = new String[st.countTokens()];
			for(int i=0; st.hasMoreTokens(); i++)
				ret[i] = st.nextToken();
			return ret;
		}
		catch(Exception ex)
		{
			String msg = LoadResourceBundle.sprintf(
				labels.getString("RtStatFrame.cannotGetNlListErr"),
				host) + ex;
			showError(msg);
			throw new AuthException(msg);
		}
	}
	/** from DdsClientIf interface */
	public String getServerHost()
	{
		if (client == null) return "none";
		return client.getHost();
	}

	/**
	 * @return the data in a particular network list.
	 */
	public synchronized byte[] getNetlist(String listname)
		throws AuthException
	{
		try
		{
			return client.getConfig("netlist:" + listname);
		}
		catch(Exception ex)
		{
			String msg = LoadResourceBundle.sprintf(
					labels.getString("RtStatFrame.cannotGetNlErr"),
					host) + ex;
			showError(msg);
			throw new AuthException(msg);
		}
	}

	/**
	 * Install a network list on the server.
	 */
	public synchronized void installNetlist(String listname, byte[] data)
		throws AuthException
	{
		client.installConfig("netlist:" + listname, data);
	}

	/**
	 * Delte a network list from the server.
	 */
	public synchronized void deleteNetlist(String listname)
		throws AuthException
	{
		client.installConfig("netlist-delete:" + listname, null);
	}

	/**
	 * @return list of outages from the server.
	 */
	public ArrayList<Outage> getOutages()
		throws AuthException
	{
		try
		{
			return client.getOutages(null, null);
		}
		catch(Exception ex)
		{
			throw new AuthException(labels.getString(
					"RtStatFrame.cannotReadOutagesErr") + ex);
		}
	}

	/**
	 * Assert (or reassert) outages.
	 * @param outages the outages
	 */
	public void assertOutages(ArrayList<Outage> outages)
		throws AuthException
	{
		try
		{
			byte data[] = client.getOutageXmlParser().outages2xml(outages);
			LddsMessage msg = new LddsMessage(LddsMessage.IdAssertOutages, "");
			msg.MsgData = data;
			msg.MsgLength = data.length;
			LddsMessage resp = client.serverExec(msg);
		}
		catch(Exception ex)
		{
			throw new AuthException(
				labels.getString("RtStatFrame.assertOutagesErr") + ex.getLocalizedMessage(),
				ex);
		}
	}

	private void pauseButton_actionPerformed(ActionEvent e)
	{
		if (!isPaused)
		{
			isPaused = true;
			pauseButton.setText(labels.getString("RtStatFrame.resume"));
		}
		else
		{
			isPaused = false;
			pauseButton.setText(labels.getString("RtStatFrame.pause"));
		}
	}

	private void passwordCheck_actionPerformed(ActionEvent e)
	{
		passwordField.setEnabled(passwordCheck.isSelected());
	}

	/**
	* Shows an error message in a modal dialog and prints it to stderr.
	* This is a convenience method.
	* @param msg the message
	*/
	public void showError( String msg )
	{
		System.err.println(msg);
		JOptionPane.showMessageDialog(this,
			AsciiUtil.wrapString(msg, 60), "Error!", JOptionPane.ERROR_MESSAGE);
	}

	/**
	 * Called from within the GUI thread after a new status XML block has been
	 * received and parsed. The passed string is the formatted HTML to be
	 * displayed in the window.
	 * @param htmlstat the status as a block of HTML.
	 */
	public void updateStatus(String htmlstat, String networkDcpStatus)
	{
		rtStatPanel.updateStatus(htmlstat);
		NetworkDcpStatusFrame ndsf = networkDcpStatusFrame;
		if (ndsf != null)
			ndsf.updateStatus(networkDcpStatus);
	}
	public void updateNetworkDcpStatus(String nds)
	{

	}
	/** Called from within the GUI thread to display an event. */
	public void displayEvent(String event)
	{
		eventsPanel.addLine(event);
	}

	/**
	  Sets the host name programmatically.
	  Must be called from within the GUI thread.
	*/
	public void setHost(String hostname)
	{
		int n = hostCombo.getItemCount();
		for(int i=0; i<n; i++)
		{
			LrgsConnection c = (LrgsConnection)hostCombo.getItemAt(i);
			if (hostname.equalsIgnoreCase(c.getHostName()))
			{
				hostCombo.setSelectedIndex(i);
				return;
			}
		}
		hostCombo.addItem(new LrgsConnection(hostname, 16003, null, null, null));
		hostCombo.setSelectedIndex(n);
	}

	public void fileSetPasswordMenuItem_actionPerformed()
	{
		if (client == null)
		{
			showError("Connect to server before attempting to change password.");
			return;
		}
		if (!client.isAuthenticated())
		{
			showError("You must login with the old password before you can change the password.");
			return;
		}

		LoginDialog ld = new LoginDialog(this,
				labels.getString("RtStatFrame.modifyPassTitle"), true);
		ld.setEditableUsername(false, user);
		launch(ld);
		if (ld.isOK())
		{
			char pw[] = ld.getPassword();
			try
			{
				DdsUser du = new DdsUser(user);
				modUser(du, new String(pw));
			}
			catch(Exception ex)
			{
				String msg = labels.getString("RtStatFrame.changePassErr")+ex;
				JOptionPane.showMessageDialog(this,
					AsciiUtil.wrapString(msg, 60), "Error!",
					JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	public void fileUserAdmin_actionPerformed()
	{
		if (client == null || !client.isConnected())
		{
			showError("Not connected.");
			return;
		}

		if (!client.isAuthenticated())
		{
			showError(labels.getString(
				"RtStatFrame.loginAsAdminErr"));
			return;
		}

		if (userListDialog == null)
		{
			userListDialog = new UserListDialog(this,
				LoadResourceBundle.sprintf(
				labels.getString("RtStatFrame.ddsUserTitle"),host), true);
			userListDialog.setDdsClientIf(this);
		}
		userListDialog.setHost(host);

		// Retrieve user list from server.
		try
		{
			ArrayList<DdsUser> userList = getUsers();

			// Non-administrators can see, and modify certain fields, in their own
			// user record. Jump directly to UserEditDialog with isAdmin=false.
			if (userList.size() == 1
			 && TextUtil.strEqual(client.getUserName(), userList.get(0).userName)
			 && !userList.get(0).isAdmin())
			{
				EditUserDialog editUserDialog = new EditUserDialog(null,
					labels.getString("UserListDialog.modUserDataTitle"), true, false);

				editUserDialog.set(host, userList.get(0), false);
				boolean done = false;
				int tries=0;
				while(!done && tries++ < 5)
				{
					launch(editUserDialog);
					if (editUserDialog.okPressed())
					{
						try
						{
							client.modUser(userList.get(0), editUserDialog.getPassword());
							done = true;
						}
						catch(AuthException ex)
						{
							JOptionPane.showMessageDialog(this,
			            		AsciiUtil.wrapString(ex.toString(),60),
								"Error!", JOptionPane.ERROR_MESSAGE);
							done = false;
						}
					}
					else
					{
						done = true;
					}
				}
			}
			else
			{
				userListDialog.setUsers(userList);
				launch(userListDialog);
			}
		}
		catch(AuthException ex)
		{
			showError(labels.getString("RtStatFrame.noPermissionOnServerErr")
					+ ex.toString());
		}
	}

	private void launch(JDialog dlg)
	{
		Dimension frameSize = this.getSize();
		Point frameLoc = this.getLocation();
		Dimension dlgSize = dlg.getPreferredSize();
		int xo = (frameSize.width - dlgSize.width) / 2;
		if (xo < 0)
		{
			xo = 0;
		}
		int yo = (frameSize.height - dlgSize.height) / 2;
		if (yo < 0)
	 	{
			yo = 0;
		}

		dlg.setLocation(frameLoc.x + xo, frameLoc.y + yo);
		if (SwingUtilities.isEventDispatchThread())
		{
			dlg.setVisible(true);
		}
		else
		{
			try
			{
				SwingUtilities.invokeAndWait(() -> dlg.setVisible(true));
			}
			catch (InvocationTargetException | InterruptedException ex)
			{
				log.atError()
				   .setCause(ex)
				   .log("Error waiting for dialog to return.");
			}
		}
	}

	public void fileLrgsConfig_actionPerformed()
	{
		if (client == null)
		{
			showError("Not connected.");
			return;
		}

		if (!client.isAuthenticated())
		{
			showError(labels.getString(
				"RtStatFrame.loginAsAdminErr"));
			return;
		}

		if (lrgsConfigDialog == null)
		{
			lrgsConfigDialog = new LrgsConfigDialog(this,
				labels.getString(
				"RtStatFrame.lrgsConfigTitle") + connectedHostName);
		}
		else
		{
			lrgsConfigDialog.setTitle(labels.getString(
				"RtStatFrame.lrgsConfigTitle") + connectedHostName);
		}
		lrgsConfigDialog.setDdsClientIf(this);
		lrgsConfigDialog.clear();
		try
		{
			getConfigurations();
		}
		catch(AuthException ex)
		{
			return;
		}
		lrgsConfigDialog.setConfig(lrgsConfig, ddsSettings, drgsSettings,
			networkDcpSettings);
		launch(lrgsConfigDialog);
	}

	private void fileNetworkLists_actionPerformed()
	{
		if (client == null || !client.isAuthenticated())
		{
			showError(labels.getString(
				"RtStatFrame.loginAsAdminErr"));
			return;
		}

		try
		{
			if (netlistDlg == null)
			{
				netlistDlg = new NetlistMaintenanceDialog(this);
			}
			else if (netlistDlg.isVisible())
			{
				netlistDlg.toFront();
				return;
			}
			String nll[] = getNetlistList();
			netlistDlg.startDialog(this, nll);
			launch(netlistDlg);
		}
		catch(AuthException ex)
		{
			showError(labels.getString("RtStatFrame.noPermissionOnServerErr")
					+ ex.toString());
		}
	}

	public void hyperlinkUpdate(HyperlinkEvent hevt)
	{
		if (hevt != null)
		{
			if (hevt.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
			{
				URL url = hevt.getURL();
				String f = url.getFile();
				if (f == null)
				{
					return;
				}
				if (f.contains("showNetworkDcps"))
				{
					showNetworkDcps();
				}
			}
		}
	}

	private void showNetworkDcps()
	{
		if (networkDcpStatusFrame == null)
		{
			networkDcpStatusFrame = new NetworkDcpStatusFrame(this);
			networkDcpStatusFrame.setVisible(true);
		}
		else
		{
			networkDcpStatusFrame.toFront();
		}
	}

	public void networkDcpStatusFrameClosed()
	{
		networkDcpStatusFrame = null;
	}
}
