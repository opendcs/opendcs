package decodes.tsdb.compedit;

import decodes.tsdb.DbCompParm;
import java.util.Comparator;

public class ComputationsEditComparator implements Comparator<DbCompParm>
{
	int column;
	CompParmTableModel model;

	public ComputationsEditComparator(int column, CompParmTableModel model)
	{
		this.column = column;
		this.model = model;
	}

	/**
	 * Compare the eqMod names of the specified type.
	 */
	public int compare(DbCompParm ds1, DbCompParm ds2)
	{
		if (ds1 == ds2)
			return 0;

		if (column == -1) // Initial sorting
		{
			if (ds1.isInput() && ds2.isOutput())
				return -1;
			else if (ds1.isOutput() && ds2.isInput())
				return 1;
			String s1 = model.getNlColumn(ds1, 0);
			String s2 = model.getNlColumn(ds2, 0);
			return s1.compareToIgnoreCase(s2);
		}

		String s1 = model.getNlColumn(ds1, column);
		String s2 = model.getNlColumn(ds2, column);

		return s1.compareToIgnoreCase(s2);
	}

	public boolean equals(Object ob) {
		return false;
	}
}
