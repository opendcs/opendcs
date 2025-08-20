/*
 * 
 * This software was written by Cove Software, LLC ("COVE") under contract 
 * to the United States Government. 
 * 
 * No warranty is provided or implied other than specific contractual terms
 * between COVE and the U.S. Government
 * 
 * U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 */
package decodes.cwms.rating;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.ImageIcon;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import lrgs.gui.DecodesInterface;

import ilex.util.EnvExpander;
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
public class CwmsRatingGuiMain extends TsdbAppTemplate
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private CwmsRatingGuiFrame theFrame;
	private boolean packFrame = false;
	private static String logfile = "cwmsrating.log";
	
	private boolean exitOnClose = true;
	
	/**
	 * Constructor for TsDbGrpEditor
	 */
	public CwmsRatingGuiMain()
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
		theFrame = new CwmsRatingGuiFrame(theDb);

		/*
		 * Validate frames that have preset sizes
		 * Pack frames that have useful preferred size info,
		 * e.g. from their layout
		 */
		if (packFrame)
			theFrame.pack();
		else
			theFrame.validate();

		/*
		 * Set the frame title icon, center the frame window, and make the frame visible
		 */
		ImageIcon titleIcon = new ImageIcon(
				EnvExpander.expand("$DECODES_INSTALL_DIR/icons/toolkit24x24.gif"));
		theFrame.setIconImage(titleIcon.getImage());
		theFrame.setVisible(true);
		
		theFrame.addWindowListener(new WindowAdapter()
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
		theFrame.dispose();
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
	}

	/** Main method */
	public static void main(String[] args)
	{
		//Create the GUI Application
		DecodesInterface.setGUI(true);
		CwmsRatingGuiMain guiApp = new CwmsRatingGuiMain();
		try
		{			
			guiApp.execute(args);
		} 
		catch (Exception ex)
		{
			log.atError().setCause(ex).log("Unable to execute application.");
		}
	}
}
