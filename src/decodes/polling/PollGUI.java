package decodes.polling;

import ilex.gui.EventsPanel;
import ilex.gui.EventsPanelQueueThread;
import ilex.gui.ShowFileDialog;
import ilex.gui.TextAreaOutputStream;
import ilex.util.EnvExpander;
import ilex.util.IDateFormat;
import ilex.util.Logger;
import ilex.util.QueueLogger;
import ilex.util.TeeLogger;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.text.DefaultCaret;

import opendcs.dai.PlatformStatusDAI;
import decodes.consumer.DirectoryConsumer;
import decodes.db.Constants;
import decodes.db.Database;
import decodes.db.Platform;
import decodes.db.PlatformStatus;
import decodes.db.RoutingSpec;
import decodes.db.TransportMedium;
import decodes.dbeditor.PlatformSelectDialog;
import decodes.gui.TopFrame;
import decodes.routing.RoutingSpecThread;
import decodes.routing.ScheduleEntryExecutive;
import decodes.tsdb.TsdbAppTemplate;
import decodes.util.CmdLineArgs;
import decodes.util.DecodesSettings;

public class PollGUI extends TsdbAppTemplate
{
	public static final String module = "PollGUI";
	
	private TopFrame theFrame = null;
	private JTextField stationField = new JTextField();
	private JTextField backlogField = new JTextField("2");
	private JTextArea sessionLogArea = new JTextArea();
	private Platform selectedPlatform = null;
	private JButton startPollButton = new JButton("Start Poll");
	private JButton selectStationButton = new JButton("Select");
	private boolean pollingInProgress = false;
	private EventsPanel eventsPanel = new EventsPanel();
	private QueueLogger queueLogger = new QueueLogger(module);
	private EventsPanelQueueThread epqt = null;
	private PrintStream sessionLogPrintStream = null;
	private RoutingSpecThread routingSpecThread = null;
	private File lastOutFile = null;
	private JFileChooser saveSessionFileChooser = new JFileChooser(EnvExpander.expand("$DCSTOOL_HOME"));


	
	public PollGUI()
	{
		super(module);
	}

	@Override
	protected void runApp() throws Exception
	{
		makeFrame();
		noExitAfterRunApp = true;
		int minPri = Logger.instance().getMinLogPriority();
		TeeLogger teeLogger = new TeeLogger(module, Logger.instance(), queueLogger);
		Logger.setLogger(teeLogger);
		teeLogger.setMinLogPriority(minPri);
		queueLogger.setMinLogPriority(minPri);
		epqt = new EventsPanelQueueThread(queueLogger, eventsPanel);
		epqt.start();
		theFrame.setVisible(true);
	}
	
	@Override
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		appNameArg.setDefaultValue("PollGUI");
	}


	public static void main(String[] args)
		throws Exception
	{
		new PollGUI().execute(args);
	}
	
	private void makeFrame()
	{
		theFrame = new TopFrame();
		theFrame.setExitOnClose(true);
		theFrame.setTitle("Station Polling");
		
		theFrame.setContentPane(makeTopPanel());
		theFrame.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				doExit();
			}
		});
		theFrame.pack();
		theFrame.trackChanges("PollGUI");
		sessionLogPrintStream = 
			new PrintStream(new TextAreaOutputStream(sessionLogArea))
		{
			public void close() {}
		};
	}
	
	private JPanel makeTopPanel()
	{
		JPanel topPanel = new JPanel(new BorderLayout());
		
		JPanel northPanel = new JPanel(new GridBagLayout());
		topPanel.add(northPanel, BorderLayout.NORTH);

		// Center panel is a split pane top & bottom.
		JSplitPane centerSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		topPanel.add(centerSplitPane, BorderLayout.CENTER);

		JPanel southPanel = new JPanel(new GridBagLayout());
		topPanel.add(southPanel, BorderLayout.SOUTH);

		// North panel contains station selector, Since Time, and a Start button.
		northPanel.add(new JLabel("Station:"),
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(8, 20, 4, 2), 0, 0));
		stationField.setEditable(false);
		northPanel.add(stationField,
			new GridBagConstraints(1, 0, 1, 1, 0.5, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(8, 0, 4, 4), 100, 0));
		selectStationButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					selectStation();
				}
			});
		northPanel.add(selectStationButton,
				new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
					GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(8, 2, 4, 20), 0, 0));
			
		northPanel.add(new JLabel("Backlog Hours:"),
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(4, 20, 8, 2), 0, 0));
		northPanel.add(backlogField,
			new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(4, 0, 8, 4), 50, 0));
		
		startPollButton.setEnabled(false);
		startPollButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					startPoll();
				}
			});
		northPanel.add(startPollButton,
			new GridBagConstraints(3, 0, 1, 2, 0.5, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(8, 20, 8, 20), 0, 0));

		// Top of split pane is a scrolling Text Area to contain the session log
		JPanel sessionLogPanel = new JPanel(new BorderLayout());
		sessionLogPanel.setBorder(new TitledBorder("Session Log"));
		JScrollPane sessionLogScrollPane = new JScrollPane();
		sessionLogScrollPane.getViewport().add(sessionLogArea);
		sessionLogArea.setEditable(false);
		DefaultCaret df = (DefaultCaret)sessionLogArea.getCaret();
		df.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		sessionLogPanel.add(sessionLogScrollPane, BorderLayout.CENTER);
		centerSplitPane.setTopComponent(sessionLogPanel);
		
		// Bottom of split pane is a scrolling event pane.
		centerSplitPane.setBottomComponent(eventsPanel);
		centerSplitPane.setResizeWeight(.5);

		// Init to 3/4 of the pane in the top, 1/4 in the bottom.
		sessionLogPanel.setPreferredSize(new Dimension(700, 600));
		eventsPanel.setPreferredSize(new Dimension(700, 200));
		
		// south panel contains three buttons: Clear, View Data, and Exit.
		JButton clearButton = new JButton("Clear");
		clearButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					clear();
				}
			});
		southPanel.add(clearButton,
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(5, 10, 5, 10), 0, 0));

		JButton viewDataButton = new JButton("View Data");
		viewDataButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					viewData();
				}
			});
		southPanel.add(viewDataButton,
			new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(5, 10, 5, 10), 0, 0));
		JButton saveSessionLogButton = new JButton("Save Session Log");
		saveSessionLogButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					saveSessionLog();
				}
			});
		southPanel.add(saveSessionLogButton,
			new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(5, 10, 5, 10), 0, 0));
		JButton exitButton = new JButton("Exit");
		exitButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					doExit();
				}
			});
		southPanel.add(exitButton,
			new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(5, 10, 5, 10), 0, 0));
		
		return topPanel;
	}
	
	protected void clear()
	{
		sessionLogArea.setText("");
		eventsPanel.clear();
	}

	protected void saveSessionLog()
	{
		if (saveSessionFileChooser.showSaveDialog(theFrame) == JFileChooser.APPROVE_OPTION)
		{
			File sessionLogFile = saveSessionFileChooser.getSelectedFile();
			if (sessionLogFile == null)
				return;

			try
			{
				FileWriter fw = new FileWriter(sessionLogFile);
				fw.write(sessionLogArea.getText());
				fw.close();
			}
			catch (IOException ex)
			{
				theFrame.showError("Cannot save to '" + sessionLogFile.getPath() + "': " + ex);
			}
		}
	}

	protected void viewData()
	{
		if (lastOutFile != null)
		{
			String path = EnvExpander.expand(
				DecodesSettings.instance().pollMessageDir + File.separator
				+ lastOutFile.getName());
			ShowFileDialog showFileDialog = new ShowFileDialog(theFrame, path, false);
			showFileDialog.setFile(new File(path));
			theFrame.launchDialog(showFileDialog);
		}
		else
			theFrame.showError("No output from last poll attempt.");
		Logger.instance().info("viewData pressed");
	
	}

	protected void startPoll()
	{
		Logger.instance().info(startPollButton.getText() + " pressed");
		
		if (!pollingInProgress)
		{
			if (selectedPlatform == null)
			{
				theFrame.showError("Please select a station before pressing Start Poll.");
				return;
			}
			int backlog = 2;
			try { backlog = Integer.parseInt(backlogField.getText().trim()); }
			catch(Exception ex)
			{
				theFrame.showError("Backlog must be an integer number of hours.");
				return;
			}
			PollingThread.backlogOverrideHours = backlog;
			
			startPollButton.setText("Stop Poll");
			selectStationButton.setEnabled(false);

			// Start the poll.
			Logger.instance().info("Poll starting...");
			pollingInProgress = true;
			doPoll(backlog);
		}
		else
		{
			sessionLogArea.append("\nStopping poll in progress...\n");
			if (routingSpecThread != null)
				routingSpecThread.shutdown();
			try { Thread.sleep(3000L); } catch(InterruptedException ex) {}
			startPollButton.setText("Start Poll");
			selectStationButton.setEnabled(true);
			pollingInProgress = false;
		}

	}

	protected void selectStation()
	{
		PlatformSelectDialog dlg = new PlatformSelectDialog(theFrame, "poll");
		dlg.setMultipleSelection(false);
		theFrame.launchDialog(dlg);
		selectedPlatform = dlg.getSelectedPlatform();
		if (selectedPlatform != null)
		{
			stationField.setText(selectedPlatform.makeFileName());
			Logger.instance().info("Selected station '" + selectedPlatform.makeFileName() + "'");
			startPollButton.setEnabled(true);;
		}
		else
		{
			stationField.setText("");
			startPollButton.setEnabled(false);
		}
	}

	private void doExit()
	{
		Logger.instance().info("doExit pressed");
		epqt.shutdown();
		theFrame.dispose();
	}

	public synchronized void addEvent(String event)
	{
		eventsPanel.addLine(event);
	}
	
	private void doPoll(int backlog)
	{
		sessionLogArea.setCaretPosition(sessionLogArea.getDocument().getLength());
		DefaultCaret df = (DefaultCaret)sessionLogArea.getCaret();
		df.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

		boolean isModem = true;
		for(TransportMedium tm : selectedPlatform.transportMedia)
			if (tm.getMediumType().equalsIgnoreCase(Constants.medium_PolledModem))
			{
				isModem = true;
				break;
			}
			else if (tm.getMediumType().equalsIgnoreCase(Constants.medium_PolledTcp))
			{
				isModem = false;
				break;
			}
		
		String rsName = isModem ? DecodesSettings.instance().pollRoutingTemplate :
			DecodesSettings.instance().pollTcpTemplate;
		
		RoutingSpec rs = Database.getDb().routingSpecList.find(rsName);
		if (rs == null)
		{
			theFrame.showError("No routing spec named '" + rsName 
				+ "' in database. This is needed as a template for "
				+ (isModem ? "Modem" : "TCP/Cellular")
				+ " platforms. Check the DECODES Setting for "
				+ (isModem ? "pollRoutingTemplate" : "pollTcpTemplate"));
			return;
		}
		
		rs.setProperty("debugLevel", 
			Logger.instance().getMinLogPriority() == Logger.E_DEBUG1 ? "1" :
			Logger.instance().getMinLogPriority() == Logger.E_DEBUG2 ? "2" :
			Logger.instance().getMinLogPriority() == Logger.E_DEBUG3 ? "3" : "0");
		
		// Retrieve up station status and set rs.sinceTime to last poll time.
		PlatformStatusDAI platformStatusDAO = theDb.makePlatformStatusDAO();
		try
		{
			PlatformStatus platStat = platformStatusDAO.readPlatformStatus(selectedPlatform.getId());
			Date lastMsgTime = null;
			if (platStat != null)
				lastMsgTime = platStat.getLastMessageTime();
			if (lastMsgTime == null) // default to 4 hours.
				lastMsgTime = new Date(System.currentTimeMillis() - 3600000L * 4);
			
			long sinceTimeMS = System.currentTimeMillis() - backlog*3600000L;
			rs.sinceTime = IDateFormat.time_t2string((int)(sinceTimeMS/1000L));
			
			// Remove the netlists in the prototype and replace with the single station name.
			rs.networkListNames.clear();
			rs.networkLists.clear();
			String dcpname = selectedPlatform.makeFileName();
			rs.setProperty("sc:DCP_NAME_0000", dcpname);
			
			rs.setProperty("pollNumTries", "1"); // Only try poll once.
			
			ScheduleEntryExecutive.setRereadRsBeforeExec(false);
			routingSpecThread = RoutingSpecThread.makeInstance(rs);

			// Set a static arg in PollingThread to tell it to use stdout as session logger.
			PollingThread.staticSessionLogger = sessionLogPrintStream;
Logger.instance().debug3("set PollingThread.staticSessionLogger" + 
(sessionLogPrintStream==null?" TO NULL!!" : ""));

			// Start the routing spec thread to do the work.
			noExitAfterRunApp = true;
			routingSpecThread.setShutdownHook(
				new Runnable()
				{
					public void run()
					{
						if (routingSpecThread.getConsumer() != null 
							&& routingSpecThread.getConsumer() instanceof DirectoryConsumer)
						{
							DirectoryConsumer dc = (DirectoryConsumer)routingSpecThread.getConsumer();
							lastOutFile = dc.getLastOutFile();
							if (lastOutFile != null)
							{
								sessionLogPrintStream.println("Output written to " 
									+ lastOutFile.getPath());
								System.out.println("Output written to " 
									+ lastOutFile.getPath());
							}
							else
								sessionLogPrintStream.println("(no active output)");
						}
						else 
							sessionLogPrintStream.println(
								routingSpecThread.getConsumer() == null ? "No output file produced." 
									: routingSpecThread.getConsumer().getClass().getName());
						pollFinished();
					}
				});
			routingSpecThread.start();
		}
		catch (Exception ex)
		{
			theFrame.showError("Error starting poll: " + ex);
			pollFinished();
		}

	}

	protected void pollFinished()
	{
		routingSpecThread = null;
		sessionLogArea.append("\nPoll complete.\n");
		Logger.instance().info("Poll complete.");
		startPollButton.setText("Start Poll");
		startPollButton.setEnabled(true);
		selectStationButton.setEnabled(true);
		pollingInProgress = false;
	}
}

