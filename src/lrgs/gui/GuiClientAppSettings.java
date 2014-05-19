/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:13  cvs
*  Added legacy code to repository
*
*  Revision 1.6  2008/01/17 18:30:43  mmaloney
*  files modified
*
*  Revision 1.5  2007/03/21 19:40:18  mmaloney
*  Moved CORBA-dependent stuff into the GUIs that use them.
*
*  Revision 1.4  2005/01/20 14:41:40  mjmaloney
*  *** empty log message ***
*
*  Revision 1.3  2004/08/31 21:08:37  mjmaloney
*  javadoc
*
*  Revision 1.2  2001/03/09 01:30:24  mike
*  3.2 release
*
*  Revision 1.1  2001/02/17 18:41:56  mike
*  Modified to use Directory rather than Naming Service.
*
*/
package lrgs.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import decodes.util.DecodesSettings;
import ilex.cmdline.*;
import ilex.gui.GuiApp;
import ilex.corba.ClientAppSettings;
import ilex.util.Logger;
import ilex.util.FileLogger;

/**
Extension of ClientAppSettings adding arguments appropriate for the LRGS
GUI applications.
*/
public class GuiClientAppSettings
	extends ClientAppSettings
{
	public StringToken url_arg;
	private StringToken log_arg;

	/** default constructor */
    public GuiClientAppSettings(boolean isNetworkApp)
	{
		super(isNetworkApp);
		url_arg = new StringToken(
			"U", "URL to LrgsSystem IOR", "", TokenOptions.optSwitch, "");
		addToken(url_arg);
		log_arg = new StringToken(
			"l", "Log File Name", "", TokenOptions.optSwitch, "");
		addToken(log_arg);
    }

	/**
	  Makes & returns a finder object so that the GUI apps can find the
	  CORBA objects that they need to interface with.
	  If the -U argument was specified on the command line, the user has
	  specified an LrgsSystem object directly. That is the finder.
	  <p>
	  Otherwise we use the General.DirectoryIOR property and -H hostname
	  argument to lookup the LrgsSystem object.
	  @return the LrgsSystemFinder object.
	*/
//	public LrgsSystemFinder makeFinder()
//		throws LrgsGuiException
//	{
//		String url = url_arg.getValue();
//		if (url.length() > 0)
//		{
//			return new UrlLrgsSystemFinder(url);
//		}
//		else
//		{
//			String dirUrl = GuiApp.getProperty("General.DirectoryIOR");
//			String host = host_arg.getValue();
//			if (dirUrl == null || dirUrl.length() == 0 || host.length() == 0)
//				throw new LrgsGuiException(
//					"Insufficient info to connect. Must specify -U <service> "
//					+ "or directory and -H <host>.");
//			return new DirectoryLrgsSystemFinder(dirUrl, host);
//		}
//	}

	/**
	 * @return the log file specified on the command line, or null if none.
	 */
	public String getLogFileName()
	{
		String r = log_arg.getValue().trim();
		if (r == null || r.length() == 0)
			return null;
		return r;
	}

	/**
	 * Parses command line arguments and sets the log file, if one was
	 * specified on the command line.
	 */
	public void parseArgs(String[] args)
		throws IllegalArgumentException
	{
		super.parseArgs(args);
		String fn = getLogFileName();
		if (fn != null)
		{
			try
			{
				FileLogger flog = 
					new FileLogger(Logger.instance().getProcName(), fn);
				Logger.setLogger(flog);
			}
			catch(IOException ex)
			{
				throw new IllegalArgumentException("Cannot open log file '"
					+ fn + "': " + ex);
			}
		}
		//New Code
		//Load the decodes.properties
		String propFile;
		propFile = super.getPropertiesFile();
		if (propFile == null || propFile.length() == 0)
		{
			// Get DECODES_INSTALL_DIR from system properties & look there.
			String installDir = System.getProperty("DECODES_INSTALL_DIR");
			if (installDir != null)
				propFile = installDir + File.separator + "decodes.properties";
		}
		DecodesSettings settings = DecodesSettings.instance();
		if (!settings.isLoaded())
		{
			Properties props = new Properties();
			try
			{
				FileInputStream fis = new FileInputStream(propFile);
				props.load(fis);
				fis.close();
			}
			catch(IOException e)
			{
				Logger.instance().log(Logger.E_WARNING,
				"GuiClientAppSettings:parseArgs " +
				"Cannot open DECODES Properties File '"+propFile+"': "+e);
			}
			settings.loadFromProperties(props);
		}
		//End new code
	}
}
