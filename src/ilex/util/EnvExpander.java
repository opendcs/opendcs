/*
*  $Id$
*
*  $Log$
*  Revision 1.5  2008/12/17 22:52:41  mjmaloney
*  case insensitive compare fix.
*
*  Revision 1.4  2008/12/15 16:51:43  mjmaloney
*  Fixed substring bug.
*
*  Revision 1.3  2008/11/19 13:43:56  mjmaloney
*  synced with opensrc
*
*  Revision 1.13  2008/08/06 21:53:30  satin
*  Corrected error in Date expansion that caused an array-out-of bound error.
*
*  Revision 1.12  2008/06/09 18:50:02  satin
*  Reorganized to facilitate expansions and added the expansion of the variables
*  ${HOME} and ${USER}.
*
*  Revision 1.11  2008/04/15 11:56:49  satin
*  Added constructor to allow the time zone to be specified rather
*  than having it default to the current one.
*
*  Added variable "WYR" that will expand to the current water year.
*  (WYR begins on October 1. )  This is useful to expand in paths that define
*  archival areas for hydrologic data.
*
*  Revision 1.10  2007/11/07 21:44:56  mmaloney
*  dev
*
*  Revision 1.9  2005/06/09 20:53:34  mjmaloney
*  bug fixes.
*
*  Revision 1.8  2005/04/06 12:21:18  mjmaloney
*  dev
*
*  Revision 1.7  2004/09/20 18:11:50  mjmaloney
*  Allow ~myuser as well as ~ for current user's directory.
*
*  Revision 1.6  2004/08/30 14:50:26  mjmaloney
*  Javadocs
*
*  Revision 1.5  2004/08/10 15:48:03  mjmaloney
*  Log message on exit saying lock file was removed.
*
*  Revision 1.4  2003/06/17 15:25:06  mjmaloney
*  Updated library versions to 3.4
*
*  Revision 1.3  2003/06/06 18:17:58  mjmaloney
*  string2props return empty props if passed string is null.
*
*  Revision 1.2  2003/06/05 19:59:48  mjmaloney
*  Support $DATE(format), where format goes with SimpleDateFormat.
*
*  Revision 1.1  2003/05/16 20:12:38  mjmaloney
*  Added EnvExpander. This is preferrable to ShellExpander because
*  it is platform independent.
*
*/
package ilex.util;

import java.util.Date;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.Properties;
import java.text.SimpleDateFormat;

/**
This class expands environment variables that were placed into system
properties according to the following rules:
<p>
<ul>
  <li>$VARNAME	Will be replaced by the value of the matching environment
  variable. If there is none, the string remains unchanged.</li>
  <li>$HOME		Will be replaced with the current users home directory.</li>
  <li>~			Will be replaced with the current users home directory.</li>
</ul>
<p>
Note that OS environment variables are not necessarily available to
Java programs unless you use a -Dname=value on the command line.
*/
public class EnvExpander
{
	/**
	Expands a string using System properties.
	@param str the string
	@return expanded string
	*/
	public static String expand( String str )
	{
		return expand(str, System.getProperties());
	}

	/**
	Expands a string using explicit properties set using current date/time.
	@param str the string
	@param props the properties
	@return expanded string
	*/
	public static String expand( String str, Properties props )
	{
		return expand(str, props, new Date());
	}

	
	/**
	Expands a string using explicit properties set and explicit date/time.
	On Unix systems, also allow ~ and ~myname, which get expanded to the
	current user's directory. Note: This cannot be used to expand other user's
	directories.

	@param str the string
	@param props the properties
	@param date the date
	@return expanded string
	*/
	public static String expand( String str, Properties props, Date date )
	{
		String tzname = props.getProperty("TZ");
		return(expand( str, props, date, tzname ));
		
	}
	public static String expand( String str, Properties props, Date date,
				String tzname )
	{
		TimeZone tz;
		
		if (str == null)
			return null;
		if ( tzname == null ) 
			tz = TimeZone.getDefault();
		else
			tz = TimeZone.getTimeZone(tzname);
		StringBuffer output = new StringBuffer();
		int length = str.length();
		for(int i=0; i<length; )
		{
			char c = str.charAt(i);
			if (c == '~')
			{
				String nm = props.getProperty("user.name");
				if ((i+1) == length
			     || !Character.isLetter(str.charAt(i+1))
			     || str.substring(i+1).startsWith(nm))
				{
					output.append(props.getProperty("user.home"));
					i++;
					if (i < length && Character.isLetter(str.charAt(i)))
						i += nm.length();
				}
				else
				{
					output.append(c);
					i++;
				}
			}
			else if (c == '$' 
			      && (i+1) < length 
			      && Character.isLetter(str.charAt(i+1)))
			{
				int j = i+2; 
				while(j < length 
				   && (Character.isLetterOrDigit(str.charAt(j))
				     || str.charAt(j) == '_' || str.charAt(j) == '.'))
					j++;
				String name = str.substring(i+1, j);
				if ( j < length
					&&	name.equalsIgnoreCase("date") && str.charAt(j) == '(') {
					j++;
					while(j < length  && str.charAt(j) != ')' ) 
						j++;
					j++;
					name = str.substring(i+1, j);
				}
				String val = expandName(name,props,date,tz);
				if (val != null)
					output.append(val);
				else
					output.append("$" + name);
				i = j;
			}
			else if (c == '$'
							&& (i+1) < length
							&& str.charAt(i+1) == '{')
			{
				String val = null;
				int j = i+2;
				while(j < length && str.charAt(j) != '}')
					j++;
				String name = str.substring(i+2, j);
				val = expandName(name,props,date,tz);
				if (val != null)
					output.append(val);
				else
					output.append("${" + name + "}");
				i = j+1;
			}
			else
			{
				output.append(c);
				i++;
			}
		}
		return output.toString();
	}
	private static String expandName(String name, Properties props, Date date, TimeZone tz)
	{
		String val = null;
		int length = name.length();
		int k;
		if (name.equalsIgnoreCase("HOME"))
			val = props.getProperty("user.home");
		else if (TextUtil.startsWithIgnoreCase(name, "DATE"))
		{
			// Look for SimpleDateFormat string in parens
			String fmt = "yyyyMMdd-HHmmss";
			k= name.indexOf('(');
			if (k != -1 && k < length && name.charAt(k) == '(')
			{
				int fmtStart = ++k;
				for(; k<length && name.charAt(k) != ')'; k++);
				fmt = name.substring(fmtStart, k);
				if (name.charAt(k) == ')')
					k++;
			}
			SimpleDateFormat sdf = new SimpleDateFormat(fmt);
			if (tz != null)
				sdf.setTimeZone(tz);
			val = sdf.format(date);
		}
		else if (name.equalsIgnoreCase("WYR"))
		{
			Calendar cal = Calendar.getInstance(tz);
			cal.setTime(date);
			int year = cal.get(Calendar.YEAR);
			int month = cal.get(Calendar.MONTH);
			if ( month >= Calendar.OCTOBER )
				year++;
			val = "WY"+Integer.toString(year);
		}
		else if (TextUtil.startsWithIgnoreCase(name, "DATE"))
		{
 	    // Look for SimpleDateFormat string in parens
			String fmt = "yyyyMMdd-HHmmss";
			k = name.indexOf('(');
			if (k != -1 && k < length && name.charAt(k) == '(')
			{
				int fmtStart = ++k;
				for(; k<length && name.charAt(k) != ')'; k++);
				fmt = name.substring(fmtStart, k);
				if (name.charAt(k) == ')')
				  k++;
			}
			SimpleDateFormat sdf = new SimpleDateFormat(fmt);
			if (tz != null)
				sdf.setTimeZone(tz);
			val = sdf.format(date);
		}
		else if (name.equalsIgnoreCase("USER"))
			val = props.getProperty("user.name");
		else if (name.equalsIgnoreCase("HOME"))
			val = props.getProperty("user.home");
		else {
			val = props.getProperty(name);
		}
		return(val);
	}

	/**
	test main.
	@param args the args
	*/
	public static void main( String[] args )
	{
		System.out.println("Expanded '" + args[0] + "' to '" 
			+ expand(args[0]) + "'");
	}
}
