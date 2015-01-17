/*
 *  $Id$
 *
 *  $Log$
 *  Revision 1.3  2014/05/30 13:15:35  mmaloney
 *  dev
 *
 *  Revision 1.2  2014/05/28 13:09:31  mmaloney
 *  dev
 *
 *  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 *  OPENDCS 6.0 Initial Checkin
 *
 *  Revision 1.5  2010/09/13 15:31:11  mmaloney
 *  dev
 *
 *  Revision 1.4  2010/09/13 15:23:47  mmaloney
 *  Temporary fix for dialog changing binary data. Replace binary values with '$'.
 *
 *  Revision 1.3  2009/03/11 18:30:01  mjmaloney
 *  iridium development
 *
 *  Revision 1.2  2008/09/24 13:58:25  mjmaloney
 *  network DCPs
 *
 *  Revision 1.1  2008/04/04 18:21:00  cvs
 *  Added legacy code to repository
 *
 *  Revision 1.17  2008/01/14 14:56:43  mmaloney
 *  dev
 *
 *  Revision 1.16  2008/01/11 22:14:45  mmaloney
 *  Internationalization
 *  ~Dan
 *
 *  Revision 1.15  2005/09/06 15:29:50  mjmaloney
 *  Added decins-6-4.xml build file
 *
 *  Revision 1.14  2004/09/20 14:18:48  mjmaloney
 *  Javadocs
 *
 *  Revision 1.13  2004/08/27 18:41:33  mjmaloney
 *  Platwiz work
 *
 *  Revision 1.12  2004/07/28 20:54:49  mjmaloney
 *  dev
 *
 */
package decodes.dbeditor;

import ilex.util.AsciiUtil;
import ilex.util.LoadResourceBundle;
import ilex.util.Logger;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.*;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Vector;

import decodes.datasource.DataSourceEndException;
import decodes.datasource.DataSourceException;
import decodes.datasource.DataSourceExec;
import decodes.datasource.RawMessage;
import decodes.db.DataSource;
import decodes.db.Database;
import decodes.db.InvalidDatabaseException;
import decodes.gui.GuiDialog;
import decodes.gui.TopFrame;
import decodes.util.DecodesSettings;

/**
 * Dialog for user to specify parameters needed to load a sample message.
 */
public class LoadMessageDialog extends GuiDialog
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

	JButton okButton = new JButton();
	JButton cancelButton = new JButton();
	ButtonGroup sourceButtonGroup = new ButtonGroup();
	static private JFileChooser jFileChooser = new JFileChooser();
	static
	{
		jFileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
	}
	JCheckBox autoCloseCheck = new JCheckBox();
	JButton selectFileButton = new JButton();
	static JTextField channelField = new JTextField();
	static JTextField dcpAddressField = new JTextField();
	JRadioButton loadFromFileButton = new JRadioButton();
	JTextField filePathField = new JTextField();
	JRadioButton loadFromLrgsButton = new JRadioButton();
	TitledBorder titledBorder1;
	TitledBorder titledBorder2;
	JScrollPane jScrollPane1 = new JScrollPane();
	static JTextArea resultsArea = new JTextArea();
	JComboBox lrgsCombo = new JComboBox();

	/** The area we are to fill. */
	private JTextArea targetArea = null;
	/** Alternately, the JTextPane we are to fill. */

	// DecodingScriptEditDialog parent;

	/** The thread that does the work. */
	LoadMessageThread loadMessageThread = null;

	// Retain Last data source used from one call to the next.
	private static int lastDataSourceIdx = -1;
	
	private SampleMessageOwner sampleMessageOwner = null;

	/**
	 * Constructor.
	 * 
	 * @param frame
	 *            the parent frame
	 * @param title
	 *            the Title of this dialog
	 * @param modal
	 *            true if this is a modal dialog
	 */
	public LoadMessageDialog(Frame frame, String title, boolean modal)
	{
		super(frame, title, modal);
		jbInit();
		sourceButtonGroup.add(loadFromLrgsButton);
		sourceButtonGroup.add(loadFromFileButton);
		loadFromLrgsButton.setSelected(true);
		dcpAddressField.setEnabled(true);
		channelField.setEnabled(true);
		filePathField.setEnabled(false);
		selectFileButton.setEnabled(false);
		autoCloseCheck.setSelected(true);
		lrgsCombo.setEnabled(true);
		fillLrgsCombo();
		getRootPane().setDefaultButton(okButton);
		pack();
	}

	/** Fill the LRGS selection combo with my list of LRGS's */
	private void fillLrgsCombo()
	{
		for (Iterator<DataSource> it = Database.getDb().dataSourceList.iterator(); it.hasNext();)
		{
			DataSource ds = it.next();
			if (ds.dataSourceType.equalsIgnoreCase("lrgs"))
				lrgsCombo.addItem(ds.getName());
		}
		DecodesSettings settings = DecodesSettings.instance();
		if (lastDataSourceIdx != -1 && lastDataSourceIdx < lrgsCombo.getItemCount())
			lrgsCombo.setSelectedIndex(lastDataSourceIdx);
		else if (settings.defaultDataSource != null && settings.defaultDataSource.length() > 0)
			lrgsCombo.setSelectedItem(settings.defaultDataSource);
	}

	/** No-args constructor for JBuilder */
	public LoadMessageDialog()
	{
		this(getDbEditFrame(), "", true);
	}

	/** Sets parent, who contains the text area being filled. */
	// public void setParent(DecodingScriptEditDialog parent)
	// {
	// this.parent = parent;
	// }

	/**
	 * Sets the target Text Area we are to fill.
	 * 
	 * @param area
	 *            The area.
	 */
	public void setTargetArea(JTextArea area)
	{
		this.targetArea = area;
	}

	/** Initialize GUI components. */
	private void jbInit()
	{
		titledBorder1 = new TitledBorder(dbeditLabels.getString("LoadMessageDialog.BorderTitle"));
		titledBorder2 = new TitledBorder(dbeditLabels.getString("LoadMessageDialog.BorderTitle2") + " ");
		JPanel panel1 = new JPanel(new BorderLayout());
		this.setModal(true);
		this.setTitle(dbeditLabels.getString("LoadMessageDialog.Title"));
		okButton.setText(genericLabels.getString("OK"));
		okButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				okButtonPressed();
			}
		});
		JPanel jPanel2 = new JPanel(new FlowLayout(FlowLayout.LEADING, 15, 15));
		cancelButton.setText(genericLabels.getString("cancel"));
		cancelButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				cancelButtonPressed();
			}
		});
		
		autoCloseCheck.setText(dbeditLabels.getString("LoadMessageDialog.CloseCheck")
			+ "                        ");
		GridLayout gridLayout1 = new GridLayout();
		JPanel jPanel3 = new JPanel(gridLayout1);
		selectFileButton.setText(genericLabels.getString("select"));
		selectFileButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				selectFileButtonPressed();
			}
		});
		
		channelField.setFont(new java.awt.Font("Monospaced", 0, 12));
		dcpAddressField.setFont(new java.awt.Font("Monospaced", 0, 12));
		loadFromFileButton.setText(dbeditLabels.getString("LoadMessageDialog.LoadButton"));
		loadFromFileButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				loadFromFileButtonPressed();
			}
		});

		loadFromLrgsButton.setText(dbeditLabels.getString("LoadMessageDialog.LRGSLoad"));
		loadFromLrgsButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				loadFromLrgsButtonPressed();
			}
		});

		JPanel whereToLoadFromPanel = new JPanel(new GridBagLayout());
		whereToLoadFromPanel.setBorder(titledBorder1);
		JPanel jPanel5 = new JPanel(new BorderLayout());
		jPanel5.setFont(new java.awt.Font("MS Sans Serif", 0, 11));
		jPanel5.setBorder(titledBorder2);
		resultsArea.setText("");
		resultsArea.setEditable(false);
		resultsArea.setText("");
		gridLayout1.setColumns(1);
		gridLayout1.setHgap(0);
		gridLayout1.setRows(2);
		getContentPane().add(panel1);
		JPanel jPanel1 = new JPanel();
		panel1.add(jPanel1, BorderLayout.NORTH);
		jPanel1.add(new JLabel(dbeditLabels.getString("LoadMessageDialog.RetrieveLabel")), null);
		panel1.add(jPanel2, BorderLayout.SOUTH);
		jPanel2.add(autoCloseCheck, null);
		jPanel2.add(okButton, null);
		jPanel2.add(cancelButton, null);
		panel1.add(jPanel3, BorderLayout.CENTER);
		jPanel3.add(whereToLoadFromPanel, null);
		whereToLoadFromPanel.add(loadFromLrgsButton, 
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(5, 10, 5, 2), 0, 0));
		whereToLoadFromPanel.add(lrgsCombo, 
			new GridBagConstraints(1, 0, 3, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(5, 0, 5, 10), 0, 0));
		whereToLoadFromPanel.add(
			new JLabel(dbeditLabels.getString("LoadMessageDialog.Address")), 
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(5, 20, 5, 2), 0, 0));
		whereToLoadFromPanel.add(dcpAddressField,
			new GridBagConstraints(1, 1, 1, 1, 0.5, 0.0, 
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, 
				new Insets(5, 0, 5, 0), 30, 0));
		whereToLoadFromPanel.add(
			new JLabel(dbeditLabels.getString("LoadMessageDialog.ChannelLabel")),
			new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(5, 20, 5, 2), 0, 0));
		whereToLoadFromPanel.add(channelField, 
			new GridBagConstraints(3, 1, 1, 1, 0.5, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(5, 0, 5, 10), 10, 0));
		
		whereToLoadFromPanel.add(loadFromFileButton, 
			new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(10, 10, 5, 2), 0, 0));
		whereToLoadFromPanel.add(
			new JLabel(dbeditLabels.getString("LoadMessageDialog.Path")),
			new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(5, 20, 5, 2), 0, 0));
		whereToLoadFromPanel.add(filePathField, 
			new GridBagConstraints(1, 3, 2, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(5, 0, 5, 5), 10, 0));
		whereToLoadFromPanel.add(selectFileButton, 
			new GridBagConstraints(3, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, 
				new Insets(5, 5, 5, 0), 0, 0));
		jPanel3.add(jPanel5, null);
		jPanel5.add(jScrollPane1, BorderLayout.CENTER);
		jScrollPane1.getViewport().add(resultsArea, null);
	}

	/**
	 * Called when LRGS Load button is pressed.
	 */
	void loadFromLrgsButtonPressed()
	{
		dcpAddressField.setEnabled(true);
		channelField.setEnabled(true);
		filePathField.setEnabled(false);
		selectFileButton.setEnabled(false);
		lrgsCombo.setEnabled(true);
	}

	/** Called when File Load button pressed. */
	void loadFromFileButtonPressed()
	{
		dcpAddressField.setEnabled(false);
		channelField.setEnabled(false);
		filePathField.setEnabled(true);
		selectFileButton.setEnabled(true);
		lrgsCombo.setEnabled(false);
	}

	/**
	 * Called when select file button is pressed.
	 */
	void selectFileButtonPressed()
	{
		if (jFileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
		{
			filePathField.setText(jFileChooser.getSelectedFile().getPath());
			okButtonPressed();
		}
	}

	/**
	 * Called when OK button is pressed.
	 */
	private void okButtonPressed()
	{
		if (loadFromFileButton.isSelected())
		{
			// parent.sampleMessageArea.setText("");
			File f = new File(filePathField.getText());
			try
			{
				FileReader fr = new FileReader(f);
				int i;
				StringBuffer sb = new StringBuffer((int) f.length());
				while ((i = fr.read()) != -1)
				{
					char c = (char) i;
					// make carriage returns visible.
					if (c == '\r')
						c = (char) 0x00AE;
					sb.append(c);
				}
				fr.close();
				sampleMessageOwner.setRawMessage(sb.toString());
				// parent.sampleMessageArea.setText(sb.toString());
				closeDlg();
			}
			catch (IOException ex)
			{
				showError("Cannot read '" + f.getName() + "': " + ex.toString());
			}
		}
		else
		// Load from LRGS
		{
			resultsArea.setText(dbeditLabels.getString("LoadMessageDialog.Validating") + "\n");

			// Validate DCP address and channel. Channel is optional.
			String dcpaddr = dcpAddressField.getText().trim();
			// MJM Don't do this validation. The address might be an Iridium ID,
			// or some other
			// kind of ID in the future.
			// if (dcpaddr.length() != 8)
			// {
			// showError(
			// dbeditLabels.getString("LoadMessageDialog.ValidateError1"));
			// return;
			// }
			// long xaddr;
			// try { xaddr = Long.parseLong(dcpaddr, 16); }
			// catch(NumberFormatException ex)
			// {
			// showError( "'" + dcpaddr +
			// dbeditLabels.getString("LoadMessageDialog.ValidateError2"));
			// return;
			// }
			// resultsArea.append(
			// dbeditLabels.getString("LoadMessageDialog.ValidResults")+"\n");
			int chan = -1;
			String s = channelField.getText().trim();
			if (s.length() > 0)
			{
				try
				{
					chan = Integer.parseInt(s);
					resultsArea.append(dbeditLabels
						.getString("LoadMessageDialog.ValidChannelResults") + "\n");
				}
				catch (NumberFormatException ex)
				{
					showError("'" + s + dbeditLabels.getString("LoadMessageDialog.ValidateError3"));
					return;
				}
			}
			else
				resultsArea.append(dbeditLabels.getString("LoadMessageDialog.ValidateError4")
					+ "\n");

			resultsArea.append(dbeditLabels.getString("LoadMessageDialog.Initializing") + " \n");
			String dsname = (String) lrgsCombo.getSelectedItem();
			lastDataSourceIdx = lrgsCombo.getSelectedIndex();
			DataSource myDS = Database.getDb().dataSourceList.get(dsname);
			if (myDS == null)
			{
				showError(dbeditLabels.getString("LoadMessageDialog.ValidateError5") + dsname
					+ dbeditLabels.getString("LoadMessageDialog.ValidateError6"));
				return;
			}
			resultsArea.append(dbeditLabels.getString("LoadMessageDialog.DataSource")
				+ myDS.getName() + "'\n");

			DataSourceExec myDSE = null;
			Properties rsProps = new Properties();
			try
			{
				myDSE = myDS.makeDelegate();
//				rsProps.setProperty("single", "true");
				rsProps.setProperty("dcpaddress", dcpaddr);
				if (chan != -1)
					rsProps.setProperty("channel", "&" + chan);
				myDSE.setAllowNullPlatform(true);
				myDSE.setAllowDapsStatusMessages(false);
			}
			catch (InvalidDatabaseException ex)
			{
				showError(dbeditLabels.getString("LoadMessageDialog.ValidateError7")
					+ myDS.getName() + "': " + ex);
				return;
			}

			okButton.setEnabled(false);
			loadMessageThread = new LoadMessageThread(this, myDSE, rsProps);
			loadMessageThread.start();
			// Return to UI thread.
		}
	}

	/**
	 * Called when Cancel button is pressed.
	 */
	void cancelButtonPressed()
	{
		if (loadMessageThread != null)
		{
			loadMessageThread.shutdown = true;
			loadMessageThread = null;
			okButton.setEnabled(true);
			resultsArea
				.append(dbeditLabels.getString("LoadMessageDialog.ValidateCancelled") + "\n");
			return;
		}
		closeDlg();
	}

	/** Closes the dialog */
	void closeDlg()
	{
		setVisible(false);
		dispose();
	}

	/**
	 * Displays error message in a sub-dialog
	 * 
	 * @param msg
	 *            the message
	 */
	public void showError(String msg)
	{
		Logger.instance().failure(msg);
		JOptionPane.showMessageDialog(this, AsciiUtil.wrapString(msg, 60),
			dbeditLabels.getString("LoadMessageDialog.Error"), JOptionPane.ERROR_MESSAGE);
	}

	/**
	 * Called from thread when a raw message is found.
	 * 
	 * @param rawMsg
	 *            the raw message to display in the area.
	 */
	void setRawMessage(RawMessage rawMsg)
	{
		final LoadMessageDialog lmd = this;
		final RawMessage rm = rawMsg;
		final StringBuilder sb = new StringBuilder();

		// MJM 20100913 When msg has pure-binary data in it, when we put it
		// in a text area and then read it out, the text area can interpret
		// some binary sequences as non-ascii characters and we get the wrong
		// data out. Temporary fix is to replace binaries with '$' and warn
		// the user that this has been done.
		byte[] msgData = rm.getData();
		boolean _hasBinary = false;
		for (int i = 0; i < msgData.length; i++)
		{
			byte b = msgData[i];
			if (b == 13) // carriage return
				sb.append((char) 0x00AE);
			else if (b > 127 || (b < 32 && b != 10))
			{
				sb.append('$');
				_hasBinary = true;
			}
			else
				sb.append((char) b);
		}
		final boolean hasBinary = _hasBinary;

		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				if (hasBinary)
					TopFrame.getDbEditFrame().showError(
						"Binary data in input has been replaced with '$' for display.");
				// StringBuffer sb = new StringBuffer(rm.toString());
				// for(int i=0; i<sb.length(); i++)
				// {
				// if (sb.charAt(i) == '\r')
				// sb.setCharAt(i, (char)0x00AE);
				// }
				sampleMessageOwner.setRawMessage(sb.toString());

				if (lmd.autoCloseCheck.isSelected())
					lmd.closeDlg();
				else
				{
					lmd.resultsArea.append(dbeditLabels.getString("LoadMessageDialog.Success")
						+ "\n");
					lmd.resultsArea.append(dbeditLabels
						.getString("LoadMessageDialog.PressToCancel"));
					lmd.loadMessageThread = null;
				}
			}
		});
	}

	/**
	 * Appends a one-line message to the progress area.
	 * 
	 * @param m
	 *            the message.
	 */
	void notifyProgress(String m)
	{
		final LoadMessageDialog lmd = this;
		final String msg = m;
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				lmd.resultsArea.append(msg + "\n");
			}
		});
	}

	/** Called from sub-thread when fatal error occurs. */
	void notifyError(String m)
	{
		final LoadMessageDialog lmd = this;
		final String msg = m;
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				lmd.resultsArea.append(msg + "\n");
				lmd.showError(msg);
				lmd.okButton.setEnabled(true);
				lmd.loadMessageThread = null;
			}
		});
	}

	/**
	 * Sets the DCP address field externally.
	 * 
	 * @param addr
	 *            the DCP address
	 */
	public static void setDcpAddress(String addr)
	{
		dcpAddressField.setText(addr == null ? "" : addr);
	}

	/**
	 * Sets the GOES channel field externally.
	 * 
	 * @param chan
	 *            the channel number, use -1 if no selection.
	 */
	public static void setGoesChannel(int chan)
	{
		channelField.setText(chan != -1 ? ("" + chan) : "");
	}

	/**
	 * Sets initial state for GOES checkbox
	 * 
	 * @param yn
	 *            true if GOES should be selected.
	 */
	public void enableGoes(boolean yn)
	{
		if (yn)
		{
			loadFromLrgsButtonPressed();
			loadFromLrgsButton.setSelected(true);
			loadFromFileButton.setSelected(false);
		}
		else
		{
			loadFromFileButtonPressed();
			loadFromLrgsButton.setSelected(false);
			loadFromFileButton.setSelected(true);
		}
	}

	public void setSampleMessageOwner(SampleMessageOwner sampleMessageOwner)
	{
		this.sampleMessageOwner = sampleMessageOwner;
	}
}

/**
 * This is the sub-thread that finds the message from my LRTG's.
 */
class LoadMessageThread extends Thread
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

	LoadMessageDialog parent;
	boolean shutdown;
	DataSourceExec dse;
	Properties rsProps;

	LoadMessageThread(LoadMessageDialog parent, DataSourceExec dse, Properties rsProps)
	{
		this.parent = parent;
		this.dse = dse;
		this.rsProps = rsProps;
	}

	public void run()
	{
		int back = 2;
		int prevback = 0;
		boolean no_init = false;
		while (!shutdown && back <= 64)
		{
			parent.notifyProgress(LoadResourceBundle.sprintf(
				dbeditLabels.getString("LoadMessageDialog.Searching"), back));
			try
			{
				if (!no_init)
					dse.init(rsProps, "now - " + back + " hours", "now - " + prevback + " hours",
						new Vector());
				RawMessage rawMsg = dse.getRawMessage();
				if (rawMsg != null)
				{
					parent.setRawMessage(rawMsg);
					dse.close();
					return;
				}
				else
				{
					no_init = true;
					continue;
				}
			}
			catch (DataSourceEndException ex)
			{
				prevback = back;
				back *= 2;
				dse.close();
				continue;
			}
			catch (DataSourceException ex)
			{
				parent.notifyError(dbeditLabels.getString("LoadMessageDialog.RunError1")
					+ dse.getName() + "': " + ex);
				shutdown = true;
			}
		}
		parent.notifyError(LoadResourceBundle.sprintf(
			dbeditLabels.getString("LoadMessageDialog.RunError2"), (back / 2)));
		dse.close();
	}
}
