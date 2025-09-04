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

import ilex.util.EnvExpander;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.eventmon.EventMonitor;

public class EventMonLauncherAction extends LauncherAction
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
			log.atError().setCause(ex).log("Cannot run Event Monitor.");
		}
		return null;
	}

}
