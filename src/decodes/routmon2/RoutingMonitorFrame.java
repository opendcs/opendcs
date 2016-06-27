/**
 * $Id$
 * 
 * Open Source Software
 * 
 * $Log$
 * Revision 1.3  2016/02/04 18:50:07  mmaloney
 * In start(), account for backslash in windows filenames.
 *
 * Revision 1.2  2015/06/04 21:37:39  mmaloney
 * Added control buttons to process monitor GUI.
 *
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 * Revision 1.8  2013/03/25 17:50:54  mmaloney
 * dev
 *
 * Revision 1.7  2013/03/25 17:13:11  mmaloney
 * dev
 *
 * Revision 1.6  2013/03/25 16:58:38  mmaloney
 * dev
 *
 * Revision 1.5  2013/03/25 15:02:20  mmaloney
 * dev
 *
 * Revision 1.4  2013/03/23 18:20:04  mmaloney
 * dev
 *
 * Revision 1.3  2013/03/23 18:01:03  mmaloney
 * dev
 *
 * Revision 1.2  2013/03/23 15:33:55  mmaloney
 * dev
 *
 * Revision 1.1  2013/03/21 18:27:40  mmaloney
 * DbKey Implementation
 *
 */
package decodes.routmon2;

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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.TimeZone;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import opendcs.dai.ComputationDAI;
import opendcs.dai.DacqEventDAI;
import opendcs.dai.LoadingAppDAI;
import opendcs.dai.ScheduleEntryDAI;
import decodes.db.Database;
import decodes.db.DatabaseIO;
import decodes.db.ScheduleEntry;
import decodes.db.ScheduleEntryStatus;
import decodes.gui.SortingListTable;
import decodes.gui.TopFrame;
import decodes.polling.DacqEvent;
import decodes.sql.SqlDatabaseIO;
import decodes.tsdb.CompAppInfo;
import decodes.tsdb.CompFilter;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesDb;
import decodes.util.DecodesSettings;


/**
 * Main frame for process status monitor GUI
 * @author mmaloney Mike Maloney, Cove Software, LLC
 */
@SuppressWarnings("serial")
public class RoutingMonitorFrame 
	extends TopFrame 
	//implements TableModelListener, ListSelectionListener
{
	private RoutingTableModel routingModel = null;
	private EventsPanel eventsPanel = new EventsPanel();
	private JSplitPane outerPane = null, innerPane = null;
	static ResourceBundle genericLabels = null;
	static ResourceBundle procmonLabels = null;
	static ResourceBundle compeditLabels = null;
	private SortingListTable routingSpecTable = null;
	private RsRunModel rsRunModel = null;
	private JTable rsRunTable = null;
	private RSBean selectedRS = null;
//	private TimeSeriesDb tsdb = null;
//	private DbPollThread dbPollThread = null;
	private ArrayList<DacqEvent> evtList = new ArrayList<DacqEvent>();
	private ScheduleEntryStatus selectedSES = null;
	private SimpleDateFormat evtTimeSdf = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
	private JLabel rsRunLabel = new JLabel();
	private JLabel runEventsLabel = new JLabel();
	private DbPollThread dbPollThread;
	
	/**
	 * Constructor
	 */
	public RoutingMonitorFrame()
	{
		DecodesSettings settings = DecodesSettings.instance();
		genericLabels = LoadResourceBundle.getLabelDescriptions("decodes/resources/generic", settings.language);
		procmonLabels = LoadResourceBundle.getLabelDescriptions("decodes/resources/procmon", settings.language);
		compeditLabels = LoadResourceBundle.getLabelDescriptions("decodes/resources/compedit", settings.language);
		evtTimeSdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		
		guiInit();
		pack();
System.out.println("Packed");
		this.trackChanges("RoutingMonitorFrame");
		SwingUtilities.invokeLater(
			new Runnable()
			{
				@Override
				public void run()
				{
					outerPane.setDividerLocation(outerPane.getHeight()*2/3);
				}
			});
	}
	
	private void guiInit()
	{
		this.setTitle(procmonLabels.getString("routingFrameTitle"));
		routingModel = new RoutingTableModel(this);
		JPanel mainPanel = (JPanel) this.getContentPane();
		mainPanel.setLayout(new BorderLayout());
		
		outerPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		mainPanel.add(outerPane, BorderLayout.CENTER);
		routingSpecTable = new SortingListTable(routingModel, routingModel.widths);
		JPanel procListPanel = new JPanel(new BorderLayout());
		JPanel p = new JPanel(new FlowLayout());
		p.add(new JLabel(procmonLabels.getString("rsPanelHeader")));
		procListPanel.add(p, BorderLayout.NORTH);
		JScrollPane scrollPane = new JScrollPane(
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setViewportView(routingSpecTable);
		procListPanel.add(scrollPane, BorderLayout.CENTER);
		outerPane.setTopComponent(procListPanel);
		innerPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		outerPane.setBottomComponent(innerPane);
		
		rsRunModel = new RsRunModel(this);
		rsRunTable = new JTable(rsRunModel);
		JScrollPane rsRunScrollPane = new JScrollPane(
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		rsRunScrollPane.setViewportView(rsRunTable);
		JPanel rsRunTablePanel = new JPanel(new BorderLayout());
		rsRunLabel.setText(LoadResourceBundle.sprintf(procmonLabels.getString("rsRunPanelHeader"), ""));
		p = new JPanel(new FlowLayout());
		p.add(rsRunLabel);
		rsRunTablePanel.add(p, BorderLayout.NORTH);
		rsRunTablePanel.add(rsRunScrollPane, BorderLayout.CENTER);
		innerPane.setTopComponent(rsRunTablePanel);

		runEventsLabel.setText(
			LoadResourceBundle.sprintf(procmonLabels.getString("runEventsHeader"), "", ""));
		p = new JPanel(new FlowLayout());
		p.add(runEventsLabel);
		eventsPanel.add(p, BorderLayout.NORTH);
		innerPane.setBottomComponent(eventsPanel);
		scrollPane.setPreferredSize(new Dimension(900, 300));
		rsRunScrollPane.setPreferredSize(new Dimension(900,300));
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
		
		routingSpecTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		routingSpecTable.getSelectionModel().addListSelectionListener(
			new ListSelectionListener()
			{
				@Override
				public void valueChanged(ListSelectionEvent e)
				{
					routingSpecSelected();
				}
			});
		rsRunTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		rsRunTable.getSelectionModel().addListSelectionListener(
			new ListSelectionListener()
			{
				@Override
				public void valueChanged(ListSelectionEvent e)
				{
					runSelected();
				}
			});
	}
	
	protected void runSelected()
	{
System.out.println("RMF runSelected");
		int sel = rsRunTable.getSelectedRow();
		if (sel == -1 || selectedRS == null)
			return;
		else
		{
			ScheduleEntryStatus ses = selectedRS.getRunHistory().get(sel);
			if (ses != selectedSES)
			{
				eventsPanel.clear();
				evtList.clear();
				selectedSES = ses;
				rsRunLabel.setText(
					LoadResourceBundle.sprintf(procmonLabels.getString("runEventsHeader"), 
					selectedRS.getRsName(), evtTimeSdf.format(selectedSES.getRunStart())));
			}

			fillEventsFor(ses);
		}
	}

	protected void routingSpecSelected()
	{
System.out.println("RMF routingSpecSelected");
		int sel = routingSpecTable.getSelectedRow();
		if (sel == -1)
			return;
		else
		{
			RSBean rsb = routingModel.getRSAt(sel);
			if (rsb != selectedRS)
			{
				rsRunModel.setActiveRS(selectedRS = rsb);
				eventsPanel.clear();
				rsRunLabel.setText(
					LoadResourceBundle.sprintf(procmonLabels.getString("rsRunPanelHeader"), 
					selectedRS.getRsName()));
				dbPollThread.updateNow();
			}
		}
	}

	protected void closePressed()
	{
System.out.println("closePressed");
		// TODO Auto-generated method stub
		
	}

	public void cleanupBeforeExit()
	{
	}
	
	
	public void launch( int x, int y, int w, int h )
	{
		setBounds(x,y,w,h);
		setVisible(true);

		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		final RoutingMonitorFrame myframe = this;
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
	

//	@Override
//	public void tableChanged(TableModelEvent e)
//	{
//		if (selectedRS == null)
//			;
//		System.out.println("RoutingSpecFrame.tableChanged");
////		else
////		{
////			final int selidx = model.getAppNameIndex(selectedProc.getCompAppInfo().getAppName());
////			if (selidx != -1)
////				SwingUtilities.invokeLater(
////					new Runnable()
////					{
////						public void run() {	processTable.setRowSelectionInterval(selidx, selidx); }
////					});
////		}	
//	}

	
	private void fillEventsFor(ScheduleEntryStatus ses)
	{
System.out.println("fillEventsFor");
		// Use DAO to get events for selected ses and fill events panel.
		DatabaseIO dbio = Database.getDb().getDbIo();
		if (dbio instanceof SqlDatabaseIO)
		{
			DacqEventDAI evtDAO = ((SqlDatabaseIO)dbio).makeDacqEventDAO();
			try
			{
				int sz = evtList.size();
				evtDAO.readEventsForScheduleStatus(ses.getKey(), evtList);
				while(sz < evtList.size())
				{
					DacqEvent evt = evtList.get(sz++);
					eventsPanel.addLine(formatEvt(evt));
				}
			}
			catch (DbIoException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
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
		
//			+ ", schedEntryStatId=" + scheduleEntryStatusId
//			+ ", platformId=" + platformId
//			+ ", msgTime=" + msgRecvTime

		return sb.toString();
	}

	public void setDbPollThread(DbPollThread dbPollThread)
	{
		this.dbPollThread = dbPollThread;
	}

	public RoutingTableModel getRoutingModel()
	{
		return routingModel;
	}
	
	public RsRunModel getRsRunModel() { return rsRunModel; }

	/**
	 * Called from DbUpdateThread after a new list has been merged to the display.
	 * Check to make sure that active RS (if there is one) is still in the list.
	 * @param seList the new list just read from the database.
	 */
	public void checkActiveRS()
	{
		if (selectedRS == null)
			return;
		for(RSBean bean : routingModel.getBeans())
			if (selectedRS == bean)
				return;
		// Fell through means that the selected RS no longer exists in database.
		rsRunModel.setActiveRS(selectedRS = null);
		eventsPanel.clear();
	}

	public RSBean getSelectedRS()
	{
		return selectedRS;
	}
}

