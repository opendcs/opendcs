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

import java.util.Formatter;
import java.util.IllegalFormatException;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.MissingResourceException;

/**
 * Loads the properties file that contains all labels used throughout
 * the application.
 * Language supported: English and/or Spanish
 * 
 */
public class LoadResourceBundle 
{
	private static Locale defaultLocale = null;
	
	/** Empty Constructor
	 */
	public LoadResourceBundle()
	{
	}
	
	/**
	 * Sets the Java Default Locale
	 * @param language the language, e.g. "en" (english), "es" (spanish)
	 */
	public static void setLocale(String language)
	{
		if (language == null || language.trim().equals(""))
		{	//Use the default Locale
			//need to parse and find out if Locale starts with 
			//en or es. if it starts with en use the en_US Locale, if it starts
			//with es use the es_US Locale
			Locale locale = Locale.getDefault();

			//Spanish - use the es_US resource bundle properties
			if (locale.getLanguage().equalsIgnoreCase("es"))
			{
				defaultLocale = new Locale("es", "US");
			}
			else if (locale.getLanguage().equalsIgnoreCase("en"))
			{	//The other choice is English
				defaultLocale = new Locale("en", "US");
			}
			else
			{	//Not a supported Locale
				defaultLocale = Locale.US;
				//Need to set Java Locale to English
				Locale.setDefault(defaultLocale);
			}
		}
		else
		{	//Use the decodes.properties language to set up the Locale
			if (language.equalsIgnoreCase("en"))
			{	//English
				//defaultLocale = new Locale("en", "US");
				defaultLocale = Locale.US;
				//Need to set Java Locale to English
				Locale.setDefault(defaultLocale);
			}
			else if (language.equalsIgnoreCase("es"))
			{	//Spanish
				defaultLocale = new Locale("es", "US");
				//Need to set Java Locale to Spanish
				Locale.setDefault(defaultLocale);
			} 
			else
			{	//Not a supported Locale
				defaultLocale = Locale.US;
				//Need to set Java Locale to English
				Locale.setDefault(defaultLocale);
			}
		}
	}
	
	/**
	 * Finds the Properties file for the given baseName.
	 * 'baseName' is a path to the file for the default class loader.
	 * The full name will be: baseName_language_country.properties.
	 * 
	 * @param baseName path name to resource file w/o language & country.
	 * @param language language used to set the Java Locale
	 * @return the resource bundle for the given base name
	 */
	public static ResourceBundle getLabelDescriptions(String baseName,
			String language)
	{
		if (defaultLocale == null)
		{
			setLocale(language);
		}
		ResourceBundle labelDescriptions;
		try
		{
			labelDescriptions = ResourceBundle.getBundle(baseName, 
				defaultLocale);
		}
		catch(MissingResourceException ex)
		{
			Logger.instance().failure("Cannot load resource bundle '"
				+ baseName + "', for language '" + language + "': " + ex);
			try 
			{
				labelDescriptions = ResourceBundle.getBundle(baseName,
					Locale.US);
			}
			catch(MissingResourceException ex2)
			{
				Logger.instance().failure("Cannot load resource bundle '"
					+ baseName + "', for US English: " + ex2);
				return null;
			}
		}
//		Locale test = Locale.getDefault();
//		System.out.println("Locale country = " + test.getCountry()
//				+ " /language " + test.getLanguage()
//				+ " /Locale to string = " + test.toString());
		return new SafeResourceBundle(labelDescriptions);
	}

	/**
	 * Accept a C-style format string with variable arguments and format into
	 * a printable string. This method is here to facilitate 
	 * internationalization. 
	 * For displayed messages that contain data, put the format string in the
	 * properties file and call this method with appropriate args.
	 */
	public static String sprintf(String format, Object... args)
	{
		try
		{
			StringBuilder sb = new StringBuilder();
			Formatter fm = new Formatter(sb, defaultLocale);
			fm.format(format, args);
			fm.close();
			return sb.toString();
		}
		catch(IllegalFormatException ex)
		{
			return format;
		}
	}
}
