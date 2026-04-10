/*
 *  Copyright 2025 OpenDCS Consortium and its Contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License")
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
*  $Id: ApiPropertiesUtil.java,v 1.3 2023/01/23 19:52:32 mmaloney Exp $
*/
package org.opendcs.odcsapi.util;

import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;

/**
A collection of static utility methods for manipulating java.util.Properties
sets.
*/
public class ApiPropertiesUtil
{

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
