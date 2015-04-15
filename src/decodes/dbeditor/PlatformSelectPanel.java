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

	public PlatformSelectPanel(String mediumType)
	{
		model = new PlatformSelectTableModel(this, mediumType);
		platformListTable = new SortingListTable(model, model.columnWidths);
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
	
	public PlatformSelectPanel(final PlatformSelectDialog psd, Site site, String mediumType)
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
		this.setPreferredSize(new Dimension(800,500));
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
	Site site;
	private PlatformSelectPanel panel;
//	private Vector vec;
	private ArrayList<Platform> platformList = new ArrayList<Platform>();
	private int sortColumn = -1;

	static String colNamesNoDesig[] = 
	{
		PlatformSelectPanel.genericLabels.getString("platform"),
		PlatformSelectPanel.dbeditLabels.getString("PlatformSelectPanel.agency"),
		PlatformSelectPanel.dbeditLabels.getString("PlatformSelectPanel.transport"),
		PlatformSelectPanel.dbeditLabels.getString("PlatformSelectPanel.config"),
		PlatformSelectPanel.genericLabels.getString("expiration"),
		PlatformSelectPanel.genericLabels.getString("description")
	};
	static String colNamesDesig[] = 
	{
		PlatformSelectPanel.genericLabels.getString("platform"),
		"Designator",
		PlatformSelectPanel.dbeditLabels.getString("PlatformSelectPanel.agency"),
		PlatformSelectPanel.dbeditLabels.getString("PlatformSelectPanel.transport"),
		PlatformSelectPanel.dbeditLabels.getString("PlatformSelectPanel.config"),
		PlatformSelectPanel.genericLabels.getString("expiration"),
		PlatformSelectPanel.genericLabels.getString("description")
	};
	
	String columnNames[] = DecodesSettings.instance().platformListDesignatorCol
		? colNamesDesig : colNamesNoDesig;

	int columnWidths [] = DecodesSettings.instance().platformListDesignatorCol
		? new int[] { 18, 6, 6, 20, 20,10, 30 } : new int[] { 22, 6, 20, 20,10, 33 };


	public PlatformSelectTableModel(PlatformSelectPanel panel, String mediumType)
	{
		super();
		this.panel = panel;
		columnizer = new PlatformSelectColumnizer();		
		for(Platform platform : Database.getDb().platformList.getPlatformVector())
		{
			if (mediumType == null 
			 || platform.getTransportMedium(mediumType) != null
			 || (mediumType.equalsIgnoreCase(Constants.medium_Goes)
				 && (platform.getTransportMedium(Constants.medium_GoesST) != null
				  || platform.getTransportMedium(Constants.medium_GoesRD) != null))
			 || (mediumType.equalsIgnoreCase("poll")
				 && (platform.getTransportMedium("polled-modem") != null
				  || platform.getTransportMedium("polled-tcp") != null)))
				platformList.add(platform);
		}
		this.sortByColumn(0);
	}
	
	public PlatformSelectTableModel(PlatformSelectPanel panel, Site site)
	{
		super();
		this.panel = panel;
		this.site = site;
		columnizer = new PlatformSelectColumnizer();
		Vector fvec = Database.getDb().platformList.getPlatformVector();
		Platform p = null;
		Vector tvec = (Vector)fvec.clone();
		SiteName usgsName = site.getName(Constants.snt_USGS);
		if (usgsName != null)
		{
			for (int i =0; i < tvec.size(); i++ )
			{
				p = (Platform)tvec.elementAt(i);
				if ( p.getSite() != null )
				{
					SiteName sn = p.getSite().getName(Constants.snt_USGS);
					if(sn != null) {
						if ( sn.equals(usgsName) )
							platformList.add(p);
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
		platformList.add(ob);
		reSort();
		fireTableDataChanged();
	}

	void deletePlatformAt(int index)
	{
		deletePlatform(platformList.get(index));
	}

	void deletePlatform(Platform ob)
	{
		platformList.remove(ob);
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
		if (!platformList.remove(oldp))
			Logger.instance().debug3("oldp was not in list.");
		addPlatform(newp);
		fireTableDataChanged();
	}

	public int getColumnCount() { return columnNames.length; }

	public String getColumnName(int col)
	{
		return columnNames[col];
	}

	public boolean isCellEditable(int r, int c) { return false; }

	public int getRowCount()
	{
		return platformList.size();
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
		return platformList.get(r);
	}

	public void sortByColumn(int c)
	{
		sortColumn = c;
		Collections.sort(platformList, new PlatformColumnComparator(c, columnizer));
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
	private boolean desig = DecodesSettings.instance().platformListDesignatorCol;

	String getColumn(Platform p, int c)
	{
		switch(c)
		{
			case 0: // Site + Designator
			{
				if (p.getSite() == null)
					return "";
				Site site = p.getSite();
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
			case 1: // Desig or Agency
				if (desig)
					return p.getPlatformDesignator() == null ? "" : p.getPlatformDesignator();
				else
					return p.agency == null ? "" : p.agency;
			case 2: // Agency or Transport-ID
			{
				if (desig)
					return p.agency == null ? "" : p.agency;
				else
					return p.getPreferredTransportId();
			}
			case 3: // Transport-ID or Config
			{
				if (desig)
					return p.getPreferredTransportId();
				else
					return p.getConfigName();
			}
			case 4: // Config or Expiration
			{
				if (desig)
					return p.getConfigName();
				else if (p.expiration == null)
					return "";
				else
				{
					return decodes.db.Constants.defaultDateFormat.format(
						p.expiration).toString();
				}
			}
			case 5: // Expiration or Description
			{
				if (desig)
				{
					if (p.expiration == null)
						return "";
					else
					{
						return decodes.db.Constants.defaultDateFormat.format(
							p.expiration).toString();
					}
				}
				else
					return p.description == null ? "" : p.description;
			}
			case 6: // desig must be true. return description
				return p.description == null ? "" : p.description;
			default:
				return "";
		}
	}
}

class PlatformColumnComparator implements Comparator<Platform>
{
	int col;
	PlatformSelectColumnizer columnizer;

	PlatformColumnComparator(int col, PlatformSelectColumnizer columnizer)
	{
		this.col = col;
		this.columnizer = columnizer;
	}

	public int compare(Platform p1, Platform p2)
	{
		if (p1 == p2)
			return 0;
		return columnizer.getColumn(p1, col).compareToIgnoreCase(
			columnizer.getColumn(p2, col));
	}
}
