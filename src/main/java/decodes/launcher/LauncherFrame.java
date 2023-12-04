/*
 * $Id: LauncherFrame.java,v 1.19 2020/04/05 21:18:57 cvs Exp $
 *
 * $Log: LauncherFrame.java,v $
 * Revision 1.19  2020/04/05 21:18:57  cvs
 * Show processes button even if not a TSDB.
 *
 * Revision 1.18  2019/12/11 14:38:02  mmaloney
 * Removed dbClassName. This now set as a function of database type.
 *
 * Revision 1.17  2019/11/13 15:11:16  mmaloney
 * Added multiple profiles feature to launcher.
 *
 * Revision 1.16  2019/10/25 15:15:19  mmaloney
 * dev
 *
 * Revision 1.15  2019/10/22 13:16:41  mmaloney
 * dev
 *
 * Revision 1.14  2019/10/22 12:39:39  mmaloney
 * Pass launcher args to launcher actions.
 *
 * Revision 1.13  2019/10/13 19:24:47  mmaloney
 * Prototypes for multi-profile launcher
 *
 * Revision 1.12  2017/08/22 19:53:58  mmaloney
 * Improve comments
 *
 * Revision 1.11  2017/06/13 20:19:32  mmaloney
 * dev
 *
 * Revision 1.10  2017/06/13 20:02:33  mmaloney
 * dev
 *
 * Revision 1.9  2016/07/21 18:13:17  mmaloney
 * Added Platform Monitor and Routing Monitor buttons to launcher.
 *
 * Revision 1.8  2015/07/17 13:28:07  mmaloney
 * Added showComputationEditor boolean.
 *
 * Revision 1.7  2015/05/21 13:26:14  mmaloney
 * RC08
 *
 * Revision 1.6  2015/05/14 13:52:20  mmaloney
 * RC08 prep
 *
 * Revision 1.5  2015/04/03 19:53:27  mmaloney
 * Fixed CWMS-5626 where by some GUIs would silently exit if user had no privilege.
 *
 * Revision 1.4  2015/02/06 18:46:59  mmaloney
 * Config option to not display certain buttons: Time Series, Groups, Run Comps, & Algorithms.
 *
 * Revision 1.3  2014/10/07 12:34:37  mmaloney
 * DecodesSetting.showNetlistEditor
 *
 * Revision 1.2  2014/05/22 12:14:09  mmaloney
 * Disable TS buttons when database is XML.
 *
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 * Revision 1.8  2013/04/26 14:29:23  mmaloney
 * gif not png.
 *
 * Revision 1.7  2013/04/26 14:22:28  mmaloney
 * Added ts list
 *
 * Revision 1.6  2013/04/09 13:33:14  mmaloney
 * Removed "Retrieval and Decoding" button that doesn't do anything.
 *
 * Revision 1.5  2013/03/26 17:48:18  mmaloney
 * Change title to OPENDCS <version>
 *
 * Revision 1.4  2013/03/25 18:56:03  mmaloney
 * Plug in Process Monitor
 *
 * Revision 1.3  2013/02/28 16:46:24  mmaloney
 * Start pdt maintenance thread.
 *
 * Revision 1.2  2012/07/24 13:41:04  mmaloney
 * Clean up resource strings.
 *
 * Revision 1.1  2012/05/17 15:14:31  mmaloney
 * Initial implementation for USBR.
 *
 *
 * This is open-source software written by Sutron Corporation under
 * contract to the federal government. Anyone is free to copy and use this
 * source code for any purpos, except that no part of the information
 * contained in this file may be claimed to be proprietary.
 *
 * Except for specific contractual terms between Sutron and the federal 
 * government, this source code is provided completely without warranty.
 */
package decodes.launcher;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Properties;
import java.util.ResourceBundle;

import ilex.cmdline.BooleanToken;
import ilex.cmdline.IntegerToken;
import ilex.cmdline.TokenOptions;
import ilex.gui.GuiApp;
import ilex.net.BasicClient;
import ilex.net.BasicServer;
import ilex.net.BasicSvrThread;
import ilex.util.AsciiUtil;
import ilex.util.EnvExpander;
import ilex.util.LoadResourceBundle;
import ilex.util.Logger;
import ilex.util.ProcWaiterCallback;
import ilex.util.ProcWaiterThread;
import ilex.util.TextUtil;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.TitledBorder;

import lrgs.gui.DecodesInterface;
import lrgs.gui.MessageBrowser;
import lrgs.nledit.NetlistEditFrame;
import lrgs.rtstat.RtStat;
import decodes.db.Platform;
import decodes.dbeditor.DbEditorFrame;
import decodes.gui.TopFrame;
import decodes.platstat.PlatformMonitor;
import decodes.platwiz.Frame1;
import decodes.platwiz.PlatformWizard;
import decodes.routmon2.RoutingMonitor;
import decodes.tsdb.algoedit.AlgorithmWizard;
import decodes.tsdb.compedit.CAPEdit;
import decodes.tsdb.comprungui.CompRunGuiFrame;
import decodes.tsdb.comprungui.RunComputationsFrameTester;
import decodes.tsdb.groupedit.TsListMain;
import decodes.tsdb.procmonitor.ProcessMonitor;
import decodes.util.DecodesException;
import decodes.util.DecodesSettings;
import decodes.util.DecodesVersion;
import decodes.util.ResourceFactory;
import decodes.util.CmdLineArgs;


@SuppressWarnings("serial")
public class LauncherFrame 
	extends JFrame
	implements ProcWaiterCallback
{
	private static ResourceBundle labels = getLabels();
	String myArgs[] = null;
//	String compArgs[] = null;
	JPanel fullPanel = new JPanel();
	GridBagLayout fullLayout = new GridBagLayout();

	JPanel decodesButtonPanel = new JPanel();
	GridLayout dcstoolLayout = new GridLayout();
	JButton lrgsStatButton = new JButton();
	TitledBorder dcstoolButtonBorder;
	JButton browserButton = new JButton();
	JButton netlistButton = new JButton();
	JButton dbeditButton = new JButton();
	JButton platwizButton = new JButton();
	JButton toolkitConfigButton = new JButton();

	JPanel tsdbButtonPanel = new JPanel();
	GridLayout tsdbLayout = new GridLayout();
	TitledBorder tsdbButtonBorder;
	JButton tseditButton = new JButton();
	JButton groupEditButton = new JButton();
	JButton compeditButton = new JButton();
	JButton runcompButton = new JButton();
	private JButton procstatButton = new JButton();
	JButton algoeditButton = new JButton();
	private JButton routmonButton = new JButton();
	private JButton platmonButton = new JButton();

	boolean exitOnClose;
	DbEditorFrame dbEditorFrame;
	WindowAdapter dbEditorReaper;
	MessageBrowser browserFrame;
	WindowAdapter browserReaper;
	NetlistEditFrame netlistEditFrame;
	WindowAdapter netlistEditReaper;
	WindowAdapter routMonReaper;
	Runnable afterDecodesInit;
	InitDecodesFrame initDecodesFrame;
	TopFrame groupEditFrame;// Time Series Button
//	private TsDbGrpEditorFrame tsDbGrpEditorFrame; // Time Series Groups Button
	JFrame procMonFrame;// Process Status Button
	WindowAdapter tsGroupReaper;
	WindowAdapter logViewerReaper;
	TopFrame tseditFrame;// Limit Status Button
	WindowAdapter tseditReaper;
	CompRunGuiFrame runComputationsFrame;// Test Computations Button
	WindowAdapter runComputationsReaper;
	TopFrame compEditFrame;// Computations Button
	WindowAdapter compEditReaper;
	WindowAdapter procMonReaper;
	TopFrame algorithmWizFrame;// Algorithms Button
	WindowAdapter algorithmWizReaper;
	Frame1 platformWizFrame;// Platform Wizard button
	WindowAdapter platformWizReaper;
	TopFrame alarmsFrame;// Alarms Button
	WindowAdapter alarmsReaper;
	private static BooleanToken noCompFilterToken;
	private static BooleanToken justPrintVersionToken;
	WindowAdapter statContactReaper;
	private TsdbType tsdbType = TsdbType.NONE;
	private TopFrame lrgsStatFrame = null;
	private WindowAdapter lrgsStatReaper;
	private String installDir;
	TopFrame toolkitSetupFrame = null;
	WindowAdapter setupFrameReaper;
	private TopFrame platmonFrame = null, routmonFrame = null;
	private WindowAdapter platmonReaper = null, routmonReaper = null;
	private String selectedProfile = null;
	private JComboBox<String> profileCombo = null;
	private JPanel profPanel = null;
	private boolean profilesShown = false;
	BasicServer profileLauncherServer = null;
	private WindowAdapter profileMgrReaper = null;
	private ProfileManagerFrame profileMgrFrame = null;

	// this is defined here to that the test can properly check.
	// Future improvements will alter how that test works.
	private String profilePath = null;

	public LauncherFrame(String args[])
	{
		myArgs = args;
		exitOnClose = true;
		dbEditorFrame = null;
		browserFrame = null;
		netlistEditFrame = null;
		afterDecodesInit = null;
		initDecodesFrame = null;
		groupEditFrame = null;
//		tsDbGrpEditorFrame = null;
		procMonFrame = null;
		tseditFrame = null;
		runComputationsFrame = null;
		compEditFrame = null;
		algorithmWizFrame = null;
		platformWizFrame = null;
		alarmsFrame = null;
Logger.instance().info("LauncherFrame ctor");		
		String dbClass = DecodesSettings.instance().getTsdbClassName();
		if (dbClass != null)
		{
			if (dbClass.contains("Hdb"))
				tsdbType = TsdbType.HDB;
			else if (dbClass.contains("OpenTsdb"))
				tsdbType = TsdbType.OPENTSDB;
			else if (dbClass.contains("Cwms"))
				tsdbType = TsdbType.CWMS;
			else
				tsdbType = TsdbType.NONE;
		}
		
		DecodesInterface.setGUI(true);

		installDir = EnvExpander.expand("$DECODES_INSTALL_DIR");
		try
		{
Logger.instance().info("LauncherFrame ctor - getting dacq launcher actions...");
			dacqLauncherActions = ResourceFactory.instance().getDacqLauncherActions();
			jbInit();
			ImageIcon lrgsStatIcon = new ImageIcon(installDir
					+ File.separator + "icons" + File.separator
					+ "retproc48x48.gif");
			ImageIcon browserIcon = new ImageIcon(installDir
					+ File.separator + "icons" + File.separator
					+ "msgbrowser48x48.gif");
			ImageIcon netlistIcon = new ImageIcon(installDir
					+ File.separator + "icons" + File.separator
					+ "netlist48x48.gif");
			ImageIcon dbeditIcon = new ImageIcon(installDir
					+ File.separator + "icons" + File.separator
					+ "dbedit48x48.gif");
			ImageIcon platwizIcon = new ImageIcon(installDir
					+ File.separator + "icons" + File.separator
					+ "platwiz48x48.gif");
			ImageIcon setupIcon = new ImageIcon(installDir
					+ File.separator + "icons" + File.separator
					+ "setup48x48.gif");
			ImageIcon platmonIcon = new ImageIcon(installDir
				+ File.separator + "icons" + File.separator
				+ "platmonIcon.png");
			ImageIcon routmonIcon = new ImageIcon(installDir
				+ File.separator + "icons" + File.separator
				+ "routmonIcon.png");

			
			lrgsStatButton.setIcon(lrgsStatIcon);
			lrgsStatButton.setHorizontalAlignment(SwingConstants.CENTER);
			
			browserButton.setIcon(browserIcon);
			browserButton.setHorizontalAlignment(SwingConstants.CENTER);
			netlistButton.setIcon(netlistIcon);
			netlistButton.setHorizontalAlignment(SwingConstants.CENTER);
			dbeditButton.setIcon(dbeditIcon);
			dbeditButton.setHorizontalAlignment(SwingConstants.CENTER);
			// platwizButton.setIcon(wizIcon);
			platwizButton.setText(labels
				.getString("LauncherFrame.platformWizardButton"));
			platwizButton.setIcon(platwizIcon);
			platwizButton.setHorizontalAlignment(SwingConstants.CENTER);
			toolkitConfigButton.setIcon(setupIcon);
			toolkitConfigButton.setHorizontalAlignment(SwingConstants.CENTER);
			platmonButton.setIcon(platmonIcon);;
			platmonButton.setHorizontalAlignment(SwingConstants.CENTER);
			routmonButton.setIcon(routmonIcon);;
			routmonButton.setHorizontalAlignment(SwingConstants.CENTER);

			setTitle(DecodesVersion.getAbbr() + " " + DecodesVersion.getVersion());
			// setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

			final LauncherFrame myframe = this;
			addWindowListener(new WindowAdapter()
			{
				public void windowClosing(WindowEvent e)
				{
					String msg = canClose();
					if (msg != null)
					{
						JOptionPane.showMessageDialog(null,
							AsciiUtil.wrapString(msg, 60), "Error!",
							JOptionPane.ERROR_MESSAGE);
						return;
					}
					
					// Now see if any subordinate profile launchers need to stay open
					// If a server exists, then at least one profile launcher was started.
					if (profileLauncherServer != null)
					{
						for(Object connobj : profileLauncherServer.getAllSvrThreads())
						{
							ProfileLauncherConn plc = (ProfileLauncherConn)connobj;
							plc.sendCmd("exit");
							if (!plc.isExitOk())
							{
								msg = "Launcher for profile '"
										+ plc.getProfileName() + "' has a window open.";
								Logger.instance().warning(msg);
								// The offending frame itself must bring itself to the fore and
								// issue an error message.
//								JOptionPane.showMessageDialog(null,
//									AsciiUtil.wrapString(msg, 60), "Error!",
//									JOptionPane.ERROR_MESSAGE);
								return;
							}
						}
						
					}
					
					myframe.cleanupBeforeExit();
					System.exit(0);
				}
			});
			dbEditorReaper = new WindowAdapter()
			{
				public void windowClosed(WindowEvent e)
				{
					dbEditorFrame = null;
					Logger.instance().log(Logger.E_DEBUG1,
						"DbEditorFrame closed");
				}
			};
			setupFrameReaper = new WindowAdapter()
			{
				public void windowClosed(WindowEvent e)
				{
					toolkitSetupFrame = null;
					Logger.instance().log(Logger.E_DEBUG1,
						"Setup frame closed");
				}
			};
			browserReaper = new WindowAdapter()
			{
				public void windowClosed(WindowEvent e)
				{
					browserFrame = null;
					Logger.instance().log(Logger.E_DEBUG1,
						"Message Browser closed");
				}
			};
			netlistEditReaper = new WindowAdapter()
			{
				public void windowClosed(WindowEvent e)
				{
					netlistEditFrame = null;
					Logger.instance().log(Logger.E_DEBUG1,
						"Network List Editor closed");
				}
			};
			tsGroupReaper = new WindowAdapter()
			{
				public void windowClosed(WindowEvent e)
				{
					groupEditFrame = null;
//					tsDbGrpEditorFrame = null;
					
					if (tsdbType == TsdbType.CWMS
					 || tsdbType == TsdbType.HDB)
						Logger.instance().log(Logger.E_DEBUG1,
						"TS DB Group Editor screen closed");
					else
						Logger.instance().log(Logger.E_DEBUG1,
						"TS DB Editor screen closed");
				}
			};
			logViewerReaper = new WindowAdapter()
			{
				public void windowClosed(WindowEvent e)
				{
					procMonFrame = null;
					Logger.instance().info("Process Status screen closed");
				}
			};
			tseditReaper = new WindowAdapter()
			{
				public void windowClosed(WindowEvent e)
				{
					tseditFrame = null;
					Logger.instance().log(Logger.E_DEBUG1,
						"Limits Status screen closed");
				}
			};

			runComputationsReaper = new WindowAdapter()
			{
				public void windowClosed(WindowEvent e)
				{
					runComputationsFrame = null;
					Logger.instance().log(Logger.E_DEBUG1,
						"Test Computations screen closed");
				}
			};
			compEditReaper = new WindowAdapter()
			{
				public void windowClosed(WindowEvent e)
				{
					compEditFrame = null;
					Logger.instance().log(Logger.E_DEBUG1,
						"Computations screen closed");
				}
			};
			procMonReaper = new WindowAdapter()
			{
				public void windowClosed(WindowEvent e)
				{
					procMonFrame = null;
					Logger.instance().log(Logger.E_DEBUG1,
						"Process Monitor screen closed");
				}
			};
			algorithmWizReaper = new WindowAdapter()
			{
				public void windowClosed(WindowEvent e)
				{
					algorithmWizFrame = null;
					Logger.instance().log(Logger.E_DEBUG1,
						"Algorithms screen closed");
				}
			};
			platformWizReaper = new WindowAdapter()
			{
				public void windowClosed(WindowEvent e)
				{
					platformWizFrame = null;
					Logger.instance().log(Logger.E_DEBUG1,
						"Platform Wizard screen closed");
				}
			};
			alarmsReaper = new WindowAdapter()
			{
				public void windowClosed(WindowEvent e)
				{
					alarmsFrame = null;
					Logger.instance().log(Logger.E_DEBUG1, "Alarms closed");
				}
			};

			lrgsStatReaper = new WindowAdapter()
			{
				public void windowClosed(WindowEvent e)
				{
					lrgsStatFrame = null;
					Logger.instance().debug1("LRGS Stat closed");
				}
			};
			
			routmonReaper = new WindowAdapter()
			{
				public void windowClosed(WindowEvent e)
				{
					routmonFrame = null;
					Logger.instance().log(Logger.E_DEBUG1, "Routing Monitor closed");
				}
			};
			platmonReaper = new WindowAdapter()
			{
				public void windowClosed(WindowEvent e)
				{
					platmonFrame = null;
					Logger.instance().log(Logger.E_DEBUG1, "Platform Monitor closed");
				}
			};
			profileMgrReaper = new WindowAdapter()
			{
				public void windowClosed(WindowEvent e)
				{
					profileMgrFrame = null;
					Logger.instance().log(Logger.E_DEBUG1,
						"ProfileManager screen closed");
					checkForProfiles();
				}
			};
			
			ImageIcon tkIcon = new ImageIcon(
				ResourceFactory.instance().getIconPath());
			setIconImage(tkIcon.getImage());

			pack();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public String canClose()
	{
		String msg = null;

		if (dbEditorFrame != null && !dbEditorFrame.canClose())
		{
			dbEditorFrame.toFront();
			msg = LoadResourceBundle.sprintf(labels.getString("LauncherFrame.windowClosingMsg"),
				labels.getString("LauncherFrame.DECODESDBEditorButton"));
		}
		else if (netlistEditFrame != null && !netlistEditFrame.okToAbandon())
		{
			netlistEditFrame.toFront();
			msg = LoadResourceBundle.sprintf(labels.getString("LauncherFrame.windowClosingMsg"),
				labels.getString("LauncherFrame.networkListMaintButton"));
		}
		else if (groupEditFrame != null && !groupEditFrame.canClose())
		{
			groupEditFrame.toFront();
			msg = LoadResourceBundle.sprintf(labels.getString("LauncherFrame.windowClosingMsg"),
				labels.getString("LauncherFrame.timeSeriesButton"));
		}
//		else if (tsDbGrpEditorFrame != null && !tsDbGrpEditorFrame.canClose())
//		{
//			tsDbGrpEditorFrame.toFront();
//			msg = LoadResourceBundle.sprintf(labels.getString("LauncherFrame.windowClosingMsg"),
//				labels.getString("LauncherFrame.timeSeriesButton"));
//		}
		else if (tseditFrame != null && !tseditFrame.canClose())
		{
			tseditFrame.toFront();
			msg = LoadResourceBundle.sprintf(labels.getString("LauncherFrame.windowClosingMsg"),
				labels.getString("LauncherFrame.limitStatusButton"));
		}
		else if (runComputationsFrame != null && !runComputationsFrame.canClose())
		{
			runComputationsFrame.toFront();
			msg = LoadResourceBundle.sprintf(labels.getString("LauncherFrame.windowClosingMsg"),
				labels.getString("LauncherFrame.testComputationsButton"));
		}
		else if (compEditFrame != null && !compEditFrame.canClose())
		{
			compEditFrame.toFront();
			msg = LoadResourceBundle.sprintf(labels.getString("LauncherFrame.windowClosingMsg"),
				labels.getString("LauncherFrame.computationsButton"));
		}
		else if (algorithmWizFrame != null && !algorithmWizFrame.canClose())
		{
			algorithmWizFrame.toFront();
			msg = LoadResourceBundle.sprintf(labels.getString("LauncherFrame.windowClosingMsg"),
				labels.getString("LauncherFrame.algorithmsButton"));
		}
		else if (platformWizFrame != null && !platformWizFrame.canClose())
		{
			platformWizFrame.toFront();
			msg = LoadResourceBundle.sprintf(labels.getString("LauncherFrame.windowClosingMsg"),
				labels.getString("LauncherFrame.platformWizardButton"));
		}
		else if (platmonFrame != null && !platmonFrame.canClose())
		{
			platmonFrame.toFront();
			msg = LoadResourceBundle.sprintf(labels.getString("LauncherFrame.windowClosingMsg"),
				labels.getString("LauncherFrame.platmonButton"));
		}
		else if (routmonFrame != null && !routmonFrame.canClose())
		{
			routmonFrame.toFront();
			msg = LoadResourceBundle.sprintf(labels.getString("LauncherFrame.windowClosingMsg"),
				labels.getString("LauncherFrame.routmonButton"));
		}
		return msg;
	}

	public void cleanupBeforeExit()
	{
		if (initDecodesFrame != null)
			initDecodesFrame.dispose();
	}

	public boolean getExitOnClose()
	{
		return exitOnClose;
	}

	public void setExitOnClose(boolean tf)
	{
		exitOnClose = tf;
	}

	protected void processWindowEvent(WindowEvent e)
	{
		super.processWindowEvent(e);
		if (e.getID() == WindowEvent.WINDOW_OPENED)
		{
			File f = new File(EnvExpander.expand("$DECODES_INSTALL_DIR/FIRST_RUN.TXT"));
			if (f.exists())
			{
				doFirstRunChecks();
				if (!f.delete())
					showError(LoadResourceBundle.sprintf(
						labels.getString("LauncherFrame.cantDelete"),
						f.getPath()));
			}
		}
	}

	/**
	 * Called once the first time the toolkit is run after an update or install.
	 * We do one-time checks here to make sure the DB and environment are
	 * correct for this version of the toolkit.
	 */
	private void doFirstRunChecks()
	{
	}

	private void jbInit() throws Exception
	{
		JPanel contentPane = (JPanel) this.getContentPane();

		dcstoolButtonBorder = new TitledBorder(BorderFactory.createEtchedBorder(Color.white, new Color(148,
			145, 140)), labels.getString("LauncherFrame.dcsToolKitCompTitle"));
		decodesButtonPanel.setLayout(dcstoolLayout);
		int rows = 4 + (DecodesSettings.instance().showPlatformWizard ? 1 : 0)
			+ (DecodesSettings.instance().showNetlistEditor ? 1 : 0)
			+ (DecodesSettings.instance().showPlatformMonitor ? 1 : 0)
			+ (DecodesSettings.instance().showRoutingMonitor ? 1 : 0);
		if (dacqLauncherActions != null)
			rows += dacqLauncherActions.size();
		dcstoolLayout.setRows(rows);
		dcstoolLayout.setColumns(1);
		decodesButtonPanel.setBorder(dcstoolButtonBorder);
		
		lrgsStatButton.setText(labels.getString("LauncherFrame.lrgsStatButton"));
		lrgsStatButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				rtstatButtonPressed();
			}
		});

		browserButton.setText(labels.getString("LauncherFrame.messageBrowserButton"));
		browserButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				msgaccessButtonPressed();
			}
		});
		netlistButton.setText(labels.getString("LauncherFrame.networkListMaintButton"));
		netlistButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				nleditButtonPressed();
			}
		});
		dbeditButton.setText(labels.getString("LauncherFrame.DECODESDBEditorButton"));
		dbeditButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				dbeditButtonPressed();
			}
		});
		platwizButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				platwizButtonPressed();
			}
		});
		
		platmonButton.setText(labels.getString("LauncherFrame.platmonButton"));
		platmonButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				platmonButtonPressed();
			}
		});
		routmonButton.setText(labels.getString("LauncherFrame.routmonButton"));
		routmonButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				routmonButtonPressed();
			}
		});

		
		toolkitConfigButton.setText(labels.getString("LauncherFrame.DCSToolkitSetupButton"));
		toolkitConfigButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				setupPressed();
			}
		});

		decodesButtonPanel.add(lrgsStatButton, null);
		decodesButtonPanel.add(browserButton, null);

		// Don't add unless showNetlistButton is true
		if (DecodesSettings.instance().showNetlistEditor)
			decodesButtonPanel.add(netlistButton, null);
		decodesButtonPanel.add(dbeditButton, null);
		if (DecodesSettings.instance().showPlatformWizard)
			decodesButtonPanel.add(platwizButton, null);
		
		if (DecodesSettings.instance().showPlatformMonitor)
			decodesButtonPanel.add(platmonButton, null);
		if (DecodesSettings.instance().showRoutingMonitor)
			decodesButtonPanel.add(routmonButton, null);
		
		if (dacqLauncherActions != null)
			for(final LauncherAction action : dacqLauncherActions)
			{
				action.setLauncherFrame(this);
				action.setLauncherArgs(myArgs);
				JButton b = new JButton();
				b.setIcon(action.getImageIcon());
				b.setText(action.getButtonLabel());
				b.addActionListener(
					new java.awt.event.ActionListener()
					{
						@Override
						public void actionPerformed(ActionEvent e)
						{
							action.buttonPressed();
						}
					});
				decodesButtonPanel.add(b, null);
			}

		decodesButtonPanel.add(toolkitConfigButton, null);

		String borderString = labels.getString("LauncherFrame.hydroMetCompTitle");
		if (tsdbType == TsdbType.CWMS)
			borderString = "CWMS Database Components";
		else if (tsdbType == TsdbType.HDB)
			borderString = "HDB Database Components";

		tsdbButtonBorder = new TitledBorder(BorderFactory.createEtchedBorder(Color.white, new Color(148, 145,
			140)), borderString);
		tsdbButtonPanel.setLayout(tsdbLayout);

		rows = 1 +
			(DecodesSettings.instance().showTimeSeriesEditor ? 1 : 0) +
			(DecodesSettings.instance().showComputationEditor ? 1 : 0) +
			(DecodesSettings.instance().showGroupEditor ? 1 : 0) +
			(DecodesSettings.instance().showTestCmputations ? 1 : 0) +
			(DecodesSettings.instance().showAlgorithmEditor ? 1 : 0);
		tsdbLayout.setRows(rows);
		tsdbLayout.setColumns(1);
		tsdbButtonPanel.setBorder(tsdbButtonBorder);

		// ROW 0: Time Series Button
		tseditButton.setIcon(new ImageIcon(installDir + File.separator + "icons" + File.separator
			+ "tsedit48x48.gif"));
		tseditButton.setText("Time Series");
		tseditButton.setHorizontalAlignment(SwingConstants.CENTER);
		tseditButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				tseditButtonPressed();
			}
		});
		if (DecodesSettings.instance().showTimeSeriesEditor)
			tsdbButtonPanel.add(tseditButton);

		// ROW 1: Time Series Groups Button
		groupEditButton.setIcon(new ImageIcon(installDir + File.separator + "icons" + File.separator
			+ "groups48x48.png"));
		groupEditButton.setText("Time Series Groups");
		groupEditButton.setHorizontalAlignment(SwingConstants.CENTER);
		groupEditButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				groupEditButtonPressed();
			}
		});
		if (DecodesSettings.instance().showGroupEditor)
			tsdbButtonPanel.add(groupEditButton, null);

		// ROW 2: Computations Button
		compeditButton.setName("computations");
		compeditButton.setIcon(new ImageIcon(installDir + File.separator + "icons" + File.separator
			+ "compedit48x48.gif"));
		compeditButton.setText(labels.getString("LauncherFrame.computationsButton"));
		compeditButton.setHorizontalAlignment(SwingConstants.CENTER);
		compeditButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				compeditButtonPressed();
			}
		});
		if (DecodesSettings.instance().showComputationEditor)
			tsdbButtonPanel.add(compeditButton, null);

		// ROW 3: Run/Test Computations Button
		runcompButton.setIcon(new ImageIcon(installDir + File.separator + "icons" + File.separator
			+ "runcomp48x48.gif"));
		runcompButton.setText(labels.getString("LauncherFrame.testComputationsButton"));
		runcompButton.setHorizontalAlignment(SwingConstants.CENTER);
		runcompButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				runcompButtonPressed();
			}
		});
		if (DecodesSettings.instance().showTestCmputations)
			tsdbButtonPanel.add(runcompButton, null);

		// ROW 4: Process Status Button
		procstatButton.setIcon(new ImageIcon(installDir + File.separator + "icons" + File.separator
			+ "procstat48x48.gif"));
		procstatButton.setText(labels.getString("LauncherFrame.processStatusButton"));
		procstatButton.setHorizontalAlignment(SwingConstants.CENTER);
		procstatButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				procstatButtonPressed();
			}
		});
		tsdbButtonPanel.add(procstatButton, null);

		// ROW 4: Algorithm Edit Button
		algoeditButton.setIcon(new ImageIcon(installDir + File.separator + "icons" + File.separator
			+ "algoedit48x48.gif"));
		algoeditButton.setText(labels.getString("LauncherFrame.algorithmsButton"));
		algoeditButton.setHorizontalAlignment(SwingConstants.CENTER);
		algoeditButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				algoeditButtonPressed();
			}
		});
		if (DecodesSettings.instance().showAlgorithmEditor)
			tsdbButtonPanel.add(algoeditButton, null);
		
		fullPanel.setLayout(fullLayout);
		fullPanel.add(decodesButtonPanel, new GridBagConstraints(0, 1, 1, 1, .5, .5,
			GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 20, 0));
//		if (tsdbType != TsdbType.NONE)
//		{
			fullPanel.add(tsdbButtonPanel, new GridBagConstraints(0, 2, 1, 1, .5, .5,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 20, 0));
//		}
		contentPane.add(fullPanel, BorderLayout.CENTER);
		setupSaved();
	}


	void rtstatButtonPressed()
	{
		String profileName = getSelectedProfile();
		if (profileName != null)
		{
			sendToProfileLauncher(profileName, "start rtstat");
			return;
		}

		if (lrgsStatFrame != null)
		{
			lrgsStatFrame.toFront();
			return;
		}
		try
		{
			RtStat rtStat = new RtStat(myArgs);
			lrgsStatFrame = rtStat.getFrame();
			lrgsStatFrame.setExitOnClose(false);
			centerWindow(lrgsStatFrame);
			lrgsStatFrame.addWindowListener(lrgsStatReaper);
			lrgsStatFrame.setVisible(true);
		}
		catch(Exception ex)
		{
			showError(ex.getMessage());
		}
	}

	
	void msgaccessButtonPressed()
	{
		String profileName = getSelectedProfile();
		if (profileName != null)
		{
			sendToProfileLauncher(profileName, "start msgaccess");
			return;
		}

		if (browserFrame != null)
		{
			browserFrame.toFront();
			return;
		}
		// DB init in progress!
		if (afterDecodesInit != null)
			return;

		try
		{
			final LauncherFrame launcherFrame = this;
			afterDecodesInit = new Runnable()
			{
				public void run()
				{
					String hostname = "";
					int port = 16003;
					String user = "";
					launcherFrame.browserFrame = new MessageBrowser(hostname,
						port, user);
					launcherFrame.browserFrame
						.addWindowListener(launcherFrame.browserReaper);
					launcherFrame.browserFrame.startup(100, 100);
				}
			};
			completeDecodesInit();
		}
		catch (DecodesException ex)
		{
			Logger.instance().log(Logger.E_FAILURE,
				"Cannot initialize DECODES: '" + ex);
		}
	}

	void nleditButtonPressed()
	{
		if (netlistEditFrame != null)
		{
			netlistEditFrame.toFront();
			return;
		}
		NetlistEditFrame.netlistDir = 
			EnvExpander.expand("$DECODES_INSTALL_DIR/netlist");
		netlistEditFrame = new NetlistEditFrame();
		netlistEditFrame.setStandAlone(false);
		centerWindow(netlistEditFrame);
		netlistEditFrame.addWindowListener(netlistEditReaper);
		netlistEditFrame.setVisible(true);
	}

	void dbeditButtonPressed()
	{
		String profileName = getSelectedProfile();
		if (profileName != null)
		{
			sendToProfileLauncher(profileName, "start dbedit");
			return;
		}

		if (dbEditorFrame != null)
		{
			dbEditorFrame.toFront();
			return;
		}
		// DB init in progress!
		if (afterDecodesInit != null)
			return;

		try
		{
			final LauncherFrame launcherFrame = this;
			afterDecodesInit = new Runnable()
			{
				public void run()
				{
					launcherFrame.dbEditorFrame = new DbEditorFrame();
//					launcherFrame.centerWindow(launcherFrame.dbEditorFrame);
					launcherFrame.dbEditorFrame.setExitOnClose(false);
					launcherFrame.dbEditorFrame.addWindowListener(launcherFrame.dbEditorReaper);
					launcherFrame.dbEditorFrame.setVisible(true);
				}
			};
			completeDecodesInit();
		}
		catch (DecodesException ex)
		{
			Logger.instance().log(Logger.E_FAILURE,
				"Cannot initialize DECODES: '" + ex);
		}
	}

	void setupPressed()
	{
		String profileName = getSelectedProfile();
		if (profileName != null)
		{
			sendToProfileLauncher(profileName, "start setup");
			return;
		}

		if (toolkitSetupFrame != null)
		{
			toolkitSetupFrame.toFront();
			return;
		}
		toolkitSetupFrame = new DecodesSetupFrame(this);
		toolkitSetupFrame.setExitOnClose(false);
		toolkitSetupFrame.addWindowListener(setupFrameReaper);
		toolkitSetupFrame.setVisible(true);
	}
	
	/**
	 * This method is called after a setup change.
	 * Enable or Disable the time-series buttons depending on the selected database type.
	 */
	void setupSaved()
	{
		boolean dbSupportsTS = DecodesSettings.instance().editDatabaseTypeCode != 
			DecodesSettings.DB_XML;
		tseditButton.setEnabled(dbSupportsTS);
		groupEditButton.setEnabled(dbSupportsTS);
		compeditButton.setEnabled(dbSupportsTS);
		runcompButton.setEnabled(dbSupportsTS);
		algoeditButton.setEnabled(dbSupportsTS);
	}

	void completeDecodesInit() throws DecodesException
	{
		initDecodesFrame = new InitDecodesFrame(getLabels());
		centerWindow(initDecodesFrame);
		initDecodesFrame.setVisible(true);

		/*
		 * Coordinate with GUI thread as follows: - DB init is done in separate
		 * thread, not GUI thread. - This method must return so that splash
		 * frame can be displayed (in the GUI thread). - DB init thread calls
		 * 'doAfterDbInit' when it's finished. - that method runs a Runnable in
		 * the GUI thread to display either the message browser or the db
		 * editor.
		 */
		final LauncherFrame launcherFrame = this;
		Thread initThread = new Thread()
		{
			public void run()
			{
				// On Linux, sometimes the init is so fast that it happens
				// before the splash screen paints. Thus the dismiss comes
				// before the paint and we are left with a splash screen up.
				// The following fixes this by pausing a half-second
				try { sleep(500L); } catch(InterruptedException ex) {}
				
				Platform.configSoftLink = true;

				// Note: Editing is a superset of decoding:
				try
				{
					if (!DecodesInterface.isInitialized())
					{
						doInitDecodes();
						DecodesInterface.initializeForEditing();
					}
				}
				catch (DecodesException ex)
				{
					final String msg = labels
						.getString("LauncherFrame.cannotInitDecodes") + ex;
					Logger.instance().log(Logger.E_FAILURE, msg);
					SwingUtilities.invokeLater(new Runnable()
					{
						public void run()
						{
							JOptionPane.showMessageDialog(null,
								AsciiUtil.wrapString(msg, 60), "Error!",
								JOptionPane.ERROR_MESSAGE);
						}
					});
					afterDecodesInit = null;
				}
				launcherFrame.doAfterDbInit();
			}
		};
		initThread.start();
	}

	public void doAfterDbInit()
	{
		if (initDecodesFrame != null)
			initDecodesFrame.setVisible(false);
		initDecodesFrame = null;
		if (afterDecodesInit != null)
			SwingUtilities.invokeLater(afterDecodesInit);
		afterDecodesInit = null;
	}

	// private void centerWindow(JFrame frame)
	private void centerWindow(Window frame)
	{
		// Center the window
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension frameSize = frame.getSize();
		if (frameSize.height > screenSize.height)
		{
			frameSize.height = screenSize.height;
		}
		if (frameSize.width > screenSize.width)
		{
			frameSize.width = screenSize.width;
		}
		frame.setLocation((screenSize.width - frameSize.width) / 2,
			(screenSize.height - frameSize.height) / 2);
	}

	static CmdLineArgs cmdLineArgs = new CmdLineArgs(true, "gui.log");
	private ArrayList<LauncherAction> dacqLauncherActions;

	public static void main(String[] args)
	{

		noCompFilterToken = new BooleanToken("L",
			"Disable Computation List filter (default=enabled)", "",
			TokenOptions.optSwitch, false);
		cmdLineArgs.addToken(noCompFilterToken);
		justPrintVersionToken = new BooleanToken("V", "Print Version and Exit",
			"", TokenOptions.optSwitch, false);
		cmdLineArgs.addToken(justPrintVersionToken);
		
		IntegerToken profilePortToken = new IntegerToken("pp",
				"When running as ProfileLauncher this is the control port to connect to", "",
				TokenOptions.optSwitch, -1);
		cmdLineArgs.addToken(profilePortToken);
		
		cmdLineArgs.parseArgs(args);
		if (justPrintVersionToken.getValue())
		{
			System.out.println(ResourceFactory.instance().startTag());
			System.exit(0);
		}

		Logger.instance().log(
			Logger.E_INFORMATION,
			"DCS Toolkit GUI Version " + ResourceFactory.instance().startTag()
					+ " Starting");

		// Verify if we need to set the Java Locale
		DecodesSettings settings = DecodesSettings.instance();
		LoadResourceBundle.setLocale(settings.language);

		labels = getLabels();

		// Make a copy of the args minus special launcher args to pass to sub programs.
		ArrayList<String> argsArray = new ArrayList<String>();
		boolean skipNext = false;
		for (int i = 0; i < args.length; i++)
		{
			if (skipNext)
				skipNext = false;
			else if (args[i].equals("-L"))
				;
			else if (args[i].startsWith("-pp"))
			{
				if (args[i].length()==3)
					skipNext = true;
			}
			else
			{
				argsArray.add(args[i]);
			}
		}
		String argsCopy[] = new String[argsArray.size()];
		argsArray.toArray(argsCopy);
		LauncherFrame frame = new LauncherFrame(argsCopy);
	
		// compArgs is used as argument only to Computations and Test Computations
		// gui.
//		frame.compArgs = args;

		frame.setExitOnClose(true);

		// Initialize the 'GuiApp' object used by LRGS & DECODES GUI's:
		GuiApp.setAppName("DCS Toolkit GUI");
		String propFile = EnvExpander.expand(
			"$DECODES_INSTALL_DIR/lrgsgui.properties", System.getProperties());
		try
		{
			GuiApp.loadProperties(propFile);
		}
		catch (FileNotFoundException ex)
		{
			Logger.instance().log(Logger.E_DEBUG1,
				"Cannot read '" + propFile + "' -- will use defaults.");
		}

		// Center the window
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension frameSize = frame.getSize();
		if (frameSize.height > screenSize.height)
		{
			frameSize.height = screenSize.height;
		}
		if (frameSize.width > screenSize.width)
		{
			frameSize.width = screenSize.width;
		}
		frame.setLocation(0, 0);
		
		DecodesInterface.maintainGoesPdt();
		
		if (profilePortToken.getValue() == -1)
		{
			frame.checkForProfiles();
			frame.setVisible(true);
		}
		else
		{
			try
			{
				frame.runProfileLauncher(profilePortToken.getValue());
			}
			catch (Exception ex)
			{
				System.err.println("Abnormal exit: " + ex);
				ex.printStackTrace(System.err);
				System.exit(1);
			}
		}
	}

	public static ResourceBundle getLabels()
	{
		if (labels == null)
		{
			DecodesSettings settings = DecodesSettings.instance();
			// Return the main label descriptions for launchre frame App
			labels = LoadResourceBundle.getLabelDescriptions(
				"decodes/resources/launcherframe", settings.language);
		}
		return labels;
	}

	private void doInitDecodes() throws DecodesException
	{
		// Load the DCS Tool Configuration

		// Initialize minimal DECODES database.
		try
		{
			DecodesInterface.setGUI(true);
			DecodesInterface.initDecodes(
				EnvExpander.expand(
					"$DECODES_INSTALL_DIR/decodes.properties"));
		}
		catch (DecodesException ex)
		{
			String msg = "Error initializing DECODES database: " + ex;
			Logger.instance().log(Logger.E_FAILURE, msg);
			System.err.println(msg);
			// showError(msg);
			throw ex;
		}
	}

	/**
	 * Starts a modal error dialog with the passed message.
	 * 
	 * @param msg
	 *            the error message
	 */
	public void showError(String msg)
	{
		Logger.instance().log(Logger.E_FAILURE, msg);
		JOptionPane.showMessageDialog(this, AsciiUtil.wrapString(msg, 60),
			"Error!", JOptionPane.ERROR_MESSAGE);
	}

	public String getProfilePath()
	{
		return profilePath;
	}
	
	private void profileComboChanged()
	{
		// Each time the profile selection combo box changes, check the new properties file
		// Activate/Deactivate tsdb buttons depending on database type
		String profileName = getSelectedProfile();
		profilePath = EnvExpander.expand("$DCSTOOL_USERDIR");
		String dcstoolPath = EnvExpander.expand("$DCSTOOL_HOME");

		final String defaultFile = profilePath.equalsIgnoreCase(dcstoolPath)
								 ? "decodes.properties"
								 : "user.properties";
		
		if(profileName == null)
		{
			profilePath = profilePath + File.separatorChar + defaultFile;
		}
		else
		{
			profilePath = profilePath + File.separatorChar + profileName + ".profile";
		}
		
		DecodesSettings ProfileSettings = new DecodesSettings();
		Properties props = new Properties();
		
		try
		{
			FileInputStream fis = new FileInputStream(profilePath);
			props.load(fis);
			fis.close();
		}
		catch (IOException ex)
		{
			Logger.instance().failure("Cannot open DECODES Properties File '" + profilePath + "': " + ex);
		}
		
		ProfileSettings.loadFromProperties(props);
		
		boolean dbSupportsTS = ProfileSettings.editDatabaseTypeCode != DecodesSettings.DB_XML;
		tseditButton.setEnabled(dbSupportsTS);
		groupEditButton.setEnabled(dbSupportsTS);
		compeditButton.setEnabled(dbSupportsTS);
		runcompButton.setEnabled(dbSupportsTS);
		algoeditButton.setEnabled(dbSupportsTS);
		
	}
	// Time Series Button
	private void groupEditButtonPressed()
	{
		String profileName = getSelectedProfile();
		if (profileName != null)
		{
			sendToProfileLauncher(profileName, "start groupedit");
			return;
		}
		if (groupEditFrame != null)
		{
			groupEditFrame.toFront();
			return;
		}

		// DB init in progress!
		if (afterDecodesInit != null)
			return;
		
		try
		{
			afterDecodesInit = new Runnable()
			{
				public void run()
				{
					try
					{
						groupEditFrame = ResourceFactory.instance().getTsdbEditorFrame(myArgs);
						if (groupEditFrame != null)
						{
							groupEditFrame.setExitOnClose(false);
							groupEditFrame.addWindowListener(tsGroupReaper);
						}
					}
					catch (Exception ex)
					{
						groupEditFrame = null;
						String msg = LoadResourceBundle.sprintf(
							labels.getString("LauncherFrame.cannotLaunch"),
							labels.getString("LauncherFrame.timeSeriesButton"))
								+ ex;
						Logger.instance().warning(msg);
						System.err.println(msg);
						ex.printStackTrace();
						showError(msg);
					}
				}
			};
			completeDecodesInit();
		}
		catch (DecodesException ex)
		{
			Logger.instance().log(Logger.E_FAILURE, "Cannot initialize DECODES: '" + ex);
		}
	}
	
	protected void tseditButtonPressed()
	{
		String profileName = getSelectedProfile();
		if (profileName != null)
		{
			sendToProfileLauncher(profileName, "start tsedit");
			return;
		}

		if (tseditFrame != null)
		{
			tseditFrame.toFront();
			return;
		}

		// DB init in progress!
		if (afterDecodesInit != null)
			return;

		try
		{
			afterDecodesInit = new Runnable()
			{
				public void run()
				{
					try
					{
						TsListMain tsListMain = new TsListMain();
						tsListMain.setExitOnClose(false);
						tsListMain.execute(myArgs);
						tseditFrame = tsListMain.getFrame();
						if (tseditFrame != null)
						{
							tseditFrame.setExitOnClose(false);
							tseditFrame.addWindowListener(tseditReaper);
						}
					}
					catch (Exception ex)
					{
						tseditFrame = null;
						String msg = LoadResourceBundle.sprintf(
							labels.getString("LauncherFrame.cannotLaunch"),
							"Time Series Editor")
								+ ex;
						Logger.instance().warning(msg);
						System.err.println(msg);
						ex.printStackTrace();
						showError(msg);
					}
				}
			};
			completeDecodesInit();
		}
		catch (DecodesException ex)
		{
			Logger.instance().log(Logger.E_FAILURE, "Cannot initialize DECODES: '" + ex);
		}
	}

	private void compeditButtonPressed()
	{
		String profileName = getSelectedProfile();
		if (profileName != null)
		{
			sendToProfileLauncher(profileName, "start compedit");
			return;
		}

		if (compEditFrame != null)
		{
			compEditFrame.toFront();
			return;
		}

		// DB init in progress!
		if (afterDecodesInit != null)
			return;

		try
		{
			afterDecodesInit = new Runnable()
			{
				public void run()
				{
					CAPEdit compEdit = new CAPEdit();
					try
					{
						compEdit.execute(myArgs);
						compEditFrame = compEdit.getFrame();
						if (compEditFrame != null)
						{
							compEdit.setExitOnClose(false);
							compEditFrame.addWindowListener(compEditReaper);
						}
					}
					catch(Exception ex)
					{
						compEditFrame = null;
						String msg = LoadResourceBundle.sprintf(
							labels.getString("LauncherFrame.cannotLaunch"),
							labels.getString("LauncherFrame.computationsButton"))
								+ ex;
						Logger.instance().warning(msg);
						System.err.println(msg);
						ex.printStackTrace();
						showError(msg);
					}
				}
			};
			completeDecodesInit();
		}
		catch (DecodesException ex)
		{
			Logger.instance().log(Logger.E_FAILURE, "Cannot initialize DECODES: '" + ex);
		}
	}

	protected void routmonButtonPressed()
	{
		String profileName = getSelectedProfile();
		if (profileName != null)
		{
			sendToProfileLauncher(profileName, "start routmon");
			return;
		}

		if (routmonFrame != null)
		{
			routmonFrame.toFront();
			return;
		}

		// DB init in progress!
		if (afterDecodesInit != null)
			return;

		try
		{
			afterDecodesInit = new Runnable()
			{
				public void run()
				{
					
					RoutingMonitor routmon = new RoutingMonitor();
					try
					{
						routmon.execute(myArgs);
						routmonFrame = routmon.getFrame();
						if (routmonFrame != null)
						{
							routmon.setExitOnClose(false);
							routmonFrame.addWindowListener(routmonReaper);
						}
					}
					catch(Exception ex)
					{
						routmonFrame = null;
						String msg = LoadResourceBundle.sprintf(
							labels.getString("LauncherFrame.cannotLaunch"),
							labels.getString("LauncherFrame.routmonButton")) + ex;
						Logger.instance().warning(msg);
						System.err.println(msg);
						ex.printStackTrace();
						showError(msg);
					}
				}
			};
			completeDecodesInit();
		}
		catch (DecodesException ex)
		{
			Logger.instance().log(Logger.E_FAILURE, "Cannot initialize DECODES: '" + ex);
		}
	}

	protected void platmonButtonPressed()
	{
		String profileName = getSelectedProfile();
		if (profileName != null)
		{
			sendToProfileLauncher(profileName, "start platmon");
			return;
		}

		if (platmonFrame != null)
		{
			platmonFrame.toFront();
			return;
		}

		// DB init in progress!
		if (afterDecodesInit != null)
			return;

		try
		{
			afterDecodesInit = new Runnable()
			{
				public void run()
				{
					
					PlatformMonitor platmon = new PlatformMonitor();
					try
					{
						platmon.execute(myArgs);
						platmonFrame = platmon.getFrame();
						if (platmonFrame != null)
						{
							platmon.setExitOnClose(false);
							platmonFrame.addWindowListener(platmonReaper);
						}
					}
					catch(Exception ex)
					{
						platmonFrame = null;
						String msg = LoadResourceBundle.sprintf(
							labels.getString("LauncherFrame.cannotLaunch"),
							labels.getString("LauncherFrame.platmonButton")) + ex;
						Logger.instance().warning(msg);
						System.err.println(msg);
						ex.printStackTrace();
						showError(msg);
					}
				}
			};
			completeDecodesInit();
		}
		catch (DecodesException ex)
		{
			Logger.instance().log(Logger.E_FAILURE, "Cannot initialize DECODES: '" + ex);
		}
	}

	
	
	private void runcompButtonPressed()
	{
		String profileName = getSelectedProfile();
		if (profileName != null)
		{
			sendToProfileLauncher(profileName, "start runcomp");
			return;
		}

		if (runComputationsFrame != null)
		{
			runComputationsFrame.toFront();
			return;
		}

		// DB init in progress!
		if (afterDecodesInit != null)
			return;

		try
		{
			afterDecodesInit = new Runnable()
			{
				public void run()
				{
					try
					{
						RunComputationsFrameTester runComputations = new RunComputationsFrameTester();
						runComputations.execute(myArgs);

						runComputationsFrame = runComputations.getFrame();

						if (runComputationsFrame != null)
						{
							runComputationsFrame.setRunCompFrametester(runComputations);
							runComputationsFrame.setExitOnClose(false);
							runComputationsFrame.addWindowListener(runComputationsReaper);
						}
					}
					catch (Exception ex)
					{
						runComputationsFrame = null;
						String msg = LoadResourceBundle.sprintf(
							labels.getString("LauncherFrame.cannotLaunch"),
							labels.getString("LauncherFrame.testComputationsButton"))
								+ ex;
						Logger.instance().warning(msg);
						System.err.println(msg);
						ex.printStackTrace();
						showError(msg);
					}
				}
			};
			completeDecodesInit();
		}
		catch (DecodesException ex)
		{
			Logger.instance().log(Logger.E_FAILURE, "Cannot initialize DECODES: '" + ex);
		}
	}

	private void procstatButtonPressed()
	{
		String profileName = getSelectedProfile();
		if (profileName != null)
		{
			sendToProfileLauncher(profileName, "start procstat");
			return;
		}

		if (procMonFrame != null)
		{
			procMonFrame.toFront();
			return;
		}

		// DB init in progress!
		if (afterDecodesInit != null)
			return;
		
		try
		{
			afterDecodesInit = new Runnable()
			{
				public void run()
				{
					try
					{
						ProcessMonitor procMon = new ProcessMonitor();
						procMon.execute(myArgs);
						procMonFrame = procMon.getFrame();
						if (procMonFrame != null)
						{
							procMon.setExitOnClose(false);
							procMonFrame.addWindowListener(procMonReaper);
						}
					}
					catch (Exception ex)
					{
						procMonFrame = null;
						String msg = LoadResourceBundle.sprintf(
							labels.getString("LauncherFrame.cannotLaunch"),
							labels.getString("LauncherFrame.processStatusButton"))
								+ ex;
						Logger.instance().warning(msg);
						System.err.println(msg);
						ex.printStackTrace();
						showError(msg);
					}
				}
			};
			completeDecodesInit();
		}
		catch (DecodesException ex)
		{
			Logger.instance().log(Logger.E_FAILURE, "Cannot initialize DECODES: '" + ex);
		}
	}

	private void algoeditButtonPressed()
	{
		String profileName = getSelectedProfile();
		if (profileName != null)
		{
			sendToProfileLauncher(profileName, "start algoedit");
			return;
		}

		if (algorithmWizFrame != null)
		{
			algorithmWizFrame.toFront();
			return;
		}
		

		try
		{
			AlgorithmWizard algoWiz = new AlgorithmWizard();
			algoWiz.execute(myArgs);
			algorithmWizFrame = algoWiz.getFrame();
			if (algorithmWizFrame != null)
			{
				algoWiz.setExitOnClose(false);
				algorithmWizFrame.addWindowListener(algorithmWizReaper);
			}
		}
		catch (Exception ex)
		{
			algorithmWizFrame = null;
			String msg = LoadResourceBundle.sprintf(
				labels.getString("LauncherFrame.cannotLaunch"),
				labels.getString("LauncherFrame.algorithmsButton"))
					+ ex;
			Logger.instance().warning(msg);
			System.err.println(msg);
			ex.printStackTrace();
			showError(msg);
		}
	}

	private void platwizButtonPressed()
	{
		String profileName = getSelectedProfile();
		if (profileName != null)
		{
			sendToProfileLauncher(profileName, "start platwiz");
			return;
		}

		if (platformWizFrame != null)
		{
			platformWizFrame.toFront();
			return;
		}

		// DB init in progress!
		if (afterDecodesInit != null)
			return;
		try
		{
			final LauncherFrame launcherFrame = this;
			afterDecodesInit = new Runnable()
			{
				public void run()
				{
					// PlatformWizard platWiz = new PlatformWizard();
					PlatformWizard.resetInstance();
					PlatformWizard platWiz = PlatformWizard.instance();
					// NOTE NOTE
					// If we do not set this to false the Define Platform
					// Sensor Panel does not work - It does not let me add
					// a configuration
					Platform.configSoftLink = false;
					decodes.db.Database db = decodes.db.Database.getDb();
					db.platformConfigList.countPlatformsUsing();
					platWiz.show();
					launcherFrame.platformWizFrame = platWiz.getFrame();
					if (launcherFrame.platformWizFrame != null)
					{
						launcherFrame.platformWizFrame.exitOnClose = false;
						launcherFrame.platformWizFrame
							.addWindowListener(launcherFrame.platformWizReaper);
					}
				}
			};
			completeDecodesInit();
			// decodes.db.Database db = decodes.db.Database.getDb();
			// Platform.configSoftLink = false;
			// db.platformConfigList.countPlatformsUsing();
			// PlatformWizard platWiz = new PlatformWizard();
			// //PlatformWizard platWiz = PlatformWizard.instance();
			// platWiz.show();
			// platformWizFrame = platWiz.getFrame();
			// if (platformWizFrame != null)
			// {
			// platformWizFrame.exitOnClose = false;
			// platformWizFrame.addWindowListener(platformWizReaper);
			// }
		}
		catch (Exception ex)
		{
			platformWizFrame = null;
			String msg = LoadResourceBundle.sprintf(
				labels.getString("LauncherFrame.cannotLaunch"),
				labels.getString("LauncherFrame.platformWizardButton"))
					+ ex;
			Logger.instance().warning(msg);
			System.err.println(msg);
			ex.printStackTrace();
			showError(msg);
		}
	}

	class Reply { String reply = null; }
	
	/**
	 * When running as a slave profile manager, this method is called instead of making the 
	 * frame visible. It connects to the master launcher on the specified port and accepts 
	 * commands of the form:
	 *     num cmd arg\n
	 * where num is an incrementing index number.
	 * After executing the command a reply is sent back to the master with the index number.
	 * Called with port=0 for testing in a terminal window.
	 * @param port The TCP port to connect to on localhost or 0 to accept commands from stdin
	 */
	private void runProfileLauncher(int port)
		throws Exception
	{
		InputStream inp = System.in;
		PrintStream outp = System.out;
		BasicClient client = null;
		
		String profileName = cmdLineArgs.getPropertiesFile();
		if (profileName == null || profileName.trim().length() == 0)
			throw new Exception("Missing properties file on cmd line");
		profileName = profileName.trim();
		int idx = profileName.lastIndexOf('.');
		if (idx > 0)
			profileName = profileName.substring(0, idx);
		Logger.instance().info("======== Launcher Starting for profile '" + profileName + "', "
			+ "debuglevel=" + Logger.priorityName[Logger.instance().getMinLogPriority()]);
		
		if (port > 0)
		{
			client = new BasicClient("localhost", port);
			Logger.instance().info("Connecting to parent launcher at localhost:" + port);
			try
			{ 
				client.connect();
				Logger.instance().info(".....connect successful.");
				inp = client.getInputStream();
				outp = new PrintStream(client.getOutputStream());
				String pn = profileName;
				int slash = pn.lastIndexOf('/');
				if (slash < 0)
					slash = pn.lastIndexOf('\\');
				if (slash > 0)
					pn = pn.substring(slash+1);
				
				outp.println(pn);
			}
			catch(IOException ex)
			{
				Logger.instance().failure("Cannot connect to localhost:" + port + " " + ex);
				throw ex;
			}
		}
		
		try
		{
			BufferedReader br = new BufferedReader(new InputStreamReader(inp));
			String line;
			while((line = br.readLine()) != null)
			{
				String words[] = line.split(" ");
				if (words.length < 2)
					continue;
				
				String cmdnum = words[0];
				final String cmd = words[1];
				final String arg = words.length > 2 ? words[2] : "";
Logger.instance().info("read command '" + line + "' cmd='" + cmd + "' arg='" + arg + "'");				
				
				if (cmd.equalsIgnoreCase("kill"))
				{
					Logger.instance().info("Exiting due to kill command.");
					break;
				}
				
				// Anything GUI-related (button pushes, frame changes, etc.) must be done in the swing thread.
				final Reply reply = new Reply();
				SwingUtilities.invokeLater(
					new Runnable()
					{
						public void run()
						{
							if (cmd.equalsIgnoreCase("status"))
							{
								reply.reply = "status good";
							}
							else if (cmd.equalsIgnoreCase("start"))
							{
								// start <buttonName>: act as if named button were pressed
								boolean found = true;
								if (arg.equalsIgnoreCase("groupedit"))
									groupEditButtonPressed();
								else if (arg.equalsIgnoreCase("rtstat"))
									rtstatButtonPressed();
								else if (arg.equalsIgnoreCase("msgaccess"))
									msgaccessButtonPressed();
								else if (arg.equalsIgnoreCase("dbedit"))
									dbeditButtonPressed();
								else if (arg.equalsIgnoreCase("platmon"))
									platmonButtonPressed();
								else if (arg.equalsIgnoreCase("routmon"))
									routmonButtonPressed();
								else if (arg.equalsIgnoreCase("setup"))
									setupPressed();
								else if (arg.equalsIgnoreCase("nledit"))
									nleditButtonPressed();
								else if (arg.equalsIgnoreCase("platwiz"))
									platwizButtonPressed();
								else if (arg.equalsIgnoreCase("compedit"))
									compeditButtonPressed();
								else if (arg.equalsIgnoreCase("tsedit"))
									tseditButtonPressed();
								else if (arg.equalsIgnoreCase("runcomp"))
									runcompButtonPressed();
								else if (arg.equalsIgnoreCase("procstat"))
									procstatButtonPressed();
								else if (arg.equalsIgnoreCase("algoedit"))
									algoeditButtonPressed();
								else
									found = false;
								if (dacqLauncherActions != null)
									for(LauncherAction action : dacqLauncherActions)
{Logger.instance().info("checking action '" + action.getTag() + "' against arg='" + arg + "'");
										if (action.getTag().equals(arg))
										{
											action.launchFrame();
											found = true;
										}
}
								if (found)
									reply.reply = cmd + " " + arg;
								else
									reply.reply = "error: no such app '" + arg + "'";
							}
							else if (cmd.equalsIgnoreCase("exit"))
							{
								String msg = canClose();
								if (msg != null)
									reply.reply = "error: " + msg;
								else
									reply.reply = "exit";
							}
							else
							{
								reply.reply = "error: Unknown command '" + cmd + "' -- ignored.";
							}
						}
					});
				
				// Allow up to 10 seconds for command to complete.
				while(reply.reply == null)
					try { Thread.sleep(250L); } catch(InterruptedException ex) {}
				outp.println(cmdnum + " " + (reply.reply == null ? "error: no reply" : reply.reply));
			}
		}
		finally
		{
			if (client != null)
				client.disconnect();
		}
		System.exit(0);
		
	}
	
	static String[] getProfileList()
	{
		File userDir = new File(EnvExpander.expand("$DCSTOOL_USERDIR"));
		File [] profileList = userDir.listFiles(
			new FilenameFilter()
			{
				@Override
				public boolean accept(File dir, String name)
				{
					// TODO Auto-generated method stub
					return TextUtil.endsWithIgnoreCase(name, ".profile");
				}
			});
		String [] names = new String[profileList.length + 1];
		names[0] = "(default)";
		for(int idx = 0; idx<profileList.length; idx++)
		{
			String n = profileList[idx].getName();
			names[idx+1] = n.substring(0, n.indexOf(".profile"));
		}
		return names;
	}

	// package private so GUI test setup can call.
	void checkForProfiles()
	{

		if (profPanel == null)
		{
			profPanel = new JPanel(new GridBagLayout());
			profPanel.add(new JLabel("Profile:"),
				new GridBagConstraints(0, 0, 1, 1, 0, 0,
					GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(2, 2, 2, 0), 0, 0));
			profileCombo = new JComboBox<>();
			profileCombo.setName("profileCombo");
			profileCombo.addActionListener(e -> profileComboChanged());


			profPanel.add(profileCombo,
				new GridBagConstraints(1, 0, 1, 1, .5, 0,
					GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 0, 2, 2), 10, 0));
			JButton profMgrButton = new JButton("...");
			profPanel.add(profMgrButton,
				new GridBagConstraints(2, 0, 1, 1, 0, 0,
					GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
			profMgrButton.addActionListener(e->startProfileManager());
		}
		
		String [] profileNames = getProfileList();
		Logger.instance().debug3("There are " + profileNames.length + " profiles.");
		if (profileNames != null && profileNames.length > 1)
		{
			profileCombo.removeAllItems();
			for(String item : profileNames)
				profileCombo.addItem(item);
			
			if (!profilesShown)
			{
				fullPanel.add(profPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, .1,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 20, 0));
				fullPanel.getIgnoreRepaint();
				profilesShown = true;
			}
		}
		else // only 1 "default" profile
		{
			// No harm done if profPanel is currently not displayed.
			fullPanel.remove(profPanel);
			fullPanel.repaint();
			profilesShown = false;
		}
		
	}

	protected void startProfileManager()
	{
		if (profileMgrFrame != null)
		{
			profileMgrFrame.toFront();
			return;
		}
		
		try
		{
			ProfileManager profileMgr = new ProfileManager();
			profileMgr.setExitOnClose(false);
			profileMgr.execute(myArgs);
			profileMgrFrame = profileMgr.getFrame();
			if (profileMgrFrame != null)
			{
				profileMgrFrame.setExitOnClose(false);
				profileMgrFrame.addWindowListener(profileMgrReaper);
			}
		}
		catch (Exception ex)
		{
			profileMgrFrame = null;
			String msg = LoadResourceBundle.sprintf(
				labels.getString("LauncherFrame.cannotLaunch"),
				"Profile Manager") + ex;
			Logger.instance().warning(msg);
			System.err.println(msg);
			ex.printStackTrace();
			showError(msg);
		}
	}

	/**
	 * @return null if default profile is in effect, otherwise, the selected profile name.
	 */
	public String getSelectedProfile()
	{
		if (profileCombo == null || profileCombo.getSelectedIndex() == 0)
			return null;
		return (String)profileCombo.getSelectedItem();
	}
	
	

	/**
	 * Send the command to the child launcher for the named profile.
	 * If the server is not yet running, start it listening on specifed port.
	 * If there is no child launcher for this profile, spawn the process and wait
	 * for it to connect.
	 * @param profileName the profile name
	 * @param cmd the command to send
	 */
	void sendToProfileLauncher(String profileName, String cmd)
	{
		final int listenPort = DecodesSettings.instance().profileLauncherPort;
		if (profileLauncherServer == null)
		{
			final LauncherFrame launcherFrame = this;
			try
			{
				Logger.instance().info("Opening listening socket for launchers on port " + listenPort);
				profileLauncherServer = 
					new BasicServer(listenPort)
//					new BasicServer(listenPort, InetAddress.getLocalHost())
					{
						protected BasicSvrThread newSvrThread( Socket sock ) 
							throws IOException
							{
								Logger.instance().info("New client connected on port " + listenPort);
								return new ProfileLauncherConn(this, sock, launcherFrame);
							}
					};
				// Call listen in a separate thread.
				(new Thread()
				{
					public void run()
					{
						try
						{
							profileLauncherServer.listen();
						}
						catch (IOException ex)
						{
							String msg = "Cannot start server listening thread on port "
								+ listenPort + ": " + ex;
							Logger.instance().failure(msg);
							profileLauncherServer = null;
							launcherFrame.showError(msg);
						}
					}
				}).start();
				Logger.instance().info("Success -- listening socket open on " + listenPort);
			}
			catch (IOException ex)
			{
				String msg = "Cannot open listening socket on port " + listenPort + ": " + ex;
				Logger.instance().failure(msg);
				showError(msg);
				profileLauncherServer = null;
				return;
			}
		}
		
		// Look through the matching connections for the specified profile.
		ProfileLauncherConn conn = searchForConn(profileName);
		if (conn == null)
		{
			// Use ilex.util.ProcWaiterThread to spawn the child launcher process.
			String launchCmd = EnvExpander.expand("$DCSTOOL_HOME/bin/launcher_start");
			launchCmd = launchCmd.replace('/', File.separatorChar);
			launchCmd = launchCmd.replace('\\', File.separatorChar);
			if (File.separatorChar == '\\')
				launchCmd = launchCmd + ".bat";
			launchCmd = launchCmd + " -pp " + listenPort + " -P " + profileName + ".profile"
				+ " -l " + "launcher-" + profileName + ".log -d1";
			try
			{
				Logger.instance().info("No launcher exists for profile '" + profileName
					+ "' -- launching command '" + launchCmd + "'");
				ProcWaiterThread.runBackground(launchCmd, "launch-" + profileName, this, profileName);
			}
			catch (IOException ex)
			{
				String msg = "Cannot execute command '" + launchCmd + "': " + ex;
				Logger.instance().failure(msg);
				showError(msg);
				return;
			}
			
			// Wait up to 10 sec for the process to start and connect to us.
			long start = System.currentTimeMillis();
			while(conn == null && System.currentTimeMillis() - start < 10000L)
			{
				if ((conn = searchForConn(profileName)) == null)
					try { Thread.sleep(200L); } catch(InterruptedException ex) {}
			}
			if (conn == null)
			{
				String msg = "Launcher with cmd '" + launchCmd + "' failed to connect after 10 sec.";
				Logger.instance().failure(msg);
				showError(msg);
				return;
			}
		}
		conn.sendCmd(cmd);
	}
	
	private ProfileLauncherConn searchForConn(String profileName)
	{
		for(Object connobj : profileLauncherServer.getAllSvrThreads())
			if (TextUtil.strEqualIgnoreCase(
				((ProfileLauncherConn)connobj).getProfileName(), profileName))
				return (ProfileLauncherConn)connobj;
		return null;
	}

	/**
	 * This method is called from ProcWaiterThread if a subordinate launcher exits.
	 */
	@Override
	public void procFinished(String procName, Object obj, int exitStatus)
	{
		// Subordinate launcher processes are not supposed to end, ever.
		Logger.instance().failure("Subordinate " + procName + " exited with status=" + exitStatus);

		// Make sure the ProfileLauncherConn is removed from the server.
		String profileName = (String)obj;
		ProfileLauncherConn conn = searchForConn(profileName);
		if (conn != null)
			conn.disconnect();
	}
	
	//TODO verify that subordinate launcher processes do not survive the launcher going down.

}
