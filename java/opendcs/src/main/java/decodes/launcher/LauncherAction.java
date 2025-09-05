/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
* 
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software 
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations 
* under the License.
*/
package decodes.launcher;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.util.DecodesException;

public abstract class LauncherAction extends WindowAdapter
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
						String msg = "Cannot launch " + buttonLabel;
						log.atError().setCause(ex).log(msg);
						launcherFrame.showError(msg + ": "+ ex);
					}
				}
			};
			launcherFrame.completeDecodesInit();
		}
		catch (DecodesException ex)
		{
			log.atError().setCause(ex).log("Cannot initialize DECODES.");
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
 		log.trace("{} window closed.", buttonLabel);
 		visibleFrame = null;
 	}
 	
 	public void setLauncherArgs(String launcherArgs[])
 	{
 		this.launcherArgs = launcherArgs;
 	}
}
