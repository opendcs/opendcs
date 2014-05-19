/*
*  $Id$
*/
package decodes.util;

import lrgs.gui.LrgsBuild;

/**
Static final variables values representing software version.
*/
public class DecodesVersion
{
	/** the program name */
	private static String name = "Device Conversion and Delivery System";

	/** the program abbreviation */
	private static String abbr = "OPENDCS";

	/** the program version number */
	private static String version = "6.0 RC" + LrgsBuild.rcNum;

	/** the program date */
	private static String modifyDate = LrgsBuild.buildDate;
	
	private static boolean _initialized = false;

	private static void checkInit()
	{
    	if (!_initialized)
    	{
    		ResourceFactory.instance().initializeDecodesVersion();
    		_initialized = true;
    	}
	}

	/**
	  @return a String containing complete version info.
	*/
	public static final String startupTag()
	{
		checkInit();
		return getAbbr() + " " + getVersion() + " built on " + LrgsBuild.buildDate;
	}

	/**
     * @return the name
     */
    public static String getName()
    {
		checkInit();
	    return name;
    }

	/**
     * @return the abbr
     */
    public static String getAbbr()
    {
		checkInit();
	    return abbr;
    }

	/**
     * @return the version
     */
    public static String getVersion()
    {
		checkInit();
	    return version;
    }

	/**
     * @return the modifyDate
     */
    public static String getModifyDate()
    {
		checkInit();
	    return modifyDate;
    }
    
	public static void setName(String nm) { name = nm; }
	public static void setAbbr(String ab) { abbr = ab; }
	public static void setVersion(String ve) { version = ve; }

	public static void main(String args[])
	{
		System.out.println(startupTag());
	}
}
