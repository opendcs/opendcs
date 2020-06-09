/*
*  $Id$
*/
package decodes.dbeditor;

import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import java.util.Iterator;
import java.util.Collections;
import java.util.Comparator;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.util.ResourceBundle;

import ilex.util.LoadResourceBundle;

import decodes.db.*;
import decodes.gui.*;

/**
Panel for selecting an equipment model.
*/
public class EquipmentModelSelectPanel extends JPanel
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

	BorderLayout borderLayout1 = new BorderLayout();
	JScrollPane jScrollPane1 = new JScrollPane();
	EquipmentModelSelectTableModel model;
	SortingListTable eqTable;

	/** No args constructor for JBuilder: */
	public EquipmentModelSelectPanel()
	{
		this(null);
	}

	/**
	  Constructor.
	  @param type if not null, only show Equipement records of this type.
	*/
	public EquipmentModelSelectPanel(String type)
	{
	    model = new EquipmentModelSelectTableModel(this, type);
		eqTable = new SortingListTable(model, null);
		eqTable.getSelectionModel().setSelectionMode(
			ListSelectionModel.SINGLE_SELECTION);

		eqTable.addMouseListener(new MouseAdapter(){
			public void mouseClicked(MouseEvent e){
				if (e.getClickCount() == 2){
	       				((EquipmentListPanel)EquipmentModelSelectPanel.this.getParent()).openPressed();

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

	/** GUI component initialization. */
	private void jbInit() throws Exception
	{
		this.setLayout(borderLayout1);
		jScrollPane1.setPreferredSize(new Dimension(453, 300));
        this.add(jScrollPane1, BorderLayout.CENTER);
		jScrollPane1.getViewport().add(eqTable, null);
	}

	/**
	 * Returns the currently-selected eqMod, or null if no eqMod is selected.
	 */
	public EquipmentModel getSelectedEquipmentModel()
	{
		int r = eqTable.getSelectedRow();
		if (r == -1)
			return null;
		return model.getEquipmentModelAt(r);
	}

	/** Refresh list from database. */
	public void refill()
	{
		model.refill();
		invalidate();
		repaint();
	}

	/**
	  Adds an equipment model to the list.
	  @param eqMod the object to add.
	*/
	public void addEquipmentModel(EquipmentModel eqMod)
	{
		model.addEquipmentModel(eqMod);
		invalidate();
		repaint();
	}

	/** Deletes the selected object. */
	public void deleteSelectedEquipmentModel()
	{
		int r = eqTable.getSelectedRow();
		if (r == -1)
			return;
		model.deleteEquipmentModelAt(r);
	}

	/** Clears any selections made. */
	public void clearSelection()
	{
		eqTable.clearSelection();
	}

	/**
	  Called from an Equipment Model editor tab after commit.
	  Replaces the old copy with the new one.
	  @param oldOb the old object.
	  @param newOb the new object.
	*/
	public void replace(EquipmentModel oldOb, EquipmentModel newOb)
	{
		model.replace(oldOb, newOb);
	}

	/**
	  Sets the current selection.
	  @param mod the selection.
	*/
	public void setSelection(EquipmentModel mod)
	{
		for(int i=0; i<model.getRowCount(); i++)
			if (mod.equals(model.getEquipmentModelAt(i)))
			{
				eqTable.setRowSelectionInterval(i, i);
				Rectangle rect = eqTable.getCellRect(i, 0, true);
				eqTable.scrollRectToVisible(rect);
				break;
			}
	}
}

class EquipmentModelSelectTableModel extends AbstractTableModel
	implements SortingListTableModel
{
	private String colNames[] = 
	{
		EquipmentModelSelectPanel.genericLabels.getString("name"),
		EquipmentModelSelectPanel.genericLabels.getString("type"),
		EquipmentModelSelectPanel.dbeditLabels.getString(
			"EquipmentModelSelectPanel.company"),
		EquipmentModelSelectPanel.genericLabels.getString("model"),
		EquipmentModelSelectPanel.genericLabels.getString("description")
	};
	private EquipmentModelSelectPanel panel;
	private Vector eqMods;
	private String eqModelType;
	private int lastSortColumn = -1;

	public EquipmentModelSelectTableModel(EquipmentModelSelectPanel ssp,
		String type)
	{
		super();
		this.panel = ssp;
		eqMods = new Vector();
		eqModelType = type;
		refill();
                this.sortByColumn(0);
	}

	void refill()
	{
		eqMods.clear();
		for(Iterator it = Database.getDb().equipmentModelList.iterator();
			it.hasNext(); )
		{
			EquipmentModel eqMod = (EquipmentModel)it.next();
			if (eqModelType != null
			 && eqModelType.equalsIgnoreCase(eqMod.equipmentType))
				continue;
			eqMods.add(eqMod);
		}
	}

	void addEquipmentModel(EquipmentModel eqMod)
	{
		for(int i=0; i<eqMods.size(); i++)
		{
			EquipmentModel em = (EquipmentModel)eqMods.elementAt(i);
			if (em.getName().equals(eqMod.getName()))
				return;
		}
		eqMods.add(eqMod);
		fireTableDataChanged();
	}

	void deleteEquipmentModelAt(int index)
	{
		EquipmentModel eqMod = (EquipmentModel)eqMods.elementAt(index);
		eqMods.remove(index);
		try { Database.getDb().getDbIo().deleteEquipmentModel(eqMod); }
		catch(DatabaseException e)
		{
			TopFrame.instance().showError(e.toString());
		}
		Database.getDb().equipmentModelList.remove(eqMod);
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
		return eqMods.size();
	}

	public Object getValueAt(int r, int c)
	{
		EquipmentModel eqMod = (EquipmentModel)eqMods.elementAt(r);
		switch(c)
		{
		case 0: return eqMod.name;
		case 1: return eqMod.equipmentType;
		case 2: return eqMod.company;
		case 3: return eqMod.model;
		case 4: return eqMod.description;
		default: return "";
		}
	}

	EquipmentModel getEquipmentModelAt(int r)
	{
		return (EquipmentModel)getRowObject(r);
	}

	public Object getRowObject(int r)
	{
		return eqMods.elementAt(r);
	}

	public void sortByColumn(int c)
	{
		lastSortColumn = c;
		Collections.sort(eqMods, new EquipmentModelNameComparator(c));
	}

	void replace(DatabaseObject oldOb, DatabaseObject newOb)
	{
		eqMods.remove(oldOb);
		eqMods.add(newOb);
		if (lastSortColumn != -1)
			sortByColumn(lastSortColumn);
		else
			fireTableDataChanged();
	}

}

/**
 * Used for sorting and searching for eqMod names.
 */
class EquipmentModelNameComparator implements Comparator
{
	int column;

	public EquipmentModelNameComparator(int column)
	{
		this.column = column;
	}

	/**
	 * Compare the eqMod names of the specified type.
	 */
	public int compare(Object ob1, Object ob2)
	{
		if (ob1 == ob2)
			return 0;
		EquipmentModel eqm1 = (EquipmentModel)ob1;
		EquipmentModel eqm2 = (EquipmentModel)ob2;

		String desc1 = eqm1.description;
		String desc2 = eqm2.description;
		if (desc1 == null) desc1 = "";
		if (desc2 == null) desc2 = "";

		switch(column)
		{
		case 0:
			return eqm1.name.compareToIgnoreCase(eqm2.name);
		case 1:
			return eqm1.equipmentType.compareToIgnoreCase(eqm2.equipmentType);
		case 2:
			return eqm1.company.compareToIgnoreCase(eqm2.company);
		case 3:
			return eqm1.model.compareToIgnoreCase(eqm2.model);
		case 4:
			return desc1.compareToIgnoreCase(desc2);
		default:
			return 0;
		}
	}

	public boolean equals(Object ob)
	{
		return false;
	}
}
