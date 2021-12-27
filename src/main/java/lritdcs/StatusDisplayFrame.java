/*
 *	$Id$
 *
 *  $Log$
 *  Revision 1.9  2013/06/12 13:00:53  mmaloney
 *  dev
 *
 *  Revision 1.8  2012/12/12 18:44:24  mmaloney
 *  Fix UI Thread problem on config requests.
 *
 *  Revision 1.7  2012/12/12 16:08:52  mmaloney
 *  Mods for 5.2
 *
 *  Revision 1.6  2012/12/12 16:01:31  mmaloney
 *  Several updates for 5.2
 *
 */
package lritdcs;

import ilex.cmdline.IntegerToken;
import ilex.cmdline.StdAppSettings;
import ilex.cmdline.TokenOptions;
import ilex.gui.EventsPanel;
import ilex.net.BasicClient;
import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;
import ilex.xml.XmlOutputStream;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TimeZone;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import lrgs.common.SearchCriteria;
import lrgs.common.SearchSyntaxException;
import lrgs.nledit.NetlistEditFrame;
import lrgs.rtstat.RtStatPanel;


public class StatusDisplayFrame extends JFrame {
	boolean shutdownFlag = false;

	JMenuBar jMenuBar1 = new JMenuBar();
	JMenu jMenu1 = new JMenu();
	JMenuItem fileExit = new JMenuItem();
	JMenu jMenu2 = new JMenu();
	JMenuItem miEditHighCriteria = new JMenuItem();
	JMenuItem miEditMediumCriteria = new JMenuItem();
	JMenuItem miEditLowCriteria = new JMenuItem();
	JMenu jMenu3 = new JMenu();
	JMenuItem helpAbout = new JMenuItem();
	TitledBorder titledBorder1;
	
	TitledBorder titledBorder2;
	TitledBorder titledBorder3;
	BorderLayout borderLayout1 = new BorderLayout();  //  @jve:decl-index=0:
	BorderLayout borderLayout2 = new BorderLayout(); // @jve:decl-index=0:

	GridBagLayout gridBagLayout2 = new GridBagLayout();
	BorderLayout borderLayout3 = new BorderLayout();
	GridBagLayout gridBagLayout4 = new GridBagLayout();
	
	EventsPanel eventsPanel = new ilex.gui.EventsPanel();
	GridBagLayout gridBagLayout3 = new GridBagLayout();
	GridBagLayout gridBagLayout5 = new GridBagLayout();
	GridBagLayout gridBagLayout6 = new GridBagLayout();
	JMenu jMenu5 = new JMenu();
	JMenuItem miQueueFlushManual = new JMenuItem();
	JMenuItem miQueueFlushAuto = new JMenuItem();
	JMenuItem miQueueFlushHigh = new JMenuItem();
	JMenuItem miQueueFlushMedium = new JMenuItem();
	JMenuItem miQueueFlushLow = new JMenuItem();
	JMenuItem miQueueFlushAll = new JMenuItem();
	JMenuItem miFileOptions = new JMenuItem();
	JMenuItem miQueueManRetrans = new JMenuItem();
	GridBagLayout gridBagLayout1 = new GridBagLayout();
	TitledBorder titledBorder4;  //  @jve:decl-index=0:
	Border border3;
	ImageIcon logoImage = new ImageIcon(EnvExpander
			.expand("$LRITDCS_HOME/bin/ilexlogo.png")); // @jve:decl-index=0:
	BorderLayout borderLayout4 = new BorderLayout();

	LritDcsStatus myStatus;
	SimpleDateFormat myDateFormat;
	
	SearchCriteria scHigh = new SearchCriteria();
	SearchCriteria scMedium = new SearchCriteria();
	SearchCriteria scLow = new SearchCriteria();
	SearchCriteria scManual = new SearchCriteria();
	private NetlistEditFrame netlistEditFrame = null;
	String lritHost = "localhost"; // @jve:decl-index=0:
	int lritPort = 17005;
	String statusPath;
	boolean configChanged = false;
	LritPollThread myPollThread;
	boolean scDlgUp = false;

	public StatusDisplayFrame() {
		LritDcsConfig cfg = LritDcsConfig.instance();
		String home = cfg.getLritDcsHome();
		statusPath = home + File.separator + "remote_lritdcs.stat";
		myStatus = new LritDcsStatus(statusPath);
		myDateFormat = new SimpleDateFormat("yyyy/DDD HH:mm:ss");
		myDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));	
		try {
			jbInit();
			
//			lritPanel.setPreferredSize(new Dimension(500, 700));
			eventsPanel.setPreferredSize(new Dimension(500, 240));

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	static StdAppSettings myArgs = new StdAppSettings(true); // @jve:decl-index=0:
	static IntegerToken portArg = new IntegerToken("p", "port number", "",
			TokenOptions.optSwitch, 17005); // @jve:decl-index=0:

	private RtStatPanel lritPanel = new RtStatPanel();
	static {
		myArgs.addToken(portArg);
	}

	public static void main(String[] args) {
		myArgs.parseArgs(args);

		LritDcsConfig.makeInstanceNoLoad();

		StatusDisplayFrame statusDisplayFrame1 = new StatusDisplayFrame();
		statusDisplayFrame1.lritHost = myArgs.getHostName();
		statusDisplayFrame1.lritPort = portArg.getValue();

		statusDisplayFrame1.setSize(900, 1000);
		statusDisplayFrame1.setVisible(true);
		statusDisplayFrame1
				.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		statusDisplayFrame1.run();
	}

	public void run() {
		myPollThread = new LritPollThread(this, lritPanel);
		myPollThread.start();

		shutdownFlag = false;
		while (!shutdownFlag) {
			try {
				Thread.sleep(1000L);
			} catch (InterruptedException ex) {
			}

			final StatusDisplayFrame theFrame = this;
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					theFrame.updateScreen();
				}
			});
		}
	}

	public void updateScreen() {
		setTitle("LRIT File Sender Status");
		
	}

	private void jbInit() 
		throws Exception 
	{
		lritPanel.setPreferredSize(new Dimension(500, 800));
	
		titledBorder3 = new TitledBorder(BorderFactory.createEtchedBorder(
				Color.white, new Color(148, 145, 140)), "Events");
		
		jMenu1.setText("File");
		fileExit.setText("Exit");
		fileExit.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				fileExit_actionPerformed(e);
			}
		});
		jMenu2.setText("Priorities");
		miEditHighCriteria.setText("High Criteria");
		miEditHighCriteria
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(ActionEvent e) {
						miEditHighCriteria_actionPerformed(e);
					}
				});
		miEditMediumCriteria.setText("Medium Criteria");
		miEditMediumCriteria
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(ActionEvent e) {
						miEditMediumCriteria_actionPerformed(e);
					}
				});

		miEditLowCriteria.setText("Low Criteria");
		miEditLowCriteria
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(ActionEvent e) {
						miEditLowCriteria_actionPerformed(e);
					}
				});
		jMenu3.setText("Help");
		helpAbout.setText("About LRIT File Sender");
		helpAbout.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				helpAbout_actionPerformed(e);
			}
		});
		this.getContentPane().setLayout(borderLayout2);

//		this.getContentPane().setSize(600, 1200); // shweta

		eventsPanel.setBorder(titledBorder3);
		eventsPanel.setMinimumSize(new Dimension(10, 150));	
		eventsPanel.setBorder(BorderFactory.createLoweredBevelBorder());
//		eventsPanel.setPreferredSize(new Dimension(10, 150));

		jMenu5.setText("Queues");
		miQueueFlushManual.setText("Flush Manual Retrans Q");
		miQueueFlushManual
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(ActionEvent e) {
						miQueueFlushManual_actionPerformed(e);
					}
				});
		miQueueFlushAuto.setText("Flush Auto Retrans Q");
		miQueueFlushHigh.setText("Flush High");
		miQueueFlushMedium.setText("Flush Medium");
		miQueueFlushLow.setText("Flush Low");
		miQueueFlushAll.setText("Flush All");
		miFileOptions.setText("Configuration");
		miFileOptions.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				miFileOptions_actionPerformed(e);
			}
		});
		miQueueManRetrans.setText("Manual Retransmission");
		miQueueManRetrans
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(ActionEvent e) {
						miQueueManRetrans_actionPerformed(e);
					}
				});
		jMenuBar1.add(jMenu1);
		jMenuBar1.add(jMenu2);
		jMenuBar1.add(jMenu5);
		jMenuBar1.add(jMenu3);
		jMenu1.add(miFileOptions);
		jMenu1.addSeparator();
		jMenu1.add(fileExit);
		jMenu2.add(miEditHighCriteria);
		jMenu2.add(miEditMediumCriteria);
		jMenu2.add(miEditLowCriteria);
		jMenu3.add(helpAbout);
		this.setJMenuBar(jMenuBar1);		
		this.getContentPane().add(lritPanel, BorderLayout.CENTER); // shweta		
		this.getContentPane().add(eventsPanel, BorderLayout.SOUTH); // shweta

		jMenu5.add(miQueueManRetrans);
		jMenu5.addSeparator();
		jMenu5.add(miQueueFlushManual);
		jMenu5.add(miQueueFlushAuto);
		jMenu5.add(miQueueFlushHigh);
		jMenu5.add(miQueueFlushMedium);
		jMenu5.add(miQueueFlushLow);
		jMenu5.addSeparator();
		jMenu5.add(miQueueFlushAll);
		miQueueFlushAuto.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				miQueueFlushAuto_actionPerformed(e);
			}
		});
		miQueueFlushHigh.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				miQueueFlushHigh_actionPerformed(e);
			}
		});
		miQueueFlushMedium
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(ActionEvent e) {
						miQueueFlushMedium_actionPerformed(e);
					}
				});
		miQueueFlushLow.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				miQueueFlushLow_actionPerformed(e);
			}
		});
		miQueueFlushAll.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				miQueueFlushAll_actionPerformed(e);
			}
		});
	}

	void fileExit_actionPerformed(ActionEvent e) {
		System.exit(0);
	}

	void miEditHighCriteria_actionPerformed(ActionEvent e) 
	{
		scDlgUp = true;
		SearchCritDialog scDlg = new SearchCritDialog(this,
				"High Priority DCP Messages", scHigh);
		launchDialog(scDlg);
		if (scDlg.okPressed)
			myPollThread.scHigh2Send = scHigh;
		scDlgUp = false;
		scDlg = null;

		//
		// String home = LritDcsConfig.instance().getLritDcsHome();
		// if (home == null)
		// {
		// showError(
		// "This function is only available on the LRIT sender machine!");
		// return;
		// }
		//
		// String fn = home + File.separator + "searchcrit." +
		// Constants.HighPri;
		// try
		// {
		// JOptionPane.showMessageDialog(this,
		// "Remember -- Do NOT enter LRGS or DAPS time \n"
		// + "ranges in LRIT search criteria files!.",
		// "No Time Ranges!",
		// JOptionPane.INFORMATION_MESSAGE);
		//
		// File file = new File(fn);
		// if (!file.exists())
		// file.createNewFile();
		// scEditorHigh = new SearchCriteriaEditor(file);
		// final StatusDisplayFrame frame = this;
		// scEditorHigh.setParent(
		// new SearchCritEditorParent()
		// {
		// public void closingSearchCritEditor()
		// {
		// frame.scEditorHigh = null;
		// }
		// });
		// scEditorHigh.startup(100,100);
		// }
		// catch(IOException ex)
		// {
		// showError("Cannot edit '" + fn + "': " + ex);
		// scEditorHigh = null;
		// }
	}

	void miEditMediumCriteria_actionPerformed(ActionEvent e) {
		scDlgUp = true;
		SearchCritDialog scDlg = new SearchCritDialog(this,
				"Medium Priority DCP Messages", scMedium);
		launchDialog(scDlg);
		if (scDlg.okPressed)
			myPollThread.scMedium2Send = scMedium;
		scDlgUp = false;
		scDlg = null;

		// if (scEditorMedium != null)
		// {
		// scEditorMedium.toFront();
		// return;
		// }
		//
		// String home = LritDcsConfig.instance().getLritDcsHome();
		// if (home == null)
		// {
		// showError(
		// "This function is only available on the LRIT sender machine!");
		// return;
		// }
		//
		// String fn = home + File.separator + "searchcrit." +
		// Constants.MediumPri;
		// try
		// {
		// JOptionPane.showMessageDialog(this,
		// "Remember -- Do NOT enter LRGS or DAPS time \n"
		// + "ranges in LRIT search criteria files!.",
		// "No Time Ranges!",
		// JOptionPane.INFORMATION_MESSAGE);
		//
		// File file = new File(fn);
		// if (!file.exists())
		// file.createNewFile();
		// scEditorMedium = new SearchCriteriaEditor(file);
		// final StatusDisplayFrame frame = this;
		// scEditorMedium.setParent(
		// new SearchCritEditorParent()
		// {
		// public void closingSearchCritEditor()
		// {
		// frame.scEditorMedium = null;
		// }
		// });
		// scEditorMedium.startup(100,100);
		// }
		// catch(IOException ex)
		// {
		// showError("Cannot edit '" + fn + "': " + ex);
		// scEditorMedium = null;
		// }
	}

	void miEditLowCriteria_actionPerformed(ActionEvent e) {
		scDlgUp = true;
		SearchCritDialog scDlg = new SearchCritDialog(this,
				"Low Priority DCP Messages", scLow);
		launchDialog(scDlg);
		if (scDlg.okPressed)
			myPollThread.scLow2Send = scLow;
		scDlg = null;
		scDlgUp = false;

		// if (scEditorLow != null)
		// {
		// scEditorLow.toFront();
		// return;
		// }
		//
		// String home = LritDcsConfig.instance().getLritDcsHome();
		// if (home == null)
		// {
		// showError(
		// "This function is only available on the LRIT sender machine!");
		// return;
		// }
		//
		// String fn = home + File.separator + "searchcrit." + Constants.LowPri;
		// try
		// {
		// JOptionPane.showMessageDialog(this,
		// "Remember -- Do NOT enter LRGS or DAPS time \n"
		// + "ranges in LRIT search criteria files!.",
		// "No Time Ranges!",
		// JOptionPane.INFORMATION_MESSAGE);
		//
		// File file = new File(fn);
		// if (!file.exists())
		// file.createNewFile();
		// scEditorLow = new SearchCriteriaEditor(file);
		// final StatusDisplayFrame frame = this;
		// scEditorLow.setParent(
		// new SearchCritEditorParent()
		// {
		// public void closingSearchCritEditor()
		// {
		// frame.scEditorLow = null;
		// }
		// });
		// scEditorLow.startup(100,100);
		// }
		// catch(IOException ex)
		// {
		// showError("Cannot edit '" + fn + "': " + ex);
		// scEditorLow = null;
		// }
	}

	void miEditNetworkLists_actionPerformed(ActionEvent e) {
		if (netlistEditFrame != null) {
			netlistEditFrame.toFront();
			return;
		}

		String home = LritDcsConfig.instance().getLritDcsHome();
		if (home == null) {
			showError("This function is only available on the LRIT sender machine!");
			return;
		}

		String nldir = home + File.separator + "netlist";
		File nldirfile = new File(nldir);
		if (!nldirfile.isDirectory())
			nldirfile.mkdirs();

		NetlistEditFrame.netlistDir = nldir;
		netlistEditFrame = new NetlistEditFrame();
		netlistEditFrame.setStandAlone(false);
		final StatusDisplayFrame frame = this;
		netlistEditFrame.addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent ev) {
				netlistEditFrame = null;
			}
		});
		netlistEditFrame.setVisible(true);
	}

	void helpAbout_actionPerformed(ActionEvent e) {
		JOptionPane.showMessageDialog(this, "LRIT File Sender Version "
				+ Constants.version + "\n" + Constants.releaseDate,
				"About LRIT File Sender", JOptionPane.INFORMATION_MESSAGE);
	}

	void showError(String msg) {
		JOptionPane.showMessageDialog(this, msg, "Error!",
				JOptionPane.ERROR_MESSAGE);
	}

	void miFileOptions_actionPerformed(ActionEvent e) {
		LritDcsConfigDialog dlg = new LritDcsConfigDialog();
		dlg.fillValues();
		//dlg.txtLritHost.setText(this.lritHost);
		launchDialog(dlg);		
		if (dlg.okPressed())
			configChanged = true;
	}

	void miQueueManRetrans_actionPerformed(ActionEvent e) {
		scDlgUp = true;
		SearchCritTimeDialog scDlg = new SearchCritTimeDialog(this,
				"Manual Retransmit Request", scManual);
		launchDialog(scDlg);		
		if (scDlg.okPressed && !scDlg.isDialogEmpty())
			myPollThread.scManual2Send = scManual;
		scDlg = null;
		scDlgUp = false;
	}

	void miQueueFlushManual_actionPerformed(ActionEvent e) {
		myPollThread.cmd = "flush manual";
	}

	void miQueueFlushAuto_actionPerformed(ActionEvent e) {
		myPollThread.cmd = "flush auto";
	}

	void miQueueFlushHigh_actionPerformed(ActionEvent e) {
		myPollThread.cmd = "flush high";
	}

	void miQueueFlushMedium_actionPerformed(ActionEvent e) {
		myPollThread.cmd = "flush medium";
	}

	void miQueueFlushLow_actionPerformed(ActionEvent e) {
		myPollThread.cmd = "flush low";
	}

	void miQueueFlushAll_actionPerformed(ActionEvent e) {
		myPollThread.cmd = "flush all";
	}

	private void launchDialog(JDialog dlg) {
		dlg.setModal(true);
		dlg.validate();
		dlg.setLocationRelativeTo(this);
		dlg.setVisible(true);
	}

	public void addEvent(String event, String from) {
		StringTokenizer st = new StringTokenizer(event);
		if (!st.hasMoreTokens())
			return;
		String pri = st.nextToken();
		if (!st.hasMoreTokens())
			return;
		String time = st.nextToken();
		if (!st.hasMoreTokens())
			return;
		String mod_num = st.nextToken();
		if (!st.hasMoreTokens())
			return;
		String text = st.nextToken("\n");

		// Reconstruct with source inserted.
		StringBuffer sb = new StringBuffer();
		sb.append(pri);
		while (sb.length() < 8)
			sb.append(' ');
		sb.append(from);
		while (sb.length() < 16)
			sb.append(' ');
		sb.append(time);
		sb.append(' ');
		sb.append(mod_num);
		sb.append(' ');
		sb.append(text);

		String evtMsg = sb.toString();
		eventsPanel.addLine(evtMsg);

	}

	class LritPollThread extends Thread 
	{
		StatusDisplayFrame parent;
		boolean shutdown;
		BasicClient lritcon;
		static final String eventsPoll = "EVENTS";
		static final String statusPoll = "STATUS";
		static final String putConfigReq = "PUTCONFIG";
		static final String getConfigReq = "GETCONFIG";
		static final String getCritReq = "GETCRIT";
		static final String putCritReq = "PUTCRIT";
		BufferedReader reader;
		PrintWriter writer;
		String cmd = null;
		SearchCriteria scHigh2Send = null;
		SearchCriteria scMedium2Send = null;
		SearchCriteria scLow2Send = null;
		SearchCriteria scManual2Send = null;

		private RtStatPanel rtStatPanel;

		private LritReportGenerator repgen;
		private XmlOutputStream xos;
		private ByteArrayOutputStream htmlOS;
		public LritDcsStatus currentStatus;
		public String lritHostName;
		// public boolean configChanged = false;

		//SearchCriteria scHigh;
		//SearchCriteria scMedium;
		//SearchCriteria scLow;
		//SearchCriteria scManual;
		boolean sendHigh = false;
		boolean sendMedium = false;
		boolean sendLow = false;
		boolean sendManual = false;

		public LritPollThread(StatusDisplayFrame parent) 
		{
			super("LritPollThread");
			this.parent = parent;
		}

		public LritPollThread(StatusDisplayFrame parent, RtStatPanel rtStatPanel) 
		{
			super("LritPollThread");
			this.parent = parent;
			this.rtStatPanel = rtStatPanel;
			currentStatus = new LritDcsStatus("currentLritStatus");
			lritHostName = "";
			htmlOS = new ByteArrayOutputStream(32000);
			xos = new XmlOutputStream(htmlOS, "html");
			repgen = new LritReportGenerator();
			configChanged = false;
		}

		/*
		 * public void run() { shutdown = false; lritcon = new
		 * BasicClient(parent.lritHost, parent.lritPort); long lastConfigPoll =
		 * 0L; while(!shutdown) { if (!lritcon.isConnected() && !tryConnect()) {
		 * emptyStatus(); try {
		 * 
		 * sleep(5000L); } catch(InterruptedException ex2) {} continue; }
		 * 
		 * try { long now = System.currentTimeMillis(); if (now - lastConfigPoll
		 * > 60000L) { lastConfigPoll = now; getConfig(); if (!parent.scDlgUp) {
		 * getCrit("high", parent.scHigh); getCrit("medium", parent.scMedium);
		 * getCrit("low", parent.scLow); } } if (parent.configChanged)
		 * sendConfig(); if (scHigh2Send != null) { putCrit("high",
		 * scHigh2Send); scHigh2Send = null; } if (scMedium2Send != null) {
		 * putCrit("medium", scMedium2Send); scMedium2Send = null; } if
		 * (scLow2Send != null) { putCrit("low", scLow2Send); scLow2Send = null;
		 * } if (scManual2Send != null) { putCrit("manual", scManual2Send);
		 * scManual2Send = null; } if (cmd != null) sendCommand();
		 * pollForStatus(); pollForEvents();
		 * 
		 * try { sleep(1000L); } catch(InterruptedException ex2) {} }
		 * catch(IOException ex) {
		 * System.err.println("Error on UI LRIT connection to '" +
		 * lritcon.getName() + "': " + ex); try { reader.close();
		 * writer.close(); } catch(Exception ex3) {} lritcon.disconnect();
		 * parent.myStatus.status = "Connect Error"; try { sleep(5000L); }
		 * catch(InterruptedException ex2) {} } } }
		 */

		public void run() 
		{
			shutdown = false;

			lritcon = new BasicClient(parent.lritHost, parent.lritPort);

			long lastConfigPoll = 0L;

			// long now = System.currentTimeMillis();
			while (!shutdown) 
			{
				if (!lritcon.isConnected() && !tryConnect()) 
				{
					emptyStatus();
					try 
					{
						sleep(5000L);
					}
					catch (InterruptedException ex2)
					{
						ex2.printStackTrace();
					}
					continue;
				}

				try 
				{
					long now = System.currentTimeMillis();
					if (now - lastConfigPoll > 60000L)
					{
						lastConfigPoll = now;
						getConfig();
						if (!parent.scDlgUp)
						{
							getCrit("high", scHigh);
							getCrit("medium", scMedium);
							getCrit("low", scLow);
							getCrit("manual", scManual);
						}
					}

					if (configChanged)
					{
						sendConfig();
						configChanged = false;
					}
					
					if (scHigh2Send != null)
					{
						putCrit("high", scHigh2Send);
						scHigh2Send = null;
					}
					if (scMedium2Send != null)
					{
						putCrit("medium", scMedium2Send);
						scMedium2Send = null;
					}
					if (scLow2Send != null)
					{
						putCrit("low", scLow2Send);
						scLow2Send = null;
					}
					if (scManual2Send != null)
					{
						putCrit("manual", scManual2Send);
						scManual2Send = null;
					}
					
					if (cmd != null)
						sendCommand();

					pollForStatus();
					pollForEvents();

					try
					{
						sleep(1000L);
					}
					catch (InterruptedException ex2)
					{
						ex2.printStackTrace();
					}
				}
				catch (IOException ex)
				{
					String msg = "Error on UI LRIT connection to '"
						+ lritcon.getName() + "': " + ex;
					Logger.instance().failure("GUI:4 " + msg);
					try
					{
						reader.close();
						writer.close();
					}
					catch (Exception ex3)
					{
					}
					lritcon.disconnect();

					try
					{
						sleep(10000L);
					}
					catch (InterruptedException ex2)
					{
						ex2.printStackTrace();
					}
				}
			}
		}

		private void emptyStatus()
		{
			currentStatus.clear();
			// currentStatus.loadFromProps(props);
			// System.out.println("Loaded status: " + currentStatus.toString());
			SwingUtilities.invokeLater(
				new Runnable() 
				{
					public void run() 
					{
						lritHostName = parent.lritHost;
						try 
						{
							htmlOS.reset();
							repgen.writeReport(xos, lritHostName, currentStatus, 0);
							String report = htmlOS.toString();
							rtStatPanel.updateStatus(report);
						}
						catch (IOException ex)
						{
							Logger.instance().warning(
								"Unexpected IO exception writing LRIT report: "
								+ ex);
						}
					}
				});
		}

		private boolean tryConnect()
		{
			try
			{
				lritcon.connect();
				reader = new BufferedReader(new InputStreamReader(
					lritcon.getInputStream()));
				writer = new PrintWriter(lritcon.getOutputStream(), true);
				return true;
			}
			catch (IOException ex)
			{
				System.err
					.println("Error connecting to UI LRIT connection at '"
						+ lritcon.getName() + "': " + ex);
				parent.myStatus.status = "Connect Error";
				return false;
			}
		}

		private void pollForStatus() throws IOException
		{
			// System.out.println("Polling for LRIT status...");
			writer.println(statusPoll);
//Logger.instance().info("send '" + statusPoll + "'");

			ByteArrayOutputStream statusBufOS = new ByteArrayOutputStream();
			byte nl[] = new byte[]{ (byte) '\n' };
			int n = 0;
			while (true)
			{
				String line = reader.readLine();
				if (line == null)
					return;
				n++;
				if (line.trim().length() == 0)
					break;
				statusBufOS.write(line.getBytes());
				statusBufOS.write(nl);
			}
			statusBufOS.close();
//Logger.instance().info("pollForStatus read " + n + " lines.");

			Properties props = new Properties();
			ByteArrayInputStream bais = new ByteArrayInputStream(
				statusBufOS.toByteArray());
			props.load(bais);
			bais.close();

			try
			{
				currentStatus.loadFromProps(props);
				// System.out.println("Loaded status: " +
				// currentStatus.toString());
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{

						lritHostName = parent.lritHost;
						try
						{
							htmlOS.reset();
							repgen.writeReport(xos, lritHostName,
								currentStatus, 0);
							String report = htmlOS.toString();
							rtStatPanel.updateStatus(report);
						}
						catch (IOException ex)
						{
							Logger.instance().warning(
								"Unexpected IO exception writing LRIT report: "
									+ ex);
						}
					}
				});
			}
			catch (StatusInvalidException ex)
			{
				System.err.println("Invalid LRIT status received & ignored.");

			}
		}

		private void pollForEvents() throws IOException
		{
			writer.println(eventsPoll);
			while (true)
			{
				String line = reader.readLine();
				if (line == null)
					break;
				line = line.trim();
				if (line.length() == 0)
					break;
				// System.out.println("line is : "+line);
				parent.eventsPanel.addLine(line);
			}
		}

		/*
		 * private void pollForEvents() throws IOException {
		 * writer.println(eventsPoll); final Vector events = new Vector();
		 * while(true) { String line = reader.readLine(); if (line == null)
		 * break; line = line.trim(); if (line.length() == 0) break;
		 * events.add(line); } SwingUtilities.invokeLater( new Runnable() {
		 * public void run() { for(int i=0; i<events.size(); i++)
		 * parent.addEvent((String)events.get(i), "LRIT"); } });
		 * 
		 * }
		 */

		private void sendConfig() throws IOException {		
			
			parent.configChanged = false;
			writer.println(putConfigReq);
			LritDcsConfig.instance().save(writer);
			writer.println();
		}

		/*
		 * private void getConfig() throws IOException {
		 * writer.println(getConfigReq);
		 * 
		 * byte cfgbuf[] = captureToBlankLine(); Properties props = new
		 * Properties(); ByteArrayInputStream bais = new
		 * ByteArrayInputStream(cfgbuf); props.load(bais);
		 * 
		 * PropertiesUtil.loadFromProps(LritDcsConfig.instance(), props); }
		 */

		private void getConfig() throws IOException
		{
			if (writer != null)
			{
//Logger.instance().info("Sending '" + getConfigReq + "'");
				writer.println(getConfigReq);

				byte cfgbuf[] = captureToBlankLine();
				Properties props = new Properties();
				ByteArrayInputStream bais = new ByteArrayInputStream(cfgbuf);
				props.load(bais);

				PropertiesUtil.loadFromProps(LritDcsConfig.instance(), props);
			}
			else
				LritDcsConfig.makeInstanceNoLoad();
		}

		private void getCrit(String what, SearchCriteria sc) throws IOException
		{
//Logger.instance().info("Sending '" + getCritReq + " " + what + "'");
			writer.println(getCritReq + " " + what);
			byte buf[] = captureTo("ENDSC");
			ByteArrayInputStream bais = new ByteArrayInputStream(buf);
			sc.clear();
			int n = 0;
			try
			{
				sc.parseFile(new InputStreamReader(bais));
			}
			catch (SearchSyntaxException ssex)
			{
				Logger.instance().warning(
					"Server returned invalid '" + what + "' search criteria: "
						+ ssex);
			}
			bais.close();
		}

		private void putCrit(String what, SearchCriteria sc) throws IOException
		{
			writer.println(putCritReq + " " + what);
			writer.println(sc.toString());
			writer.println();
		}

		// / Sends a simple command and expects no response.
		private void sendCommand() throws IOException {			
			if (cmd != null)
				writer.println(cmd);
			cmd = null;
		}

		private byte[] captureToBlankLine() throws IOException
		{
			ByteArrayOutputStream configBufOS = new ByteArrayOutputStream();
			byte nl[] = new byte[]{ (byte) '\n' };
			int n = 0;
			while (true)
			{
				String line = reader.readLine();
				if (line == null)
					throw new IOException("Socket closed by LRIT Sender");
				n++;
				line = line.trim();
				if (line.length() == 0)
					break;
				configBufOS.write(line.getBytes());
				configBufOS.write(nl);
			}
			configBufOS.close();
//Logger.instance().info("capturetoBlankLine read " + n + " lines.");
			return configBufOS.toByteArray();
		}

		private byte[] captureTo(String endTag) throws IOException
		{
//if (endTag.equals("ENDSC"))
//{
//Logger.instance().info("reading to ENDSC");
//}
			ByteArrayOutputStream configBufOS = new ByteArrayOutputStream();
			byte nl[] = new byte[]{ (byte) '\n' };
			int n = 0;
			while (true)
			{
				String line = reader.readLine();
				if (line == null)
					throw new IOException("Socket closed by LRIT Sender");
				n++;
				line = line.trim();
//if (endTag.equals("ENDSC"))
//Logger.instance().info("Read '" + line + "'");
				if (line.equals(endTag))
					break;
				configBufOS.write(line.getBytes());
				configBufOS.write(nl);
			}
			configBufOS.close();
//if (endTag.equals("ENDSC"))
//Logger.instance().info("Found ENDSC " + n + " lines read.");

			return configBufOS.toByteArray();
		}
	}

}

class StatusField extends JTextField {
	static Font fieldFont = new java.awt.Font("SansSerif", 1, 12);
	static Color badColor = new Color(255, 100, 100);
	static Color goodColor = Color.green;

	public StatusField() {
		super();
		setFont(fieldFont);
		setBackground(Color.black);
		setForeground(Color.green);
		setEditable(false);
		setText("");
	}

	public void set(String text, boolean good) {
		setForeground(good ? goodColor : badColor);
		setText(text);
	}
}
