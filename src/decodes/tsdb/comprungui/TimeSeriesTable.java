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

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;
import java.util.Vector;

import ilex.gui.ColumnGroup;
import ilex.gui.GroupableTableHeader;
import ilex.var.NoConversionException;
import ilex.var.TimedVariable;

import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import decodes.tsdb.CTimeSeries;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.util.DecodesSettings;

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
	}
	
	TimeSeriesTable(TimeSeriesTableModel newmodel,TimeSeriesDb newdb)
	{
		super(newmodel);
		this.autoCreateColumnsFromModel=true;
		mymodel=newmodel;
		mydb=newdb;
		mymodel=new TimeSeriesTableModel(mydb);
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
	
	protected JTableHeader createDefaultTableHeader()
	{
		return new GroupableTableHeader(columnModel);
	}
}

class TimeSeriesTableModel extends AbstractTableModel implements TableModel
{
	public Vector<CTimeSeries> inputs;
	public Vector<CTimeSeries> outputs;
	private CTimeSeries maxseries=null;
	TimeSeriesDb mydb;
	int rows;
	private DecimalFormat df1 = new DecimalFormat("###0.00");
	private ArrayList<Date> allTimes;
	private SimpleDateFormat sdf;
	
	/**
	 * Creates a new table model
	 * 
	 * @param rows the row count.
	 */
	public TimeSeriesTableModel(TimeSeriesDb newdb)
	{
		rows=0;
		inputs = new Vector<CTimeSeries>();
		outputs = new Vector<CTimeSeries>();
		mydb=newdb;
		allTimes = new ArrayList<Date>();
		String timeZoneStr = DecodesSettings.instance().sqlTimeZone;
		timeZoneStr = timeZoneStr == null ? "UTC" : timeZoneStr;
		sdf = new SimpleDateFormat("ddMMMyyyy, HH:mm");		
		sdf.setTimeZone(TimeZone.getTimeZone(timeZoneStr));
	}

	public void setDb(TimeSeriesDb newdb)
	{
		mydb=newdb;
	}

	public void setInOut(Vector<CTimeSeries> inputs,
							Vector<CTimeSeries> outputs)
	{
		allTimes.clear();

		this.inputs=inputs;
		this.outputs=outputs;

		// Build an aggregate list of all distinct times.
		for(CTimeSeries cts : inputs)
		{
			int n = cts.size();
			for(int i=0; i<n; i++)
			{
				TimedVariable tv = cts.sampleAt(i);
				if (tv == null)
					break;
				Date d = tv.getTime();
				if (!allTimes.contains(d))
					allTimes.add(d);
			}
		}
		for(CTimeSeries cts : outputs)
		{
			int n = cts.size();
			for(int i=0; i<n; i++)
			{
				TimedVariable tv = cts.sampleAt(i);
				if (tv == null)
					break;
				Date d = tv.getTime();
				if (!allTimes.contains(d))
					allTimes.add(d);
			}
		}
		Collections.sort(allTimes);
		rows = allTimes.size();

		this.fireTableChanged(
				new TableModelEvent(this,TableModelEvent.HEADER_ROW));
		this.fireTableDataChanged();
	}

	/**
	 * Returns the column count.
	 * 
	 * @return # columns to accomodate all time series plus time.
	 */
	public int getColumnCount()
	{
		return 3*inputs.size()+3*outputs.size()+1;
	}

	/**
	 * Returns the row count.
	 * 
	 * @return The row count.
	 */
	public int getRowCount()
	{
		return rows;
	}

	/**
	 * Returns the value at the specified cell in the table.
	 * 
	 * @param row the row index.
	 * @param column the column index.
	 * @return The value.
	 */
	public Object getValueAt(int row, int column)
	{
		if (row > allTimes.size())
			return "";

		Date d = allTimes.get(row);
		if (column == 0)
			return sdf.format(d);

		int tsIndex = (column - 1)/3;
		CTimeSeries cts = (tsIndex < inputs.size()) ?
			inputs.get(tsIndex) : 
			outputs.get(tsIndex - inputs.size());
		TimedVariable tv = cts.findWithin(d.getTime()/1000L, 1);
		if (tv == null)
			return "";

		switch ((column-1)%3)
		{
		case 0: // Value
			try { return df1.format(tv.getDoubleValue()); }
			catch (NoConversionException e)
			{
				return tv.getStringValue();
			}
		case 1:
			return convertLim(tv);
		case 2:
			return convertRev(tv);
		}
		return null;
	}

	private String convertLim(TimedVariable var)
	{
		if(mydb!=null)
			return mydb.flags2LimitCodes(var.getFlags());
		else
			return "";
	}
	
	private String convertRev(TimedVariable var)
	{
		if(mydb!=null)
			return mydb.flags2RevisionCodes(var.getFlags());
		else
			return "";
	}

	/**
	 * Returns the column name.
	 * 
	 * @param column
	 *            the column index.
	 * 
	 * @return The column name.
	 */
	public String getColumnName(int column)
	{
		if(column==0)
		{
			return CompRunGuiFrame.dateTimeColumnLabel;
		}
		int index = (column - 1)/3;
		switch ((column-1)%3)
		{
		case 0:
			CTimeSeries myseries;
			if(index>=inputs.size())
			{
				index=index-inputs.size();
				myseries = outputs.get(index);
			}
			else
			{
				myseries = inputs.get(index);
			}
			return myseries.getUnitsAbbr();
		case 1:
			return mydb.getLimitLabel();
		case 2:
			return mydb.getRevisionLabel();
		}
		return null;
	}
}
