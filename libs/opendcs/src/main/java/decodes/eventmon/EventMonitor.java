/**
 * $Id$
 * 
 * This software was written by Cove Software, LLC ("COVE") under contract 
 * to the United States Government. No warranty is provided or implied 
 * other than specific contractual terms between COVE and the U.S. Government
 * 
 * Copyright 2017 U.S. Government.
 *
 * $Log$
 */
package decodes.eventmon;

import ilex.util.EnvExpander;
import ilex.util.Logger;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.ImageIcon;

import lrgs.gui.DecodesInterface;

import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TsdbAppTemplate;

public class EventMonitor 
	extends TsdbAppTemplate
{
	private static String module = "eventmon";
	private EventMonitorFrame pmFrame = null;
	private boolean exitOnClose = true;

	public EventMonitor()
	{
		super(module + ".log");
	}

	@Override
	public void runApp() throws Exception
	{
		pmFrame = new EventMonitorFrame(this);
		ImageIcon titleIcon = new ImageIcon(
				EnvExpander.expand("$DECODES_INSTALL_DIR/icons/toolkit24x24.gif"));
		pmFrame.setIconImage(titleIcon.getImage());
		
		pmFrame.setVisible(true);
		
		pmFrame.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				close();
			}
		});
		noExitAfterRunApp = true;
	}
	
	void close()
	{
		if (pmFrame != null)
			pmFrame.dispose();
		if (exitOnClose)
			System.exit(0);
	}


	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		DecodesInterface.setGUI(true);
		EventMonitor guiApp = new EventMonitor();
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
	
	public EventMonitorFrame getFrame() { return pmFrame; }

	@Override
	public void createDatabase()
	{
		// EventMonitor must work with XML DECODES databases too.
		// So don't create the TSDB interface.
	}
	
	@Override
	public void tryConnect()
	{
		// Likewise, don't connect to the TSDB. Just DECODES DB.
	}
}
