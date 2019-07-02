/*
 * $Id$
 * 
 * $Log$
 * Revision 1.2  2014/08/22 17:23:10  mmaloney
 * 6.1 Schema Mods and Initial DCP Monitor Implementation
 *
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 * Revision 1.4  2013/04/26 14:22:47  mmaloney
 * added getFrame()
 *
 * Revision 1.3  2012/05/15 15:12:14  mmaloney
 * Use DECODES_INSTALL_DIR, not DCSTOOL_HOME because this is in the legacy branch.
 *
 * Revision 1.2  2012/01/17 17:54:34  mmaloney
 * Call DecodesInterface.setGUI(true).
 *
 * Revision 1.1  2011/03/01 15:55:57  mmaloney
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
import java.util.ResourceBundle;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import lrgs.gui.DecodesInterface;

import ilex.util.AsciiUtil;
import ilex.util.EnvExpander;
import ilex.util.LoadResourceBundle;
import ilex.util.Logger;
import decodes.db.Database;
import decodes.tsdb.BadConnectException;
import decodes.tsdb.TsdbAppTemplate;
import decodes.util.DecodesException;
import decodes.util.DecodesSettings;

/**
 * 
 * This is the main class for the Time Series Database Group Editor GUI.
 * This class calls the TsDbGrpEditorFrame class which is the frame 
 * that contains the Time Series Groups Tab at the moment. It may be
 * expanded to contain the Time Series Data Descriptor Tab and the Alarms Tab.
 * 
 */
public class TsListMain extends TsdbAppTemplate
{
	private static String module = "TsList";

	private TsListFrame tsListFrame;
	private boolean packFrame = false;
	private static String logfile = "tslist.log";
	
	private boolean exitOnClose = true;
	
	/**
	 * Constructor for TsDbGrpEditor
	 */
	public TsListMain()
	{
		super(logfile);
	}

	/**
	 * Run the GUI application.
	 */
	@Override
	public void runApp()
	{
		/*
		 * Create Time Series top frame and pass theDb and labelDescriptor.
		 */
		tsListFrame = new TsListFrame(TsDbGrpEditor.theDb);

		/*
		 * Validate frames that have preset sizes
		 * Pack frames that have useful preferred size info,
		 * e.g. from their layout
		 */
		if (packFrame)
			tsListFrame.pack();
		else
			tsListFrame.validate();

		/*
		 * Set the frame title icon, center the frame window, and make the frame visible
		 */
		ImageIcon titleIcon = new ImageIcon(
				EnvExpander.expand("$DECODES_INSTALL_DIR/icons/toolkit24x24.gif"));
		tsListFrame.setIconImage(titleIcon.getImage());
//		tsListFrame.centerOnScreen();
		tsListFrame.setVisible(true);
		
		tsListFrame.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				close();
			}
		});
		noExitAfterRunApp = true;
	}

	private void close()
	{
		tsListFrame.dispose();
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
	
	public TsListFrame getFrame() { return tsListFrame; }

	/** Main method */
	public static void main(String[] args)
	{
		//Create the GUI Application
		DecodesInterface.setGUI(true);
		TsListMain guiApp = new TsListMain();
		try
		{			
			guiApp.execute(args);
		} 
		catch (Exception ex)
		{
			System.err.println(
				module + " Can not initialize Group Editor. "
				+ ex.getMessage());
		}
	}
}
