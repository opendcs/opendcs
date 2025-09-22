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
package decodes.tsdb.alarm.editor;

import ilex.util.EnvExpander;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.ImageIcon;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;


import lrgs.gui.DecodesInterface;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TsdbAppTemplate;

public class AlarmEditor extends TsdbAppTemplate
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();

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
			log.atError().setCause(ex).log("Can not initialize.");
		}
	}

	public void setExitOnClose(boolean exitOnClose)
	{
		this.exitOnClose = exitOnClose;
	}

	public TimeSeriesDb getTsdb() { return theDb; }
	
}
