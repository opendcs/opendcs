package decodes.gui;

import javax.swing.table.TableModel;

/**
  * This interface works with SortingListTable.
  * @deprecated Swing JTable sorts correctly with standard models.
  *             Models should not attempt to sort their data.
  */
@Deprecated
public interface SortingListTableModel extends TableModel
{
	/**
	  Overload to sort model rows by the selected column.
	  @param column the column number to sort by.
	*/
	public void sortByColumn(int column);

	/**
	  Return the object represented by the specified row.
	  @param row the row object to retrieve.
	*/
	public Object getRowObject(int row);
}
