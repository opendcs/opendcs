/**
 * $Id$
 *
 * $Log$
 * Revision 1.1  2016/07/20 15:40:12  mmaloney
 * First platstat impl GUI.
 *
 */
package decodes.platstat;

import ilex.gui.EventsPanel;
import ilex.util.LoadResourceBundle;
import ilex.util.Logger;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.TimeZone;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import opendcs.dai.DacqEventDAI;
import decodes.db.Database;
import decodes.db.DatabaseIO;
import decodes.db.PlatformStatus;
import decodes.db.ScheduleEntry;
import decodes.gui.SortingListTable;
import decodes.gui.TopFrame;
import decodes.polling.DacqEvent;
import decodes.sql.SqlDatabaseIO;
import decodes.tsdb.DbIoException;
import decodes.util.DecodesSettings;


/**
 * Main frame for process status monitor GUI
 * @author mmaloney Mike Maloney, Cove Software, LLC
 */
@SuppressWarnings("serial")
public class PlatformMonitorFrame 
	extends TopFrame 
{
	private PlatformTableModel platstatModel = null;
	private EventsPanel eventsPanel = new EventsPanel();
	private JSplitPane outerPane = null;
	static ResourceBundle genericLabels = null;
	static ResourceBundle procmonLabels = null;
	private SortingListTable platstatTable = null;
	private PlatformStatus selectedPS = null;
	private ArrayList<DacqEvent> evtList = new ArrayList<DacqEvent>();
	private SimpleDateFormat evtTimeSdf = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
	private JLabel platEventsLabel = new JLabel();
	private PlatformMonitor parent = null;
	private boolean inDbUpdate = false;
	
	/**
	 * Constructor
	 */
	public PlatformMonitorFrame(PlatformMonitor parent)
	{
		this.parent = parent;
		DecodesSettings settings = DecodesSettings.instance();
		genericLabels = LoadResourceBundle.getLabelDescriptions("decodes/resources/generic", settings.language);
		procmonLabels = LoadResourceBundle.getLabelDescriptions("decodes/resources/procmon", settings.language);
		evtTimeSdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		
		guiInit();
		pack();
		this.trackChanges("PlatformMonitorFrame");
	}
	
	private void guiInit()
	{
		this.setTitle(procmonLabels.getString("platmon.title"));
		platstatModel = new PlatformTableModel(this);
		JPanel mainPanel = (JPanel) this.getContentPane();
		mainPanel.setLayout(new BorderLayout());
		
		outerPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		mainPanel.add(outerPane, BorderLayout.CENTER);
		platstatTable = new SortingListTable(platstatModel, platstatModel.widths);
		JPanel psListPanel = new JPanel(new BorderLayout());
		JPanel p = new JPanel(new FlowLayout());
		p.add(new JLabel(procmonLabels.getString("platmon.header")));
		psListPanel.add(p, BorderLayout.NORTH);
		JScrollPane scrollPane = new JScrollPane(
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setViewportView(platstatTable);
		psListPanel.add(scrollPane, BorderLayout.CENTER);
		outerPane.setTopComponent(psListPanel);
		
		platEventsLabel.setText(
			LoadResourceBundle.sprintf(procmonLabels.getString("platmon.eventsHeader"), "", ""));
		p = new JPanel(new FlowLayout());
		p.add(platEventsLabel);
		eventsPanel.add(p, BorderLayout.NORTH);
		outerPane.setBottomComponent(eventsPanel);
		
		psListPanel.setPreferredSize(new Dimension(900, 300));
		eventsPanel.setPreferredSize(new Dimension(900, 300));
		
		JPanel buttonPanel = new JPanel(new FlowLayout());
		mainPanel.add(buttonPanel, BorderLayout.SOUTH);
		
		JButton closeButton = new JButton(genericLabels.getString("close"));
		closeButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					closePressed();
				}
			});
		buttonPanel.add(closeButton);
		
		platstatTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		platstatTable.getSelectionModel().addListSelectionListener(
			new ListSelectionListener()
			{
				@Override
				public void valueChanged(ListSelectionEvent e)
				{
					platformSelected();
				}
			});
	}
	
	protected synchronized void platformSelected()
	{
		if (inDbUpdate)
			return;

		int sel = platstatTable.getSelectedRow();
		if (sel == -1)
			return;
		else
		{
			PlatformStatus ps = (PlatformStatus)platstatModel.getRowObject(sel);
			if (ps != selectedPS)
			{
				selectedPS = ps;
				eventsPanel.clear();
				evtList.clear();
				platEventsLabel.setText(
					LoadResourceBundle.sprintf(procmonLabels.getString("platmon.eventsHeader"), 
						selectedPS.getPlatformName()));
			}

			fillEventsFor(ps);
		}
	}


	protected void closePressed()
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
		final PlatformMonitorFrame myframe = this;
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

	public synchronized void addEvent(String event)
	{
		eventsPanel.addLine(event);
	}
	

	private void fillEventsFor(PlatformStatus ps)
	{
		// Use DAO to get events for selected ses and fill events panel.
		DatabaseIO dbio = Database.getDb().getDbIo();
		if (dbio instanceof SqlDatabaseIO)
		{
			DacqEventDAI evtDAO = ((SqlDatabaseIO)dbio).makeDacqEventDAO();
			try
			{
				int sz = evtList.size();
				evtDAO.readEventsForPlatform(ps.getKey(), evtList);
				while(sz < evtList.size())
				{
					DacqEvent evt = evtList.get(sz++);
					eventsPanel.addLine(formatEvt(evt));
				}
			}
			catch (DbIoException ex)
			{
				Logger.instance().warning("Error reading events: " + ex);
			}
			finally
			{
				evtDAO.close();
			}
		}
	}

	private String formatEvt(DacqEvent evt)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(Logger.priorityName[evt.getEventPriority()] + " ");
		sb.append(evtTimeSdf.format(evt.getEventTime()) + " ");
		if (evt.getSubsystem() != null)
			sb.append("(" + evt.getSubsystem() + ") ");
		sb.append(evt.getEventText());
		
		return sb.toString();
	}

	/**
	 * Called from DbUpdateThread periodically when a new update has been
	 * read from the database. Merge the info into the model and update
	 * the screen in the Swing thread.
	 * @param psList
	 * @param seList 
	 * @param seList 
	 */
	public synchronized void updateFromDb(ArrayList<PlatformStatus> psList)
	{
		platstatModel.merge(psList);

		final int selectedPSIdx = platstatModel.indexOf(selectedPS);
		if (selectedPSIdx == -1)
		{
			// Either there is no selection, or a previous selection was removed
			// from the database.
			selectedPS = null;
		}

		SwingUtilities.invokeLater(
			new Runnable()
			{
				@Override
				public void run()
				{
					inDbUpdate = true;
					platstatModel.updated();
					if (selectedPSIdx != -1 && selectedPSIdx != platstatTable.getSelectedRow())
					{
						platstatTable.setRowSelectionInterval(selectedPSIdx, selectedPSIdx);
					}
					if (selectedPS != null)
						fillEventsFor(selectedPS);
					inDbUpdate = false;
				}
			});
	}
	
	public PlatformStatus getselectedPS()
	{
		return selectedPS;
	}

	public void setDefaults()
	{
		SwingUtilities.invokeLater(
			new Runnable()
			{
				@Override
				public void run()
				{
					outerPane.setDividerLocation(.4);
				}
			});

	}
}

