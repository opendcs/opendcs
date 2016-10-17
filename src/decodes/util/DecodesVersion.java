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
	private static String name = "OpenDCS";

	/** the program abbreviation */
	private static String abbr = "OPENDCS";

	/** the program version number */
	private static String version = "6.3 RC" + LrgsBuild.rcNum;

	/** the program date */
	private static String modifyDate = LrgsBuild.buildDate;
	
	/**
	  @return a String containing complete version info.
	*/
	public static final String startupTag()
	{
		return getAbbr() + " " + getVersion() + " built on " + LrgsBuild.buildDate;
	}

	/**
     * @return the name
     */
    public static String getName()
    {
	    return name;
    }

	/**
     * @return the abbr
     */
    public static String getAbbr()
    {
	    return abbr;
    }

	/**
     * @return the version
     */
    public static String getVersion()
    {
	    return version;
    }

	/**
     * @return the modifyDate
     */
    public static String getModifyDate()
    {
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
