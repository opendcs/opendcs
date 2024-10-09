/**
 * $Id$
 * 
 * Copyright 2015 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * 
 * $Log$
 */
package decodes.cwms.validation.gui;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.ImageIcon;

import ilex.util.EnvExpander;
import ilex.util.Logger;
import lrgs.gui.DecodesInterface;
import decodes.db.Database;
import decodes.tsdb.TsdbAppTemplate;
import decodes.util.DecodesException;

public class ScreeningEditor extends TsdbAppTemplate
{
	public static final String module = "ScreeningEditor";
	private ScreeningEditFrame frame = null;
	private boolean exitOnClose = true;


	public ScreeningEditor()
	{
		super("scredit");
	}

	@Override
	protected void runApp() throws Exception
	{
		frame = new ScreeningEditFrame(theDb);
		frame.getScreeningIdListTab().refresh();
		
		ImageIcon titleIcon = new ImageIcon(
			EnvExpander.expand("$DECODES_INSTALL_DIR/icons/toolkit24x24.gif"));
		frame.setIconImage(titleIcon.getImage());
		frame.setVisible(true);
	
		frame.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				close();
			}
		});
		noExitAfterRunApp = true;
	}
	
	public void initDecodes()
		throws DecodesException
	{
		if (DecodesInterface.isInitialized())
			return;
		DecodesInterface.initDecodesMinimal(cmdLineArgs.getProfile().getFile().getAbsolutePath());
		DecodesInterface.readSiteList();
		Database.getDb().dataTypeSet.read();
	}

	private void close()
	{
		frame.dispose();
		if (exitOnClose)
			System.exit(0);
	}

	public void setExitOnClose(boolean tf)
	{
		exitOnClose = tf;
	}


	public static void main(String[] args)
	{
		DecodesInterface.setGUI(true);
		ScreeningEditor guiApp = new ScreeningEditor();
		try
		{
			guiApp.setExitOnClose(true);
			guiApp.execute(args);
		} 
		catch (Exception ex)
		{
			String msg = module + " Can not initialize: " + ex.getMessage();
			Logger.instance().failure(msg);
			System.err.println(msg);
			ex.printStackTrace(System.err);
		}
	}

}
