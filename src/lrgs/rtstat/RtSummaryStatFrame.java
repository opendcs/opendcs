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
import java.util.Date;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.Enumeration;
import java.util.Properties;
import org.w3c.dom.Document;

import ilex.gui.JobDialog;
import ilex.util.AsciiUtil;
import ilex.util.AuthException;
import ilex.util.ByteUtil;
import ilex.util.EnvExpander;
import ilex.util.LoadResourceBundle;
import ilex.util.ProcWaiterCallback;
import ilex.util.ProcWaiterThread;
import ilex.util.PropertiesUtil;


/**
Main frame for the LRGS Real-Time Summary Status Application.
*/
public class RtSummaryStatFrame
	extends JFrame
	implements ProcWaiterCallback
{
	private static ResourceBundle labels = 
		RtSummaryStat.getLabels();
	private static ResourceBundle genericLabels = 
		RtSummaryStat.getGenericLabels();
	private JPanel contentPane;
	private BorderLayout contentPaneLayout = new BorderLayout();
	private JPanel topPanel = new JPanel();
	private JLabel urlLabel;
	private JTextField urlField = new JTextField();
	private JButton pauseButton = new JButton();
	private JButton exitButton = new JButton();
	private GridBagLayout topPanelLayout = new GridBagLayout();
    private RtSummaryStatPanel rtSummaryStatPanel = new RtSummaryStatPanel();

	/** True if the display is currently paused. */
	boolean isPaused = false;
	int scanPeriod;

	/** The background polling thread. */
	RtSummaryStatThread myThread;
	private String url;
	private HashSet<String> runningRtStats;

	/** Constructor. */
	public RtSummaryStatFrame(int scanPeriod, String url)
	{
		urlLabel = new JLabel(labels.getString(
				"RtSummaryStatFrame.lrgsMonURL"));
		this.scanPeriod = scanPeriod;
		this.url = url;
		enableEvents(AWTEvent.WINDOW_EVENT_MASK);
		try
		{
			jbInit();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		isPaused = false;

		Dimension d = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
		d.width = 1000;
		d.height -= 160;
		setSize(d);
		runningRtStats = new HashSet<String>();
	}

	public void startThread()
	{
		myThread = new RtSummaryStatThread(this, scanPeriod);
		myThread.start();
		rtSummaryStatPanel.htmlPanel.addHyperlinkListener(
			new HyperlinkListener()
			{
				public void hyperlinkUpdate(HyperlinkEvent e)
				{
					if (e.getEventType() != HyperlinkEvent.EventType.ACTIVATED)
						return;
					startRtStat(e.getURL());
				}
			});
	}

	private void startRtStat(URL url)
	{
		// URL is of the form:
		//   http://lrgsmon-host/sub/directories/lrgs-host.html[?ext-host]
		// If an external host name argument is provided, use it.
		// Otherwise, parse out the "lrgs-host" from the html file name.
		String host = url.getQuery();
		if (host == null || host.trim().length() == 0)
		{
			host = url.getPath();
			int idx = host.lastIndexOf("/");
			if (idx >= 0)
				host = host.substring(idx+1);
			if (host.toLowerCase().endsWith(".html"))
				host = host.substring(0, host.length()-5);
		}

		if (runningRtStats.contains(host))
		{
			showError(labels.getString(
				"RtSummaryStatFrame.realTimeWindowOpenErr"));
			return;
		}
		runningRtStats.add(host);

		// Spawn a new JVM and execute RtStat in it.
		StringBuilder cmd = new StringBuilder();
		cmd.append("java -cp ");
		cmd.append(System.getProperty("java.class.path"));
		String p = System.getProperty("LRGSHOME");
		if (p != null)
			cmd.append(" -DLRGSHOME=" + p);
		p = System.getProperty("DECODES_INSTALL_DIR");
		if (p != null)
			cmd.append(" -DDECODES_INSTALL_DIR=" + p);
		cmd.append(" lrgs.rtstat.RtStat -h " + host);
		String cmds = cmd.toString();
System.out.println("Executing: " + cmds);
		try { ProcWaiterThread.runBackground(cmds, host, this, host); }
		catch(IOException ex)
		{
			showError(LoadResourceBundle.sprintf(labels.getString(
					"RtSummaryStatFrame.cantRunErr"),host, cmds) + ex);
			runningRtStats.remove(host);
		}
	}

	/** Initializes GUI components. */
	private void jbInit()
		throws Exception
	{
		contentPane = (JPanel)this.getContentPane();
		contentPane.setLayout(contentPaneLayout);
		this.setSize(new Dimension(793, 716));
		this.setTitle(labels.getString("RtSummaryStatFrame.title"));

		topPanel.setLayout(topPanelLayout);
		contentPane.add(topPanel, BorderLayout.NORTH);

//		urlField.setMinimumSize(new Dimension(60, 23));
//		urlField.setPreferredSize(new Dimension(60, 23));
		urlField.setText(url);
		urlField.addActionListener(
			new java.awt.event.ActionListener()
			{
	    		public void actionPerformed(ActionEvent e)
	    		{
					urlField_actionPerformed();
	    		}
			});

		exitButton.setText(genericLabels.getString("exit"));
		exitButton.setMinimumSize(new Dimension(100, 27));
		exitButton.setPreferredSize(new Dimension(100, 27));
		exitButton.addActionListener(
			new java.awt.event.ActionListener()
			{
	    		public void actionPerformed(ActionEvent e)
	    		{
					exitButton_actionPerformed();
	    		}
			});

		pauseButton.setMinimumSize(new Dimension(100, 27));
		pauseButton.setPreferredSize(new Dimension(100, 27));
		pauseButton.setText(labels.getString("RtStatFrame.pause"));
		pauseButton.addActionListener(
			new java.awt.event.ActionListener()
			{
	    		public void actionPerformed(ActionEvent e)
	    		{
					pauseButton_actionPerformed();
	    		}
			});

		topPanel.add(urlLabel,  new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, 
			new Insets(10, 10, 10, 2), 0, 0));
		topPanel.add(urlField,  new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
			new Insets(10, 0, 10, 6), 0, 6));
		topPanel.add(pauseButton,  new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, 
			new Insets(10, 5, 10, 5), 0, 0));
		topPanel.add(exitButton,  new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, 
			new Insets(10, 5, 10, 10), 0, 0));

		contentPane.add(rtSummaryStatPanel, BorderLayout.CENTER);
	}

	//File | Exit action performed
	private void exitButton_actionPerformed()
	{
		System.exit(0);
	}

	//Overridden so we can exit when window is closed
	protected void processWindowEvent(WindowEvent e)
	{
		super.processWindowEvent(e);
		if (e.getID() == WindowEvent.WINDOW_CLOSING)
		{
			exitButton_actionPerformed();
		}
	}

    private void pauseButton_actionPerformed()
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
	public void updateStatus()
	{
    	String url = urlField.getText().trim();
		if (url.length() == 0)
			return;
    	try { rtSummaryStatPanel.updateStatus(url); }
		catch(IOException ex)
		{
			showError(LoadResourceBundle.sprintf(labels.getString(
					"RtSummaryStatFrame.cantUpdateErr"), url) + ex);
		}
	}

//	private void loadHistory()
//	{
//		file f = new File(EnvExpander.expand("$LRGSHOME/SumStatHistory"));
//		if (!f.canRead())
//		{
//			f = new File(EnvExpander.expand("$HOME/SumStatHistory"));
//			if (!f.canRead())
//			{
//				f = new File(EnvExpander.expand(
//					"$DECODES_INSTALL_DIR/SumStatHistory"));
//				if (!f.canRead())
//				{
//					// no history;
//					f = null;
//				}
//			}
//		}
//		try
//		{
//			FileInputStream fis = new FileInputStream(file);
//			connectionList.clear();
//			connectionList.load(fis);
//			fis.close();
//		}
//		catch(IOException ioe)
//		{
//			System.out.println("No previously recorded connections");
//		}
//		Enumeration enames = connectionList.propertyNames();
//		int selected = -1;
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
//		if (selected == -1)
//		{
//			hostCombo.addItem(connectedHostName);
//			selected = i;
//		}
//		hostCombo.setSelectedIndex(selected);
//	}

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

	public void setLrgsMonUrl(String lrgsMonUrl)
	{
		urlField.setText(lrgsMonUrl);
	}

	public void procFinished(String procName, Object obj, int exitStatus)
	{
		String host = (String)obj;
System.out.println("RtStat for host '" + host + "' terminated.");
		runningRtStats.remove(host);
	}

	private void urlField_actionPerformed()
	{
		if (myThread != null)
			myThread.updateNow = true;
	}
}
