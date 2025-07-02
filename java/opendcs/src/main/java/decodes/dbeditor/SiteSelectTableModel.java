package decodes.dbeditor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.EnumValue;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.gui.TopFrame;

class SiteSelectTableModel extends javax.swing.table.AbstractTableModel
{
	private String colNames[] = { "Local", "NWSHB5", "USGS" };
	private SiteSelectPanel panel;
	private List<Site> sites;

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
				colNames[i] = ev.getValue();
			}
			colNames[col.size()] = SiteSelectPanel.descriptionLabel;
		}
		sites = new ArrayList<>();
		refill();
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
		Site site = sites.get(index);
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
		Site site = sites.get(r);
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
		return sites.get(r);
	}

}

