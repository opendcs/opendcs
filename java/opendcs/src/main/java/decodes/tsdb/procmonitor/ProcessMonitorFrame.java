/**
 * $Id$
 * 
 * Open Source Software
 * 
 * $Log$
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
package decodes.tsdb.procmonitor;

import ilex.gui.EventsPanel;
import ilex.util.EnvExpander;
import ilex.util.LoadResourceBundle;
import ilex.util.Logger;
import ilex.util.ProcWaiterThread;

import java.awt.BorderLayout;
import java.awt.Dimension;
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
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
public class ProcessMonitorFrame 
	extends TopFrame implements TableModelListener, ListSelectionListener
{
	private ProcStatTableModel model = null;
	private EventsPanel eventsPanel = new EventsPanel();
	private JSplitPane splitPane = null;
	private static ResourceBundle genericLabels = null;
	private static ResourceBundle procmonLabels = null;
	private static ResourceBundle compeditLabels = null;
	private SortingListTable processTable = null;
	private AppInfoStatus selectedProc = null;
	private ArrayList<ProcessEditDialog> editDialogs = new ArrayList<ProcessEditDialog>();
	private TimeSeriesDb tsdb = null;
	private DbPollThread dbPollThread = null;
	
	/**
	 * Constructor
	 */
	public ProcessMonitorFrame()
	{
		DecodesSettings settings = DecodesSettings.instance();
		genericLabels = LoadResourceBundle.getLabelDescriptions("decodes/resources/generic", settings.language);
		procmonLabels = LoadResourceBundle.getLabelDescriptions("decodes/resources/procmon", settings.language);
		compeditLabels = LoadResourceBundle.getLabelDescriptions("decodes/resources/compedit", settings.language);

		guiInit();
		pack();
		this.trackChanges("ProcessMonitorFrame");
		SwingUtilities.invokeLater(
			new Runnable()
			{
				@Override
				public void run()
				{
					splitPane.setDividerLocation(splitPane.getHeight()*3/4);
				}
			});
	}
	
	private void guiInit()
	{
		this.setTitle("Process Monitor");
		model = new ProcStatTableModel(this);
		JPanel mainPanel = (JPanel) this.getContentPane();
		mainPanel.setLayout(new BorderLayout());
		mainPanel.add(new JLabel(procmonLabels.getString("frameTitle")), BorderLayout.NORTH);
		splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		mainPanel.add(splitPane, BorderLayout.CENTER);
		processTable = new SortingListTable(model, model.widths);
		JPanel procListPanel = new JPanel(new BorderLayout());
		JScrollPane scrollPane = new JScrollPane(
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setViewportView(processTable);
		procListPanel.add(scrollPane, BorderLayout.CENTER);
		splitPane.setTopComponent(procListPanel);
		splitPane.setBottomComponent(eventsPanel);
		scrollPane.setPreferredSize(new Dimension(900, 300));
		eventsPanel.setPreferredSize(new Dimension(900, 300));
		
		JPanel buttonPanel = new JPanel(new GridBagLayout());
		procListPanel.add(buttonPanel, BorderLayout.EAST);
		
		JButton startButton = new JButton(genericLabels.getString("start"));
		startButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					startPressed();
				}
			});
		buttonPanel.add(startButton,
			new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(2, 5, 2, 5), 0, 0));
		
		JButton stopButton = new JButton(genericLabels.getString("stop"));
		stopButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					stopPressed();
				}
			});
		buttonPanel.add(stopButton,
			new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(2, 5, 2, 5), 0, 0));


		JButton editButton = new JButton(genericLabels.getString("edit"));
		editButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					editPressed();
				}
			});
		buttonPanel.add(editButton,
			new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(2, 5, 2, 5), 0, 0));

		JButton newButton = new JButton(genericLabels.getString("new"));
		newButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					newPressed();
				}
			});
		buttonPanel.add(newButton,
			new GridBagConstraints(0, 3, 1, 1, 1.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(2, 5, 2, 5), 0, 0));


		JButton deleteButton = new JButton(genericLabels.getString("delete"));
		deleteButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					deletePressed();
				}
			});
		buttonPanel.add(deleteButton,
			new GridBagConstraints(0, 4, 1, 1, 1.0, 1.0,
				GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
				new Insets(2, 5, 2, 5), 0, 0));
		
		processTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		processTable.getSelectionModel().addListSelectionListener(this);
		model.addTableModelListener(this);
		
		processTable.addMouseListener(
			new MouseAdapter()
			{
				public void mouseClicked(MouseEvent e)
				{
					if (e.getClickCount() == 2)
						editPressed();
				}
			});
	}
	
	protected void deletePressed()
	{
		AppInfoStatus sel = getSelectedProc();
		if (sel == null)
		{
			showError(procmonLabels.getString("selectThenDelete"));
			return;
		}
		if (sel.getCompLock() != null && !sel.getCompLock().isStale())
		{
			showError(procmonLabels.getString("deleteRunning"));
			return;
		}
		String appType = sel.getCompAppInfo().getProperty("appType");
		
		// If this is a compproc, make sure there are no comps assigned to it.
		if (appType != null && appType.equalsIgnoreCase("ComputationProcess") && tsdb != null)
		{
			ComputationDAI computationDAO = tsdb.makeComputationDAO();
			try
			{
				CompFilter compFilter = new CompFilter();
				compFilter.setProcessId(sel.getCompAppInfo().getAppId());
				if (computationDAO.listCompsForGUI(compFilter).size() > 0)
				{
					showError(LoadResourceBundle.sprintf(procmonLabels.getString("cannotDelete_existingComps"), 
						sel.getCompAppInfo().getAppName()));
					return;
				}
			}
			catch(Exception ex) {}
			finally { computationDAO.close(); }
		}
		else if (appType != null && appType.equalsIgnoreCase("RoutingScheduler"))
		{
			ScheduleEntryDAI seDAO = Database.getDb().getDbIo().makeScheduleEntryDAO();
			try
			{
				if (seDAO.listScheduleEntries(sel.getCompAppInfo()).size() > 0)
				{
					showError(LoadResourceBundle.sprintf(procmonLabels.getString("cannotDelete_existingSched"), 
						sel.getCompAppInfo().getAppName()));
					return;
				}
			}
			catch(Exception ex) {}
			finally { seDAO.close(); }
		}
		if (showConfirm("Confirm", 
			LoadResourceBundle.sprintf(procmonLabels.getString("confirmDelete"), sel.getCompAppInfo().getAppName()), 
			JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
		{
			LoadingAppDAI laDAO = Database.getDb().getDbIo().makeLoadingAppDAO();
			try
			{
				laDAO.deleteComputationApp(sel.getCompAppInfo());
				dbPollThread.pollNow();
			}
			catch(Exception ex)
			{
				showError(LoadResourceBundle.sprintf(procmonLabels.getString("deleteError"), ex.toString()));
				return;
			}
			finally { laDAO.close(); }
		}
	}

	protected void newPressed()
	{
	    String newName = JOptionPane.showInputDialog(compeditLabels.getString("ProcessListPanel.NewInput"));
		if (newName == null)
			return;
		LoadingAppDAI loadingAppDAO = decodes.db.Database.getDb().getDbIo().makeLoadingAppDAO();
		try
		{
			if (loadingAppDAO.getComputationApp(newName) != null)
			{
				showError(compeditLabels.getString("ProcessListPanel.NewError"));
				return;
			}
		}
		catch(NoSuchObjectException ex)
		{
			// Ok: It means there is no match for newName.
		}
		catch (DbIoException ex)
		{
			String msg = LoadResourceBundle.sprintf(procmonLabels.getString("cannotRead"), ex.toString());
			Logger.instance().warning(msg);
			System.err.println(msg);
			ex.printStackTrace(System.err);
		}
		finally
		{
			loadingAppDAO.close();
		}

		CompAppInfo cai = new CompAppInfo();
		cai.setAppName(newName);
		
		ProcessEditDialog dlg = new ProcessEditDialog(this, 
			LoadResourceBundle.sprintf(procmonLabels.getString("editProcDialog"), "New Process"));
		dlg.setEditedObject(cai);
		editDialogs.add(dlg);
		launchDialog(dlg);
	}

	protected void editPressed()
	{
		AppInfoStatus sel = getSelectedProc();
		if (sel == null)
		{
			this.showError(procmonLabels.getString("selectThenEdit"));
			return;
		}
		for(Iterator<ProcessEditDialog> pedit = editDialogs.iterator(); pedit.hasNext(); )
		{
			ProcessEditDialog dlg = pedit.next();
			if (dlg.getEditedObject() == sel.getCompAppInfo())
			{
				dlg.toFront();
				return;
			}
		}
		ProcessEditDialog dlg = new ProcessEditDialog(this, 
			LoadResourceBundle.sprintf(procmonLabels.getString("editProcDialog"), sel.getCompAppInfo().getAppName()));
		dlg.setEditedObject(sel.getCompAppInfo());
		editDialogs.add(dlg);
		launchDialog(dlg);
	}

	protected void startPressed()
	{
		AppInfoStatus sel = getSelectedProc();
		if (sel == null)
		{
			this.showError(procmonLabels.getString("selectThenStart"));
			return;
		}
		String procName = sel.getCompAppInfo().getAppName();
		String startCmd = sel.getCompAppInfo().getProperty("startCmd");
		if (startCmd == null)
		{
			showError(procmonLabels.getString("noStartCmd"));
			return;
		}
		startCmd = EnvExpander.expand(startCmd);
		if (sel.getCompLock() != null && !sel.getCompLock().isStale())
		{
			showError(LoadResourceBundle.sprintf(
				procmonLabels.getString("alreadyRunning"), procName));
			return;
		}
		if (!sel.getCompAppInfo().canRunLocally())
		{
			showError(LoadResourceBundle.sprintf(
				procmonLabels.getString("notAllowed"), procName));
			return;
		}

		String ev = LoadResourceBundle.sprintf(procmonLabels.getString("startingProc"), procName,
			startCmd);
		Logger.instance().info(ev);
		addEvent(ev);
		try
		{
			ProcWaiterThread.runBackground(startCmd, procName);
		}
		catch (IOException ex)
		{
			// If it's windoze try to change slashes to backslash and tack on .bat
			if (System.getProperty("os.name").toLowerCase().startsWith("win"))
			{
				StringBuilder sb = new StringBuilder(startCmd);
				int exeEnd = sb.length();
				for(int idx = 0; idx < sb.length(); idx++)
				{
					char c = sb.charAt(idx);
					if (c == ' ')
					{
						exeEnd = idx;
						break;
					}
					else if (c == '/')
						sb.setCharAt(idx, '\\');
				}
				if (exeEnd > 4 && !sb.toString().toLowerCase().substring(0, exeEnd).endsWith(".bat"))
					sb.insert(exeEnd, ".bat");
				Logger.instance().info("Execution of '" + startCmd + "' failed. Windozified to '" + sb.toString() 
					+ "' and trying again...");
				startCmd = sb.toString();
				try
				{
					ProcWaiterThread.runBackground(startCmd, procName);
					return;
				}
				catch(IOException ex2) { ex = ex2; }
			}
			String msg = LoadResourceBundle.sprintf(procmonLabels.getString("cannotStart"), procName,
				sel.getCompAppInfo().getAppName(), ex.toString());
			Logger.instance().warning(msg);
			System.err.println(msg);
			ex.printStackTrace(System.err);
		}
	}
	
	private void stopPressed()
	{
		AppInfoStatus sel = getSelectedProc();
		if (sel == null)
		{
			this.showError(procmonLabels.getString("selectThenStop"));
			return;
		}
		String procName = sel.getCompAppInfo().getAppName();
		if (sel.getCompLock() == null || sel.getCompLock().isStale())
		{
			showError(LoadResourceBundle.sprintf(
				procmonLabels.getString("notRunning"), procName));
			return;
		}
		String q = 
			sel.getCompLock().isRunningLocally() ? 
				LoadResourceBundle.sprintf(procmonLabels.getString("verifyStop"), procName) :
				LoadResourceBundle.sprintf(procmonLabels.getString("notThisHost"), procName, 
					sel.getCompLock().getHost());
		int r = showConfirm(genericLabels.getString("confirm"), q, JOptionPane.YES_NO_OPTION);
		if (r != JOptionPane.YES_OPTION)
			return;

		String ev = LoadResourceBundle.sprintf(procmonLabels.getString("stoppingProc"), procName,
			sel.getCompAppInfo().getAppName());
		Logger.instance().info(ev);
		addEvent(ev);

		LoadingAppDAI loadingAppDAO = decodes.db.Database.getDb().getDbIo().makeLoadingAppDAO();
		try
		{
			loadingAppDAO.releaseCompProcLock(sel.getCompLock());
		}
		catch (DbIoException ex)
		{
			String msg = LoadResourceBundle.sprintf(procmonLabels.getString("cannotStop"), procName,
				sel.getCompAppInfo().getAppName(), ex.toString());
			Logger.instance().warning(msg);
			System.err.println(msg);
			ex.printStackTrace(System.err);
		}
		finally
		{
			loadingAppDAO.close();
		}
	}


	public void cleanupBeforeExit()
	{
	}
	
	
//	/**
//	 * @param args
//	 */
//	public static void main(String[] args)
//	{
//		ProcessMonitorFrame f = new ProcessMonitorFrame();
////		f.centerOnScreen();
//		Rectangle r = f.getBounds();
//		f.setExitOnClose(true);
//		f.launch(r.x, r.y, r.width, r.height);
//	}
	
	public void launch( int x, int y, int w, int h )
	{
		setBounds(x,y,w,h);
		setVisible(true);

		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		final ProcessMonitorFrame myframe = this;
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

	public ProcStatTableModel getModel() { return model; }
	
	public synchronized void addEvent(String event)
	{
		eventsPanel.addLine(event);
	}
	
	private AppInfoStatus getSelectedProc()
	{
		int idx = processTable.getSelectedRow();
		if (idx < 0)
			return null;
		return model.getAppAt(idx);
	}

	@Override
	public void tableChanged(TableModelEvent e)
	{
		// If a selection was made, re-make it.
		if (selectedProc == null)
			;
		else
		{
			final int selidx = model.getAppNameIndex(selectedProc.getCompAppInfo().getAppName());
			if (selidx != -1)
				SwingUtilities.invokeLater(
					new Runnable()
					{
						public void run() {	processTable.setRowSelectionInterval(selidx, selidx); }
					});
		}	
	}

	@Override
	public void valueChanged(ListSelectionEvent e)
	{
		// This is from the process table's list Selection Model.
		// It is called when the row selection on the table has changed.
		int sel = processTable.getSelectedRow();
		if (sel == -1)
			selectedProc = null;
		else
		{
			selectedProc = model.getAppAt(sel);
		}
	}
	
	public void dialogClosed(ProcessEditDialog dlg)
	{
		for(Iterator<ProcessEditDialog> pedit = editDialogs.iterator(); pedit.hasNext(); )
		{
			if (dlg == pedit.next())
			{
				pedit.remove();
				break;
			}
		}
		dbPollThread.pollNow();
	}

	public void setTsdb(TimeSeriesDb tsdb)
	{
		this.tsdb = tsdb;
	}

	public void setDbPollThread(DbPollThread dbPollThread)
	{
		this.dbPollThread = dbPollThread;
	}
}

