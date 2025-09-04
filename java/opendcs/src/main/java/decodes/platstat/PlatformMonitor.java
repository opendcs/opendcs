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
package decodes.platstat;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.ImageIcon;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.util.EnvExpander;
import lrgs.gui.DecodesInterface;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TsdbAppTemplate;

public class PlatformMonitor extends TsdbAppTemplate
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	public static final String module = "PlatformMonitor";
	private boolean exitOnClose = true;
	private PlatformMonitorFrame frame = null;
	private DbPollThread dbPollThread = null;


	public PlatformMonitor()
	{
		super("platmon.log");
	}

	@Override
	protected void runApp() throws Exception
	{
		frame = new PlatformMonitorFrame(this);
		dbPollThread = new DbPollThread(this);
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
		frame.setDefaults();

		noExitAfterRunApp = true;
		dbPollThread.start();
	}

	public void setExitOnClose(boolean exitOnClose)
	{
		this.exitOnClose = exitOnClose;
	}

	public TimeSeriesDb getTsdb() { return theDb; }

	public void close()
	{
		dbPollThread.shutdown();
		if (frame != null)
			frame.dispose();
		if (exitOnClose)
			System.exit(0);
	}


	public static void main(String[] args)
	{
		DecodesInterface.setGUI(true);
		PlatformMonitor guiApp = new PlatformMonitor();
		try
		{
			guiApp.setExitOnClose(true);
			guiApp.execute(args);
		}
		catch (Exception ex)
		{
			log.atError().setCause(ex).log("Can not initialize.");
		}
	}

	public PlatformMonitorFrame getFrame()
	{
		return frame;
	}

	@Override
	public void createDatabase()
	{
		// PlatformMonitor must work with XML DECODES databases too.
		// So don't create the TSDB interface.
	}

	@Override
	public void tryConnect()
	{
		// Likewise, don't connect to the TSDB. Just DECODES DB.
	}
}
