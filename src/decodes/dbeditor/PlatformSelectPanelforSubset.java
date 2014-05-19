/*
*  $Id$
*/
package decodes.dbeditor;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.Vector;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.ResourceBundle;

import ilex.util.LoadResourceBundle;
import ilex.util.Logger;

import decodes.gui.*;
import decodes.db.*;

/**
Displays a sorting-list of Platform objects in the database.
*/
public class PlatformSelectPanelforSubset extends JPanel
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

    BorderLayout borderLayout1 = new BorderLayout();
    JScrollPane jScrollPane1 = new JScrollPane();
	PlatformSelectTableModelforSubset model;
    SortingListTable platformListTable;

	/** Constructor. */
    public PlatformSelectPanelforSubset(Vector<Platform> vec)
	{
		model = new PlatformSelectTableModelforSubset(this,vec);
    	platformListTable = new SortingListTable(model,
			new int[] { 22, 10, 13, 18, 15, 33 });
		platformListTable.getSelectionModel().setSelectionMode(
			ListSelectionModel.SINGLE_SELECTION);
		setMultipleSelection(false);

        try
		{
            jbInit();
        }
        catch(Exception ex)
		{
            ex.printStackTrace();
        }
    }

	public void setMultipleSelection(boolean ok)
	{
		platformListTable.getSelectionModel().setSelectionMode(
			ok ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION :
			ListSelectionModel.SINGLE_SELECTION);
	}

	/** Initializes GUI components. */
    private void jbInit() throws Exception
	{
        this.setLayout(borderLayout1);
        this.add(jScrollPane1, BorderLayout.CENTER);
        jScrollPane1.getViewport().add(platformListTable, null);
    }

	/**
	  After saving, and edit panel will need to replace the old object
	  with the newly modified one. It calls this method to do this.
	  @param oldp the old object
	  @param newp the new object
	*/
	public void replacePlatform(Platform oldp, Platform newp)
	{
		model.replacePlatform(oldp, newp);
	}

	/**
	 * Adds a new platform to the list.
	  @param newp the new object
	 */
	public void addPlatform(Platform newp)
	{
		model.addPlatform(newp);
	}

	/**
	 * @return the currently-selected platform, or null if none selected.
	 */
	public Platform getSelectedPlatform()
	{
		int r = platformListTable.getSelectedRow();
		if (r == -1)
			return null;
		return model.getPlatformAt(r);
	}

	/**
	 * @return all currently-selected platforms, or empty array if none.
	 */
	public Platform[] getSelectedPlatforms()
	{
		int idx[] = platformListTable.getSelectedRows();
		Platform ret[] = new Platform[idx.length];
		for(int i=0; i<idx.length; i++)
			ret[i] = model.getPlatformAt(idx[i]);
		return ret;
	}

	/** Resorts the list by the current column selection. */
	public void reSort()
	{
		model.reSort();
	}

	/**
	 * Deletes the specified platform from the list.
	 * @param ob the object to delete.
	 */
	public void deletePlatform(Platform ob)
	{
		model.deletePlatform(ob);
	}
}

class PlatformSelectTableModelforSubset extends AbstractTableModel
	implements SortingListTableModel
{
	private static String columnNames[] =
	{
		PlatformSelectPanelforSubset.genericLabels.getString("platform"),
		PlatformSelectPanelforSubset.dbeditLabels.getString("PlatformSelectPanelforSubset.agency"),
		PlatformSelectPanelforSubset.dbeditLabels.getString("PlatformSelectPanelforSubset.transport"),
		PlatformSelectPanelforSubset.dbeditLabels.getString("PlatformSelectPanelforSubset.config"),
		PlatformSelectPanelforSubset.genericLabels.getString("expiration"),
		PlatformSelectPanelforSubset.genericLabels.getString("description")
	};
	private PlatformSelectPanelforSubset panel;
	private Vector vec;
	private int sortColumn = -1;

	public PlatformSelectTableModelforSubset(PlatformSelectPanelforSubset panel,
					Vector pvec)
	{
		super();
		this.panel = panel;
		vec = pvec;
	}

	void addPlatform(Platform ob)
	{
		Database.getDb().platformList.add(ob);
		reSort();
		fireTableDataChanged();
	}

	void deletePlatformAt(int index)
	{
		Platform ob = (Platform)vec.elementAt(index);
		deletePlatform(ob);
	}

	void deletePlatform(Platform ob)
	{
		try
		{
                  if (ob.idIsSet())
                  {
			Database.getDb().platformList.removePlatform(ob);
			Database.getDb().getDbIo().deletePlatform(ob);
                  }
		}
		catch(DatabaseException e)
		{
			TopFrame.instance().showError(e.toString());
		}
		fireTableDataChanged();
	}

	void replacePlatform(Platform oldp, Platform newp)
	{
		Database.getDb().platformList.removePlatform(oldp);
		addPlatform(newp);
	}

	public int getColumnCount() { return columnNames.length; }

	public String getColumnName(int col)
	{
		return columnNames[col];
	}

	public boolean isCellEditable(int r, int c) { return false; }

	public int getRowCount()
	{
		return vec.size();
	}

	public Object getValueAt(int r, int c)
	{
		return PlatformSelectColumnizerforSubset.getColumn(getPlatformAt(r), c);
	}

	Platform getPlatformAt(int r)
	{
		return (Platform)getRowObject(r);
	}

	public Object getRowObject(int r)
	{
		return vec.elementAt(r);
	}

	public void sortByColumn(int c)
	{
		sortColumn = c;
		Collections.sort(vec, new PlatformColumnComparatorforSubset(c));
	}

	public void reSort()
	{
		if (sortColumn >= 0)
			sortByColumn(sortColumn);
	}

}

/**
 Helper class to retrieve platform fields by column number. Used for
 displaying values in the table and for sorting.
*/
class PlatformSelectColumnizerforSubset
{
	static String getColumn(Platform p, int c)
	{
		switch(c)
		{
		case 0: // Site + Designator
		  {
			Site site = p.site;
			if (p.site == null)
				return "";
		    else
			{
				String r = site.getPreferredName().getNameValue();
				String d = p.getPlatformDesignator();
				if (d != null)
					r = r + "-" + d;
			    return r;
			}
		  }
		case 1: // Agency
			return p.agency == null ? "" : p.agency;
		case 2: // Transport-ID
			return p.getPreferredTransportId();
		case 3: // Config
			return p.getConfigName();
		case 4: // Expiration
			if (p.expiration == null)
				return "";
			else
			{
				return decodes.db.Constants.defaultDateFormat.format(
					p.expiration).toString();
			}
		case 5: // Description
			return p.description == null ? "" : p.description;
		default:
			return "";
		}
	}
}

class PlatformColumnComparatorforSubset implements Comparator
{
	int col;

	PlatformColumnComparatorforSubset(int col)
	{
		this.col = col;
	}

	public int compare(Object ob1, Object ob2)
	{
		if (ob1 == ob2)
			return 0;
		Platform p1 = (Platform)ob1;
		Platform p2 = (Platform)ob2;
		return PlatformSelectColumnizerforSubset.getColumn(p1, col).compareToIgnoreCase(
			PlatformSelectColumnizerforSubset.getColumn(p2, col));
	}
}
