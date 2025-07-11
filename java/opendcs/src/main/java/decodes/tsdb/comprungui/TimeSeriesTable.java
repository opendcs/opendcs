/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. 
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*
*  $Log$
*  Revision 1.2  2017/08/22 19:57:35  mmaloney
*  Refactor
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.6  2012/09/17 20:32:16  mmaloney
*  dev
*
*  Revision 1.5  2012/09/17 15:23:51  mmaloney
*  Tabular display should be unique string, not display name.
*
*  Revision 1.4  2012/07/30 20:29:34  mmaloney
*  Cosmetic gui improvements.
*
*  Revision 1.3  2008/11/20 18:49:38  mjmaloney
*  merge from usgs mods
*
*/
package decodes.tsdb.comprungui;

import java.util.Vector;

import ilex.gui.ColumnGroup;
import ilex.gui.GroupableTableHeader;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import decodes.tsdb.CTimeSeries;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;

@SuppressWarnings("serial")
public class TimeSeriesTable extends JTable
{

	public TimeSeriesTable(TimeSeriesDb newdb)
	{
		super();
		this.autoCreateColumnsFromModel=true;
		
		setModel(new TimeSeriesTableModel(newdb));
	}
	
	TimeSeriesTable(TimeSeriesTableModel newmodel,TimeSeriesDb newdb)
	{
		super(newmodel);
		this.autoCreateColumnsFromModel=false;
		setModel(newmodel);
		setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
	}	

	@Override
	public void tableChanged(TableModelEvent e)
	{

		TableColumnModel cm = this.getColumnModel();
		GroupableTableHeader header = (GroupableTableHeader)this.getTableHeader();
		TimeSeriesTableModel mymodel = (TimeSeriesTableModel)e.getSource();
		
		System.out.println("Columns" + cm.getColumnCount());
		int[] selectedRows = null;
		if (e.getType() == TableModelEvent.UPDATE)
		{
			selectedRows = selectionModel.getSelectedIndices();
			while (cm.getColumnCount() > 0)
			{
				cm.removeColumn(cm.getColumn(0));
			}

			for(int pos=0;pos<mymodel.inputs.size();pos++)
			{
				CTimeSeries cts = mymodel.inputs.get(pos);
				TimeSeriesIdentifier tsid = cts.getTimeSeriesIdentifier();
				String tsName = tsid != null ? tsid.getUniqueString() : cts.getDisplayName();
				ColumnGroup input = new ColumnGroup(CompRunGuiFrame.inputLabel + tsName);
				TableColumn value = new TableColumn(pos*3+1);
				value.setMinWidth(30);
				value.setHeaderValue(mymodel.getColumnName(1));
				TableColumn limQual = new TableColumn(pos*3+2);
				limQual.setHeaderValue(mymodel.getColumnName(2));
				limQual.setMinWidth(30);
				TableColumn rev = new TableColumn(pos*3+3);
				rev.setHeaderValue(mymodel.getColumnName(3));
				rev.setMinWidth(30);
				input.add(value);
				input.add(limQual);
				input.add(rev);
				cm.addColumn(value);
				cm.addColumn(limQual);
				cm.addColumn(rev);
				header.addColumnGroup(input);
			}
			for(int pos=mymodel.inputs.size(); pos<mymodel.outputs.size()+mymodel.inputs.size();pos++)
			{
				CTimeSeries cts = mymodel.outputs.get(pos-mymodel.inputs.size());
				TimeSeriesIdentifier tsid = cts.getTimeSeriesIdentifier();
				String tsName = tsid != null ? tsid.getUniqueString() : cts.getDisplayName();

				ColumnGroup output = new ColumnGroup(CompRunGuiFrame.outputLabel + tsName);
				TableColumn value = new TableColumn(pos*3+1);
				value.setMinWidth(30);
				value.setHeaderValue(mymodel.getColumnName(1));
				TableColumn limQual = new TableColumn(pos*3+2);
				limQual.setHeaderValue(mymodel.getColumnName(2));
				limQual.setMinWidth(30);
				TableColumn rev = new TableColumn(pos*3+3);
				rev.setHeaderValue(mymodel.getColumnName(3));
				rev.setMinWidth(30);
				//add columns  from outputs to column group and set minimum size
				output.add(value);
				output.add(limQual);
				output.add(rev);
				cm.addColumn(value);
				cm.addColumn(limQual);
				cm.addColumn(rev);
				header.addColumnGroup(output);
			}
		}
		super.tableChanged(e);
		if (selectedRows != null && selectedRows.length > 0)
		{
			for(int i: selectedRows)
			{
				selectionModel.addSelectionInterval(i, i);
			}
		}
	}

	public void setInOut(Vector<CTimeSeries> inputs,
							Vector<CTimeSeries> outputs)
	{
		((TimeSeriesTableModel)getModel()).setInOut(inputs,outputs);
	}
	
	

	@Override
	protected JTableHeader createDefaultTableHeader()
	{
		return new GroupableTableHeader(super.getColumnModel());
	}
}
