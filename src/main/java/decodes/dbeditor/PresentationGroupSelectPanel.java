/*
*  $Id$
*/
package decodes.dbeditor;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.ResourceBundle;

import decodes.gui.SortingListTable;
import decodes.gui.SortingListTableModel;
import decodes.gui.TopFrame;

import decodes.dbeditor.PresentationGroupListPanel;
import decodes.db.PresentationGroup;
import decodes.db.DatabaseException;
import decodes.db.PresentationGroupList;
import decodes.db.Database;
import decodes.db.Constants;

@SuppressWarnings("serial")
public class PresentationGroupSelectPanel extends JPanel
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

	BorderLayout borderLayout1 = new BorderLayout();
	JScrollPane jScrollPane1 = new JScrollPane();
	PGTableModel model;
	SortingListTable pgTable;

	public PresentationGroupSelectPanel(final PresentationGroupListPanel pglp)
	{
		model = new PGTableModel(this);
		pgTable = new SortingListTable(model, null);
		pgTable.getSelectionModel().setSelectionMode(
		ListSelectionModel.SINGLE_SELECTION);
		pgTable.addMouseListener(
			new MouseAdapter()
			{
				public void mouseClicked(MouseEvent e)
				{
					if (e.getClickCount() == 2)
						pglp.openPressed();
				}
			});
		try {
		    jbInit();
		}
		catch(Exception ex) {
		    ex.printStackTrace();
		}
	}

	private void jbInit() throws Exception
	{
		this.setLayout(borderLayout1);
		jScrollPane1.setPreferredSize(new Dimension(453, 300));
        this.add(jScrollPane1, BorderLayout.CENTER);
		jScrollPane1.getViewport().add(pgTable, null);
	}

	/**
	 * Returns the currently-selected object, or null if none is selected.
	 */
	public PresentationGroup getSelection()
	{
		int r = pgTable.getSelectedRow();
		if (r == -1)
		{
			return null;
		}
		int modelRow = pgTable.convertRowIndexToModel(r);
		return model.getObjectAt(modelRow);
	}

	public void refill()
	{
		model.refill();
	}

	public void addPG(PresentationGroup ob)
	{
		model.add(ob);
		invalidate();
		repaint();
	}

	public void deleteSelection()
	{
		int r = pgTable.getSelectedRow();
		if (r == -1)
		{
			return;
		}
		int modelRow = pgTable.convertRowIndexToModel(r);
		model.deleteAt(modelRow);
	}

	public void replace(PresentationGroup oldPg, PresentationGroup newPg)
	{
		model.replace(oldPg, newPg);
	}
}

class PGTableModel extends javax.swing.table.AbstractTableModel
	implements SortingListTableModel
{
	static String colNames[] = 
	{
		PresentationGroupSelectPanel.genericLabels.getString("name"),
		PresentationGroupSelectPanel.dbeditLabels.getString(
			"PresentationGroupSelectPanel.inheritsFrom"),
		PresentationGroupSelectPanel.genericLabels.getString("lastMod"),
		PresentationGroupSelectPanel.genericLabels.getString("isProduction")
	};
	private PresentationGroupSelectPanel panel;
	private PresentationGroupList pgList;
	private int lastSortColumn;

	public PGTableModel(PresentationGroupSelectPanel pgsp)
	{
		super();
		lastSortColumn = -1;
		this.panel = pgsp;
		pgList = Database.getDb().presentationGroupList;
		refill();
		this.sortByColumn(0);
	}

	void refill()
	{
		if (lastSortColumn != -1)
			this.sortByColumn(lastSortColumn);
		fireTableDataChanged();
	}

	public void add(PresentationGroup ob)
	{
		pgList.add(ob);
		fireTableDataChanged();
	}

	void replace(PresentationGroup oldOb, PresentationGroup newOb)
	{
		pgList.remove(oldOb);
		pgList.add(newOb);

		if (lastSortColumn != -1)
			sortByColumn(lastSortColumn);
		else
			fireTableDataChanged();
	}

	void deleteAt(int index)
	{
		PresentationGroup ob = pgList.getPGAt(index);
		pgList.remove(ob);
		try { Database.getDb().getDbIo().deletePresentationGroup(ob); }
		catch(DatabaseException e)
		{
			TopFrame.instance().showError(e.toString());
		}
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
		return pgList.size();
	}

	public Object getValueAt(int r, int c)
	{
		return PGColumnizer.getColumn(getObjectAt(r), c);
	}

	PresentationGroup getObjectAt(int r)
	{
		return (PresentationGroup)getRowObject(r);
	}

	public Object getRowObject(int r)
	{
		return pgList.getPGAt(r);
	}

	public void sortByColumn(int c)
	{
		lastSortColumn = c;
		Collections.sort(pgList.getVector(), new PGColumnComparator(c));
		fireTableDataChanged();
	}
}

class PGColumnizer
{
	static String getColumn(PresentationGroup pg, int c)
	{
		switch(c)
		{
		case 0: return pg.groupName;
		case 1: return pg.inheritsFrom == null ? "" : pg.inheritsFrom;
		case 2:
			return pg.lastModifyTime == null ? "" :
				Constants.defaultDateFormat.format(pg.lastModifyTime);
		case 3: return "" + pg.isProduction;
		default: return "";
		}
	}
}

class PGColumnComparator implements Comparator
{
	int col;

	PGColumnComparator(int col)
	{
		this.col = col;
	}

	public int compare(Object ob1, Object ob2)
	{
		if (ob1 == ob2)
			return 0;
		PresentationGroup pg1 = (PresentationGroup)ob1;
		PresentationGroup pg2 = (PresentationGroup)ob2;
		return PGColumnizer.getColumn(pg1, col).compareToIgnoreCase(
			PGColumnizer.getColumn(pg2, col));
	}
}
