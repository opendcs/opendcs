/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.3  2013/03/28 17:29:09  mmaloney
*  Refactoring for user-customizable decodes properties.
*
*  Revision 1.2  2009/06/23 17:20:25  mjmaloney
*  dev
*
*  Revision 1.1  2008/04/04 18:21:08  cvs
*  Added legacy code to repository
*
*  Revision 1.5  2007/12/19 17:56:35  mmaloney
*  Remove -s argument from base class.
*
*  Revision 1.4  2004/08/30 14:50:16  mjmaloney
*  Javadocs
*
*  Revision 1.3  2004/05/17 22:59:09  mjmaloney
*  Default for -h arg set to "localhost"
*
*  Revision 1.2  2001/11/23 18:01:23  mike
*  Added separate constructor for specifying non-network applications.
*
*  Revision 1.1  2001/08/24 19:25:11  mike
*  Created.
*
*/
package ilex.cmdline;


/**
 * StdAppSettings encapsulates a group of common command-line arguments
 * that are useful in many types of applications. These include:
 * <ul>
 *   <li>-h hostname</li>
 *   <li>-d debuglevel (numeric)</li>
 *   <li>-P properties-file</li>
 *  </ul>
 */
public class StdAppSettings extends ApplicationSettings
{
    // Public strings set by command line options:
	private StringToken host_arg;
	private StringToken properties_arg;
	private IntegerToken debuglevel_arg;
	private boolean profileSet = false;

	/** Default constructor builds set with all standard args. */
	public StdAppSettings()
	{
		this(true);
	}

	/**
	  Constructor that specifies whether or not this is a network-aware
	  application. Such applications will have hostname and servicename
	  arguments.
	  @param isNetworkApp true if this application is network-aware.
	*/
	public StdAppSettings(boolean isNetworkApp)
	{
		super();

		if (isNetworkApp)
		{
			host_arg = new StringToken(
				"h", "host-name", "", TokenOptions.optSwitch, "");
			addToken(host_arg);
		}
		else
		{
			host_arg = null;
		}

		properties_arg = new StringToken(
			"P", "Name (or path) of DECODES properties file", "", TokenOptions.optSwitch, "");
		debuglevel_arg = new IntegerToken("d", "debug-level", "",
			TokenOptions.optSwitch, 0);

		addToken(properties_arg);
		addToken(debuglevel_arg);
	}

	/**
	 * @return the host name specified on the command line
	 * arguments. Returns the local host name if none was
	 * specified.
	 */
	public String getHostName()
	{
		if (host_arg == null)
			return null;

		try
		{
			String r = host_arg.getValue();
			if (r == null || r.length() == 0)
				//return InetAddress.getLocalHost().getHostName();
				return "localhost";
			return r;
		}
		catch(Exception e) {}
		return null;
	}

    /**
     * @return the service specified on the command line, or null if none.
     */
//	public String getService()
//	{
//		if (service_arg == null)
//			return null;
//
//		String r = service_arg.getValue();
//		if (r != null && r.length() > 0)
//			return r;
//		return null;
//	}

	/**
	 * @return the name of the properties file specified on the command
	 * line or null if none was.
	 */
	public String getPropertiesFile()
	{
		String r = properties_arg.getValue();
		if (r != null && r.length() > 0)
		{
			profileSet = true;
			return r;
		}
		return null;
	}

	/**
	 * Let's applications know if the user manually set the profile.
	 *
	 * @return
	 */
	public boolean getProfileSet()
	{
		return profileSet;
	}

    /**
     * @return the numeric debug-level specified on the command line, or
     * 0 if none was specified.
     */
	public int getDebugLevel()
	{
		return debuglevel_arg.getValue();
	}
}
