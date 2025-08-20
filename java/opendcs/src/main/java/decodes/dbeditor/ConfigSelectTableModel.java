/*
 * Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package decodes.dbeditor;

import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

import decodes.db.*;
import decodes.gui.TopFrame;

class ConfigSelectTableModel extends AbstractTableModel
{
	private String colNames[] = 
	{ 
		ConfigSelectPanel.dbeditLabels.getString("ConfigSelectPanel.col0"),
		ConfigSelectPanel.dbeditLabels.getString("ConfigSelectPanel.col1"),
		ConfigSelectPanel.dbeditLabels.getString("ConfigSelectPanel.col2"),
		ConfigSelectPanel.dbeditLabels.getString("ConfigSelectPanel.col3")
	};
	private List<PlatformConfig> items;

	public ConfigSelectTableModel()
	{
		super();
		refill();
	}

	void refill()
	{
		try 
		{
			Database.getDb().platformConfigList.read();
			Database.getDb().platformConfigList.countPlatformsUsing();
		} catch (DatabaseException dbe ) { }
		items = new ArrayList<>(Database.getDb().platformConfigList.values());
		fireTableDataChanged();
	}

	void add(PlatformConfig ob)
	{
		for(int i = 0; i< items.size(); i++)
		{
			PlatformConfig pc = items.get(i);
			if (pc.configName.equals(ob.configName))
				return;
		}
		items.add(ob);
		fireTableDataChanged();
	}

	void replace(PlatformConfig oldOb, PlatformConfig newOb)
	{
		int row = items.indexOf(oldOb);
		if (row == -1) return;

		items.set(row, newOb);
		fireTableRowsUpdated(row, row);
	}

	void deleteAt(int index)
	{
		PlatformConfig ob = items.get(index);
		items.remove(index);
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
		return items.size();
	}

	public Object getValueAt(int row, int col)
	{
		PlatformConfig pc = items.get(row);
		switch(col) {
			case 0: return pc.configName;
			case 1: return pc.getEquipmentModelName();
			case 2: return pc.numPlatformsUsing;    // return an Integer
			case 3: return pc.description;
			default: throw new IllegalArgumentException("Bad column: "+col);
		}
	}

	PlatformConfig getConfigAt(int r)
	{
		return (PlatformConfig)getRowObject(r);
	}

	public Object getRowObject(int r)
	{
		return items.get(r);
	}

}
