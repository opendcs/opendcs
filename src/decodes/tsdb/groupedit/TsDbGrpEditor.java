/*
 * $Id$
 * 
 * $Log$
 * Revision 1.12  2012/07/24 14:03:16  mmaloney
 * Site.explicitList = true;
 *
 * Revision 1.11  2012/07/24 13:40:14  mmaloney
 * groupedit cosmetic bugs
 *
 * Revision 1.10  2012/05/15 15:11:52  mmaloney
 * exitOnClose boolean moved to base class TopFrame
 *
 * Revision 1.9  2012/01/17 17:54:34  mmaloney
 * Call DecodesInterface.setGUI(true).
 *
 * Revision 1.8  2011/03/01 15:55:57  mmaloney
 * Implement TsListMain, TsListPanel, and TsListFrame
 *
 * Revision 1.7  2011/02/03 20:00:23  mmaloney
 * Time Series Group Editor Mods
 *
 * Revision 1.6  2011/02/02 20:40:34  mmaloney
 * bug fixes
 *
 * 
 */
package decodes.tsdb.groupedit;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.ImageIcon;

import lrgs.gui.DecodesInterface;

import ilex.util.EnvExpander;
import ilex.util.Logger;

import decodes.db.Database;
import decodes.db.Site;
import decodes.tsdb.BadConnectException;
import decodes.tsdb.TsdbAppTemplate;
import decodes.util.DecodesException;

/**
 * 
 * This is the main class for the Time Series Database Group Editor GUI.
 * This class calls the TsDbGrpEditorFrame class which is the frame 
 * that contains the Time Series Groups Tab at the moment. It may be
 * expanded to contain the Time Series Data Descriptor Tab and the Alarms Tab.
 * 
 */
public class TsDbGrpEditor extends TsdbAppTemplate
{
	//Editor
	private static String module = "TsDbGrpEditor";
	//Editor Owner

	//Editor Components
	private TsDbGrpEditorFrame tsDbGrpEditorFrame;
	private boolean packFrame = false;
	//Time Series DB

	//Miscellaneous
	private boolean startApp;
	private String exMsg;
	
	private boolean exitOnClose = true;
	
	/**
	 * Constructor for TsDbGrpEditor
	 */
	public TsDbGrpEditor()
	{
		super("TsDbGrpEditor.log");
		startApp = true;
		exMsg = "";
		Site.explicitList = true;
	}

	/**
	 * Get the frame for TsDbGrpEditor
	 * @return TsDbGrpEditorFrame
	 */
	public TsDbGrpEditorFrame getFrame()
	{
		return tsDbGrpEditorFrame; 
	}

	/**
	 * Run the GUI application.
	 */
	@Override
	public void runApp()
	{
		//If can not connect to DB, display errors and exit.
		if (!startApp)
		{
			System.err.println("Cannot initialize: " + exMsg);
			return;
		}
		
		//If can connect to DB, launch application.
		/*
		 * Create Time Series top frame and pass theDb and labelDescriptor.
		 */
		tsDbGrpEditorFrame = new TsDbGrpEditorFrame(TsDbGrpEditor.theDb);

		/*
		 * Validate frames that have preset sizes
		 * Pack frames that have useful preferred size info,
		 * e.g. from their layout
		 */
		if (packFrame)
			tsDbGrpEditorFrame.pack();
		else
			tsDbGrpEditorFrame.validate();

		/*
		 * Set the frame title icon, center the frame window, and make the frame visible
		 */
		ImageIcon titleIcon = new ImageIcon(
				EnvExpander.expand("$DECODES_INSTALL_DIR/icons/toolkit24x24.gif"));
		tsDbGrpEditorFrame.setIconImage(titleIcon.getImage());
//		tsDbGrpEditorFrame.centerOnScreen();
		tsDbGrpEditorFrame.setVisible(true);
		
		tsDbGrpEditorFrame.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				close();
			}
		});

	}

	//Overload this method so that we catch when a Database Connection
	//error occurs and displays an Error to the user.
	@Override
	protected void badConnect(String appName, BadConnectException ex)
	{
		String msg = appName + " Cannot connect to DB: " + ex.getMessage();
		System.err.println(msg);
		Logger.instance().failure(msg);
		startApp = false;
		exMsg = ex.toString();
	}
	
	//Overload this method so that we catch when a Database Connection
	//error occurs and displays an Error to the user.
	@Override
	protected void authFileEx(String afn, Exception ex)
	{
		String msg = "Cannot read DB auth from file '" + afn + "': " + ex;
		System.err.println(msg);
		Logger.instance().failure(msg);
		startApp = false;
		exMsg = ex.toString();
	}
	
	private void close()
	{
		tsDbGrpEditorFrame.dispose();
		if (exitOnClose)
			System.exit(0);
	}

	public void setExitOnClose(boolean tf)
	{
		exitOnClose = tf;
	}
	
	public void initDecodes()
		throws DecodesException
	{
		if (DecodesInterface.isInitialized())
			return;
		DecodesInterface.initDecodesMinimal(cmdLineArgs.getPropertiesFile());
		DecodesInterface.readSiteList();
		Database.getDb().dataTypeSet.read();
	}

	/** Main method */
	public static void main(String[] args)
	{
		//Create the GUI Application
		DecodesInterface.setGUI(true);
		TsDbGrpEditor guiApp = new TsDbGrpEditor();
		try
		{			
			guiApp.execute(args);
		} 
		catch (Exception ex)
		{
			String msg = module + " Can not initialize Group Editor. "
				+ ex.getMessage();
			Logger.instance().failure(msg);
			System.err.println(msg);
			ex.printStackTrace(System.err);
		}
	}
}
