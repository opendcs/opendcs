package decodes.tsdb.algoedit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.ResourceBundle;

import javax.swing.table.AbstractTableModel;
import decodes.gui.SortingListTableModel;


/**
Model for the properties table in the algorithm wizard.
*/
class AlgoPropTableModel 
	extends AbstractTableModel implements SortingListTableModel
{
	private ResourceBundle labels = null;
	ArrayList<AlgoProp> theProps = new ArrayList<AlgoProp>();
	static String columnNames[] = null; 
	static int columnWidths[] = { 34, 33, 33 };

	public AlgoPropTableModel(AlgoData theData)
	{
		labels = AlgorithmWizard.getLabels();
		columnNames = new String[3];
		columnNames[0] = 
			labels.getString("AlgoPropTableModel.propNameColumn");
		columnNames[1] = 
			labels.getString("AlgoPropTableModel.javaTypeColumn");
		columnNames[2] = 
			labels.getString("AlgoPropTableModel.defaultColumn");
	}

	public AlgoProp findByName(String nm)
	{
		for(AlgoProp its : theProps)
			if (its.name.equalsIgnoreCase(nm))
				return its;
		return null;
	}


	public void sortByColumn(int c)
	{
		Collections.sort(theProps, new AlgoPropComparator(c));
		fireTableDataChanged();
	}

	public Object getRowObject(int arg0)
	{
		return theProps.get(arg0);
	}

	public int getRowCount()
	{
		return theProps.size();
	}

	public int getColumnCount()
	{
		return columnNames.length;
	}

	public void add(AlgoProp addme)
	{
		theProps.add(addme);
		fireTableDataChanged();
	}

	public void clear()
	{
		theProps.clear();
		fireTableDataChanged();
	}

	public void deleteAt(int index)
	{
		theProps.remove(index);
		fireTableDataChanged();
	}

	public String getColumnName(int col)
	{
		return columnNames[col];
	}

	public Object getValueAt(int rowIndex, int columnIndex)
	{
		if (theProps.get(rowIndex) != null)
			return getNlColumn(theProps.get(rowIndex), columnIndex);
		else
			return "";
	}

	public static String getNlColumn(AlgoProp obj, int columnIndex)
	{
		switch (columnIndex)
		{
		case 0:
			return obj.name;
		case 1:
			return obj.javaType;
		case 2:
			return obj.defaultValue;
		default:
			return "";
		}
	}
}

class AlgoPropComparator implements Comparator
{
	int column;

	public AlgoPropComparator(int column)
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
		AlgoProp ds1 = (AlgoProp) ob1;
		AlgoProp ds2 = (AlgoProp) ob2;

		String s1 = AlgoPropTableModel.getNlColumn(ds1, column);
		String s2 = AlgoPropTableModel.getNlColumn(ds2, column);

		return s1.compareToIgnoreCase(s2);
	}

	public boolean equals(Object ob)
	{
		return false;
	}
}
