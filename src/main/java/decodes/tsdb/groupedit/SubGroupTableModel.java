package decodes.tsdb.groupedit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import javax.swing.table.AbstractTableModel;

import decodes.gui.SortingListTableModel;
import decodes.tsdb.TsGroup;

public class SubGroupTableModel extends AbstractTableModel implements SortingListTableModel
{
	ArrayList<SubGroupReference> subgroups = new ArrayList<SubGroupReference>();
	static String colname[] = { "Group ID", "Name", "Type", "Description", "Combine" };
	static int colwidth[] = {  15, 15, 15, 40, 15 };
	int sortColumn = 0;

	public SubGroupTableModel()
	{
	}

	@Override
	public int getRowCount()
	{
		return subgroups.size();
	}

	@Override
	public int getColumnCount()
	{
		return colname.length;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		SubGroupReference ref = (SubGroupReference)getRowObject(rowIndex);
		if (ref == null)
			return "";
		
		return getColumn(ref, columnIndex);
	}
	
	public String getColumn(SubGroupReference ref, int columnIndex)
	{
		switch(columnIndex)
		{
		case 0: return ref.groupId.toString();
		case 1: return ref.groupName;
		case 2: return ref.groupType;
		case 3: return ref.groupDesc;
		case 4: return ref.combine;
		}
		return "";
	}

	@Override
	public void sortByColumn(int c)
	{
		sortColumn = c;
		Collections.sort(subgroups, 
			new Comparator<SubGroupReference>()
			{
				@Override
				public int compare(SubGroupReference ref1, SubGroupReference ref2)
				{
					int r = getColumn(ref1, sortColumn).compareTo(getColumn(ref2, sortColumn));
					if (r == 0)
						r = ref1.groupId.compareTo(ref2.groupId);
					return r;
				}
			});
		this.fireTableDataChanged();
	}
	
	public String getColumnName(int col)
	{
		return colname[col];
	}

	@Override
	public Object getRowObject(int row)
	{
		return row < subgroups.size() ? subgroups.get(row) : null;
	}
	
	public void removeAt(int row)
	{
		if (row < subgroups.size())
			subgroups.remove(row);
		fireTableDataChanged();
	}
	
	public void add(TsGroup grp, String combine)
	{
		for(Iterator<SubGroupReference> refit = subgroups.iterator(); refit.hasNext(); )
		{
			SubGroupReference ref = refit.next();
			if (ref.groupId.equals(grp.getGroupId()))
				refit.remove();
		}
		subgroups.add(new SubGroupReference(grp, combine));
		sortByColumn(sortColumn);
	}

}
