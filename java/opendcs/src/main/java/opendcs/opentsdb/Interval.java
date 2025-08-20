package opendcs.opentsdb;

import java.util.Calendar;

import opendcs.dao.CachableDbObject;

import decodes.db.Constants;
import decodes.sql.DbKey;

/**
 * Bean class for storing an interval used for a time series interval or
 * duration. An interval is specified by a unique name (e.g. "1day").
 * It is associated with a calendar constant and a multiplier.
 * 
 * @author mmaloney Mike Maloney, Cove Software, LLC.
 */
public class Interval
	implements CachableDbObject
{
	private DbKey key = Constants.undefinedId;
	
	private String name = null;
	
	/** One of MINUTE, HOUR_OF_DAY, DAY_OF_MONTH, WEEK_OF_YEAR, MONTH, YEAR */
	private int calConstant = Calendar.HOUR_OF_DAY;
	
	private int calMultiplier = 1;

	/** Constructor used by xml parser */
	public Interval(String name)
	{
		this.name = name;
	}

	public Interval(DbKey key, String name, int calConstant, int calMultiplier)
	{
		super();
		this.key = key;
		this.name = name;
		this.calConstant = calConstant;
		this.calMultiplier = calMultiplier;
	}

	@Override
	public DbKey getKey()
	{
		return key;
	}

	@Override
	public String getUniqueName()
	{
		return name;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public int getCalConstant()
	{
		return calConstant;
	}

	public void setCalConstant(int calConstant)
	{
		this.calConstant = calConstant;
	}

	public int getCalMultiplier()
	{
		return calMultiplier;
	}

	public void setCalMultiplier(int calMultiplier)
	{
		this.calMultiplier = calMultiplier;
	}

	public void setKey(DbKey key)
	{
		this.key = key;
	}
}
