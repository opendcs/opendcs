package org.opendcs.odcsapi.util;

/**
 * HydroJSON represents time format via python specifications
 * (see https://docs.python.org/2/library/time.html)
 * This class provides utilities for converting these to Java
 * SimpleDateFormat specification.
 * @author mmaloney
 */
public class PythonDate
{

	public PythonDate()
	{
	}
	
	/**
	 * Convert a date/time spec from the python format specifier to a Java
	 * format for SimpleDateFormat
	 * @param pyFmt
	 * @return the SimpleDateFormat spec
	 */
	public static String pyFmt2sdf(String pyFmt)
	{
		String ret = pyFmt;
		ret = ret.replaceAll("%Y", "yyyy");
		ret = ret.replaceAll("%y", "yy");
		ret = ret.replaceAll("%m", "MM");
		ret = ret.replaceAll("%B", "MMM");
		ret = ret.replaceAll("%d", "dd");
		ret = ret.replaceAll("%j", "DDD");
		ret = ret.replaceAll("%H", "HH");
		ret = ret.replaceAll("%I", "hh");
		ret = ret.replaceAll("%M", "mm");
		ret = ret.replaceAll("%S", "ss");
		ret = ret.replaceAll("%Z", "z");
		ret = ret.replaceAll("%z", "z");
		ret = ret.replaceAll("T", "'T'");
		
		return ret;
	}

}
