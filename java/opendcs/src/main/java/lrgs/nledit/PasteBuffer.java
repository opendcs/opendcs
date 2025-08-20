package lrgs.nledit;

import java.util.Vector;

public class PasteBuffer
{
	static private Vector buf = new Vector(); // Singleton buffer

    public PasteBuffer()
	{
    }

	public void cut(NetworkListTable table)
	{
		buf.clear();
		int n = table.getRowCount();
		int selected[] = new int[table.getSelectedRowCount()];
		int si = 0;
		for(int i = 0; i < n; i++)
			if (table.isRowSelected(i))
			{
				buf.add(table.getItemAt(i)); // Add the item, not a copy.
				selected[si++] = i;
		    }
		// Remove in reverse order
		while(--si >= 0)
				table.deleteItemAt(selected[si]);
		table.invalidate();
		table.repaint();
	}

	public void copy(NetworkListTable table)
	{
		buf.clear();
		int n = table.getRowCount();
		for(int i = 0; i < n; i++)
			if (table.isRowSelected(i))
				buf.add(new lrgs.common.NetworkListItem(table.getItemAt(i)));
	}

	public void paste(NetworkListTable table)
	{
		// Find the first selected item in the table.
		int n = table.getRowCount();
		int ins = -1;
		for(int i = 0; i < n; i++)
			if (table.isRowSelected(i))
			{
				ins = i;
				break;
			}
		if (ins == -1) // nothing selected, add to end.
			table.model.addItems(buf);
		else
			table.model.addItemsAt(ins, buf);
//		table.invalidate();
//		table.repaint();
	}
}