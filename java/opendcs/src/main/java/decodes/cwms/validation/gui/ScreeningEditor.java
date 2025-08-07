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
package decodes.cwms.validation.gui;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.ImageIcon;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.util.EnvExpander;
import lrgs.gui.DecodesInterface;
import decodes.db.Database;
import decodes.tsdb.TsdbAppTemplate;
import decodes.util.DecodesException;

public class ScreeningEditor extends TsdbAppTemplate
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
			log.atError().setCause(ex).log("Unable to initialize screening editor.");
		}
	}

}
