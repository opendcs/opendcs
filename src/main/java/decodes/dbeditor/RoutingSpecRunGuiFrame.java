/*
 * $Id$
 * 
 * $Log$
 * Revision 1.3  2018/11/14 15:59:56  mmaloney
 * Remove obsolete catch block.
 *
 * Revision 1.2  2018/06/04 19:23:38  mmaloney
 * HDB issue where deleted values were being displayed on table and graph.
 *
 * Revision 1.1  2017/08/22 19:57:35  mmaloney
 * Refactor
 *
 * 
 * Copyright 2014 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 */
package decodes.dbeditor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Date;
import java.util.ResourceBundle;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import decodes.gui.TopFrame;
import decodes.tsdb.TimeSeriesDb;
import decodes.util.DecodesSettings;
import ilex.gui.DateTimeCalendar;

/**
 * The frame for running computations interactively.
 */
@SuppressWarnings("serial")
public class RoutingSpecRunGuiFrame extends TopFrame
{
	static ResourceBundle labels = null;
	private static ResourceBundle genericLabels = null;
	public static String description;
	public static String selectFromListLabel;
	private String removeFromListLabel;
	private String selectButtonLabel;
	private String selectTimeRun;
	private String fromLabel;
	private String toLabel;
	private String runRoutingSpecButton;
	private String saveOutputButton;
	private String noRoutingSpecSelectedErr;
	private String saveRoutingSpecOutput;
	private DateTimeCalendar fromDTCal;
	private DateTimeCalendar toDTCal;
	private String module = "RoutingSpecRunGuiFrame";
	private String timeZoneStr;
	private TraceDialog traceDialog = null;
	private TimeSeriesDb theDb;

	public RoutingSpecRunGuiFrame()
	{
		super();
		timeZoneStr = DecodesSettings.instance().sqlTimeZone;
		timeZoneStr = timeZoneStr == null ? "UTC" : timeZoneStr;
//		setAllLabels();

		Date tempDate = new Date();
		fromDTCal = new DateTimeCalendar(fromLabel, null, "dd MMM yyyy", timeZoneStr);
		toDTCal = new DateTimeCalendar(toLabel, tempDate, "dd MMM yyyy", timeZoneStr);

		JPanel mycontent = (JPanel)this.getContentPane();
		mycontent.setLayout(new BoxLayout(mycontent, BoxLayout.Y_AXIS));
		this.setTitle(labels.getString("RoutingSpecRunGuiFrame.frameTitle"));
		this.trackChanges("runcomps");
		// this.setSize(750,825);//800
		mycontent.add(listPanel());
		mycontent.add(timePanel());
		pack();

		// Default operation is to do nothing when user hits 'X' in
		// upper right to close the window. We will catch the closing
		// event and do the same thing as if user had hit close.
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				doClose();
			}
		});
		exitOnClose = true;

	}

	private void setAllLabels()
	{
		removeFromListLabel = labels.getString("RoutingSpecRunGuiFrame.removeFromListLabel");
		selectButtonLabel = labels.getString("RoutingSpecRunGuiFrame.selectButtonLabel");
		selectTimeRun = labels.getString("RoutingSpecRunGuiFrame.selectTimeRun");
		fromLabel = labels.getString("RoutingSpecRunGuiFrame.fromLabel");
		toLabel = labels.getString("RoutingSpecRunGuiFrame.toLabel");
		runRoutingSpecButton = labels.getString("RoutingSpecRunGuiFrame.runCompsButton");
		saveOutputButton = labels.getString("RoutingSpecRunGuiFrame.saveOutputButton");
		noRoutingSpecSelectedErr = labels.getString("RoutingSpecRunGuiFrame.noCompSelectedErr");
		saveRoutingSpecOutput = labels.getString("RoutingSpecRunGuiFrame.saveCompOutput");
	}

	private JPanel listPanel()
	{
		JPanel list = new JPanel();
		list.setPreferredSize(new Dimension(600, 200));
		list.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED), labels
			.getString("RoutingSpecRunGuiFrame.selectCompRunTitle")));
		list.setLayout(new BorderLayout());
		JScrollPane myscroll = new JScrollPane();
		list.add(myscroll, BorderLayout.CENTER);
		JPanel listButtonPanel = new JPanel();
		list.add(listButtonPanel, BorderLayout.EAST);
		listButtonPanel.setLayout(new GridBagLayout());
		return list;
	}

	private JPanel timePanel()
	{
		JPanel time = new JPanel();
		time.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED), selectTimeRun));
		time.setLayout(new BorderLayout());

		JPanel timehalf = new JPanel();
		timehalf.setLayout(new GridBagLayout());
		timehalf.add(fromDTCal, new GridBagConstraints(0, 0, 1, 1, 0.5, 0, GridBagConstraints.CENTER,
			GridBagConstraints.HORIZONTAL, new Insets(4, 10, 4, 10), 0, 0));
		timehalf.add(new JLabel(" (" + timeZoneStr + ")"), new GridBagConstraints(1, 0, 1, 1, 0, 0,
			GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(4, 10, 4, 10), 0, 0));
		timehalf.add(toDTCal, new GridBagConstraints(0, 1, 1, 1, 0.5, 0, GridBagConstraints.CENTER,
			GridBagConstraints.HORIZONTAL, new Insets(4, 10, 4, 10), 0, 0));
		time.add(timehalf, BorderLayout.WEST);

		JPanel runhalf = new JPanel();
		runhalf.setLayout(new GridBagLayout());
		JButton runButton = new JButton(runRoutingSpecButton);
		runButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				runButtonPressed();
			}
		});
		JButton saveButton = new JButton(saveOutputButton);
		saveButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				saveButtonPressed();
			}
		});

		runhalf.add(runButton, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.CENTER,
			GridBagConstraints.HORIZONTAL, new Insets(4, 10, 0, 10), 0, 0));
		runhalf.add(saveButton, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.CENTER,
			GridBagConstraints.HORIZONTAL, new Insets(4, 10, 0, 10), 0, 0));
		time.add(runhalf, BorderLayout.EAST);
		return time;
	}

	protected void traceButtonPressed()
	{
		traceDialog.setVisible(true);
	}

	/** Sets the Database used here */
	public void setDb(TimeSeriesDb mydb)
	{
		this.theDb = mydb;
	}

	private void runButtonPressed()
	{
	}

	private boolean doClose()
	{
		return true;
	}

	/**
	 * This is used when this GUI is launch from the Comp Edit GUI
	 * 
	 * @return true or false
	 */
	public boolean closeFromParent()
	{
		return doClose();
	}

	private void saveButtonPressed()
	{
		int r = JOptionPane.showConfirmDialog(this, "Are you sure to save the computation output ?",
			labels.getString("RoutingSpecRunGuiFrame.frameTitle"), JOptionPane.YES_NO_OPTION);
		if (r == JOptionPane.YES_OPTION)
		{
			saveCompOutput();
		}

	}

	private void saveCompOutput()
	{
	}

}