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

import decodes.tsdb.alarm.editor.AlarmEditor;

public class AlarmEditLauncherAction extends LauncherAction
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	public AlarmEditLauncherAction()
	{
		super("Alarm Editor",
			new ImageIcon(EnvExpander.expand("$DCSTOOL_HOME/icons/alarms48x48.gif")));
		tag = "alarmedit";
		log.info("Instantiating {} launcher action.", tag);
	}

	@Override
	protected JFrame launchFrame()
	{
		AlarmEditor alarmEdit = new AlarmEditor();

		try
		{
			alarmEdit.setExitOnClose(false);
			alarmEdit.execute(launcherArgs);
			return alarmEdit.getFrame();
		}
		catch (Exception ex)
		{
			log.atError().setCause(ex).log("Cannot run {}", this.buttonLabel);
		}
		return null;
	}

}
