package decodes.tsdb.compedit;

import java.util.Comparator;

import javax.swing.table.TableModel;

import decodes.tsdb.CompAppInfo;

public class ProcessesListComparator implements Comparator<CompAppInfo>
{
	int column;
	Class<?> sortType;
    TableModel table;

	public ProcessesListComparator(int column, Class<?> sortType)
	{
		this.column = column;
		this.sortType = sortType;
	}

	/**
	 * Compare the eqMod names of the specified type.
	 */
	public int compare(CompAppInfo ds1, CompAppInfo ds2)
	{
		if (ds1 == ds2)
			return 0;

		Object s1 = ProcessesListPanelTableModel.getNlColumn(ds1, column);
		Object s2 = ProcessesListPanelTableModel.getNlColumn(ds2, column);
		if (sortType == Integer.class)
		{
			try
			{
				int i1 = (Integer)s1;
				int i2 = (Integer)s2;
				return i1 - i2;
			}
			catch(Exception ex)
			{
				/* fall through to string compare */
			}
		}
		else if (sortType == Long.class)
		{
			try
			{
				long i1 = (Long)s1;
				long i2 = (Long)s2;
				return (int)(i1 - i2);
			}
			catch(Exception ex)
			{
				/* fall through to string compare */
			}
		}

		return (s1.toString()).compareToIgnoreCase(s2.toString());
	}

	public boolean equals(Object ob)
	{
		return false;
	}
}