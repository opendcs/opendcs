/**
 * $Id$
 * 
 * This software was written by Cove Software, LLC ("COVE") under contract 
 * to the United States Government. No warranty is provided or implied 
 * other than specific contractual terms between COVE and the U.S. Government
 * 
 * Copyright 2017 U.S. Government.
 *
 * $Log$
 */
package decodes.eventmon;

import ilex.gui.EventsPanel;
import ilex.util.EnvExpander;
import ilex.util.LoadResourceBundle;
import ilex.util.Logger;
import ilex.util.ProcWaiterThread;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.TimeZone;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import opendcs.dai.ComputationDAI;
import opendcs.dai.LoadingAppDAI;
import opendcs.dai.ScheduleEntryDAI;
import decodes.db.Database;
import decodes.gui.SortingListTable;
import decodes.gui.TopFrame;
import decodes.polling.DacqEvent;
import decodes.tsdb.CompAppInfo;
import decodes.tsdb.CompFilter;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesDb;
import decodes.util.DecodesSettings;


/**
 * Main frame for event monitor GUI
 * @author mmaloney Mike Maloney, Cove Software, LLC
 */
@SuppressWarnings("serial")
public class EventMonitorFrame 
	extends TopFrame
{
	private DacqEventTableModel model = null;
	ResourceBundle genericLabels = null;
	ResourceBundle eventmonLabels = null;
//	private static ResourceBundle compeditLabels = null;
	private SortingListTable dacqEventTable = null;
//	private AppInfoStatus selectedProc = null;
//	private ArrayList<ProcessEditDialog> editDialogs = new ArrayList<ProcessEditDialog>();
	private TimeSeriesDb tsdb = null;
	private JComboBox processCombo = new JComboBox();
	private JComboBox severityCombo = new JComboBox(
		new String[] { "INFO", "WARNING", "FAILURE", "FATAL" });
	private JTextField containingField = new JTextField();
	
	//TODO replace since and until fields with calendar/time widgets
	private JTextField sinceField = new JTextField();
	private JTextField untilField = new JTextField();
	
	private String timeFormat = "yyyy/MM/dd-HH:mm:ss";
	private SimpleDateFormat sdf = new SimpleDateFormat(timeFormat);
	
	private EventMonitor parent = null;
	
	/**
	 * Constructor
	 */
	public EventMonitorFrame(EventMonitor parent)
	{
		this.parent = parent;
		sdf.setTimeZone(TimeZone.getTimeZone(DecodesSettings.instance().guiTimeZone));
		DecodesSettings settings = DecodesSettings.instance();
		genericLabels = LoadResourceBundle.getLabelDescriptions("decodes/resources/generic", settings.language);
		eventmonLabels = LoadResourceBundle.getLabelDescriptions("decodes/resources/eventmon", settings.language);
		DacqEvent.setTimeFormat(timeFormat, DecodesSettings.instance().guiTimeZone);
		guiInit();
		pack();
		trackChanges("EventMonitorFrame");
	}
	
	private void guiInit()
	{
		this.setTitle(eventmonLabels.getString("frameTitle"));
		model = new DacqEventTableModel(this);
		
		model.reload(null, null, null, Logger.E_INFORMATION, null);
		
		processCombo.addItem("<any>");
		ArrayList<CompAppInfo> apps = new ArrayList<CompAppInfo>();
		apps.addAll(model.getAllApps());
		Collections.sort(apps, 
			new Comparator<CompAppInfo>()
			{
				@Override
				public int compare(CompAppInfo o1, CompAppInfo o2)
				{
					return o1.getAppName().compareTo(o2.getAppName());
				}
			});
		for(CompAppInfo app : model.getAllApps())
			processCombo.addItem(app.getAppName());
		JPanel mainPanel = (JPanel) this.getContentPane();
		mainPanel.setLayout(new BorderLayout());
		
		
		// =========== North Filter Panel ====================
		JPanel filterPanel = new JPanel(new GridBagLayout());
		mainPanel.add(filterPanel, BorderLayout.NORTH);
		filterPanel.setBorder(new TitledBorder(eventmonLabels.getString("filter_border")));
		
		filterPanel.add(new JLabel(eventmonLabels.getString("process") + ":"),
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 15, 2, 2), 0, 0));
		filterPanel.add(processCombo,
			new GridBagConstraints(1, 0, 1, 1, 0.3, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(2, 0, 2, 20), 0, 0));

		filterPanel.add(new JLabel(eventmonLabels.getString("minseverity") + ":"),
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 15, 2, 2), 0, 0));
		filterPanel.add(severityCombo,
			new GridBagConstraints(1, 1, 1, 1, 0.3, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(2, 0, 2, 20), 0, 0));
	
		filterPanel.add(new JLabel(eventmonLabels.getString("containing") + ":"),
			new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 15, 2, 2), 0, 0));
		filterPanel.add(containingField,
			new GridBagConstraints(1, 2, 1, 1, 0.3, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(2, 0, 2, 20), 0, 0));

		filterPanel.add(new JLabel(eventmonLabels.getString("since") + ":"),
			new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 15, 2, 2), 0, 0));
		filterPanel.add(sinceField,
			new GridBagConstraints(3, 0, 1, 1, 0.3, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(2, 0, 2, 20), 0, 0));
		sinceField.setToolTipText(timeFormat);

		filterPanel.add(new JLabel(eventmonLabels.getString("until") + ":"),
			new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 15, 2, 2), 0, 0));
		filterPanel.add(untilField,
			new GridBagConstraints(3, 1, 1, 1, 0.3, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(2, 0, 2, 20), 0, 0));
		untilField.setToolTipText(timeFormat);

		JButton applyButton = new JButton(" " + eventmonLabels.getString("apply") + " ");
		applyButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					applyPressed();
				}
			});
		filterPanel.add(applyButton,
			new GridBagConstraints(2, 2, 2, 1, 0.3, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(2, 5, 2, 5), 0, 0));

		// =========== Center Panel Contains Event Table ====================
	
		JPanel tablePanel = new JPanel(new BorderLayout());
		JScrollPane scrollPane = new JScrollPane(
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		dacqEventTable = new SortingListTable(model, model.widths);
		scrollPane.setViewportView(dacqEventTable);
		tablePanel.add(scrollPane, BorderLayout.CENTER);
		mainPanel.add(tablePanel, BorderLayout.CENTER);
		
		// =========== South Button Panel ====================

		JPanel buttonPanel = new JPanel(new FlowLayout());
		mainPanel.add(buttonPanel, BorderLayout.SOUTH);
		
		JButton exportButton = new JButton(genericLabels.getString("export"));
		exportButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					exportPressed();
				}
			});
		buttonPanel.add(exportButton);
		
		JButton quitButton = new JButton(genericLabels.getString("quit"));
		quitButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					quitPressed();
				}
			});
		buttonPanel.add(quitButton);

		dacqEventTable.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
	}
	

	protected void applyPressed()
	{
		String appName = (String)processCombo.getSelectedItem();
		if (appName.equals("any"))
			appName = null;
		String s = sinceField.getText().trim();
		Date since = null;
		if (s.length() > 0)
		{
			try { since = sdf.parse(s); }
			catch(ParseException ex)
			{
				showError("Invalid since time '" + s + "' -- must be in format: " + timeFormat);
				return;
			}
//System.out.println("Since=" + since);
		}
		
		Date until = null;
		s = untilField.getText().trim();
		if (s.length() > 0)
		{
			try { until = sdf.parse(s); }
			catch(ParseException ex)
			{
				showError("Invalid until time '" + s + "' -- must be in format: " + timeFormat);
				return;
			}
//System.out.println("Until=" + until);
		}
		
		s = (String)severityCombo.getSelectedItem();
		int minSeverity = 
			s.equals("INFO") ? Logger.E_INFORMATION :
			s.equals("WARNING") ? Logger.E_WARNING :
			s.equals("FAILURE") ? Logger.E_FAILURE : Logger.E_FATAL;

		String containing = containingField.getText().trim();
		if (containing.length() == 0)
			containing = null;
		
		model.reload(appName, since, until, minSeverity, containing);
	}

	protected void exportPressed()
	{
		showError("Export not implemented yet. Try cut/paste.");
		//TODO if no rows selected, export entire table.
		// else only export selected rows.
		
		// TODO Query for file name
		
		// TODO Save selected rows as an excel file
		
	}
	
	private void quitPressed()
	{
		parent.close();
	}


	public void cleanupBeforeExit()
	{
	}
	
	public void launch( int x, int y, int w, int h )
	{
		setBounds(x,y,w,h);
		setVisible(true);

		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		final EventMonitorFrame myframe = this;
		addWindowListener(
			new WindowAdapter()
			{
				public void windowClosed(WindowEvent e)
				{
					myframe.cleanupBeforeExit();
					if (exitOnClose)
						System.exit(0);
				}
			});
	}

	public DacqEventTableModel getModel() { return model; }
	
	public void setExitOnClose(boolean exitOnClose)
	{
		this.exitOnClose = exitOnClose;
	}


}

