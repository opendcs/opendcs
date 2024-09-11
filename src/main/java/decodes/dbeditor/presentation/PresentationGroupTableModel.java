/**
 * Copyright 2024 The OpenDCS Consortium and contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package decodes.dbeditor.presentation;

import java.util.ResourceBundle;

import javax.swing.table.AbstractTableModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import decodes.db.Constants;
import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.PresentationGroup;
import decodes.db.PresentationGroupList;
import decodes.dbeditor.PresentationGroupSelectPanel;
import decodes.gui.TopFrame;

public class PresentationGroupTableModel extends AbstractTableModel
{
	private static final Logger log = LoggerFactory.getLogger(PresentationGroupTableModel.class);
    private static final ResourceBundle generic = ResourceBundle.getBundle("decodes/resources/generic");
    private static final ResourceBundle dbedit = ResourceBundle.getBundle("decodes/resources/dbedit");
	static String colNames[] = 
	{
		generic.getString("name"),
		dbedit.getString("PresentationGroupSelectPanel.inheritsFrom"),
		generic.getString("lastMod"),
		generic.getString("isProduction")
	};
	private PresentationGroupSelectPanel panel;
	private PresentationGroupList pgList;
	private final Database db;

	public PresentationGroupTableModel(PresentationGroupSelectPanel pgsp, Database db)
	{
		super();
		this.panel = pgsp;
		this.db = db;
		pgList = db.presentationGroupList;
		refill();
	}

	public void refill()
	{
		fireTableDataChanged();
	}

	public void add(PresentationGroup ob)
	{
		pgList.add(ob);
		fireTableDataChanged();
	}

	public void replace(PresentationGroup oldOb, PresentationGroup newOb)
	{
		pgList.remove(oldOb);
		pgList.add(newOb);
	}

	public void deleteAt(int index)
	{
		try
		{ 
			PresentationGroup ob = pgList.getPGAt(index);		
			db.getDbIo().deletePresentationGroup(ob);
			pgList.remove(ob);
			fireTableDataChanged();
		}
		catch(DatabaseException e)
		{
			log.atError()
			   .setCause(e)
			   .log("Unable to delete Presentation Group.");
			TopFrame.instance().showError(e.toString());
		}
		
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

	public boolean isCellEditable(int r, int c)
	{
		return false;
	}

	public int getRowCount()
	{
		return pgList.size();
	}

	public Object getValueAt(int r, int c)
	{
		return getColumnValue(getObjectAt(r), c);
	}

	public PresentationGroup getObjectAt(int r)
	{
		return (PresentationGroup)getRowObject(r);
	}

	public Object getRowObject(int r)
	{
		return pgList.getPGAt(r);
	}

	private static Object getColumnValue(PresentationGroup pg, int c)
	{
		switch(c)
		{
		case 0: return pg.groupName;
		case 1: return pg.inheritsFrom == null ? "" : pg.inheritsFrom;
		case 2:
			return pg.lastModifyTime == null ? "" : pg.lastModifyTime;
		case 3: return pg.isProduction;
		default: return "";
		}
	}
}

    
