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
 * Revision 1.1  2017/06/27 13:44:47  mmaloney
 * Added for 6.4
 *
 */
package decodes.launcher;

import ilex.util.EnvExpander;
import ilex.util.Logger;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

import decodes.eventmon.EventMonitor;

public class EventMonLauncherAction
	extends LauncherAction
{
	public EventMonLauncherAction()
	{
		super("Event Monitor", 
			new ImageIcon(EnvExpander.expand("$DCSTOOL_HOME/icons/important48x48.png")));
		tag = "eventmon";
	}
	
	@Override
	protected JFrame launchFrame()
	{
		EventMonitor evtMon = new EventMonitor();
		try
		{
			evtMon.setExitOnClose(false);
			evtMon.runApp();
			return evtMon.getFrame();
		}
		catch (Exception ex)
		{
			String msg = "Cannot run Event Monitor: " + ex;
			Logger.instance().warning(msg);
			System.err.println(msg);
			ex.printStackTrace(System.err);
		}
		return null;
	}

}
