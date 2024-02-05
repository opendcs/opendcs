/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.4  2009/08/12 19:54:30  mjmaloney
*  usgs merge
*
*  Revision 1.3  2008/09/28 19:22:44  mjmaloney
*  Added <all> and <production> network lists
*
*  Revision 1.2  2008/06/06 15:10:06  cvs
*  updates from USGS & fixes to update-check.
*
*  Revision 1.1  2008/04/04 18:21:01  cvs
*  Added legacy code to repository
*
*  Revision 1.10  2008/01/24 13:57:46  mmaloney
*  modified files for internationalization
*
*  Revision 1.9  2008/01/11 22:14:46  mmaloney
*  Internationalization
*  ~Dan
*
*  Revision 1.8  2004/09/19 20:08:58  mjmaloney
*  javadocs added. Removed unused classes.
*
*  Revision 1.7  2002/10/31 18:53:12  mjmaloney
*  Fixed copy() functions for SQL.
*
*  Revision 1.6  2002/04/06 21:15:48  mike
*  Conform to new SortingListTableModel to preserve selections after sort.
*
*  Revision 1.5  2001/11/12 01:49:35  mike
*  dev
*
*  Revision 1.4  2001/11/10 21:17:37  mike
*  Implementing Netlist Edit Screen
*
*  Revision 1.3  2001/11/10 14:55:46  mike
*  Implementing sources & network list editors.
*
*/
package decodes.dbeditor;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.Comparator;
import java.util.Collections;
import java.util.ResourceBundle;

import decodes.gui.*;
import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.NetworkList;
import decodes.db.NetworkListEntry;
import decodes.db.NetworkListList;

import ilex.util.LoadResourceBundle;

/**
Dbedit panel that shows a sorting list of network lists in the database.
*/
public class NetlistListPanel extends JPanel
	implements ListOpsController
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();
	
    BorderLayout borderLayout1 = new BorderLayout();
    JLabel jLabel1 = new JLabel();
    JPanel jPanel1 = new JPanel();
    BorderLayout borderLayout2 = new BorderLayout();
    JScrollPane jScrollPane1 = new JScrollPane();
    ListOpsPanel listOpsPanel = new ListOpsPanel(this);

    JTable netlistListTable;
	NetlistListTableModel tableModel;
	DbEditorFrame parent;

	/** Constructor. */
	public NetlistListPanel()
	{
        try
		{
			tableModel = new NetlistListTableModel();
			netlistListTable = new SortingListTable(tableModel,
				new int[] { 40, 40, 20} );
		    netlistListTable.getSelectionModel().setSelectionMode(
				ListSelectionModel.SINGLE_SELECTION);
			netlistListTable.addMouseListener(
				new MouseAdapter()
				{
					public void mouseClicked(MouseEvent e)
					{
						if (e.getClickCount() == 2)
						{
	       					openPressed();
						}
					}
				});
            jbInit();
        }
        catch(Exception ex) {
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
        jLabel1.setHorizontalAlignment(SwingConstants.CENTER);
        jLabel1.setText(
    			dbeditLabels.getString("NetlistListPanel.DefinedLists"));
        this.setLayout(borderLayout1);
        jPanel1.setLayout(borderLayout2);
        this.add(jLabel1, BorderLayout.NORTH);
        this.add(jPanel1, BorderLayout.CENTER);
        jPanel1.add(jScrollPane1, BorderLayout.CENTER);
        this.add(listOpsPanel, BorderLayout.SOUTH);
        jScrollPane1.getViewport().add(netlistListTable, null);
    }

	private NetworkList getSelection()
	{
		int idx = netlistListTable.getSelectedRow();
		if (idx == -1)
		{
			return null;
		}
		int modelRow = netlistListTable.convertRowIndexToModel(idx);
	    return tableModel.getObjectAt(modelRow);
	}

	/** @return type of entity that this panel edits. */
	public String getEntityType() { return 
		dbeditLabels.getString("NetlistListPanel.NetListsText"); }

	/** Called when the 'Open' button is pressed. */
	public void openPressed()
	{
		NetworkList nl = getSelection();
		if (nl == null)
			TopFrame.instance().showError(
					LoadResourceBundle.sprintf(
					dbeditLabels.getString("NetlistListPanel.OpenError"),
					getEntityType())
				);
		else
		    doOpen(nl);
	}

	/** Called when the 'New' button is pressed. */
	public void newPressed()
	{
	    String newName = JOptionPane.showInputDialog(
				dbeditLabels.getString("NetlistListPanel.NewPrompt")+
				" " + getEntityType() + ":");
		if (newName == null)
			return;

		if (Database.getDb().networkListList.find(newName) != null)
		{
			TopFrame.instance().showError(
			LoadResourceBundle.sprintf(
			dbeditLabels.getString("NetlistListPanel.NewError")
			, getEntityType()));
			return;
		}

		NetworkList ob = new NetworkList(newName);
		Database.getDb().networkListList.add(ob);
		tableModel.fireTableDataChanged();
		doOpen(ob);
	}

	/** Called when the 'Copy' button is pressed. */
	public void copyPressed()
	{
		NetworkList nl = getSelection();
		if (nl == null)
		{
			TopFrame.instance().showError(
					LoadResourceBundle.sprintf(
					dbeditLabels.getString("NetlistListPanel.CopyError1"),
					getEntityType()));
			return;
		}
	    String newName = JOptionPane.showInputDialog(
			dbeditLabels.getString("NetlistListPanel.CopyPrompt"));
		if (newName == null)
			return;

		if (Database.getDb().networkListList.find(newName) != null)
		{
			TopFrame.instance().showError(
					LoadResourceBundle.sprintf(
					dbeditLabels.getString("NetlistListPanel.NewError")
					, getEntityType()));
			return;
		}

		NetworkList ob = nl.copy();
		ob.clearId();
		ob.name = newName;
		try { ob.write(); }
		catch(DatabaseException e)
		{
			TopFrame.instance().showError(
				dbeditLabels.getString("NetlistListPanel.CopyError2")
				+ ob.name + "': " + e);
			return;
		}
		tableModel.addNetworkList(ob);
		doOpen(ob);
	}

	/** Called when the 'Delete' button is pressed. */
	public void deletePressed()
	{
		NetworkList nl = getSelection();
		if (nl == null)
		{
			TopFrame.instance().showError(
					LoadResourceBundle.sprintf(
					dbeditLabels.getString("NetlistListPanel.DeleteError"),
					getEntityType()));
			return;
		}

		DbEditorTabbedPane netlistTabbedPane = parent.getNetworkListTabbedPane();
		DbEditorTab tab = netlistTabbedPane.findEditorFor(nl);
		if (tab != null)
		{
			TopFrame.instance().showError(
				LoadResourceBundle.sprintf(
				dbeditLabels.getString("NetlistListPanel.DeleteError1")
				, getEntityType()));
			return;
		}
		int r = JOptionPane.showConfirmDialog(this,
				dbeditLabels.getString("NetlistListPanel.DeletePrompt1")
				+" " + getEntityType()+ " " + nl.name +
				"?", dbeditLabels.getString("NetlistListPanel.DeletePrompt2")
				, JOptionPane.YES_NO_OPTION);
		if (r == JOptionPane.YES_OPTION)
			tableModel.deleteObject(nl);
	}

	public void refreshPressed()
	{
	}

	/**
	  Opens an object in an Edit Panel.
	  @param nl the object to be edited.
	*/
	private void doOpen(NetworkList nl)
	{
		DbEditorTabbedPane dbtp = parent.getNetworkListTabbedPane();
		DbEditorTab tab = dbtp.findEditorFor(nl);
		if (tab != null)
			dbtp.setSelectedComponent(tab);
		else
		{
			NetlistEditPanel newTab = new NetlistEditPanel(nl);
			newTab.setParent(parent);
			String title = nl.name;
			dbtp.add(title, newTab);
			dbtp.setSelectedComponent(newTab);
		}
	}

	/** Resorts this list of network list names. */
	public void resort()
	{
		this.tableModel.resort();
	}

}

class NetlistListTableModel extends AbstractTableModel
	implements SortingListTableModel
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();
	
	static String columnNames[] = { 
		dbeditLabels.getString("NetlistListPanel.TableColumn1"), 
		dbeditLabels.getString("NetlistListPanel.TableColumn2"), 
		dbeditLabels.getString("NetlistListPanel.TableColumn3") };
	private int lastSortColumn = -1;
	private NetworkListList theList;

	public NetlistListTableModel()
	{
		super();
		theList = Database.getDb().networkListList;
		refill();
		this.sortByColumn(0);
	}

	void refill()
	{
	}

	public int getRowCount()
	{
		return theList.size();
	}

	public int getColumnCount() { return columnNames.length; }

	public String getColumnName(int col)
	{
		return columnNames[col];
	}

	NetworkList getObjectAt(int r)
	{
		return (NetworkList)getRowObject(r);
	}

	public Object getRowObject(int r)
	{
		if (r >= 0 && r < getRowCount())
			return theList.getList().elementAt(r);
		else return null;
	}

	void addNetworkList(NetworkList ob)
	{
		theList.add(ob);
		fireTableDataChanged();
	}

	void deleteObject(NetworkList ob)
	{
		theList.remove(ob);

		try { Database.getDb().getDbIo().deleteNetworkList(ob); }
		catch(DatabaseException e)
		{
			TopFrame.instance().showError(
				"Error trying to delete network list: " + e.toString());
		}
		fireTableDataChanged();
	}

	public Object getValueAt(int r, int c)
	{
		NetworkList ob = getObjectAt(r);
		if (ob == null)
			return "";
		else
			return getNlColumn(ob, c);
	}

	public static String getNlColumn(NetworkList ob, int c)
	{
		switch(c)
		{
		case 0: return ob.name;
		case 1: return ob.transportMediumType;
		case 2: return "" + ob.networkListEntries.size();
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
		Collections.sort(theList.getList(), new NetworkListComparator(c));
		fireTableDataChanged();
	}

	void replace(NetworkList oldOb, NetworkList newOb)
	{
		theList.remove(oldOb);
		theList.add(newOb);
		if (lastSortColumn != -1)
			sortByColumn(lastSortColumn);
		else
			fireTableDataChanged();
	}
}

class NetworkListComparator implements Comparator<NetworkList>
{
	int column;

	public NetworkListComparator(int column)
	{
		this.column = column;
	}

	/**
	 * Compare the eqMod names of the specified type.
	 */
	public int compare(NetworkList ds1, NetworkList ds2)
	{
		if (ds1 == ds2)
			return 0;
		String s1 = NetlistListTableModel.getNlColumn(ds1, column);
		String s2 = NetlistListTableModel.getNlColumn(ds2, column);

		return s1.compareToIgnoreCase(s2);
	}

	public boolean equals(Object ob)
	{
		return false;
	}
}
