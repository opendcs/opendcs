/**
 * $Id$
 * 
 * Copyright 2015 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * 
 * $Log$
 * Revision 1.9  2019/04/22 18:13:53  mmaloney
 * dev
 *
 * Revision 1.8  2019/04/22 17:42:12  mmaloney
 * dev
 *
 * Revision 1.7  2016/03/24 19:01:49  mmaloney
 * Added debug.
 *
 * Revision 1.6  2016/03/09 16:45:52  mmaloney
 * Null Ptr Checks
 *
 * Revision 1.5  2015/11/12 15:17:13  mmaloney
 * Added HEC headers.
 *
 */
package decodes.cwms.validation;

import ilex.util.Logger;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import opendcs.dao.CachableDbObject;
import decodes.db.EngineeringUnit;
import decodes.db.NoConversionException;
import decodes.db.UnitConverter;
import decodes.sql.DbKey;

/**
 * Represents a named set of screening-criteria for CWMS.
 */
public class Screening
	implements CachableDbObject
{
	/** surrogate database key */
	private DbKey screeningCode = DbKey.NullKey;
	
	/**
	 * Unique name of this screening set. For DATCHK screenings,
	 * the unique 6-part CWMS time-series identifier is used.
	 * For CWMS, this is an arbitrary name that may be shared by
	 * multiple time-series.
	 */
	private String screeningName = null;
	
	/** Description */
	private String screeningDesc = null;
	
	/** The units that the checks are done in */
	private String checkUnitsAbbr = null;
	
	ArrayList<ScreeningCriteria> criteriaSeasons = 
		new ArrayList<ScreeningCriteria>();
	
	/** When read from CWMS database, paramId will always be present */
	private String paramId = null;
	
	/** In a CWMS database, this can be set to act as a filter on candidate time series. */
	private String paramTypeId = "Inst";
	
	/** In a CWMS database, this can be set to act as a filter on candidate time series. */
	private String durationId = "1Day";
	
	private boolean rangeActive = true;
	private boolean rocActive = true;
	private boolean constActive = true;
	private boolean durMagActive = true;

	public Screening()
	{
	}
	
	/**
	 * Constructor
	 * @param screeningCode surrogate database key
	 * @param screeningName Unique name of this screening set
	 * @param screeningDesc Description
	 * @param checkUnitsAbbr units that the checks are done in
	 */
	public Screening(DbKey screeningCode, String screeningName,
			String screeningDesc, String checkUnitsAbbr)
	{
		super();
		this.screeningCode = screeningCode;
		this.screeningName = screeningName;
		this.screeningDesc = screeningDesc;
		this.checkUnitsAbbr = checkUnitsAbbr;
	}

	public DbKey getScreeningCode()
	{
		return screeningCode;
	}

	public String getScreeningName()
	{
		return screeningName;
	}

	public String getScreeningDesc()
	{
		return screeningDesc;
	}

	public String getCheckUnitsAbbr()
	{
		return checkUnitsAbbr;
	}
	
	/**
	 * Adds a screening criteria and keeps the set in order.
	 * @param screeningCriteria
	 */
	public void add(ScreeningCriteria screeningCriteria)
	{
		Calendar c = screeningCriteria.getSeasonStart();
		if (c == null || criteriaSeasons.size() == 0)
			criteriaSeasons.add(0, screeningCriteria);
		else
		{
			for(int idx = 0; idx < criteriaSeasons.size(); idx++)
				if (before(c, criteriaSeasons.get(idx).getSeasonStart()))
				{
					criteriaSeasons.add(idx, screeningCriteria);
					return;
				}
			// Fell through, this is after all the existing seasons
			criteriaSeasons.add(screeningCriteria);
		}
		screeningCriteria.setScreening(this);
	}
	
	/**
	 * @return true if the time-of-year represented by c1 is before c2.
	 */
	private boolean before(Calendar c1, Calendar c2)
	{
		int mon1 = c1.get(Calendar.MONTH);
		int mon2 = c2.get(Calendar.MONTH);
		if (mon1 < mon2)
			return true;
		else if (mon1 > mon2)
			return false;
		// else months are equal, compare the day
		int day1 = c1.get(Calendar.DAY_OF_MONTH);
		int day2 = c2.get(Calendar.DAY_OF_MONTH);
		if (day1 < day2)
			return true;
		else if (day1 > day2)
			return false;
		// else days are equal, compare the hour
		int hr1 = c1.get(Calendar.HOUR_OF_DAY);
		int hr2 = c2.get(Calendar.HOUR_OF_DAY);
		if (hr1 < hr2)
			return true;
		else if (hr1 > hr2)
			return false;
		// else hours are equal, compare the minute
		int min1 = c1.get(Calendar.MINUTE);
		int min2 = c2.get(Calendar.MINUTE);
		if (min1 < min2)
			return true;
		else if (min1 > min2)
			return false;
		// else minutes are equal, compare the second
		int sec1 = c1.get(Calendar.SECOND);
		int sec2 = c2.get(Calendar.SECOND);
		if (sec1 < sec2)
			return true;
		else if (sec1 > sec2)
			return false;
		// else seconds are equal, compare the millisecond
		int ms1 = c1.get(Calendar.MILLISECOND);
		int ms2 = c2.get(Calendar.MILLISECOND);
		if (ms1 < ms2)
			return true;
		else if (ms1 > ms2)
			return false;

		// Calendars are equal
		return false;
	}

	public ScreeningCriteria findForDate(Date d)
	{
		if (criteriaSeasons.size() == 0)
			return null;
		// If there are all-time checks, they will be sorted to the front.
		if (criteriaSeasons.get(0).getSeasonStart() == null
		 || criteriaSeasons.size() == 1)
			return criteriaSeasons.get(0);
		
		// There are multiple seasons, sorted in ascending order
		Calendar cal = Calendar.getInstance();
		cal.setTimeZone(criteriaSeasons.get(0).getSeasonStart().getTimeZone());
		cal.setTime(d);
		ScreeningCriteria prevSeason = criteriaSeasons.get(criteriaSeasons.size()-1);
		for(ScreeningCriteria sc : criteriaSeasons)
		{
			if (before(cal, sc.getSeasonStart()))
				return prevSeason;
			prevSeason = sc;
		}
		// Fell through means all seasons are before this date, return last one.
		return criteriaSeasons.get(criteriaSeasons.size()-1);
	}

	public void setScreeningCode(DbKey screeningCode)
	{
		this.screeningCode = screeningCode;
	}

	public String getParamId()
	{
		return paramId;
	}

	public void setParamId(String paramId)
	{
		this.paramId = paramId;
	}

	public String getParamTypeId()
	{
		return paramTypeId;
	}

	public void setParamTypeId(String paramTypeId)
	{
		this.paramTypeId = paramTypeId;
	}

	public String getDurationId()
	{
		return durationId;
	}

	public void setDurationId(String durationId)
	{
		this.durationId = durationId;
	}

	public ArrayList<ScreeningCriteria> getCriteriaSeasons()
	{
		return criteriaSeasons;
	}

	public boolean isRangeActive()
	{
		return rangeActive;
	}

	public void setRangeActive(boolean rangeActive)
	{
		this.rangeActive = rangeActive;
	}

	public boolean isRocActive()
	{
		return rocActive;
	}

	public void setRocActive(boolean rocActive)
	{
		this.rocActive = rocActive;
	}

	public boolean isConstActive()
	{
		return constActive;
	}

	public void setConstActive(boolean constActive)
	{
		this.constActive = constActive;
	}

	public boolean isDurMagActive()
	{
		return durMagActive;
	}

	public void setDurMagActive(boolean durMagActive)
	{
		this.durMagActive = durMagActive;
	}

	@Override
	public DbKey getKey()
	{
		return screeningCode;
	}

	@Override
	public String getUniqueName()
	{
		return screeningName;
	}

	public void setScreeningName(String screeningName)
	{
		this.screeningName = screeningName;
	}

	public void setCheckUnitsAbbr(String checkUnitsAbbr)
	{
		this.checkUnitsAbbr = checkUnitsAbbr;
	}
	
	public void setSeasonTimeZone(TimeZone tz)
	{
		for(ScreeningCriteria crit : criteriaSeasons)
		{
			Calendar cal = crit.getSeasonStart();
			int month = cal.get(Calendar.MONTH), day = cal.get(Calendar.DAY_OF_MONTH);
			if (cal != null)
				cal.setTimeZone(tz);
			// MJM Note: Changing the calendar tz may change the day/month. Have to reset.
			cal.set(Calendar.MONTH, month);
			cal.set(Calendar.DAY_OF_MONTH, day);
			cal.set(Calendar.HOUR, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
		}
	}

	public void convertUnits(String paramUnitsAbbr)
		throws NoConversionException
	{
		EngineeringUnit chkEU = EngineeringUnit.getEngineeringUnit(checkUnitsAbbr);
		if (chkEU == null)
			throw new NoConversionException("Screening.convertUnits: Invalid check units abbr '"
				+ checkUnitsAbbr + "'");
		EngineeringUnit prmEU = EngineeringUnit.getEngineeringUnit(paramUnitsAbbr);
		if (prmEU == null)
			throw new NoConversionException("Screening.convertUnits: Invalid param units abbr '"
				+ paramUnitsAbbr + "'");
		UnitConverter uc = decodes.db.Database.getDb().unitConverterSet.get(chkEU, prmEU);
		if (uc == null)
			throw new NoConversionException("Cannot derive a converter from '"
				+ checkUnitsAbbr + "' to '" + paramUnitsAbbr + "'");
		Logger.instance().debug1("Screening.convertUnits: converting screening units from " + checkUnitsAbbr
			+ " to " + paramUnitsAbbr);
		
		// Descend through all checks and convert the limits to the new units.
		// then set this.checkUnitsAbbr
		for(ScreeningCriteria crit : criteriaSeasons)
			crit.convertUnits(paramUnitsAbbr, uc);
		
		checkUnitsAbbr = paramUnitsAbbr;
		
	}

	public void setScreeningDesc(String screeningDesc)
	{
		this.screeningDesc = screeningDesc;
	}
}
