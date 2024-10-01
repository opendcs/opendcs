package decodes.gui;

import javax.swing.*;
import javax.swing.table.*;

/**
Static utility to adjust column widths.
*/
public class TableColumnAdjuster
{
	/**
	 * Adjusts widths of the table according to percentages that you supply
	 * in an array.
	 * @param table the JTable
	 * @param percent array of integers, one for each column.
	 */
	public static void adjustColumnWidths(JTable table, int[] percent)
	{
		int totalWidth = 0;
		TableColumn tc[] = new TableColumn[percent.length];

//		tableWidth = table.getWidth();
		// Get total width of all columns
		for(int i=0; i<percent.length; i++)
		{
			tc[i] = table.getColumn(table.getColumnName(i));
			totalWidth += tc[i].getWidth();
		}
		// Adjust each width to the specified percentage
//		for(int i=0; i<percent.length; i++)
		for(int i = percent.length-1; i >= 0; i--)
		{
			int w = (int)(totalWidth * (double)percent[i]/100.0);
			tc[i].setPreferredWidth(w);
			tc[i].setWidth(w);
		}
		
		
	}
}
