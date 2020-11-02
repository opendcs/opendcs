/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:09  cvs
*  Added legacy code to repository
*
*  Revision 1.9  2004/08/30 14:50:19  mjmaloney
*  Javadocs
*
*  Revision 1.8  2001/03/03 03:15:29  mike
*  GUI mods for 3.2
*
*  Revision 1.7  2000/10/23 13:17:02  mike
*  New scheme for help urls.
*
*  Revision 1.6  2000/05/31 16:00:21  mike
*  Implemented Save button in properties dialog.
*
*  Revision 1.5  2000/04/04 19:07:25  mike
*  dev
*
*  Revision 1.4  2000/03/30 22:54:04  mike
*  dev
*
*  Revision 1.3  2000/03/28 00:43:18  mike
*  dev
*
*  Revision 1.2  2000/03/25 22:03:25  mike
*  dev
*
*  Revision 1.1  2000/03/23 21:07:53  mike
*  Created
*
*/
package ilex.gui;

import java.util.Properties;
import java.util.StringTokenizer;
import ilex.util.RunQueue;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import ilex.gui.MenuFrame;

/**
* This is a singleton class for a GUI application. It provides several
* static resources.
*/
public class GuiApp
{
	static Properties properties = null;
	static String appName = "GuiApp";
	static MenuFrame topFrame = null;
	static RunQueue corbaQueue = null;
	static File propertiesFile = null;

	/**
	* Sets the globally accessible application name.
	* Call this method once, typically from your main on start-up, to set
	* a name which is available to all GUI elements.
	* @param name the applciation's name
	*/
	public static void setAppName( String name )
	{
		appName = name;
	}

	/**
	* Gets the globally-accessible application name.
	* @return the applciation's name
	*/
	public static String getAppName( )
	{
		return appName;
	}

	/**
	* @return the top level frame for this application.
	*/
	public static MenuFrame getTopFrame( )
	{
		return topFrame;
	}

	/**
	* Sets the top level frame for this application.
	* @param f the frame.
	*/
	public static void setTopFrame( MenuFrame f )
	{
		topFrame = f;
	}

	/**
	* Loads application properties from the specified file.
	* This method searches for the property files in the following
	* locations:
	* <ul>
	* <li>Specified path if filename contains a complete path name.
	* <li>Relative to current directory, simple filename or relative path.
	* <li>Current user's home directory
	* <li>The directories specified by the CLASSPATH environment variable.
	* </ul>
	* @param filename the properties file
	* @throws FileNotFoundException if the file could not be found, or was
	* not accessible in any of these locations.
	*/
	public static void loadProperties( String filename ) throws FileNotFoundException
	{
		properties = new Properties();

		propertiesFile = new File(filename);
		if (tryLoadProperties(propertiesFile))
			return;
		String userhome = System.getProperty("user.home");
		if (userhome != null && userhome.length()>0)
		{
			if (userhome.charAt(userhome.length()-1) == File.separatorChar
			 || filename.charAt(0) == File.separatorChar)
			 	propertiesFile = new File(userhome + filename);
			else
			 	propertiesFile = new File(userhome + File.separatorChar + filename);
			if (tryLoadProperties(propertiesFile))
				return;
		}
		String cp = System.getProperty("java.class.path");
		if (cp != null && cp .length() > 0)
		{
			StringTokenizer st = new StringTokenizer(cp, File.pathSeparator);
			while(st.hasMoreTokens())
				if (tryLoadProperties(
					propertiesFile = new File(st.nextToken() + File.separator + filename)))
					return;
		}
		propertiesFile = new File(filename);
		throw new FileNotFoundException("'" + filename +
			"' could not be opened in current, user, or classpath directores.");
	}

	/**
	* Saves the current properties in a file in the user's home directory.
	* @throws IOException if IO error
	*/
	public static void saveProperties( ) throws IOException
	{
		if (propertiesFile == null)
			throw new IOException("No file name specified.");

		FileOutputStream fos = new FileOutputStream(propertiesFile);
		properties.store(fos, appName + " Properties");
		fos.close();
	}

	/**
	* Tries to load properties and returns true on success.
	* @param file the file to load.
	* @return true if success.
	*/
	private static boolean tryLoadProperties( File file )
	{
		try
		{
			FileInputStream fis = new FileInputStream(file);
			properties.load(fis);
			fis.close();
			return true;
		}
		catch(IOException ioe)
		{
			return false;
		}
	}



	/**
	* Provides a static point of reference to the application properties.
	* Returns the property set, or null if none has been loaded.
	* @return the Properties set for this application
	*/
	public static Properties getProperties( )
	{
		if (properties == null)
			properties = new Properties();
		return properties;
	}

	/**
	* Returns the value of a specific property.
	* Returns, null if the property is undefined.
	* @param label the property name
	* @return the Property value
	*/
	public static String getProperty( String label )
	{
		return properties != null ? properties.getProperty(label) : null;
	}

	/**
	* Returns the integer value of the specified property.
	* If the property was not found or a parse error occurred,
	* the specified default value is returned.
	* @param label the property name
	* @param defaultValue the default returned if there is no such property
	* @return property value
	*/
	public static int getIntProperty( String label, int defaultValue )
	{
		int ret = defaultValue;
		String s = getProperty(label);
		if (s != null)
		{
			try { ret = Integer.parseInt(s); }
			catch (Exception e) {}
		}
		return ret;
	}

	/**
	* Returns the boolean value of the specified property.
	* If the property was not found or a parse error occurred,
	* the specified default value is returned.
	* @param label the property name
	* @param defaultValue the default returned if there is no such property
	* @return property value
	*/
	public static boolean getBooleanProperty( String label, boolean defaultValue )
	{
		boolean ret = defaultValue;
		String s = getProperty(label);
		if (s != null)
			ret = Boolean.valueOf(s).booleanValue();
		return ret;
	}

	/**
	* Returns the value of the specified property.
	* If it doesn't exist
	* in the current property set, it is added with the specified default
	* value. Hence, this method is guaranteed not to return null.
	* @param label the property name
	* @param dflt the default returned if there is no such property
	* @return property value
	*/
	public static String getProperty( String label, String dflt )
	{
		if (properties == null)
			properties = new Properties();
		String ret = properties.getProperty(label);
		if (ret == null)
			properties.setProperty(label, ret=dflt);
		return ret;
	}

	/**
	* Sets a property. 
	* @param label
	* @param value 
	*/
	public static void setProperty( String label, String value )
	{
		if (properties == null)
			properties = new Properties();
		properties.setProperty(label, value);
	}


	/**
	* Removes a property from the properties set. This is used primarily
	* for removing deprecated properties after reading a legacy property
	* file.
	* @param label
	*/
	public static void rmProperty( String label )
	{
		if (properties == null)
			return;
		properties.remove(label);
	}

	/**
	* Provides a singleton RunQueue for CORBA operations.
	* @return
	*/
	public static RunQueue getCorbaQueue( )
	 {
	 	if (corbaQueue == null)
	 	{
	 		corbaQueue = new RunQueue();
	 		corbaQueue.start();
	 	}
	 	return corbaQueue;
	 }
}
