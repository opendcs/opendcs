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
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;

import decodes.tsdb.CTimeSeries;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;

@SuppressWarnings("serial")
public class TimeSeriesTable extends JTable
{
	TimeSeriesTableModel mymodel;
	TimeSeriesDb mydb;
	TimeSeriesTable(TimeSeriesDb newdb)
	{
		super();
		this.autoCreateColumnsFromModel=true;
		mydb=newdb;
		mymodel=new TimeSeriesTableModel(mydb);
		setModel(mymodel);
	}
	
	TimeSeriesTable(TimeSeriesTableModel newmodel,TimeSeriesDb newdb)
	{
		super(newmodel);
		this.autoCreateColumnsFromModel=true;
		mymodel=newmodel;
		mydb=newdb;
		mymodel=new TimeSeriesTableModel(mydb);
		setModel(mymodel);
	}
	
	/**
	 * Method to set the database obj in case the initial db is null
	 * when set at the constructor
	 * @param newdb
	 */
	public void setTsdb(TimeSeriesDb newdb)
	{
		mydb = newdb;
		mymodel.setDb(newdb);  
	}

	public void setInOut(Vector<CTimeSeries> inputs,
							Vector<CTimeSeries> outputs)
	{
		this.setModel(mymodel);
		mymodel.setInOut(inputs,outputs);
		
		TableColumnModel cm = this.getColumnModel();
		
		cm.getColumn(0).setMinWidth(120);
		for(int pos=0;pos<mymodel.inputs.size();pos++)
		{
			CTimeSeries cts = mymodel.inputs.get(pos);
			TimeSeriesIdentifier tsid = cts.getTimeSeriesIdentifier();
			String tsName = tsid != null ? tsid.getUniqueString() : cts.getDisplayName();
			ColumnGroup input = 
				new ColumnGroup(CompRunGuiFrame.inputLabel + tsName);
			cm.getColumn((pos)*3+1).setMinWidth(30);
			input.add(cm.getColumn((pos)*3+1));
			cm.getColumn((pos)*3+2).setMinWidth(30);
			input.add(cm.getColumn((pos)*3+2));
			cm.getColumn((pos)*3+3).setMinWidth(30);
			input.add(cm.getColumn((pos)*3+3));
			GroupableTableHeader header = (GroupableTableHeader) this
				.getTableHeader();
			header.addColumnGroup(input);
		}
		for(int pos=mymodel.inputs.size();
						pos<mymodel.outputs.size()+mymodel.inputs.size();pos++)
		{
			CTimeSeries cts = mymodel.outputs.get(pos-mymodel.inputs.size());
			TimeSeriesIdentifier tsid = cts.getTimeSeriesIdentifier();
			String tsName = tsid != null ? tsid.getUniqueString() : cts.getDisplayName();

			ColumnGroup output = 
						new ColumnGroup(CompRunGuiFrame.outputLabel + tsName);

			//add columns  from outputs to column group and set minimum size
			cm.getColumn((pos)*3+1).setMinWidth(30);
			output.add(cm.getColumn((pos)*3+1));     
			cm.getColumn((pos)*3+2).setMinWidth(30);
			output.add(cm.getColumn((pos)*3+2));
			cm.getColumn((pos)*3+3).setMinWidth(30);
			output.add(cm.getColumn((pos)*3+3));
			GroupableTableHeader header = (GroupableTableHeader) this
				.getTableHeader();
			header.addColumnGroup(output);
		}
	}
	
	@Override
	protected JTableHeader createDefaultTableHeader()
	{
		return new GroupableTableHeader(columnModel);
	}
}
