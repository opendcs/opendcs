package lrgs.multistat;

import ilex.gui.EventsPanel;
import ilex.util.AuthException;
import ilex.util.EnvExpander;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import lrgs.rtstat.RtStatPanel;
import lrgs.rtstat.UserListDialog;
import lritdcs.SearchCritDialog;
import decodes.gui.TopFrame;

@SuppressWarnings("serial")
public class MultiStatFrame extends TopFrame
{
	JPanel contentPane;
	BorderLayout borderLayout1 = new BorderLayout();
	JPanel alarmPanel = new JPanel();
	EventsPanel eventsPanel = new EventsPanel();
	JSpinner alarmSpinner;
	JButton alarmInfoButton = new JButton();
	JButton cancelButton = new JButton();
	JTabbedPane tabbedPane = new JTabbedPane();
	JPanel summaryPanel = new JPanel();
	RtStatPanel lrgs1Detail = new RtStatPanel();
	RtStatPanel lrgs2Detail = new RtStatPanel();
	RtStatPanel lrgs3Detail = new RtStatPanel();
	RtStatPanel lrgs4Detail = new RtStatPanel();
	LrgsSummaryStatPanel lrgs1Summary = new LrgsSummaryStatPanel();
	LrgsSummaryStatPanel lrgs2Summary = new LrgsSummaryStatPanel();
	LrgsSummaryStatPanel lrgs3Summary = new LrgsSummaryStatPanel();
	LrgsSummaryStatPanel lrgs4Summary = new LrgsSummaryStatPanel();
	JMenuBar menuBar = new JMenuBar();
	JMenu fileMenu = new JMenu();
	JMenuItem fileMenu_exit = new JMenuItem();
	JMenuItem fileMenu_UserAdmin = new JMenuItem();
	JTextField ackUserField = new JTextField();
	JButton historyButton = new JButton();
	GridBagLayout gridBagLayout1 = new GridBagLayout();
	JTextField numAlarmsField = new JTextField();

	// ========================
	AlarmList alarmList;
	InformationDlg alarmInfoDialog = new InformationDlg();
	JCheckBox muteCheck = new JCheckBox();
	SoundThread soundThread = new SoundThread();
	AlarmMaskList alarmMaskList = new AlarmMaskList();

	static MultiStatConfig cfg = MultiStatConfig.instance(); // @jve:decl-index=0:

	MSLrgsConThread lrgs1Thread;
	MSLrgsConThread lrgs2Thread;
	MSLrgsConThread lrgs3Thread;
	MSLrgsConThread lrgs4Thread;
	SearchCritDialog scDlg = null;
	HistDlg alarmHistDlg;
	long mutedAt = 0L;

	/** Dialog for editing users. */
	private UserListDialog userListDialog = null;

	// / Last time message was received from any DDS connection.
	public long lastRetrieval = 0L;

	// Construct the frame
	public MultiStatFrame()
	{
		enableEvents(AWTEvent.WINDOW_EVENT_MASK);
		try
		{
			alarmList = new AlarmList();
			alarmHistDlg = null;
			alarmSpinner = new JSpinner(alarmList);
			JSpinner.DefaultEditor df = (JSpinner.DefaultEditor) alarmSpinner.getEditor();
			alarmSpinner.addChangeListener(new ChangeListener()
			{
				public void stateChanged(ChangeEvent e)
				{
					alarmSpinnerChanged();
				}
			});
			df.getTextField().setBackground(Color.black);
			df.getTextField().setEditable(false);
			df.getTextField().setFont(new java.awt.Font("Dialog", 0, 18));
			jbInit();
			alarmInfoButton.setEnabled(false);
			cancelButton.setEnabled(false);
			ackUserField.setText(MultiStatConfig.instance().operator);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		alarmSpinner.setModel(alarmList);
		initSummaries();
		muteCheck.setSelected(cfg.mute);
		if (cfg.mute)
			mutedAt = System.currentTimeMillis();
		trackChanges("multistat");
	}

	public void startup()
	{
		if (!soundThread.isAlive())
			soundThread.start();
		if (muteCheck.isSelected())
			soundThread.shutup();

		lrgs1Thread = new MSLrgsConThread(1, lrgs1Summary, tabbedPane, lrgs1Detail, this);
		lrgs1Thread.start();
		lrgs2Thread = new MSLrgsConThread(2, lrgs2Summary, tabbedPane, lrgs2Detail, this);
		lrgs2Thread.start();
		lrgs3Thread = new MSLrgsConThread(3, lrgs3Summary, tabbedPane, lrgs3Detail, this);
		lrgs3Thread.start();
		lrgs4Thread = new MSLrgsConThread(4, lrgs4Summary, tabbedPane, lrgs4Detail, this);
		lrgs4Thread.start();
	}
	
	public void restart()
	{
		SwingUtilities.invokeLater(
			new Runnable()
			{
				public void run()
				{
//System.out.println("MultiStatFrame.restart()");
					// Remove summary panels and then re-add the ones configured.
					summaryPanel.remove(lrgs1Summary);
					summaryPanel.remove(lrgs2Summary);
					summaryPanel.remove(lrgs3Summary);
					summaryPanel.remove(lrgs4Summary);
					
					tabbedPane.remove(lrgs1Detail);
					tabbedPane.remove(lrgs2Detail);
					tabbedPane.remove(lrgs3Detail);
					tabbedPane.remove(lrgs4Detail);
			
					if (cfg.Lrgs1HostName != null)
					{
						summaryPanel.add(lrgs1Summary, null);
						tabbedPane.add(lrgs1Detail, cfg.Lrgs1DisplayName);
					}
					if (cfg.Lrgs2HostName != null)
					{
						summaryPanel.add(lrgs2Summary, null);
						tabbedPane.add(lrgs2Detail, cfg.Lrgs2DisplayName);
					}
					if (cfg.Lrgs3HostName != null)
					{
						summaryPanel.add(lrgs3Summary, null);
						tabbedPane.add(lrgs3Detail, cfg.Lrgs3DisplayName);
					}
					if (cfg.Lrgs4HostName != null)
					{
						summaryPanel.add(lrgs4Summary, null);
						tabbedPane.add(lrgs4Detail, cfg.Lrgs4DisplayName);
					}
				}
			});
	}

	// Component initialization
	private void jbInit() throws Exception
	{
		contentPane = (JPanel) this.getContentPane();
		contentPane.setLayout(borderLayout1);
		this.setJMenuBar(menuBar);
		this.setSize(new Dimension(1024, 800));
		this.setTitle("WCDAS DCS Dissemination Status");
		eventsPanel.setBorder(BorderFactory.createLoweredBevelBorder());
		eventsPanel.setPreferredSize(new Dimension(10, 140));
		alarmPanel.setLayout(gridBagLayout1);
		alarmInfoButton.setPreferredSize(new Dimension(160, 27));
		alarmInfoButton.setToolTipText("More info on currently displayed alarm.");
		alarmInfoButton.setText("More Info");
		alarmInfoButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				alarmInfoButton_actionPerformed(e);
			}
		});
		cancelButton.setPreferredSize(new Dimension(160, 27));
		cancelButton.setToolTipText("Cancels the currently displayed alarm.");
		cancelButton.setText("Acknowledge:");
		cancelButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				cancelButton_actionPerformed(e);
			}
		});
		alarmSpinner.setMinimumSize(new Dimension(25, 24));
		alarmSpinner.setPreferredSize(new Dimension(25, 24));
		GridLayout gridLayout = new GridLayout();
		summaryPanel.setLayout(gridLayout);
		gridLayout.setColumns(4);
		gridLayout.setHgap(2);

		lrgs1Summary.setBorder(BorderFactory.createEtchedBorder());
		lrgs2Summary.setBorder(BorderFactory.createEtchedBorder());
		lrgs3Summary.setBorder(BorderFactory.createEtchedBorder());
		lrgs4Summary.setBorder(BorderFactory.createEtchedBorder());
		fileMenu.setText("File");
		fileMenu_exit.setText("Exit");
		fileMenu_exit.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				fileMenu_exit_actionPerformed(e);
			}
		});
		fileMenu_UserAdmin.setText("User Administration");
		fileMenu_UserAdmin.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				fileMenu_UserAdmin_actionPerformed(e);
			}
		});

		alarmPanel.setMinimumSize(new Dimension(243, 42));
		alarmPanel.setPreferredSize(new Dimension(275, 90));
		muteCheck.setText("Mute");
		muteCheck.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				muteCheck_actionPerformed(e);
			}
		});
		ackUserField.setPreferredSize(new Dimension(120, 23));
		ackUserField.setToolTipText("Operator Initials");
		ackUserField.setText("");
		historyButton.setPreferredSize(new Dimension(160, 27));
		historyButton.setText("History");
		historyButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				historyButton_actionPerformed(e);
			}
		});
		numAlarmsField.setFont(new java.awt.Font("Dialog", Font.BOLD, 13));
		numAlarmsField.setPreferredSize(new Dimension(50, 30));
		numAlarmsField.setText("0");
		numAlarmsField.setEditable(false);
		
		contentPane.add(alarmPanel, BorderLayout.NORTH);
		
//		JSplitPane centerPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tabbedPane, eventsPanel);
//		contentPane.add(centerPane, BorderLayout.CENTER);
		contentPane.add(eventsPanel, BorderLayout.SOUTH);
		contentPane.add(tabbedPane, BorderLayout.CENTER);

		tabbedPane.add(summaryPanel, "Summary Status");

		menuBar.add(fileMenu);
		fileMenu.add(fileMenu_UserAdmin);
		fileMenu.addSeparator();
		fileMenu.add(fileMenu_exit);

		alarmPanel.add(new JLabel("Alarm:"), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
			GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(9, 5, 0, 0), 0, 0));
		alarmPanel.add(alarmInfoButton, new GridBagConstraints(0, 1, 2, 1, 0.2, 0.0, GridBagConstraints.WEST,
			GridBagConstraints.NONE, new Insets(5, 10, 10, 0), 0, 0));
		alarmPanel.add(cancelButton, new GridBagConstraints(2, 1, 1, 1, 0.2, 0.0, GridBagConstraints.EAST,
			GridBagConstraints.NONE, new Insets(5, 20, 10, 2), 0, 0));
		alarmPanel.add(ackUserField, new GridBagConstraints(3, 1, 1, 1, 0.2, 0.0, GridBagConstraints.WEST,
			GridBagConstraints.NONE, new Insets(5, 2, 10, 20), 0, 0));
		alarmPanel.add(historyButton, new GridBagConstraints(4, 1, 1, 1, 0.2, 0.0, GridBagConstraints.CENTER,
			GridBagConstraints.NONE, new Insets(5, 5, 10, 5), 0, 0));
		alarmPanel.add(muteCheck, new GridBagConstraints(5, 1, 2, 1, 0.2, 0.0, GridBagConstraints.EAST,
			GridBagConstraints.NONE, new Insets(5, 5, 10, 10), 0, 0));
		alarmPanel.add(alarmSpinner, new GridBagConstraints(1, 0, 5, 1, 1.0, 1.0, GridBagConstraints.CENTER,
			GridBagConstraints.BOTH, new Insets(7, 0, 0, 0), 0, 0));
		alarmPanel.add(numAlarmsField, new GridBagConstraints(6, 0, 1, 1, 0.0, 0.0,
			GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

	}

	// Overridden so we can exit when window is closed
	protected void processWindowEvent(WindowEvent e)
	{
		super.processWindowEvent(e);
		if (e.getID() == WindowEvent.WINDOW_CLOSING)
		{
			System.exit(0);
		}
	}

	void alarmInfoButton_actionPerformed(ActionEvent e)
	{
		Alarm alarm = (Alarm) alarmSpinner.getValue();

		if (!alarm.isEmpty())
		{
			// System.out.println("Info requested for alarm '" + alarm + "'");
			this.displayInfoPage(alarm);
			alarmInfoDialog.setVisible(true);
		}
	}

	void cancelButton_actionPerformed(ActionEvent e)
	{
		String op = MultiStatConfig.instance().operator = ackUserField.getText().trim();
		if (op.length() == 0)
		{
			showError("You must enter an operator name or initials before" + " you can acknowledge an alarm.");
			return;
		}
		alarmList.cancelCurrentAlarm();
		numAlarmsField.setText("" + alarmList.getNumAlarms());
		if (alarmList.getNumAlarms() == 0)
		{
			alarmInfoButton.setEnabled(false);
			cancelButton.setEnabled(false);
		}
	}

	void initSummaries()
	{
		lrgs1Summary.systemNameField.setText("LRGS: " + cfg.Lrgs1DisplayName);
		lrgs1Summary.dataSourceField.setText("");
		lrgs1Summary.systemStatusField.setText("Not Connected");
		lrgs1Summary.systemStatusField.setWarning();
		lrgs1Summary.systemTimeField.setText("?");
		lrgs1Summary.systemTimeField.setWarning();
		lrgs1Summary.numClientsField.setText("?");
		lrgs1Summary.numClientsField.setWarning();
		lrgs1Summary.msgsThisHourField.setText("?");
		lrgs1Summary.msgsThisHourField.setWarning();
		lrgs1Summary.alarmsField.setText("?");
		lrgs1Summary.alarmsField.setWarning();

		lrgs2Summary.systemNameField.setText("LRGS: " + cfg.Lrgs2DisplayName);
		lrgs2Summary.dataSourceField.setText("");
		lrgs2Summary.systemStatusField.setText("Not Connected");
		lrgs2Summary.systemStatusField.setWarning();
		lrgs2Summary.systemTimeField.setText("?");
		lrgs2Summary.systemTimeField.setWarning();
		lrgs2Summary.numClientsField.setText("?");
		lrgs2Summary.numClientsField.setWarning();
		lrgs2Summary.msgsThisHourField.setText("?");
		lrgs2Summary.msgsThisHourField.setWarning();
		lrgs2Summary.alarmsField.setText("?");
		lrgs2Summary.alarmsField.setWarning();

		lrgs3Summary.systemNameField.setText("LRGS: " + cfg.Lrgs3DisplayName);
		lrgs3Summary.dataSourceField.setText("");
		lrgs3Summary.systemStatusField.setText("Not Connected");
		lrgs3Summary.systemStatusField.setWarning();
		lrgs3Summary.systemTimeField.setText("?");
		lrgs3Summary.systemTimeField.setWarning();
		lrgs3Summary.numClientsField.setText("?");
		lrgs3Summary.numClientsField.setWarning();
		lrgs3Summary.msgsThisHourField.setText("?");
		lrgs3Summary.msgsThisHourField.setWarning();
		lrgs3Summary.alarmsField.setText("?");
		lrgs3Summary.alarmsField.setWarning();
	}

	void fileMenu_exit_actionPerformed(ActionEvent e)
	{
		System.exit(0);
	}

	private void alarmSpinnerChanged()
	{
		Alarm alarm = (Alarm) alarmSpinner.getValue();
		if (!alarm.isEmpty())
		{
			char p = alarm.priority.charAt(0);
			Color c = (p == 'W') ? Color.yellow : (p == 'F') ? Color.red : Color.white;
			JSpinner.DefaultEditor df = (JSpinner.DefaultEditor) alarmSpinner.getEditor();
			final JFormattedTextField ftf = df.getTextField();
			ftf.setForeground(c);
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					ftf.moveCaretPosition(0);
					ftf.setSelectionStart(0);
					ftf.setSelectionEnd(0);
				}
			});
			alarmInfoButton.setEnabled(true);
			cancelButton.setEnabled(true);
			displayInfoPage(alarm);
		}
		else
		{
			alarmInfoButton.setEnabled(false);
			cancelButton.setEnabled(false);
			soundThread.shutup();
		}
	}

	private void displayInfoPage(Alarm alarm)
	{
		String url = MultiStatConfig.instance().alarmInfoBaseUrl + "alarm-" + alarm.module + "-"
			+ alarm.alarmNum + ".html";
		alarmInfoDialog.setPage(url);
	}

	public void addAlarm(Alarm alarm)
	{
		alarmList.addAlarm(alarm);
		alarmInfoButton.setEnabled(true);
		cancelButton.setEnabled(true);
		if (!muteCheck.isSelected())
		{
			File soundFile = new File(EnvExpander.expand(MultiStatConfig.instance().soundFile));
			soundThread.playMulti(soundFile, 4000L, 100);
		}
		numAlarmsField.setText("" + alarmList.getNumAlarms());
	}

	void muteCheck_actionPerformed(ActionEvent e)
	{
		if (muteCheck.isSelected())
		{
			soundThread.shutup();
			mutedAt = System.currentTimeMillis();
		}
		else if (alarmList.getNumAlarms() > 0)
		{
			File soundFile = new File(EnvExpander.expand(MultiStatConfig.instance().soundFile));
			soundThread.playMulti(soundFile, 4000L, 100);
		}
	}

	public void addEvent(String event, String from)
	{
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

		if (pri.charAt(0) == 'D')
			return;

		int col = mod_num.indexOf(':');
		int len = mod_num.length();
		if (col <= 0 || col >= len - 1)
			return;
		boolean instant = (mod_num.charAt(len - 1) == '-');
		if (instant)
			mod_num = mod_num.substring(0, len - 1);

		String module = mod_num.substring(0, col);
		int num = 0;

		try
		{
			num = Integer.parseInt(mod_num.substring(col + 1));
		}
		catch (NumberFormatException ex)
		{
			return;
		}

		if (num < 0 && pri.charAt(0) == 'I')
		{
			alarmList.cancelAlarm(module, -num);
			if (alarmList.getNumAlarms() == 0)
			{
				alarmInfoButton.setEnabled(false);
				cancelButton.setEnabled(false);
			}
		}
		else
		{
			if (!alarmMaskList.isMasked(from, module, num))
				addAlarm(new Alarm(pri, from, time, module, num, text, instant));
		}
		numAlarmsField.setText("" + alarmList.getNumAlarms());
	}

	public void historyButton_actionPerformed(ActionEvent e)
	{
		if (alarmHistDlg == null)
		{
			alarmHistDlg = new HistDlg(this, alarmList.cancelledAlarmList);
			Point loc = this.getLocation();
			Dimension frmSize = this.getSize();
			Dimension dlgSize = new Dimension(frmSize.width - 200, 500);
			int x = (frmSize.width - dlgSize.width) / 2 + loc.x;
			int y = (frmSize.height - dlgSize.height) / 2 + loc.y;
			alarmHistDlg.setLocation(x, y);
		}
		alarmHistDlg.setVisible(true);
	}

	public void fileMenu_UserAdmin_actionPerformed(ActionEvent e)
	{
		if (cfg.adminLrgs == null || cfg.adminLrgs.length() == 0)
		{
			showError("Your configuration doesn't specify an administrative LRGS."
				+ " You must do so to administer DDS users.");
			return;
		}
		MSLrgsConThread dcif = null;
		String dn1 = lrgs1Thread.getLrgsDisplayName();
		String dn2 = lrgs2Thread.getLrgsDisplayName();
		String dn3 = lrgs3Thread.getLrgsDisplayName();
		String dn4 = lrgs4Thread.getLrgsDisplayName();
		if (dn1 != null && cfg.adminLrgs.equalsIgnoreCase(dn1))
			dcif = lrgs1Thread;
		else if (dn2 != null && cfg.adminLrgs.equalsIgnoreCase(dn2))
			dcif = lrgs2Thread;
		else if (dn3 != null && cfg.adminLrgs.equalsIgnoreCase(dn3))
			dcif = lrgs3Thread;
		else if (dn4 != null && cfg.adminLrgs.equalsIgnoreCase(dn4))
			dcif = lrgs4Thread;
		else
		{
			showError("The 'adminLrgs' display name (" + cfg.adminLrgs + ") in your configuration doesn't "
				+ "match any of the LRGS's.");
			return;
		}
		if (userListDialog == null)
		{
			userListDialog = new UserListDialog(this, "DDS Users on host " + dcif.getLrgsDisplayName(), true);
			userListDialog.setDdsClientIf(dcif);
		}
		userListDialog.setHost(dcif.getLrgsDisplayName());

		// Retrieve user list from server.
		try
		{
			userListDialog.setUsers(dcif.getUsers());
			launch(userListDialog);
		}
		catch (AuthException ex)
		{
			showError(ex.toString());
		}
	}

	private void launch(JDialog dlg)
	{
		Dimension frameSize = this.getSize();
		Point frameLoc = this.getLocation();
		Dimension dlgSize = dlg.getPreferredSize();
		int xo = (frameSize.width - dlgSize.width) / 2;
		if (xo < 0)
			xo = 0;
		int yo = (frameSize.height - dlgSize.height) / 2;
		if (yo < 0)
			yo = 0;

		dlg.setLocation(frameLoc.x + xo, frameLoc.y + yo);
		dlg.setVisible(true);
	}
}
