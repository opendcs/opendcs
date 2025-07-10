package decodes.tsdb.comprungui;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;
import java.util.Vector;

import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import decodes.tsdb.CTimeSeries;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.VarFlags;
import decodes.util.DecodesSettings;
import ilex.var.NoConversionException;
import ilex.var.TimedVariable;

class TimeSeriesTableModel extends AbstractTableModel
{
	public Vector<CTimeSeries> inputs;
	public Vector<CTimeSeries> outputs;
	private CTimeSeries maxseries=null;
	TimeSeriesDb mydb;
	int rows;
	private DecimalFormat df1 = new DecimalFormat("###0.00");
	private ArrayList<Date> allTimes;
	public static final SimpleDateFormat sdf = new SimpleDateFormat("ddMMMyyyy, HH:mm");
	
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
				// Don't display values that are flagged for deletion.
				if (VarFlags.mustDelete(tv))
					continue;
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
				// Don't display values that are flagged for deletion.
				if (VarFlags.mustDelete(tv))
					continue;
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
		if (tv == null || VarFlags.mustDelete(tv))
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