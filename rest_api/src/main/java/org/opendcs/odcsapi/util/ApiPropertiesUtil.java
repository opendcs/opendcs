/*
*  $Id: ApiPropertiesUtil.java,v 1.3 2023/01/23 19:52:32 mmaloney Exp $
*/
package org.opendcs.odcsapi.util;

import java.util.Properties;
import java.util.StringTokenizer;

import java.util.logging.Logger;

import java.util.Enumeration;

/**
A collection of static utility methods for manipulating java.util.Properties
sets.
*/
public class ApiPropertiesUtil
{
	public static final String possibleDelims  = ",;|#+!~^&*";
	public static final String possibleAssigns = "=:><`()[]";
	
	/**
	* Returns true if two properties sets are the same. Meaning they have
	* exactly the same keys and values.
	* In this method, keys are compared case SENSITIVE.
	* @param p1 the first properties set.
	* @param p2 the second properties set.
	* @return true if keys and values in both sets are equal, or if both are null.
	*/
	public static boolean propertiesEqual( Properties p1, Properties p2 )
	{
		if (p1 == null)
			return p2 == null;
		else if (p2 == null)
			return false;
		
		if (p1.size() != p2.size())
		{
			return false;
		}
		for(Enumeration it = p1.keys(); it.hasMoreElements();)
		{
			Object k = it.nextElement();
			Object v1 = p1.get(k);
			Object v2 = p2.get(k);
			if (v2 == null || !v1.equals(v2))
			{
				return false;
			}
		}
		return true;
	}

	/**
	* Returns the properties in a comma-separated string.
	* Warning: no accomodation is made for property values that may contain
	* commas.
	* @param pr the properties set
	* @return String representation
	*/
	public static String props2string( Properties pr )
	{
		int dIdx = 0;
		int aIdx = 0;
		nextDelim:
		while(dIdx < possibleDelims.length())
		{
			for(Object v : pr.values())
			{
				String sv = (String)v;
				if (sv.indexOf(possibleDelims.charAt(dIdx)) >= 0)
				{
					dIdx++;
					continue nextDelim;
				}
			}
			break;
		}
	  nextAssign:
		while(aIdx < possibleAssigns.length())
		{
			for(Object v : pr.values())
			{
				String sv = (String)v;
				if (sv.indexOf(possibleAssigns.charAt(aIdx)) >= 0)
				{
					aIdx++;
					continue nextAssign;
				}
			}
			break;
		}

		if (dIdx == possibleDelims.length())
		{
			Logger.getLogger(ApiConstants.loggerName).warning("Cannot encode props because values contain "
				+ "all possible delims " + possibleDelims);
			dIdx = aIdx = 0;
		}
		if (aIdx == possibleAssigns.length())
		{
			Logger.getLogger(ApiConstants.loggerName).warning("Cannot encode props because values contain"
				+ " all possible assignment operators " + possibleAssigns);
			dIdx = aIdx = 0;
		}
		StringBuffer ret = new StringBuffer();
		char delim = possibleDelims.charAt(dIdx);
		char assign = possibleAssigns.charAt(aIdx);
		if (dIdx != 0 || aIdx != 0)
		{
			ret.append(delim);
			ret.append(assign);
		}
		int i=0;
		Enumeration en = pr.propertyNames();
		while(en.hasMoreElements())
		{
			String key = (String)en.nextElement();
			String value = pr.getProperty(key);
			if (i++ > 0)
			{
				ret.append(delim);
				ret.append(' ');
			}
			ret.append(key);
			ret.append(assign);
			ret.append(value);
		}
		return ret.toString();
	}

	/**
	* Converts string containing comma-separated assignments into a Properties
	* set.
	* @param s the string
	* @return the Properties
	*/
	public static Properties string2props( String s )
	{
		Properties ret = new Properties();
		if (s == null)
			return ret;
		s = s.trim();
		if (s.length() < 3) // Would have to have at least one name=value
			return ret;
		
		char delim = ',';
		char assign = '=';
		if (!Character.isLetterOrDigit(s.charAt(0)))
		{
			delim = s.charAt(0);
			assign = s.charAt(1);
			s = s.substring(2);
		}

		StringTokenizer tokenizer = new StringTokenizer(s, "" + delim);
//			TextUtil.collapseWhitespace(s), ",");
	
		while(tokenizer.hasMoreTokens())
		{
			String tok = tokenizer.nextToken().trim();
			int ei = tok.indexOf(assign);
			if (ei == -1)
				ret.setProperty(tok, "");
			else
				ret.setProperty(tok.substring(0,ei).trim(), 
					tok.substring(ei+1).trim());
		}
		return ret;
	}

	/**
	* Search for a property, ignoring case
	* @param pr the Properties
	* @param key the key
	* @return the value, or null if no match
	*/
	public static String getIgnoreCase( Properties pr, String key )
	{
		Enumeration en = pr.propertyNames();		
		while(en.hasMoreElements())
		{
			String k = (String)en.nextElement();			
			if (key.trim().equalsIgnoreCase(k.trim()))
				return pr.getProperty(k);
		}
		return null;
	}

	/**
	* Search for a property, remove it and return its value, ignoring case in 
	* the name.
	* @param pr the Properties
	* @param key the key
	* @return the property value if found, null if not found.
	*/
	public static String rmIgnoreCase(Properties pr, String key)
	{
		Enumeration en = pr.propertyNames();
		while(en.hasMoreElements())
		{
			String k = (String)en.nextElement();
			if (key.equalsIgnoreCase(k))
			{
				String ret = pr.getProperty(k);
				pr.remove(k);
				return ret;
			}
		}
		return null;
	}

	/**
	 * Copies all top-level values from one properties set into another.
	 * Does not change the 'default' properties of the target.
	 * The targetProps is not cleared first, so after return it will have
	 * all its original properties plus the added/overwritten ones from
	 * the sourceProps.
	 * @param targetProps the properties set to copy to.
	 * @param sourceProps the properties set to copy from.
	 */
	public static void copyProps(Properties targetProps, Properties sourceProps)
	{
		for(Enumeration e = sourceProps.propertyNames(); e.hasMoreElements(); )
		{
			String k = (String)e.nextElement();
			targetProps.setProperty(k, sourceProps.getProperty(k));
		}
	}

}
