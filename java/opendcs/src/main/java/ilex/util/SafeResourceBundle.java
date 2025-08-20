/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*/
package ilex.util;

import java.util.Enumeration;
import java.util.Locale;
import java.util.ResourceBundle;

public class SafeResourceBundle
	extends ResourceBundle
{
	ResourceBundle theBundle;

	public SafeResourceBundle(ResourceBundle theBundle)
	{
		this.theBundle = theBundle;
	}

	/**
	 * Try to get an object from the bundle. If null, return the key.
	 * If the key is null return the string "null-resource-key".
	 * Thus this method will never return null or throw an exception so
	 * the higher-level getString method should never throw
	 * MissingResourceException.
	 */
	protected Object handleGetObject(String key)
	{
		if (key == null)
			return "null-resource-key";
		try
		{
			Object ret = theBundle.getObject(key);
			if (ret != null) 
				return ret;
			Logger.instance().warning("Unknown resource key '" + key 
				+ "' - not found in resource file.");
			return key;
		}
		catch(Exception ex)
		{
			Logger.instance().warning("Bad resource key '" + key 
				+ "': " + ex);
			return "bad-key:" + key;
		}
	}

	public Enumeration<String> getKeys()
	{
		return theBundle.getKeys();
	}

	public Locale getLocale()
	{
		return theBundle.getLocale();
	}
}
