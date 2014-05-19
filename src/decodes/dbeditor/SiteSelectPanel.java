/*
*  $Id$
*
*  $Log$
*  Revision 1.12  2013/07/10 19:24:01  mmaloney
*  Don't modify list if site delete fails.
*
*  Revision 1.11  2013/07/10 19:21:45  mmaloney
*  Don't modify list if site delete fails.
*
*  Revision 1.10  2013/07/10 19:15:24  mmaloney
*  Don't modify list if site delete fails.
*
*  Revision 1.9  2010/12/05 15:51:02  mmaloney
*  cleanup
*
*  Revision 1.8  2010/12/05 15:46:57  mmaloney
*  Allow app to change the SiteSelectPanel being used in SiteSelectDialog. This was broken before.
*
*  Revision 1.7  2010/02/23 22:52:32  mjmaloney
*  Double-click modifications to usgs-modified code.
*
*  Revision 1.6  2009/08/12 19:55:24  mjmaloney
*  usgs merge
*
*  Revision 1.5  2009/01/22 00:31:33  mjmaloney
*  DB Caching improvements to make msgaccess start quicker.
*  Remove the need to cache the entire database.
*
*  Revision 1.4  2008/11/20 18:49:21  mjmaloney
*  merge from usgs mods
*
*/
package decodes.dbeditor;

import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import java.util.Iterator;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import javax.swing.*;
import java.util.ResourceBundle;

import ilex.util.Logger;
import ilex.util.TextUtil;

import decodes.db.*;
import decodes.gui.*;
import decodes.util.DecodesSettings;

@SuppressWarnings("serial")
public class SiteSelectPanel extends JPanel
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();
	static String descriptionLabel = genericLabels.getString("description");

	BorderLayout borderLayout1 = new BorderLayout();
	JScrollPane jScrollPane1 = new JScrollPane();
	SiteSelectTableModel model;
	SortingListTable siteTable;
	SiteSelectDialog parentDialog = null;
	SiteListPanel parentPanel = null;

	public SiteSelectPanel()
	{
	    model = new SiteSelectTableModel(this);
		int cc = model.getColumnCount();
		int widths[] = new int[cc];
		for(int i=0; i<cc-1; i++)
			widths[i] = 10;
		widths[cc-1] = 30;
		siteTable = new SortingListTable(model, widths);
		setMultipleSelection(false);

		siteTable.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				if (e.getClickCount() == 2)
				{
					if (parentDialog != null)
						parentDialog.openPressed();
					else if (parentPanel != null)
						parentPanel.openPressed();
				}
			}
		} );

		try {
		    jbInit();
		}
		catch(Exception ex) {
		    ex.printStackTrace();
		}
	}

	public void setMultipleSelection(boolean ok)
	{
		siteTable.getSelectionModel().setSelectionMode(
			ok ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION :
			ListSelectionModel.SINGLE_SELECTION);
	}

	private void jbInit() throws Exception
	{
		this.setLayout(borderLayout1);
		jScrollPane1.setPreferredSize(new Dimension(600, 300));
        this.add(jScrollPane1, BorderLayout.CENTER);
		jScrollPane1.getViewport().add(siteTable, null);
	}

	/**
	 * @return the currently-selected site, or null if no site is selected.
	 */
	public Site getSelectedSite()
	{
		int r = siteTable.getSelectedRow();
		if (r == -1)
			return null;
		return model.getSiteAt(r);
	}

	public void clearSelection()
	{
		siteTable.clearSelection();
	}

	/**
	 * @return all currently-selected sites, or empty array if none.
	 */
	public Site[] getSelectedSites()
	{
		int idx[] = siteTable.getSelectedRows();
		Site ret[] = new Site[idx.length];
		for(int i=0; i<idx.length; i++)
			ret[i] = model.getSiteAt(idx[i]);
		return ret;
	}

	public void refill()
	{
		model.refill();
		invalidate();
		repaint();
	}

	public void addSite(Site site)
	{
		model.addSite(site);
		invalidate();
		repaint();
	}

	public void deleteSelectedSite()
	{
		int r = siteTable.getSelectedRow();
		if (r == -1)
			return;
		model.deleteSiteAt(r);
	}

	public void setParentDialog(SiteSelectDialog parentDialog)
	{
		this.parentDialog = parentDialog;
	}

	public void setParentPanel(SiteListPanel parentPanel)
	{
		this.parentPanel = parentPanel;
	}
	
	public void setSelection(SiteName sn)
	{
		for(int col=0; col < model.getColumnCount(); col++)
		{
			if (sn.getNameType().equalsIgnoreCase(model.getColumnName(col)))
			{
				for(int row=0; row < model.getRowCount(); row++)
				{
					if (sn.getNameValue().equalsIgnoreCase(
						(String)model.getValueAt(row, col)))
					{
						siteTable.clearSelection();
						siteTable.setRowSelectionInterval(row, row);
						return;
					}
				}
			}
		}
	}
}

class SiteSelectTableModel extends javax.swing.table.AbstractTableModel
	implements SortingListTableModel
{
	private String colNames[] = { "Local", "NWSHB5", "USGS" };
	private SiteSelectPanel panel;
	private Vector sites;

	public SiteSelectTableModel(SiteSelectPanel ssp)
	{
		super();
		this.panel = ssp;
		// Populate the column headers from the enumeration for site name type.
		decodes.db.DbEnum nameTypeEnum = Database.getDb().getDbEnum("SiteNameType");
		if (nameTypeEnum != null)
		{
			Collection col = nameTypeEnum.values();
			colNames = new String[col.size() + 1];
			Iterator it = col.iterator();
			for(int i = 0; i < colNames.length && it.hasNext(); i++)
			{
				EnumValue ev = (EnumValue)it.next();
				colNames[i] = ev.value;
			}
			colNames[col.size()] = SiteSelectPanel.descriptionLabel;
		}
		sites = new Vector();
		refill();

		// MJM 20080707 - Initial sorting should be by preferred name type.
		int c = -1;
		if (DecodesSettings.instance().siteNameTypePreference != null)
			for(int i=0; i<getColumnCount(); i++)
				if (colNames[i].equalsIgnoreCase(
					DecodesSettings.instance().siteNameTypePreference))
				{
					c = i;
					break;
				}
		if (c == -1)
			c = 0;
		this.sortByColumn(c);
	}

	void refill()
	{
		sites.clear();
		for(Iterator it = Database.getDb().siteList.iterator(); it.hasNext(); )
		{
			Site site = (Site)it.next();
			sites.add(site);
		}
	}

	void addSite(Site site)
	{
		sites.add(site);
		fireTableDataChanged();
	}

	void deleteSiteAt(int index)
	{
		Site site = (Site)sites.elementAt(index);
		try { Database.getDb().getDbIo().deleteSite(site); }
		catch(DatabaseException e)
		{
			TopFrame.instance().showError(e.toString());
			fireTableDataChanged();
			return;
		}
		sites.remove(index);
		Database.getDb().siteList.removeSite(site);
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

	public int getRowCount()
	{
		return sites.size();
	}

	public Object getValueAt(int r, int c)
	{
		Site site = (Site)sites.elementAt(r);
		if (c == colNames.length-1)
			return site.getDescription();
		String type = colNames[c];
		SiteName sn = site.getName(type);
		return sn != null ? sn.getDisplayName() : "";
	}

	Site getSiteAt(int r)
	{
		return (Site)getRowObject(r);
	}

	public Object getRowObject(int r)
	{
		return sites.elementAt(r);
	}

	public void sortByColumn(int c)
	{
		Collections.sort(sites, new SiteNameComparator(colNames[c], null));
	}
}

/**
 * Used for sorting and searching for site names.
 */
class SiteNameComparator implements Comparator
{
	String nameType;
	String nameValue;

	public SiteNameComparator(String type, String value)
	{
		nameType = type;
		nameValue = value;
	}

	/**
	 * Compare the site names of the specified type.
	 */
	public int compare(Object ob1, Object ob2)
	{
		if (ob1 == ob2)
			return 0;
		Site site1 = (Site)ob1;
		Site site2 = (Site)ob2;
		if (nameType == SiteSelectPanel.descriptionLabel)
			return TextUtil.strCompareIgnoreCase(site1.getDescription(),
				site2.getDescription());
		SiteName sn1 = site1.getName(nameType);
		String ss1 = sn1 != null ? sn1.getDisplayName() : "";
		SiteName sn2 = site2.getName(nameType);
		String ss2 = sn2 != null ? sn2.getDisplayName() : "";
		if (ss1.length() > 0 && ss2.length() == 0)
			return -1;
		else if (ss1.length() == 0 && ss2.length() > 0)
			return 1;
		if (nameType != null && nameType.equalsIgnoreCase("hdb"))
		{
			if (!ss1.equals("") && !ss2.equals(""))
			{
				int i1 = 0;
				int i2 = 0;
				try 
				{
					i1 = Integer.parseInt(ss1);
				}
				catch (Exception ex)
				{
					Logger.instance().warning(
						" SiteSelectPanel - SiteNameComparator " +
						" Can not sort column by hdb site name type." +
						" HDB site name is not a number. Site: " + ss1);
					return 1;
				}
				try 
				{
					i2 = Integer.parseInt(ss2);
				}
				catch (Exception ex)
				{
					Logger.instance().warning(
						" SiteSelectPanel - SiteNameComparator " +
						" Can not sort column by hdb site name type." +
						" HDB site name is not a number. Site: " + ss2);
					return -1;
				}
				return i1 - i2;
			}
		}
		return ss1.compareToIgnoreCase(ss2);
	}

	public boolean equals(Object ob)
	{
		return false;
	}
}
