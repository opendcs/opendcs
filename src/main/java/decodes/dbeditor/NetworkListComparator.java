package decodes.dbeditor;

import decodes.db.NetworkList;
import java.util.Comparator;

public class NetworkListComparator implements Comparator<NetworkList>
{
	int column;

	public NetworkListComparator(int column)
	{
		this.column = column;
	}

	/**
	 * Compare the eqMod names of the specified type.
	 */
	public int compare(NetworkList ds1, NetworkList ds2)
	{
		if (ds1 == ds2)
			return 0;
		String s1 = NetlistListTableModel.getNlColumn(ds1, column);
		String s2 = NetlistListTableModel.getNlColumn(ds2, column);

		return s1.compareToIgnoreCase(s2);
	}

	public boolean equals(Object ob)
	{
		return false;
	}
}
