/**
 * $Id$
 * 
 * Copyright 2017 U.S. Government
 * 
 * $Log$
 * Revision 1.3  2019/10/25 15:15:19  mmaloney
 * dev
 *
 * Revision 1.2  2019/10/22 12:39:39  mmaloney
 * Pass launcher args to launcher actions.
 *
 * Revision 1.1  2017/06/13 20:02:33  mmaloney
 * dev
 *
 */
package decodes.launcher;

import ilex.util.Logger;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

import decodes.util.DecodesException;

public abstract class LauncherAction
	extends WindowAdapter
{
	protected String buttonLabel = null;
	
	protected ImageIcon imageIcon = null;
	
	private JFrame visibleFrame = null;

	private LauncherFrame launcherFrame = null;
	
	protected String launcherArgs[] = {};
	
	protected String tag = "";


	public String getTag()
	{
		return tag;
	}

	protected LauncherAction(String buttonLabel, ImageIcon imageIcon)
	{
		super();
		this.buttonLabel = buttonLabel;
		this.imageIcon = imageIcon;
	}

	public void buttonPressed()
	{
		Profile profile = launcherFrame.getSelectedProfile();
		if (profile != null)
		{
			launcherFrame.sendToProfileLauncher(profile, "start " + tag);
			return;
		}

		if (visibleFrame != null)
		{
			visibleFrame.toFront();
			return;
		}
		
		// DB init in progress!
		if (launcherFrame.afterDecodesInit != null)
			return;

		final LauncherAction thisAction = this;
		try
		{
			launcherFrame.afterDecodesInit = new Runnable()
			{
				public void run()
				{
					try
					{
						visibleFrame = launchFrame();
						visibleFrame.addWindowListener(thisAction);
					}
					catch (Exception ex)
					{
						String msg = "Cannot launch " + buttonLabel + ": " + ex;
						Logger.instance().warning(msg);
						System.err.println(msg);
						ex.printStackTrace();
						launcherFrame.showError(msg);
					}
				}
			};
			launcherFrame.completeDecodesInit();
		}
		catch (DecodesException ex)
		{
			Logger.instance().failure("Cannot initialize DECODES: '" + ex);
		}
	}

	/**
	 * Concrete subclass overloads this to create the JFrame.
	 * 
	 * @return
	 */
	protected abstract JFrame launchFrame();
	
	public String getButtonLabel()
	{
		return buttonLabel;
	}

	public ImageIcon getImageIcon()
	{
		return imageIcon;
	}

	/**
	 * Called by the launcher immediately after creation.
	 * @param launcherFrame
	 */
	public void setLauncherFrame(LauncherFrame launcherFrame)
	{
		this.launcherFrame = launcherFrame;
	}
	
 	public void windowClosed(WindowEvent ev)
 	{
 		Logger.instance().debug1(buttonLabel + " window closed.");
 		visibleFrame = null;
 	}
 	
 	public void setLauncherArgs(String launcherArgs[])
 	{
 		this.launcherArgs = launcherArgs;
 	}
}
