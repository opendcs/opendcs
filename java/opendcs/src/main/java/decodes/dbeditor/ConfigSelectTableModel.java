package decodes.dbeditor;

import java.util.Collections;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.DatabaseObject;
import decodes.db.PlatformConfig;
import decodes.gui.SortingListTableModel;
import decodes.gui.TopFrame;

class ConfigSelectTableModel extends AbstractTableModel
	implements SortingListTableModel
{
	private String colNames[] = 
	{ 
		ConfigSelectPanel.dbeditLabels.getString("ConfigSelectPanel.col0"),
		ConfigSelectPanel.dbeditLabels.getString("ConfigSelectPanel.col1"),
		ConfigSelectPanel.dbeditLabels.getString("ConfigSelectPanel.col2"),
		ConfigSelectPanel.dbeditLabels.getString("ConfigSelectPanel.col3")
	};
	private ConfigSelectPanel panel;
	private Vector vec;
	private int lastSortColumn;

	public ConfigSelectTableModel(ConfigSelectPanel ssp)
	{
		super();
		lastSortColumn = -1;
		this.panel = ssp;
		refill();
		this.sortByColumn(0);
	}

	void refill()
	{
		try 
		{
			Database.getDb().platformConfigList.read();
			Database.getDb().platformConfigList.countPlatformsUsing();
		} catch (DatabaseException dbe ) { };
		vec = new Vector(Database.getDb().platformConfigList.values());
	}

	void add(PlatformConfig ob)
	{
		for(int i=0; i<vec.size(); i++)
		{
			PlatformConfig pc = (PlatformConfig)vec.elementAt(i);
			if (pc.configName.equals(ob.configName))
				return;
		}
		vec.add(ob);
		fireTableDataChanged();
	}

	void replace(DatabaseObject oldOb, DatabaseObject newOb)
	{
		vec.remove(oldOb);
		vec.add(newOb);
		if (lastSortColumn != -1)
			sortByColumn(lastSortColumn);
		else
			fireTableDataChanged();
	}

	void deleteAt(int index)
	{
		PlatformConfig ob = (PlatformConfig)vec.elementAt(index);
		vec.remove(index);
		try { Database.getDb().getDbIo().deleteConfig(ob); }
		catch(DatabaseException e)
		{
			TopFrame.instance().showError(e.toString());
		}
		Database.getDb().platformConfigList.remove(ob);
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
		return vec.size();
	}

	public Object getValueAt(int r, int c)
	{
		return ConfigColumnizer.getColumn(getConfigAt(r), c);
	}

	PlatformConfig getConfigAt(int r)
	{
		return (PlatformConfig)getRowObject(r);
	}

	public Object getRowObject(int r)
	{
		return vec.elementAt(r);
	}

	public void sortByColumn(int c)
	{
		lastSortColumn = c;
		Collections.sort(vec, new pcColumnComparator(c));
		fireTableDataChanged();
	}
}
