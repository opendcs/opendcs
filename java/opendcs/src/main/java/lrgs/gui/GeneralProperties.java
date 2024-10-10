/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.3  2013/03/28 17:29:09  mmaloney
*  Refactoring for user-customizable decodes properties.
*
*  Revision 1.2  2008/11/21 16:19:51  mjmaloney
*  Sync with OS Repository.
*
*  Revision 1.1  2008/04/04 18:21:13  cvs
*  Added legacy code to repository
*
*  Revision 1.16  2004/08/31 21:08:37  mjmaloney
*  javadoc
*
*  Revision 1.15  2004/01/02 17:37:41  mjmaloney
*  Release Prep
*
*  Revision 1.14  2003/12/14 22:25:58  mjmaloney
*  Initial working editor with file IO but no CORBA.
*
*  Revision 1.13  2002/06/19 19:25:17  mjmaloney
*  Release preparation.
*
*  Revision 1.12  2002/05/03 18:53:18  mjmaloney
*  All DECODES operations are now encapsulated in the DecodesInterface class.
*  This allows the MessageBrowser to fail gracefully if the user attempts to
*  decode data and DECODES is not installed on this machine.
*  Also, Modified several properties to support pull-down menus in the dialog.
*
*  Revision 1.11  2001/02/28 21:09:39  mike
*  GUI DR fixe.
*
*  Revision 1.10  2001/02/17 18:41:56  mike
*  Modified to use Directory rather than Naming Service.
*
*  Revision 1.9  2000/10/23 13:16:43  mike
*  New scheme for help URL properties. Just specify the root. Each screen
*  adds the unique name to the root URL.
*
*  Revision 1.8  2000/05/24 19:40:02  mike
*  Default for unix text editor is now 'emacs'
*
*  Revision 1.7  2000/05/04 19:51:27  mike
*  Added isWindows method.
*
*  Revision 1.6  2000/05/03 15:31:47  mike
*  Initialize browser differently under Windows than Unix.
*
*  Revision 1.5  2000/04/06 20:33:49  mike
*  Update help URLs to be the same as the screen class with ".html" extension.
*
*  Revision 1.4  2000/04/05 20:35:05  mike
*  dev
*
*  Revision 1.3  2000/04/01 02:16:28  mike
*  dev
*
*  Revision 1.2  2000/03/30 22:53:31  mike
*  dev
*
*  Revision 1.1  2000/03/27 22:17:52  mike
*  Created
*
*/
package lrgs.gui;

import java.io.FileNotFoundException;

import decodes.util.DecodesVersion;

import ilex.gui.*;
import ilex.util.EnvExpander;

/**
This class has methods for accessing and initializing the LRGS GUI
properties.
*/
public class GeneralProperties
{
	public static final String prefix = "General.";

	/**
	 * Loads the LRGS properties file (either specified with -P argument or
	 * from the default) and initializes the general properties required by
	 * all GUI apps.
	 * @param the command line arguments
	 */
	public static void init()
	{
		String propfile = EnvExpander.expand("$HOME/" + LrgsApp.PropFile);

		try { GuiApp.loadProperties(propfile); }
		catch(FileNotFoundException fnfe)
		{
			System.err.println(fnfe);
			System.err.println("Default property values will be used.");
		}

		// Remove the deprecated Help URL properties.
		GuiApp.rmProperty(prefix+"HelpContents");
		GuiApp.rmProperty(prefix+"HelpAbout");
		GuiApp.rmProperty("Events.Help");
		GuiApp.rmProperty("LrgsAccess.Help");
		GuiApp.rmProperty("LrgsControl.Help");
		GuiApp.rmProperty("LrgsServices.Help");
		GuiApp.rmProperty("MessageBrowser.Help");
		GuiApp.rmProperty("RealTimeStatus.Help");
		GuiApp.rmProperty("SearchCritEditor.Help");

		// Init help root URL & set default if necessary.
		String s = getHelpRootUrl();  
		if (s.equals("http://www.ilexeng.com/LRGS-3.2/help/"))
			GuiApp.setProperty(prefix+"HelpRoot",
				"http://www.ilexeng.com/" + LrgsApp.SubDir + "/help/");

		String OSName = System.getProperty("os.name");
		if (OSName.startsWith("Windows"))
		{
			GuiApp.getProperty(prefix+"TextEditor","notepad.exe");
			GuiApp.getProperty(prefix+"Browser",
				"C:\\Program Files\\Netscape\\Communicator\\Program\\Netscape.exe");
		}
		else
		{
			GuiApp.getProperty(prefix+"Browser","mozilla");
			if (OSName.startsWith("Linux"))
				GuiApp.getProperty(prefix+"TextEditor","gedit");
			else
				GuiApp.getProperty(prefix+"TextEditor","xedit");
		}
	}

//	/** @return true if we're running on a windoze box. */
//	public static boolean isWindows()
//	{
//		String OSName = System.getProperty("os.name");
//		return OSName.startsWith("Win");
//	}

	/** @return the URL for the root of the help directory. */
	public static String getHelpRootUrl()
	{
		return GuiApp.getProperty(prefix+"HelpRoot",
			"http://www.covesw.com/" + DecodesVersion.getAbbr() + "/help/");
	}

	/** @return the URL for the "contents" help page. */
	public static String getHelpContentsUrl()
	{
		return getHelpRootUrl() + "contents.html";
	}

	/** @return the URL for the "about" help page. */
	public static String getHelpAboutUrl()
	{
		return getHelpRootUrl() + "about.html";
	}
}


