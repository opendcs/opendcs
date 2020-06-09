/*
*  $Id$
*/
package lrgs.gui;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;

import decodes.util.CmdLineArgs;

import java.util.ResourceBundle;

import ilex.gui.*;
import ilex.cmdline.*;
import ilex.util.IDateFormat;
import ilex.util.LoadResourceBundle;
import ilex.util.Logger;

import lrgs.common.*;
import lrgs.ldds.*;

/**
The MessageOutput frame is usually started from the MessageBrowser.
The current connection parameters and search criteria are evaluated,
a new connection opened, and the resulting data is saved to a file.
*/
public class MessageOutput extends MenuFrame
	implements DcpMsgOutputMonitor
{
	private static ResourceBundle labels = null;
	private static ResourceBundle genericLabels = null;
	
	public static final int DEFAULT_PORT_NUM = LddsParams.DefaultPort;
	public static final int StartHeight = 260;
	public static final int StartWidth = 520;//440;
	private static final String TITLE = "DCP Message Output";

	private String hostName, userName;
	private int portNum;
	private SearchCriteria searchcrit;
	private boolean sendNetworkLists;
	private String prefix, suffix;
	private int count;

	private JTextField outputFileField;
	private DynamicLabel dcpAddressLabel, msgTimeLabel, countLabel;
	private JButton selectFileButton, runButton, pauseButton, quitButton;
	private JCheckBox closeWhenDoneCheck;
	private JRadioButton OverwriteRadio, AppendRadio, FailRadio;
	private boolean IamPaused;
	private LddsClient client;
	private FileOutputStream outs;
	private DcpMsgOutputThread outputThread;
	private StatusUpdater statusUpdater;
	private boolean doDecode;
	private boolean showRaw;
	private String beforeData, afterData;
	private String passwd;
	private static JFileChooser filechooser;
//	static
//	{
//		filechooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
//	}

	/** 
	  Constructor.
	  @param hostName the host
	  @param portNum the port
	  @param userName the user
	  @param searchcrit the pre-loaded SearchCriteria object
	  @param prefix printed before each raw message
	  @param suffix printed after each raw message
	  @param sendNetworkLists true if we should send network lists
	         reference din the searchcrit.
	  @param doDecode true if we should try to decode the data.
	  @param beforeData printed before decoded data
	  @param afterData printed after decoded data
	*/
	public MessageOutput(String hostName, int portNum, String userName,
		SearchCriteria searchcrit, String prefix, String suffix, 
		boolean sendNetworkLists, boolean doDecode,
		String beforeData, String afterData, boolean showRaw)
	{
		super(TITLE);
		labels = MessageBrowser.getLabels();
		genericLabels = MessageBrowser.getGenericLabels();
		setTitle(labels.getString("MessageOutput.frameTitle"));
		
		filechooser = new JFileChooser();
		filechooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
		 
		this.hostName = hostName != null ? hostName : "localhost";
		this.portNum = portNum == 0 ? DEFAULT_PORT_NUM : portNum;
		this.userName = userName != null ? userName :
			System.getProperty("user.name");
		this.searchcrit = searchcrit;
		this.prefix = prefix;
		this.suffix = suffix;
		this.sendNetworkLists = sendNetworkLists;
		this.doDecode = doDecode;
		this.beforeData = beforeData;
		this.afterData = afterData;
		this.showRaw = showRaw;
		this.passwd = null;

		initProperties();

		Container contpane = getContentPane();
		contpane.setLayout(new BorderLayout());

		// North contains output file selection area.
		JPanel north = new JPanel(new FlowLayout(FlowLayout.CENTER));
		contpane.add(north, BorderLayout.NORTH);
		north.add(new JLabel(labels.getString("MessageOutput.outputFile")));
		north.add(outputFileField = new JTextField(20));
		outputFileField.setText(GuiApp.getProperty("MessageOutput.OutputFile"));

		north.add(selectFileButton =
			new SingleClickButton(genericLabels.getString("select"))
			{
				public void buttonPressed(AWTEvent event)
				{
					selectFileButtonPress();
				}
			});

		// South contains close checkbox, run, pause, and quit buttons.
		JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER));
		contpane.add(south, BorderLayout.SOUTH);
		closeWhenDoneCheck = new JCheckBox(
				labels.getString("MessageOutput.closeWhenDone"),
			GuiApp.getBooleanProperty("MessageOutput.CloseWhenDone", false));
		south.add(closeWhenDoneCheck);
		south.add(runButton = new JButton(
				labels.getString("MessageOutput.run")));
		runButton.addActionListener(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent av)
				{
					runButtonPress();
				}
			});
		south.add(pauseButton = new JButton(
			labels.getString("MessageOutput.pause")));
		pauseButton.addActionListener(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent av)
				{
					pauseButtonPress();
				}
			});
		south.add(quitButton = new JButton(
			labels.getString("MessageOutput.quit")));
		quitButton.addActionListener(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent av)
				{
					quitButtonPress();
				}
			});

		// West contains panel with radio buttons:
		JPanel west = new JPanel(new GridLayout(3, 1, 4, 4));
		contpane.add(west, BorderLayout.WEST);
		west.setBorder(new TitledBorder(
			labels.getString("MessageOutput.ifFileExists")));
		ButtonGroup radios = new ButtonGroup();
		String ofe = GuiApp.getProperty("MessageOutput.FileExists").toLowerCase();
		FailRadio = new JRadioButton(labels.getString("MessageOutput.fail"),
			ofe.length() > 0 && ofe.charAt(0) == 'f');
		radios.add(FailRadio);
		west.add(FailRadio);
		AppendRadio = new JRadioButton(
				labels.getString("MessageOutput.append"),
			ofe.length() > 0 && ofe.charAt(0) == 'a');
		radios.add(AppendRadio);
		west.add(AppendRadio);
		OverwriteRadio = new JRadioButton(
				labels.getString("MessageOutput.overwrite"),
			ofe.length() > 0 && ofe.charAt(0) == 'o');
		radios.add(OverwriteRadio);
		west.add(OverwriteRadio);

		// CENTER contains panel with current status
		JPanel center = new JPanel(new GridLayout(3, 1, 4, 4));
		contpane.add(center, BorderLayout.CENTER);
		center.setBorder(new TitledBorder(
			labels.getString("MessageOutput.currentlyWriting")));
		JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		p.add(new JLabel(
			labels.getString("MessageOutput.DCPAddress")));
		p.add(dcpAddressLabel = new DynamicLabel("", 17));
		center.add(p);
		p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		p.add(new JLabel(
			labels.getString("MessageOutput.DAPSTime")));
		p.add(msgTimeLabel = new DynamicLabel("", 17));
		center.add(p);
		p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		p.add(new JLabel(
			labels.getString("MessageOutput.numberSaved")));
		p.add(countLabel = new DynamicLabel("", 17));
		center.add(p);

		statusUpdater = new StatusUpdater(this);
		statusUpdater.start();
	}

	/**
	  Sets the password, telling this module to do an authenticated connect.
	*/
	public void setPassword(String pw)
	{
		passwd = pw;
	}

	/**
	 * Queries the GuiApp singleton for all the properties used by this
	 * display. This makes sure they're in the property set and initializes
	 * default values.
	 */
	public static void initProperties()
	{
		GuiApp.getProperty("MessageOutput.OutputFile", "messages.txt");

		String nm = "MessageOutput.FileExists";
		GuiApp.getProperty(nm, "Fail");
		EditPropsAction.registerEditor(nm, 
			new JComboBox(new String[] { "Fail", "Append", "Overwrite" }));

		GuiApp.getProperty("MessageOutput.Timeout", "60");
		nm = "MessageOutput.CloseWhenDone";
		GuiApp.getProperty(nm, "true");
		EditPropsAction.registerEditor(nm, 
			new JComboBox(new String[] { "true", "false" }));
	}

	/** Sets the screen size. */
	public void setSize(Dimension d)
	{
		GuiApp.setProperty("MessageOutput.height", ""+d.height);
		GuiApp.setProperty("MessageOutput.width", ""+d.width);
	}

	/** Called when screen moved. Saves location in properties. */
	public void movedTo(Point p)
	{
		GuiApp.setProperty("MessageOutput.x", ""+p.x);
		GuiApp.setProperty("MessageOutput.y", ""+p.y);
	}

	/** Starts the GUI at the specified location. */
	public void startup(int x, int y)
	{
		int width = GuiApp.getIntProperty("MessageOutput.width", StartWidth);
		int height = GuiApp.getIntProperty("MessageOutput.height", StartHeight);
		x = GuiApp.getIntProperty("MessageOutput.x", x);
		y = GuiApp.getIntProperty("MessageOutput.y", y);
		launch(x, y, width, height);
	}

	/** Called before application exit. */
	public void cleanupBeforeExit()
	{
		if (client != null)
		{
			try
			{
				client.sendGoodbye();
				client.disconnect();
			}
			catch(Exception e) {}
		}
	}

	/**
	  Called when user presses the select-file button.
	  Displays a FileChooser dialog.
	*/
	public void selectFileButtonPress()
	{
		if (filechooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
			outputFileField.setText(filechooser.getSelectedFile().getPath());
	}

	/**
	  Called when user presses the run button.
	*/
	public void runButtonPress()
	{
		// If starting fresh, open output file and client interface.
		if (outs == null || client == null)
		{
			count = 0;

			if (openOutputFile() == false)
				return;

			if (attachLrgsClient() == false)
			{
				try { outs.close(); }
				catch(IOException ioe) {}
				outs = null;
				return;
			}
			outputThread = new DcpMsgOutputThread(this, client, outs,
				GuiApp.getIntProperty("MessageOutput.Timeout", 5),
				prefix, suffix);
			outputThread.doDecode = doDecode;
			outputThread.beforeData = beforeData;
			outputThread.afterData = afterData;
			outputThread.showRaw = showRaw;
			outputThread.disableDecodingErrorMessages();
			outputThread.start();
		}
		// Otherwise we must just be pausing
		IamPaused = false;
	}

	/**
	  Opens the output file.
	  If it previously existed, check the Append, Overwrite, and Fail radio
	  buttons and act appropriatly.
	  @return true if file opened successfully for output
	*/
	public boolean openOutputFile()
	{
		String s = outputFileField.getText();
		if (s == null || s.length() == 0)
		{
			showError(
				labels.getString("MessageOutput.noOutputFileSpec"));
			return false;
		}
		try
		{
			File f = new File(s);
			if (f.exists())
			{
				if (AppendRadio.isSelected())
				{
					outs = new FileOutputStream(s, true);
				}
				else if (OverwriteRadio.isSelected())
				{
					outs = new FileOutputStream(s, false);
				}
				else // Either FailRadio check or nothing checked
				{
					showError(LoadResourceBundle.sprintf(
					labels.getString("MessageOutput.cannotOverWrite"),
					s));
					return false;
				}
			}
			else // create new file
			{
				outs = new FileOutputStream(f);
			}
		}
		catch(IOException ioe)
		{
			showError(ioe.toString());
		}
		return true;
	}

	/**
	* Attaches to the server, sends HELLO, and sends searchcrit.
	*/
	public boolean attachLrgsClient()
	{
		String what="";
		try
		{
			what = labels.getString("MessageOutput.constructingClientI");
			client = new LddsClient(hostName, portNum);

			// MJM 20030223 added...
			client.enableMultiMessageMode(true);

			what = 
			labels.getString("MessageOutput.connectingToServer");
			client.connect();
			if (passwd != null && passwd.length() > 0)
			{
				what = 
					labels.getString("MessageOutput.authenticatingToServer");
				client.sendAuthHello(userName, passwd);
			}
			else
			{
				what = 
				labels.getString("MessageOutput.loggingIntoServer");
				client.sendHello(userName);
			}
			what = 
				labels.getString("MessageOutput.sendingNetworkLists");
			if (searchcrit != null)
			{
				if (sendNetworkLists 
				 && searchcrit.NetlistFiles != null 
				 && searchcrit.NetlistFiles.size() > 0)
				{
					for(String s : searchcrit.NetlistFiles)
					{
						File f = NetlistFinder.find(s);
						if (f != null)
							client.sendNetList(f, s);
					}
				}
				what = 
					labels.getString("MessageOutput.sendingSearchCriteria");
				client.sendSearchCrit(searchcrit);
			}
			return true;
		}
		catch(Exception e)
		{
			String errmsg = "Error " + what + ":" + e;
			showError(errmsg);
			return false;
		}
	}


	/** Called when user presse the Pause button. */
	public void pauseButtonPress()
	{
		if (outputThread != null)
			IamPaused = true;
	}

	/** Called when user presse the Quit button. */
	public void quitButtonPress()
	{
		// Stop output if it's currently going.
		if (outputThread != null)
			outputThread.cleanupAndDie();
		if (outs != null)
		{
			try { outs.close(); }
			catch(IOException ioe) {}
		}
		if (client != null)
		{
			try
			{
				client.sendGoodbye();
				client.disconnect();
			}
			catch (Exception e){}
		}
		dispose();
	}

	/**
	 * Called from output thread to signify that all messages have
	 * been received and saved.
	 * This means successful completion.
	 */
	public void dcpMsgOutputDone()
	{
		// Close output file.
		try {outs.close(); }
		catch(IOException ioe) {}
		outs = null;

		// Close interface to server.
		try
		{
			outputThread.cleanupAndDie();
			client.sendGoodbye();
			client.disconnect();
		}
		catch(Exception e) {}
		client = null;
		if (closeWhenDoneCheck.isSelected())
			dispose();
		else
			JOptionPane.showMessageDialog(getContentPane(),
				labels.getString("MessageOutput.outputCompleted"));
	}

	/**
	 Output thread will call this if it encounters a fatal error.
	 @param msg the error message
	*/
	public void dcpMsgOutputError(String msg)
	{
		showError(msg);
		try { outs.close(); }
		catch(IOException ioe) {}
		outs = null;
		try {client.disconnect(); }
		catch(Exception e) {}
		client = null;
	}

	/**
	 * Called from output thread after each message has been
	 * saved, displays status on screen.
	 * @param msg the DcpMsg just received.
	 */
	public void dcpMsgOutputStatus(final DcpMsg msg)
	{
		try
		{
			final String s = IDateFormat.toString(msg.getDapsTime(), false);
			final int c = ++count;

			statusUpdater.setStatus(msg.getDcpAddress().toString(),
				s, "" + c);
		}
		catch(NumberFormatException ex)
		{
			Logger.instance().log(Logger.E_WARNING,
				"Cannot get DAPS Time Stamp from message '"
				+ msg.getHeader() + ": " + ex);
		}
	}

private static int idx = 0;
private static int idx2 = 0;

	class StatusUpdater extends Thread
	{
		String addr = "";
		String time = "";
		String count = "";
		final MessageOutput parent;

		StatusUpdater(MessageOutput p)
		{
			parent = p;
			setStatus("", "", "");
		}

		public void run()
		{
			// Need to sleep to avoid race condition before panel is made
			// visible.
			try { sleep(5000L); } catch(InterruptedException e) {}
			while(parent.isVisible())
			{
				SwingUtilities.invokeLater(
					new Runnable()
					{
						public void run()
						{
							try
							{
								parent.dcpAddressLabel.setText(addr);
								parent.msgTimeLabel.setText(time);
								parent.countLabel.setText(count);
							}
							catch(Exception e)
							{
							}
						}
					});

				try { sleep(1000L); }
				catch(InterruptedException e) {}
			}
		}

		void setStatus(String addr, String time, String count)
		{
			this.addr = addr;
			this.time = time;
			this.count = count;
		}
	}

	/** Does nothing. */
	public void dcpMsgTimeout()
	{
	}

	/**
	 * Called from output thread after each message has been
	 * saved to determine if user has pressed the PAUSE button.
	 */
	public boolean dcpMsgOutputIsPaused()
	{
		return IamPaused;
	}

	// Usage <SearchCriteriaEditor -f Filename.
	static CmdLineArgs cmdLineArgs = new CmdLineArgs(true, "util.log");
	static StringToken searchcrit_arg= new StringToken(
		"f", "Search Crit File", "", TokenOptions.optSwitch, "");
	static IntegerToken port_arg = new IntegerToken(
		"p", "Port Number", "", TokenOptions.optSwitch, DEFAULT_PORT_NUM);
	static StringToken user_arg = new StringToken(
		"u", "User Name", "", TokenOptions.optSwitch|TokenOptions.optRequired, "");
	static StringToken prefix_arg = new StringToken(
		"b", "Prefix", "", TokenOptions.optSwitch, "\\n");
	static StringToken suffix_arg = new StringToken(
		"a", "After", "", TokenOptions.optSwitch, "\\n");
	static BooleanToken netlist_arg = new BooleanToken(
		"n", "Send Network Lists", "", TokenOptions.optSwitch, false);
	static BooleanToken doDecode_arg = new BooleanToken(
		"R", "Decode Data", "", TokenOptions.optSwitch, false);
	static StringToken beforeData_arg = new StringToken(
		"B", "Before Data", "", TokenOptions.optSwitch, "\\n----\\n");
	static StringToken afterData_arg = new StringToken(
		"A", "After Data", "", TokenOptions.optSwitch, "====\\n");
	static
	{
		cmdLineArgs.addToken(searchcrit_arg);
		cmdLineArgs.addToken(port_arg);
		cmdLineArgs.addToken(user_arg);
		cmdLineArgs.addToken(prefix_arg);
		cmdLineArgs.addToken(suffix_arg);
		cmdLineArgs.addToken(netlist_arg);
		cmdLineArgs.addToken(doDecode_arg);
		cmdLineArgs.addToken(beforeData_arg);
		cmdLineArgs.addToken(afterData_arg);
	}


	/**
	Test main so user can start this frame directly from command line.
	*/
	public static void main(String args[])
		throws Exception
	{
		// Parse command line args & get argument values:
		cmdLineArgs.parseArgs(args);

		GuiApp.setAppName(LrgsApp.ShortID);
		GeneralProperties.init();

		String scf = searchcrit_arg.getValue();
		SearchCriteria searchcrit = null;
		if (scf != null && scf.length() > 0)
			searchcrit = new SearchCriteria(new File(scf));

		MessageOutput me = new MessageOutput(
			cmdLineArgs.getHostName(), port_arg.getValue(), 
			user_arg.getValue(),
			searchcrit, prefix_arg.getValue(), suffix_arg.getValue(),
			netlist_arg.getValue(),
			doDecode_arg.getValue(),
			beforeData_arg.getValue(), afterData_arg.getValue(), true);

		GuiApp.setTopFrame(me);
		me.startup(100, 100);
	}
}
