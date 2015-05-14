/*
*  $Id$
*/
package lrgs.gui;

import decodes.util.DecodesVersion;

/**
 * This class provides constant string identifiers like program names
 * and version numbers.
 */
public class LrgsApp
{
	/** Major version number */
	public static final int majorVersion = 9;

	/** Minor version number */
	public static final int minorVersion = 0;

	/** Sub version number */
	public static final String subVersion = DecodesVersion.getName() + "-" + DecodesVersion.getVersion();

	/** Source Last Modified */
	public static final String releasedOn = LrgsBuild.buildDate;

	/**
	 * Displayable Version number.
	 */
	public static final String AppVersion = "" + majorVersion + "." 
		+ minorVersion + "." + subVersion;

	public static final String AppAbbreviation = "LRGS";
	public static final String AppFullName = "Local Readout Ground Station";

	/**
	 * Abbreviation for when a short ID is needed for display.
	 */
	public static final String ShortID = AppAbbreviation + " " + 
		DecodesVersion.getAbbr() + " " + DecodesVersion.getVersion();

	/**
	 * Full name and version number of this application.
	 */
	public static final String FullID = AppFullName + " " + AppVersion
		+ " " + releasedOn;

	/**
	 * Sub-directory name for building unique path to files for this version:
	 */
	public static final String SubDir = AppAbbreviation + "-" + AppVersion;

	/**
	 * Name of properties file for this application.
	 */
	public static final String PropFile = "lrgsgui.properties";

	public static void main(String args[])
	{
		System.out.println("AppFullName: " + AppFullName);
		System.out.println("ShortID: " + ShortID);
		System.out.println("FullID: " + FullID);
	}
}



