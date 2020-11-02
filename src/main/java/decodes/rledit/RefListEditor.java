/*
*	$Id$
*
*	$Log$
*	Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*	OPENDCS 6.0 Initial Checkin
*	
*	Revision 1.6  2013/06/26 14:01:08  mmaloney
*	RefListEditor IS a GUI.
*	
*	Revision 1.5  2013/03/28 17:29:09  mmaloney
*	Refactoring for user-customizable decodes properties.
*	
*	Revision 1.4  2012/08/31 13:25:37  mmaloney
*	Get rid of stdout debug.
*	
*	Revision 1.3  2012/07/15 20:09:58  mmaloney
*	Removed reference to proprietary com.sutron stuff from open-source classes.
*	
*	Revision 1.2  2012/07/12 15:42:39  iblagoev
*	dev
*	
*	Revision 1.1  2008/04/04 18:21:04  cvs
*	Added legacy code to repository
*	
*	Revision 1.5  2008/02/10 20:17:34  mmaloney
*	dev
*	
*	Revision 1.2  2008/02/01 15:20:40  cvs
*	modified files for internationalization
*	
*	Revision 1.4  2006/04/14 12:36:08  mmaloney
*	DecodesSettings now uses ilex.util.PropertiesUtil to load & save. This will
*	make it easier to add new properties. DatabaseType has been renamed with a
*	'Code' suffix.
*	
*	Revision 1.3  2004/12/21 14:46:06  mjmaloney
*	Added javadocs
*	
*	Revision 1.2  2004/03/31 17:02:56  mjmaloney
*	Implemented decodes db interface and enum list table.
*	
*	Revision 1.1	2004/02/02 22:12:57	mjmaloney
*	dev.
*
*/
package decodes.rledit;

import javax.swing.UIManager;
import java.awt.*;
import java.io.*;
import java.util.*;

import lrgs.gui.DecodesInterface;

import ilex.util.*;
import decodes.db.*;
import decodes.util.*;

/**
Main class for the reference list editor.
*/
public class RefListEditor 
{
	private static ResourceBundle genericLabels = null;
	private static ResourceBundle labels = null;
	boolean packFrame = false;

	/** Construct the application. */
	public RefListEditor() 
	{
		RefListFrame frame = new RefListFrame();
		//Validate frames that have preset sizes
		//Pack frames that have useful preferred size info, e.g. from their layout
		if (packFrame) {
			frame.pack();
		}
		else {
			frame.validate();
		}
		//Center the window
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension frameSize = frame.getSize();
		if (frameSize.height > screenSize.height) {
			frameSize.height = screenSize.height;
		}
		if (frameSize.width > screenSize.width) {
			frameSize.width = screenSize.width;
		}
		frame.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
		frame.setVisible(true);
	}

	/**
	 * @return resource bundle containing generic labels for the selected
	 * language.
	 */
	public static ResourceBundle getGenericLabels() 
	{
		if (genericLabels == null)
		{
			DecodesSettings settings = DecodesSettings.instance();
			genericLabels = LoadResourceBundle.getLabelDescriptions(
				"decodes/resources/generic", settings.language);
		}
		return genericLabels;
	}

	/**
	 * @return resource bundle containing eledit-Editor labels for the selected
	 * language.
	 */
	public static ResourceBundle getLabels()
	{
		if (labels == null)
		{
			DecodesSettings settings = DecodesSettings.instance();
			labels = LoadResourceBundle.getLabelDescriptions(
				"decodes/resources/rledit", settings.language);
		}
		return labels;
	}

	/**
	  Command line arguments.
	  Only standard non-network-app arguments are required.
	*/
	static CmdLineArgs cmdLineArgs = new CmdLineArgs(false, "rledit.log");

	/**
	  Main method.
	  @param args command line arguments.
	*/
	public static void main(String[] args) 
		throws Exception
	{
		try {
//			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			UIManager.setLookAndFeel(new javax.swing.plaf.metal.MetalLookAndFeel());
		}
		catch(Exception e) {
			e.printStackTrace();
		}

		Logger.setLogger(new StderrLogger("DecodesDbEditor"));

		// Parse command line arguments.
		cmdLineArgs.parseArgs(args);
		genericLabels = getGenericLabels();
		labels = getLabels();
		Logger.instance().log(Logger.E_INFORMATION,
			"RefListEditor Starting (" + DecodesVersion.startupTag()
			+ ") =====================");

		DecodesSettings settings = DecodesSettings.instance();
		DecodesInterface.setGUI(true);

		// Construct the database and the interface specified by properties.
		Database db = new decodes.db.Database();
		
		Database.setDb(db);
		DatabaseIO dbio = 
			DatabaseIO.makeDatabaseIO(settings.editDatabaseTypeCode,
			settings.editDatabaseLocation);
		db.setDbIo(dbio);
		db.enumList.read();
		db.dataTypeSet.read();
		db.engineeringUnitList.read();
		
		new RefListEditor();
	}
}
