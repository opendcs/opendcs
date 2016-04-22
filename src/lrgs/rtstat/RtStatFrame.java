/*
* $Id$
*/
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
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.Enumeration;
import java.util.Properties;

import org.w3c.dom.Document;

import decodes.gui.AboutBox;
import decodes.gui.TopFrame;
import ilex.gui.EventsPanel;
import ilex.gui.JobDialog;
import ilex.gui.LoginDialog;
import ilex.util.AsciiUtil;
import ilex.util.AuthException;
import ilex.util.EnvExpander;
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
import lrgs.ddsrecv.DdsRecvSettings;
import lrgs.drgs.DrgsInputSettings;
import lrgs.db.Outage;
import ilex.util.LoadResourceBundle;


/**
Main frame for the LRGS Real-Time Status Application.
*/
public class RtStatFrame
	extends TopFrame
	implements DdsClientIf, HyperlinkListener
{
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
	private JMenuItem fileOutageMaintenance = new JMenuItem();
	private JMenuItem fileNetworkLists = new JMenuItem();
	private JMenu jMenuHelp = new JMenu();
	private JMenuItem jMenuHelpAbout = new JMenuItem();
	private BorderLayout borderLayout1 = new BorderLayout();
	private JPanel topPanel = new JPanel();
	private JLabel hostLabel = new JLabel();
	private JComboBox hostCombo = new JComboBox();
	private JLabel portLabel = new JLabel();
	private JTextField portField = new JTextField(6);
	private JLabel userLabel = new JLabel();
	JTextField userField = new JTextField(8);
	private JPasswordField passwordField = new JPasswordField(12);
	private JButton connectButton = new JButton();
	private GridBagLayout gridBagLayout1 = new GridBagLayout();
	private JButton pauseButton = new JButton();
	private JSplitPane jSplitPane1 = new JSplitPane();
	private RtStatPanel rtStatPanel = new RtStatPanel();
	private EventsPanel eventsPanel = new EventsPanel(false);
	private JCheckBox passwordCheck = new JCheckBox();

	/** True if the display is currently paused. */
	boolean isPaused = false;

	/** The object used to communicate with the server. */
	private LddsClient client;

	/** The background polling thread. */
	RtStatFrameThread myThread;

	/** The currently selected host name. */
	private String connectedHostName = "";

	/** List of historical DDS connections. */
	private Properties connectionList = new Properties();

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

	/** Constructor. */
	public RtStatFrame(int scanPeriod, String iconFile, String headerFile)
	{
//		enableEvents(AWTEvent.WINDOW_EVENT_MASK);
		exitOnClose = true;
		try
		{
			jbInit();
		}
		catch (Exception e)
		{
			e.printStackTrace();
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
		//loadConnectionsCombo();
		hostCombo.setEditable(true);
		hostCombo.addActionListener(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent ae)
				{
					setPortFromHostSelection();
				}
			});
		loadConnectionsField(hostCombo, connectionList, connectedHostName);
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
	
			fileOutageMaintenance.setText(
					labels.getString("RtStatFrame.outages"));
			fileOutageMaintenance.addActionListener(
				new java.awt.event.ActionListener()
				{
		    		public void actionPerformed(ActionEvent e)
		    		{
						fileOutageMaintenance_actionPerformed();
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
		topPanel.setLayout(gridBagLayout1);
		portLabel.setText(
				labels.getString("RtStatFrame.port"));
//		portField.setMinimumSize(new Dimension(60, 23));
//		portField.setPreferredSize(new Dimension(60, 23));
		portField.setText("16003");
		userLabel.setText(
				labels.getString("RtStatFrame.user"));
//		userField.setMinimumSize(new Dimension(90, 23));
//		userField.setPreferredSize(new Dimension(90, 23));
//		passwordField.setMinimumSize(new Dimension(90, 23));
//		passwordField.setPreferredSize(new Dimension(90, 23));
		connectButton.setText(
				labels.getString("RtStatFrame.connectButton"));
//		connectButton.setMinimumSize(new Dimension(100, 27));
//		connectButton.setPreferredSize(new Dimension(100, 27));
		connectButton.addActionListener(
			new java.awt.event.ActionListener()
			{
	    		public void actionPerformed(ActionEvent e)
	    		{
					connectButton_actionPerformed(e);
	    		}
			});
//		pauseButton.setMinimumSize(new Dimension(100, 27));
//		pauseButton.setPreferredSize(new Dimension(100, 27));
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
		passwordCheck.setText(
				labels.getString("RtStatFrame.password"));
		passwordCheck.addActionListener(
			new java.awt.event.ActionListener()
			{
	    		public void actionPerformed(ActionEvent e)
	    		{
					passwordCheck_actionPerformed(e);
	    		}
			});
		jMenuFile.add(fileSetPasswordMenuItem);
		jMenuFile.add(fileUserAdmin);
		if (canConfig)
		{
			jMenuFile.add(fileLrgsConfig);
			jMenuFile.add(fileOutageMaintenance);
			jMenuFile.add(fileNetworkLists);
		}
		jMenuFile.addSeparator();
		jMenuFile.add(jMenuFileExit);
		jMenuHelp.add(jMenuHelpAbout);
		jMenuBar1.add(jMenuFile);
		jMenuBar1.add(jMenuHelp);
		contentPane.add(topPanel, BorderLayout.NORTH);
		topPanel.add(hostLabel,  new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
	        ,GridBagConstraints.WEST, GridBagConstraints.NONE, 
			new Insets(4, 5, 4, 1), 0, 0));
		topPanel.add(hostCombo,  new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
	        ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
			new Insets(4, 0, 4, 6), 0, 0));
		topPanel.add(portLabel,   new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0
	        ,GridBagConstraints.EAST, GridBagConstraints.NONE, 
			new Insets(4, 6, 4, 1), 0, 0));
		topPanel.add(portField,  new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0
	        ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
			new Insets(4, 0, 4, 6), 0, 0));
		topPanel.add(userLabel,   new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0
	        ,GridBagConstraints.EAST, GridBagConstraints.NONE, 
			new Insets(4, 6, 4, 1), 0, 0));
		topPanel.add(userField,  new GridBagConstraints(5, 0, 1, 1, 0.5, 0.0
	        ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
			new Insets(4, 0, 4, 6), 0, 0));
		topPanel.add(passwordCheck,  new GridBagConstraints(6, 0, 1, 1, 0.0, 0.0
	        ,GridBagConstraints.EAST, GridBagConstraints.NONE, 
			new Insets(4, 6, 4, 1), 0, 0));
		topPanel.add(passwordField,  new GridBagConstraints(8, 0, 1, 1, 0.5, 0.0
	        ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
			new Insets(4, 0, 4, 6), 0, 0));
		topPanel.add(connectButton,  new GridBagConstraints(9, 0, 1, 1, 0.0, 0.0
	        ,GridBagConstraints.CENTER, GridBagConstraints.NONE, 
			new Insets(4, 20, 4, 0), 0, 0));
		topPanel.add(pauseButton,  new GridBagConstraints(10, 0, 1, 1, 0.0, 0.0
	        ,GridBagConstraints.CENTER, GridBagConstraints.NONE, 
			new Insets(4, 10, 4, 5), 0, 0));
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
			System.exit(0);
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
		dlg.show();
	}

	//Overridden so we can exit when window is closed
//	protected void processWindowEvent(WindowEvent e)
//	{
//		if (exitOnClose)
//			System.exit(0);
//		super.processWindowEvent(e);
//		if (e.getID() == WindowEvent.WINDOW_CLOSING)
//		{
//			jMenuFileExit_actionPerformed(null);
//		}
//	}

	public synchronized void connectButton_actionPerformed(ActionEvent e)
	{
		final String thost = ((String)hostCombo.getSelectedItem()).trim();
		if (thost.length() == 0)
		{
			showError(labels.getString("RtStatFrame.hostOrIpEmptyErr"));
			return;
		}

		int p = 16003;
		String ps = portField.getText().trim();
		if (ps.length() > 0)
		{
			try { p = Integer.parseInt(ps); }
			catch(NumberFormatException ex)
			{
				showError(labels.getString(	"RtStatFrame.portNumErr"));
				return;
			}
		}
		port = p;

		user = userField.getText().trim();
		if (user.length() == 0)
		{
			showError(
					labels.getString("RtStatFrame.userEmptyErr"));
			return;
		}

		passwd = null;
		if (passwordCheck.isSelected())
		{
			passwd = passwordField.getText().trim();
			if (passwd.length() == 0)
			{
				showError(
						labels.getString("RtStatFrame.passAuthErr"));
				return;
			}
		}

		setTitle(labels.getString("RtStatFrame.frameTitle"));
		host = thost;
		doConnect();
	}

	private void doConnect()
	{
		if (client != null)
		{
			try { client.sendGoodbye(); } catch(Exception ex) {}
			client.disconnect();
		}
		client = null;
		final LddsClient tclient = new LddsClient(host, port);
		final JobDialog dlg =
			new JobDialog(this, 
				labels.getString("RtStatFrame.connectingToInfo") + host, true);
		dlg.setCanCancel(true);

		Thread backgroundJob =
			new Thread()
			{
				public void run()
				{
					try { sleep(1000L); } catch(InterruptedException ex) {}
					while(host != null && !dlg.wasCancelled()
						&& dlg.isVisible())
					{
						dlg.addToProgress(LoadResourceBundle.sprintf(
								labels.getString("RtStatFrame.connectingInfo"),
							host, port));
						try { tclient.connect(); }
						catch(Exception ex)
						{
							dlg.addToProgress(
							labels.getString("RtStatFrame.cannotConnectErr")
							+ ex);
							tclient.disconnect();
							try { sleep(5000L); } 
							catch(InterruptedException ex2) {}
							continue;
						}
						try
						{
							if (passwd == null)
							{
								dlg.addToProgress(LoadResourceBundle.sprintf(
								labels.getString("RtStatFrame.sendingUserInfo"),
								user));
								tclient.sendHello(user);
							}
							else
							{
								dlg.addToProgress(
								LoadResourceBundle.sprintf(
								labels.getString(
								"RtStatFrame.authenticatingUserInfo"),
								user));
								tclient.sendAuthHello(user, passwd);
							}
							dlg.addToProgress(labels.getString(
									"RtStatFrame.connectionSuccessInfo"));
							try { sleep(2000L); } 
							catch(InterruptedException ex) {}
							dlg.allDone();
							return;
						}
						catch(ServerError ex)
						{
							dlg.addToProgress(labels.getString(
									"RtStatFrame.connectionRejectedErr") 
									+ ex);
							tclient.disconnect();
						}
						catch(ProtocolError ex)
						{
							dlg.addToProgress(
							labels.getString("RtStatFrame.protocolErr") + ex);
							tclient.disconnect();
						}
						catch(Exception ex)
						{
							dlg.addToProgress(labels.getString(
							  "RtStatFrame.authenticationErr") + ex);
							tclient.disconnect();
						}
						// Pause 5 seconds and try again.
						try { sleep(5000L); } 
						catch(InterruptedException ex) {}
					}
				}
			};
		backgroundJob.start();
		launch(dlg);
		if (tclient.isConnected())
		{
			setTitle(labels.getString("RtStatFrame.frameTitle")+": " + host);
			client = tclient;
			updateConnectionList(host, "" + port, user);
			displayEvent("Connected to " + host + ":"
				+ port + " as user '" + user + "'");
		}

		//rtStatPanel.setPage("file:///C:/tmp/lrgsstatus.html");
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
			return null;
		try { return client.getStatus(); }
		catch(final Exception ex)
		{
			client.disconnect();
			client = null;
			SwingUtilities.invokeLater(
				new Runnable()
				{
					public void run()
					{
						doConnect();
					}
				});
			return null;
		}
	}

	public synchronized ArrayList<DdsUser> getUsers()
		throws AuthException
	{
		if (client == null || isPaused)
			throw new AuthException(
					labels.getString("RtStatFrame.listUsersErr"));
		return client.getUsers();
	}

	public synchronized void modUser(DdsUser ddsUser, String pw)
		throws AuthException
	{
		if (client == null || isPaused)
			throw new AuthException(
					labels.getString("RtStatFrame.listUsersErr"));
		client.modUser(ddsUser, pw);
	}

	public void rmUser(String userName)
		throws AuthException
	{
		if (client == null || isPaused)
			throw new AuthException(
					labels.getString("RtStatFrame.listUsersErr"));
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
			return null;
		try { return client.getEvents(); }
		catch(final Exception ex)
		{
			client.disconnect();
			client = null;
			SwingUtilities.invokeLater(
				new Runnable()
				{
					public void run()
					{
						doConnect();
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
		try { props.store(baos, "LRGS Configuration Modified on "+new Date());}
		catch(IOException ex)
		{
			throw new AuthException(
				labels.getString("RtStatFrame.constructConfigArrayErr" +ex));
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
		try { settings.storeToXml(baos); }
		catch(IOException ex)
		{
			throw new AuthException(
				labels.getString("RtStatFrame.constructddsConfigArrayErr")
				+ ex);
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
		try { settings.storeToXml(baos); }
		catch(IOException ex)
		{
			throw new AuthException(
				labels.getString("RtStatFrame.constructdrgsConfigArrayErr")
				+ ex);
		}
		byte cfgData[] = baos.toByteArray();
		client.installConfig("drgs", cfgData);
	}

	public void applyNetworkDcpSettings(DrgsInputSettings settings)
		throws AuthException
	{
		// There was no networkDCP config prior to version 10.
		if (client.getServerProtoVersion() < 10)
			return;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try { settings.storeToXml(baos); }
		catch(IOException ex)
		{
			throw new AuthException(
				labels.getString("RtStatFrame.constructdrgsConfigArrayErr")
				+ ex);
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
		try { return client.getOutages(null, null); }
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
			throw new AuthException(labels.getString(
					"RtStatFrame.assertOutagesErr") + ex);
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

	public static void loadConnectionsField(JComboBox hostCombo, Properties connectionList,
		String connectedHostName)
	{
		String fn = LddsClient.getLddsConnectionsFile();
		File file = new File(fn);
		try
		{
			FileInputStream fis = new FileInputStream(file);
			connectionList.clear();
			connectionList.load(fis);
			fis.close();
		}
		catch(IOException ioe)
		{
			System.out.println("No previously recorded connections");
		}
		
		class ht 
			implements Comparable<ht>
		{
			long lastUse;
			String hostname;
			@Override
			public int compareTo(ht o)
			{
				if (lastUse > o.lastUse)
					return -1;
				else if (lastUse < o.lastUse)
					return 1;
				return hostname.compareTo(o.hostname);
			}
			public ht(long lastUse, String hostname)
			{
				super();
				this.lastUse = lastUse;
				this.hostname = hostname;
			}
		}
		ArrayList<ht> hosts = new ArrayList<ht>();
		for(Object key : connectionList.keySet())
		{
			String host = (String)key;
			String v = connectionList.getProperty(host);
			long lastUse = 0L;
			String f[] = v.split(" ");
			if (f != null && f.length >= 3)
				try { lastUse = Long.parseLong(f[2]); }
				catch(NumberFormatException ex) {}
			hosts.add(new ht(lastUse, host));
		}
		Collections.sort(hosts);
		int selected = -1;
		for(int i = 0; i<hosts.size(); i++)
		{
			ht x = hosts.get(i);
			hostCombo.addItem(x.hostname);
			if (connectedHostName != null && connectedHostName.equalsIgnoreCase(x.hostname))
				selected = i;
		}
		
//		Enumeration enames = connectionList.propertyNames();
//		int i = 0;
//		hostCombo.removeAllItems();
//		for(; enames.hasMoreElements(); i++)
//		{
//			String s = (String)enames.nextElement();
//			if (connectedHostName != null 
//			 && connectedHostName.equalsIgnoreCase(s))
//				selected = i;
//			hostCombo.addItem(s);
//		}
		if (selected == -1)
		{
			hostCombo.addItem(connectedHostName);
			selected = hosts.size();
		}
	}

	private void setPortFromHostSelection()
	{
		String host = (String)hostCombo.getSelectedItem();
		if (host == null || host.length() == 0)
			return;
		String hl = connectionList.getProperty(host);
		if (hl == null || hl.length() == 0)
		{
			portField.setText("16003");
			return;
		}
		StringTokenizer st = new StringTokenizer(hl);
		if (st.hasMoreTokens())
			portField.setText(st.nextToken());
		else
			portField.setText("16003");
		if (st.hasMoreTokens())
			userField.setText(st.nextToken());
	}

	/** 
	  Updates the connection list file with a new connection.
	*/
	private void updateConnectionList(String host, String port, String user)
	{
		connectedHostName = host;
		connectionList.setProperty(host, port + " " + user + " " + System.currentTimeMillis());
		String fn = LddsClient.getLddsConnectionsFile();
		File file = new File(fn);
		try
		{
			FileOutputStream fos = new FileOutputStream(file);
			connectionList.store(fos, "Recent LRGS Connections");
			fos.close();
		}
		catch(IOException ioe)
		{
			System.out.println("Cannot save connections to '"
				+ file.getPath() + "': " + ioe);
		}
		loadConnectionsField(hostCombo, connectionList, connectedHostName);
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
			String h = (String)hostCombo.getItemAt(i);
			if (hostname.equalsIgnoreCase(h))
			{
				hostCombo.setSelectedIndex(i);
				return;
			}
		}
		hostCombo.addItem(hostname);
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
//System.out.println("Modifying password");
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
						done = true;
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
		if (xo < 0) xo = 0;
		int yo = (frameSize.height - dlgSize.height) / 2;
		if (yo < 0) yo = 0;
		
		dlg.setLocation(frameLoc.x + xo, frameLoc.y + yo);
		dlg.setVisible(true);
	}

	public void fileLrgsConfig_actionPerformed()
	{
		if (client == null)
		{
			showError("Not connected.");
			return;
			
		}
		if ( !client.isAuthenticated())
		{
			showError(labels.getString(
				"RtStatFrame.loginAsAdminErr"));
			return;
		}

		if (lrgsConfigDialog == null)
			lrgsConfigDialog = new LrgsConfigDialog(this,
				labels.getString(
				"RtStatFrame.lrgsConfigTitle") + connectedHostName);
		lrgsConfigDialog.setDdsClientIf(this);
		lrgsConfigDialog.clear();
		try { getConfigurations(); }
		catch(AuthException ex) 
		{
			return;
		}
		lrgsConfigDialog.setConfig(lrgsConfig, ddsSettings, drgsSettings,
			networkDcpSettings);
		launch(lrgsConfigDialog);
	}

	private void fileOutageMaintenance_actionPerformed()
	{
		if (!client.isAuthenticated())
		{
			showError(labels.getString(
				"RtStatFrame.loginAsAdminErr"));
			return;
		}
		try
		{
			ArrayList<Outage> data = getOutages();
			OutageMaintenanceDialog dlg = new OutageMaintenanceDialog(this);
			dlg.startDialog(this, data);
			launch(dlg);
		}
		catch(AuthException ex)
		{
			showError(
					labels.getString("RtStatFrame.noPermissionOnServerErr")
					+ ex.toString());
		}
	}

	private void fileNetworkLists_actionPerformed()
	{
		if (!client.isAuthenticated())
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
					return;
				if (f.contains("showNetworkDcps"))
					showNetworkDcps();
			}
		}
	}
	
	private void showNetworkDcps()
	{
		if (networkDcpStatusFrame == null)
		{
			networkDcpStatusFrame = new NetworkDcpStatusFrame(this);
			networkDcpStatusFrame.setVisible(true);
			//System.out.println("Created networkDcpStatusFrame");
		}
		else
			networkDcpStatusFrame.toFront();
	}
	
	public void networkDcpStatusFrameClosed()
	{
		networkDcpStatusFrame = null;
		//System.out.println("networkDcpStatusFrameClosed");
	}
}
