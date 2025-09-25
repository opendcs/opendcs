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
package decodes.tsdb.procmonitor;

import ilex.util.EnvExpander;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.ImageIcon;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import lrgs.gui.DecodesInterface;

import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TsdbAppTemplate;

public class ProcessMonitor extends TsdbAppTemplate
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private static String module = "procmon";
	private ProcessMonitorFrame pmFrame = null;
	private boolean exitOnClose = true;
	private DbPollThread dbPollThread = null;

	public ProcessMonitor()
	{
		super(module + ".log");
		dbPollThread = new DbPollThread(this);
	}

	@Override
	protected void runApp() throws Exception
	{
		pmFrame = new ProcessMonitorFrame();
		pmFrame.setTsdb(theDb);
		ImageIcon titleIcon = new ImageIcon(
				EnvExpander.expand("$DECODES_INSTALL_DIR/icons/toolkit24x24.gif"));
		pmFrame.setIconImage(titleIcon.getImage());
		pmFrame.setDbPollThread(dbPollThread);

		dbPollThread.start();
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

	private void close()
	{
		dbPollThread.shutdown();
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
		ProcessMonitor guiApp = new ProcessMonitor();
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

	public ProcessMonitorFrame getFrame() { return pmFrame; }

	@Override
	public void createDatabase()
	{
		// ProcessMonitor must work with XML DECODES databases too.
		// So don't create the TSDB interface.
	}

	@Override
	public void tryConnect()
	{
		// Likewise, don't connect to the TSDB. Just DECODES DB.
	}
}
