/**
 * $Id$
 * 
 * $Log$
 * 
 * This software was written by Cove Software, LLC ("COVE") under contract
 * to the United States Government. No warranty is provided or implied other
 * than specific contractual terms between COVE and the U.S. Government.
 *
 * Copyright 2014 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * All rights reserved.
 */
package decodes.decoder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.TimeZone;

import decodes.db.EnumValue;

public class Season
{
	/** one-word abbreviation for season */
	private String abbr = null;
	/** multi-word more descriptive name */
	private String name = null;
	/** String representation of season start */
	private String start = null;
	/** String representation of season end */
	private String end = null;
	/** Time Zone ID */
	private String tz = null;
	
	private SimpleDateFormat sdf = new SimpleDateFormat("MM/dd-HH:mm");
	private boolean modified = false;
	
	public Season()
	{
	}
	
	/**
	 * Return true if the passed Date is within this season.
	 * That is, it is >= the start and < the end.
	 * @param d
	 * @return true if the passed Date is within this season
	 */
	public boolean isInSeason(Date d)
	{
		if (modified && tz != null)
			sdf.setTimeZone(TimeZone.getTimeZone(tz));
		String test = sdf.format(d);
		
		// date/times are all normalized to date format. Do a string compare.
		if (start.equals(end))
			// Special case -- season is all year.
			return true;
		if (start.compareTo(end) < 0)
			// normal case start is before end
			return test.compareTo(start) >= 0 && test.compareTo(end) < 0;
		else // there is a year wrap-around (e.g. winter)
			return test.compareTo(start) >= 0 || test.compareTo(end) < 0;
	}
	
	public void setFromEnum(EnumValue ev)
		throws FieldParseException
	{
		modified = true;
		setAbbr(ev.getValue());
		setName(ev.getDescription());
		String ecn = ev.getEditClassName();
		if (ecn == null)
			throw new FieldParseException("No edit class name");
		StringTokenizer st = new StringTokenizer(ev.getEditClassName());
		if (st.countTokens() < 2)
			throw new FieldParseException("Options must be MM/dd-HH:mm MM/dd-HH:mm [tz]");
		
		setStart(formatDateTime(st.nextToken(), "start"));
		setEnd(formatDateTime(st.nextToken(), "end"));
		if (st.hasMoreTokens())
			setTz(st.nextToken());
	}
	
	private enum state { M, d, H, m };
	public static String formatDateTime(String s, String what)
		throws FieldParseException
	{
		state ss = state.M;
		String expect = " extected 'MM/dd-HH:mm'";
		String M, d, H, m;
		M = d = H = m = "";
		for(int idx = 0; idx < s.length(); idx++)
		{
			char c = s.charAt(idx);
			switch(ss)
			{
			case M:
				if (c == '/')
					ss = state.d;
				else if (M.length() > 2)
					throw new FieldParseException("Month field too long in " + what + expect);
				else if (!Character.isDigit(c))
					throw new FieldParseException("Non-digit in Month field in " + what + expect);
				else	
					M += c;
				break;
			case d:
				if (c == '-')
					ss = state.H;
				else if (d.length() > 2)
					throw new FieldParseException("Day field too long in " + what + expect);
				else if (!Character.isDigit(c))
					throw new FieldParseException("Non-digit in Day field in " + what + expect);
				else	
					d += c;
				break;
			case H:
				if (c == ':')
					ss = state.m;
				else if (H.length() > 2)
					throw new FieldParseException("Hour field too long in " + what + expect);
				else if (!Character.isDigit(c))
					throw new FieldParseException("Non-digit in Hour field in " + what + expect);
				else	
					H += c;
				break;
			case m:
				if (m.length() > 2)
					throw new FieldParseException("Minute field too long in " + what + expect);
				else if (!Character.isDigit(c))
					throw new FieldParseException("Non-digit in Hour Minute in " + what + expect);
				else	
					m += c;
				break;
			}
		}
		if (M.length() == 0 || d.length() == 0)
			throw new FieldParseException("Month and day fields required!");
		if (H.length() == 0)
			H = "12";
		if (m.length() == 0)
			m = "00";
		return M + "/" + d + "-" + H + ":" + m;
	}
	
	public void saveToEnum(EnumValue ev)
	{
		ev.setValue(abbr);
		ev.setDescription(name);
		String ec = start + " " + end;
		if (tz != null)
			ec = ec + " " + tz;
		ev.setEditClassName(ec);
	}

	public String getAbbr()
	{
		return abbr;
	}

	public void setAbbr(String abbr)
	{
		this.abbr = abbr;
		modified = true;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
		modified = true;
	}

	public String getStart()
	{
		return start;
	}

	/**
	 * Sets the start string in format MM/dd-HH:mm.
	 * Add additional digits to fields if necessary.
	 * @param start the start date/time
	 */
	public void setStart(String start)
	{
		this.start = start;
		modified = true;
	}

	public String getEnd()
	{
		return end;
	}

	/**
	 * Sets the end string in format MM/dd-HH:mm.
	 * Add additional digits to fields if necessary.
	 * @param end the start date/time
	 */
	public void setEnd(String end)
	{
		this.end = end;
		modified = true;
	}

	public String getTz()
	{
		return tz;
	}

	public void setTz(String tz)
	{
		this.tz = tz;
		modified = true;
	}
}
