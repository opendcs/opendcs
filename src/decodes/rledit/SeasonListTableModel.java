/*
*  $Id$
*
*  $Log$
*
 * This software was written by Cove Software, LLC ("COVE") under contract
 * to the United States Government. No warranty is provided or implied other
 * than specific contractual terms between COVE and the U.S. Government.
 *
 * Copyright 2014 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * All rights reserved.
*/
package decodes.rledit;

import ilex.util.Logger;

import javax.swing.table.*;

import java.util.*;

import decodes.db.*;
import decodes.decoder.FieldParseException;
import decodes.decoder.Season;
import decodes.gui.*;

/**
Table Model for a DECODES Enumeration. The table will show all of the
values defined in the database for a specific Enum.
*/
@SuppressWarnings("serial")
public class SeasonListTableModel extends AbstractTableModel
	implements SortingListTableModel
{
	private static ResourceBundle labels = RefListEditor.getLabels();
	
	private ArrayList<Season> seasons = new ArrayList<Season>();

	/** Column headers. */
	private String columnNames[] = new String[5];
	static int colWidths[] = { 15, 35, 20, 20, 10 };

	/** Constructor. */
	public SeasonListTableModel()
	{
		columnNames[0] = labels.getString("SeasonsTab.abbr");
		columnNames[1] = labels.getString("SeasonsTab.name");
		columnNames[2] = labels.getString("SeasonsTab.start");
		columnNames[3] = labels.getString("SeasonsTab.end");
		columnNames[4] = labels.getString("SeasonsTab.tz");

		DbEnum seasonEnum = Database.getDb().enumList.getEnum(Constants.enum_Season);
		if (seasonEnum == null)
		{
			Logger.instance().info("Season enum missing, will create.");
			seasonEnum = new DbEnum(Constants.enum_Season);
		}
		seasonEnum.sort();
		for(Iterator<EnumValue> evit = seasonEnum.iterator(); evit.hasNext(); )
		{
			EnumValue ev = evit.next();
			Season season = new Season();
			try
			{
				season.setFromEnum(ev);
				seasons.add(season);
			}
			catch(FieldParseException ex)
			{
				Logger.instance().warning("Bad enum value '" + ev.getValue() 
					+ "' in Season enum: " + ex + " -- skipped.");
			}
		}
	}
	
	public void storeBackToEnum()
	{
		DbEnum seasonEnum = Database.getDb().enumList.getEnum(Constants.enum_Season);
		seasonEnum.clear();
		int sortNum = 0;
		for(Season season : seasons)
		{
			EnumValue ev = new EnumValue(seasonEnum, season.getAbbr());
			season.saveToEnum(ev);
			ev.setSortNumber(sortNum++);
			seasonEnum.addValue(ev);
		}
	}

	/** @return number of enum values (rows). */
	public int getRowCount()
	{
		return seasons.size();
	}

	/** @return number of enum columns (constant). */
	public int getColumnCount() { return columnNames.length; }
	
	/**
	 * Return String value at specified row/column.
	 * @param row the row
	 * @param column the column
	 * @return String value
	 */
	public Object getValueAt(int row, int column)
	{
		Season season = (Season)getRowObject(row);
		switch(column)
		{
		case 0: return season.getAbbr();
		case 1: return season.getName();
		case 2: return season.getStart();
		case 3: return season.getEnd();
		case 4: return season.getTz() == null ? "" : season.getTz();
		default: return "";
		}
	}

	/**
	 * Return column name.
	 * @param col column number
	 * @return column name
	 */
	public String getColumnName(int col)
	{
		return columnNames[col];
	}

	/**
	 * Does nothing -- we don't sort enum values, the user controls the
	 * order directly.
	 * @param column ignored.
	 */
	public void sortByColumn(int column)
	{
	}

	/**
	 * Return EnumValue object at specified row.
	 * @param row the row
	 * @return EnumValue object at specified row
	 */
	public Object getRowObject(int row)
	{
		return seasons.get(row);
	}

	/**
	 * Move the specified row up one.
	 * @param row the row
	 */
	public boolean moveUp(int row)
	{
		if (row <= 0 || row >= seasons.size())
			return false;
		Season s = seasons.get(row);
		Season p = seasons.get(row-1);
		seasons.set(row-1, s);
		seasons.set(row, p);
		fireTableDataChanged();
		return true;
	}

	/**
	 * Move the specified row down one.
	 * @param row the row
	 */
	public boolean moveDown(int row)
	{
		if (row < 0 || row >= seasons.size()-1)
			return false;
		Season s = seasons.get(row);
		Season n = seasons.get(row+1);
		seasons.set(row+1, s);
		seasons.set(row, n);
		fireTableDataChanged();
		return true;
	}
	
	public void add(Season season)
	{
		seasons.add(season);
		fireTableDataChanged();
	}
	
	public void deleteAt(int row)
	{
		if (row >= 0 && row < seasons.size())
			seasons.remove(row);
		fireTableDataChanged();
	}
}
