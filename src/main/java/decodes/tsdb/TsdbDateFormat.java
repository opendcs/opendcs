
package decodes.tsdb;

import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.TimeZone;
import java.util.Date;

/**
Static methods for parsing & printing dates in a variety of formats.
*/
public class TsdbDateFormat
{
	private SimpleDateFormat formats[] =
		{
			new SimpleDateFormat("dd MMM yyyy HH:mm"),
			new SimpleDateFormat("dd MMM yyyy"),
			new SimpleDateFormat("yyyy-MM-dd HH:mm"),
			new SimpleDateFormat("yyyy-MM-dd"),
			new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss"),
			new SimpleDateFormat("dd-MMM-yyyy HH:mm")
		};
	
	public TsdbDateFormat(TimeZone tz)
	{
		setTimeZone(tz);
	}

	/** Return the default, preferred date format. */
	public String getDefaultFormat() { return formats[0].toPattern(); }

	/**
	 * Sets the time zone for all subsequent parses.
	 */
	public void setTimeZone(TimeZone tz)
	{
		for(SimpleDateFormat sdf : formats)
			sdf.setTimeZone(tz);
	}

	/**
	 * Parses a string in one of the accepted formats into a java Date object.
	 * @return the result of the parse
	 * @throws ParseException if all formats fail.
	 */
	public Date parse(String str)
		throws ParseException
	{
		for(SimpleDateFormat sdf : formats)
		{
			try { return sdf.parse(str); }
			catch(ParseException ex) { }
		}
		throw new ParseException("All valid formats failed.", 0);
	}

	/**
	 * Formats a date according to the 1st in the list (the preferred format)
	 */
	public String format(Date d)
	{
		return formats[0].format(d);
	}
}
