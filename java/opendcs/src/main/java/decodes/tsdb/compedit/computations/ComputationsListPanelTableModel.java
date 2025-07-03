package decodes.tsdb.compedit.computations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ResourceBundle;

import javax.swing.table.AbstractTableModel;

import decodes.gui.SortingListTableModel;
import decodes.tsdb.DbComputation;
import decodes.tsdb.compedit.CAPEdit;
import decodes.tsdb.compedit.ComputationsListPanel;

@SuppressWarnings("serial")
public class ComputationsListPanelTableModel extends AbstractTableModel
{
	private final ArrayList<DbComputation> comps = new ArrayList<>();

	private static ResourceBundle compLabels = CAPEdit.instance().compeditDescriptions;

	private final String[] columnNames;
	/**
	 * Constructor
	 */
	public ComputationsListPanelTableModel(String[] columnNames)
	{
		this.columnNames = columnNames;
	}
	

	public void setContents(Collection<DbComputation> comps)
	{
		this.comps.clear();
		this.comps.addAll(comps);
		fireTableDataChanged();
	}

	
	public Object getValueAt(int rowIndex, int columnIndex) 
	{
		DbComputation cil = comps.get(rowIndex);
		if (cil != null)
		{
			return getNlColumn(cil, columnIndex);
		}
		else
		{
			return "";
		}
	}

	public DbComputation getCompAt(int rowIndex)
	{
		return comps.get(rowIndex);
	}
		
	public static Object getNlColumn(DbComputation comp, int columnIndex) 
	{
		switch (columnIndex) {
		case 0:
			return comp.getId();
		case 1:
			return comp.getName();
		case 2:
		  {
			String s = comp.getAlgorithmName();
			return s != null ? s :
				compLabels.getString(
					"ComputationsFilterPanel.N1ColumnNull");
		  }
		case 3:
			return comp.getApplicationName();
		case 4:
			return comp.isEnabled();
		case 5:
			return comp.getComment();
		default:
			return "";
		}
	}

	public boolean compExists(String name)
	{
		for(DbComputation dc : comps)
		{
			if (name.equalsIgnoreCase(dc.getName()))
				return true;
		}
		return false;
	}


	@Override
	public int getRowCount()
	{
		return this.comps.size();
	}


	@Override
	public int getColumnCount()
	{
		return this.columnNames.length;
	}

	@Override
	public String getColumnName(int col)
	{
		return columnNames[col];
	}
}