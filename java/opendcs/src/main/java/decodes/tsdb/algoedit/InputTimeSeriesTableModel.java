package decodes.tsdb.algoedit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.ResourceBundle;

import javax.swing.table.AbstractTableModel;

import decodes.tsdb.algo.RoleTypes;
import decodes.gui.SortingListTableModel;


/**
Model for the input time series table in the algorithm wizard.
*/
class InputTimeSeriesTableModel extends AbstractTableModel implements
		SortingListTableModel
{
	private ResourceBundle labels = null;
	ArrayList<InputTimeSeries> myvector = new ArrayList<InputTimeSeries>();
	static String columnNames[] = null; 
	static int columnWidths[] = { 34, 33, 33 };

	public InputTimeSeriesTableModel()
	{
		labels = AlgorithmWizard.getLabels();
		columnNames = new String[3];
		columnNames[0] = 
			labels.getString("InputTimeSeriesTableModel.roleNameColumn");
		columnNames[1] = 
			labels.getString("InputTimeSeriesTableModel.javaTypeColumn");
		columnNames[2] = 
			labels.getString("InputTimeSeriesTableModel.parmInTypeCodeColumn");		
	}
	
	public void sortByColumn(int c)
	{
		Collections.sort(myvector, new InputTimeSeriesComparator(c));
		fireTableDataChanged();
	}

	public InputTimeSeries findByName(String nm)
	{
		for(InputTimeSeries its : myvector)
			if (its.roleName.equalsIgnoreCase(nm))
				return its;
		return null;
	}

	public Object getRowObject(int arg0)
	{
		return myvector.get(arg0);
	}

	public int getRowCount()
	{
		return myvector.size();
	}

	public int getColumnCount()
	{
		return columnNames.length;
	}

	public void add(InputTimeSeries addme)
	{
		myvector.add(addme);
		fireTableDataChanged();
	}

	public InputTimeSeries getInputTimeSeries(int index)
	{
		return myvector.get(index);
	}

	public void clear()
	{
		myvector.clear();
		fireTableDataChanged();
	}

	public void deleteInputTimeSeries(int index)
	{
		myvector.remove(index);
		fireTableDataChanged();
	}

	public void fill()
	{
		fireTableDataChanged();
	}

	public String getColumnName(int col)
	{
		return columnNames[col];
	}

	public Object getValueAt(int rowIndex, int columnIndex)
	{
		if (myvector.get(rowIndex) != null)
			return getNlColumn(myvector.get(rowIndex), columnIndex);
		else
			return "";
	}

	public static String getNlColumn(InputTimeSeries obj, int columnIndex)
	{
		switch (columnIndex)
		{
		case 0:
			return obj.roleName;
		case 1:
			return obj.javaType;
		case 2:
		  {
			int idx = RoleTypes.getIndex(obj.roleTypeCode);
			return idx == -1 ? obj.roleTypeCode 
				: RoleTypes.getRoleType(idx);
		  }
		default:
			return "";
		}
	}
}

class InputTimeSeriesComparator implements Comparator
{
	int column;

	public InputTimeSeriesComparator(int column)
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
		InputTimeSeries ds1 = (InputTimeSeries) ob1;
		InputTimeSeries ds2 = (InputTimeSeries) ob2;

		String s1 = InputTimeSeriesTableModel.getNlColumn(ds1, column);
		String s2 = InputTimeSeriesTableModel.getNlColumn(ds2, column);

		return s1.compareToIgnoreCase(s2);
	}

	public boolean equals(Object ob)
	{
		return false;
	}
}
