package decodes.dbeditor;

import ilex.util.LoadResourceBundle;
import ilex.util.Logger;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.ResourceBundle;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;

import opendcs.dai.ScheduleEntryDAI;

import decodes.db.Constants;
import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.ScheduleEntry;
import decodes.gui.SortingListTable;
import decodes.gui.SortingListTableModel;
import decodes.gui.TopFrame;
import decodes.tsdb.DbIoException;

/**
 * This JPanel contains a list of ScheduleEntry objects and allow the user to
 * open, new, copy, delete, and refresh.
 * 
 * @author mmaloney Mike Maloney Cove Software, LLC
 */
@SuppressWarnings("serial")
public class ScheduleListPanel extends JPanel implements ListOpsController
{
	private ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	private ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

	private JTable scheduleEntryTable;
	private ScheduleEntryTableModel tableModel;
	private DbEditorFrame parent;

	/** Constructor. */
	public ScheduleListPanel()
	{
		try
		{
			guiInit();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	/**
	 * Sets the parent frame object. Each list panel needs to know this.
	 * 
	 * @param parent
	 *            the DbEditorFrame
	 */
	void setParent(DbEditorFrame parent)
	{
		this.parent = parent;
	}

	/** Initializes GUI components. */
	private void guiInit() throws Exception
	{
		tableModel = new ScheduleEntryTableModel();
		scheduleEntryTable = new SortingListTable(tableModel, new int[]
		{ 20, 20, 20, 20, 20 });
		scheduleEntryTable.getSelectionModel().setSelectionMode(
			ListSelectionModel.SINGLE_SELECTION);
		scheduleEntryTable.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				if (e.getClickCount() == 2)
				{
					openPressed();
				}
			}
		});
		
		
		this.setLayout(new BorderLayout());
		JLabel titleLabel = new JLabel(
			dbeditLabels.getString("ScheduleEntryPanel.DefinedLists"));
		titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
		this.add(titleLabel, BorderLayout.NORTH);

		JPanel tablePanel = new JPanel(new BorderLayout());
		this.add(tablePanel, BorderLayout.CENTER);
		
		JScrollPane tableScrollPane = new JScrollPane();
		tablePanel.add(tableScrollPane, BorderLayout.CENTER);
		tableScrollPane.getViewport().add(scheduleEntryTable, null);

		this.add(new ListOpsPanel(this), BorderLayout.SOUTH);
	}

	private ScheduleEntry getSelection()
	{
		int idx = scheduleEntryTable.getSelectedRow();
		if (idx == -1)
			return null;
		return tableModel.getObjectAt(idx);
	}

	/** @return type of entity that this panel edits. */
	public String getEntityType()
	{
		return dbeditLabels.getString("ScheduleEntryPanel.EntityName");
	}

	/** Called when the 'Open' button is pressed. */
	public void openPressed()
	{
		ScheduleEntry se = getSelection();
		if (se == null)
			TopFrame.instance().showError(
				LoadResourceBundle.sprintf(
					dbeditLabels.getString("ScheduleEntryPanel.OpenNoSelection"),
					getEntityType()));
		else
			doOpen(se);
	}

	/** Called when the 'New' button is pressed. */
	public void newPressed()
	{
		String newName = JOptionPane.showInputDialog(
			LoadResourceBundle.sprintf(
				dbeditLabels.getString("ScheduleEntryPanel.PromptNewName"),
				getEntityType()));
		if (newName == null)
			return;

		if (findInDb(newName) != null)
		{
			TopFrame.instance().showError(
				LoadResourceBundle.sprintf(
					dbeditLabels.getString("ScheduleEntryPanel.NewAlreadyExists"),
					getEntityType()));
			return;
		}

		doOpen(new ScheduleEntry(newName));
	}

	private ScheduleEntry findInDb(String name)
	{
		for(ScheduleEntry se : Database.getDb().schedEntryList)
			if (se.getName().equalsIgnoreCase(name))
				return se;
		return null;
	}
	
	/**
	 * Make a copy of the passed object but with the new name
	 * @param se the object to copy
	 * @param newName the new name
	 * @return the copy
	 */
	private ScheduleEntry copy(ScheduleEntry se, String newName)
	{
		ScheduleEntry ret = new ScheduleEntry(newName);
		ret.setLoadingAppId(se.getLoadingAppId());
		ret.setRoutingSpecId(se.getRoutingSpecId());
		ret.setStartTime(se.getStartTime());
		ret.setTimezone(se.getTimezone());
		ret.setRunInterval(se.getRunInterval());
		ret.setEnabled(se.isEnabled());
		ret.setLoadingAppName(se.getLoadingAppName());
		ret.setRoutingSpecName(se.getRoutingSpecName());
		
		return ret;
	}
	
	@Override
	public void copyPressed()
	{
		ScheduleEntry se = getSelection();
		if (se == null)
		{
			TopFrame.instance().showError(
				LoadResourceBundle.sprintf(
					dbeditLabels.getString("ScheduleEntryPanel.CopyNoSelection"),
					getEntityType()));
			return;
		}
		String newName = JOptionPane.showInputDialog(dbeditLabels
			.getString("ScheduleEntryPanel.PromptCopyName"));
		if (newName == null)
			return;

		if (findInDb(newName) != null)
		{
			TopFrame.instance().showError(
				LoadResourceBundle.sprintf(
					dbeditLabels.getString("ScheduleEntryPanel.NewAlreadyExists"),
					getEntityType()));
			return;
		}

		ScheduleEntry ob = copy(se, newName);
		try
		{
			ob.write();
		}
		catch (DatabaseException ex)
		{
			TopFrame.instance().showError(
				LoadResourceBundle.sprintf(
					dbeditLabels.getString("ScheduleEntryPanel.SaveError"),
					getEntityType()) + " " + ex);
			return;
		}
		tableModel.add(ob);
		doOpen(ob);
	}
	
	@Override
	public void deletePressed()
	{
		ScheduleEntry ob = getSelection();
		if (ob == null)
		{
			TopFrame.instance().showError(
				LoadResourceBundle.sprintf(
					dbeditLabels.getString("ScheduleEntryPanel.DeleteNoSelection"),
					getEntityType()));
			return;
		}

		DbEditorTabbedPane dbtp = parent.getScheduleListTabbedPane();
		DbEditorTab tab = dbtp.findEditorFor(ob);
		if (tab != null)
		{
			TopFrame.instance().showError(
				LoadResourceBundle.sprintf(
					dbeditLabels.getString("ScheduleEntryPanel.DeleteWhileEditing"),
					getEntityType()));
			return;
		}
		int r = JOptionPane.showConfirmDialog(this,
			LoadResourceBundle.sprintf(
				genericLabels.getString("confirmDelete"), getEntityType()),
			genericLabels.getString("confirm"),
			JOptionPane.YES_NO_OPTION);
		if (r == JOptionPane.YES_OPTION)
			tableModel.deleteObject(ob);
	}

	@Override
	public void refreshPressed()
	{
		tableModel.refill();
	}

	/**
	 * Opens an object in an Edit Panel.
	 * @param se the object to be edited.
	 */
	private void doOpen(ScheduleEntry se)
	{
		DbEditorTabbedPane dbtp = parent.getScheduleListTabbedPane();
		DbEditorTab tab = dbtp.findEditorFor(se);
		if (tab != null)
			dbtp.setSelectedComponent(tab);
		else
		{
			ScheduleEntryEditPanel newTab = new ScheduleEntryEditPanel(parent, se);
			String title = se.getName();
			dbtp.add(title, newTab);
			dbtp.setSelectedComponent(newTab);
		}
	}

	/** Resorts this list of network list names. */
	public void resort()
	{
		this.tableModel.resort();
	}
	
	/**
	 * Called from the edit panel after a schedule entry is saved.
	 * @param se the schedule entry just saved.
	 */
	public void addScheduleEntry(ScheduleEntry se)
	{
		tableModel.add(se);
	}

}

@SuppressWarnings("serial")
class ScheduleEntryTableModel extends AbstractTableModel implements
	SortingListTableModel
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

	static String columnNames[] =
	{
		dbeditLabels.getString("ScheduleEntryPanel.TableColumn1"),
		dbeditLabels.getString("ScheduleEntryPanel.TableColumn2"),
		dbeditLabels.getString("ScheduleEntryPanel.TableColumn3"),
		dbeditLabels.getString("ScheduleEntryPanel.TableColumn4"),
		dbeditLabels.getString("ScheduleEntryPanel.TableColumn5")
	};
	private int lastSortColumn = -1;
	private ArrayList<ScheduleEntry> theList = new ArrayList<ScheduleEntry>();

	public ScheduleEntryTableModel() 
	{
		super();
		refill();
		this.sortByColumn(0);
	}

	void refill() 
	{
		ScheduleEntryDAI scheduleEntryDAO = Database.getDb().getDbIo().makeScheduleEntryDAO();
		if (scheduleEntryDAO == null)
		{
			Logger.instance().debug1("Cannot write schedule entries. Not supported on this database.");
			return;
		}

		try
		{
			theList.clear();
			ArrayList<ScheduleEntry> sea = scheduleEntryDAO.listScheduleEntries(null);
			for(Iterator<ScheduleEntry> seit = sea.iterator(); seit.hasNext(); )
			{
				ScheduleEntry se = seit.next();
				if (se.getName().toLowerCase().endsWith("-manual"))
					seit.remove();
			}
			theList.addAll(sea);
			resort();
		}
		catch(DbIoException ex)
		{
			TopFrame.instance().showError(
				LoadResourceBundle.sprintf(
					dbeditLabels.getString("ScheduleEntryPanel.CannotLoadError"),
						dbeditLabels.getString("ScheduleEntryPanel.EntityName"), ex));
		}
		finally
		{
			scheduleEntryDAO.close();
		}
	}

	public int getRowCount()
	{
		return theList.size();
	}

	public int getColumnCount()
	{
		return columnNames.length;
	}

	public String getColumnName(int col)
	{
		return columnNames[col];
	}

	public ScheduleEntry getObjectAt(int r)
	{
		return (ScheduleEntry) getRowObject(r);
	}

	public Object getRowObject(int r)
	{
		if (r >= 0 && r < getRowCount())
			return theList.get(r);
		else
			return null;
	}

	void add(ScheduleEntry ob)
	{
		for(Iterator<ScheduleEntry> seit = theList.iterator(); seit.hasNext(); )
		{
			ScheduleEntry se = seit.next();
			if (ob.getName().equalsIgnoreCase(se.getName()))
			{
				seit.remove();
				break;
			}
		}
		theList.add(ob);
		fireTableDataChanged();
	}

	void deleteObject(ScheduleEntry ob)
	{
		ScheduleEntryDAI scheduleEntryDAO = Database.getDb().getDbIo().makeScheduleEntryDAO();
		if (scheduleEntryDAO == null)
		{
			Logger.instance().debug1("Cannot delete schedule entry. Not supported on this database.");
			return;
		}

		try
		{
			scheduleEntryDAO.deleteScheduleEntry(ob);
			theList.remove(ob);
			resort();
		}
		catch(DbIoException ex)
		{
			TopFrame.instance().showError(
				LoadResourceBundle.sprintf(
					dbeditLabels.getString("ScheduleEntryPanel.CannotDeleteError"), ex));
		}
		finally
		{
			scheduleEntryDAO.close();
		}

		fireTableDataChanged();
	}

	public Object getValueAt(int r, int c)
	{
		ScheduleEntry ob = getObjectAt(r);
		if (ob == null)
			return "";
		else
			return getSEColumn(ob, c);
	}

	public static String getSEColumn(ScheduleEntry ob, int c)
	{
		switch (c)
		{
		case 0: return ob.getName();
		case 1: return ob.getLoadingAppName();
		case 2: return ob.getRoutingSpecName();
		case 3: return ob.isEnabled() ?
			genericLabels.getString("yes") : genericLabels.getString("no");
		case 4: return ob.getLastModified() == null ? "" : 
			Constants.defaultDateFormat.format(ob.getLastModified());
		default:
			return "";
		}

	}

	public void resort()
	{
		if (lastSortColumn != -1)
			sortByColumn(lastSortColumn);
	}

	public void sortByColumn(int c)
	{
		lastSortColumn = c;
		Collections.sort(theList, new ScheduleEntryComparator(c));
		fireTableDataChanged();
	}

	void replace(ScheduleEntry oldOb, ScheduleEntry newOb)
	{
		theList.remove(oldOb);
		theList.add(newOb);
		if (lastSortColumn != -1)
			sortByColumn(lastSortColumn);
		else
			fireTableDataChanged();
	}
}

class ScheduleEntryComparator implements Comparator<ScheduleEntry>
{
	int column;

	public ScheduleEntryComparator(int column)
	{
		this.column = column;
	}

	/**
	 * Compare the eqMod names of the specified type.
	 */
	public int compare(ScheduleEntry ds1, ScheduleEntry ds2)
	{
		if (ds1 == ds2)
			return 0;
		String s1 = ScheduleEntryTableModel.getSEColumn(ds1, column);
		String s2 = ScheduleEntryTableModel.getSEColumn(ds2, column);

		return s1.compareToIgnoreCase(s2);
	}

	public boolean equals(Object ob)
	{
		return false;
	}
}
