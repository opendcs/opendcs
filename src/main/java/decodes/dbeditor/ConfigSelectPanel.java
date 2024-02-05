/*
*  $Id$
*
*  $Log$
*  Revision 1.9  2011/01/07 16:01:56  mmaloney
*  bugfixes
*
*  Revision 1.8  2010/09/16 15:03:13  mmaloney
*  dev
*
*  Revision 1.7  2010/09/16 14:49:11  mmaloney
*  double-click code causing exception. Removed for now.
*
*  Revision 1.6  2009/08/12 19:54:57  mjmaloney
*  usgs merge
*
*  Revision 1.5  2008/12/14 00:51:28  mjmaloney
*  platform count fix
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
import java.util.Enumeration;
import java.util.ResourceBundle;
import javax.swing.*;

import ilex.util.TextUtil;
import decodes.db.*;
import decodes.gui.*;

/**
Panel for selecting a Platform Configuration. Used inside ConfigSelectDialog.
*/
public class ConfigSelectPanel extends JPanel
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

	BorderLayout borderLayout1 = new BorderLayout();
	JScrollPane jScrollPane1 = new JScrollPane();
	ConfigSelectTableModel model;
	SortingListTable configTable;
	ConfigSelectDialog parentDialog = null;

	/** Constructor. */
	public ConfigSelectPanel()
	{
	    model = new ConfigSelectTableModel(this);
		configTable = new SortingListTable(model, null);
		configTable.getSelectionModel().setSelectionMode(
			ListSelectionModel.SINGLE_SELECTION);

		configTable.addMouseListener(
			new MouseAdapter()
			{
				public void mouseClicked(MouseEvent e)
				{
					if (e.getClickCount() == 2)
					{
						JPanel p = (JPanel)ConfigSelectPanel.this.getParent();
						if (p instanceof ConfigsListPanel)
							((ConfigsListPanel)p).openPressed();
						else if (parentDialog != null)
						{
							parentDialog.selectButtonPressed();
						}
					}
				}
			});
		try {
		    jbInit();
		}
		catch(Exception ex) {
		    ex.printStackTrace();
		}
	}

	/** Initializes GUI components. */
	private void jbInit() throws Exception
	{
		this.setLayout(borderLayout1);
		jScrollPane1.setPreferredSize(new Dimension(453, 300));
        this.add(jScrollPane1, BorderLayout.CENTER);
		jScrollPane1.getViewport().add(configTable, null);
	}

	/**
	 * @return the currently-selected object, or null if none is selected.
	 */
	public PlatformConfig getSelection()
	{
		int r = configTable.getSelectedRow();
		if (r == -1)
			return null;
		int modelRow = configTable.convertRowIndexToModel(r);
		return (PlatformConfig)model.getRowObject(modelRow);
	}

	/** Tells the panel to refresh itself from the database config list. */
	public void refill()
	{
		model.refill();
		invalidate();
		repaint();
	}

	/** 
	  Adds a configuration to the model.
	  @param ob the new configuration.
	*/
	public void addConfig(PlatformConfig ob)
	{
		model.add(ob);
		invalidate();
		repaint();
	}

	/**
	  Deletes the currently-selected configuration. 
	*/
	public void deleteSelection()
	{
		int r = configTable.getSelectedRow();
		if (r == -1)
			return;
		int modelRow = configTable.convertRowIndexToModel(r);
		model.deleteAt(modelRow);
	}

	/**
	  Replaces a configuration with another. Called when the Save button
	  is pressed in the config edit panel. Recall that the panel edits a 
	  copy of the config. Thus when it is saved, the new (modified) copy
	  replaces the old one.
	  @param oldPc the old platform config
	  @param newPc the new platform config
	*/
	public void replace(PlatformConfig oldPc, PlatformConfig newPc)
	{
		model.replace(oldPc, newPc);
	}

	/**
	  Sets the current selection.
	  @param name the name of the selected configuration.
	*/
	public void setSelection(String name)
	{
		int n = model.getRowCount();
		for(int i=0; i<n; i++)
		{
			PlatformConfig pc = model.getConfigAt(i);
			if (pc.configName.equalsIgnoreCase(name))
			{
				configTable.setRowSelectionInterval(i, i);
				Rectangle rect = configTable.getCellRect(i, 0, true);
				configTable.scrollRectToVisible(rect);
				break;
			}
		}
	}

	/** Clears any selections which have been made. */
	public void clearSelection()
	{
		configTable.clearSelection();
	}

	public void setParentDialog(ConfigSelectDialog parentDialog)
	{
		this.parentDialog = parentDialog;
	}
}

class ConfigSelectTableModel extends javax.swing.table.AbstractTableModel
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

class ConfigColumnizer
{
	static String getColumn(PlatformConfig pc, int c)
	{
		switch(c)
		{
		case 0: return pc.configName;
		case 1: return pc.equipmentModel == null ? "" : 
			pc.equipmentModel.name;
		case 2: return "" + pc.numPlatformsUsing;
		case 3: return pc.description;
		default: return "";
		}
	}
}

class pcColumnComparator implements Comparator
{
	int col;

	pcColumnComparator(int col)
	{
		this.col = col;
	}

	public int compare(Object ob1, Object ob2)
	{
		if (ob1 == ob2)
			return 0;
		PlatformConfig pc1 = (PlatformConfig)ob1;
		PlatformConfig pc2 = (PlatformConfig)ob2;
		switch(col)
		{
		case 0: return TextUtil.strCompareIgnoreCase(
			pc1.configName, pc2.configName);
		case 1: return TextUtil.strCompareIgnoreCase(
			pc1.getEquipmentModelName(), pc2.getEquipmentModelName());
		case 2: return pc1.numPlatformsUsing - pc2.numPlatformsUsing;
		case 3: return TextUtil.strCompareIgnoreCase(
			pc1.description, pc2.description);
		default: return 0;
		}
	}
}
