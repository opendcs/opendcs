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
package lrgs.gui;

import java.io.FileNotFoundException;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.gui.*;
import ilex.util.EnvExpander;

/**
This class has methods for accessing and initializing the LRGS GUI
properties.
*/
public class GeneralProperties
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	public static final String prefix = "General.";

	/**
	 * Loads the LRGS properties file (either specified with -P argument or
	 * from the default) and initializes the general properties required by
	 * all GUI apps.
	 */
	public static void init()
	{
		String propfile = EnvExpander.expand("$HOME/" + LrgsApp.PropFile);

		try
		{
			GuiApp.loadProperties(propfile);
		}
		catch(FileNotFoundException fnfe)
		{
			log.atWarn().setCause(fnfe).log("Default property values will be used.");
		}

		// Remove the deprecated Help URL properties.
		GuiApp.rmProperty(prefix+"HelpContents");
		GuiApp.rmProperty(prefix+"HelpAbout");
		GuiApp.rmProperty("Events.Help");
		GuiApp.rmProperty("LrgsAccess.Help");
		GuiApp.rmProperty("LrgsControl.Help");
		GuiApp.rmProperty("LrgsServices.Help");
		GuiApp.rmProperty("MessageBrowser.Help");
		GuiApp.rmProperty("RealTimeStatus.Help");
		GuiApp.rmProperty("SearchCritEditor.Help");

	}
}