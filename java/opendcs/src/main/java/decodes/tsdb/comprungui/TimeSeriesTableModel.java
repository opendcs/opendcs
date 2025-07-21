package decodes.tsdb.comprungui;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.function.Function;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import decodes.tsdb.CTimeSeries;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.VarFlags;
import decodes.util.DecodesSettings;
import ilex.var.NoConversionException;
import ilex.var.TimedVariable;

public final class TimeSeriesTableModel extends AbstractTableModel
{
	public final Vector<CTimeSeries> inputs;
	public final Vector<CTimeSeries> outputs;
	int rows;
	private DecimalFormat df1 = new DecimalFormat("###0.00");
	private final ArrayList<Date> allTimes;
	public static final SimpleDateFormat sdf = new SimpleDateFormat("ddMMMyyyy, HH:mm");

	private ColumnInfo limitColumn = new ColumnInfo("", v -> null);
	private ColumnInfo revColumn = new ColumnInfo("", v -> null);
	
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
		allTimes = new ArrayList<Date>();
		String timeZoneStr = DecodesSettings.instance().sqlTimeZone;
		timeZoneStr = timeZoneStr == null ? "UTC" : timeZoneStr;
		
		sdf.setTimeZone(TimeZone.getTimeZone(timeZoneStr));
	}

	public void addInput(CTimeSeries input)
	{
		this.inputs.add(input);
		updateTimeList();
	}

	public void addOutput(CTimeSeries output)
	{
		this.outputs.add(output);
		updateTimeList();
	}

	public void clearTimesSeries()
	{
		allTimes.clear();
		inputs.clear();
		outputs.clear();
		updateTimeList();
	}

	private void updateTimeList()
	{
		allTimes.clear();
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

		this.fireTableDataChanged();
	}

	public void setInOut(Vector<CTimeSeries> inputs,
							Vector<CTimeSeries> outputs)
	{
		

		this.inputs.clear();
		this.inputs.addAll(inputs);
		this.outputs.clear();
		this.outputs.addAll(outputs);

		updateTimeList();
	}

	/**
	 * Returns the column count.
	 * 
	 * @return # columns to accomodate all time series plus time.
	 */
	public int getColumnCount()
	{
		return 3*inputs.size()+3*outputs.size();
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
		{
			return sdf.format(d);
		}
		int tsIndex = (column - 1)/3;
		CTimeSeries cts = (tsIndex < inputs.size()) ? inputs.get(tsIndex) : outputs.get(tsIndex - inputs.size());
		TimedVariable tv = cts.findWithin(d.getTime()/1000L, 1);
		if (tv == null || VarFlags.mustDelete(tv))
		{
			return "";
		}

		switch ((column-1)%3)
		{
			case 0: // Value
				try { return df1.format(tv.getDoubleValue()); }
				catch (NoConversionException e)
				{
					return tv.getStringValue();
				}
			case 1:
				return limitColumn.valueLookup.apply(tv);
			case 2:
				return revColumn.valueLookup.apply(tv);
		}
		return null;
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
				return limitColumn.name;
			case 2:
				return revColumn.name;
		}
		return null;
	}

	public void setLimitColumnInfo(ColumnInfo info)
	{
		this.limitColumn = info;
	}

	public void setRevColumnInfo(ColumnInfo info)
	{
		this.revColumn = info;
	}


	public static class ColumnInfo
	{
		public final String name;
		public final Function<TimedVariable, String> valueLookup;


		public ColumnInfo(String name, Function<TimedVariable, String> lookupFunction)
		{
			this.name = name;
			this.valueLookup = lookupFunction;
		}
	}
}