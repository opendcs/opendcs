/*
*  $Id$
*  
*  Open Source Software
*  
*  $Log$
*  Revision 1.5  2013/03/21 18:27:40  mmaloney
*  DbKey Implementation
*
*/
package decodes.dbeditor;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;

import ilex.util.PropertiesUtil;
import decodes.gui.*;
import java.util.Comparator;
import java.util.Collections;
import java.util.Iterator;
import java.util.ResourceBundle;

import ilex.util.LoadResourceBundle;

import decodes.db.DatabaseObject;
import decodes.db.DatabaseException;
import decodes.db.Database;
import decodes.db.DataSource;
import decodes.db.DataSourceList;
import decodes.db.RoutingSpec;
import decodes.db.Constants;

/**
DBEDIT panel containing sorting list of data sources.
*/
public class SourcesListPanel extends JPanel
	implements ListOpsController
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

	BorderLayout borderLayout1 = new BorderLayout();
	ListOpsPanel sourcesListOpsPanel = new ListOpsPanel(this);
	JLabel jLabel1 = new JLabel();
	JScrollPane jScrollPane1 = new JScrollPane();
	JTable dataSourceListTable;
	DataSourceListTableModel tableModel;
	DbEditorFrame parent;

	/** Constructor. */
	public SourcesListPanel()
	{
		try
		{
			tableModel = new DataSourceListTableModel(this);
			dataSourceListTable = new SortingListTable(tableModel,
				new int[] { 25, 15, 50, 10 });
	 		dataSourceListTable.getSelectionModel().setSelectionMode(
		    	ListSelectionModel.SINGLE_SELECTION);

			dataSourceListTable.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e){
					if (e.getClickCount() == 2){
		       				openPressed();
					}
				}
			} );
			jbInit();
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	  Sets the parent frame object. Each list panel needs to know this.
	  @param parent the DbEditorFrame
	*/
	void setParent(DbEditorFrame parent)
	{
		this.parent = parent;
	}

	/** Initializes GUI components. */
    private void jbInit() throws Exception {
        this.setLayout(borderLayout1);
        jLabel1.setHorizontalAlignment(SwingConstants.CENTER);
        jLabel1.setText(
			dbeditLabels.getString("SourcesListPanel.title"));
        this.add(sourcesListOpsPanel, BorderLayout.SOUTH);
        this.add(jLabel1, BorderLayout.NORTH);
        this.add(jScrollPane1, BorderLayout.CENTER);
        jScrollPane1.getViewport().add(dataSourceListTable, null);
    }

	/** @return type of entity that this panel edits. */
	public String getEntityType() 
	{ 
		return dbeditLabels.getString("ListPanel.dataSourceEntity"); 
	}

	/** @return currently selected DataSource */
	private DataSource getSelection()
	{
		int idx = dataSourceListTable.getSelectedRow();
		if (idx == -1)
			return null;
	    return tableModel.getObjectAt(idx);
	}

	/** Called when the 'Open' button is pressed. */
	public void openPressed()
	{
		DataSource ds = getSelection();
		if (ds == null)
			TopFrame.instance().showError(
				LoadResourceBundle.sprintf(
					dbeditLabels.getString("ListPanel.selectOpen"),
					getEntityType()));
		else
		    doOpen(ds);
	}

	/** Called when the 'New' button is pressed. */
	public void newPressed()
	{
		String newName = JOptionPane.showInputDialog(
		LoadResourceBundle.sprintf(
		dbeditLabels.getString("ListPanel.enterNewName"),
		getEntityType()));
		if (newName == null)
			return;
		if (Database.getDb().dataSourceList.get(newName) != null)
		{
			TopFrame.instance().showError(
				LoadResourceBundle.sprintf(
					dbeditLabels.getString("ListPanel.alreadyExists"),
					getEntityType()));
			return;
		}

		DataSource ob = new DataSource(newName, "LRGS");
		Database.getDb().dataSourceList.add(ob);
		tableModel.fireTableDataChanged();
		doOpen(ob);
	}

	/** Called when the 'Copy' button is pressed. */
	public void copyPressed()
	{
		DataSource ds = getSelection();
		if (ds == null)
		{
			TopFrame.instance().showError(
				LoadResourceBundle.sprintf(
					dbeditLabels.getString("ListPanel.selectCopy"),
					getEntityType()));
			return;
		}
		String newName = JOptionPane.showInputDialog(
			dbeditLabels.getString("ListPanel.enterCopyName"));
		if (newName == null)
			return;

		if (Database.getDb().dataSourceList.get(newName) != null)
		{
			TopFrame.instance().showError(
				LoadResourceBundle.sprintf(
					dbeditLabels.getString("ListPanel.alreadyExists"),
					getEntityType()));
			return;
		}

		DataSource ob = ds.copy();
		ob.clearId();
		ob.setName(newName);
		this.tableModel.addDataSource(ob);
		doOpen(ob);
	}

	/** Called when the 'Delete' button is pressed. */
	public void deletePressed()
	{
		DataSource ds = getSelection();
		if (ds == null)
		{
			TopFrame.instance().showError(
				LoadResourceBundle.sprintf(
					dbeditLabels.getString("ListPanel.selectDelete"),
					getEntityType()));
			return;
		}

		// Don't allow delete if this source is being used by a routing spec.
		ds.countUsedBy();
		if (ds.numUsedBy > 0)
		{
			TopFrame.instance().showError(
				LoadResourceBundle.sprintf(
					dbeditLabels.getString("SourcesListPanel.beingUsed"),
					ds.numUsedBy));
			return;
		}

		// Also don't allow delete if this source is a group member of 
		// another data source.
		for(Iterator it = Database.getDb().dataSourceList.iterator();
			it.hasNext(); )
		{
			DataSource x = (DataSource)it.next();
			if (x == ds)
				continue;
			if (x.isInGroup(ds.getName()))
			{
				TopFrame.instance().showError(
					LoadResourceBundle.sprintf(
						dbeditLabels.getString("SourcesListPanel.isGroupMember"),
						x.getName()));
				return;
			}
		}

		DbEditorTabbedPane sourcesTabbedPane = parent.getSourcesTabbedPane();
		DbEditorTab tab = sourcesTabbedPane.findEditorFor(ds);
		if (tab != null)
		{
			TopFrame.instance().showError(
				LoadResourceBundle.sprintf(
					dbeditLabels.getString("ListPanel.beingEdited"),
					getEntityType()));
			return;
		}
		int r = JOptionPane.showConfirmDialog(this,
			LoadResourceBundle.sprintf(
				dbeditLabels.getString("ListPanel.confirmDeleteMsg"),
				getEntityType()),
			dbeditLabels.getString("ListPanel.confirmDeleteTitle"),
			JOptionPane.YES_NO_OPTION);
		if (r == JOptionPane.YES_OPTION)
			tableModel.deleteObject(ds);
	}

	/** Called when the 'Help' button is pressed. */
	public void refreshPressed()
	{
	}

	/**
	  Opens the data source in a new edit panel.
	  @param ds the data source to open. 
	*/
	private void doOpen(DataSource ds)
	{
		DbEditorTabbedPane dbtp = parent.getSourcesTabbedPane();
		DbEditorTab tab = dbtp.findEditorFor(ds);
		if (tab != null)
			dbtp.setSelectedComponent(tab);
		else
		{
			SourceEditPanel newTab = new SourceEditPanel(ds);
			newTab.setParent(parent);
			String title = ds.getName();
			dbtp.add(title, newTab);
			dbtp.setSelectedComponent(newTab);
		}
	}

	/** Resorts the list by the specified column. */
	public void resort()
	{
		this.tableModel.resort();
	}
}


class DataSourceListTableModel extends AbstractTableModel
	implements SortingListTableModel
{
	private String colNames[] = 
	{
		SourcesListPanel.genericLabels.getString("name"),
		SourcesListPanel.genericLabels.getString("type"),
		SourcesListPanel.dbeditLabels.getString("SourcesListPanel.args"),
		SourcesListPanel.dbeditLabels.getString("SourcesListPanel.usedBy")
	};
	private SourcesListPanel panel;
	private int lastSortColumn = -1;
	private DataSourceList theList;

	public DataSourceListTableModel(SourcesListPanel slp)
	{
		super();
		this.panel = slp;
		theList = Database.getDb().dataSourceList;
		theList.countUsedBy();
		this.sortByColumn(0);
		refill();
	}

	void refill()
	{
	}

	public int getRowCount()
	{
		return theList.size();
	}

	DataSource getObjectAt(int r)
	{
		return (DataSource)getRowObject(r);
	}

	public Object getRowObject(int r)
	{
		if (r >= 0 && r < getRowCount())
			return theList.getList().get(r);
		else return null;
	}

	void addDataSource(DataSource ds)
	{
		theList.add(ds);
		fireTableDataChanged();
	}

	void deleteObject(DataSource ds)
	{
		theList.remove(ds);
		try { Database.getDb().getDbIo().deleteDataSource(ds); }
		catch(DatabaseException e)
		{
			if ( ds.getId() != Constants.undefinedId ) 
				TopFrame.instance().showError(e.toString());
		}
		fireTableDataChanged();
	}

	public int getColumnCount()
	{
		int r = colNames.length;
		return r;
	}

	public String getColumnName(int c)
	{
		return colNames[c];
	}

	public boolean isCellEditable(int r, int c) { return false; }

	public Object getValueAt(int r, int c)
	{
		DataSource ds = getObjectAt(r);
		if (ds == null)
			return "";
		else
			return getDsColumn(ds, c);
	}

	public static String getDsColumn(DataSource ds, int c)
	{
		switch(c)
		{
		case 0: return ds.getName();
		case 1:
			return ds.dataSourceType == null ? "" : ds.dataSourceType;
		case 2:
			return ds.dataSourceArg == null ? "" : ds.dataSourceArg;
		case 3:
			return "" + ds.numUsedBy;
		default: return "";
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
		Collections.sort(theList.getList(), new DataSourceComparator(c));
		fireTableDataChanged();
	}

	void replace(DataSource oldOb, DataSource newOb)
	{
		theList.remove(oldOb);
		theList.add(newOb);
		if (lastSortColumn != -1)
			sortByColumn(lastSortColumn);
		else
			fireTableDataChanged();
	}

}

class DataSourceComparator implements Comparator
{
	int column;

	public DataSourceComparator(int column)
	{
		this.column = column;
	}

	/**
	 * Compare the eqMod names of the specified type.
	 */
	public int compare(Object ob1, Object ob2)
	{
		if (ob1 == ob2)
			return 0;
		DataSource ds1 = (DataSource)ob1;
		DataSource ds2 = (DataSource)ob2;

		String s1 = DataSourceListTableModel.getDsColumn(ds1, column);
		String s2 = DataSourceListTableModel.getDsColumn(ds2, column);

		return s1.compareToIgnoreCase(s2);
	}

	public boolean equals(Object ob)
	{
		return false;
	}
}
