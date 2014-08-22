/*
*  $Id$
*/
package decodes.dbeditor;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.xml.crypto.Data;

import java.awt.event.*;
import java.util.ArrayList;
import java.util.Vector;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.ResourceBundle;

import ilex.util.LoadResourceBundle;
import ilex.util.Logger;

import decodes.gui.*;
import decodes.db.*;
import decodes.util.DecodesSettings;

/**
Displays a sorting-list of Platform objects in the database.
*/
public class PlatformSelectPanel extends JPanel
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

	BorderLayout borderLayout1 = new BorderLayout();
	JScrollPane jScrollPane1 = new JScrollPane();
	PlatformSelectTableModel model;
	SortingListTable platformListTable;
	PlatformSelectDialog parentDialog = null;
	PlatformListPanel parentPanel = null;

	/** Constructor. */
	public PlatformSelectPanel()
	{
		this(DecodesSettings.instance().transportMediumTypePreference);
	}

	public PlatformSelectPanel(String mediumType)
	{
		model = new PlatformSelectTableModel(this, mediumType);
		platformListTable = new SortingListTable(model,
			new int[] { 22, 10, 13, 18, 15, 33 });
		platformListTable.getSelectionModel().setSelectionMode(
			ListSelectionModel.SINGLE_SELECTION);
		setMultipleSelection(false);
		platformListTable.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				if (e.getClickCount() == 2)
				{
					if (parentDialog != null)
						parentDialog.openPressed();
					else if (parentPanel != null)
						parentPanel.openPressed();
//	       			((PlatformListPanel)PlatformSelectPanel.this.getParent()).openPressed();
				}
			}
		} );
		try
		{
			jbInit();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public void setParentDialog(PlatformSelectDialog dlg)
	{
		parentDialog = dlg;
	}
	
	public PlatformSelectPanel(final PlatformSelectDialog psd, Site site,
					String mediumType)
	{
		if ( site == null ) 
			model = new PlatformSelectTableModel(this, mediumType);
		else
			model = new PlatformSelectTableModel(this, site);
		platformListTable = new SortingListTable(model,
			new int[] { 22, 6, 20, 20,10, 33 });

		platformListTable.getSelectionModel().setSelectionMode(
			ListSelectionModel.SINGLE_SELECTION);
		setMultipleSelection(false);
		platformListTable.addMouseListener(new MouseAdapter(){
			public void mouseClicked(MouseEvent e){
				if (e.getClickCount() == 2){
	       				psd.openPressed();
				}
			}
		} );
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
		this.setPreferredSize(new Dimension(700,500));
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

	public void setParentPanel(PlatformListPanel parentPanel)
	{
		this.parentPanel = parentPanel;
	}
}

class PlatformSelectTableModel extends AbstractTableModel
	implements SortingListTableModel
{
	PlatformSelectColumnizer columnizer;
	String mediumType;
	Site site;

	private static String columnNames[] =
	{
		PlatformSelectPanel.genericLabels.getString("platform"),
		PlatformSelectPanel.dbeditLabels.getString("PlatformSelectPanel.agency"),
		PlatformSelectPanel.dbeditLabels.getString("PlatformSelectPanel.transport"),
		PlatformSelectPanel.dbeditLabels.getString("PlatformSelectPanel.config"),
		PlatformSelectPanel.genericLabels.getString("expiration"),
		PlatformSelectPanel.genericLabels.getString("description")
	};
	private PlatformSelectPanel panel;
	private Vector vec;
	private int sortColumn = -1;

	public PlatformSelectTableModel(PlatformSelectPanel panel, String mediumType)
	{
		super();
		this.panel = panel;
		this.mediumType = mediumType;
		columnizer = new PlatformSelectColumnizer(mediumType);		
		vec = Database.getDb().platformList.getPlatformVector();
		this.sortByColumn(0);
	}
	
	public PlatformSelectTableModel(PlatformSelectPanel panel, Site site)
	{
		super();
		this.panel = panel;
		this.site = site;
		columnizer = new PlatformSelectColumnizer(site );
		Vector fvec = Database.getDb().platformList.getPlatformVector();
		Platform p = null;
		Vector tvec = (Vector)fvec.clone();
		vec = new Vector();
		SiteName usgsName = site.getName(Constants.snt_USGS);
		if (usgsName != null)
		{
			for (int i =0; i < tvec.size(); i++ )
			{
				p = (Platform)tvec.elementAt(i);
				if ( p.site != null )
				{
					SiteName sn = p.site.getName(Constants.snt_USGS);
					if(sn != null) {
						if ( sn.equals(usgsName) )
							vec.add(p);
					}
					
				}
			}
		}
		this.sortByColumn(0);
	}

	public PlatformSelectTableModel(PlatformSelectPanel platformSelectPanel) {
		// TODO Auto-generated constructor stub
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
		return columnizer.getColumn(getPlatformAt(r), c);
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
		Collections.sort(vec, new PlatformColumnComparator(c, columnizer));
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
class PlatformSelectColumnizer
{
	private String mediumType;
	private Site site;

	public PlatformSelectColumnizer(Site site) 
	{
		this.site = site;
	}
	public PlatformSelectColumnizer(String mediumType)
	{
		this.mediumType = mediumType;
	}

	String getColumn(Platform p, int c)
	{
		switch(c)
		{
			case 0: // Site + Designator
			{
				if (p.site == null)
					return "";
				Site site = p.site;
				SiteName sn = site.getPreferredName();
				if ( sn == null )
					return "";
				String r = sn.getNameValue();
				if ( r == null ) 
					return "";
				String d = p.getPlatformDesignator();
				if (d != null)
					r = r + "-" + d;
				return r;
			}
			case 1: // Agency
				return p.agency == null ? "" : p.agency;
			case 2: // Transport-ID
			{
//			TransportMedium tm = p.getTransportMedium(mediumType);
//			if (tm != null) return tm.getMediumId();
				return p.getPreferredTransportId();
			}
			case 3: // Config
				return p.getConfigName();
			case 4: // Expiration
			{
				if (p.expiration == null)
					return "";
				else
				{
					return decodes.db.Constants.defaultDateFormat.format(
						p.expiration).toString();
				}
			}
			case 5: // Description
				return p.description == null ? "" : p.description;
			default:
				return "";
		}
	}
}

class PlatformColumnComparator implements Comparator
{
	int col;
	PlatformSelectColumnizer columnizer;

	PlatformColumnComparator(int col, PlatformSelectColumnizer columnizer)
	{
		this.col = col;
		this.columnizer = columnizer;
	}

	public int compare(Object ob1, Object ob2)
	{
		if (ob1 == ob2)
			return 0;
		Platform p1 = (Platform)ob1;
		Platform p2 = (Platform)ob2;
		return columnizer.getColumn(p1, col).compareToIgnoreCase(
			columnizer.getColumn(p2, col));
	}
}
