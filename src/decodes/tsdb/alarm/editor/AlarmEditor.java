/**
 * $Id$
 * 
 * Copyright 2017 Cove Software, LLC. All rights reserved.
 * 
 * $Log$
 * Revision 1.2  2017/06/13 16:35:15  mmaloney
 * Added getFrame method to support launcher.
 *
 * Revision 1.1  2017/05/17 20:36:57  mmaloney
 * First working version.
 *
 */
package decodes.tsdb.alarm.editor;

import ilex.util.EnvExpander;
import ilex.util.Logger;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.ImageIcon;

import lrgs.gui.DecodesInterface;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TsdbAppTemplate;

public class AlarmEditor
	extends TsdbAppTemplate

{
	private static String module = "alarmedit";
	private AlarmEditFrame aeFrame = null;
	private boolean exitOnClose = true;

	public AlarmEditor()
	{
		super(module + ".log");
	}

	@Override
	public void runApp() throws Exception
	{
		aeFrame = new AlarmEditFrame(this);
		ImageIcon titleIcon = new ImageIcon(
				EnvExpander.expand("$DECODES_INSTALL_DIR/icons/toolkit24x24.gif"));
		aeFrame.setIconImage(titleIcon.getImage());
		
		aeFrame.setVisible(true);
		
		aeFrame.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				close();
			}
		});
		noExitAfterRunApp = true;
	}
	
	public AlarmEditFrame getFrame() { return aeFrame; }
	
	void close()
	{
		if (aeFrame != null)
			aeFrame.dispose();
		if (exitOnClose)
			System.exit(0);
	}


	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		DecodesInterface.setGUI(true);
		AlarmEditor guiApp = new AlarmEditor();
		try
		{			
			guiApp.execute(args);
		} 
		catch (Exception ex)
		{
			String msg = module + " Can not initialize. " + ex.getMessage();
			Logger.instance().failure(msg);
			System.err.println(msg);
			ex.printStackTrace(System.err);
		}
	}

	public void setExitOnClose(boolean exitOnClose)
	{
		this.exitOnClose = exitOnClose;
	}

	public TimeSeriesDb getTsdb() { return theDb; }
	
	@Override
	public void createDatabase()
	{
		// No need for TSDB interface.
	}
	
	@Override
	public void tryConnect()
	{
		// Likewise, don't connect to the TSDB. Just DECODES DB.
	}

}
