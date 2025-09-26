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
package decodes.tsdb.groupedit;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.ImageIcon;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import lrgs.gui.DecodesInterface;

import ilex.util.EnvExpander;

import decodes.db.Database;
import decodes.db.Site;
import decodes.tsdb.BadConnectException;
import decodes.tsdb.TsdbAppTemplate;
import decodes.util.DecodesException;

/**
 *
 * This is the main class for the Time Series Database Group Editor GUI.
 * This class calls the TsDbGrpEditorFrame class which is the frame
 * that contains the Time Series Groups Tab at the moment. It may be
 * expanded to contain the Time Series Data Descriptor Tab and the Alarms Tab.
 *
 */
public class TsDbGrpEditor extends TsdbAppTemplate
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	//Editor
	private static String module = "TsDbGrpEditor";
	//Editor Owner

	//Editor Components
	private TsDbGrpEditorFrame tsDbGrpEditorFrame;
	private boolean packFrame = false;
	//Time Series DB

	//Miscellaneous
	private boolean startApp;
	private Throwable exMsg;

	private boolean exitOnClose = true;

	/**
	 * Constructor for TsDbGrpEditor
	 */
	public TsDbGrpEditor()
	{
		super("TsDbGrpEditor.log");
		startApp = true;
		exMsg = null;
		Site.explicitList = true;
	}

	/**
	 * Get the frame for TsDbGrpEditor
	 * @return TsDbGrpEditorFrame
	 */
	public TsDbGrpEditorFrame getFrame()
	{
		return tsDbGrpEditorFrame;
	}

	/**
	 * Run the GUI application.
	 */
	@Override
	public void runApp()
	{
		//If can not connect to DB, display errors and exit.
		if (!startApp)
		{
			log.atError().setCause(exMsg).log("Cannot initialize.");
			return;
		}

		//If can connect to DB, launch application.
		/*
		 * Create Time Series top frame and pass theDb and labelDescriptor.
		 */
		tsDbGrpEditorFrame = new TsDbGrpEditorFrame(TsDbGrpEditor.theDb);

		/*
		 * Validate frames that have preset sizes
		 * Pack frames that have useful preferred size info,
		 * e.g. from their layout
		 */
		if (packFrame)
			tsDbGrpEditorFrame.pack();
		else
			tsDbGrpEditorFrame.validate();

		/*
		 * Set the frame title icon, center the frame window, and make the frame visible
		 */
		ImageIcon titleIcon = new ImageIcon(
				EnvExpander.expand("$DECODES_INSTALL_DIR/icons/toolkit24x24.gif"));
		tsDbGrpEditorFrame.setIconImage(titleIcon.getImage());
		tsDbGrpEditorFrame.setVisible(true);

		tsDbGrpEditorFrame.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				close();
			}
		});
		noExitAfterRunApp = true;
	}

	//Overload this method so that we catch when a Database Connection
	//error occurs and displays an Error to the user.
	@Override
	protected void badConnect(String appName, BadConnectException ex)
	{
		log.atError().setCause(ex).log("Cannot connect to DB");
		exMsg = ex;
	}

	//Overload this method so that we catch when a Database Connection
	//error occurs and displays an Error to the user.
	@Override
	protected void authFileEx(String afn, Exception ex)
	{
		log.atError().setCause(ex).log("Cannot read DB auth from file '{}'", afn);
		startApp = false;
		exMsg = ex;
	}

	private void close()
	{
		tsDbGrpEditorFrame.dispose();
		if (exitOnClose)
			System.exit(0);
	}

	public void setExitOnClose(boolean tf)
	{
		exitOnClose = tf;
	}

	public void initDecodes()
		throws DecodesException
	{
		if (DecodesInterface.isInitialized())
			return;
		DecodesInterface.initDecodesMinimal(cmdLineArgs.getPropertiesFile());
		DecodesInterface.readSiteList();
		Database.getDb().dataTypeSet.read();
	}

	/** Main method */
	public static void main(String[] args)
	{
		DecodesInterface.setGUI(true);
		TsDbGrpEditor guiApp = new TsDbGrpEditor();
		try
		{
			guiApp.execute(args);
		}
		catch (Exception ex)
		{
			log.atError().setCause(ex).log("Can not initialize Group Editor.");
		}
	}
}